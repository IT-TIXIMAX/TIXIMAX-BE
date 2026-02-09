package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Model.DTOResponse.DashBoard.RoutePaymentSummary;

import com.tiximax.txm.Model.DTOResponse.Payment.DailyPaymentRevenue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentCode(String paymentCode);

    List<Payment> findByOrdersOrderCode(String orderCode);

    Optional<Payment> findByPaymentCode(String paymentCode);

    Optional<Payment> findFirstByOrdersOrderIdAndStatus(Long orderId, PaymentStatus paymentStatus);

    @Query("SELECT p FROM Payment p JOIN p.relatedOrders o WHERE o.orderId = :orderId AND p.isMergedPayment = true AND p.status = :status")
    Optional<Payment> findMergedPaymentByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") PaymentStatus status);

    @Query("""
            SELECT p 
            FROM Payment p 
            JOIN p.orders o 
            WHERE p.staff = :staff 
              AND o.status = :orderStatus
              AND p.status = :paymentStatus
            ORDER BY p.actionAt DESC
            """)
    List<Payment> findAllByStaffAndOrderStatusAndPaymentStatusOrderByActionAtDesc(
            @Param("staff") Staff staff,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );

    @Query("""
                SELECT DISTINCT p
                FROM Payment p
                JOIN p.partialShipments ps
                WHERE p.staff.id = :staffId
                  AND ps.status = :status
            """)
    List<Payment> findPaymentsByStaffAndPartialStatus(
            @Param("staffId") Long staffId,
            @Param("status") OrderStatus status
    );

    long countByStaff_AccountIdAndOrdersIn(Long accountId, List<Orders> orders);

    List<Payment> findByStaff_AccountIdAndStatusAndActionAtBetween(
            Long staffId,
            PaymentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

//    @Query("""
//            SELECT COALESCE(SUM(p.collectedAmount), 0)
//            FROM Payment p
//            WHERE p.status IN (
//                com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN,
//                com.tiximax.txm.Enums.PaymentStatus.DA_HOAN_TIEN,
//                com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN_SHIP
//            )
//              AND p.actionAt BETWEEN :start AND :end """)
//    BigDecimal sumCollectedAmountBetween(@Param("start") LocalDateTime start,
//                                         @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status IN (
                com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN,
                com.tiximax.txm.Enums.PaymentStatus.DA_HOAN_TIEN,
                com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN_SHIP
            )
              AND p.actionAt BETWEEN :start AND :end """)
    BigDecimal sumAmountBetween(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(p.collectedAmount), 0)
            FROM Payment p
            WHERE p.status = com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN_SHIP
              AND p.actionAt BETWEEN :start AND :end """)
    BigDecimal sumShipRevenueBetween(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(p.collectedAmount), 0)
            FROM Payment p
            WHERE p.status IN (
                com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN,
                com.tiximax.txm.Enums.PaymentStatus.DA_HOAN_TIEN
            )
              AND p.actionAt BETWEEN :start AND :end """)
    BigDecimal sumPurchaseBetween(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    List<Payment> findByRelatedOrdersContaining(Orders order);

    @Query(value = "SELECT p.* FROM payment p " +
            "JOIN payment_orders po ON p.payment_id = po.payment_id " +
            "WHERE po.order_id = :orderId " +
            "AND p.status = :status",
            nativeQuery = true)
    Optional<Payment> findPaymentForOrder(@Param("orderId") Long orderId,
                                          @Param("status") String status);


    @Query("SELECT COALESCE(SUM(p.collectedAmount), 0) " +
            "FROM Payment p " +
            "WHERE p.status = :status " +
            "AND p.actionAt BETWEEN :start AND :end")
    BigDecimal sumCollectedAmountByStatusAndActionAtBetween(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.collectedAmount), 0) " +
            "FROM Payment p " +
            "WHERE p.status IN :statuses " +
            "AND p.actionAt BETWEEN :start AND :end")
    BigDecimal sumCollectedAmountByStatusesAndActionAtBetween(
            @Param("statuses") List<PaymentStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


    @Query("SELECT MONTH(p.actionAt), SUM(p.collectedAmount) FROM Payment p WHERE YEAR(p.actionAt) = :year AND p.status = 'DA_THANH_TOAN' GROUP BY MONTH(p.actionAt)")
    List<Object[]> sumRevenueByMonth(@Param("year") int year);

    @Query("SELECT MONTH(p.actionAt), SUM(p.amount) FROM Payment p WHERE YEAR(p.actionAt) = :year AND p.paymentType = 'PURCHASE' GROUP BY MONTH(p.actionAt)")
        // Adjust based on actual purchase logic
    List<Object[]> sumPurchaseByMonth(@Param("year") int year);

    @Query("SELECT MONTH(p.actionAt), SUM(p.collectedAmount) FROM Payment p WHERE YEAR(p.actionAt) = :year AND p.status = 'DA_THANH_TOAN_SHIP' GROUP BY MONTH(p.actionAt)")
    List<Object[]> sumShipByMonth(@Param("year") int year);

    @Query("""
    SELECT DISTINCT p
    FROM Payment p
    JOIN p.partialShipments ps
    WHERE ps.orders.orderId = :orderId
      AND p.status IN :statuses
        """)
        Optional<Payment> findPaymentByPartialShipment(
                @Param("orderId") Long orderId,
                @Param("statuses") List<PaymentStatus> statuses
        );

    @Query(value = """
            WITH raw AS
            (SELECT DISTINCT p.payment_id,
                r.name AS route_name,
                p.collected_amount AS revenue
            FROM payment p
            LEFT JOIN partial_shipment ps ON ps.payment_id = p.payment_id
            LEFT JOIN order_links ol ON ol.partial_shipment_id = ps.id
            LEFT JOIN orders o ON o.order_id  = ol.order_id
            LEFT JOIN route r ON r.route_id = o.route_id
            WHERE p.action_at BETWEEN :start AND :end
            AND p.status = :status
            )
            SELECT route_name,
                sum(coalesce(revenue)) as total_revenue
            FROM raw
            GROUP by 1
    """, nativeQuery = true)
    List<Object[]> sumCollectedAmountByRouteNativeRaw(
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = """
    WITH ds AS (
        SELECT
            DATE(p.action_at) AS payment_date,
            SUM(COALESCE(p.collected_amount, 0)) AS revenue
        FROM payment p
        WHERE p.status = 'DA_THANH_TOAN'
          AND p.action_at BETWEEN :startDate AND :endDate
        GROUP BY DATE(p.action_at)
    )
    SELECT payment_date, revenue
    FROM ds
    ORDER BY payment_date ASC
    """, nativeQuery = true)
    List<Object[]> getDailyPaymentRevenueNative(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
    WITH ds AS (
        SELECT
            DATE(p.action_at) AS payment_date,
            SUM(COALESCE(p.collected_amount, 0)) AS revenue
        FROM payment p
        WHERE p.status = 'DA_THANH_TOAN_SHIP'
          AND p.action_at BETWEEN :startDate AND :endDate
        GROUP BY DATE(p.action_at)
    )
    SELECT payment_date, revenue
    FROM ds
    ORDER BY payment_date ASC
    """, nativeQuery = true)
    List<Object[]> getDailyPaymentShippingNative(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}