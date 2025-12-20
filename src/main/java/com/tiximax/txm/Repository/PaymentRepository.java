package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Model.RoutePaymentSummary;
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
    @Query("""
    SELECT COALESCE(SUM(p.collectedAmount), 0)
    FROM Payment p
    WHERE p.status IN (
        com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN,
        com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN_SHIP
    )
      AND p.actionAt BETWEEN :start AND :end """)
        BigDecimal sumCollectedAmountBetween(@Param("start") LocalDateTime start,
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
    WHERE p.status = com.tiximax.txm.Enums.PaymentStatus.DA_THANH_TOAN
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



//    BigDecimal sumCollectedAmountByStatusAndActionAtBetween(PaymentStatus paymentStatus, LocalDateTime start, LocalDateTime end);
    @Query("SELECT COALESCE(SUM(p.collectedAmount), 0) " +
            "FROM Payment p " +
            "WHERE p.status = :status " +
            "AND p.actionAt BETWEEN :start AND :end")
    BigDecimal sumCollectedAmountByStatusAndActionAtBetween(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT MONTH(p.actionAt), SUM(p.collectedAmount) FROM Payment p WHERE YEAR(p.actionAt) = :year AND p.status = 'DA_THANH_TOAN' GROUP BY MONTH(p.actionAt)")
    List<Object[]> sumRevenueByMonth(@Param("year") int year);

    @Query("SELECT MONTH(p.actionAt), SUM(p.amount) FROM Payment p WHERE YEAR(p.actionAt) = :year AND p.paymentType = 'PURCHASE' GROUP BY MONTH(p.actionAt)")  // Adjust based on actual purchase logic
    List<Object[]> sumPurchaseByMonth(@Param("year") int year);

    @Query("SELECT MONTH(p.actionAt), SUM(p.collectedAmount) FROM Payment p WHERE YEAR(p.actionAt) = :year AND p.status = 'DA_THANH_TOAN_SHIP' GROUP BY MONTH(p.actionAt)")
    List<Object[]> sumShipByMonth(@Param("year") int year);

//    @Query("""
//    SELECT o.route.id    AS routeName,
//           COALESCE(SUM(p.collectedAmount), 0) AS totalAmount
//    FROM Payment p
//    JOIN p.orders o
//    WHERE p.status = :status
//      AND p.actionAt BETWEEN :start AND :end
//    GROUP BY o.route.id
//    ORDER BY totalAmount DESC
//    """)
//    List<RoutePaymentSummary> sumRevenueByRoute(
//            @Param("status") PaymentStatus status,
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end);

    @Query("""
    SELECT new com.tiximax.txm.Model.RoutePaymentSummary(
        o.route.routeId,
        COALESCE(SUM(p.collectedAmount), 0)
    )
    FROM Payment p
    JOIN p.orders o
    WHERE p.status = :status
      AND p.actionAt BETWEEN :start AND :end
    GROUP BY o.route.routeId
    ORDER BY SUM(p.collectedAmount) DESC
    """)
    List<RoutePaymentSummary> sumRevenueByRoute(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}

