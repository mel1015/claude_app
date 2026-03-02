package com.stockreport.domain.signal;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalRepository extends MongoRepository<Signal, String> {
    List<Signal> findByActiveOrderByCreatedAtDesc(boolean active);
    List<Signal> findAllByOrderByCreatedAtDesc();
}
