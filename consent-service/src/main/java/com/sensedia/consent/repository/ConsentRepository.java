package com.sensedia.consent.repository;

import com.sensedia.consent.domain.Consent;
import com.sensedia.consent.domain.ConsentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRepository extends MongoRepository<Consent, UUID> {

    Optional<Consent> findByIdempotencyKey(String idempotencyKey);

    List<Consent> findByStatusAndExpirationDateTimeBefore(ConsentStatus status, LocalDateTime dateTime);
}
