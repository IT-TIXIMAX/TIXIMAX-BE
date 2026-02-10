package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Model.DTOResponse.DashBoard.LocationSummary;
import com.tiximax.txm.Model.DTOResponse.DashBoard.StockSummary;
import com.tiximax.txm.Model.DTOResponse.Domestic.WarehouseShip;
import com.tiximax.txm.Model.Projections.CustomerDeliveryRow;
import com.tiximax.txm.Model.Projections.CustomerInventoryProjection;
import com.tiximax.txm.Model.Projections.CustomerInventoryRow;
import com.tiximax.txm.Model.Projections.CustomerShipmentRow;
import com.tiximax.txm.Model.Projections.DraftDomesticDeliveryRow;
import com.tiximax.txm.Model.Projections.ExportedQuantityProjection;
import com.tiximax.txm.Model.Projections.WarehouseFeeProjection;
import com.tiximax.txm.Model.Projections.WarehouseStatisticRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Query("""
    SELECT w.status
    FROM Warehouse w
    WHERE w.trackingCode IN :trackingCodes
    GROUP BY w.status
    """)
    List<WarehouseStatus> findDistinctStatusesByTrackingCodes(
            @Param("trackingCodes") List<String> trackingCodes
    );

    
    @Query("""
        SELECT w.trackingCode, SUM(w.netWeight)
        FROM Warehouse w
        WHERE w.trackingCode IN :trackingCodes
        GROUP BY w.trackingCode
    """)
    List<Object[]> sumCollectWeightByTrackingCodes(
            @Param("trackingCodes") List<String> trackingCodes
    );

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
      select
        c.customerCode as customerCode,
        ol.shipmentCode as trackingCode
    from Warehouse w
      join w.orders o
      join o.customer c
      join w.orderLinks ol
    where ol.status = :orderLinkStatus
      and (:staffId is null or o.staff.id = :staffId)
      and c.customerCode in :customerCodes
    """)
    List<CustomerShipmentRow> findShipmentCodesByCustomerCodes(
            @Param("orderLinkStatus") OrderLinkStatus orderLinkStatus,
            @Param("customerCodes") List<String> customerCodes,
            @Param("staffId") Long staffId
    );

@Query("""
    SELECT w.trackingCode
    FROM Warehouse w
    WHERE w.trackingCode IN :codes
""")
List<String> findExistingTrackingCodesByStatus(
        @Param("codes") List<String> codes
      //  @Param("status") WarehouseStatus status
);

    @Query(nativeQuery = true,
            value = "SELECT " +
                    "COALESCE(SUM(w.weight), 0), " +
                    "COALESCE(SUM(w.net_weight), 0) " +
                    "FROM warehouse w " +
                    "JOIN orders o ON w.order_id = o.order_id " +
                    "WHERE o.route_id = :routeId " +
                    "AND w.packing_id IS NULL " +
                    "AND EXISTS (" +
                    "  SELECT 1 FROM order_links ol " +
                    "  WHERE ol.warehouse_id = w.warehouse_id " +
                    "  AND ol.status = 'DA_NHAP_KHO_NN'" +
                    ")")
    List<Object[]> sumCurrentStockWeightByRoute(@Param("routeId") Long routeId);

@Query("""
    select
        c.customerCode as customerCode,
        c.name as customerName,
        c.phone as phoneNumber,
        max(o.address.addressName) as address,
        r.name as routeName
    from Warehouse w
        join w.packing p
        join w.orders o
        join o.customer c
        join o.route r
        left join o.staff s
    where w.status in :warehouseStatuses
      and p.flightCode is not null
      and (:staffId is null or s.accountId = :staffId)
      and (:customerCode is null or c.customerCode = :customerCode)
      and (:routeId is null or r.routeId = :routeId)
      and not exists (
            select 1
            from DraftDomesticShipment ds
            where ds.shipmentCode  = w.trackingCode
      )
    group by
        c.customerCode,
        c.name,
        c.phone,
        r.name
