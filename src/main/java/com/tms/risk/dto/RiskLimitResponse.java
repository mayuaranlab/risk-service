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
public class RiskLimitResponse {
    private Long limitId;
    private Long accountId;
    private String accountCode;
    private Long instrumentId;
    private String symbol;
    private String limitType;
    private BigDecimal limitValue;
    private BigDecimal warningThreshold;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
