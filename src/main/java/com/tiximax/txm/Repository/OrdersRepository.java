package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import com.tiximax.txm.Model.DTOResponse.Customer.InactiveCustomerProjection;
import com.tiximax.txm.Model.DTOResponse.DashBoard.InventoryDaily;
import com.tiximax.txm.Model.DTOResponse.DashBoard.LocationSummary;
import com.tiximax.txm.Model.DTOResponse.DashBoard.PackedSummary;
import com.tiximax.txm.Model.DTOResponse.DashBoard.PendingSummary;
import com.tiximax.txm.Model.DTOResponse.Order.OrderInfo;
import com.tiximax.txm.Model.DTOResponse.Order.OrderLinkRefund;
import com.tiximax.txm.Model.DTOResponse.Order.OrderSummaryDTO;
import com.tiximax.txm.Model.DTOResponse.Order.RefundResponse;
import com.tiximax.txm.Model.DTOResponse.Order.TopByWeightAndOrderType;
import com.tiximax.txm.Model.EnumFilter.ShipStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Orders findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    List<Orders> findAllByOrderCodeIn(List<String> orderCodes);

//    @Query("SELECT o FROM Orders o WHERE :status IS NULL OR o.status = :status")
    @Query("SELECT o FROM Orders o WHERE o.status = :status")
    Page<Orders> findByStatus(@Param("status") OrderStatus status, Pageable pageable);

        @Query(value = "SELECT DISTINCT o FROM Orders o " +
                "LEFT JOIN FETCH o.customer c " +
                "WHERE o.status IN :statuses",
        countQuery = "SELECT COUNT(o) FROM Orders o WHERE o.status IN :statuses")
        Page<Orders> findByStatuses(@Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);



//    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId AND (:status IS NULL OR o.status = :status)")
    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId AND o.status = :status")
    Page<Orders> findByStaffAccountIdAndStatus(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

//    @Query("SELECT o FROM Orders o WHERE o.route.routeId IN :routeIds AND (:status IS NULL OR o.status = :status)")
    @Query("SELECT o FROM Orders o WHERE o.route.routeId IN :routeIds AND o.status = :status")
    Page<Orders> findByRouteRouteIdInAndStatus(@Param("routeIds") Set<Long> routeIds, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Orders o " +
            "LEFT JOIN FETCH o.payments p " +
            "LEFT JOIN FETCH p.relatedOrders ro " +
            "WHERE o.staff.accountId = :staffId AND o.status = :status")
    Page<Orders> findByStaffAccountIdAndStatusForPayment(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

@Query("""
   SELECT DISTINCT o FROM Orders o 
   LEFT JOIN o.payments p 
   LEFT JOIN p.relatedOrders ro 
   WHERE o.status = :status
   AND (:orderCode IS NULL 
        OR o.orderCode ILIKE CONCAT('%', CAST(:orderCode AS string), '%')
   )
""")
Page<Orders> findByStatusForPayment(
        @Param("status") OrderStatus status,
        @Param("orderCode") String orderCode,
        Pageable pageable
);



    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.orderLinks WHERE o.route.routeId IN :routeIds AND o.status = :status")
    Page<Orders> findByRouteRouteIdInAndStatusWithLinks(@Param("routeIds") Set<Long> routeIds, @Param("status") OrderStatus status, Pageable pageable);

    long countByStaffAccountIdAndStatus(Long staffId, OrderStatus status);

    @Query("SELECT o FROM Orders o WHERE o.customer.customerCode = :customerCode AND o.status = :status")
    List<Orders> findByCustomerCodeAndStatus(@Param("customerCode") String customerCode, @Param("status") OrderStatus status);

List<Orders> findByCustomerCustomerCodeAndStatusIn(String customerCode, List<OrderStatus> statuses);

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.warehouses w LEFT JOIN FETCH w.orderLinks WHERE o.status = :status")
    Page<Orders> findByStatusWithWarehousesAndLinks(@Param("status") OrderStatus status, Pageable pageable);

    Page<Orders> findByStatusInAndWarehouses_Location_LocationId(List<OrderStatus> statuses, Long locationId, Pageable pageable);
@Query("""
    SELECT DISTINCT o
    FROM Orders o
    WHERE o.route.routeId IN :routeIds
      AND o.status = :status
      AND o.orderType = :orderType
      AND (
           :orderCode IS NULL
           OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', CAST(:orderCode AS string), '%'))
      )
      AND (
           :customerCode IS NULL
           OR LOWER(o.customer.customerCode) LIKE LOWER(CONCAT('%', CAST(:customerCode AS string), '%'))
      )
""")
Page<Orders> findByRouteAndStatusAndTypeWithSearch(
        @Param("routeIds") Set<Long> routeIds,
        @Param("status") OrderStatus status,
        @Param("orderType") OrderType orderType,
        @Param("orderCode") String orderCode,
        @Param("customerCode") String customerCode,
        Pageable pageable
);

@Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.Order.OrderSummaryDTO(
        o.orderId,
        o.orderCode,
        o.orderType,
        o.status,
        o.createdAt,
        o.exchangeRate,
        o.finalPriceOrder,
        o.checkRequired,
        o.pinnedAt
    )
    FROM Orders o
    WHERE o.route.routeId IN :routeIds
      AND o.status = :status
      AND o.orderType = :orderType
      AND (
           :orderCode IS NULL
           OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :orderCode, '%'))
      )
      AND (
           :customerCode IS NULL
           OR LOWER(o.customer.customerCode) LIKE LOWER(CONCAT('%', :customerCode, '%'))
      )
