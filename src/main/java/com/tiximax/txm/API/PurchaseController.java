package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Enums.PurchaseFilter;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Service.PurchaseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/purchases")
@SecurityRequirement(name = "bearerAuth")

public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PostMapping("/add")
    public ResponseEntity<Purchases> addPurchase(@RequestParam String orderCode, @RequestBody PurchaseRequest purchaseRequest) {
        Purchases purchase = purchaseService.createPurchase(orderCode, purchaseRequest);
        return ResponseEntity.ok(purchase);
    }

    @PostMapping("auction/add")
    public ResponseEntity<Purchases> addAuction(@RequestParam String orderCode, @RequestBody AuctionRequest purchaseRequest) {
        Purchases purchase = purchaseService.createAuction(orderCode, purchaseRequest);
        return ResponseEntity.ok(purchase);
    }   

    @GetMapping("/{page}/{size}/paging")
    public ResponseEntity<Page<Purchases>> getAllPurchases(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("purchaseTime").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Purchases> purchasesPage = (Page<Purchases>) purchaseService.getAllPurchases(pageable);
        return ResponseEntity.ok(purchasesPage);
    }

    @GetMapping("/{purchaseId}")
    public ResponseEntity<PurchaseDetail> getPurchaseById(@PathVariable Long purchaseId) {
        PurchaseDetail purchaseDetail = purchaseService.getPurchaseById(purchaseId);
        return ResponseEntity.ok(purchaseDetail);
    }

    @PutMapping("/shipment/{purchaseId}")
    public ResponseEntity<Purchases> updateShipment(
            @PathVariable Long purchaseId,
            @RequestBody ShipmentCode shipmentCode) {

        Purchases updated = purchaseService.updateShipmentForPurchase(purchaseId, shipmentCode.getShipmentCode());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/shipment-ship-fee/{purchaseId}")
    public ResponseEntity<Purchases> updateShipmentAnd(
            @PathVariable Long purchaseId,
            @RequestBody UpdateShipmentRequest request)
            {

        Purchases updated = purchaseService.updateShipmentForPurchaseAndShipFee(purchaseId, request.getShipmentCode() , request.getShipFee());
        return ResponseEntity.ok(updated);
}

    @DeleteMapping("/{purchaseId}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long purchaseId) {
        purchaseService.deletePurchase(purchaseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending-shipment")
    public ResponseEntity<List<PendingShipmentPurchase>> getPurchasesWithPendingShipment() {
        List<PendingShipmentPurchase> result = purchaseService.getPurchasesWithPendingShipment();
        return ResponseEntity.ok(result);
    }
    
   @GetMapping("/all-purchase/{page}/{size}")
public ResponseEntity<Page<PurchasePendingShipment>> getFilteredPurchases(
        @PathVariable int page,
        @PathVariable int size,
        @RequestParam(required = false) PurchaseFilter status,
        @RequestParam(required = false) String orderCode,
        @RequestParam(required = false) String customerCode,
        @RequestParam(required = false) String shipmentCode
) {
    Pageable pageable = PageRequest.of(page, size);

    Page<PurchasePendingShipment> result =
            purchaseService.getPurchasesWithFilteredOrderLinks(
                    status,
                    orderCode,
                    customerCode,
                    shipmentCode,
                    pageable
            );
    return ResponseEntity.ok(result);
}

@GetMapping("/lack-shipment-code/{page}/{size}")
public ResponseEntity<Page<PurchasePendingShipment>> getPendingShipmentFullPurchases(
        @PathVariable int page,
        @PathVariable int size,
        @RequestParam(required = false) PurchaseFilter status,
        @RequestParam(required = false) String orderCode,
        @RequestParam(required = false) String customerCode
) {
    Pageable pageable = PageRequest.of(page, size);

    Page<PurchasePendingShipment> result =
            purchaseService.getFullPurchases(
                    status,
                    orderCode,
                    customerCode,
                    pageable
            );

    return ResponseEntity.ok(result);
}

@PostMapping("/money-exchange/add")
public ResponseEntity<Purchases> createMoneyExchange(
        @RequestParam String orderCode,
        @RequestBody ExchangeRequest exchangeRequest
) {
    Purchases purchase = purchaseService.createMoneyExchange(orderCode, exchangeRequest);
    return ResponseEntity.ok(purchase);
}

    @PatchMapping("/{purchaseId}")
    public ResponseEntity<Purchases> updatePurchase(
            @PathVariable Long purchaseId,
            @RequestBody UpdatePurchaseRequest request) {
        Purchases updated = purchaseService.updatePurchase(purchaseId, request);
        return ResponseEntity.ok(updated);
    }
}
