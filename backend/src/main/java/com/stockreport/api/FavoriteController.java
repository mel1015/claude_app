package com.stockreport.api;

import com.stockreport.dto.request.FavoriteCreateRequest;
import com.stockreport.dto.response.FavoriteDto;
import com.stockreport.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<?> getFavorites() {
        List<FavoriteDto> favorites = favoriteService.getFavorites();
        return ResponseEntity.ok(Map.of("data", favorites, "meta", Map.of("timestamp", Instant.now())));
    }

    @PostMapping
    public ResponseEntity<?> addFavorite(@Valid @RequestBody FavoriteCreateRequest request) {
        FavoriteDto favorite = favoriteService.addFavorite(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("data", favorite, "meta", Map.of("timestamp", Instant.now())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFavorite(@PathVariable String id) {
        favoriteService.removeFavorite(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-ticker")
    public ResponseEntity<Void> removeFavoriteByTicker(
            @RequestParam String ticker, @RequestParam String market) {
        favoriteService.removeFavoriteByTickerAndMarket(ticker, market);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkFavorite(@RequestParam String ticker, @RequestParam String market) {
        boolean isFavorite = favoriteService.checkFavorite(ticker, market);
        return ResponseEntity.ok(Map.of(
                "data", Map.of("isFavorite", isFavorite),
                "meta", Map.of("timestamp", Instant.now())));
    }
}
