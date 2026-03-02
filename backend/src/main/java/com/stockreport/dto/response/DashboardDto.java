package com.stockreport.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {
    private LocalDate tradeDate;
    private List<StockDto> favorites;
    private List<StockDto> topVolumeKr;
    private List<StockDto> topVolumeUs;
    private List<NewsDto> latestNews;
    private List<SignalDto> activeSignals;
    private MarketSummary krSummary;
    private MarketSummary usSummary;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarketSummary {
        private String market;
        private int totalStocks;
        private int risingStocks;
        private int fallingStocks;
        private double avgChangeRate;
    }
}
