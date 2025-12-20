package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.WarehouseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByTrackingCode(String trackingCode);
@Query("""
    SELECT DISTINCT w
    FROM Warehouse w
    JOIN w.orderLinks ol
    WHERE w.status = :status
      AND w.location.locationId = :locationId
      AND ol.status IN :validStatuses
      AND w.trackingCode = ol.shipmentCode
      AND (
    :trackingCode IS NULL
    OR w.trackingCode LIKE CONCAT('%', CAST(:trackingCode AS string), '%')
)
""")
Page<Warehouse> findWarehousesForPacking(
        @Param("status") WarehouseStatus status,
        @Param("locationId") Long locationId,
        @Param("validStatuses") List<OrderLinkStatus> validStatuses,
        @Param("trackingCode") String trackingCode,
        Pageable pageable
);

    List<Warehouse> findAllByStatus(WarehouseStatus warehouseStatus);

    @Query("SELECT w FROM Warehouse w WHERE w.trackingCode IN :trackingCodes")
    List<Warehouse> findByTrackingCodeIn(@Param("trackingCodes") List<String> trackingCodes);

    Optional<Warehouse> findByTrackingCode(String trackingCode);

    List<Warehouse> findByPackingPackingIdAndTrackingCodeIn(Long packingId, List<String> trackingCodes);

    @Query("SELECT COALESCE(SUM(w.netWeight), 0) " +
            "FROM Warehouse w " +
            "WHERE w.createdAt BETWEEN :start AND :end")
    Double sumNetWeightByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(w.weight), 0) " +
            "FROM Warehouse w " +
            "WHERE w.createdAt BETWEEN :start AND :end")
    Double sumWeightByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT w FROM Warehouse w " +
            "LEFT JOIN FETCH w.orders o " +
            "LEFT JOIN FETCH w.orderLinks ol " +
            "WHERE w.trackingCode IN :codes")
    List<Warehouse> findByTrackingCodeInWithOrdersAndLinks(@Param("codes") List<String> codes);

    @Query("SELECT w FROM Warehouse w " +
            "LEFT JOIN FETCH w.orders o " +
            "LEFT JOIN FETCH w.orderLinks ol " +
            "WHERE w.trackingCode IN :codes AND w.packing IS NULL")
    List<Warehouse> findByTrackingCodeInAndPackingIsNullWithOrdersAndLinks(
            @Param("codes") List<String> codes);

    @Query("SELECT MONTH(w.createdAt), SUM(w.weight) FROM Warehouse w WHERE YEAR(w.createdAt) = :year GROUP BY MONTH(w.createdAt)")
    List<Object[]> sumWeightByMonth(@Param("year") int year);

    @Query("SELECT MONTH(w.createdAt), SUM(w.netWeight) FROM Warehouse w WHERE YEAR(w.createdAt) = :year GROUP BY MONTH(w.createdAt)")
    List<Object[]> sumNetWeightByMonth(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(w.netWeight), 0) " +
            "FROM Warehouse w " +
            "WHERE w.packing.flightCode = :flightCode")
    BigDecimal sumNetWeightByFlightCode(@Param("flightCode") String flightCode);

    @Query("SELECT COALESCE(SUM(w.netWeight * o.priceShip), 0) " +
            "FROM Warehouse w JOIN w.orders o " +
            "WHERE w.packing.flightCode = :flightCode")
    BigDecimal sumWeightedRevenueByFlightCode(@Param("flightCode") String flightCode);

    @Query("""
    SELECT 
        o.customer.id,              
        COALESCE(SUM(w.weight), 0.0),   
        COALESCE(SUM(w.netWeight), 0.0),
        o.priceShip                    
    FROM Warehouse w
    JOIN w.orders o
    WHERE w.packing.flightCode = :flightCode
    GROUP BY o.customer.id, o.priceShip
    """)
    List<Object[]> sumNetWeightAndPriceShipByCustomer(@Param("flightCode") String flightCode);

}