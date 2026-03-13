package com.stockreport.domain.stock;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "stock_daily_cache")
@CompoundIndexes({
        @CompoundIndex(def = "{'ticker': 1, 'market': 1, 'tradeDate': 1, 'timeframe': 1}", unique = true),
        @CompoundIndex(def = "{'ticker': 1, 'market': 1, 'timeframe': 1}"),
        @CompoundIndex(def = "{'market': 1, 'tradeDate': 1, 'timeframe': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDailyCache {

    @Id
    private String id;

    private String ticker;

    private Market market;

    @Builder.Default
    private Timeframe timeframe = Timeframe.DAILY;

    private LocalDate tradeDate;
    private String name;

    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;

    private Long volume;
    private Double marketCap;
    private Double changeRate;

    private Double ma5;
    private Double ma10;
    private Double ma20;
    private Double ma60;
    private Double rsi14;
    private Double macd;
    private Double macdSignal;
    private Double macdHist;

    private LocalDateTime collectedAt;
}