""")
Page<OrderSummaryDTO> findOrderSummaryForPurchaser(
        @Param("routeIds") Set<Long> routeIds,
        @Param("status") OrderStatus status,
        @Param("orderType") OrderType orderType,
        @Param("orderCode") String orderCode,
        @Param("customerCode") String customerCode,
        Pageable pageable
);

@Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.Order.OrderSummaryDTO(
        o.orderId,
        o.orderCode,
        o.orderType,
        o.status,
        o.createdAt,
        o.exchangeRate,
        o.finalPriceOrder,
        o.checkRequired,
        o.pinnedAt
    )
    FROM Orders o
    WHERE o.route.routeId IN :routeIds
      AND o.status = :status
      AND o.orderType = :orderType
""")
Page<OrderSummaryDTO> findOrderSummaryNoSearch(
        @Param("routeIds") Set<Long> routeIds,
        @Param("status") OrderStatus status,
        @Param("orderType") OrderType orderType,
        Pageable pageable
);

@Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.Order.OrderSummaryDTO(
        o.orderId,
        o.orderCode,
        o.orderType,
        o.status,
        o.createdAt,
        o.exchangeRate,
        o.finalPriceOrder,
        o.checkRequired,
        o.pinnedAt
    )
    FROM Orders o
    WHERE o.route.routeId IN :routeIds
      AND o.status = :status
      AND o.orderType = :orderType
      AND LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :orderCode, '%'))
