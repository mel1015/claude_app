package com.stockreport.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalDto {
    private String id;
    private String name;
    private String marketFilter;
    private JsonNode conditions;
    private boolean active;
    private LocalDateTime lastRunAt;
    private List<StockDto> lastResults;
    private LocalDateTime createdAt;
}
