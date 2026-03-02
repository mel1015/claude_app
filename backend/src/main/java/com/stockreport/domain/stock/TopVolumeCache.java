package com.stockreport.domain.stock;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "top_volume_cache")
@CompoundIndex(def = "{'market': 1, 'tradeDate': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopVolumeCache {

    @Id
    private String id;

    private Market market;

    private LocalDate tradeDate;
    private int rank;
    private String ticker;
    private String name;
    private Long volume;
    private Double closePrice;
    private Double changeRate;
}
