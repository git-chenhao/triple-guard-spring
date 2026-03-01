package com.chenhao.tripleguard.service;

import com.chenhao.tripleguard.config.AlertConfig;
import com.chenhao.tripleguard.entity.Alert;
import com.chenhao.tripleguard.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 告警去重服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private final AlertRepository alertRepository;
    private final AlertConfig alertConfig;

    public boolean isDuplicate(String fingerprint) {
        if (!alertConfig.getDeduplication().isEnabled()) {
            return false;
        }

        Optional<Alert> existingAlert = alertRepository.findByFingerprint(fingerprint);
        if (existingAlert.isEmpty()) {
            return false;
        }

        Alert alert = existingAlert.get();
        LocalDateTime timeWindow = LocalDateTime.now()
                .minusSeconds(alertConfig.getDeduplication().getTimeWindowSeconds());

        return alert.getReceivedAt().isAfter(timeWindow);
    }

    public boolean isDeduplicationEnabled() {
        return alertConfig.getDeduplication().isEnabled();
    }
}
