package com.tms.risk.controller;

import com.tms.risk.dto.AlertAcknowledgeRequest;
import com.tms.risk.dto.RiskAlertResponse;
import com.tms.risk.dto.RiskLimitRequest;
import com.tms.risk.dto.RiskLimitResponse;
import com.tms.risk.entity.RiskAlert;
import com.tms.risk.entity.RiskLimit;
import com.tms.risk.repository.RiskAlertRepository;
import com.tms.risk.repository.RiskLimitRepository;
import com.tms.risk.service.RiskEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk", description = "Risk management APIs")
public class RiskController {

    private final RiskEvaluationService riskEvaluationService;
    private final RiskLimitRepository riskLimitRepository;
    private final RiskAlertRepository riskAlertRepository;

    // ============= Alerts =============

    @GetMapping("/alerts")
    @Operation(summary = "Get all open alerts")
    public ResponseEntity<List<RiskAlertResponse>> getOpenAlerts() {
        List<RiskAlert> alerts = riskEvaluationService.getOpenAlerts();
        return ResponseEntity.ok(alerts.stream()
            .map(this::toAlertResponse)
            .collect(Collectors.toList()));
    }

    @GetMapping("/alerts/critical")
    @Operation(summary = "Get critical alerts")
    public ResponseEntity<List<RiskAlertResponse>> getCriticalAlerts() {
        List<RiskAlert> alerts = riskEvaluationService.getCriticalAlerts();
        return ResponseEntity.ok(alerts.stream()
            .map(this::toAlertResponse)
            .collect(Collectors.toList()));
    }

    @GetMapping("/alerts/{alertId}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<RiskAlertResponse> getAlert(@PathVariable Long alertId) {
        return riskAlertRepository.findById(alertId)
            .map(this::toAlertResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<RiskAlertResponse> acknowledgeAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody AlertAcknowledgeRequest request) {

        log.info("Acknowledging alert: alertId={}, by={}", alertId, request.getAcknowledgedBy());
        RiskAlert alert = riskEvaluationService.acknowledgeAlert(alertId, request.getAcknowledgedBy());
        return ResponseEntity.ok(toAlertResponse(alert));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    @Operation(summary = "Resolve an alert")
    public ResponseEntity<RiskAlertResponse> resolveAlert(@PathVariable Long alertId) {
        log.info("Resolving alert: alertId={}", alertId);
        RiskAlert alert = riskEvaluationService.resolveAlert(alertId);
        return ResponseEntity.ok(toAlertResponse(alert));
    }

    // ============= Limits =============

    @GetMapping("/limits")
    @Operation(summary = "Get all active limits")
    public ResponseEntity<List<RiskLimitResponse>> getAllLimits() {
        List<RiskLimit> limits = riskLimitRepository.findAll();
        return ResponseEntity.ok(limits.stream()
            .map(this::toLimitResponse)
            .collect(Collectors.toList()));
    }

    @GetMapping("/limits/account/{accountCode}")
    @Operation(summary = "Get limits for an account")
    public ResponseEntity<List<RiskLimitResponse>> getLimitsByAccount(@PathVariable String accountCode) {
        List<RiskLimit> limits = riskLimitRepository.findByAccountCodeAndIsActiveTrue(accountCode);
        return ResponseEntity.ok(limits.stream()
            .map(this::toLimitResponse)
            .collect(Collectors.toList()));
    }

    @PostMapping("/limits")
    @Operation(summary = "Create a new risk limit")
    public ResponseEntity<RiskLimitResponse> createLimit(@Valid @RequestBody RiskLimitRequest request) {
        log.info("Creating risk limit: type={}, accountCode={}", request.getLimitType(), request.getAccountCode());

        RiskLimit limit = RiskLimit.builder()
            .accountId(request.getAccountId())
            .accountCode(request.getAccountCode())
            .instrumentId(request.getInstrumentId())
            .symbol(request.getSymbol())
            .limitType(request.getLimitType())
            .limitValue(request.getLimitValue())
            .warningThreshold(request.getWarningThreshold())
            .isActive(true)
            .build();

        limit = riskLimitRepository.save(limit);
        return ResponseEntity.ok(toLimitResponse(limit));
    }

    @PutMapping("/limits/{limitId}")
    @Operation(summary = "Update a risk limit")
    public ResponseEntity<RiskLimitResponse> updateLimit(
            @PathVariable Long limitId,
            @Valid @RequestBody RiskLimitRequest request) {

        return riskLimitRepository.findById(limitId)
            .map(limit -> {
                limit.setLimitValue(request.getLimitValue());
                limit.setWarningThreshold(request.getWarningThreshold());
                limit.setIsActive(request.getIsActive());
                return ResponseEntity.ok(toLimitResponse(riskLimitRepository.save(limit)));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/limits/{limitId}")
    @Operation(summary = "Deactivate a risk limit")
    public ResponseEntity<Void> deactivateLimit(@PathVariable Long limitId) {
        return riskLimitRepository.findById(limitId)
            .map(limit -> {
                limit.setIsActive(false);
                riskLimitRepository.save(limit);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Risk Service is running");
    }

    // ============= Mappers =============

    private RiskAlertResponse toAlertResponse(RiskAlert alert) {
        return RiskAlertResponse.builder()
            .alertId(alert.getAlertId())
            .limitId(alert.getLimitId())
            .alertType(alert.getAlertType().name())
            .severity(alert.getSeverity().name())
            .accountCode(alert.getAccountCode())
            .symbol(alert.getSymbol())
            .triggeringTradeId(alert.getTriggeringTradeId())
            .currentValue(alert.getCurrentValue())
            .limitValue(alert.getLimitValue())
            .utilizationPct(alert.getUtilizationPct())
            .message(alert.getMessage())
            .status(alert.getStatus().name())
            .acknowledgedBy(alert.getAcknowledgedBy())
            .acknowledgedAt(alert.getAcknowledgedAt())
            .resolvedAt(alert.getResolvedAt())
            .createdAt(alert.getCreatedAt())
            .build();
    }

    private RiskLimitResponse toLimitResponse(RiskLimit limit) {
        return RiskLimitResponse.builder()
            .limitId(limit.getLimitId())
            .accountId(limit.getAccountId())
            .accountCode(limit.getAccountCode())
            .instrumentId(limit.getInstrumentId())
            .symbol(limit.getSymbol())
            .limitType(limit.getLimitType().name())
            .limitValue(limit.getLimitValue())
            .warningThreshold(limit.getWarningThreshold())
            .isActive(limit.getIsActive())
            .createdAt(limit.getCreatedAt())
            .updatedAt(limit.getUpdatedAt())
            .build();
    }
}
