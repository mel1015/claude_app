package com.stockreport.api;

import com.stockreport.dto.request.SignalCreateRequest;
import com.stockreport.dto.request.SignalUpdateRequest;
import com.stockreport.dto.response.StockDto;
import com.stockreport.service.SignalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalService signalService;

    @GetMapping
    public ResponseEntity<?> getSignals() {
        return ResponseEntity.ok(Map.of("data", signalService.getSignals(), "meta", Map.of("timestamp", Instant.now())));
    }

    @PostMapping
    public ResponseEntity<?> createSignal(@Valid @RequestBody SignalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("data", signalService.createSignal(request), "meta", Map.of("timestamp", Instant.now())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSignal(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("data", signalService.getSignal(id), "meta", Map.of("timestamp", Instant.now())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSignal(@PathVariable String id, @RequestBody SignalUpdateRequest request) {
        return ResponseEntity.ok(Map.of("data", signalService.updateSignal(id, request), "meta", Map.of("timestamp", Instant.now())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSignal(@PathVariable String id) {
        signalService.deleteSignal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<?> runSignal(@PathVariable String id) {
        List<StockDto> results = signalService.runSignal(id);
        return ResponseEntity.ok(Map.of("data", results, "meta", Map.of("timestamp", Instant.now(), "count", results.size())));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<?> getSignalResults(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("data", signalService.getSignalResults(id), "meta", Map.of("timestamp", Instant.now())));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateConditions(@RequestBody Map<String, String> body) {
        signalService.validateConditions(body.get("conditions"));
        return ResponseEntity.ok(Map.of("data", Map.of("valid", true), "meta", Map.of("timestamp", Instant.now())));
    }
}
