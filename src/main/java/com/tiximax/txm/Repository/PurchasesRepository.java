package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Enums.PurchaseFilter;

import com.tiximax.txm.Model.PurchaseProfitResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Repository

public interface PurchasesRepository extends JpaRepository<Purchases, Long> {

    boolean existsByPurchaseCode(String purchaseCode);

    @Query("SELECT COALESCE(SUM(p.finalPriceOrder), 0) FROM Purchases p WHERE p.orders.orderId = :orderId")
    BigDecimal getTotalFinalPriceByOrderId(@Param("orderId") Long orderId);

    @Query("""
    SELECT DISTINCT p FROM Purchases p
    JOIN FETCH p.orderLinks ol
    JOIN p.orders o
    WHERE o.route.routeId IN :routeIds
      AND EXISTS (
        SELECT 1 FROM OrderLinks link
        WHERE link.purchase = p
          AND (link.shipmentCode IS NULL OR link.shipmentCode = '')
      )
    """)
    Page<Purchases> findPurchasesWithPendingShipmentByRoutes(
            @Param("routeIds") Set<Long> routeIds,
            Pageable pageable
    );

@Query(
    value = """
        SELECT * FROM (
            SELECT DISTINCT
                p.*,
                CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM order_links ol2
                        WHERE ol2.purchase_id = p.purchase_id
                          AND (ol2.shipment_code IS NULL OR TRIM(ol2.shipment_code) = '')
                          AND ol2.status IN ('DA_MUA', 'DAU_GIA_THANH_CONG')
                    ) THEN 0
                    ELSE 1
                END AS sort_value
            FROM purchases p
            JOIN orders o ON o.order_id = p.order_id
            JOIN customer c ON c.account_id = o.customer_id
            JOIN order_links ol ON ol.purchase_id = p.purchase_id
            WHERE o.route_id IN :routeIds
              AND (
                    (:status IS NULL AND ol.status IN ('DA_MUA', 'DAU_GIA_THANH_CONG'))
                    OR (:status IS NOT NULL AND ol.status = :status)
                  )
              AND p.is_purchased = true
              AND NOT EXISTS (
                    SELECT 1
                    FROM order_links olx
                    WHERE olx.purchase_id = p.purchase_id
                      AND olx.shipment_code IS NOT NULL
                      AND TRIM(olx.shipment_code) <> ''
              )
              AND (
                    :orderCode IS NULL
                    OR o.order_code ILIKE CONCAT('%', CAST(:orderCode AS TEXT), '%')
              )
              AND (
                    :customerCode IS NULL
                    OR c.customer_code ILIKE CONCAT('%', CAST(:customerCode AS TEXT), '%')
              )
        ) t
        ORDER BY t.sort_value ASC, t.purchase_id DESC
        """,
    countQuery = """
        SELECT COUNT(DISTINCT p.purchase_id)
        FROM purchases p
        JOIN orders o ON o.order_id = p.order_id
        JOIN customer c ON c.account_id = o.customer_id
        JOIN order_links ol ON ol.purchase_id = p.purchase_id
        WHERE o.route_id IN :routeIds
          AND (
                (:status IS NULL AND ol.status IN ('DA_MUA', 'DAU_GIA_THANH_CONG'))
                OR (:status IS NOT NULL AND ol.status = :status)
              )
          AND p.is_purchased = true
          AND NOT EXISTS (
                SELECT 1
                FROM order_links olx
                WHERE olx.purchase_id = p.purchase_id
                  AND olx.shipment_code IS NOT NULL
                  AND TRIM(olx.shipment_code) <> ''
          )
          AND (
                :orderCode IS NULL
                OR o.order_code ILIKE CONCAT('%', CAST(:orderCode AS TEXT), '%')
          )
          AND (
                :customerCode IS NULL
                OR c.customer_code ILIKE CONCAT('%', CAST(:customerCode AS TEXT), '%')
          )
        """,
    nativeQuery = true
)
Page<Purchases> findPurchasesSortedByPendingShipment(
        @Param("routeIds") Set<Long> routeIds,
        @Param("status") String status,
        @Param("orderCode") String orderCode,
        @Param("customerCode") String customerCode,
        Pageable pageable
);