""")
Page<DraftDomesticDeliveryRow> findDraftDomesticDelivery(
        @Param("warehouseStatuses") List<WarehouseStatus> warehouseStatuses,
        @Param("staffId") Long staffId,
        @Param("customerCode") String customerCode,
        @Param("routeId") Long routeId,
        Pageable pageable
);


@Query("""
    select
        c.customerCode as customerCode,
        w.trackingCode as trackingCode
    from Warehouse w
        join w.orders o
        join o.customer c
        join o.route r
        join w.packing p
    where w.status in :warehouseStatuses
      and p.flightCode is not null
      and c.customerCode in :customerCodes
      and (:staffId is null or o.staff.accountId = :staffId)
      and (:routeId is null or r.routeId = :routeId)
      and not exists (
            select 1
            from DraftDomesticShipment ds
            where ds.shipmentCode  = w.trackingCode
      )
""")
List<CustomerShipmentRow> findTrackingCodesByCustomerCodes(
        @Param("warehouseStatuses") List<WarehouseStatus> warehouseStatuses,
        @Param("customerCodes") List<String> customerCodes,
        @Param("staffId") Long staffId,
        @Param("routeId") Long routeId
);

@Query("""
    SELECT COALESCE(SUM(w.weight), 0)
    FROM Warehouse w
    WHERE w.trackingCode IN :trackingCodes
""")
Double sumWeightByTrackingCodes(
        @Param("trackingCodes") List<String> trackingCodes
);

        List<Warehouse> findByTrackingCodeInAndStatus(
            List<String> trackingCodes,
            WarehouseStatus status
    );
@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Warehouse w
        set w.status = :newStatus
        where w.trackingCode in :codes
          and w.status = :oldStatus
    """)
    int updateStatusByTrackingCodes(
            @Param("codes") List<String> codes,
            @Param("oldStatus") WarehouseStatus oldStatus,
            @Param("newStatus") WarehouseStatus newStatus
    );
    @Query("""
        SELECT DISTINCT ol.orders.orderId
        FROM OrderLinks ol
        WHERE ol.shipmentCode IN :shipmentCodes
    """)
    List<Long> findOrderIdsByShipmentCodes(
            @Param("shipmentCodes") List<String> shipmentCodes
    );

    long countByTrackingCodeIn(Collection<String> trackingCodes);

    long countByTrackingCodeInAndStatus(
            Collection<String> trackingCodes,
            WarehouseStatus status
    );

    @Query(nativeQuery = true,
            value = "SELECT " +
                    "COALESCE(SUM(w.weight), 0), " +
                    "COALESCE(SUM(w.net_weight), 0) " +
                    "FROM warehouse w " +
                    "JOIN orders o ON w.order_id = o.order_id " +
                    "WHERE o.route_id = :routeId " +
                    "AND w.packing_id IS NULL " +
                    "AND EXISTS (" +
                    "  SELECT 1 FROM order_links ol " +
                    "  WHERE ol.warehouse_id = w.warehouse_id " +
                    "  AND ol.status = 'DA_NHAP_KHO_NN'" +
                    ")")
    List<Object[]> sumUnpackedStockWeightByRoute(@Param("routeId") Long routeId);

