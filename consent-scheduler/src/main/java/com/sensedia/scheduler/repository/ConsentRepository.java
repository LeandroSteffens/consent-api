package com.sensedia.scheduler.repository;

import com.sensedia.scheduler.domain.Consent;
import com.sensedia.scheduler.domain.ConsentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentRepository extends MongoRepository<Consent, UUID> {

    List<Consent> findByStatusAndExpirationDateTimeBefore(ConsentStatus status, LocalDateTime dateTime);
}
