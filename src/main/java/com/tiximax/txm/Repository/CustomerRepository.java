package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Model.DTOResponse.DashBoard.CustomerTop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCustomerCode(String customerCode);

    @Query("SELECT c FROM Customer c WHERE (:keyword IS NULL OR (c.phone LIKE %:keyword% OR c.name LIKE %:keyword%)) AND c.staffId = :staffId")
    List<Customer> findByPhoneOrNameContainingAndStaffId(@Param("keyword") String keyword, @Param("staffId") Long staffId);

    Page<Customer> findByStaffId(Long staffId, Pageable pageable);

    @Query("""
       SELECT c FROM Customer c
       WHERE (:staffId is null or c.staffId = :staffId)
       AND (
            :keyword IS NULL
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
       )
    """)
    Page<Customer> searchByStaff(
            @Param("staffId") Long staffId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.staffId = :staffId AND c.createdAt BETWEEN :startDate AND :endDate")
    long countByStaffIdAndCreatedAtBetween(@Param("staffId") Long staffId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.staffId = :staffId")
    long countByStaffId(@Param("staffId") Long staffId);

    Optional<Customer> findByCustomerCode(String customerCode);
    
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM Customer c WHERE c.accountId = :accountId")
    Customer getCustomerById(@Param("accountId") Long accountId);

    @Query("SELECT c FROM Customer c WHERE c.accountId = :customerId")
    Optional<Customer> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT c.customerCode FROM Customer c ORDER BY c.customerCode DESC LIMIT 1")
    String findLatestCustomerCode();

    List<Customer> findByCreatedAtBetween(Pageable pageable, LocalDateTime start, LocalDateTime end);

    @Query("SELECT MONTH(c.createdAt), COUNT(c) FROM Customer c WHERE YEAR(c.createdAt) = :year GROUP BY MONTH(c.createdAt)")
    List<Object[]> countNewCustomersByMonth(@Param("year") int year);

    @Query(value = """
    SELECT
        s.account_id,
        COALESCE(s.name, 'Khách tự đăng ký') AS staff_name,
        COUNT(*) AS new_customer_count
    FROM customer c
    LEFT JOIN account s ON s.account_id = c.staff_id
    WHERE EXISTS (
        SELECT 1 
        FROM account a 
        WHERE a.account_id = c.account_id 
          AND a.created_at BETWEEN :start AND :end
    )
    GROUP BY s.account_id, s.name
    HAVING COUNT(*) > 0
    ORDER BY new_customer_count DESC, staff_name
    """, nativeQuery = true)
    List<Object[]> sumNewCustomersByStaffNativeRaw(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.CustomerTop(
            c.accountId,
            c.customerCode,
            COALESCE(c.name, c.username, c.customerCode),
            c.phone,
            c.email,
            c.totalOrders,
            c.totalWeight,
            c.totalAmount
        )
        FROM Customer c
        WHERE c.totalOrders > 0
        ORDER BY c.totalOrders DESC
    """)
    Page<CustomerTop> findTopByTotalOrders(Pageable pageable);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.CustomerTop(
            c.accountId,
            c.customerCode,
            COALESCE(c.name, c.username, c.customerCode),
            c.phone,
            c.email,
            c.totalOrders,
            c.totalWeight,
            c.totalAmount,
            c.balance
        )
        FROM Customer c
        WHERE c.totalWeight > 0
        ORDER BY c.totalWeight DESC
    """)
    Page<CustomerTop> findTopByTotalWeight(Pageable pageable);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.CustomerTop(
            c.accountId,
            c.customerCode,
            COALESCE(c.name, c.username, c.customerCode),
            c.phone,
            c.email,
            c.totalOrders,
            c.totalWeight,
            c.totalAmount,
            c.balance
        )
        FROM Customer c
        WHERE c.totalAmount > 0
        ORDER BY c.totalAmount DESC
    """)
    Page<CustomerTop> findTopByTotalAmount(Pageable pageable);

    @Query("""
        SELECT new com.tiximax.txm.Model.DTOResponse.DashBoard.CustomerTop(
            c.accountId,
            c.customerCode,
            COALESCE(c.name, c.username, c.customerCode),
            c.phone,
            c.email,
            c.totalOrders,
            c.totalWeight,
            c.totalAmount,
            c.balance
        )
        FROM Customer c
        WHERE c.balance > 0
        ORDER BY c.balance DESC
    """)
    Page<CustomerTop> findTopByBalance(Pageable pageable);
}