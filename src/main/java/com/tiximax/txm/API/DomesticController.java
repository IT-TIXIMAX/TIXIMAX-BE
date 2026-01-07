package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Domestic;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Model.CheckInDomestic;
import com.tiximax.txm.Model.CreateDomesticRequest;
import com.tiximax.txm.Model.DomesticDelivery;
import com.tiximax.txm.Model.DomesticRecieve;
import com.tiximax.txm.Model.DomesticResponse;
import com.tiximax.txm.Model.DomesticSend;
import com.tiximax.txm.Model.VNPostTrackingCode;
import com.tiximax.txm.Model.EnumFilter.DeliveryStatus;
import com.tiximax.txm.Service.DomesticService;
import com.tiximax.txm.Utils.AccountUtils;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@CrossOrigin
@RequestMapping("/domestics")
@SecurityRequirement(name = "bearerAuth")

public class DomesticController {

    @Autowired
    private DomesticService domesticService;

    @Autowired
    private AccountUtils accountUtils;

    @PostMapping("/received")
    public ResponseEntity<DomesticRecieve> createDomesticForWarehousing(@RequestBody CreateDomesticRequest request) {
        if (request == null || request.getPackingCode().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        DomesticRecieve domestic = domesticService.createDomesticForWarehousing(request.getPackingCode(), request.getNote());
        return ResponseEntity.ok(domestic);
    }
      @GetMapping("/ready-for-delivery-to-ship")
    public ResponseEntity<List<Map<String, Object>>> getReadyForDeliveryOrders( 
        @RequestParam List<String> shipmentCodes) {
        List<Map<String, Object>> result = domesticService.getReadyForDeliveryOrdersToShip(shipmentCodes);
      
        return ResponseEntity.ok(result);
    } 
    @GetMapping("/check-in-domestic/{shipmentCode}")
    public ResponseEntity<CheckInDomestic>  checkInDomestic(@RequestParam String shipmentCode) {
        CheckInDomestic checkInDomestic = domesticService.getCheckInDomestic(shipmentCode);
        return ResponseEntity.ok(checkInDomestic);
    }

    @GetMapping("/ready-for-delivery/{page}/{size}")
    public ResponseEntity<List<Map<String, Object>>> getReadyForDeliveryOrders(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        List<Map<String, Object>> result = domesticService.getReadyForDeliveryOrders(pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/transfer-to-customer")
    public ResponseEntity<List<DomesticResponse>> transferToCustomer(){
        List<DomesticResponse> domestic = domesticService.TransferToCustomer();
        return ResponseEntity.ok(domestic);
    }
     @GetMapping("/{id}")
    public ResponseEntity<Domestic> getDomesticById(@PathVariable Long id) {
    Optional<Domestic> domestic = domesticService.getDomesticById(id);
    return domestic.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
}
    @GetMapping("/delivered-today")
    public ResponseEntity<List<DomesticResponse>> getDomesticDeliveredOnDaily() {
        List<DomesticResponse> delivered = domesticService.getDomesticDeliveredOnDaily();
        return ResponseEntity.ok(delivered);
    }
    @GetMapping("/ready-by-customer/{customerCode}")
    public ResponseEntity<List<Map<String, Object>>> getReadyByCustomerCode(
        @PathVariable String customerCode) {
        List<Map<String, Object>> result = domesticService.getReadyForDeliveryOrdersByCustomerCode(customerCode);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/transfer-by-customer/{customerCode}")
    public ResponseEntity<List<DomesticResponse>> transferByCustomerCode(@PathVariable String customerCode, @RequestBody VNPostTrackingCode vNPostTrackingCode) {
    if (customerCode == null || customerCode.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
    }
    String code = customerCode.trim().toUpperCase();
    List<DomesticResponse> result = domesticService.transferByCustomerCode(code,vNPostTrackingCode.getVNPostTrackingCode());
    return ResponseEntity.ok(result);
}
@PostMapping("/transfer-to-warehouse")
public ResponseEntity<Domestic> transferPackingToWarehouse(
        @RequestBody Map<String, Object> requestBody) {

    // Validate request body
    if (!requestBody.containsKey("packingCodes") ||
        !requestBody.containsKey("toLocationId")) {
        return ResponseEntity.badRequest().body(null);
    }

    List<String> packingCodes = (List<String>) requestBody.get("packingCodes");
    Long toLocationId = Long.parseLong(requestBody.get("toLocationId").toString());
    String note = requestBody.containsKey("note") ? requestBody.get("note").toString() : null;

    Domestic domestic = domesticService.TranferPackingToWarehouse(packingCodes, toLocationId, note);

    return ResponseEntity.ok(domestic);
}
@PostMapping("/received-from-warehouse/{domesticId}")
public ResponseEntity<Domestic> receivedPackingFromWarehouse(@PathVariable Long domesticId) {

    if (domesticId == null || domesticId <= 0) {
        return ResponseEntity.badRequest().build();
    }

    Domestic domestic = domesticService.RecievedPackingFromWarehouse(domesticId);

    return ResponseEntity.ok(domestic);
}

  @PostMapping("/transfer-by-shipment")
    public ResponseEntity<List<DomesticResponse>> transferByShipmentCode(@RequestBody List<String> shipmentCodes) {
        if (shipmentCodes == null || shipmentCodes.isEmpty()) {
            return ResponseEntity.badRequest().build();  
        }
        List<DomesticResponse> domesticResponses = domesticService.transferByShipmentCode(shipmentCodes);
        
        if (domesticResponses.isEmpty()) {
            return ResponseEntity.noContent().build();  
        }
        return ResponseEntity.ok(domesticResponses);  
    }
    @PostMapping("/scan-import/{shipmentCode}")
public ResponseEntity<Map<String, Object>> scanImportToDomestic(
        @PathVariable String shipmentCode) {

    if (shipmentCode == null || shipmentCode.trim().isEmpty()) {
        throw new IllegalArgumentException("ShipmentCode không được để trống!");
    }

    Boolean result = domesticService.scanImportToDomestic(shipmentCode.trim());

    return ResponseEntity.ok(
            Map.of(
                    "success", result,
                    "message", "Scan nhập kho nội địa thành công",
                    "shipmentCode", shipmentCode
            )
    );
}
@GetMapping("/preview-transfer-by-customer/{customerCode}")
public ResponseEntity<List<DomesticSend>> previewTransferByCustomerCode(
        @PathVariable String customerCode) {

    if (customerCode == null || customerCode.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
    }
    String code = customerCode.trim().toUpperCase();

    List<DomesticSend> result =
            domesticService.previewTransferByCustomerCode(code);

    return ResponseEntity.ok(result);
}

@GetMapping("/delivery/{page}/{size}")
public ResponseEntity<Page<DomesticDelivery>> getDomesticDelivery(
        @RequestParam DeliveryStatus status,
        @RequestParam(required = false) String customerCode,
        @PathVariable int page,
        @PathVariable int size
) {
    if (status == null) {
        return ResponseEntity.badRequest().build();
    }

    Account currentAccount = accountUtils.getAccountCurrent();
    Pageable pageable = PageRequest.of(page, size);

    Long staffId =
            AccountRoles.STAFF_SALE.equals(currentAccount.getRole())
                    ? currentAccount.getAccountId()
                    : null;

    Page<DomesticDelivery> result =
            domesticService.getDomesticDeliveryByCustomerPaged(
                    status,
                    customerCode,
                    staffId,
                    pageable
            );

    return ResponseEntity.ok(result);
}
}

