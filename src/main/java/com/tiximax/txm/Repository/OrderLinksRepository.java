package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Model.DTOResponse.DashBoard.ExchangeMoneySummary;
import com.tiximax.txm.Model.DTOResponse.DashBoard.PurchaseDetailDashboard;
import com.tiximax.txm.Model.DTOResponse.DashBoard.PurchaseSummary;
import com.tiximax.txm.Model.DTOResponse.Order.OrderLinkSummaryDTO;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository

public interface OrderLinksRepository extends JpaRepository<OrderLinks, Long> {

    boolean existsByTrackingCode(String orderLinkCode);
    
    boolean existsByShipmentCode(String shipmentCode);

    boolean existsByShipmentCodeAndOrders_OrderIdNot(String shipmentCode, Long orderId);

    List<OrderLinks> findByTrackingCodeIn(List<String> trackingCodes);

    List<OrderLinks> findByOrdersOrderId(Long orderId);

    @Query("SELECT ol FROM OrderLinks ol LEFT JOIN FETCH ol.orders WHERE ol.shipmentCode = :shipmentCode")
    List<OrderLinks> findByShipmentCode(@Param("shipmentCode") String shipmentCode);

    @Query("""
    SELECT ol
    FROM OrderLinks ol
    WHERE ol.shipmentCode IN :shipmentCodes
      AND ol.shipmentCode IS NOT NULL
      AND TRIM(ol.shipmentCode) <> ''
""")
    List<OrderLinks> findByShipmentCodeIn( @Param("shipmentCodes") List<String> shipmentCodes);

        @Query("""
        SELECT ol
        FROM OrderLinks ol
        WHERE ol.orders.customer.customerCode = :customerCode
          AND ol.shipmentCode IS NOT NULL
          AND ol.shipmentCode <> ''
          AND ol.status = :status
          AND ol.partialShipment IS NULL
  """)
  List<OrderLinks> findLinksInWarehouseWithoutPartialShipment(
          @Param("customerCode") String customerCode,
          @Param("status") OrderLinkStatus status
  );

 boolean existsByShipmentCodeAndStatusNot(
            String shipmentCode,
            OrderLinkStatus status
    );
    // OrderLinksRepository.java
    @Query("""
            SELECT ol FROM OrderLinks ol 
            WHERE ol.status = 'DA_MUA' 
            AND (ol.shipmentCode IS NULL OR ol.shipmentCode = '')
            """)
    List<OrderLinks> findPendingShipmentLinks();

     List<OrderLinks> findByWarehouse(Warehouse warehouse);

