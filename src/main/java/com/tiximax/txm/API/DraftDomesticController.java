package com.tiximax.txm.API;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.Carrier;
import com.tiximax.txm.Enums.DraftDomesticStatus;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.DraftDomesticRequest;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.UpdateDraftDomesticInfoRequest;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.UpdateDraftShipmentRequest;
import com.tiximax.txm.Model.DTOResponse.Domestic.ShipCodePayment;
import com.tiximax.txm.Model.DTOResponse.DraftDomestic.AvailableAddDarfDomestic;
import com.tiximax.txm.Model.DTOResponse.DraftDomestic.DraftDomesticResponse;
import com.tiximax.txm.Service.DraftDomesticService;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@CrossOrigin
@RequestMapping("/draft-domestics")
@SecurityRequirement(name = "bearerAuth")
public class DraftDomesticController {

    @Autowired
    private DraftDomesticService draftDomesticService;
    @Autowired
        private AccountUtils accountUtils;
    @PostMapping("/add") 
    public ResponseEntity<DraftDomesticResponse> addDraftDomestic(@RequestBody DraftDomesticRequest draft){
        var response = draftDomesticService.addDraftDomestic(draft);
        return ResponseEntity.ok(response);
    }
@GetMapping("/{page}/{size}")
public ResponseEntity<Page<DraftDomesticResponse>> getAllDraftDomestic(
        @RequestParam(required = false) String customerCode,
        @RequestParam(required = false) String shipmentCode,
        @RequestParam(required = false) DraftDomesticStatus status,
        @RequestParam(required = false) Carrier carrier,   
        @PathVariable int page,
        @PathVariable int size
) {

    Pageable pageable = PageRequest.of(page, size);

    Page<DraftDomesticResponse> result =
            draftDomesticService.getAllDraftDomestic(
                    customerCode,
                    shipmentCode,
                    status,
                    carrier,
                    pageable
            );

    return ResponseEntity.ok(result);
}

     @PatchMapping("/{id}/info")
        public ResponseEntity<?> updateDraftInfo(
                @PathVariable Long id,
                @RequestBody UpdateDraftDomesticInfoRequest request
        ) {
        return ResponseEntity.ok(
                draftDomesticService.updateDraftInfo(id, request)
        );
        }
        @GetMapping("/ship-code/payment/{page}/{size}")
        public ResponseEntity<List<ShipCodePayment>> getAllShipByStaff(
        @PathVariable int page,
        @PathVariable int size,
        @RequestParam(required = false) String shipCode
    ) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(
                draftDomesticService.getAllShipByStaff(staff.getAccountId(),shipCode,pageable)
        );
    }

    @PostMapping("/{id}/shipments/add")
    public ResponseEntity<?> addShipments(
            @PathVariable Long id,
            @RequestBody UpdateDraftShipmentRequest request
    ) {
        return ResponseEntity.ok(
                draftDomesticService.addShipments(id, request.getShippingCodes())
        );
    }
    @PostMapping("/{id}/shipments/remove")
    public ResponseEntity<?> removeShipments(
            @PathVariable Long id,
            @RequestBody UpdateDraftShipmentRequest request
    ) {
        return ResponseEntity.ok(
                draftDomesticService.removeShipments(id, request.getShippingCodes())
        );
    }
@GetMapping("locked")
public ResponseEntity<List<DraftDomesticResponse>> getLockedDraftNotExported(
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        @RequestParam Carrier carrier
) {
    Account staff = (Staff) accountUtils.getAccountCurrent();

    Long staffId = null;
    if (staff.getRole() == AccountRoles.STAFF_SALE) { 
        staffId = staff.getAccountId();
    }

    return ResponseEntity.ok(
        draftDomesticService.getLockedDraftNotExported(endDate, staffId, carrier)
    );
}

    @GetMapping("/{id}")
        public ResponseEntity<DraftDomesticResponse> getDraftDomestic(
                @PathVariable Long id
        ) {
        return ResponseEntity.ok(
                draftDomesticService.getDraftDomestic(id)
        );
    }
    @GetMapping("/available-add/{page}/{size}")
        public ResponseEntity<Page<AvailableAddDarfDomestic>> getAvailableAddDraftDomestic(
                @RequestParam(required = false) String customerCode,
                @RequestParam(required = false) Long routeId,
                @PathVariable int page,
                @PathVariable int size
        ) {
        Pageable pageable = PageRequest.of(page, size);

        Long staffId = null;
        var account = accountUtils.getAccountCurrent();
        if (account != null && account.getRole().equals(AccountRoles.STAFF_SALE)) {
                staffId = account.getAccountId();
        }
        return ResponseEntity.ok(
                draftDomesticService.getAvailableAddDraftDomestic(
                        customerCode,
                        staffId,
                        routeId,
                        pageable
                )
        );
}

        @PostMapping("/export/ids")
       public ResponseEntity<Map<String, Object>> lockDraftDomestic(
                @RequestBody List<Long> draftIds
        ) {
        Boolean result = draftDomesticService.ExportDraftDomestic(draftIds);
          return ResponseEntity.ok(
            Map.of(
                    "success", result,
                    "message", "Export danh sách thành công"
            )
    );
}
@GetMapping("/available-to-ship/{page}/{size}")
public ResponseEntity<Page<DraftDomesticResponse>> getAvailableToShip(
        @RequestParam(required = false) Long routeId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDateTime,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endDateTime,
        @PathVariable int page,
        @PathVariable int size
) {
         Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(draftDomesticService.getAvailableToShip(routeId, startDateTime, endDateTime, pageable));
}
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteDraftDomestic(@PathVariable Long id) {
    draftDomesticService.deleteDraftDomestic(id);
    return ResponseEntity.ok("Xóa mẫu vận chuyển nội địa thành công");
}
}


