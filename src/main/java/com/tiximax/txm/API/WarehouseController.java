package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Model.DTORequest.Warehouse.WarehouseRequest;
import com.tiximax.txm.Model.DTOResponse.OrderLink.InfoShipmentCode;
import com.tiximax.txm.Model.DTOResponse.Warehouse.WarehouseSummary;
import com.tiximax.txm.Service.WarehouseService;
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
@RequestMapping("/warehouse")
@SecurityRequirement(name = "bearerAuth")

public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

 
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
    @PostMapping("/shipment/{shipmentCode}")
    public ResponseEntity<Warehouse> createWarehouseEntryByShipmentCode(
            @PathVariable String shipmentCode,
            @RequestBody WarehouseRequest warehouseRequest) {
        Warehouse warehouse = warehouseService.createWarehouseEntryByShipmentCode(shipmentCode, warehouseRequest);
        return ResponseEntity.ok(warehouse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Warehouse> getWarehouseById(@PathVariable Long id) {
        Optional<Warehouse> warehouseOptional = warehouseService.getWarehouseById(id);
        return warehouseOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
    @PostMapping("/list-shipment")
    public ResponseEntity<String> createWarehouseEntryByListShipmentCode(@RequestBody List<String> shipmentCodes) {
        return ResponseEntity.ok(warehouseService.createWarehouseEntryByListShipmentCodes(shipmentCodes));
    }

    @GetMapping("/{page}/{size}/ready-warehouses")
    public ResponseEntity<Page<WarehouseSummary>> getReadyWarehouses(
            @PathVariable int page,
            @PathVariable int size,
            @RequestParam(required = false) String trackingCode
    ) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<WarehouseSummary> warehousePage =
                warehouseService.getWarehousesForPacking(pageable, trackingCode);

        return ResponseEntity.ok(warehousePage);
    }

    @GetMapping("/totals")
    public ResponseEntity<Map<String, Double>> getWarehouseTotals() {
        return ResponseEntity.ok(warehouseService.calculateWarehouseTotals());
    }
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN','STAFF_SALE')")
    @PutMapping("/{trackingCode}")
    public ResponseEntity<Warehouse> updateWarehouseNetWeight(
            @PathVariable String trackingCode,
            @RequestBody  WarehouseRequest request) {
        Warehouse updatedWarehouse = warehouseService.updateWarehouseNetWeight(trackingCode, request);
        return ResponseEntity.ok(updatedWarehouse);
    }

    @GetMapping("/check-netweight/{trackingCode}")
    public ResponseEntity<Boolean> checkNetWeight(@PathVariable String trackingCode) {
        return ResponseEntity.ok(warehouseService.hasNetWeight(trackingCode));
    }

    @GetMapping("/suggest-shipment")
    public ResponseEntity<List<String>> suggestShipment(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(warehouseService.suggestShipmentCodes(keyword));
    }
    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN','STAFF_SALE')")
    @PatchMapping("/{trackingCode}")
    public ResponseEntity<Warehouse> updateWarehouse(
            @PathVariable String trackingCode,
            @RequestBody WarehouseRequest request) {
        Warehouse updatedWarehouse = warehouseService.updateWarehouse(trackingCode, request);
        return ResponseEntity.ok(updatedWarehouse);
    }
}
