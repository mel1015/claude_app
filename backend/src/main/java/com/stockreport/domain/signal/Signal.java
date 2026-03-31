package com.stockreport.domain.signal;

import com.stockreport.domain.stock.Timeframe;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "signals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signal {

    @Id
    private String id;

    private String name;

    private String marketFilter;

    @Builder.Default
    private Timeframe timeframe = Timeframe.DAILY;

    private String conditions;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime lastRunAt;

    private String lastResult;

    private String lastAnalysis;

    @CreatedDate
    private LocalDateTime createdAt;
}
