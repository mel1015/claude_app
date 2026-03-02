package com.stockreport.domain.news;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "news_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsCache {

    @Id
    private String id;

    private String title;

    private String summary;

    private String url;
    private String source;

    @Indexed
    private String market;

    @Indexed
    private String relatedTicker;

    @Indexed
    private LocalDateTime publishedAt;

    private LocalDateTime fetchedAt;
}
