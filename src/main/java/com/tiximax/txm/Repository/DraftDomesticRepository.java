package com.tiximax.txm.Repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tiximax.txm.Entity.DraftDomestic;

public interface DraftDomesticRepository extends JpaRepository<DraftDomestic, Long> {
     @Query(
        value = """
            SELECT DISTINCT d
            FROM DraftDomestic d
            JOIN d.customer c
            LEFT JOIN d.shippingList sl
            WHERE
              (:customerCode IS NULL OR c.customerCode = :customerCode)
              AND (:shipmentCode IS NULL OR sl = :shipmentCode)
        """,
        countQuery = """
            SELECT COUNT(DISTINCT d.id)
            FROM DraftDomestic d
            JOIN d.customer c
            LEFT JOIN d.shippingList sl
            WHERE
              (:customerCode IS NULL OR c.customerCode = :customerCode)
              AND (:shipmentCode IS NULL OR sl = :shipmentCode)
        """
    )
    Page<DraftDomestic> findAllWithFilter(
            @Param("customerCode") String customerCode,
            @Param("shipmentCode") String shipmentCode,
            Pageable pageable
    );
  
}


