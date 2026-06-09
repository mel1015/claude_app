package com.stockreport.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalServiceTest {

    // 비정상 대량 매칭 판정 (PR #25 sanity guard) — 50건 이상 & 평가 대상의 20% 초과

    @Test
    void abnormal_whenManyAndHighRatio() {
        assertTrue(SignalService.isMatchRatioAbnormal(300, 600)); // 50%
    }

    @Test
    void normal_whenFewMatches() {
        assertFalse(SignalService.isMatchRatioAbnormal(5, 600));
    }

    @Test
    void normal_whenBelowAbsoluteFloor() {
        assertFalse(SignalService.isMatchRatioAbnormal(49, 50)); // 98%지만 절대 건수 50 미만
    }

    @Test
    void normal_whenRatioExactlyAtThreshold() {
        assertFalse(SignalService.isMatchRatioAbnormal(50, 250)); // 정확히 20% → 초과 아님
    }

    @Test
    void abnormal_whenJustOverThreshold() {
        assertTrue(SignalService.isMatchRatioAbnormal(51, 250)); // 20.4%
    }

    @Test
    void normal_whenZeroTotal() {
        assertFalse(SignalService.isMatchRatioAbnormal(100, 0));
    }
}
