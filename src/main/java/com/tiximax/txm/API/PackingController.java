package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Model.DTORequest.Packing.AssignFlightRequest;
import com.tiximax.txm.Model.DTORequest.Packing.PackingRequest;
import com.tiximax.txm.Model.DTOResponse.Packing.PackingCheckResponse;
import com.tiximax.txm.Model.DTOResponse.Packing.PackingEligibleOrder;
import com.tiximax.txm.Model.DTOResponse.Packing.PackingExport;
import com.tiximax.txm.Model.DTOResponse.Packing.PackingInWarehouse;
import com.tiximax.txm.Service.PackingService;
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

@RestController
@CrossOrigin
@RequestMapping("/packings")
@SecurityRequirement(name = "bearerAuth")

public class PackingController {

    @Autowired
    private PackingService packingService;

    @GetMapping("/eligible-orders/{page}/{size}")
    public ResponseEntity<Page<PackingEligibleOrder>> getEligibleOrdersForPacking(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PackingEligibleOrder> eligibleOrdersPage = packingService.getEligibleOrdersForPacking(pageable);
        return ResponseEntity.ok(eligibleOrdersPage);
    }

    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
    @PostMapping
    public ResponseEntity<Packing> createPacking(@RequestBody PackingRequest request) {
        Packing packing = packingService.createPacking(request);
        return ResponseEntity.ok(packing);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Packing> getPackingById(@PathVariable Long id) {
        Packing packing = packingService.getPackingById(id);
        if (packing == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(packing);
    }
    @GetMapping("/in-warehouse/{page}/{size}")
    public ResponseEntity<Page<PackingInWarehouse>> getPackingsInWarehouse(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("packedDate").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PackingInWarehouse> packingsPage = packingService.getPackingsInWarehouse(pageable);
        return ResponseEntity.ok(packingsPage);
    }
    @GetMapping("/awaiting-flight/{page}/{size}")
    public ResponseEntity<Page<Packing>> getPackingsAwaitingFlight(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("packedDate").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Packing> packingsPage = packingService.getPackingsAwaitingFlight(pageable);
        return ResponseEntity.ok(packingsPage);
    }
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
    @PutMapping("/assign-flight")
    public ResponseEntity<String> assignFlightCode(@RequestBody AssignFlightRequest request) {
        List<Long> packingIds = request.getPackingIds();
        String flightCode = request.getFlightCode();
        if (packingIds == null || packingIds.isEmpty() || flightCode == null || flightCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        packingService.assignFlightCode(packingIds, flightCode);
        return ResponseEntity.ok("Flight code assigned successfully");
    }

    @GetMapping("/flying-away/{page}/{size}")
    public ResponseEntity<Page<Packing>> getPackingsWithFlightStatus(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("packedDate").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Packing> packingsPage = packingService.getPackingsWithFlightStatus(pageable);
        return ResponseEntity.ok(packingsPage);
    }
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
    @PatchMapping("/packing/{packingCode}/remove-shipments")
    public ResponseEntity<Packing> removeShipmentsFromPacking(
            @PathVariable String packingCode,
            @RequestBody List<String> shipmentCodes) {
        Packing updatedPacking = packingService.removeShipmentFromPacking(packingCode, shipmentCodes);
        return ResponseEntity.ok(updatedPacking);
    }
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
    @PatchMapping("/{packingCode}/add-shipments")
    public ResponseEntity<Packing> addShipmentsToPacking(
            @PathVariable String packingCode,
            @RequestBody List<String> shipmentCodes) {

        Packing updatedPacking = packingService.addShipmentsToPacking(packingCode, shipmentCodes);
        return ResponseEntity.ok(updatedPacking);
    }

    @GetMapping("/{packingCode}/list")
    public ResponseEntity<List<String>> getPackingList(
            @PathVariable String packingCode) {
        List<String> packingList = packingService.getPackingListByCode(packingCode);
        return ResponseEntity.ok(packingList);
    }
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
   @PostMapping("/check")
    public ResponseEntity<PackingCheckResponse> checkPacking(
        @RequestBody List<String> shipmentCodes) {
        PackingCheckResponse result =
            packingService.checkCreatePacking(shipmentCodes);
    return ResponseEntity.ok(result);
}

    @GetMapping("/export")
    public List<PackingExport> exportChoBay(
            @RequestParam("packingIds") List<Long> packingIds) {
        return packingService.getPackingExportByIdsChoBay(packingIds);
    }

    @GetMapping("/{packingId}/warehouses")
    public ResponseEntity<List<Warehouse>> getWarehousesByPackingId(@PathVariable Long packingId) {
        List<Warehouse> warehouses = packingService.getWarehousesByPackingId(packingId);
        return ResponseEntity.ok(warehouses);
    }

}