    @Query("""
    SELECT DISTINCT ol.shipmentCode
    FROM OrderLinks ol
    WHERE ol.status = 'DA_MUA'
      AND ol.shipmentCode IS NOT NULL
      AND ol.shipmentCode != ''
      AND (:keyword IS NULL OR ol.shipmentCode LIKE %:keyword%)
    """)
    List<String> suggestShipmentCodes(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(ol) FROM OrderLinks ol WHERE ol.orders.createdAt BETWEEN :start AND :end")
    long countByOrdersCreatedAtBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    boolean existsByShipmentCodeAndLinkIdNot(String newShipmentCode, Long orderLinkId);

    @Query("SELECT ol FROM OrderLinks ol WHERE ol.shipmentCode IN :codes")
    List<OrderLinks> findAllByShipmentCodeIn(@Param("codes") List<String> codes);

//    Long countByOrders_CreatedAtBetween(LocalDateTime start, LocalDateTime end);
    @Query("SELECT COUNT(ol) " +
            "FROM OrderLinks ol " +
            "WHERE ol.orders.createdAt BETWEEN :start AND :end")
    Long countByOrders_CreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Trong OrderLinksRepository
    @Query("""
    SELECT DISTINCT ol
    FROM OrderLinks ol
    WHERE ol.shipmentCode IN :shipmentCodes
      AND ol.shipmentCode IS NOT NULL
      AND TRIM(ol.shipmentCode) <> ''
""")
    Set<OrderLinks> findByShipmentCodeIn( @Param("shipmentCodes") Collection<String> shipmentCodes);

    @Query("SELECT ol FROM OrderLinks ol WHERE ol.orders.customer = :customer AND ol.shipmentCode IS NOT NULL")
    List<OrderLinks> findByCustomerWithShipment(@Param("customer") Customer customer);

//    @Query("""
//        SELECT DISTINCT ol
//        FROM OrderLinks ol
//        LEFT JOIN FETCH ol.warehouse w
//        WHERE ol.orders.customer = :customer
//        AND ol.shipmentCode IS NOT NULL
//    """)
//    List<OrderLinks> findByCustomerWithShipmentAndWarehouse(@Param("customer") Customer customer);

    @Query("SELECT MONTH(ol.orders.createdAt), COUNT(ol) FROM OrderLinks ol WHERE YEAR(ol.orders.createdAt) = :year GROUP BY MONTH(ol.orders.createdAt)")
    List<Object[]> countLinksByMonth(@Param("year") int year);

    List<OrderLinks> findByShipmentCodeInAndStatus(
            List<String> shipmentCodes,
            OrderLinkStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update OrderLinks ol
        set ol.status = :newStatus
        where ol.shipmentCode in :codes
          and ol.status = :oldStatus
    """)
    int updateStatusByShipmentCodes(
            @Param("codes") List<String> codes,
            @Param("oldStatus") OrderLinkStatus oldStatus,
            @Param("newStatus") OrderLinkStatus newStatus
    );

    @Query("""
        SELECT COUNT(ol)
        FROM OrderLinks ol
        WHERE ol.orders.orderId = :orderId
    """)
    long countAllByOrderId(@Param("orderId") Long orderId);

    @Query("""
        SELECT COUNT(ol)
        FROM OrderLinks ol
        WHERE ol.orders.orderId = :orderId
          AND ol.status IN (:finishedStatuses)
    """)
    long countFinishedByOrderId(
            @Param("orderId") Long orderId,
            @Param("finishedStatuses") Set<OrderLinkStatus> finishedStatuses
    );
    @Query("""
        SELECT DISTINCT ol.orders.orderId
        FROM OrderLinks ol
        WHERE ol.shipmentCode IN :shipmentCodes
    """)
    List<Long> findOrderIdsByShipmentCodes(
            @Param("shipmentCodes") List<String> shipmentCodes
    );

    long countByShipmentCodeIn(Collection<String> shipmentCodes);

    long countByShipmentCodeInAndStatus(
            Collection<String> shipmentCodes,
            OrderLinkStatus status
    );
    boolean existsByShipmentCodeInAndStatusNot(
        Collection<String> shipmentCodes,
        OrderLinkStatus status
);
        @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending(
        ol.linkId,
        ol.productName,
        ol.quantity,
        ol.shipmentCode,
        ol.shipWeb,
        ol.website,
        ol.classify,
        ol.purchaseImage,
        ol.trackingCode,
        ol.status,
        ol.orders.customer.customerCode,
        ol.orders.customer.name,
        ol.orders.staff.staffCode,
        ol.orders.staff.name,
        ol.purchase.purchaseId
    )
    FROM OrderLinks ol
    WHERE ol.purchase.purchaseId IN :purchaseIds
      AND (ol.shipmentCode IS NULL OR ol.shipmentCode = '')
""")
List<OrderLinkPending> findPendingLinksDTO(
        @Param("purchaseIds") Set<Long> purchaseIds
);

@Query("""
SELECT new com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending(
    ol.linkId,
    ol.productName,
    ol.quantity,
    ol.shipmentCode,
    ol.shipWeb,
    ol.website,
    ol.classify,
    ol.purchaseImage,
    ol.trackingCode,
    ol.status,
    ol.purchase.purchaseId
)
FROM OrderLinks ol
WHERE ol.purchase.purchaseId IN :purchaseIds
  AND (:status IS NULL OR ol.status = :status)
  AND (
        :shipmentCode IS NULL
        OR LOWER(ol.shipmentCode) LIKE CONCAT('%', :shipmentCode, '%')
  )
""")
List<OrderLinkPending> findPendingLinksDTOv2(
        @Param("purchaseIds") Set<Long> purchaseIds,
        @Param("status") OrderLinkStatus status,
        @Param("shipmentCode") String shipmentCode
);
@Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending(
        ol.linkId,
        ol.productName,
        ol.quantity,
        ol.shipmentCode,
        ol.shipWeb,
        ol.website,
        ol.classify,
        ol.purchaseImage,
        ol.trackingCode,
        ol.status,
        ol.orders.customer.customerCode,
        ol.orders.customer.name,
        ol.orders.staff.staffCode,        
        ol.orders.staff.name,
        ol.purchase.purchaseId
    )
    FROM OrderLinks ol
    WHERE ol.purchase.purchaseId IN :purchaseIds
      AND (:status IS NULL OR ol.status = :status)
""")
List<OrderLinkPending> findPendingLinksNoShipmentCode(
        @Param("purchaseIds") List<Long> purchaseIds,
        @Param("status") OrderLinkStatus status
);
 @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending(
            ol.linkId,
            ol.productName,
            ol.quantity,
            ol.shipmentCode,
            ol.shipWeb,
            ol.website,
            ol.classify,
            ol.purchaseImage,
            ol.trackingCode,
            ol.status,
            c.customerCode,
            c.name,
            s.staffCode,
            s.name,
            ol.purchase.purchaseId
        )
        FROM OrderLinks ol
        JOIN ol.orders o
        JOIN o.customer c
        JOIN o.staff s
        WHERE ol.purchase.purchaseId IN :purchaseIds
          AND (:status IS NULL OR ol.status = :status)
          AND (:shipmentCode IS NULL OR ol.shipmentCode LIKE CONCAT('%', :shipmentCode, '%'))
    """)
    List<OrderLinkPending> findOrderLinkPendingDTO(
            @Param("purchaseIds") List<Long> purchaseIds,
            @Param("status") OrderLinkStatus status,
            @Param("shipmentCode") String shipmentCode
    );
    @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending(
        ol.linkId,
        ol.productName,
        ol.quantity,
        ol.shipmentCode,
        ol.shipWeb,
        ol.website,
        ol.classify,
        ol.purchaseImage,
        ol.trackingCode,
        ol.status,
        c.customerCode,
        c.name,
        s.staffCode,
        s.name,
        ol.purchase.purchaseId
    )
    FROM OrderLinks ol
    JOIN ol.orders o
    JOIN o.customer c
    JOIN o.staff s
    WHERE ol.purchase.purchaseId IN :purchaseIds
      AND (:status IS NULL OR ol.status = :status)
""")
List<OrderLinkPending> findOrderLinkPendingWithoutShipmentCode(
        @Param("purchaseIds") List<Long> purchaseIds,
        @Param("status") OrderLinkStatus status
);
@Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.PurchaseSummary(

        COUNT(ol),

        SUM(CASE
                WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.CHO_MUA
                THEN 1 ELSE 0
            END),

        SUM(CASE
                WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA
                THEN 1 ELSE 0
            END),

        SUM(CASE
                WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA
                 AND ol.shipmentCode IS NOT NULL
                 AND ol.shipmentCode <> ''
                 AND ol.purchase IS NOT NULL
                THEN 1 ELSE 0
            END),

        SUM(CASE
                WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA
                 AND (ol.shipmentCode IS NULL OR ol.shipmentCode = '')
                THEN 1 ELSE 0
            END)
    )
    FROM OrderLinks ol
    JOIN ol.orders o
    WHERE o.orderType = com.tiximax.txm.Enums.OrderType.MUA_HO
    AND o.status IN (
    com.tiximax.txm.Enums.OrderStatus.CHO_MUA,
    com.tiximax.txm.Enums.OrderStatus.CHO_NHAP_KHO_NN
        )
      AND (
            :routeIds IS NULL
            OR o.route.routeId IN :routeIds
          )
      AND ol.status IN (
            com.tiximax.txm.Enums.OrderLinkStatus.CHO_MUA,
            com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA,
            com.tiximax.txm.Enums.OrderLinkStatus.MUA_SAU
      )
""")
PurchaseSummary getPurchaseSummary(
        @Param("routeIds") List<Long> routeIds
);

@Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.ExchangeMoneySummary(

        COUNT(ol),

        SUM(CASE
                WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.CHO_MUA
                THEN 1 ELSE 0
            END),

        SUM(CASE
                WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA
                THEN 1 ELSE 0
            END)
    )
    FROM OrderLinks ol
    JOIN ol.orders o
    WHERE o.orderType = com.tiximax.txm.Enums.OrderType.CHUYEN_TIEN
    AND o.status IN (
    com.tiximax.txm.Enums.OrderStatus.CHO_MUA,
    com.tiximax.txm.Enums.OrderStatus.CHO_NHAP_KHO_NN
        )
      AND (
            :routeIds IS NULL
            OR o.route.routeId IN :routeIds
          )
      AND ol.status IN (
            com.tiximax.txm.Enums.OrderLinkStatus.CHO_MUA,
            com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA
      )
""")
ExchangeMoneySummary getExchangeSummary(
        @Param("routeIds") List<Long> routeIds
);

        @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.Order.OrderLinkSummaryDTO(
                ol.linkId,
                ol.productName,
                ol.quantity,
                ol.productLink,
                ol.note, 
                ol.shipmentCode,
                ol.priceWeb,
                ol.shipWeb,
                ol.totalWeb,
                ol.purchaseFee,
                ol.extraCharge,
                ol.finalPriceVnd,
                ol.website,
                ol.classify,
                ol.purchaseImage,
                ol.trackingCode,
                ol.status,
                ol.groupTag,
           
                ol.orders.orderId
        )
        FROM OrderLinks ol
        WHERE ol.orders.orderId IN :orderIds
        """)
        List<OrderLinkSummaryDTO> findOrderLinkSummaryByOrderIds(
                @Param("orderIds") List<Long> orderIds
        );

