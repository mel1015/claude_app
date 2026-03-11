package com.stockreport.service;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DataCollectionStatusService {

    public enum Status { IDLE, COLLECTING, COMPLETED, FAILED }

    private volatile Status status = Status.IDLE;
    private volatile String message = "";
    private volatile Instant lastUpdated = Instant.now();

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

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getLastUpdated() { return lastUpdated; }
    public boolean isCollecting() { return status == Status.COLLECTING; }
}
