package com.sensedia.consent.repository;

import com.sensedia.consent.domain.ConsentHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentHistoryRepository extends MongoRepository<ConsentHistory, UUID> {

    List<ConsentHistory> findByConsentIdOrderByTimestampDesc(UUID consentId);
}
