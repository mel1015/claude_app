package com.stockreport.service;

import com.stockreport.domain.favorite.Favorite;
import com.stockreport.domain.favorite.FavoriteRepository;
import com.stockreport.domain.stock.Market;
import com.stockreport.dto.request.FavoriteCreateRequest;
import com.stockreport.dto.response.FavoriteDto;
import com.stockreport.dto.response.StockDto;
import com.stockreport.exception.StockNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StockService stockService;

    @Transactional(readOnly = true)
    public List<FavoriteDto> getFavorites() {
        return favoriteRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public FavoriteDto addFavorite(FavoriteCreateRequest request) {
        Market market = Market.valueOf(request.getMarket().toUpperCase());
        if (favoriteRepository.existsByTickerAndMarket(request.getTicker(), market)) {
            throw new IllegalStateException("이미 즐겨찾기에 추가된 종목입니다: " + request.getTicker());
        }
        Favorite favorite = Favorite.builder()
                .ticker(request.getTicker().toUpperCase())
                .market(market).name(request.getName())
                .build();
        return toDto(favoriteRepository.save(favorite));
    }

    @Transactional
    public void removeFavorite(String id) {
        if (!favoriteRepository.existsById(id)) {
            throw new StockNotFoundException("즐겨찾기를 찾을 수 없습니다: " + id);
        }
        favoriteRepository.deleteById(id);
    }

    @Transactional
    public void removeFavoriteByTickerAndMarket(String ticker, String market) {
        favoriteRepository.deleteByTickerAndMarket(
                ticker.toUpperCase(), Market.valueOf(market.toUpperCase()));
    }

    @Transactional(readOnly = true)
    public boolean checkFavorite(String ticker, String market) {
        return favoriteRepository.existsByTickerAndMarket(
                ticker.toUpperCase(), Market.valueOf(market.toUpperCase()));
    }

    private FavoriteDto toDto(Favorite favorite) {
        StockDto latestStock = null;
        try {
            latestStock = stockService.getLatestStock(favorite.getTicker(), favorite.getMarket());
        } catch (Exception e) {
            log.warn("Failed to get latest stock for {}: {}", favorite.getTicker(), e.getMessage());
        }
        return FavoriteDto.builder()
                .id(favorite.getId()).ticker(favorite.getTicker())
                .market(favorite.getMarket().name())
                .name(favorite.getName() != null ? favorite.getName() :
                        (latestStock != null ? latestStock.getName() : favorite.getTicker()))
                .createdAt(favorite.getCreatedAt()).latestStock(latestStock)
                .build();
    }
}
