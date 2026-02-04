package com.tiximax.txm.Repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tiximax.txm.Entity.DraftDomesticShipment;
import com.tiximax.txm.Model.Projections.ShipCodeBasicProjection;

public interface DraftDomesticShipmentRepository  extends JpaRepository<DraftDomesticShipment, Long> {
      boolean existsByShipmentCode(String shipmentCode);

    List<DraftDomesticShipment> findByDraftDomesticId(Long draftId);

    @Query("""
        SELECT s.shipmentCode
        FROM DraftDomesticShipment s
        WHERE s.draftDomestic.id = :draftId
    """)
    List<String> findCodesByDraftId(Long draftId);

      @Query("""
        SELECT s
        FROM DraftDomesticShipment s
        JOIN FETCH s.draftDomestic d
        JOIN FETCH d.customer
        WHERE d.staff.accountId = :staffId
        AND (:keyword IS NULL OR s.shipmentCode LIKE %:keyword% OR d.shipCode LIKE %:keyword%)
        AND (:payment IS NULL OR d.payment = :payment)
    """)
    Page<DraftDomesticShipment> findShipmentsByStaff(
            Long staffId,
            String keyword,
            Boolean payment,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT
        d.shipCode   AS shipCode,
        c.accountId AS customerId,
        c.name      AS customerName,
        d.createdAt AS createdAt
    FROM DraftDomesticShipment s
        JOIN s.draftDomestic d
        JOIN d.customer c
    WHERE d.staff.accountId = :staffId
      AND (:payment IS NULL OR d.payment = :payment)
      AND (
            :keywordLike IS NULL
            OR d.shipCode LIKE :keywordLike
            OR s.shipmentCode LIKE :keywordLike
      )
    ORDER BY d.createdAt DESC
""")
Page<ShipCodeBasicProjection> findShipCodesByStaff(
        @Param("staffId") Long staffId,
        @Param("keywordLike") String keywordLike,
        @Param("payment") Boolean payment,
        Pageable pageable
);

@Query("""
    SELECT s
    FROM DraftDomesticShipment s
    JOIN FETCH s.draftDomestic d
    WHERE d.shipCode IN :shipCodes
""")
List<DraftDomesticShipment> findByShipCodes(List<String> shipCodes);
        @Query("""
        SELECT s
        FROM DraftDomesticShipment s
        JOIN FETCH s.draftDomestic d
        JOIN FETCH d.customer
        WHERE d.shipCode = :shipCode
    """)
    List<DraftDomesticShipment> findByShipCode(String shipCode);

   @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM DraftDomesticShipment s
        WHERE s.draftDomestic.id = :draftId
    """)
    int deleteByDraftDomesticId(Long draftId);

    @Query("""
        SELECT s
        FROM DraftDomesticShipment s
        WHERE s.draftDomestic.id = :draftId
    """)
    List<DraftDomesticShipment> findByDraftId(Long draftId);

    @Modifying
    @Query("""
        DELETE FROM DraftDomesticShipment s
        WHERE s.draftDomestic.id = :draftId
          AND s.shipmentCode IN :codes
    """)
    int deleteByDraftIdAndCodes(
            Long draftId,
            Set<String> codes
    );
    long countByDraftDomesticId(Long draftId);
     @Query("""
        SELECT s.draftDomestic.id, s.shipmentCode
        FROM DraftDomesticShipment s
        WHERE s.draftDomestic.id IN :draftIds
    """)
    List<Object[]> findShipmentCodesByDraftIds(List<Long> draftIds);

    @Query("""
        SELECT COUNT(DISTINCT s.draftDomestic.id)
        FROM DraftDomesticShipment s
        WHERE s.draftDomestic.id IN :draftIds
    """)
    long countDraftWithShipment(List<Long> draftIds);

      @Query("""
        SELECT s.shipmentCode
        FROM DraftDomesticShipment s
        JOIN s.draftDomestic d
        WHERE d.shipCode = :shipCode
    """)
    List<String> findShipmentCodesByShipCode(String shipCode);
}
