package com.stockreport.domain.signal;

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

    private String conditions;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime lastRunAt;

    private String lastResult;

    @CreatedDate
    private LocalDateTime createdAt;
}
