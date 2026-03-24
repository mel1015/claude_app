package com.stockreport.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.stockreport.domain.stock.Timeframe;
import com.stockreport.dto.response.ParseTextResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
                if (!response.isSuccessful() || response.body() == null) return null;
                String responseBody = response.body().string();
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
                    ((tools.jackson.databind.node.ObjectNode) resultNode).remove("timeframe");
                }
                return new ParseTextResult(objectMapper.writeValueAsString(resultNode), timeframe);
            }
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
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "(no body)";
                    log.error("Gemini API error {}: {}", response.code(), errBody);
                    try {
                        JsonNode errJson = objectMapper.readTree(errBody);
                        String msg = errJson.path("error").path("message").stringValue("");
                        return "Gemini API 오류 (HTTP " + response.code() + "): " + (msg.isBlank() ? errBody : msg);
                    } catch (Exception ignored) {
                        return "Gemini API 호출 실패: HTTP " + response.code() + " - " + errBody;
                    }
                }
                if (response.body() == null) {
                    return "Gemini API 호출 실패: 응답 본문 없음";
                }
                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                return root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").stringValue("분석 결과를 가져올 수 없습니다.");
            }
        } catch (Exception e) {
            log.error("Gemini API error", e);
            return "Gemini API 오류: " + e.getMessage();
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
