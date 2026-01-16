package com.tiximax.txm.Repository;


import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tiximax.txm.Entity.DraftDomestic;
import com.tiximax.txm.Enums.Carrier;
import com.tiximax.txm.Enums.DraftDomesticStatus;
import com.tiximax.txm.Enums.WarehouseStatus;

public interface DraftDomesticRepository extends JpaRepository<DraftDomestic, Long> {
@Query("""
    SELECT DISTINCT d
    FROM DraftDomestic d
    JOIN d.customer c
    JOIN d.staff s
    LEFT JOIN d.shippingList sl
    WHERE
      (:customerCode IS NULL OR c.customerCode = :customerCode)
      AND (:shipmentCode IS NULL OR sl = :shipmentCode)
      AND (:status IS NULL OR d.status = :status)
      AND (:carrier IS NULL OR d.carrier = :carrier)
      AND (:staffId IS NULL OR s.id = :staffId)
""")
Page<DraftDomestic> findAllWithFilter(
        @Param("customerCode") String customerCode,
        @Param("shipmentCode") String shipmentCode,
        @Param("status") DraftDomesticStatus status,
        @Param("carrier") Carrier carrier,
        @Param("staffId") Long staffId,
        Pageable pageable
);


    boolean existsByShipCode(String shipCode);

  @Query("""
    SELECT d.shipCode
    FROM DraftDomestic d
    WHERE d.shipCode LIKE CONCAT(:customerCode, '-%')
    ORDER BY d.shipCode DESC
    """)
    List<String> findShipCodesByCustomer(
            @Param("customerCode") String customerCode
    );

    @Query("""
    SELECT d
    FROM DraftDomestic d
    WHERE d.status = 'LOCKED'
      AND d.createdAt BETWEEN :startDate AND :endDate
    ORDER BY d.createdAt DESC
""")
List<DraftDomestic> findLockedBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
);


@Query("""
    SELECT DISTINCT d
    FROM DraftDomestic d
    JOIN d.shippingList s
    JOIN Warehouse w ON w.trackingCode = s
    WHERE w.status = com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
      AND d.status = 'LOCKED'
      AND (:routeId IS NULL OR w.orders.route.routeId = :routeId)
      AND d.createdAt >= COALESCE(:startDateTime, d.createdAt)
      AND d.createdAt <= COALESCE(:endDateTime, d.createdAt)
""")
Page<DraftDomestic> getDraftToExport(
        @Param("routeId") Long routeId,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        Pageable pageable
);




      @Query("""
      SELECT s
      FROM DraftDomestic d
      JOIN d.shippingList s
      WHERE s IN :codes
  """)
  List<String> findExistingTrackingCodesInDraft(
          @Param("codes") List<String> codes
  );
  
  Optional<DraftDomestic> findByVNPostTrackingCode(String vnPostTrackingCode);

  Optional<DraftDomestic> findByShipCode(String shipCode);
  
 @Query("""
    SELECT DISTINCT d
    FROM DraftDomestic d, Warehouse w
    WHERE w.trackingCode MEMBER OF d.shippingList
      AND w.status = :status
      AND d.status = 'DRAFT'
      AND (:routeId IS NULL OR w.orders.route.routeId = :routeId)
""")
Page<DraftDomestic> availableToLock(
        @Param("status") WarehouseStatus status,
        @Param("routeId") Long routeId,
        Pageable pageable
);


      @Query("""
    SELECT DISTINCT d
    FROM DraftDomestic d
    JOIN d.shippingList sl
    WHERE d.status = 'DRAFT'
      AND sl IN :shipmentCodes
""")
List<DraftDomestic> findDraftByShipmentCodes(
        @Param("shipmentCodes") Collection<String> shipmentCodes
);


}


