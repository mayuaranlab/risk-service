package com.tms.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAlertResponse {
    private Long alertId;
    private Long limitId;
    private String alertType;
    private String severity;
    private String accountCode;
    private String symbol;
    private String triggeringTradeId;
    private BigDecimal currentValue;
    private BigDecimal limitValue;
    private BigDecimal utilizationPct;
    private String message;
    private String status;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
