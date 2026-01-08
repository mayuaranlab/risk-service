package com.tms.risk.repository;

import com.tms.risk.entity.RiskAlert;
import com.tms.risk.entity.RiskAlert.AlertStatus;
import com.tms.risk.entity.RiskAlert.AlertType;
import com.tms.risk.entity.RiskAlert.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {

    List<RiskAlert> findByStatus(AlertStatus status);

    List<RiskAlert> findByAccountCodeAndStatus(String accountCode, AlertStatus status);

    List<RiskAlert> findBySeverityAndStatus(Severity severity, AlertStatus status);

    Page<RiskAlert> findByStatusIn(List<AlertStatus> statuses, Pageable pageable);

    @Query("SELECT r FROM RiskAlert r WHERE r.status = 'OPEN' " +
           "AND r.severity IN ('HIGH', 'CRITICAL') " +
           "ORDER BY r.createdAt DESC")
    List<RiskAlert> findCriticalOpenAlerts();

    @Query("SELECT r FROM RiskAlert r WHERE r.accountCode = :accountCode " +
           "AND r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<RiskAlert> findRecentAlertsByAccount(@Param("accountCode") String accountCode,
                                               @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM RiskAlert r WHERE r.status = 'OPEN' AND r.severity = :severity")
    long countOpenAlertsBySeverity(@Param("severity") Severity severity);

    List<RiskAlert> findByTriggeringTradeId(String tradeId);

    @Query("SELECT r FROM RiskAlert r WHERE r.alertType = :alertType " +
           "AND r.accountCode = :accountCode AND r.symbol = :symbol " +
           "AND r.status = 'OPEN'")
    List<RiskAlert> findExistingOpenAlert(@Param("alertType") AlertType alertType,
                                           @Param("accountCode") String accountCode,
                                           @Param("symbol") String symbol);
}
