package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Model.DTORequest.Route.RouteRequest;
import com.tiximax.txm.Model.DTOResponse.Route.ExchangeRateList;
import com.tiximax.txm.Service.AccountRouteService;
import com.tiximax.txm.Service.RouteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/routes")
@SecurityRequirement(name = "bearerAuth")

public class RouteController {

    @Autowired
    private RouteService routeService;

    @Autowired
    private AccountRouteService accountRouteService;

    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody RouteRequest routeRequest) {
        Route route = routeService.createRoute(routeRequest);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<Route> getRouteById(@PathVariable Long routeId) {
        Route route = routeService.getRouteById(routeId);
        return ResponseEntity.ok(route);
    }

    @GetMapping
    public ResponseEntity<List<Route>> getAllRoutes() {
        List<Route> routes = routeService.getAllRoutes();
        return ResponseEntity.ok(routes);
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<Route> updateRoute(@PathVariable Long routeId, @RequestBody RouteRequest routeRequest) {
        Route route = routeService.updateRoute(routeId, routeRequest);
        return ResponseEntity.ok(route);
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long routeId) {
        routeService.deleteRoute(routeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exchange-rates")
    public ResponseEntity<?> getExchangeRates() {
        try {
            ExchangeRateList rates = routeService.getExchangeRate();
            return ResponseEntity.ok(rates);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch exchange rates");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @PutMapping("/update-exchange-rates")
    public ResponseEntity<?> updateExchangeRates() {
        try {
            routeService.updateExchangeRate();
            return ResponseEntity.ok().body("Exchange rates updated successfully");
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update exchange rates");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

}