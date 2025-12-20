package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import com.tiximax.txm.Model.EnumFilter.ShipStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Orders findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    List<Orders> findAllByOrderCodeIn(List<String> orderCodes);

    @Query("SELECT o FROM Orders o WHERE :status IS NULL OR o.status = :status")
    Page<Orders> findByStatus(@Param("status") OrderStatus status, Pageable pageable);
    
        @Query(value = "SELECT DISTINCT o FROM Orders o " +
                "LEFT JOIN FETCH o.customer c " +
                "WHERE o.status IN :statuses",
        countQuery = "SELECT COUNT(o) FROM Orders o WHERE o.status IN :statuses")
        Page<Orders> findByStatuses(@Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);



    @Query("SELECT o FROM Orders o WHERE o.staff.accountId = :staffId AND (:status IS NULL OR o.status = :status)")
    Page<Orders> findByStaffAccountIdAndStatus(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Orders o WHERE o.route.routeId IN :routeIds AND (:status IS NULL OR o.status = :status)")
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

    @Query("SELECT o FROM Orders o " +
            "WHERE o.leftoverMoney IS NOT NULL " +
            "AND o.leftoverMoney < :threshold " +
            "AND EXISTS (" +
            "   SELECT 1 FROM OrderLinks ol " +
            "   WHERE ol.orders = o AND ol.status = 'DA_HUY'" +
            ")")
    Page<Orders> findOrdersWithRefundableCancelledLinks(
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

    // Cho staff sale
    @Query("SELECT o FROM Orders o " +
            "WHERE o.staff.accountId = :staffId " +
            "AND o.leftoverMoney IS NOT NULL " +
            "AND o.leftoverMoney < :threshold " +
            "AND EXISTS (" +
            "   SELECT 1 FROM OrderLinks ol " +
            "   WHERE ol.orders = o AND ol.status = 'DA_HUY'" +
            ")")
    Page<Orders> findByStaffIdAndRefundableCancelledLinks(
            @Param("staffId") Long staffId,
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

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
    @Query("""
        SELECT o FROM Orders o
        LEFT JOIN o.customer c
        LEFT JOIN o.orderLinks ol
        WHERE 
            (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
    """)
    Page<Orders> findAllWithFilters(
            @Param("shipmentCode") String shipmentCode,
            @Param("customerCode") String customerCode,
            @Param("orderCode") String orderCode,
            Pageable pageable
    );

    @Query("""
        SELECT o FROM Orders o
        LEFT JOIN o.customer c
        LEFT JOIN o.orderLinks ol
        WHERE 
            o.staff.accountId = :accountId
            AND (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
    """)
    Page<Orders> findByStaffAccountIdWithFilters(
            @Param("accountId") Long accountId,
            @Param("shipmentCode") String shipmentCode,
            @Param("customerCode") String customerCode,
            @Param("orderCode") String orderCode,
            Pageable pageable
    );

    @Query("""
        SELECT o FROM Orders o
        LEFT JOIN o.customer c
        LEFT JOIN o.orderLinks ol
        WHERE 
            o.route.routeId IN :routeIds
            AND (:shipmentCode IS NULL OR ol.shipmentCode LIKE %:shipmentCode%)
            AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
            AND (:orderCode IS NULL OR o.orderCode LIKE %:orderCode%)
    """)
    Page<Orders> findByRouteRouteIdInWithFilters(
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
}

