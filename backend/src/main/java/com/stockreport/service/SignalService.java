package com.stockreport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockreport.domain.signal.Signal;
import com.stockreport.domain.signal.SignalRepository;
import com.stockreport.domain.stock.Market;
import com.stockreport.domain.stock.StockDailyCache;
import com.stockreport.domain.stock.StockDailyCacheRepository;
import com.stockreport.domain.stock.Timeframe;
import com.stockreport.dto.response.ParseTextResult;
import com.stockreport.dto.request.SignalCreateRequest;
import com.stockreport.dto.request.SignalUpdateRequest;
import com.stockreport.dto.response.SignalDto;
import com.stockreport.dto.response.StockDto;
import com.stockreport.exception.StockNotFoundException;
import com.stockreport.service.signal.SignalEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignalService {

    private final SignalRepository signalRepository;
    private final StockDailyCacheRepository stockDailyCacheRepository;
    private final SignalEvaluator signalEvaluator;
    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final GeminiService geminiService;

    public List<SignalDto> getSignals() {
        return signalRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    public SignalDto getSignal(String id) {
        return signalRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new StockNotFoundException("시그널을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public SignalDto createSignal(SignalCreateRequest request) {
        validateConditions(request.getConditions());
        Signal signal = Signal.builder()
                .name(request.getName()).marketFilter(request.getMarketFilter())
                .timeframe(request.getTimeframe() != null ? request.getTimeframe() : Timeframe.DAILY)
                .conditions(request.getConditions()).active(request.isActive())
                .build();
        return toDto(signalRepository.save(signal));
    }

    @Transactional
    public SignalDto updateSignal(String id, SignalUpdateRequest request) {
        Signal signal = signalRepository.findById(id)
                .orElseThrow(() -> new StockNotFoundException("시그널을 찾을 수 없습니다: " + id));
        if (request.getName() != null) signal.setName(request.getName());
        if (request.getMarketFilter() != null) signal.setMarketFilter(request.getMarketFilter());
        if (request.getTimeframe() != null) signal.setTimeframe(request.getTimeframe());
        if (request.getConditions() != null) {
            validateConditions(request.getConditions());
            signal.setConditions(request.getConditions());
        }
        if (request.getActive() != null) signal.setActive(request.getActive());
        return toDto(signalRepository.save(signal));
    }

    @Transactional
    public void deleteSignal(String id) {
        if (!signalRepository.existsById(id)) throw new StockNotFoundException("시그널을 찾을 수 없습니다: " + id);
        signalRepository.deleteById(id);
    }

    @Transactional
    public List<StockDto> runSignal(String id) {
        Signal signal = signalRepository.findById(id)
                .orElseThrow(() -> new StockNotFoundException("시그널을 찾을 수 없습니다: " + id));
        List<StockDto> results = executeSignal(signal);
        signal.setLastRunAt(LocalDateTime.now());
        try { signal.setLastResult(objectMapper.writeValueAsString(results)); }
        catch (JsonProcessingException e) { log.warn("Failed to serialize results"); }
        signalRepository.save(signal);
        return results;
    }

    public List<StockDto> getSignalResults(String id) {
        Signal signal = signalRepository.findById(id)
                .orElseThrow(() -> new StockNotFoundException("시그널을 찾을 수 없습니다: " + id));
        if (signal.getLastResult() == null) return List.of();
        try {
            return objectMapper.readValue(signal.getLastResult(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StockDto.class));
        } catch (JsonProcessingException e) { return List.of(); }
    }

    public String analyzeWithGemini(String name, String marketFilter, String timeframe, String conditions) {
        return geminiService.analyzeSignalStrategy(name, marketFilter, timeframe, conditions);
    }

    public ParseTextResult parseTextToConditions(String text) {
        ParseTextResult result = geminiService.parseTextToConditions(text);
        if (result == null || result.conditions() == null || result.conditions().isBlank()) {
            throw new IllegalArgumentException("조건 생성에 실패했습니다. 다시 시도해주세요.");
        }
        validateConditions(result.conditions());
        return result;
    }

    public boolean validateConditions(String conditionsJson) {
        try {
            validateNode(objectMapper.readTree(conditionsJson));
            return true;
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 조건 JSON: " + e.getMessage());
        }
    }

    private List<StockDto> executeSignal(Signal signal) {
        try {
            JsonNode conditions = objectMapper.readTree(signal.getConditions());
            List<Market> markets = new ArrayList<>();
            String mf = signal.getMarketFilter();
            if (mf == null || mf.equals("ALL")) markets.addAll(List.of(Market.KOSPI, Market.KOSDAQ, Market.NYSE, Market.NASDAQ));
            else if (mf.equals("KR")) markets.addAll(List.of(Market.KOSPI, Market.KOSDAQ));
            else if (mf.equals("US")) markets.addAll(List.of(Market.NYSE, Market.NASDAQ));

            List<StockDto> matched = new ArrayList<>();
            Timeframe timeframe = signal.getTimeframe() != null ? signal.getTimeframe() : Timeframe.DAILY;
            for (Market market : markets) {
                LocalDate latestDate = stockDailyCacheRepository
                        .findFirstByMarketAndTimeframeOrderByTradeDateDesc(market, timeframe)
                        .map(StockDailyCache::getTradeDate).orElse(LocalDate.now());

                // 크로스오버 조건을 위한 이전 봉 데이터 (ticker → stockMap)
                LocalDate prevDate = stockDailyCacheRepository
                        .findFirstByMarketAndTimeframeAndTradeDateBeforeOrderByTradeDateDesc(market, timeframe, latestDate)
                        .map(StockDailyCache::getTradeDate).orElse(null);
                Map<String, Map<String, Double>> prevStocksMap = prevDate != null
                        ? stockDailyCacheRepository.findByMarketAndTradeDateAndTimeframeOrderByVolumeDesc(
                                market, prevDate, timeframe, PageRequest.of(0, Integer.MAX_VALUE))
                                .stream().collect(Collectors.toMap(StockDailyCache::getTicker, signalEvaluator::stockToMap))
                        : Map.of();

                List<StockDailyCache> stocks = stockDailyCacheRepository
                        .findByMarketAndTradeDateAndTimeframeOrderByVolumeDesc(market, latestDate, timeframe, PageRequest.of(0, Integer.MAX_VALUE));
                for (StockDailyCache stock : stocks) {
                    Map<String, Double> currentMap = signalEvaluator.stockToMap(stock);
                    Map<String, Double> prevMap = prevStocksMap.get(stock.getTicker());
                    if (signalEvaluator.evaluate(conditions, currentMap, prevMap)) matched.add(stockService.toDto(stock));
                }
            }
            return matched;
        } catch (Exception e) {
            log.error("Failed to execute signal {}: {}", signal.getId(), e.getMessage(), e);
            return List.of();
        }
    }

    private void validateNode(JsonNode node) {
        if (node.has("conditions")) {
            if (!node.has("logic")) throw new IllegalArgumentException("그룹 노드에 logic 필드가 없습니다");
            for (JsonNode child : node.get("conditions")) validateNode(child);
        } else {
            if (!node.has("field")) throw new IllegalArgumentException("리프 노드에 field가 없습니다");
            if (!node.has("operator")) throw new IllegalArgumentException("리프 노드에 operator가 없습니다");
            if (!node.has("value") && !node.has("compareField")) throw new IllegalArgumentException("리프 노드에 value 또는 compareField가 없습니다");
        }
    }

    private SignalDto toDto(Signal signal) {
        List<StockDto> lastResults = null;
        if (signal.getLastResult() != null) {
            try { lastResults = objectMapper.readValue(signal.getLastResult(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StockDto.class)); }
            catch (Exception e) { lastResults = List.of(); }
        }
        JsonNode conditionsNode = null;
        try { conditionsNode = objectMapper.readTree(signal.getConditions()); }
        catch (Exception e) { log.warn("Failed to parse signal conditions"); }

        return SignalDto.builder()
                .id(signal.getId()).name(signal.getName()).marketFilter(signal.getMarketFilter())
                .timeframe(signal.getTimeframe() != null ? signal.getTimeframe() : Timeframe.DAILY)
                .conditions(conditionsNode).active(signal.isActive())
                .lastRunAt(signal.getLastRunAt()).lastResults(lastResults).createdAt(signal.getCreatedAt())
                .build();
    }
}