""")
Page<OrderSummaryDTO> findOrderSummaryByOrderCode(
        Set<Long> routeIds,
        OrderStatus status,
        OrderType orderType,
        String orderCode,
        Pageable pageable
);

@Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.Order.OrderSummaryDTO(
        o.orderId,
        o.orderCode,
        o.orderType,
        o.status,
        o.createdAt,
        o.exchangeRate,
        o.finalPriceOrder,
        o.checkRequired,
        o.pinnedAt
    )
    FROM Orders o
    JOIN o.customer c
    WHERE o.route.routeId IN :routeIds
      AND o.status = :status
      AND o.orderType = :orderType
      AND LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :customerCode, '%'))
""")
Page<OrderSummaryDTO> findOrderSummaryByCustomerCode(
        Set<Long> routeIds,
        OrderStatus status,
        OrderType orderType,
        String customerCode,
        Pageable pageable
);




    List<Orders> findByStaff_AccountIdAndRoute_RouteIdInAndCreatedAtBetween(Long accountId, Set<Long> routeIds, LocalDateTime startDate, LocalDateTime endDate);

    List<Orders> findByStaff_AccountIdAndRoute_RouteIdIn(Long accountId, Set<Long> routeIds);

    Page<Orders> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.status IN :statuses " +
            "AND o.leftoverMoney IS NOT NULL " +
            "AND o.leftoverMoney < :threshold")
    Page<Orders> findByStatusInAndLeftoverMoneyLessThan(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId " +
            "AND o.status IN :statuses " +
            "AND o.leftoverMoney IS NOT NULL " +
            "AND o.leftoverMoney < :threshold")
    Page<Orders> findByStaffAccountIdAndStatusInAndLeftoverMoneyLessThan(
            @Param("staffId") Long staffId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT o FROM Orders o
    JOIN FETCH o.orderLinks ol
    WHERE o.route.routeId IN :routeIds
      AND o.status = 'DANG_XU_LY'
      AND o.orderType = :orderType
      AND EXISTS (
        SELECT 1 FROM OrderLinks link
        WHERE link.orders = o AND link.status = 'MUA_SAU'
      )
    """)
    Page<Orders> findProcessingOrdersWithBuyLaterLinks(
            @Param("routeIds") Set<Long> routeIds,
            @Param("orderType") OrderType orderType,
            Pageable pageable
    );
@Query("""
    SELECT o
    FROM Orders o
    JOIN o.orderLinks ol
    WHERE ol.status IN :statuses
    AND (
        :shipmentCode IS NULL 
        OR LOWER(CAST(ol.shipmentCode AS string)) 
            LIKE LOWER(CAST(CONCAT('%', :shipmentCode, '%') AS string))
    )
    AND (
        :customerCode IS NULL 
        OR LOWER(CAST(o.customer.customerCode AS string)) 
            LIKE LOWER(CAST(CONCAT('%', :customerCode, '%') AS string))
    )
    ORDER BY
        CASE
            WHEN ol.status = 'DA_NHAP_KHO_VN' THEN 0
            WHEN ol.status = 'CHO_GIAO' THEN 1
            ELSE 2
        END,
        ol.status
    """)
Page<Orders> filterOrdersByLinkStatus(
        @Param("statuses") List<OrderLinkStatus> statuses,
        @Param("shipmentCode") String shipmentCode,
        @Param("customerCode") String customerCode,
        Pageable pageable
);


@Query("""
    SELECT DISTINCT o
    FROM Orders o
    JOIN o.orderLinks ol
    WHERE ol.status IN :statuses

    AND (
        :shipmentCode IS NULL 
        OR LOWER(CAST(ol.shipmentCode AS string)) 
            LIKE LOWER(CAST(CONCAT('%', :shipmentCode, '%') AS string))
    )

    AND (
        :customerCode IS NULL 
        OR LOWER(CAST(o.customer.customerCode AS string)) 
            LIKE LOWER(CAST(CONCAT('%', :customerCode, '%') AS string))
    )

    AND (
        :routeIds IS NULL 
        OR o.route.routeId IN :routeIds
    )
    """)
Page<Orders> filterOrdersByLinkStatusAndRoutes(
        @Param("statuses") List<OrderLinkStatus> statuses,
        @Param("shipmentCode") String shipmentCode,
        @Param("customerCode") String customerCode,
        @Param("routeIds") Set<Long> routeIds,
        Pageable pageable
);



    List<Orders> findByStaff_AccountIdAndCreatedAtBetween(Long accountId, LocalDateTime startDate, LocalDateTime endDate);
        Page<Orders> findByStaffAccountId(Long accountId, Pageable pageable);
        Page<Orders> findByRouteRouteIdIn(Set<Long> routeIds, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Orders> findByCustomerAndLeftoverMoneyGreaterThan(Customer customer, BigDecimal zero);

//    @Query("SELECT o FROM Orders o " +
//            "WHERE o.leftoverMoney IS NOT NULL " +
//            "AND o.leftoverMoney < :threshold " +
//            "AND EXISTS (" +
//            "   SELECT 1 FROM OrderLinks ol " +
//            "   WHERE ol.orders = o AND ol.status = 'DA_HUY'" +
//            ")")
//    Page<Orders> findOrdersWithRefundableCancelledLinks(
//            @Param("threshold") BigDecimal threshold,
//            Pageable pageable
//    );

    @Query("""
    SELECT NEW com.tiximax.txm.Model.DTOResponse.Order.RefundResponse(
        o.orderId,
        o.orderCode,
        o.orderType,
        o.status,
        o.createdAt,
        o.exchangeRate,
        o.finalPriceOrder,
        o.leftoverMoney,
        o.customer.name,
        o.staff.name
    )
    FROM Orders o
    JOIN o.orderLinks ol
    WHERE o.leftoverMoney IS NOT NULL
      AND o.leftoverMoney < :threshold
      AND (:orderCode IS NULL OR o.orderCode = :orderCode)
      AND ol.status = 'DA_HUY'
    GROUP BY
        o.orderId,
        o.orderCode,
        o.orderType,
        o.status,
        o.createdAt,
        o.exchangeRate,
        o.finalPriceOrder,
        o.leftoverMoney,
        o.customer.name,
        o.staff.name
""")
    Page<RefundResponse> findOrdersWithRefundableCancelledLinks(
            @Param("orderCode") String orderCode,
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

    @Query("""
    SELECT NEW com.tiximax.txm.Model.DTOResponse.Order.RefundResponse(
        o.orderId,
        o.orderCode,
        o.orderType,
        o.status,
        o.createdAt,
        o.exchangeRate,
        o.finalPriceOrder,
        o.leftoverMoney,
        o.customer.name,
        o.staff.name
    )
    FROM Orders o
    JOIN o.orderLinks ol
    WHERE o.leftoverMoney IS NOT NULL
      AND o.leftoverMoney < :threshold
      AND o.staff.accountId = :staffId
      AND (:orderCode IS NULL OR o.orderCode = :orderCode)
      AND ol.status = 'DA_HUY'
    GROUP BY
        o.orderId,
        o.orderCode,
        o.orderType,
        o.status,
        o.createdAt,
        o.exchangeRate,
        o.finalPriceOrder,
        o.leftoverMoney,
        o.customer.name,
        o.staff.name
""")
    Page<RefundResponse> findByStaffIdAndRefundableCancelledLinks(
            @Param("staffId") Long staffId,
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

//    @Query("SELECT o FROM Orders o " +
//            "WHERE o.staff.accountId = :staffId " +
//            "AND o.leftoverMoney IS NOT NULL " +
//            "AND o.leftoverMoney < :threshold " +
//            "AND EXISTS (" +
//            "   SELECT 1 FROM OrderLinks ol " +
//            "   WHERE ol.orders = o AND ol.status = 'DA_HUY'" +
//            ")")
//    Page<RefundResponse> findByStaffIdAndRefundableCancelledLinks(
//            @Param("staffId") Long staffId,
//            @Param("threshold") BigDecimal threshold,
//            Pageable pageable
//    );

    @Query("""
    SELECT DISTINCT o FROM Orders o
    JOIN o.orderLinks ol
    WHERE o.staff.accountId = :staffId
      AND (ol.shipmentCode IS NULL OR TRIM(ol.shipmentCode) = '')
    """)
    Page<Orders> findOrdersWithEmptyShipmentCodeByStaff(
            @Param("staffId") Long staffId,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT o FROM Orders o
    LEFT JOIN FETCH o.orderLinks ol
    WHERE (
        UPPER(o.orderCode) LIKE UPPER(CONCAT('%', :keyword, '%'))
        OR (ol.shipmentCode IS NOT NULL AND UPPER(ol.shipmentCode) LIKE UPPER(CONCAT('%', :keyword, '%')))
    )
    AND (
        :isAdminOrManager = true 
        OR o.staff.accountId = :staffId
    )
    """)
    Page<Orders> searchOrdersByCodeOrShipment(
            @Param("keyword") String keyword,
            @Param("staffId") Long staffId,
            @Param("isAdminOrManager") boolean isAdminOrManager,
            Pageable pageable
    );
