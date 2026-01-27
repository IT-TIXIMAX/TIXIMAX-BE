package com.tiximax.txm.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tiximax.txm.Entity.AutoPayment;

@Repository
public interface AutoPaymentRepository extends JpaRepository<AutoPayment, Long> {

    boolean existsByPaymentCode(String paymentCode);
}
