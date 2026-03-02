package com.stockreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableMongoAuditing
public class StockReportApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockReportApplication.class, args);
    }
}
