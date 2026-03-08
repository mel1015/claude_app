package com.stockreport.dto.request;

import com.stockreport.domain.stock.Timeframe;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignalCreateRequest {

    @NotBlank(message = "시그널 이름은 필수입니다")
    private String name;

    private String marketFilter = "ALL";

    private Timeframe timeframe = Timeframe.DAILY;

    @NotBlank(message = "조건은 필수입니다")
    private String conditions;

    private boolean active = true;
}