//    @Query(nativeQuery = true,
//            value = "SELECT " +
//                    "COALESCE(SUM(w.weight), 0), " +
//                    "COALESCE(SUM(w.net_weight), 0) " +
//                    "FROM warehouse w " +
//                    "JOIN orders o ON w.order_id = o.order_id " +
//                    "WHERE o.route_id = :routeId " +
//                    "AND w.packing_id IS NOT NULL " +
//                    "AND EXISTS (" +
//                    "  SELECT 1 FROM order_links ol " +
//                    "  WHERE ol.warehouse_id = w.warehouse_id " +
//                    "  AND ol.status = 'DA_DONG_GOI'" +
//                    ")")
//    List<Object[]> sumPackedStockWeightByRoute(@Param("routeId") Long routeId);

        @Query("""
        SELECT
        COUNT(w.warehouseId)            AS totalCodes,
        COALESCE(SUM(w.netWeight), 0)   AS totalWeight,
        COUNT(DISTINCT o.customer.accountId) AS totalCustomers
        FROM Warehouse w
        JOIN w.orders o
        JOIN o.route r
        WHERE w.status IN (
        com.tiximax.txm.Enums.WarehouseStatus.DA_NHAP_KHO_VN,
        com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
        )
        AND (:routeId IS NULL OR r.routeId = :routeId)
        """)
        WarehouseStatisticRow inStock(
        @Param("routeId") Long routeId
        );


        
   @Query(
    value = """
        SELECT
            COUNT(DISTINCT w.warehouse_id) AS totalCodes,
            COALESCE(SUM(w.net_weight), 0) AS totalWeight,
            COUNT(DISTINCT o.customer_id)  AS totalCustomers
        FROM warehouse w
        JOIN orders o
            ON o.order_id = w.order_id
        JOIN domestic d
            ON w.tracking_code = ANY(d.shipping_list)
        WHERE w.status = 'DA_GIAO'
          AND d.carrier = :carrier
          AND d.timestamp BETWEEN :fromDate AND :toDate
          AND (:routeId IS NULL OR o.route_id = :routeId)
    """,
    nativeQuery = true
)
WarehouseStatisticRow exportByCarrierWithDate(
    @Param("carrier") String carrier,
    @Param("fromDate") LocalDateTime fromDate,
    @Param("toDate") LocalDateTime toDate,
    @Param("routeId") Long routeId
);

         @Query("""
        SELECT
        COUNT(w.warehouseId)            AS totalCodes,
        COALESCE(SUM(w.netWeight), 0)   AS totalWeight,
        COUNT(DISTINCT o.customer.accountId) AS totalCustomers
        FROM Warehouse w
        JOIN w.orders o
        JOIN o.route r
        WHERE w.status = com.tiximax.txm.Enums.WarehouseStatus.DA_NHAP_KHO_VN
        AND (:routeId IS NULL OR r.routeId = :routeId)
        """)
        WarehouseStatisticRow unpaidShipping(
        @Param("routeId") Long routeId
        );

        @Query("""
        SELECT
        COUNT(w.warehouseId)            AS totalCodes,
        COALESCE(SUM(w.netWeight), 0)   AS totalWeight,
        COUNT(DISTINCT o.customer.accountId) AS totalCustomers
        FROM Warehouse w
        JOIN w.orders o
        JOIN o.route r
        WHERE w.status = com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
        AND (:routeId IS NULL OR r.routeId = :routeId)
        """)
        WarehouseStatisticRow paidShipping(
        @Param("routeId") Long routeId
        );

        @Modifying
        @Query("""
        UPDATE Warehouse w
        SET w.status = :status
        WHERE w.trackingCode IN :trackingCodes
        """)
        void updateWarehouseStatusByTrackingCodes(
                @Param("status") WarehouseStatus status,
                @Param("trackingCodes") List<String> trackingCodes
        );

   @Query("""
    select count(w)
    from Warehouse w
        join w.orders o
        join o.customer c
    where c.customerCode = :customerCode
      and w.status in :statuses
      and not exists (
            select 1
            from DraftDomesticShipment ds
            where ds.shipmentCode  = w.trackingCode
      )
""")
int countAvailableByCustomerCode(
        @Param("customerCode") String customerCode,
        @Param("statuses") List<WarehouseStatus> statuses
);


    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.StockSummary(
            COUNT(w.warehouseId),
            COALESCE(SUM(w.weight), 0.0),
            COALESCE(SUM(w.netWeight), 0.0)
        )
        FROM Warehouse w
        LEFT JOIN w.orders o
        LEFT JOIN o.route r
        WHERE w.status = 'DA_NHAP_KHO_NN'
          AND w.packing IS NULL
          AND (:routeId IS NULL OR r.routeId = :routeId)
    """)
    StockSummary getStockSummary(@Param("routeId") Long routeId);

    @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.LocationSummary(
        l.locationId,
        l.name,
        COUNT(DISTINCT w.warehouseId),
        COUNT(DISTINCT w.orders.orderId),
        COALESCE(SUM(w.weight), 0.0),
        COALESCE(SUM(w.netWeight), 0.0)
    )
    FROM Warehouse w
    JOIN w.location l
    LEFT JOIN w.orders o
    LEFT JOIN o.route r
    WHERE w.status = 'DA_NHAP_KHO_NN'
      AND w.packing IS NULL
    GROUP BY l.locationId, l.name
""")
    List<LocationSummary> getStockSummaryByLocation();

    @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.StockSummary(
        COUNT(w.warehouseId),
        COALESCE(SUM(w.weight), 0.0),
        COALESCE(SUM(w.netWeight), 0.0)
    )
    FROM Warehouse w
    WHERE w.status = 'DA_NHAP_KHO_NN'
      AND w.packing IS NULL
      AND w.location.locationId = :locationId
