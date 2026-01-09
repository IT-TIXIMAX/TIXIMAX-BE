package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Domestic;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Model.DTORequest.Domestic.CreateDomesticRequest;
import com.tiximax.txm.Model.DTORequest.Domestic.VNPostTrackingCode;
import com.tiximax.txm.Model.DTOResponse.Domestic.CheckInDomestic;
import com.tiximax.txm.Model.DTOResponse.Domestic.DomesticDelivery;
import com.tiximax.txm.Model.DTOResponse.Domestic.DomesticRecieve;
import com.tiximax.txm.Model.DTOResponse.Domestic.DomesticResponse;
import com.tiximax.txm.Model.DTOResponse.Domestic.DomesticSend;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_DOMESTIC')")
    @PostMapping("/received")
    public ResponseEntity<DomesticRecieve> createDomesticForWarehousing(@RequestBody CreateDomesticRequest request) {
        if (request == null || request.getPackingCode().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        DomesticRecieve domestic = domesticService.createDomesticForWarehousing(request.getPackingCode(), request.getNote());
        return ResponseEntity.ok(domestic);
    }

    @GetMapping("/check-in-domestic/{shipmentCode}")
    public ResponseEntity<CheckInDomestic>  checkInDomestic(@RequestParam String shipmentCode) {
        CheckInDomestic checkInDomestic = domesticService.getCheckInDomestic(shipmentCode);
        return ResponseEntity.ok(checkInDomestic);
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
   
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_DOMESTIC')")
    @PostMapping("/transfer-by-customer/{customerCode}")
    public ResponseEntity<List<DomesticResponse>> transferByCustomerCode(@PathVariable String customerCode, @RequestBody VNPostTrackingCode vNPostTrackingCode) {
    if (customerCode == null || customerCode.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
    }
    String code = customerCode.trim().toUpperCase();
    List<DomesticResponse> result = domesticService.transferByCustomerCode(code,vNPostTrackingCode.getVNPostTrackingCode());
    return ResponseEntity.ok(result);
}

@PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_DOMESTIC')")
@PostMapping("/received-from-warehouse/{domesticId}")
public ResponseEntity<Domestic> receivedPackingFromWarehouse(@PathVariable Long domesticId) {

    if (domesticId == null || domesticId <= 0) {
        return ResponseEntity.badRequest().build();
    }

    Domestic domestic = domesticService.RecievedPackingFromWarehouse(domesticId);

    return ResponseEntity.ok(domestic);
}
 
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_DOMESTIC')")
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

