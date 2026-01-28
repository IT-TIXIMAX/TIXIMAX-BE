package com.tiximax.txm.Service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tiximax.txm.Entity.AutoPayment;
import com.tiximax.txm.Enums.PaymentPurpose;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Repository.AutoPaymentRepository;

@Service
public class AutoPaymentService {

    @Autowired
    private AutoPaymentRepository autoPaymentRepository;


    public List<AutoPayment> getAll() {
        return autoPaymentRepository.findAll();
    }

    public AutoPayment getById(Long id) {
        return autoPaymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AutoPayment với id: " + id));
    }

    public AutoPayment create(BigDecimal amount, String paymentCode, PaymentPurpose purpose) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền không hợp lệ!");
        }

        if (paymentCode == null || paymentCode.trim().isEmpty()) {
            throw new BadRequestException("PaymentCode không được để trống!");
        }

        if (autoPaymentRepository.existsByPaymentCode(paymentCode)) {
            throw new BadRequestException("PaymentCode đã tồn tại!");
        }

        AutoPayment autoPayment = new AutoPayment();
        autoPayment.setAmount(amount);
        autoPayment.setPaymentCode(paymentCode);
        autoPayment.setPaymentPurpose(purpose);

        return autoPaymentRepository.save(autoPayment);
    }

    public AutoPayment update(Long id, BigDecimal amount, PaymentPurpose purpose) {

        AutoPayment autoPayment = getById(id);

        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Số tiền không hợp lệ!");
            }
            autoPayment.setAmount(amount);
        }

        if (purpose != null) {
            autoPayment.setPaymentPurpose(purpose);
        }

        return autoPaymentRepository.save(autoPayment);
    }


    public void delete(Long id) {
        AutoPayment autoPayment = getById(id);
        autoPaymentRepository.delete(autoPayment);
    }
}