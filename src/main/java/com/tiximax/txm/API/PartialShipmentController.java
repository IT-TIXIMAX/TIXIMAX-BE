package com.tiximax.txm.API;

import com.tiximax.txm.Entity.PartialShipment;
import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Model.DTORequest.OrderLink.ShipmentCodesRequest;
import com.tiximax.txm.Model.DTOResponse.Payment.PartialPayment;
import com.tiximax.txm.Service.PartialShipmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@RestController
@CrossOrigin
@RequestMapping("/partial-shipment")
@SecurityRequirement(name = "bearerAuth")

public class PartialShipmentController {

    @Autowired
    private PartialShipmentService partialShipmentService;
    @PreAuthorize("hasAnyRole('STAFF_SALE','LEAD_SALE')")
    @PostMapping("/partial-shipment/{isUseBalance}/{bankId}/{priceShipDos}/{customerVoucherId}")
    public ResponseEntity<Payment> createPartialShipment(@RequestBody ShipmentCodesRequest selectedTrackingCode,
                                                                    @PathVariable boolean isUseBalance,
                                                                    @PathVariable Long bankId,
                                                                    @PathVariable BigDecimal priceShipDos,
                                                                    @RequestParam(required = false) Long customerVoucherId) {
        List<PartialShipment> partial = partialShipmentService.createPartialShipment(selectedTrackingCode, isUseBalance, bankId, priceShipDos, customerVoucherId);
        Payment payment = partial.get(0).getPayment();                                                             
        return ResponseEntity.ok(payment);
    }
    @GetMapping("/{id}")
    public ResponseEntity<PartialShipment> getPartialShipmentById(@PathVariable Long id) {
    Optional<PartialShipment> partialShipment = partialShipmentService.getById(id);
    return partialShipment.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}
@GetMapping("/{page}/{size}")
public ResponseEntity<Page<PartialPayment>> getPartialPayments(
        @PathVariable int page,
        @PathVariable int size,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) String orderCode
) {
    Pageable pageable = PageRequest.of(page, size);

    Page<PartialPayment> result =
            partialShipmentService.getPartialPayments(
                    pageable,
                    status,
                    orderCode
            );

    return ResponseEntity.ok(result);
}

@PreAuthorize("hasAnyRole('STAFF_SALE','LEAD_SALE')")
@PostMapping("/by-ship-code/{shipCode}/{isUseBalance}/{bankId}/{priceShipDos}")
public ResponseEntity<Payment> createPartialShipmentByShipCode(
        @PathVariable String shipCode,
        @PathVariable boolean isUseBalance,
        @PathVariable Long bankId,
        @PathVariable BigDecimal priceShipDos,
        @RequestParam(required = false) Long customerVoucherId
) {
    List<PartialShipment> partials =
            partialShipmentService.createPartialShipmentByShipCode(
                    shipCode,
                    isUseBalance,
                    bankId,
                    priceShipDos,
                    customerVoucherId
            );

    Payment payment = partials.get(0).getPayment();

    return ResponseEntity.ok(payment);
}

}
