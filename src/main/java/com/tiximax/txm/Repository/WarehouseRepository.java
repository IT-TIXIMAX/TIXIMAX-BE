package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Model.CustomerDeliveryRow;

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

    @Query(value = """
    SELECT 
        r.name AS route_name,
        COALESCE(SUM(w.weight), 0.0) AS total_weight,
        COALESCE(SUM(w.net_weight), 0.0) AS total_net_weight
    FROM route r
    LEFT JOIN orders o ON o.route_id = r.route_id
    LEFT JOIN warehouse w ON w.order_id = o.order_id
        AND w.created_at >= :start
        AND w.created_at < :end
    GROUP BY r.route_id, r.name
    ORDER BY total_weight DESC, total_net_weight DESC
    """, nativeQuery = true)
    List<Object[]> sumWeightByRouteNativeRaw(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

        @Query("""
        SELECT DISTINCT w
        FROM Warehouse w
        JOIN w.orderLinks ol
        JOIN w.orders o
        JOIN o.customer c
        WHERE ol.status = :status
        AND (:customerCode IS NULL OR c.customerCode = :customerCode)
        """)
        List<Warehouse> findWarehousesByOrderLinkStatus(
        @Param("status") OrderLinkStatus status,
        @Param("customerCode") String customerCode
);

 @Query("""
    SELECT COUNT(ol)
    FROM OrderLinks ol
    JOIN ol.warehouse w
    JOIN w.packing p
    JOIN ol.orders o
    JOIN o.customer c
    WHERE p.flightCode = :flightCode
      AND c.accountId = :customerId
      AND ol.status = :importedStatus
""")
int countNotImportedByCustomerAndFlight(
        @Param("customerId") Long customerId,
        @Param("flightCode") String flightCode,
        @Param("importedStatus") OrderLinkStatus importedStatus
);


  @Query("""
    SELECT COUNT(ol)
    FROM OrderLinks ol
    JOIN ol.warehouse w
    JOIN w.packing p
    JOIN ol.orders o
    JOIN o.customer c
    WHERE p.flightCode = :flightCode
      AND c.accountId = :customerId
      AND ol.status = :importedStatus
""")
int countImportedByCustomerAndFlight(
        @Param("customerId") Long customerId,
        @Param("flightCode") String flightCode,
        @Param("importedStatus") OrderLinkStatus importedStatus
);

   @Query("""
    SELECT COUNT(ol)
    FROM OrderLinks ol
    JOIN ol.orders o
    JOIN o.customer c
    WHERE c.accountId = :customerId
      AND ol.status = :importedStatus
        """)
        int countInventoryByCustomer(
                @Param("customerId") Long customerId,
                @Param("importedStatus") OrderLinkStatus importedStatus
        );

    @Query("""
        SELECT COUNT(w)
        FROM Warehouse w
        WHERE w.packing.flightCode = :flightCode
    """)
    int countByFlightCode(@Param("flightCode") String flightCode);

    @Query(
    value = """
        select distinct w
        from Warehouse w
        join w.orders o
        join o.customer c
        join w.orderLinks ol
        where ol.status = :status
          and (
                :customerCode IS NULL
                OR upper(c.customerCode) = :customerCode
          )
    """,
    countQuery = """
        select count(distinct w.id)
        from Warehouse w
        join w.orders o
        join o.customer c
        join w.orderLinks ol
        where ol.status = :status
          and (
                :customerCode IS NULL
                OR upper(c.customerCode) = :customerCode
          )
    """
)
Page<Warehouse> findByOrderLinkStatusAndCustomerCode(
        @Param("status") OrderLinkStatus status,
        @Param("customerCode") String customerCode,
        Pageable pageable
);
 @Query("""
    select
      c.customerCode as customerCode,
      c.name as customerName,
      c.phone as phoneNumber,
      max(a.addressName) as address,
      s.name as staffName,
      s.staffCode as staffCode
    from Warehouse w
      join w.orders o
      join o.customer c
      left join o.staff s
      left join o.address a
      join w.orderLinks ol
    where ol.status = :orderLinkStatus
      and (:staffId is null or s.id = :staffId)
      and (
           :customerCode is null
           or upper(c.customerCode) like concat('%', cast(:customerCode as string), '%')
      )
    group by c.customerCode, c.name, c.phone, s.name, s.staffCode
    """)
    Page<CustomerDeliveryRow> findDomesticDelivery(
            @Param("orderLinkStatus") OrderLinkStatus orderLinkStatus,
            @Param("customerCode") String customerCode,
            @Param("staffId") Long staffId,
            Pageable pageable
    );

    
    @Query("""
    select c.customerCode, ol.shipmentCode
    from Warehouse w
      join w.orders o
      join o.customer c
      join w.orderLinks ol
    where ol.status = :orderLinkStatus
      and (:staffId is null or o.staff.id = :staffId)
      and c.customerCode in :customerCodes
    """)
    List<Object[]> findShipmentCodesByCustomerCodes(
            @Param("orderLinkStatus") OrderLinkStatus orderLinkStatus,
            @Param("customerCodes") List<String> customerCodes,
            @Param("staffId") Long staffId
    );

 @Query("""
    SELECT w.trackingCode
    FROM Warehouse w
    WHERE w.trackingCode IN :codes
      AND w.status = :status
""")
List<String> findExistingTrackingCodesByStatus(
        @Param("codes") List<String> codes,
        @Param("status") WarehouseStatus status
);
}
