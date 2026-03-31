package com.stockreport.api;

import com.stockreport.dto.response.StockAnalysisReport;
import com.stockreport.dto.response.StockDto;
import com.stockreport.service.GeminiService;
import com.stockreport.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final GeminiService geminiService;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<?> getStocks(
            @RequestParam(defaultValue = "ALL") String market,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "volume", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StockDto> stocks = stockService.getStocks(market, query, pageable);
        return ResponseEntity.ok(Map.of(
                "data", stocks.getContent(),
                "meta", Map.of("page", stocks.getNumber(), "size", stocks.getSize(),
                        "totalElements", stocks.getTotalElements(), "totalPages", stocks.getTotalPages(),
                        "timestamp", Instant.now())));
    }

    @GetMapping("/{market}/{ticker}")
    public ResponseEntity<?> getStock(@PathVariable String market, @PathVariable String ticker) {
        return ResponseEntity.ok(Map.of(
                "data", stockService.getStock(market, ticker),
                "meta", Map.of("timestamp", Instant.now())));
    }

    @GetMapping("/{market}/{ticker}/history")
    public ResponseEntity<?> getHistory(
            @PathVariable String market, @PathVariable String ticker,
            @RequestParam(defaultValue = "90") int days) {
        List<StockDto> history = stockService.getStockHistory(market, ticker, days);
        return ResponseEntity.ok(Map.of(
                "data", history,
                "meta", Map.of("timestamp", Instant.now(), "days", days)));
    }

    @GetMapping("/top-volume")
    public ResponseEntity<?> getTopVolume(@RequestParam(defaultValue = "ALL") String market) {
        return ResponseEntity.ok(Map.of(
                "data", stockService.getTopVolume(market),
                "meta", Map.of("timestamp", Instant.now())));
    }

    @PostMapping("/{market}/{ticker}/analyze")
    public ResponseEntity<?> analyzeStock(@PathVariable String market, @PathVariable String ticker) {
        StockDto stock = stockService.getStock(market, ticker);
        List<StockDto> history = stockService.getStockHistory(market, ticker, 30);

        String analysisJson = geminiService.analyzeStock(stock, history);
        if (analysisJson == null) {
            return ResponseEntity.ok(Map.of(
                    "data", Map.of(),
                    "meta", Map.of("timestamp", Instant.now(), "error", "AI 분석을 수행할 수 없습니다")));
        }

        try {
            StockAnalysisReport report = objectMapper.readValue(analysisJson, StockAnalysisReport.class);
            return ResponseEntity.ok(Map.of(
                    "data", report,
                    "meta", Map.of("timestamp", Instant.now())));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "data", Map.of(),
                    "meta", Map.of("timestamp", Instant.now(), "error", "분석 결과 파싱 실패")));
        }
    }
}
