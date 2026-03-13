package com.stockreport.scheduler;

import com.stockreport.domain.signal.Signal;
import com.stockreport.domain.signal.SignalRepository;
import com.stockreport.dto.response.StockDto;
import com.stockreport.service.SignalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SignalScheduler {

    private final SignalRepository signalRepository;
    private final SignalService signalService;

    @Async
    @EventListener(DataCollectionCompletedEvent.class)
    public void onDataCollectionCompleted(DataCollectionCompletedEvent event) {
        List<Signal> activeSignals = signalRepository.findByActiveOrderByCreatedAtDesc(true);
        if (activeSignals.isEmpty()) {
            log.debug("[시그널 자동 실행] 활성 시그널 없음 — skip");
            return;
        }
        log.info("[시그널 자동 실행] 활성 시그널 {}개 실행 시작", activeSignals.size());
        for (Signal signal : activeSignals) {
            try {
                List<StockDto> results = signalService.runSignal(signal.getId());
                log.info("[시그널 자동 실행] '{}' — 매칭 {}개", signal.getName(), results.size());
            } catch (Exception e) {
                log.error("[시그널 자동 실행] '{}' 실행 실패: {}", signal.getName(), e.getMessage());
            }
        }
        log.info("[시그널 자동 실행] 완료");
    }
}
