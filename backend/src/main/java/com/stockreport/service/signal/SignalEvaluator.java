package com.stockreport.service.signal;

import tools.jackson.databind.JsonNode;
import com.stockreport.domain.stock.StockDailyCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class SignalEvaluator {

    public boolean evaluate(JsonNode node, Map<String, Double> stock) {
        return evaluate(node, stock, null);
    }

    public boolean evaluate(JsonNode node, Map<String, Double> current, Map<String, Double> prev) {
        if (node == null) return false;
        if (node.has("conditions")) {
            String logic = node.path("logic").asText("AND");
            List<Boolean> results = StreamSupport
                    .stream(node.get("conditions").spliterator(), false)
                    .map(c -> evaluate(c, current, prev))
                    .toList();
            return "AND".equals(logic)
                    ? results.stream().allMatch(r -> r)
                    : results.stream().anyMatch(r -> r);
        } else {
            String field = node.path("field").asText();
            String operator = node.path("operator").asText();
            double val = current.getOrDefault(field, 0.0);
            double cmp = node.has("compareField")
                    ? current.getOrDefault(node.path("compareField").asText(), 0.0)
                    : node.path("value").asDouble();
            return switch (operator) {
                case ">"  -> val > cmp;
                case ">=" -> val >= cmp;
                case "<"  -> val < cmp;
                case "<=" -> val <= cmp;
                case "==" -> Math.abs(val - cmp) < 0.0001;
                case "!=" -> Math.abs(val - cmp) >= 0.0001;
                case "crossover" -> {
                    if (prev == null) yield false;
                    double prevVal = prev.getOrDefault(field, 0.0);
                    double prevCmp = node.has("compareField")
                            ? prev.getOrDefault(node.path("compareField").asText(), 0.0)
                            : node.path("value").asDouble();
                    yield prevVal <= prevCmp && val > cmp;
                }
                case "crossunder" -> {
                    if (prev == null) yield false;
                    double prevVal = prev.getOrDefault(field, 0.0);
                    double prevCmp = node.has("compareField")
                            ? prev.getOrDefault(node.path("compareField").asText(), 0.0)
                            : node.path("value").asDouble();
                    yield prevVal >= prevCmp && val < cmp;
                }
                default -> { log.warn("Unknown operator: {}", operator); yield false; }
            };
        }
    }

    public Map<String, Double> stockToMap(StockDailyCache stock) {
        Map<String, Double> map = new HashMap<>();
        if (stock.getClosePrice() != null)  map.put("close_price",  stock.getClosePrice());
        if (stock.getOpenPrice() != null)   map.put("open_price",   stock.getOpenPrice());
        if (stock.getHighPrice() != null)   map.put("high_price",   stock.getHighPrice());
        if (stock.getLowPrice() != null)    map.put("low_price",    stock.getLowPrice());
        if (stock.getVolume() != null)      map.put("volume",       stock.getVolume().doubleValue());
        if (stock.getChangeRate() != null)  map.put("change_rate",  stock.getChangeRate());
        if (stock.getMa5() != null)         map.put("ma5",          stock.getMa5());
        if (stock.getMa10() != null)        map.put("ma10",         stock.getMa10());
        if (stock.getMa20() != null)        map.put("ma20",         stock.getMa20());
        if (stock.getMa60() != null)        map.put("ma60",         stock.getMa60());
        if (stock.getRsi14() != null)       map.put("rsi14",        stock.getRsi14());
        if (stock.getMacd() != null)        map.put("macd",         stock.getMacd());
        if (stock.getMacdSignal() != null)  map.put("macd_signal",  stock.getMacdSignal());
        if (stock.getMacdHist() != null)    map.put("macd_hist",    stock.getMacdHist());
        return map;
    }
}
