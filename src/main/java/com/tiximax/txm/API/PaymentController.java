package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Model.PaymentAuctionResponse;
import com.tiximax.txm.Model.SmsRequest;
import com.tiximax.txm.Service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/payments")
@SecurityRequirement(name = "bearerAuth")

public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/order/{orderCode}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable String orderCode) {
        List<Payment> payments = paymentService.getPaymentsByOrderCode(orderCode);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/merged/{depositPercent}/{isUseBalance}/{bankId}")
    public ResponseEntity<Payment> createMergedPayment(@RequestBody Set<String> orderCodes,
                                                       @PathVariable Integer depositPercent,
                                                       @PathVariable boolean isUseBalance,
                                                       @PathVariable Long bankId) {
        Payment createdPayment = paymentService.createMergedPayment(orderCodes, depositPercent, isUseBalance, bankId);
        return ResponseEntity.ok(createdPayment);
    }

    @PostMapping("/merged/payment-after-auction/{depositPercent}/{isUseBalance}/{bankId}")
    public ResponseEntity<Payment> createMergedPaymentAfterAuction(@RequestBody Set<String> orderCodes,
                                                       @PathVariable Integer depositPercent,
                                                       @PathVariable boolean isUseBalance,
                                                       @PathVariable Long bankId) {
        Payment createdPayment = paymentService.createMergedPaymentAfterAuction(orderCodes, depositPercent, isUseBalance, bankId);
        return ResponseEntity.ok(createdPayment);
    }

    @PostMapping("/merged-shipping/{isUseBalance}/{bankId}/{priceShipDos}/{customerVoucherId}")
    public ResponseEntity<Payment> createMergedPaymentShipping(@RequestBody Set<String> orderCodes,
                                                               @PathVariable boolean isUseBalance,
                                                               @PathVariable Long bankId,
                                                               @PathVariable BigDecimal priceShipDos,
                                                               @RequestParam(required = false) Long customerVoucherId) {
        Payment createdPayment = paymentService.createMergedPaymentShipping(orderCodes, isUseBalance, bankId, priceShipDos, customerVoucherId);
        return ResponseEntity.ok(createdPayment);
    }

    @PutMapping("/confirm/{paymentCode}")
    public ResponseEntity<Payment> confirmPayment(@PathVariable String paymentCode) {
        Payment confirmedPayment = paymentService.confirmedPayment(paymentCode);
        return ResponseEntity.ok(confirmedPayment);
    }

    @PutMapping("/confirm-shipping/{paymentCode}")
    public ResponseEntity<Payment> confirmPaymentShipping(@PathVariable String paymentCode) {
        Payment confirmedPayment = paymentService.confirmedPaymentShipment(paymentCode);
        return ResponseEntity.ok(confirmedPayment);
    }

    @GetMapping("/auction")
    public ResponseEntity<List<PaymentAuctionResponse>> getAuctionPayment() {
       List<PaymentAuctionResponse> confirmedPayment = paymentService.getPaymentByStaffandStatus();
        return ResponseEntity.ok(confirmedPayment);
    }
    @GetMapping("/partial-payment")
    public ResponseEntity<List<Payment>> getPaymentsByPartialStatus(
    ) {
        List<Payment> payments = paymentService.getPaymentsByPartialStatus();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("code/{paymentCode}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentCode) {
        Optional<Payment> payment = paymentService.getPaymentByCode(paymentCode);
        return payment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("id/{paymentId}")
    public ResponseEntity<Optional<Payment>> getPaymentsByOrderId(@PathVariable Long paymentId) {
        Optional<Payment> payment = paymentService.getPaymentsById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{orderId}/pending")
    public ResponseEntity<Payment> getPendingPaymentByOrderId(@PathVariable Long orderId) {
        Optional<Payment> payment = paymentService.getPendingPaymentByOrderId(orderId);
        return payment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/sms-external")
    public ResponseEntity<?> getExternalSms() {
        try {
            SmsRequest smsData = paymentService.getSmsFromExternalApi();
            return ResponseEntity.ok(smsData);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch SMS data");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

}
