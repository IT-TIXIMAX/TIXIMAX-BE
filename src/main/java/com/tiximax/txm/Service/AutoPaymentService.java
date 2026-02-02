package com.tiximax.txm.Service;

import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiximax.txm.Entity.AutoPayment;
import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Enums.PaymentPurpose;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.Payment.SmsRequest;
import com.tiximax.txm.Repository.AutoPaymentRepository;
import com.tiximax.txm.Repository.PaymentRepository;

import jakarta.transaction.Transactional;

@Service
public class AutoPaymentService {

    @Autowired
    private AutoPaymentRepository autoPaymentRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentService paymentService;
   @Value("${sms.secret}")
    private String secret;


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

    @Transactional
     public void verifyRaw(String rawBody, String signature) {
    try {
        String expected = hmac(rawBody);

        if (!expected.equals(signature)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid signature"
            );
        }

        SmsRequest req =
            new ObjectMapper().readValue(rawBody, SmsRequest.class);

        long now = System.currentTimeMillis();
        if (Math.abs(now - req.getTimestamp()) > 300_000) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Expired request"
            );
        }

    String txmCode = extractTXMGD(req.getContent());

    if (txmCode == null) {
    
        return;
    }

    Payment payment = paymentRepository.findByPaymentCode(txmCode)
        .orElse(null);

    if (payment == null) {
        return;
    }
    if (payment.getCollectedAmount().compareTo(req.getAmount()) != 0){
        return;
    }
    create(req.getAmount(), txmCode , payment.getPurpose());

    if(payment.getPurpose() == PaymentPurpose.THANH_TOAN_DON_HANG){
        
        paymentService.confirmedPayment(txmCode);
    } else {
         paymentService.confirmedPaymentShipment(txmCode);
    }
    } catch (JsonProcessingException e) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid JSON payload"
        );
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}

private static final Pattern TXMGD_PATTERN =
        Pattern.compile("\\bTXMGD[0-9A-F]{8}\\b");



public static String extractTXMGD(String content) {
    if (content == null || content.isBlank()) return null;

    Matcher matcher = TXMGD_PATTERN.matcher(content.toUpperCase());
    return matcher.find() ? matcher.group() : null;
}



    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes()));
    }

}