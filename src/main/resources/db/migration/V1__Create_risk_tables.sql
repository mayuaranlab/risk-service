-- Risk Service Database Schema
-- V1: Initial risk tables

-- Risk Limit Table
CREATE TABLE risk_limit (
    limit_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    account_id BIGINT NULL,
    account_code VARCHAR(50) NULL,
    instrument_id BIGINT NULL,
    symbol VARCHAR(20) NULL,
    limit_type VARCHAR(50) NOT NULL,
    limit_value DECIMAL(18, 4) NOT NULL,
    warning_threshold DECIMAL(5, 2) NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Indexes for risk limit queries
CREATE INDEX idx_risk_limit_account_code ON risk_limit(account_code);
CREATE INDEX idx_risk_limit_symbol ON risk_limit(symbol);
CREATE INDEX idx_risk_limit_type ON risk_limit(limit_type);
CREATE INDEX idx_risk_limit_active ON risk_limit(is_active);

-- Risk Alert Table
CREATE TABLE risk_alert (
    alert_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    limit_id BIGINT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    account_id BIGINT NULL,
    account_code VARCHAR(50) NULL,
    instrument_id BIGINT NULL,
    symbol VARCHAR(20) NULL,
    triggering_trade_id VARCHAR(50) NULL,
    current_value DECIMAL(18, 4) NULL,
    limit_value DECIMAL(18, 4) NULL,
    utilization_pct DECIMAL(5, 2) NULL,
    message VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    acknowledged_by VARCHAR(100) NULL,
    acknowledged_at DATETIME2 NULL,
    resolved_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_risk_alert_limit
        FOREIGN KEY (limit_id) REFERENCES risk_limit(limit_id)
);

-- Indexes for risk alert queries
CREATE INDEX idx_risk_alert_status ON risk_alert(status);
CREATE INDEX idx_risk_alert_severity ON risk_alert(severity);
CREATE INDEX idx_risk_alert_account_code ON risk_alert(account_code);
CREATE INDEX idx_risk_alert_created_at ON risk_alert(created_at);
CREATE INDEX idx_risk_alert_trade_id ON risk_alert(triggering_trade_id);

-- Trigger to update updated_at timestamp for risk_limit
CREATE TRIGGER trg_risk_limit_updated_at
ON risk_limit
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE risk_limit
    SET updated_at = GETDATE()
    FROM risk_limit r
    INNER JOIN inserted i ON r.limit_id = i.limit_id;
END;
GO

-- Insert default risk limits
INSERT INTO risk_limit (account_code, limit_type, limit_value, warning_threshold, is_active)
VALUES
    (NULL, 'MAX_SINGLE_TRADE_VALUE', 1000000.00, 80.00, 1),
    (NULL, 'MAX_POSITION_VALUE', 5000000.00, 80.00, 1),
    (NULL, 'MAX_POSITION_QUANTITY', 100000.00, 90.00, 1);
