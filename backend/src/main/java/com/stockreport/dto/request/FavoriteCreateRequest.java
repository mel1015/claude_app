package com.stockreport.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCreateRequest {

    @NotBlank(message = "티커는 필수입니다")
    private String ticker;

    @NotBlank(message = "마켓은 필수입니다")
    private String market;

    private String name;
}
