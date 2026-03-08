package com.stockreport.dto.request;

import com.stockreport.domain.stock.Timeframe;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignalUpdateRequest {
    private String name;
    private String marketFilter;
    private Timeframe timeframe;
    private String conditions;
    private Boolean active;
}
