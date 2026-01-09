package com.tiximax.txm.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.DraftDomesticRequest;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.UpdateDraftDomesticInfoRequest;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.UpdateDraftShipmentRequest;
import com.tiximax.txm.Model.DTOResponse.DraftDomestic.AvailableAddDarfDomestic;
import com.tiximax.txm.Model.DTOResponse.DraftDomestic.DraftDomesticResponse;
import com.tiximax.txm.Service.DraftDomesticService;
import com.tiximax.txm.Utils.AccountUtils;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

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
    @GetMapping("")
        public ResponseEntity<Page<DraftDomesticResponse>> getAllDraftDomestic(
                @RequestParam(required = false) String customerCode,
                @RequestParam(required = false) String shipmentCode,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size
        ){
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                draftDomesticService.getAllDraftDomestic(
                        customerCode,
                        shipmentCode,
                        pageable
                )
        );
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
}

