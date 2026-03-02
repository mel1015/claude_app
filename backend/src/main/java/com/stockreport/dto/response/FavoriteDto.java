package com.stockreport.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteDto {
    private String id;
    private String ticker;
    private String market;
    private String name;
    private LocalDateTime createdAt;
    private StockDto latestStock;
}
