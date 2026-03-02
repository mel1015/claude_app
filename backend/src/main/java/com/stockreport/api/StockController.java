package com.stockreport.api;

import com.stockreport.dto.response.StockDto;
import com.stockreport.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @GetMapping
    public ResponseEntity<?> getStocks(
            @RequestParam(defaultValue = "ALL") String market,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable) {
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
}
