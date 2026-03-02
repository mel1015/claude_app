package com.stockreport.dto.request;

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

    @NotBlank(message = "조건은 필수입니다")
    private String conditions;

    private boolean active = true;
}