//    @Query("""
//        SELECT o FROM Orders o
//        LEFT JOIN o.customer c
//        LEFT JOIN o.orderLinks ol
//        WHERE
//            (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
//            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
//            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
//    """)
//    Page<Orders> findAllWithFilters(
//            @Param("shipmentCode") String shipmentCode,
//            @Param("customerCode") String customerCode,
//            @Param("orderCode") String orderCode,
//            Pageable pageable
//    );

    @Query("""
        SELECT DISTINCT new com.tiximax.txm.Model.DTOResponse.Order.OrderInfo(
            o.orderId,
            o.orderCode,
            o.orderType,
            o.status,
            c.customerCode,
            c.name,
            s.name,
            o.exchangeRate,
            o.finalPriceOrder,
            o.createdAt
        )
        FROM Orders o
        LEFT JOIN o.customer c
        LEFT JOIN o.staff s
        LEFT JOIN o.orderLinks ol WITH (:shipmentCode IS NOT NULL)
        WHERE
            (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
        """)
    Page<OrderInfo> findAllWithFilters(@Param("shipmentCode") String shipmentCode,
                                       @Param("customerCode") String customerCode,
                                       @Param("orderCode") String orderCode,
                                       Pageable pageable);

//    @Query("""
//        SELECT o FROM Orders o
//        LEFT JOIN o.customer c
//        LEFT JOIN o.orderLinks ol
//        WHERE
//            o.staff.accountId = :accountId
//            AND (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
//            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
//            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
//    """)
//    Page<Orders> findByStaffAccountIdWithFilters(
//            @Param("accountId") Long accountId,
//            @Param("shipmentCode") String shipmentCode,
//            @Param("customerCode") String customerCode,
//            @Param("orderCode") String orderCode,
//            Pageable pageable
//    );

    @Query("""
        SELECT DISTINCT new com.tiximax.txm.Model.DTOResponse.Order.OrderInfo(
            o.orderId,
            o.orderCode,
            o.orderType,
            o.status,
            c.customerCode,
            c.name,
            s.name,
            o.exchangeRate,
            o.finalPriceOrder,
            o.createdAt
        )
        FROM Orders o
        JOIN o.customer c
        JOIN o.staff s
        LEFT JOIN o.orderLinks ol WITH (:shipmentCode IS NOT NULL)
        WHERE
            o.staff.accountId = :accountId
            AND (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
        """)
    Page<OrderInfo> findByStaffAccountIdWithFilters(
                                       @Param("accountId") Long accountId,
                                       @Param("shipmentCode") String shipmentCode,
                                       @Param("customerCode") String customerCode,
                                       @Param("orderCode") String orderCode,
                                       Pageable pageable);

