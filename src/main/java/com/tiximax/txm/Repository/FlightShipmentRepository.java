package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.ExpenseRequest;
import com.tiximax.txm.Entity.FlightShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface FlightShipmentRepository  extends JpaRepository<FlightShipment, Long> {
    boolean existsByFlightCode(String flightCode);

    @Query(value = """
    WITH customer_flight_weight AS (
        SELECT
            o.customer_id,
            SUM(DISTINCT w.net_weight) AS total_net_weight_customer
        FROM packing p
        INNER JOIN warehouse w ON w.packing_id = p.packing_id
        INNER JOIN orders o ON w.order_id = o.order_id
        WHERE p.flight_code = :flightCode
          AND w.net_weight IS NOT NULL
          AND w.net_weight > 0
        GROUP BY o.customer_id
    ),
    customer_cost AS (
        SELECT
            GREATEST(
                CAST(cw.total_net_weight_customer AS numeric),
                COALESCE(MAX(r.min_weight), 0)
            ) AS effective_weight,
            AVG(o.price_ship) AS price_ship_customer
        FROM customer_flight_weight cw
        INNER JOIN orders o ON o.customer_id = cw.customer_id
        INNER JOIN route r ON o.route_id = r.route_id
        WHERE EXISTS (
            SELECT 1 FROM warehouse w2
            INNER JOIN packing p2 ON w2.packing_id = p2.packing_id
            WHERE w2.order_id = o.order_id
              AND p2.flight_code = :flightCode
              AND w2.net_weight > 0
        )
        GROUP BY cw.customer_id, cw.total_net_weight_customer
    )
    SELECT COALESCE(SUM(effective_weight * price_ship_customer), 0)
    FROM customer_cost
""", nativeQuery = true)
    BigDecimal calculateProfitForFlight(@Param("flightCode") String flightCode);

}