""")
    StockSummary getStockSummaryByLocationId(@Param("locationId") Long locationId);

    @Query("""
    select
        c.customerCode as customerCode,
        c.name as customerName,
        s.staffCode as staffCode,
        s.name as staffName,

        count(w.warehouseId) as exportedCode,
        coalesce(sum(w.netWeight), 0) as exportedWeightKg,

        (
            select count(w2.warehouseId)
            from Warehouse w2
            where w2.orders.customer = c
              and w2.status in (
                    com.tiximax.txm.Enums.WarehouseStatus.DA_NHAP_KHO_VN,
                    com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
              )
        ) as remainingCode,

        (
            select coalesce(sum(w2.netWeight), 0)
            from Warehouse w2
            where w2.orders.customer = c
              and w2.status in (
                    com.tiximax.txm.Enums.WarehouseStatus.DA_NHAP_KHO_VN,
                    com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
              )
        ) as remainingWeightKg

    from Warehouse w
        join w.orders o
        join o.customer c
        join o.staff s
    where w.status = com.tiximax.txm.Enums.WarehouseStatus.DA_GIAO
    group by
        c.customerCode,
        c.name,
        s.staffCode,
        s.name
""")
        List<CustomerInventoryRow> getCustomerInventoryDashboard();

    @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.Domestic.WarehouseShip(
        w.trackingCode,
        w.netWeight,
        w.weight,
        w.height,
        w.length,
        w.width,
        o.priceShip
    )
    FROM Warehouse w
    JOIN w.orders o
    WHERE w.trackingCode IN :trackingCodes
    """)
    List<WarehouseShip> findWarehouseShips(
            @Param("trackingCodes") List<String> trackingCodes
    );
    @Query("""
    SELECT w
    FROM Warehouse w
    JOIN FETCH w.orders o
    JOIN FETCH o.route
    WHERE w.trackingCode IN :trackingCodes
    """)
    List<Warehouse> findByTrackingCodeInFetchOrders(
            @Param("trackingCodes") List<String> trackingCodes
    );

    @Query("""
    SELECT
        w.trackingCode AS trackingCode,
        d.shipCode     AS shipCode,
        r.routeId      AS routeId,
        r.minWeight    AS minWeight,
        o.priceShip    AS priceShip,
        w.netWeight    AS netWeight
    FROM Warehouse w
    JOIN w.orders o
    JOIN o.route r
    JOIN DraftDomesticShipment s
        ON s.shipmentCode = w.trackingCode
    JOIN s.draftDomestic d
    WHERE w.trackingCode IN :trackingCodes
    """)
    List<WarehouseFeeProjection> findWarehouseFees(
            @Param("trackingCodes") List<String> trackingCodes
    );


