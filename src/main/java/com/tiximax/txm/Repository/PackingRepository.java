package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PackingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PackingRepository extends JpaRepository<Packing, Long> {

    boolean existsByPackingCode(String packingCode);

    List<Packing> findByPackingCodeStartingWith(String baseCode);

    Page<Packing> findByFlightCodeIsNull(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Packing p " +
            "JOIN p.warehouses w " +
            "WHERE p.flightCode IS NULL " +
            "AND w.location.locationId = :locationId")
    Page<Packing> findByFlightCodeIsNullAndWarehouses_Location_LocationId(
            @Param("locationId") Long locationId, Pageable pageable);

    @Query("SELECT p FROM Packing p JOIN p.warehouses w WHERE p.status = :status AND w.location.locationId = :warehouseLocationId")
    Page<Packing> findByStatusAndWarehouses_Location_LocationId(
            @Param("status") PackingStatus status,
            @Param("warehouseLocationId") Long warehouseLocationId,
            Pageable pageable);

    List<Packing> findAllByPackingCodeIn(List<String> packingCode);

        @Query("""
        SELECT DISTINCT p
        FROM Packing p
        LEFT JOIN FETCH p.warehouses w
        WHERE p.packingId IN :ids
    """)
    List<Packing> findAllChoBayWithWarehouses(@Param("ids") List<Long> ids);

    Optional<Packing> findByPackingCode(String packingCode);

    Page<Packing> findByStatus(PackingStatus packingStatus, Pageable pageable);

    @Query("SELECT p.packingList FROM Packing p WHERE p.packingCode = :packingCode")
    List<String> findPackingListByCode(@Param("packingCode") String packingCode);

}