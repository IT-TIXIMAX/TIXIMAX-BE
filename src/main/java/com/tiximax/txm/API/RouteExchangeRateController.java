package com.tiximax.txm.API;

import com.tiximax.txm.Entity.RouteExchangeRate;
import com.tiximax.txm.Model.EffectiveRateResponse;
import com.tiximax.txm.Service.RouteExchangeRateService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/route-exchange")
@SecurityRequirement(name = "bearerAuth")

public class RouteExchangeRateController {
    @Autowired
    private RouteExchangeRateService routeExchangeRateService;

    @PostMapping
    public ResponseEntity<RouteExchangeRate> addExchange(@RequestBody RouteExchangeRate request) {
        return ResponseEntity.ok(routeExchangeRateService.addExchange(request));
    }

    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<RouteExchangeRate>> getByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(routeExchangeRateService.findByRouteId(routeId));
    }

    @GetMapping("/effective/{routeId}")
    public ResponseEntity<EffectiveRateResponse> getEffectiveRate(
            @PathVariable Long routeId,
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(routeExchangeRateService.getEffectiveRate(routeId, date));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        routeExchangeRateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
