package com.tms.risk.repository;

import com.tms.risk.entity.RiskLimit;
import com.tms.risk.entity.RiskLimit.LimitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskLimitRepository extends JpaRepository<RiskLimit, Long> {

    List<RiskLimit> findByAccountCodeAndIsActiveTrue(String accountCode);

    List<RiskLimit> findBySymbolAndIsActiveTrue(String symbol);

    List<RiskLimit> findByAccountCodeAndSymbolAndIsActiveTrue(String accountCode, String symbol);

    Optional<RiskLimit> findByAccountCodeAndLimitTypeAndIsActiveTrue(String accountCode, LimitType limitType);

    Optional<RiskLimit> findByAccountCodeAndSymbolAndLimitTypeAndIsActiveTrue(
        String accountCode, String symbol, LimitType limitType);

    @Query("SELECT r FROM RiskLimit r WHERE r.isActive = true " +
           "AND (r.accountCode = :accountCode OR r.accountCode IS NULL) " +
           "AND (r.symbol = :symbol OR r.symbol IS NULL)")
    List<RiskLimit> findApplicableLimits(@Param("accountCode") String accountCode,
                                          @Param("symbol") String symbol);

    List<RiskLimit> findByLimitTypeAndIsActiveTrue(LimitType limitType);
}