@Query("""
    SELECT
        c.customerCode AS customerCode,
        c.name         AS customerName,
        s.staffCode    AS staffCode,
        s.name         AS staffName,

        COUNT(
            CASE
                WHEN w.status = com.tiximax.txm.Enums.WarehouseStatus.DA_GIAO
                THEN 1
            END
        ) AS exportedCode,

        COALESCE(
            SUM(
                CASE
                    WHEN w.status = com.tiximax.txm.Enums.WarehouseStatus.DA_GIAO
                    THEN w.netWeight
                END
            ),
            0
        ) AS exportedWeight,
        COUNT(
            CASE
                WHEN w.status IN (
                    com.tiximax.txm.Enums.WarehouseStatus.DA_NHAP_KHO_VN,
                    com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
                )
                THEN 1
            END
        ) AS remainingCode,

        COALESCE(
            SUM(
                CASE
                    WHEN w.status IN (
                        com.tiximax.txm.Enums.WarehouseStatus.DA_NHAP_KHO_VN,
                        com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
                    )
                    THEN w.netWeight
                END
            ),
            0
        ) AS remainingWeight

    FROM Warehouse w
    JOIN w.orders o
    JOIN o.customer c
    JOIN o.staff s

    WHERE (:routeId IS NULL OR o.route.routeId = :routeId)
      AND w.status IN (
          com.tiximax.txm.Enums.WarehouseStatus.DA_GIAO,
          com.tiximax.txm.Enums.WarehouseStatus.DA_NHAP_KHO_VN,
          com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
      )
      AND w.createdAt BETWEEN :startDate AND :endDate

    GROUP BY
        c.customerCode,
        c.name,
        s.staffCode,
        s.name
""")
Page<CustomerInventoryProjection> dashboardInventory(
        @Param("routeId") Long routeId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
);


    @Query("""
        SELECT 
            FUNCTION('DATE', w.createdAt) as date,
            COUNT(w) as inboundCount,
            COALESCE(SUM(COALESCE(w.weight, 0)), 0) as inboundKg,
            COALESCE(SUM(COALESCE(w.netWeight, 0)), 0) as inboundNetKg,
            COUNT(CASE WHEN w.packing IS NOT NULL THEN 1 END) as packedCount,
            COALESCE(SUM(CASE WHEN w.packing IS NOT NULL THEN COALESCE(w.weight, 0) ELSE 0 END), 0) as packedKg
        FROM Warehouse w
        WHERE w.createdAt >= :start
          AND w.createdAt < :end
          AND w.location.id = :locationId
        GROUP BY FUNCTION('DATE', w.createdAt)
        ORDER BY date
        """)
    List<Object[]> findDailyStatsByLocation(
            @Param("locationId") Long locationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT
            w.staff.id as staffId,
            w.staff.staffCode as staffCode,
            w.staff.name as name,
            w.staff.department as department,
            COUNT(w) as inboundCount,
            COALESCE(SUM(COALESCE(w.weight, 0)), 0) as inboundKg,
            COALESCE(SUM(COALESCE(w.netWeight, 0)), 0) as inboundNetKg,
            COUNT(CASE WHEN w.packing IS NOT NULL THEN 1 END) as packedCount,
            COALESCE(SUM(CASE WHEN w.packing IS NOT NULL THEN COALESCE(w.weight, 0) ELSE 0 END), 0) as packedKg
        FROM Warehouse w
        WHERE w.createdAt >= :start
          AND w.createdAt < :end
          AND w.location.id = :locationId
        GROUP BY w.staff.id, w.staff.staffCode, w.staff.name, w.staff.department
        ORDER BY inboundCount DESC
        """)
    List<Object[]> findStaffSummaryByLocation(
            @Param("locationId") Long locationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

@Query("""
    SELECT
        FUNCTION('date', w.createdAt)              AS date,
        COUNT(w.warehouseId)                       AS totalCode,
        COALESCE(SUM(w.netWeight), 0)              AS totalWeight,
        COUNT(DISTINCT c.accountId)                AS totalCustomers
    FROM Warehouse w
        JOIN w.orders o
        JOIN o.customer c
        JOIN o.route r
    WHERE
        w.status = com.tiximax.txm.Enums.WarehouseStatus.DA_GIAO
        AND w.createdAt BETWEEN :startDate AND :endDate
        AND (:routeId IS NULL OR r.routeId = :routeId)
    GROUP BY FUNCTION('date', w.createdAt)
    ORDER BY FUNCTION('date', w.createdAt)
""")
List<ExportedQuantityProjection> getExportedQuantityDaily(
        @Param("routeId") Long routeId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
);


}
