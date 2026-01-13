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
import com.tiximax.txm.Enums.WarehouseStatus;

public interface DraftDomesticRepository extends JpaRepository<DraftDomestic, Long> {
@Query(
    value = """
        SELECT DISTINCT d
        FROM DraftDomestic d
        JOIN d.customer c
        JOIN d.staff s
        LEFT JOIN d.shippingList sl
        WHERE
          (:customerCode IS NULL OR c.customerCode = :customerCode)
          AND (:shipmentCode IS NULL OR sl = :shipmentCode)
          AND (:isLocked IS NULL OR d.isLocked = :isLocked)
          AND (:isExported IS NULL OR d.isExported = :isExported)
          AND (:staffId IS NULL OR s.id = :staffId)
    """,
    countQuery = """
        SELECT COUNT(d)
        FROM DraftDomestic d
        JOIN d.customer c
        JOIN d.staff s
        WHERE
          (:customerCode IS NULL OR c.customerCode = :customerCode)
          AND (:isLocked IS NULL OR d.isLocked = :isLocked)
          AND (:isExported IS NULL OR d.isExported = :isExported)
          AND (:staffId IS NULL OR s.id = :staffId)
          AND (
              :shipmentCode IS NULL
              OR EXISTS (
                  SELECT 1 FROM d.shippingList sl WHERE sl = :shipmentCode
              )
          )
    """
)
Page<DraftDomestic> findAllWithFilter(
        @Param("customerCode") String customerCode,
        @Param("shipmentCode") String shipmentCode,
        @Param("isLocked") Boolean isLocked,
        @Param("isExported") Boolean isExported,
        @Param("staffId") Long staffId,
        Pageable pageable
);
    boolean existsByShipCode(String shipCode);

    @Query("""
        SELECT d.shipCode
        FROM DraftDomestic d
        WHERE d.shipCode LIKE CONCAT(:baseCode, '-%')
        ORDER BY d.shipCode DESC
    """)
    List<String> findShipCodesByBaseCode(@Param("baseCode") String baseCode);


@Query(
    value = """
        SELECT DISTINCT d
        FROM DraftDomestic d
        JOIN d.shippingList s
        JOIN Warehouse w ON w.trackingCode = s
        WHERE w.status = com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
          AND d.isLocked = true
          AND (:routeId IS NULL OR w.orders.route.routeId = :routeId)
          AND d.createdAt >= COALESCE(:startDateTime, d.createdAt)
          AND d.createdAt <= COALESCE(:endDateTime, d.createdAt)
    """,
    countQuery = """
        SELECT COUNT(DISTINCT d)
        FROM DraftDomestic d
        JOIN d.shippingList s
        JOIN Warehouse w ON w.trackingCode = s
        WHERE w.status = com.tiximax.txm.Enums.WarehouseStatus.CHO_GIAO
          AND d.isLocked = true
          AND (:routeId IS NULL OR w.orders.route.routeId = :routeId)
          AND d.createdAt >= COALESCE(:startDateTime, d.createdAt)
          AND d.createdAt <= COALESCE(:endDateTime, d.createdAt)
    """
)
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
  @Query("""
    SELECT d.isLocked
    FROM DraftDomestic d
    WHERE d.id = :draftId
""")
Boolean isDraftLocked(@Param("draftId") Long draftId);

 Optional<DraftDomestic> findByVNPostTrackingCode(String vnPostTrackingCode);

  Optional<DraftDomestic> findByShipCode(String shipCode);
  
     @Query(
        value = """
            SELECT DISTINCT d
            FROM DraftDomestic d,
                 Warehouse w
            WHERE w.trackingCode MEMBER OF d.shippingList
              AND w.status = :status
              AND d.isLocked = false
              AND (:routeId IS NULL OR w.orders.route.routeId = :routeId)
        """,
        countQuery = """
            SELECT COUNT(DISTINCT d)
            FROM DraftDomestic d,
                 Warehouse w
            WHERE w.trackingCode MEMBER OF d.shippingList
              AND w.status = :status
              AND d.isLocked = false
              AND (:routeId IS NULL OR w.orders.route.routeId = :routeId)
        """
    )
    Page<DraftDomestic> AvailableToLock(
            @Param("status") WarehouseStatus status,
            @Param("routeId") Long routeId,
            Pageable pageable
    );

      @Query("""
        SELECT DISTINCT d
        FROM DraftDomestic d
        JOIN d.shippingList sl
        WHERE d.isLocked = false
          AND sl IN :shipmentCodes
    """)
    List<DraftDomestic> findDraftByShipmentCodes(
            @Param("shipmentCodes") Collection<String> shipmentCodes
    );

}


