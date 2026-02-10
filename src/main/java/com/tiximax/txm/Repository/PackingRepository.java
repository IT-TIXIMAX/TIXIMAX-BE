package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Enums.PackingStatus;
import com.tiximax.txm.Model.DTOResponse.DashBoard.LocationSummary;
import com.tiximax.txm.Model.DTOResponse.DashBoard.PackedSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PackingRepository extends JpaRepository<Packing, Long> {

    boolean existsByPackingCode(String packingCode);

    List<Packing> findByPackingCodeStartingWith(String baseCode);

    Page<Packing> findByFlightCodeIsNull(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Packing p " +
            "JOIN p.warehouses w " +
            "WHERE p.flightCode IS NULL " +
            "AND w.location.locationId = :locationId")
    Page<Packing> findByFlightCodeIsNullAndWarehouses_Location_LocationId(
            @Param("locationId") Long locationId, Pageable pageable);

    @Query("SELECT p FROM Packing p JOIN p.warehouses w WHERE p.status = :status AND w.location.locationId = :warehouseLocationId")
    Page<Packing> findByStatusAndWarehouses_Location_LocationId(
            @Param("status") PackingStatus status,
            @Param("warehouseLocationId") Long warehouseLocationId,
            Pageable pageable);

    List<Packing> findAllByPackingCodeIn(List<String> packingCode);

        @Query("""
        SELECT DISTINCT p
        FROM Packing p
        LEFT JOIN FETCH p.warehouses w
        WHERE p.packingId IN :ids
    """)
    List<Packing> findAllChoBayWithWarehouses(@Param("ids") List<Long> ids);

    Optional<Packing> findByPackingCode(String packingCode);

    Page<Packing> findByStatus(PackingStatus packingStatus, Pageable pageable);

    @Query("SELECT p.packingList FROM Packing p WHERE p.packingCode = :packingCode")
    List<String> findPackingListByCode(@Param("packingCode") String packingCode);

    boolean existsByFlightCode(String flightCode);

    @Query("""
    SELECT COALESCE(MAX(r.minWeight), 0)
    FROM Packing p
    JOIN p.warehouses w
    JOIN w.orders o
    JOIN o.route r
    WHERE p.flightCode = :flightCode
    """)
    BigDecimal findRouteMinWeightViaWarehouse(@Param("flightCode") String flightCode);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.PackedSummary(
            COUNT(DISTINCT p.packingId),
            COUNT(DISTINCT w.warehouseId),
            COUNT(DISTINCT w.orders.orderId),
            COALESCE(SUM(w.weight), 0.0),
            COALESCE(SUM(w.netWeight), 0.0)
        )
        FROM Packing p
        JOIN p.warehouses w
        LEFT JOIN w.orders o
        LEFT JOIN o.route r
        WHERE p.packedDate >= :start
          AND p.packedDate < :end
          AND p.flightCode IS NOT NULL
          AND (:routeId IS NULL OR r.routeId = :routeId)
        """)
    PackedSummary getPackedSummary(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId
    );

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.LocationSummary(
            l.locationId,
            l.name,
            COUNT(DISTINCT w.warehouseId),
            COUNT(DISTINCT w.orders.orderId),
            COUNT(DISTINCT p.packingId),
            COALESCE(SUM(w.weight), 0.0),
            COALESCE(SUM(w.netWeight), 0.0)
        )
        FROM Packing p
        JOIN p.warehouses w
        JOIN w.location l
        WHERE p.packedDate >= :start
          AND p.packedDate < :end
          AND p.flightCode IS NOT NULL
        GROUP BY l.locationId, l.name
        """)
    List<LocationSummary> getPackedSummaryByLocation(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.PackedSummary(
        COUNT(DISTINCT p.packingId),
        COUNT(DISTINCT w.warehouseId),
        COUNT(DISTINCT w.orders.orderId),
        COALESCE(SUM(w.weight), 0.0),
        COALESCE(SUM(w.netWeight), 0.0)
    )
    FROM Packing p
    JOIN p.warehouses w
    WHERE p.packedDate >= :start
      AND p.packedDate < :end
      AND p.flightCode IS NOT NULL
      AND w.location.locationId = :locationId
    """)
    PackedSummary getPackedSummaryByLocationId(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("locationId") Long locationId
    );

    @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.PackedSummary(
        COUNT(DISTINCT p.packingId),
        COUNT(DISTINCT w.warehouseId),
        COUNT(DISTINCT w.orders.orderId),
        COALESCE(SUM(w.weight), 0.0),
        COALESCE(SUM(w.netWeight), 0.0)
    )
    FROM Packing p
    JOIN p.warehouses w
    WHERE p.packedDate >= :start
      AND p.packedDate < :end
      AND p.flightCode IS NULL
      AND w.location.locationId = :locationId
    """)
    PackedSummary getAwaitingFlightSummaryByLocation(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("locationId") Long locationId
    );

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.PackedSummary(
            COUNT(DISTINCT p.packingId),
            COUNT(DISTINCT w.warehouseId),
            COUNT(DISTINCT w.orders.orderId),
            COALESCE(SUM(w.weight), 0.0),
            COALESCE(SUM(w.netWeight), 0.0)
        )
        FROM Packing p
        JOIN p.warehouses w
        LEFT JOIN w.orders o
        LEFT JOIN o.route r
        WHERE p.packedDate >= :start
          AND p.packedDate < :end
          AND p.flightCode IS NULL
          AND (:routeId IS NULL OR r.routeId = :routeId)
        """)
    PackedSummary getAwaitFlightSummary(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId
    );

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.LocationSummary(
            l.locationId,
            l.name,
            COUNT(DISTINCT w.warehouseId),
            COUNT(DISTINCT w.orders.orderId),
            COUNT(DISTINCT p.packingId),
            COALESCE(SUM(w.weight), 0.0),
            COALESCE(SUM(w.netWeight), 0.0)
        )
        FROM Packing p
        JOIN p.warehouses w
        JOIN w.location l
        WHERE p.packedDate >= :start
          AND p.packedDate < :end
          AND p.flightCode IS NULL
        GROUP BY l.locationId, l.name
        """)
    List<LocationSummary> getAwaitingFlightPackedSummaryByLocation(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Packing> findByFlightCode(String flightCode);

     @Query("""
        SELECT DISTINCT p.flightCode
        FROM Packing p
        JOIN p.warehouses w
        JOIN w.orders o
        WHERE p.flightCode IS NOT NULL
          AND p.packedDate >= :fromDate
          AND o.route.routeId = :routeId
          AND NOT EXISTS (
              SELECT 1
              FROM FlightShipment fs
              WHERE fs.flightCode = p.flightCode
          )
    """)
    List<String> findAvailableFlightCodesByRouteLast3Months(
            @Param("routeId") Long routeId,
            @Param("fromDate") LocalDateTime fromDate
    );
}