package com.stockreport.scheduler;

import org.springframework.context.ApplicationEvent;

public class DataCollectionCompletedEvent extends ApplicationEvent {

    public DataCollectionCompletedEvent(Object source) {
        super(source);
    }
}
