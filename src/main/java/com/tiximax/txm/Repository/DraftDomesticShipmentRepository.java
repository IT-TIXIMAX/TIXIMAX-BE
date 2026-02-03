package com.tiximax.txm.Repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.tiximax.txm.Entity.DraftDomesticShipment;

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
        WHERE d.staff.id = :staffId
          AND (:payment IS NULL OR d.payment = :payment)
          AND (
              :keyword IS NULL
              OR d.shipCode LIKE %:keyword%
              OR s.shipmentCode LIKE %:keyword%
          )
    """)
    List<DraftDomesticShipment> findShipmentsByStaff(
            Long staffId,
            String keyword,
            Boolean payment
    );
        @Query("""
        SELECT s
        FROM DraftDomesticShipment s
        JOIN FETCH s.draftDomestic d
        JOIN FETCH d.customer
        WHERE d.shipCode = :shipCode
    """)
    List<DraftDomesticShipment> findByShipCode(String shipCode);

    
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
