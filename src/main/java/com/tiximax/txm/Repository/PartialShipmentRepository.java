package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.PartialShipment;
import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartialShipmentRepository  extends JpaRepository<PartialShipment, Long> {
     List<PartialShipment> findByPayment(Payment payment);


    @Query("""
        SELECT ps FROM PartialShipment ps
        WHERE (:status IS NULL OR ps.status = :status)
        AND (:orderCode IS NULL OR ps.orders.orderCode LIKE %:orderCode%)
    """)
    Page<PartialShipment> findForPartialPayment(
            @Param("status") OrderStatus status,
            @Param("orderCode") String orderCode,
             Pageable pageable
    );

     @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE PartialShipment ps
        SET ps.status = :status,
            ps.shipmentDate = :shipmentDate
        WHERE ps.payment.paymentId = :paymentId
    """)
    int updateAllByPaymentId(
            @Param("paymentId") Long paymentId,
            @Param("status") OrderStatus status,
            @Param("shipmentDate") LocalDateTime shipmentDate
    );

     @Query("""
        SELECT DISTINCT ps
        FROM PartialShipment ps
        JOIN FETCH ps.orders o
        JOIN FETCH ps.readyLinks rl
        LEFT JOIN FETCH o.orderLinks ol
        WHERE ps.payment.paymentId = :paymentId
    """)
    List<PartialShipment> findDetailByPaymentId(
            @Param("paymentId") Long paymentId
    );

    @Query("""
        SELECT ps FROM PartialShipment ps
        WHERE ps.staff.accountId = :staffId
        AND (:status IS NULL OR ps.status = :status)
    """)
    Page<PartialShipment> findForPartialPaymentByStaff(
            @Param("staffId") Long staffId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );
}

