package com.stockreport.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAnalysisReport {
    private String recommendation;   // "매수" / "매도" / "관망"
    private int score;               // 1-10
    private TargetPrice targetPrice;
    private Double stopLoss;
    private String technicalAnalysis; // chart-based analysis
    private String fundamentalNote;   // PER, PBR, etc.
    private String globalContext;     // international/economic context
    private String riskFactors;
    private String summary;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetPrice {
        private Double low;
        private Double high;
    }
}
