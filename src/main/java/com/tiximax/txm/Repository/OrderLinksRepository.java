package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.OrderLinkStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository

public interface OrderLinksRepository extends JpaRepository<OrderLinks, Long> {

    boolean existsByTrackingCode(String orderLinkCode);
    
    boolean existsByShipmentCode(String shipmentCode);

    List<OrderLinks> findByTrackingCodeIn(List<String> trackingCodes);

    List<OrderLinks> findByOrdersOrderId(Long orderId);

    @Query("SELECT ol FROM OrderLinks ol LEFT JOIN FETCH ol.orders WHERE ol.shipmentCode = :shipmentCode")
    List<OrderLinks> findByShipmentCode(@Param("shipmentCode") String shipmentCode);

    @Query("""
    SELECT ol
    FROM OrderLinks ol
    WHERE ol.shipmentCode IN :shipmentCodes
      AND ol.shipmentCode IS NOT NULL
      AND TRIM(ol.shipmentCode) <> ''
""")
    List<OrderLinks> findByShipmentCodeIn( @Param("shipmentCodes") List<String> shipmentCodes);

        @Query("""
        SELECT ol
        FROM OrderLinks ol
        WHERE ol.orders.customer.customerCode = :customerCode
          AND ol.shipmentCode IS NOT NULL
          AND ol.shipmentCode <> ''
          AND ol.status = :status
          AND ol.partialShipment IS NULL
  """)
  List<OrderLinks> findLinksInWarehouseWithoutPartialShipment(
          @Param("customerCode") String customerCode,
          @Param("status") OrderLinkStatus status
  );


    // OrderLinksRepository.java
    @Query("""
            SELECT ol FROM OrderLinks ol 
            WHERE ol.status = 'DA_MUA' 
            AND (ol.shipmentCode IS NULL OR ol.shipmentCode = '')
            """)
    List<OrderLinks> findPendingShipmentLinks();

     List<OrderLinks> findByWarehouse(Warehouse warehouse);

    @Query("""
    SELECT DISTINCT ol.shipmentCode
    FROM OrderLinks ol
    WHERE ol.status = 'DA_MUA'
      AND ol.shipmentCode IS NOT NULL
      AND ol.shipmentCode != ''
      AND (:keyword IS NULL OR ol.shipmentCode LIKE %:keyword%)
    """)
    List<String> suggestShipmentCodes(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(ol) FROM OrderLinks ol WHERE ol.orders.createdAt BETWEEN :start AND :end")
    long countByOrdersCreatedAtBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    boolean existsByShipmentCodeAndLinkIdNot(String newShipmentCode, Long orderLinkId);

    @Query("SELECT ol FROM OrderLinks ol WHERE ol.shipmentCode IN :codes")
    List<OrderLinks> findAllByShipmentCodeIn(@Param("codes") List<String> codes);

//    Long countByOrders_CreatedAtBetween(LocalDateTime start, LocalDateTime end);
    @Query("SELECT COUNT(ol) " +
            "FROM OrderLinks ol " +
            "WHERE ol.orders.createdAt BETWEEN :start AND :end")
    Long countByOrders_CreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Trong OrderLinksRepository
    @Query("""
    SELECT DISTINCT ol
    FROM OrderLinks ol
    WHERE ol.shipmentCode IN :shipmentCodes
      AND ol.shipmentCode IS NOT NULL
      AND TRIM(ol.shipmentCode) <> ''
""")
    Set<OrderLinks> findByShipmentCodeIn( @Param("shipmentCodes") Collection<String> shipmentCodes);

    @Query("SELECT ol FROM OrderLinks ol WHERE ol.orders.customer = :customer AND ol.shipmentCode IS NOT NULL")
    List<OrderLinks> findByCustomerWithShipment(@Param("customer") Customer customer);

    @Query("SELECT MONTH(ol.orders.createdAt), COUNT(ol) FROM OrderLinks ol WHERE YEAR(ol.orders.createdAt) = :year GROUP BY MONTH(ol.orders.createdAt)")
    List<Object[]> countLinksByMonth(@Param("year") int year);

}
