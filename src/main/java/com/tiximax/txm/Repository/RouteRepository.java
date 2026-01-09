package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByName(String name);

    boolean existsByRouteId(Long routeId);
}