@Query(
    value = """
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.PurchaseDetailDashboard(

            o.customer.customerCode,
            o.customer.name,
            o.staff.staffCode,
            o.staff.name,

            COUNT(ol),

            SUM(CASE
                    WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.CHO_MUA
                    THEN 1 ELSE 0
                END),

            SUM(CASE
                    WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA
                    THEN 1 ELSE 0
                END),

            SUM(CASE
                    WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA
                     AND ol.shipmentCode IS NOT NULL
                     AND ol.shipmentCode <> ''
                     AND ol.purchase IS NOT NULL
                    THEN 1 ELSE 0
                END),

            SUM(CASE
                    WHEN ol.status = com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA
                     AND (ol.shipmentCode IS NULL OR ol.shipmentCode = '')
                    THEN 1 ELSE 0
                END)
        )
        FROM OrderLinks ol
        JOIN ol.orders o
        WHERE o.orderType = com.tiximax.txm.Enums.OrderType.MUA_HO
          AND o.status IN (
                com.tiximax.txm.Enums.OrderStatus.CHO_MUA,
                com.tiximax.txm.Enums.OrderStatus.CHO_NHAP_KHO_NN
          )
          AND (
                :routeIds IS NULL
                OR o.route.routeId IN :routeIds
          )
          AND ol.status IN (
                com.tiximax.txm.Enums.OrderLinkStatus.CHO_MUA,
                com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA,
                com.tiximax.txm.Enums.OrderLinkStatus.MUA_SAU
          )
        GROUP BY
            o.customer.customerCode,
            o.customer.name,
            o.staff.staffCode,
            o.staff.name
    """,
    countQuery = """
        SELECT COUNT(DISTINCT
            o.customer.customerCode,
            o.staff.staffCode
        )
        FROM OrderLinks ol
        JOIN ol.orders o
        WHERE o.orderType = com.tiximax.txm.Enums.OrderType.MUA_HO
          AND o.status IN (
                com.tiximax.txm.Enums.OrderStatus.CHO_MUA,
                com.tiximax.txm.Enums.OrderStatus.CHO_NHAP_KHO_NN
          )
          AND (
                :routeIds IS NULL
                OR o.route.routeId IN :routeIds
          )
          AND ol.status IN (
                com.tiximax.txm.Enums.OrderLinkStatus.CHO_MUA,
                com.tiximax.txm.Enums.OrderLinkStatus.DA_MUA,
                com.tiximax.txm.Enums.OrderLinkStatus.MUA_SAU
          )
    """
)
Page<PurchaseDetailDashboard> getPurchaseDetailDashboard(
        @Param("routeIds") List<Long> routeIds,
        Pageable pageable
);


}