 @Query(
    value = """
        SELECT DISTINCT p.*
        FROM purchases p
        JOIN orders o ON o.order_id = p.order_id
        JOIN customer c ON c.account_id = o.customer_id
        JOIN order_links ol
            ON ol.purchase_id = p.purchase_id
           AND (
                (:status IS NULL AND ol.status IN ('DA_NHAP_KHO_NN', 'DA_MUA', 'DAU_GIA_THANH_CONG'))
                OR (:status IS NOT NULL AND ol.status = :status)
           )
        WHERE o.route_id IN :routeIds
          AND (
                :orderCode IS NULL
                OR o.order_code ILIKE CONCAT('%', CAST(:orderCode AS TEXT), '%')
          )
          AND (
                :customerCode IS NULL
                OR c.customer_code ILIKE CONCAT('%', CAST(:customerCode AS TEXT), '%')
          )
          AND (
                :shipmentCode IS NULL
                OR ol.shipment_code ILIKE CONCAT('%', CAST(:shipmentCode AS TEXT), '%')
          )
        ORDER BY p.purchase_id DESC
        """,
    countQuery = """
        SELECT COUNT(DISTINCT p.purchase_id)
        FROM purchases p
        JOIN orders o ON o.order_id = p.order_id
        JOIN customer c ON c.account_id = o.customer_id
        JOIN order_links ol
            ON ol.purchase_id = p.purchase_id
           AND (
                (:status IS NULL AND ol.status IN ('DA_NHAP_KHO_NN', 'DA_MUA', 'DAU_GIA_THANH_CONG'))
                OR (:status IS NOT NULL AND ol.status = :status)
           )
        WHERE o.route_id IN :routeIds
          AND (
                :orderCode IS NULL
                OR o.order_code ILIKE CONCAT('%', CAST(:orderCode AS TEXT), '%')
          )
          AND (
                :customerCode IS NULL
                OR c.customer_code ILIKE CONCAT('%', CAST(:customerCode AS TEXT), '%')
          )
          AND (
                :shipmentCode IS NULL
                OR ol.shipment_code ILIKE CONCAT('%', CAST(:shipmentCode AS TEXT), '%')
          )
        """,
    nativeQuery = true
)
Page<Purchases> findPurchasesWithFilteredOrderLinks(
        @Param("routeIds") Set<Long> routeIds,
        @Param("status") String status,
        @Param("orderCode") String orderCode,
        @Param("customerCode") String customerCode,
        @Param("shipmentCode") String shipmentCode,
        Pageable pageable
);

    @Query(value = """
    SELECT
        SUM(net_profit_per_purchase) AS total_net_profit
    FROM (
        SELECT
            (ol_sum.total_web_sum * COALESCE(o.exchange_rate,0)
             - COALESCE(p.final_price_order,0) * COALESCE(rer.exchange_rate,0)
            ) AS net_profit_per_purchase,
            ol_sum.total_web_sum
        FROM purchases p
        JOIN orders o ON o.order_id = p.order_id
        JOIN route r ON r.route_id = o.route_id

        LEFT JOIN LATERAL (
            SELECT rer.exchange_rate
            FROM route_exchange_rate rer
            WHERE rer.route_id = r.route_id
              AND DATE(p.purchase_time) >= rer.start_date
              AND (rer.end_date IS NULL OR DATE(p.purchase_time) <= rer.end_date)
            ORDER BY rer.start_date DESC
            LIMIT 1
        ) rer ON TRUE

        LEFT JOIN LATERAL (
            SELECT COALESCE(SUM(ol2.total_web),0) AS total_web_sum
            FROM order_links ol2
            WHERE ol2.purchase_id = p.purchase_id
        ) ol_sum ON TRUE

        WHERE p.purchase_time >= :start
          AND p.purchase_time <  :endPlusOne
          AND o.route_id = :routeId
          AND ol_sum.total_web_sum > 0
    ) t
    """, nativeQuery = true)
    BigDecimal calculateEstimatedPurchaseProfitByRoute(
            @Param("start") LocalDateTime start,
            @Param("endPlusOne") LocalDateTime endPlusOne,
            @Param("routeId") Long routeId);

    @Query(value = """
    SELECT
        SUM(net_profit_per_purchase) AS total_net_profit
    FROM (
        SELECT
            (ol_sum.total_web_sum * COALESCE(o.exchange_rate,0)
             - COALESCE(p.final_price_order,0) * COALESCE(rer.exchange_rate,0)
            ) AS net_profit_per_purchase,
            ol_sum.total_web_sum
        FROM purchases p
        JOIN orders o ON o.order_id = p.order_id
        JOIN route r ON r.route_id = o.route_id

        LEFT JOIN LATERAL (
            SELECT rer.exchange_rate
            FROM route_exchange_rate rer
            WHERE rer.route_id = r.route_id
              AND DATE(p.purchase_time) >= rer.start_date
              AND (rer.end_date IS NULL OR DATE(p.purchase_time) <= rer.end_date)
            ORDER BY rer.start_date DESC
            LIMIT 1
        ) rer ON TRUE

        LEFT JOIN LATERAL (
            SELECT COALESCE(SUM(ol2.total_web),0) AS total_web_sum
            FROM order_links ol2
            WHERE ol2.purchase_id = p.purchase_id
              AND ol2.status NOT IN ('CHO_MUA','DA_MUA','DAU_GIA_THANH_CONG','MUA_SAU','DA_HUY')
        ) ol_sum ON TRUE

        WHERE p.purchase_time >= :start
          AND p.purchase_time <  :endPlusOne
          AND o.route_id = :routeId
          AND ol_sum.total_web_sum > 0
    ) t
    """, nativeQuery = true)
    BigDecimal calculateActualPurchaseProfitByRoute(
            @Param("start") LocalDateTime start,
            @Param("endPlusOne") LocalDateTime endPlusOne,
            @Param("routeId") Long routeId
    );

}