//    @Query("""
//        SELECT o FROM Orders o
//        LEFT JOIN o.customer c
//        LEFT JOIN o.orderLinks ol
//        WHERE
//            o.route.routeId IN :routeIds
//            AND (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
//            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
//            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
//    """)
//    Page<Orders> findByRouteRouteIdInWithFilters(
//            @Param("routeIds") Set<Long> routeIds,
//            @Param("shipmentCode") String shipmentCode,
//            @Param("customerCode") String customerCode,
//            @Param("orderCode") String orderCode,
//            Pageable pageable
//    );

    @Query("""
        SELECT DISTINCT new com.tiximax.txm.Model.DTOResponse.Order.OrderInfo(
            o.orderId,
            o.orderCode,
            o.orderType,
            o.status,
            c.customerCode,
            c.name,
            s.name,
            o.exchangeRate,
            o.finalPriceOrder,
            o.createdAt
        )
        FROM Orders o
        LEFT JOIN o.customer c
        LEFT JOIN o.staff s
        LEFT JOIN o.orderLinks ol WITH (:shipmentCode IS NOT NULL)
        WHERE
            o.route.routeId IN :routeIds
            AND (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
        """)
    Page<OrderInfo> findByRouteRouteIdInWithFilters(
            @Param("routeIds") Set<Long> routeIds,
            @Param("shipmentCode") String shipmentCode,
            @Param("customerCode") String customerCode,
            @Param("orderCode") String orderCode,
            Pageable pageable
    );

    @Query("SELECT MONTH(o.createdAt), COUNT(o) FROM Orders o WHERE YEAR(o.createdAt) = :year GROUP BY MONTH(o.createdAt)")
    List<Object[]> countOrdersByMonth(@Param("year") int year);

    @Query("SELECT SUM(o.leftoverMoney) FROM Orders o WHERE o.leftoverMoney > 0 AND o.createdAt BETWEEN :start AND :end")
    BigDecimal sumLeftoverMoneyPositive(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(ABS(o.leftoverMoney)) FROM Orders o WHERE o.leftoverMoney < 0 AND o.createdAt BETWEEN :start AND :end")
    BigDecimal sumLeftoverMoneyNegative(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(o.leftoverMoney) FROM Orders o WHERE o.leftoverMoney > 0")
    BigDecimal sumLeftoverMoneyPositiveAll();

    @Query("SELECT SUM(ABS(o.leftoverMoney)) FROM Orders o WHERE o.leftoverMoney < 0")
    BigDecimal sumLeftoverMoneyNegativeAll();

    @Query(value = """
    SELECT 
        r.name AS route_name,
        COALESCE(COUNT(DISTINCT o.order_id), 0) AS total_orders,
        COALESCE(COUNT(DISTINCT ol.link_id), 0) AS total_links
    FROM route r
    LEFT JOIN orders o ON o.route_id = r.route_id 
        AND o.created_at BETWEEN :start AND :end
    LEFT JOIN order_links ol ON ol.order_id = o.order_id 
    GROUP BY r.route_id, r.name
    ORDER BY total_orders DESC, total_links DESC
    """, nativeQuery = true)
    List<Object[]> sumOrdersAndLinksByRouteNativeRaw(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
    SELECT COUNT(ol)
    FROM OrderLinks ol
    WHERE ol.orders.orderId = :orderId
      AND ol.status = 'CHO_NHAP_KHO_VN'
        """)
     int countNotImported(@Param("orderId") Long orderId);

      @Query("""
    SELECT COUNT(ol)
    FROM OrderLinks ol
    WHERE ol.orders.orderId = :orderId
      AND ol.status = 'DA_NHAP_KHO_VN'
        """)
     int countImported(@Param("orderId") Long orderId);

    @Query("SELECT o FROM Orders o " +
            "LEFT JOIN FETCH o.orderLinks ol " +
            "LEFT JOIN FETCH o.warehouses w " +
            "LEFT JOIN FETCH o.feedback f " +
            "WHERE o.staff.accountId = :staffId " +
            "AND o.createdAt BETWEEN :start AND :end")
    List<Orders> findByStaffIdWithDetailsForPerformance(
            @Param("staffId") Long staffId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = """
           With goods_by_staff_route AS (
                SELECT
                    o.route_id,
                    o.staff_id,
                    COALESCE(SUM(ol.total_web), 0) AS total_goods
                FROM order_links ol
                LEFT JOIN warehouse w
                    ON ol.warehouse_id = w.warehouse_id
                LEFT JOIN purchases p
                    ON ol.purchase_id = p.purchase_id
                LEFT JOIN orders o
                    ON o.order_id = COALESCE(w.order_id, p.order_id)
                WHERE
                    COALESCE(w.created_at, p.purchase_time) >= :start
                    AND COALESCE(w.created_at, p.purchase_time) < :end
                    AND (:routeId IS NULL OR o.route_id = :routeId)
                    AND ol.status NOT IN ('CHO_MUA', 'DA_MUA', 'DA_HUY', 'MUA_SAU', 'DAU_GIA_THANH_CONG')
                GROUP BY o.route_id, o.staff_id
            )
            SELECT
                COALESCE(r.name, 'Không xác định') AS route_name,
                s.staff_code AS staff_code,
                a.name AS staff_name,
                COALESCE(g.total_goods, 0) AS total_goods
            FROM orders o
            JOIN staff s ON o.staff_id = s.account_id
            JOIN account a ON s.account_id = a.account_id
            LEFT JOIN route r ON o.route_id = r.route_id
            LEFT JOIN weight_by_staff_route w
                   ON w.route_id = o.route_id
                  AND w.staff_id = o.staff_id
            LEFT JOIN goods_by_staff_route g
                   ON g.route_id = o.route_id
                  AND g.staff_id = o.staff_id
            WHERE (:routeId IS NULL OR o.route_id = :routeId)
            GROUP BY
                r.name,
                s.staff_code,
                a.name,
                g.total_goods
            ORDER BY
                route_name ASC,
                total_goods DESC;
    """, nativeQuery = true)
    List<Object[]> staffKPIByRoute(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId);

    @Query(value = """
            WITH
                 payment_ship_qualified AS (
                     SELECT DISTINCT
                         p.payment_id,
                         w.warehouse_id,
                         w.net_weight,
                         r.route_id,
                         r.name AS route_name,
                         r.min_weight,
                         o.staff_id
                     FROM payment p
                     INNER JOIN payment_orders po ON po.payment_id = p.payment_id
                     INNER JOIN orders o ON o.order_id = po.order_id
                     INNER JOIN warehouse w ON w.order_id = o.order_id
                     INNER JOIN route r ON o.route_id = r.route_id
                     WHERE p.status = 'DA_THANH_TOAN_SHIP'
                       AND p.staff_id = o.staff_id
                       AND p.action_at >= :start
                       AND p.action_at < :end
                       AND (:routeId IS NULL OR r.route_id = :routeId)
                 ),
                 raw_weight_per_payment AS (
                     SELECT
                         route_id,
                         staff_id,
                         payment_id,
                         SUM(net_weight) AS raw_total_weight,
                         MIN(min_weight) AS min_weight
                     FROM payment_ship_qualified
                     GROUP BY route_id, staff_id, payment_id
                 ),
                 adjusted_weight_by_staff_route AS (
                     SELECT
                         route_id,
                         staff_id,
                         SUM(
                             CASE
                                 WHEN raw_total_weight < COALESCE(min_weight, 0)
                                 THEN COALESCE(min_weight, raw_total_weight)
                                 ELSE raw_total_weight
                             END
                         ) AS total_shipping_weight_adjusted
                     FROM raw_weight_per_payment
                     GROUP BY route_id, staff_id
                 ),
                 goods_by_staff_route AS (
                     SELECT
                         o.route_id,
                         o.staff_id,
                         COALESCE(SUM(ol.total_web), 0) AS total_goods
                     FROM order_links ol
                     LEFT JOIN warehouse w ON ol.warehouse_id = w.warehouse_id
                     LEFT JOIN purchases p ON ol.purchase_id = p.purchase_id
                     LEFT JOIN orders o ON o.order_id = COALESCE(w.order_id, p.order_id)
                     WHERE COALESCE(w.created_at, p.purchase_time) >= :start
                       AND COALESCE(w.created_at, p.purchase_time) < :end
                       AND (:routeId IS NULL OR o.route_id = :routeId)
                       AND ol.status NOT IN ('CHO_MUA', 'DA_MUA', 'DA_HUY', 'MUA_SAU', 'DAU_GIA_THANH_CONG')
                     GROUP BY o.route_id, o.staff_id
                 ),
                 partial_weight_by_staff_route AS (
                     SELECT
                         r.route_id,
                         r.name AS route_name,
                         o.staff_id,
                         ROUND(CAST(SUM(COALESCE(ps.collect_weight, 0)) AS numeric), 3) AS tong_collect_weight_partial
                     FROM partial_shipment ps
                     INNER JOIN orders o ON o.order_id = ps.order_id
                     INNER JOIN route r ON o.route_id = r.route_id
                     INNER JOIN staff s ON o.staff_id = s.account_id
                     INNER JOIN account a ON s.account_id = a.account_id
                     WHERE ps.collect_weight IS NOT NULL
                       AND ps.collect_weight > 0
                       AND ps.shipment_date >= :start
                       AND ps.shipment_date < :end
                       AND (:routeId IS NULL OR r.route_id = :routeId)
                       AND a.role IN ('STAFF_SALE', 'LEAD_SALE')
                     GROUP BY r.route_id, r.name, o.staff_id
                     HAVING SUM(COALESCE(ps.collect_weight, 0)) > 0
                 )
             SELECT
                 COALESCE(r.name, 'Không xác định') AS route_name,
                 s.staff_code,
                 a.name AS staff_name,
                 COALESCE(g.total_goods, 0) AS total_goods,
                 COALESCE(aw.total_shipping_weight_adjusted, 0) AS total_shipping_weight_adjusted,
                 COALESCE(pw.tong_collect_weight_partial, 0) AS tong_collect_weight_partial
             FROM orders o
             JOIN staff s ON o.staff_id = s.account_id
             JOIN account a ON s.account_id = a.account_id
             LEFT JOIN route r ON o.route_id = r.route_id
             LEFT JOIN adjusted_weight_by_staff_route aw
                    ON aw.route_id = o.route_id
                   AND aw.staff_id = o.staff_id
             LEFT JOIN goods_by_staff_route g
                    ON g.route_id = o.route_id
                   AND g.staff_id = o.staff_id
             LEFT JOIN partial_weight_by_staff_route pw
                    ON pw.route_id = o.route_id
                   AND pw.staff_id = o.staff_id
             WHERE a.role IN ('STAFF_SALE', 'LEAD_SALE')
                AND (:routeId IS NULL OR o.route_id = :routeId)
             GROUP BY
                 r.name,
                 s.staff_code,
                 a.name,
                 g.total_goods,
                 aw.total_shipping_weight_adjusted,
                 pw.tong_collect_weight_partial
             ORDER BY
                 route_name ASC,
                 total_goods DESC;
    """, nativeQuery = true)
    List<Object[]> aggregateStaffKPIByRoute(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId);

    @Query(value = """
        SELECT
            r.name AS route_name,
            COUNT(DISTINCT o.order_id) AS total_orders,
            COUNT(DISTINCT CASE WHEN o.status = 'DA_GIAO' THEN o.order_id END) AS completed_orders,
            COUNT(DISTINCT ol.link_id) AS total_parcels
        FROM orders o
        LEFT JOIN order_links ol ON ol.order_id = o.order_id
        LEFT JOIN route r ON o.route_id = r.route_id
        WHERE o.staff_id = :staffId
          AND o.created_at >= :start
          AND o.created_at < :end
          AND (:routeId IS NULL OR o.route_id = :routeId)
        GROUP BY r.name
        ORDER BY r.name ASC
    """, nativeQuery = true)
    List<Object[]> getOrdersSummary(
            @Param("staffId") Long staffId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId
    );

    @Query(value = """
        SELECT
            r.name AS route_name,
            COALESCE(SUM(ol.total_web), 0) AS total_goods
        FROM order_links ol
        LEFT JOIN warehouse w ON ol.warehouse_id = w.warehouse_id
        LEFT JOIN purchases pu ON ol.purchase_id = pu.purchase_id
        LEFT JOIN orders o ON o.order_id = COALESCE(w.order_id, pu.order_id)
        LEFT JOIN route r ON o.route_id = r.route_id
        WHERE o.staff_id = :staffId
          AND COALESCE(w.created_at, pu.purchase_time) >= :start
          AND COALESCE(w.created_at, pu.purchase_time) < :end
          AND (:routeId IS NULL OR o.route_id = :routeId)
          AND ol.status NOT IN ('CHO_MUA', 'DA_MUA', 'DA_HUY', 'MUA_SAU', 'DAU_GIA_THANH_CONG')
        GROUP BY r.name
        ORDER BY r.name ASC
    """, nativeQuery = true)
    List<Object[]> getGoodsValue(
            @Param("staffId") Long staffId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId
    );

    @Query(value = """
    WITH
        payment_ship_qualified AS (
            SELECT DISTINCT
                p.payment_id,
                w.warehouse_id,
                w.net_weight,
                r.route_id,
                r.name AS route_name,
                r.min_weight
            FROM payment p
            INNER JOIN payment_orders po ON po.payment_id = p.payment_id
            INNER JOIN orders o ON o.order_id = po.order_id
            INNER JOIN warehouse w ON w.order_id = o.order_id
            INNER JOIN route r ON o.route_id = r.route_id
            WHERE p.status = 'DA_THANH_TOAN_SHIP'
              AND p.staff_id = :staffId
              AND p.action_at >= :start
              AND p.action_at < :end
              AND (:routeId IS NULL OR r.route_id = :routeId)
        ),
        raw_weight_per_payment AS (
            SELECT
                route_id,
                route_name,
                SUM(net_weight) AS raw_total_weight,
                min_weight
            FROM payment_ship_qualified
            GROUP BY route_id, route_name, payment_id, min_weight
        ),
        adjusted_weight AS (
            SELECT
                route_name,
                SUM(
                    CASE
                        WHEN raw_total_weight < COALESCE(min_weight, 0)
                        THEN COALESCE(min_weight, raw_total_weight)
                        ELSE raw_total_weight
                    END
                ) AS total_net_weight
            FROM raw_weight_per_payment
            GROUP BY route_name
        ),
        partial_weight_by_route AS (
            SELECT
                r.name AS route_name,
                ROUND(CAST(SUM(COALESCE(ps.collect_weight, 0)) AS numeric), 3) AS total_partial_weight
            FROM partial_shipment ps
            INNER JOIN orders o ON o.order_id = ps.order_id
            INNER JOIN route r ON o.route_id = r.route_id
            WHERE ps.collect_weight IS NOT NULL
              AND ps.collect_weight > 0
              AND o.staff_id = :staffId
              AND ps.shipment_date >= :start
              AND ps.shipment_date < :end
              AND (:routeId IS NULL OR r.route_id = :routeId)
            GROUP BY r.name
        )
    SELECT
        aw.route_name,
        aw.total_net_weight,
        COALESCE(pw.total_partial_weight, 0) AS total_partial_weight
    FROM adjusted_weight aw
    LEFT JOIN partial_weight_by_route pw
           ON pw.route_name = aw.route_name
    ORDER BY aw.route_name ASC
""", nativeQuery = true)
    List<Object[]> getShippingWeight(
            @Param("staffId") Long staffId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId
    );

    @Query(value = """
        SELECT
            r.name AS route_name,
            COUNT(*) AS bad_feedback_count
        FROM feedback f
        INNER JOIN orders o ON f.order_id = o.order_id
        LEFT JOIN route r ON o.route_id = r.route_id
        WHERE o.staff_id = :staffId
          AND f.rating < 3
          AND o.created_at >= :start
          AND o.created_at < :end
          AND (:routeId IS NULL OR o.route_id = :routeId)
        GROUP BY r.name
        ORDER BY r.name ASC
    """, nativeQuery = true)
    List<Object[]> getBadFeedbacks(
            @Param("staffId") Long staffId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId
    );

    @Query(value = """
        SELECT
            COUNT(DISTINCT a.account_id) AS new_customers_in_period
        FROM customer c
        INNER JOIN account a ON a.account_id = c.account_id
        WHERE c.staff_id = :staffId
          AND a.created_at >= :start
          AND a.created_at < :end
    """, nativeQuery = true)
    Object[] getNewCustomers(
            @Param("staffId") Long staffId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = """
        SELECT
            s.staff_code,
            a.name AS staff_name,
            s.department
        FROM staff s
        INNER JOIN account a ON s.account_id = a.account_id
        WHERE s.account_id = :staffId
          AND a.role IN ('STAFF_SALE', 'LEAD_SALE')
    """, nativeQuery = true)
    Object[] getStaffBasicInfo(@Param("staffId") Long staffId);

    @EntityGraph(attributePaths = {"orderLinks"})
    Optional<Orders> findByOrderId(Long orderId);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.PendingSummary(
            COUNT(DISTINCT ol.purchase.id),
            COUNT(ol.linkId),
            COUNT(DISTINCT ol.orders.orderId)
        )
        FROM OrderLinks ol
        LEFT JOIN ol.orders o
        LEFT JOIN o.route r
        WHERE ol.status = 'DA_MUA'
          AND (:routeId IS NULL OR r.routeId = :routeId)
        """)
    PendingSummary getPendingSummary(@Param("routeId") Long routeId);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.LocationSummary(
            l.locationId,
            l.name,
            COUNT(DISTINCT ol.purchase.id),
            0.0,
            0.0
        )
        FROM OrderLinks ol
        JOIN ol.orders o
        JOIN o.route r
        JOIN r.warehouseLocations l
        WHERE ol.status = 'DA_MUA'
        GROUP BY l.locationId, l.name
        """)
    List<LocationSummary> getPendingSummaryByLocation();

    @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.PendingSummary(
        COUNT(DISTINCT ol.purchase.id),
        COUNT(ol.linkId),
        COUNT(DISTINCT ol.orders.orderId)
    )
    FROM OrderLinks ol
    JOIN ol.warehouse w
    WHERE ol.status = 'DA_MUA'
      AND w.location.locationId = :locationId
""")
    PendingSummary getPendingSummaryByLocationId(@Param("locationId") Long locationId);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.Order.OrderInfo(
            o.orderId,
            o.orderCode,
            o.orderType,
            o.status,
            c.customerCode,
            c.name,
            s.name,
            o.exchangeRate,
            o.finalPriceOrder,
            o.createdAt
        )
        FROM Orders o
        LEFT JOIN o.customer c
        LEFT JOIN o.staff s
        WHERE o.status = :status
        """)
    Page<OrderInfo> findOrderInfoByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.Order.OrderInfo(
            o.orderId,
            o.orderCode,
            o.orderType,
            o.status,
            c.customerCode,
            c.name,
            s.name,
            o.exchangeRate,
            o.finalPriceOrder,
            o.createdAt
        )
        FROM Orders o
        LEFT JOIN o.customer c
        LEFT JOIN o.staff s
        WHERE o.staff.accountId = :staffId
          AND o.status = :status
        """)
    Page<OrderInfo> findOrderInfoByStaffIdAndStatus(
            @Param("staffId") Long staffId,
            @Param("status") OrderStatus status,
            Pageable pageable);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.Order.OrderInfo(
            o.orderId,
            o.orderCode,
            o.orderType,
            o.status,
            c.customerCode,
            c.name,
            o.exchangeRate,
            o.finalPriceOrder,
            o.createdAt
        )
        FROM Orders o
        LEFT JOIN o.customer c
        WHERE o.route.routeId IN :routeId
          AND o.status = :status
        """)
    Page<OrderInfo> findOrderInfoByRouteIdAndStatus(Long routeId, OrderStatus status, Pageable pageable);

    @Query("""
    SELECT NEW com.tiximax.txm.Model.DTOResponse.Order.OrderLinkRefund(
        ol.linkId,
        ol.productLink,
        ol.productName,
        ol.quantity,
        ol.priceWeb,
        ol.shipWeb,
        ol.totalWeb,
        ol.purchaseFee,
        ol.extraCharge,
        ol.finalPriceVnd,
        ol.trackingCode,
        ol.classify,
        ol.purchaseImage,
        ol.website,
        ol.shipmentCode,
        ol.status,
        ol.note,
        ol.groupTag
    )
    FROM OrderLinks ol
    WHERE ol.orders.orderId = :orderId
      AND ol.status = 'DA_HUY'
