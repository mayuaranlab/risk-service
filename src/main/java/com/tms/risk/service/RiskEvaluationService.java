package com.tms.risk.service;

import com.tms.common.config.kafka.KafkaTopics;
import com.tms.common.observability.logging.CorrelationIdFilter;
import com.tms.common.observability.metrics.TradeMetrics;
import com.tms.risk.entity.RiskAlert;
import com.tms.risk.entity.RiskLimit;
import com.tms.risk.repository.RiskAlertRepository;
import com.tms.risk.repository.RiskLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskEvaluationService {

    private final RiskLimitRepository riskLimitRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TradeMetrics tradeMetrics;

    @Transactional
    public List<RiskAlert> evaluatePosition(Map<String, Object> positionEvent) {
        String correlationId = (String) positionEvent.getOrDefault("correlationId",
            CorrelationIdFilter.getCurrentCorrelationId());
        String positionId = (String) positionEvent.get("positionId");
        String accountCode = (String) positionEvent.get("accountCode");
        String symbol = (String) positionEvent.get("symbol");
        String triggeringTradeId = (String) positionEvent.get("triggeringTradeId");

        log.info("Evaluating risk for position: positionId={}, accountCode={}, symbol={}",
            positionId, accountCode, symbol);

        BigDecimal newQuantity = new BigDecimal((String) positionEvent.get("newQuantity"));
        BigDecimal avgCost = new BigDecimal((String) positionEvent.get("avgCost"));
        BigDecimal costBasis = new BigDecimal((String) positionEvent.get("costBasis"));

        List<RiskLimit> applicableLimits = riskLimitRepository.findApplicableLimits(accountCode, symbol);
        List<RiskAlert> alerts = new ArrayList<>();

        for (RiskLimit limit : applicableLimits) {
            Optional<RiskAlert> alert = evaluateLimit(limit, positionEvent, newQuantity, avgCost,
                costBasis, triggeringTradeId, correlationId);
            alert.ifPresent(a -> {
                alerts.add(riskAlertRepository.save(a));
                publishRiskAlert(a, correlationId);
                tradeMetrics.incrementRiskAlerts(a.getSeverity().name());
            });
        }

        if (alerts.isEmpty()) {
            log.debug("No risk alerts generated for position: positionId={}", positionId);
        } else {
            log.warn("Generated {} risk alerts for position: positionId={}", alerts.size(), positionId);
        }

        return alerts;
    }

    private Optional<RiskAlert> evaluateLimit(RiskLimit limit, Map<String, Object> positionEvent,
                                               BigDecimal quantity, BigDecimal avgCost,
                                               BigDecimal costBasis, String tradeId, String correlationId) {

        BigDecimal currentValue = calculateCurrentValue(limit.getLimitType(), quantity, avgCost, costBasis);
        BigDecimal utilizationPct = currentValue.divide(limit.getLimitValue(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        // Check for existing open alert to avoid duplicates
        List<RiskAlert> existingAlerts = riskAlertRepository.findExistingOpenAlert(
            RiskAlert.AlertType.LIMIT_BREACH,
            (String) positionEvent.get("accountCode"),
            (String) positionEvent.get("symbol"));

        boolean isBreach = currentValue.compareTo(limit.getLimitValue()) >= 0;
        boolean isWarning = !isBreach && limit.getWarningThreshold() != null &&
            utilizationPct.compareTo(limit.getWarningThreshold()) >= 0;

        if (!isBreach && !isWarning) {
            // If we had an open alert and now we're back to normal, resolve it
            existingAlerts.forEach(alert -> {
                alert.setStatus(RiskAlert.AlertStatus.RESOLVED);
                alert.setResolvedAt(java.time.LocalDateTime.now());
                riskAlertRepository.save(alert);
            });
            return Optional.empty();
        }

        // Don't create duplicate alerts
        if (!existingAlerts.isEmpty()) {
            log.debug("Alert already exists for this limit breach");
            return Optional.empty();
        }

        RiskAlert.Severity severity = determineSeverity(utilizationPct, isBreach);
        RiskAlert.AlertType alertType = isBreach ? RiskAlert.AlertType.LIMIT_BREACH : RiskAlert.AlertType.LIMIT_WARNING;

        String message = String.format("%s: %s at %.2f%% utilization (Current: %s, Limit: %s)",
            isBreach ? "LIMIT BREACH" : "WARNING",
            limit.getLimitType().name(),
            utilizationPct,
            currentValue.setScale(2, RoundingMode.HALF_UP),
            limit.getLimitValue().setScale(2, RoundingMode.HALF_UP));

        return Optional.of(RiskAlert.builder()
            .limitId(limit.getLimitId())
            .alertType(alertType)
            .severity(severity)
            .accountId(limit.getAccountId())
            .accountCode(limit.getAccountCode())
            .instrumentId(limit.getInstrumentId())
            .symbol(limit.getSymbol())
            .triggeringTradeId(tradeId)
            .currentValue(currentValue)
            .limitValue(limit.getLimitValue())
            .utilizationPct(utilizationPct)
            .message(message)
            .status(RiskAlert.AlertStatus.OPEN)
            .build());
    }

    private BigDecimal calculateCurrentValue(RiskLimit.LimitType limitType,
                                              BigDecimal quantity, BigDecimal avgCost,
                                              BigDecimal costBasis) {
        return switch (limitType) {
            case MAX_POSITION_VALUE -> costBasis;
            case MAX_POSITION_QUANTITY -> quantity.abs();
            default -> costBasis;
        };
    }

    private RiskAlert.Severity determineSeverity(BigDecimal utilizationPct, boolean isBreach) {
        if (isBreach) {
            if (utilizationPct.compareTo(BigDecimal.valueOf(120)) >= 0) {
                return RiskAlert.Severity.CRITICAL;
            }
            return RiskAlert.Severity.HIGH;
        }
        if (utilizationPct.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return RiskAlert.Severity.MEDIUM;
        }
        return RiskAlert.Severity.LOW;
    }

    private void publishRiskAlert(RiskAlert alert, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "RiskAlert");
        event.put("eventTime", Instant.now().toEpochMilli());
        event.put("correlationId", correlationId);
        event.put("source", "risk-service");

        event.put("alertId", alert.getAlertId().toString());
        event.put("alertType", alert.getAlertType().name());
        event.put("severity", alert.getSeverity().name());
        event.put("accountCode", alert.getAccountCode());
        event.put("symbol", alert.getSymbol());
        event.put("triggeringTradeId", alert.getTriggeringTradeId());
        event.put("currentValue", alert.getCurrentValue().toString());
        event.put("limitValue", alert.getLimitValue().toString());
        event.put("utilizationPct", alert.getUtilizationPct().toString());
        event.put("message", alert.getMessage());

        String key = alert.getAccountCode() + ":" + alert.getSeverity().name();
        kafkaTemplate.send(KafkaTopics.RISK_ALERTS, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish RiskAlert event: alertId={}", alert.getAlertId(), ex);
                } else {
                    log.info("RiskAlert event published: alertId={}, severity={}",
                        alert.getAlertId(), alert.getSeverity());
                }
            });
    }

    @Transactional
    public RiskAlert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        RiskAlert alert = riskAlertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        alert.setStatus(RiskAlert.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(java.time.LocalDateTime.now());

        return riskAlertRepository.save(alert);
    }

    @Transactional
    public RiskAlert resolveAlert(Long alertId) {
        RiskAlert alert = riskAlertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        alert.setStatus(RiskAlert.AlertStatus.RESOLVED);
        alert.setResolvedAt(java.time.LocalDateTime.now());

        return riskAlertRepository.save(alert);
    }

    public List<RiskAlert> getOpenAlerts() {
        return riskAlertRepository.findByStatus(RiskAlert.AlertStatus.OPEN);
    }

    public List<RiskAlert> getCriticalAlerts() {
        return riskAlertRepository.findCriticalOpenAlerts();
    }
}
