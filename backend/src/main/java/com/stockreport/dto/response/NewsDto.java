package com.stockreport.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDto {
    private String id;
    private String title;
    private String summary;
    private String url;
    private String source;
    private String market;
    private String relatedTicker;
    private LocalDateTime publishedAt;
}
