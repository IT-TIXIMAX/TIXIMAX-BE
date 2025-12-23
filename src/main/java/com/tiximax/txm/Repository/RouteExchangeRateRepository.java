package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.RouteExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository

public interface RouteExchangeRateRepository extends JpaRepository<RouteExchangeRate, Long> {
    boolean existsByRoute_RouteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
            Long routeId, LocalDate startDate, LocalDate endDate, Long id);

    boolean existsByRoute_RouteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long routeId, LocalDate startDate, LocalDate endDate);

    @Query("""
        SELECT rer FROM RouteExchangeRate rer
        WHERE rer.route.routeId = :routeId
          AND rer.startDate <= :date
          AND (rer.endDate >= :date)
        ORDER BY rer.startDate DESC
        LIMIT 1
    """)
    Optional<RouteExchangeRate> findEffectiveRate(@Param("routeId") Long routeId, @Param("date") LocalDate date);

    @Query("SELECT rer FROM RouteExchangeRate rer " +
            "WHERE rer.route.routeId = :routeId " +
            "  AND rer.startDate <= :endDate " +
            "  AND (rer.endDate IS NULL OR rer.endDate >= :startDate) " +
            "ORDER BY rer.startDate ASC")
    List<RouteExchangeRate> findApplicableRates(@Param("routeId") Long routeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT r.exchangeRate FROM Route r WHERE r.routeId = :routeId")
    BigDecimal getDefaultRate(@Param("routeId") Long routeId);

}