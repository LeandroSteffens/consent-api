package com.sensedia.scheduler.scheduler;

import com.sensedia.scheduler.domain.Consent;
import com.sensedia.scheduler.domain.ConsentHistory;
import com.sensedia.scheduler.domain.ConsentStatus;
import com.sensedia.scheduler.repository.ConsentHistoryRepository;
import com.sensedia.scheduler.repository.ConsentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConsentExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConsentExpirationScheduler.class);

    private final ConsentRepository repository;
    private final ConsentHistoryRepository historyRepository;

    @Scheduled(cron = "0 * * * * *")
    public void expireOldConsents() {
        LocalDateTime now = LocalDateTime.now();

        List<Consent> expiredConsents = repository.findByStatusAndExpirationDateTimeBefore(ConsentStatus.ACTIVE, now);

        if (!expiredConsents.isEmpty()) {
            log.info("Iniciando rotina de expiração. {} consentimentos encontrados.", expiredConsents.size());

            for (Consent consent : expiredConsents) {
                consent.setStatus(ConsentStatus.EXPIRED);
                repository.save(consent);

                ConsentHistory history = ConsentHistory.builder()
                        .id(UUID.randomUUID())
                        .consentId(consent.getId())
                        .action("EXPIRE")
                        .consentSnapshot(consent)
                        .timestamp(LocalDateTime.now())
                        .build();

                historyRepository.save(history);
            }
            log.info("Rotina de expiração finalizada com sucesso.");
        }
    }
}
