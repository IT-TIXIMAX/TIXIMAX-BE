package com.tiximax.txm.API;

import com.tiximax.txm.Entity.FlightShipment;
import com.tiximax.txm.Model.FlightShipmentRequest;
import com.tiximax.txm.Model.FlightShipmentResponse;
import com.tiximax.txm.Service.FlightShipmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/flight-shipment")
@SecurityRequirement(name = "bearerAuth")

public class FlightShipmentController {

    @Autowired
    private FlightShipmentService flightShipmentService;

    @PostMapping
    public ResponseEntity<FlightShipment> create(@RequestBody FlightShipmentRequest request) {
        return ResponseEntity.ok(flightShipmentService.createFlightShipment(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightShipmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(flightShipmentService.getFlightShipmentId(id));
    }

    @GetMapping
    public ResponseEntity<List<FlightShipmentResponse>> getAll() {
        return ResponseEntity.ok(flightShipmentService.getAllFlightShipment());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FlightShipmentResponse> updateFlightShipment(@PathVariable Long id,
                                                            @RequestBody FlightShipmentRequest request) {
        return ResponseEntity.ok(flightShipmentService.updateFlightShipment(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlightShipment(@PathVariable Long id) {
        flightShipmentService.deleteFlightShipment(id);
        return ResponseEntity.noContent().build();
    }
}
