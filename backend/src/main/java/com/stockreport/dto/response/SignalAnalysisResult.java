package com.stockreport.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalAnalysisResult {

    private StrategyAssessment strategyAssessment;
    private List<StockAnalysis> stockAnalyses;
    private String marketContext;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StrategyAssessment {
        private String validity;
        private int score;
        private String reasoning;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockAnalysis {
        private String ticker;
        private String name;
        private String recommendation;
        private TargetPrice targetPrice;
        private Double stopLoss;
        private String reasoning;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TargetPrice {
        private Double low;
        private Double high;
    }
}
