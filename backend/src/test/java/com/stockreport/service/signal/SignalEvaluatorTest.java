package com.stockreport.service.signal;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalEvaluatorTest {

    private final SignalEvaluator evaluator = new SignalEvaluator();
    private final JsonMapper mapper = JsonMapper.builder().build();

    private JsonNode cond(String json) {
        return mapper.readTree(json);
    }

    private Map<String, Double> map(Object... kv) {
        Map<String, Double> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Double) kv[i + 1]);
        return m;
    }

    // ── null 지표 가드 (PR #24 회귀 방지) ─────────────────────────────

    @Test
    void closeGtMa60_whenMa60Missing_returnsFalse() {
        JsonNode node = cond("{\"field\":\"close_price\",\"operator\":\">\",\"compareField\":\"ma60\"}");
        Map<String, Double> current = map("close_price", 1000.0); // ma60 미계산
        assertFalse(evaluator.evaluate(node, current, null),
                "ma60이 null이면 0 폴백으로 close > 0 이 항상 참이 되면 안 됨");
    }

    @Test
    void closeGtMa60_normal_returnsTrue() {
        JsonNode node = cond("{\"field\":\"close_price\",\"operator\":\">\",\"compareField\":\"ma60\"}");
        assertTrue(evaluator.evaluate(node, map("close_price", 1000.0, "ma60", 800.0), null));
    }

    // ── crossover (골든크로스) ────────────────────────────────────────

    @Test
    void crossover_whenPrevNull_returnsFalse() {
        JsonNode node = cond("{\"field\":\"ma5\",\"operator\":\"crossover\",\"compareField\":\"ma20\"}");
        assertFalse(evaluator.evaluate(node, map("ma5", 110.0, "ma20", 100.0), null));
    }

    @Test
    void crossover_whenRealCross_returnsTrue() {
        JsonNode node = cond("{\"field\":\"ma5\",\"operator\":\"crossover\",\"compareField\":\"ma20\"}");
        Map<String, Double> current = map("ma5", 110.0, "ma20", 100.0); // 현재 ma5 > ma20
        Map<String, Double> prev = map("ma5", 95.0, "ma20", 100.0);     // 직전 ma5 <= ma20
        assertTrue(evaluator.evaluate(node, current, prev));
    }

    @Test
    void crossover_whenAlreadyAbove_returnsFalse() {
        JsonNode node = cond("{\"field\":\"ma5\",\"operator\":\"crossover\",\"compareField\":\"ma20\"}");
        Map<String, Double> current = map("ma5", 110.0, "ma20", 100.0);
        Map<String, Double> prev = map("ma5", 105.0, "ma20", 100.0);    // 직전에 이미 ma5 > ma20 (돌파 아님)
        assertFalse(evaluator.evaluate(node, current, prev));
    }

    @Test
    void crossover_whenPrevCompareFieldMissing_returnsFalse() {
        JsonNode node = cond("{\"field\":\"ma5\",\"operator\":\"crossover\",\"compareField\":\"ma20\"}");
        Map<String, Double> current = map("ma5", 110.0, "ma20", 100.0);
        Map<String, Double> prev = map("ma5", 95.0); // 직전 ma20 미계산
        assertFalse(evaluator.evaluate(node, current, prev),
                "prev ma20이 null이면 0 폴백으로 거짓 crossover가 성립하면 안 됨");
    }

    // ── AND 그룹 (골든크로스 & 추세강화 전략 형태) ────────────────────

    @Test
    void andGroup_whenIndicatorMissing_returnsFalse() {
        JsonNode node = cond("{\"logic\":\"AND\",\"conditions\":["
                + "{\"field\":\"ma5\",\"operator\":\"crossover\",\"compareField\":\"ma20\"},"
                + "{\"field\":\"close_price\",\"operator\":\">\",\"compareField\":\"ma60\"}]}");
        // 현재 ma5>ma20 이지만 ma60 미계산 → close>ma60 가드로 그룹 전체 false
        Map<String, Double> current = map("ma5", 110.0, "ma20", 100.0, "close_price", 1000.0);
        Map<String, Double> prev = map("ma5", 95.0, "ma20", 100.0);
        assertFalse(evaluator.evaluate(node, current, prev));
    }
}
