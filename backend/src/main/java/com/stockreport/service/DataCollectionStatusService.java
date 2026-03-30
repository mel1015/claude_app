package com.stockreport.service;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DataCollectionStatusService {

    public enum Status { IDLE, COLLECTING, COMPLETED, FAILED }

    private volatile Status status = Status.IDLE;
    private volatile String message = "";
    private volatile Instant lastUpdated = Instant.now();

    // KR/US 시장별 수집 상태 (병렬 수집 진행률 추적)
    private volatile String krStatus = "IDLE";
    private volatile String usStatus = "IDLE";

    public void start(String message) {
        this.status = Status.COLLECTING;
        this.message = message;
        this.lastUpdated = Instant.now();
    }

    public void complete(String message) {
        this.status = Status.COMPLETED;
        this.message = message;
        this.lastUpdated = Instant.now();
    }

    public void fail(String message) {
        this.status = Status.FAILED;
        this.message = message;
        this.lastUpdated = Instant.now();
    }

    public void startKr() { this.krStatus = "COLLECTING"; }
    public void completeKr() { this.krStatus = "DONE"; }
    public void failKr() { this.krStatus = "FAILED"; }
    public void startUs() { this.usStatus = "COLLECTING"; }
    public void completeUs() { this.usStatus = "DONE"; }
    public void failUs() { this.usStatus = "FAILED"; }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getLastUpdated() { return lastUpdated; }
    public boolean isCollecting() { return status == Status.COLLECTING; }
    public String getKrStatus() { return krStatus; }
    public String getUsStatus() { return usStatus; }
}
