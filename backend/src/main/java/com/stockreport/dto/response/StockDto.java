package com.stockreport.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockDto {
    private String id;
    private String ticker;
    private String market;
    private String name;
    private LocalDate tradeDate;
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
    private Boolean isFavorite;
}
