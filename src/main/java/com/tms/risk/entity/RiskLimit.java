package com.tms.risk.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_limit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "limit_id")
    private Long limitId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_code", length = 50)
    private String accountCode;

    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "symbol", length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false, length = 50)
    private LimitType limitType;

    @Column(name = "limit_value", precision = 18, scale = 4, nullable = false)
    private BigDecimal limitValue;

    @Column(name = "warning_threshold", precision = 5, scale = 2)
    private BigDecimal warningThreshold; // Percentage (e.g., 80.00 = 80%)

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum LimitType {
        MAX_POSITION_VALUE,      // Maximum value of a single position
        MAX_POSITION_QUANTITY,   // Maximum quantity for a position
        MAX_ACCOUNT_EXPOSURE,    // Maximum total exposure for an account
        MAX_SINGLE_TRADE_VALUE,  // Maximum value of a single trade
        MAX_DAILY_TRADES,        // Maximum number of trades per day
        MAX_CONCENTRATION,       // Maximum concentration in a single security
        MAX_SECTOR_EXPOSURE,     // Maximum exposure to a sector
        MAX_LOSS_LIMIT           // Maximum loss allowed
    }
}
