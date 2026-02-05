package com.tiximax.txm.Repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tiximax.txm.Entity.DraftDomestic;
import com.tiximax.txm.Enums.Carrier;
import com.tiximax.txm.Enums.DraftDomesticStatus;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Model.DTOResponse.DraftDomestic.DraftDomesticResponse;

public interface DraftDomesticRepository extends JpaRepository<DraftDomestic, Long> {
 @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.DraftDomestic.DraftDomesticResponse(
        d.id,
        c.name,
        d.phoneNumber,
        d.address,
        d.shipCode,
        d.VNPostTrackingCode,
        s.staffCode,
        d.payment,
        CAST(d.weight AS string)
    )
    FROM DraftDomestic d
    JOIN d.customer c
    JOIN d.staff s
    WHERE
        (:customerCode IS NULL OR c.customerCode = :customerCode)
        AND (:status IS NULL OR d.status = :status)
        AND (:carrier IS NULL OR d.carrier = :carrier)
        AND (:staffId IS NULL OR s.id = :staffId)
        AND (
            :shipmentCode IS NULL
            OR EXISTS (
                SELECT 1
                FROM DraftDomesticShipment ds
                WHERE ds.draftDomestic.id = d.id
                  AND ds.shipmentCode = :shipmentCode
            )
        )
    ORDER BY d.createdAt DESC
""")
Slice<DraftDomesticResponse> findDraftDomesticSlice(
        String customerCode,
        DraftDomesticStatus status,
        Carrier carrier,
        Long staffId,
        String shipmentCode,
        Pageable pageable
);
  @Query("""
      SELECT ds.draftDomestic.id, ds.shipmentCode
      FROM DraftDomesticShipment ds
      WHERE ds.draftDomestic.id IN :draftIds
  """)
  List<Object[]> findShipmentCodesByDraftIds(
          @Param("draftIds") List<Long> draftIds
  );
@Query("""
    SELECT DISTINCT d
    FROM DraftDomesticShipment s
    JOIN s.draftDomestic d
    WHERE s.shipmentCode IN :codes
""")
List<DraftDomestic> findDraftByShipmentCodes(Set<String> codes);



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
    WHERE d.status = :status
      AND (:staffId IS NULL OR d.staff.accountId = :staffId)
      AND d.carrier = :carrier
      AND d.createdAt >= :startDate
      AND d.createdAt <  :endDate
    ORDER BY d.createdAt DESC
""")
List<DraftDomestic> findLockedBetween(
        @Param("status") DraftDomesticStatus status,
        @Param("carrier") Carrier carrier,
        @Param("staffId") Long staffId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
);


@Query("""
    SELECT DISTINCT d
    FROM DraftDomestic d
    WHERE d.status = 'DRAFT'
      AND (:startDateTime IS NULL OR d.createdAt >= :startDateTime)
      AND (:endDateTime IS NULL OR d.createdAt < :endDateTime)
      AND EXISTS (
          SELECT 1
          FROM DraftDomesticShipment s
          JOIN Warehouse w ON w.trackingCode = s.shipmentCode
          WHERE s.draftDomestic = d
            AND (:routeId IS NULL OR w.orders.route.routeId = :routeId)
      )
""")
Page<DraftDomestic> getDraftToExport(
        @Param("routeId") Long routeId,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        Pageable pageable
);





   @Query("""
    SELECT DISTINCT s.shipmentCode
    FROM DraftDomesticShipment s
    WHERE s.shipmentCode IN :codes
      AND s.draftDomestic.status = 'DRAFT'
""")
List<String> findExistingTrackingCodesInDraft(
        @Param("codes") List<String> codes
);

  
  Optional<DraftDomestic> findByVNPostTrackingCode(String vnPostTrackingCode);

  Optional<DraftDomestic> findByShipCode(String shipCode);
  
@Query("""
    SELECT DISTINCT d
    FROM DraftDomestic d
    JOIN DraftDomesticShipment s ON s.draftDomestic = d
    JOIN Warehouse w ON w.trackingCode = s.shipmentCode
    WHERE w.status = :status
      AND d.status = 'DRAFT'
      AND (:routeId IS NULL OR w.orders.route.routeId = :routeId)
""")
Page<DraftDomestic> availableToLock(
        @Param("status") WarehouseStatus status,
        @Param("routeId") Long routeId,
        Pageable pageable
);

List<DraftDomestic> findByStatus(DraftDomesticStatus status);

      @Query("""
    SELECT DISTINCT d
FROM DraftDomestic d
JOIN DraftDomesticShipment s
     ON s.draftDomestic = d
  WHERE d.status = 'DRAFT'
  AND s.shipmentCode IN :shipmentCodes
""")
List<DraftDomestic> findDraftByShipmentCodes(
        @Param("shipmentCodes") Collection<String> shipmentCodes
);

@Query("""
 SELECT DISTINCT d
    FROM DraftDomestic d
    WHERE d.staff.accountId = :staffId
        And d.status = 'DRAFT'
        And d.payment = false
        AND (:payment IS NULL OR d.payment = :payment)
    """)
 Page<DraftDomestic> findByStaff(
        @Param("staffId") Long staffId,
        @Param ("payment") Boolean payment,
        Pageable pageable
 );
        
  @Query("""
       SELECT DISTINCT d
        FROM DraftDomestic d
        LEFT JOIN DraftDomesticShipment s
        ON s.draftDomestic = d
        WHERE d.staff.accountId = :staffId
        AND d.status = 'DRAFT'
        AND (:payment IS NULL OR d.payment = :payment)
        AND (
        LOWER(d.shipCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(s.shipmentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        ORDER BY d.createdAt DESC
    """)
    Page<DraftDomestic> searchByStaffAndKeyword(
            @Param("staffId") Long staffId,
            @Param("keyword") String keyword,
            @Param ("payment") Boolean payment
          ,Pageable pageable
    );
    
}


