package com.stockreport.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.stockreport.domain.stock.Timeframe;
import com.stockreport.dto.response.ParseTextResult;
import com.stockreport.dto.response.StockAnalysisReport;
import com.stockreport.dto.response.StockDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api-key:}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    // 서킷 브레이커: 10회 슬라이딩 윈도우 중 50% 실패 시 30초 OPEN
    private final CircuitBreaker geminiCircuitBreaker = CircuitBreaker.of("geminiApi",
            CircuitBreakerConfig.custom()
                    .slidingWindowSize(10)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .build());

    // 재시도: 최대 3회, 지수 백오프 (1s → 2s → 4s)
    private final Retry geminiRetry = Retry.of("geminiApi",
            RetryConfig.custom()
                    .maxAttempts(3)
                    .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
                    .build());

    public ParseTextResult parseTextToConditions(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String prompt = """
                당신은 주식 시그널 조건 생성기입니다.
                사용자의 자연어 설명을 아래 JSON 형식으로 변환하세요.

                ## 지원 필드 (field / compareField)
                - close_price: 종가
                - open_price: 시가
                - high_price: 고가
                - low_price: 저가
                - volume: 거래량
                - change_rate: 등락률(%%)
                - ma5: 5봉 이동평균
                - ma10: 10봉 이동평균
                - ma20: 20봉 이동평균
                - ma60: 60봉 이동평균
                - rsi14: RSI(14)
                - macd: MACD
                - macd_signal: MACD Signal
                - macd_hist: MACD Histogram

                ## 지원 연산자 (operator)
                >, >=, <, <=, ==, !=
                crossover: 이전 봉에서는 미충족, 현재 봉에서 충족 (상향 돌파)
                crossunder: 이전 봉에서는 충족, 현재 봉에서 미충족 (하향 이탈)

                ## timeframe 감지
                - 월봉/월간/월 기준 언급 시: "timeframe": "MONTHLY"
                - 주봉/주간/주 기준 언급 시: "timeframe": "WEEKLY"
                - 일봉/일간/일 기준 또는 언급 없을 시: "timeframe": "DAILY"

                ## JSON 형식 (timeframe 포함)
                {"version":"1.0","timeframe":"DAILY","logic":"AND","conditions":[{"id":"c1","field":"rsi14","operator":"<","value":30}]}

                필드 간 비교 (compareField):
                {"version":"1.0","timeframe":"MONTHLY","logic":"AND","conditions":[{"id":"c1","field":"close_price","operator":">","compareField":"ma10"}]}

                ## 변환 규칙
                - "종가가 MA 위" → field: close_price, compareField: 해당 MA 필드
                - "종가가 MA10 상향 돌파/골든크로스" → operator: crossover, compareField: ma10
                - "종가가 MA10 하향 이탈/데드크로스" → operator: crossunder, compareField: ma10
                - 숫자 비교는 value, 필드 간 비교는 compareField 사용
                - value와 compareField 중 반드시 하나만 사용
                - 조건이 모호해도 반드시 가장 합리적인 조건 하나 이상 생성
                - JSON만 출력하고 다른 텍스트는 절대 포함하지 말 것
                - id는 c1, c2, c3... 또는 g1, g2... 형식

                ## 변환할 내용
                %s
                """.formatted(text);

        try {
            String responseBody = callGeminiApi(prompt);
            JsonNode root = objectMapper.readTree(responseBody);
            String result = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").stringValue("");
            result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            // timeframe 추출 후 conditions에서 제거
            JsonNode resultNode = objectMapper.readTree(result);
            String timeframeStr = resultNode.path("timeframe").stringValue("DAILY");
            Timeframe timeframe;
            try { timeframe = Timeframe.valueOf(timeframeStr); }
            catch (Exception ex) { timeframe = Timeframe.DAILY; }

            // timeframe 필드를 conditions JSON에서 제거
            if (resultNode.has("timeframe")) {
                ((ObjectNode) resultNode).remove("timeframe");
            }
            return new ParseTextResult(objectMapper.writeValueAsString(resultNode), timeframe);
        } catch (Exception e) {
            log.error("Gemini parse error", e);
            return null;
        }
    }

    public String analyzeSignalStrategy(String signalName, String marketFilter, String timeframe, String conditionsJson) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Gemini API 키가 설정되지 않았습니다. application.yml에 gemini.api-key를 설정해주세요.";
        }

        String humanReadable = conditionsToHumanReadable(conditionsJson);
        String prompt = buildPrompt(signalName, marketFilter, timeframe, humanReadable, conditionsJson);

        try {
            String responseBody = callGeminiApi(prompt);
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").stringValue("분석 결과를 가져올 수 없습니다.");
        } catch (Exception e) {
            log.error("Gemini API error", e);
            return "Gemini API 오류: " + e.getMessage();
        }
    }

    public String analyzeSignalResults(String signalName, String marketFilter,
                                       String timeframe, String conditionsJson, List<StockDto> matchedStocks) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String prompt = buildSignalResultsPrompt(signalName, marketFilter, timeframe, conditionsJson, matchedStocks);

        try {
            String responseBody = callGeminiApi(prompt);
            return extractJsonFromGeminiResponse(responseBody);
        } catch (Exception e) {
            log.error("[Gemini] Signal results analysis error: {}", e.getMessage());
            return null;
        }
    }

    private String extractJsonFromGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").stringValue("");
        String cleaned = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        objectMapper.readTree(cleaned); // JSON 유효성 검증
        return cleaned;
    }

    private String buildSignalResultsPrompt(String signalName, String marketFilter,
                                             String timeframe, String conditionsJson, List<StockDto> stocks) {
        String humanReadable = conditionsToHumanReadable(conditionsJson);

        int limit = Math.min(stocks.size(), 10);
        StringBuilder stockData = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            StockDto s = stocks.get(i);
            stockData.append(String.format(
                    "- %s(%s) [%s] | 종가: %,.0f | 등락률: %+.2f%% | 거래량: %,d | " +
                    "RSI: %.1f | MACD: %.2f | MACD Signal: %.2f | MACD Hist: %.2f | " +
                    "MA5: %,.0f | MA10: %,.0f | MA20: %,.0f | MA60: %,.0f\n",
                    s.getName() != null ? s.getName() : s.getTicker(),
                    s.getTicker(),
                    s.getMarket() != null ? s.getMarket() : "",
                    s.getClosePrice() != null ? s.getClosePrice() : 0,
                    s.getChangeRate() != null ? s.getChangeRate() : 0,
                    s.getVolume() != null ? s.getVolume() : 0,
                    s.getRsi14() != null ? s.getRsi14() : 0,
                    s.getMacd() != null ? s.getMacd() : 0,
                    s.getMacdSignal() != null ? s.getMacdSignal() : 0,
                    s.getMacdHist() != null ? s.getMacdHist() : 0,
                    s.getMa5() != null ? s.getMa5() : 0,
                    s.getMa10() != null ? s.getMa10() : 0,
                    s.getMa20() != null ? s.getMa20() : 0,
                    s.getMa60() != null ? s.getMa60() : 0));
        }

        return """
                당신은 주식 투자 전략 및 종목 분석 전문가입니다.
                시그널 조건에 매칭된 종목들을 분석하고 투자 의견을 제시해주세요.

                ## 시그널 정보
                - 이름: %s
                - 마켓: %s
                - 기준봉: %s
                - 조건:
                %s

                ## 매칭 종목 (%d개 중 상위 %d개)
                %s

                ## 요청 사항
                아래 형식으로 분석해주세요. 반드시 JSON으로만 응답하세요.

                ```json
                {
                  "strategyAssessment": {
                    "validity": "유효/보통/취약",
                    "score": 7,
                    "reasoning": "전략 타당성에 대한 1-2문장 평가"
                  },
                  "stockAnalyses": [
                    {
                      "ticker": "005930",
                      "name": "삼성전자",
                      "recommendation": "매수",
                      "targetPrice": {"low": 72000, "high": 80000},
                      "stopLoss": 65000,
                      "reasoning": "1-2문장 근거"
                    }
                  ],
                  "marketContext": "현재 시장 상황에 대한 1문장 코멘트"
                }
                ```

                ## 규칙
                - recommendation은 반드시 "매수", "매도", "관망" 중 하나
                - targetPrice는 현재가 기준 합리적 범위로 설정
                - stopLoss는 현재가 대비 하방 리스크 기준
                - 기술적 지표(RSI, MACD, MA)와 가격 추세를 근거에 반영
                - 한국 주식은 원(KRW), 미국 주식은 달러(USD) 단위로 표시
                - JSON만 출력하고 다른 텍스트는 절대 포함하지 말 것
                """.formatted(
                signalName,
                marketKorean(marketFilter),
                timeframeKorean(timeframe),
                humanReadable,
                stocks.size(), limit,
                stockData.toString());
    }

    /** CircuitBreaker + Retry로 Gemini API 호출, 응답 본문 반환 */
    private String callGeminiApi(String prompt) throws Exception {
        CheckedSupplier<String> decorated = Retry.decorateCheckedSupplier(geminiRetry,
                CircuitBreaker.decorateCheckedSupplier(geminiCircuitBreaker, () -> {
                    Map<String, Object> requestBody = Map.of(
                            "contents", List.of(Map.of(
                                    "parts", List.of(Map.of("text", prompt))
                            ))
                    );
                    String jsonBody = objectMapper.writeValueAsString(requestBody);
                    Request request = new Request.Builder()
                            .url(GEMINI_URL + "?key=" + apiKey)
                            .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                            .build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) {
                            throw new RuntimeException("Gemini API HTTP " + response.code());
                        }
                        return response.body().string();
                    }
                }));
        try {
            return decorated.get();
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private String buildPrompt(String name, String market, String timeframe, String humanReadable, String conditionsJson) {
        return """
                당신은 주식 투자 전략 분석 전문가입니다.
                아래 주식 시그널 전략을 분석하고 적합도를 평가해주세요.

                ## 시그널 정보
                - 이름: %s
                - 마켓: %s
                - 기준 봉: %s
                - 조건:
                %s
                ## 원본 조건 JSON (참고용)
                %s

                ## 참고: 연산자 설명
                - crossover: 이전 봉 미충족 → 현재 봉 충족 (상향 돌파)
                - crossunder: 이전 봉 충족 → 현재 봉 미충족 (하향 이탈)

                ## 평가 항목
                다음 항목을 한국어로 분석해주세요:

                1. **전략 논리 타당성** (이 조건들이 투자 이론상 의미 있는지)
                2. **강점** (이 전략의 장점)
                3. **위험 요소** (과최적화, 거래량 부족, 시장 편향 등)
                4. **개선 제안** (추가하면 좋을 조건이나 수정 사항)
                5. **적합도 점수** (1~10점, 10점이 최고)

                간결하고 실용적으로 분석해주세요.
                """.formatted(name, marketKorean(market), timeframeKorean(timeframe), humanReadable, conditionsJson);
    }

    private String timeframeKorean(String timeframe) {
        if (timeframe == null) return "일봉";
        return switch (timeframe) {
            case "WEEKLY" -> "주봉";
            case "MONTHLY" -> "월봉";
            default -> "일봉";
        };
    }

    private String marketKorean(String market) {
        if (market == null) return "전체";
        return switch (market) {
            case "KR" -> "한국 (KOSPI/KOSDAQ)";
            case "US" -> "미국 (NYSE/NASDAQ)";
            default -> "전체";
        };
    }

    private String conditionsToHumanReadable(String conditionsJson) {
        try {
            JsonNode root = objectMapper.readTree(conditionsJson);
            return nodeToString(root, 0);
        } catch (Exception e) {
            return conditionsJson;
        }
    }

    private String nodeToString(JsonNode node, int depth) {
        String indent = "  ".repeat(depth);
        if (node.has("conditions")) {
            String logic = node.path("logic").stringValue("AND");
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("[").append(logic).append("]\n");
            for (JsonNode child : node.get("conditions")) {
                sb.append(nodeToString(child, depth + 1));
            }
            return sb.toString();
        } else {
            String field = node.path("field").stringValue("");
            String op = node.path("operator").stringValue("");
            String right;
            String compareField = node.path("compareField").stringValue("");
            if (node.has("compareField") && !compareField.isBlank()) {
                right = fieldLabel(compareField);
            } else {
                right = String.valueOf(node.path("value").asDouble());
            }
            String opLabel = switch (op) {
                case "crossover"  -> "상향돌파";
                case "crossunder" -> "하향이탈";
                default -> op;
            };
            return indent + fieldLabel(field) + " " + opLabel + " " + right + "\n";
        }
    }

    public StockAnalysisReport analyzeStock(StockDto stock, List<StockDto> history) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String prompt = buildStockAnalysisPrompt(stock, history);
        try {
            String responseBody = callGeminiApi(prompt);
            String cleaned = extractJsonFromGeminiResponse(responseBody);
            return objectMapper.readValue(cleaned, StockAnalysisReport.class);
        } catch (Exception e) {
            log.error("[Gemini] Stock analysis error for {}: {}", stock.getTicker(), e.getMessage());
            return null;
        }
    }

    private String buildStockAnalysisPrompt(StockDto stock, List<StockDto> history) {
        String currency = (stock.getMarket() != null &&
                (stock.getMarket().equals("KOSPI") || stock.getMarket().equals("KOSDAQ"))) ? "KRW" : "USD";
        String marketLabel = (stock.getMarket() != null &&
                (stock.getMarket().equals("KOSPI") || stock.getMarket().equals("KOSDAQ"))) ? "한국" : "미국";

        // Build recent price history summary (last 20 entries from history)
        StringBuilder historyData = new StringBuilder();
        int histLimit = Math.min(history.size(), 20);
        for (int i = 0; i < histLimit; i++) {
            StockDto h = history.get(i);
            historyData.append(String.format(
                    "  %s | 시:%,.0f 고:%,.0f 저:%,.0f 종:%,.0f | 등락률:%+.2f%% | 거래량:%,d\n",
                    h.getTradeDate(),
                    h.getOpenPrice() != null ? h.getOpenPrice() : 0,
                    h.getHighPrice() != null ? h.getHighPrice() : 0,
                    h.getLowPrice() != null ? h.getLowPrice() : 0,
                    h.getClosePrice() != null ? h.getClosePrice() : 0,
                    h.getChangeRate() != null ? h.getChangeRate() : 0,
                    h.getVolume() != null ? h.getVolume() : 0));
        }

        return """
                당신은 주식 투자 분석 전문가입니다.
                아래 종목에 대한 종합 투자 분석을 수행하세요.

                ## 종목 정보
                - 종목명: %s
                - 티커: %s
                - 시장: %s (%s)
                - 기준일: %s
                - 현재가: %,.0f %s
                - 등락률: %+.2f%%
                - 거래량: %,d
                - 시가총액: %s

                ## 기술적 지표
                - MA5: %s | MA10: %s | MA20: %s | MA60: %s
                - RSI(14): %s
                - MACD: %s | Signal: %s | Histogram: %s

                ## 최근 %d일 가격 추이
                %s

                ## 분석 요청 사항
                1. **차트 분석** (technicalAnalysis): 이동평균선 배열, RSI 과매수/과매도, MACD 크로스, 지지/저항선, 추세 판단
                2. **펀더멘털** (fundamentalNote): 해당 종목의 PER, PBR, 배당수익률 등 알려진 밸류에이션 지표와 기업 실적/성장성 평가 (당신의 지식 기반)
                3. **글로벌/경제 정세** (globalContext): 해당 종목에 영향을 미치는 최근 국제 경제 흐름, 산업 동향, 정책/금리/환율 변화
                4. **리스크** (riskFactors): 투자 시 주의해야 할 위험 요소
                5. **종합 의견** (summary): 위 분석을 종합한 1-2문장 투자 판단 요약

                반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.

                ```json
                {
                  "recommendation": "매수",
                  "score": 7,
                  "targetPrice": {"low": 70000, "high": 80000},
                  "stopLoss": 62000,
                  "technicalAnalysis": "차트 분석 내용",
                  "fundamentalNote": "펀더멘털 분석 내용",
                  "globalContext": "글로벌/경제 정세 분석",
                  "riskFactors": "리스크 요인",
                  "summary": "종합 투자 의견"
                }
                ```

                ## 규칙
                - recommendation은 반드시 "매수", "매도", "관망" 중 하나
                - score는 1(매우 부정)~10(매우 긍정)
                - targetPrice는 현재가 기준 합리적 범위 (%s 단위)
                - stopLoss는 현재가 대비 하방 리스크 기준 (%s 단위)
                - 각 분석 항목은 2-4문장으로 구체적이고 실용적으로 작성
                - JSON만 출력하고 다른 텍스트는 절대 포함하지 말 것
                """.formatted(
                stock.getName() != null ? stock.getName() : stock.getTicker(),
                stock.getTicker(),
                stock.getMarket(), marketLabel,
                stock.getTradeDate(),
                stock.getClosePrice() != null ? stock.getClosePrice() : 0, currency,
                stock.getChangeRate() != null ? stock.getChangeRate() : 0,
                stock.getVolume() != null ? stock.getVolume() : 0,
                stock.getMarketCap() != null ? String.format("%,.0f", stock.getMarketCap()) : "N/A",
                formatIndicator(stock.getMa5()), formatIndicator(stock.getMa10()),
                formatIndicator(stock.getMa20()), formatIndicator(stock.getMa60()),
                formatIndicator(stock.getRsi14()),
                formatIndicator(stock.getMacd()), formatIndicator(stock.getMacdSignal()),
                formatIndicator(stock.getMacdHist()),
                histLimit, historyData.toString(),
                currency, currency);
    }

    private String formatIndicator(Double value) {
        return value != null ? String.format("%.2f", value) : "N/A";
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case "close_price" -> "종가";
            case "open_price" -> "시가";
            case "high_price" -> "고가";
            case "low_price" -> "저가";
            case "volume" -> "거래량";
            case "change_rate" -> "등락률(%)";
            case "ma5" -> "MA5";
            case "ma10" -> "MA10";
            case "ma20" -> "MA20";
            case "ma60" -> "MA60";
            case "rsi14" -> "RSI(14)";
            case "macd" -> "MACD";
            case "macd_signal" -> "MACD Signal";
            case "macd_hist" -> "MACD Hist";
            default -> field;
        };
    }
}
