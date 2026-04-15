package com.sensedia.scheduler.repository;

import com.sensedia.scheduler.domain.ConsentHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConsentHistoryRepository extends MongoRepository<ConsentHistory, UUID> {
}
