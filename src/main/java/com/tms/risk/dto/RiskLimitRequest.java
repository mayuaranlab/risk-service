package com.tms.risk.dto;

import com.tms.risk.entity.RiskLimit.LimitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLimitRequest {

    private Long accountId;
    private String accountCode;
    private Long instrumentId;
    private String symbol;

    @NotNull(message = "Limit type is required")
    private LimitType limitType;

    @NotNull(message = "Limit value is required")
    @Positive(message = "Limit value must be positive")
    private BigDecimal limitValue;

    private BigDecimal warningThreshold;

    private Boolean isActive = true;
}
