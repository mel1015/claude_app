package com.stockreport.service;

import com.stockreport.dto.response.SignalAnalysisResult;
import com.stockreport.dto.response.StockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Slf4j
public class SlackNotificationService {

    private final String webhookUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SlackNotificationService(@Value("${slack.webhook-url:}") String webhookUrl, ObjectMapper objectMapper) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    public void sendSignalAlert(String signalName, String marketFilter, String timeframe, List<StockDto> results) {
        sendSignalAlert(signalName, marketFilter, timeframe, results, null);
    }

    public void sendSignalAlert(String signalName, String marketFilter, String timeframe,
                                 List<StockDto> results, String analysisJson) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("[Slack] SLACK_WEBHOOK_URL 미설정 — 알림 skip (시그널: {}, 매칭: {}개)", signalName, results.size());
            return;
        }

        String message = buildMessage(signalName, marketFilter, timeframe, results);

        if (analysisJson != null) {
            message += buildAnalysisSection(analysisJson);
        }

        if (message.length() > 3900) {
            message = message.substring(0, 3900) + "\n...(분석 결과가 잘렸습니다)";
        }

        String payload = "{\"text\": " + escapeJson(message) + "}";

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Slack] 알림 발송 완료 — 시그널: {}, 매칭: {}개, AI분석: {}", signalName, results.size(), analysisJson != null);
        } catch (Exception e) {
            log.error("[Slack] 알림 발송 실패 — 시그널: {}, 오류: {}", signalName, e.getMessage());
        }
    }

    private String buildMessage(String signalName, String marketFilter, String timeframe, List<StockDto> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("*[").append(signalName).append("]* 조건 충족 종목 발생!\n");
        sb.append("마켓: ").append(marketFilter != null ? marketFilter : "ALL");
        sb.append(" | 기준봉: ").append(timeframe);
        sb.append(" | 종목 수: ").append(results.size()).append("개\n\n");

        int limit = Math.min(results.size(), 20);
        for (int i = 0; i < limit; i++) {
            StockDto s = results.get(i);
            sb.append("• ").append(s.getName() != null ? s.getName() : s.getTicker());
            sb.append(" (").append(s.getTicker()).append(")");
            sb.append(" - ").append(s.getMarket() != null ? s.getMarket() : "");
            if (s.getChangeRate() != null) {
                sb.append(String.format(" | %+.2f%%", s.getChangeRate()));
            }
            sb.append("\n");
        }
        if (results.size() > 20) {
            sb.append("...외 ").append(results.size() - 20).append("개\n");
        }

        return sb.toString();
    }

    private String buildAnalysisSection(String analysisJson) {
        try {
            SignalAnalysisResult analysis = objectMapper.readValue(analysisJson, SignalAnalysisResult.class);
            StringBuilder sb = new StringBuilder();
            sb.append("\n---\n");
            sb.append("*AI 분석 리포트*\n\n");

            if (analysis.getStrategyAssessment() != null) {
                var sa = analysis.getStrategyAssessment();
                sb.append(String.format("*전략 평가:* %s (%d/10)\n", sa.getValidity(), sa.getScore()));
                sb.append(sa.getReasoning()).append("\n\n");
            }

            if (analysis.getMarketContext() != null) {
                sb.append("*시장 상황:* ").append(analysis.getMarketContext()).append("\n\n");
            }

            if (analysis.getStockAnalyses() != null && !analysis.getStockAnalyses().isEmpty()) {
                sb.append("*종목별 의견:*\n");
                for (var stock : analysis.getStockAnalyses()) {
                    String emoji = switch (stock.getRecommendation()) {
                        case "매수" -> "🟢";
                        case "매도" -> "🔴";
                        default -> "🟡";
                    };
                    sb.append(String.format("%s *%s*(%s): *%s*",
                            emoji, stock.getName(), stock.getTicker(), stock.getRecommendation()));
                    if (stock.getTargetPrice() != null) {
                        sb.append(String.format(" | 목표: %,.0f~%,.0f",
                                stock.getTargetPrice().getLow(), stock.getTargetPrice().getHigh()));
                    }
                    if (stock.getStopLoss() != null) {
                        sb.append(String.format(" | 손절: %,.0f", stock.getStopLoss()));
                    }
                    sb.append("\n  └ ").append(stock.getReasoning()).append("\n");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            log.warn("[Slack] AI 분석 결과 파싱 실패: {}", e.getMessage());
            return "\n---\n_AI 분석 결과 파싱 실패_\n";
        }
    }

    private String escapeJson(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
