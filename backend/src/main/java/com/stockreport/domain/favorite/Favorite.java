package com.stockreport.domain.favorite;

import com.stockreport.domain.stock.Market;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "favorites")
@CompoundIndex(def = "{'ticker': 1, 'market': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {

    @Id
    private String id;

    private String ticker;

    private Market market;

    private String name;

    @CreatedDate
    private LocalDateTime createdAt;
}
