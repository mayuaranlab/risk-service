package com.tms.risk.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_alert")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "limit_id")
    private Long limitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_code", length = 50)
    private String accountCode;

    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "symbol", length = 20)
    private String symbol;

    @Column(name = "triggering_trade_id", length = 50)
    private String triggeringTradeId;

    @Column(name = "current_value", precision = 18, scale = 4)
    private BigDecimal currentValue;

    @Column(name = "limit_value", precision = 18, scale = 4)
    private BigDecimal limitValue;

    @Column(name = "utilization_pct", precision = 5, scale = 2)
    private BigDecimal utilizationPct;

    @Column(name = "message", length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum AlertType {
        LIMIT_BREACH,
        LIMIT_WARNING,
        POSITION_CONCENTRATION,
        UNUSUAL_ACTIVITY,
        LOSS_THRESHOLD
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AlertStatus {
        OPEN,
        ACKNOWLEDGED,
        RESOLVED,
        DISMISSED
    }
}
