package com.tms.risk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertAcknowledgeRequest {

    @NotBlank(message = "Acknowledged by is required")
    private String acknowledgedBy;
}
