package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.ExpenseRequest;
import com.tiximax.txm.Entity.FlightShipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightShipmentRepository  extends JpaRepository<FlightShipment, Long> {
    boolean existsByFlightCode(String flightCode);
}
