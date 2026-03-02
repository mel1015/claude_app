package com.stockreport.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignalUpdateRequest {
    private String name;
    private String marketFilter;
    private String conditions;
    private Boolean active;
}