""")
    List<OrderLinkRefund> findRefundableCancelledLinksByOrderId(
            @Param("orderId") Long orderId
    );

    @Query("""
    SELECT NEW com.tiximax.txm.Model.DTOResponse.Customer.InactiveCustomerProjection(
        c.accountId,
        c.name,
        MAX(s.accountId),
        MAX(s.name),
        MAX(o.createdAt),
        COUNT(o.id)
    )
    FROM Orders o
    JOIN Customer c ON o.customer.accountId = c.accountId
    JOIN Staff s ON o.staff.accountId = s.accountId
    WHERE (:staffId IS NULL OR s.accountId = :staffId)
    GROUP BY c.accountId, c.name
    HAVING COUNT(o.id) >= 2
       AND MAX(o.createdAt) <= :inactiveDate
    ORDER BY MAX(o.createdAt) DESC
""")
    Page<InactiveCustomerProjection> findInactiveCustomersByStaff(
            @Param("staffId") Long staffId,
            @Param("inactiveDate") LocalDateTime inactiveDate,
            Pageable pageable
    );

    @Query(value = """
    WITH ds AS (
        SELECT
            o.customer_id,
            o.order_type,
            o.staff_id,
            cust_acc.name AS customer_name,
            staff_acc.name AS staff_name,
            SUM(COALESCE(w.net_weight, 0)) AS total_weight
        FROM orders o
        LEFT JOIN order_links ol ON o.order_id = ol.order_id
        LEFT JOIN warehouse w ON w.warehouse_id = ol.warehouse_id
        LEFT JOIN account cust_acc ON o.customer_id = cust_acc.account_id
        LEFT JOIN account staff_acc ON o.staff_id = staff_acc.account_id
        WHERE w.created_at > :startDate
          AND w.created_at < :endDate
        GROUP BY
            o.customer_id,
            o.order_type,
            o.staff_id,
            cust_acc.name,
            staff_acc.name
        HAVING SUM(COALESCE(w.net_weight, 0)) > 0
    ),
    ranked AS (
        SELECT *,
               DENSE_RANK() OVER (PARTITION BY order_type ORDER BY total_weight DESC) AS rank_
        FROM ds
    )
    SELECT
        customer_id   AS customerId,
        customer_name AS customerName,
        staff_id      AS staffId,
        staff_name    AS staffName,
        order_type    AS orderType,
        total_weight  AS totalWeight,
        rank_         AS rank
    FROM ranked
    WHERE rank_ <= :limit
      AND order_type = :orderType
    ORDER BY order_type, rank_
    """, nativeQuery = true)
    List<Object[]> findTopByWeightAndOrderType(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderType") String orderType,
            @Param("limit") int limit
    );

}
