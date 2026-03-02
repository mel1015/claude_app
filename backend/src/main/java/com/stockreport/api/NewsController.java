package com.stockreport.api;

import com.stockreport.dto.response.NewsDto;
import com.stockreport.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<?> getNews(
            @RequestParam(defaultValue = "ALL") String market,
            @RequestParam(required = false) String ticker,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NewsDto> news = newsService.getNews(market, ticker, pageable);
        return ResponseEntity.ok(Map.of(
                "data", news.getContent(),
                "meta", Map.of("page", news.getNumber(), "size", news.getSize(),
                        "totalElements", news.getTotalElements(), "totalPages", news.getTotalPages(),
                        "timestamp", Instant.now())));
    }
}
