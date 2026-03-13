package com.stockreport.service;

import com.stockreport.dto.response.StockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Slf4j
public class SlackNotificationService {

    private final String webhookUrl;
    private final RestClient restClient;

    public SlackNotificationService(@Value("${slack.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.create();
    }

    public void sendSignalAlert(String signalName, String marketFilter, String timeframe, List<StockDto> results) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("[Slack] SLACK_WEBHOOK_URL 미설정 — 알림 skip (시그널: {}, 매칭: {}개)", signalName, results.size());
            return;
        }

        String message = buildMessage(signalName, marketFilter, timeframe, results);
        String payload = "{\"text\": " + escapeJson(message) + "}";

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Slack] 알림 발송 완료 — 시그널: {}, 매칭: {}개", signalName, results.size());
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
