package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Enums.PurchaseFilter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    @Query("""
        SELECT COALESCE(
            SUM(o.finalPriceOrder - (p.finalPriceOrder * :exchangeRate)), 
            0
        )
        FROM Purchases p
        JOIN p.orders o
        WHERE p.purchased = true
          AND p.purchaseTime >= :start
          AND p.purchaseTime < :end
          AND o.route.routeId = :routeId
        """)
    BigDecimal calculatePurchaseProfitByRoute(
            @Param("exchangeRate") BigDecimal exchangeRate,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("routeId") Long routeId);
}
