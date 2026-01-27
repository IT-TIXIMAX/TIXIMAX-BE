package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Model.DTOResponse.Staff.StaffInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository

public interface StaffRepository extends JpaRepository<Staff, Long> {
    boolean existsByStaffCode(String staffCode);

    @Query("SELECT s.staffCode FROM Staff s ORDER BY s.staffCode DESC LIMIT 1")
    String findLatestStaffCode();

    @Query("""
    SELECT new com.tiximax.txm.Model.DTOResponse.Staff.StaffInfo(
        a.accountId,
        a.name,
        a.username,
        a.staffCode,
        a.department,
        a.role,
        a.email,
        a.phone,
        a.status,
        a.createdAt
    )
    FROM Account a
    WHERE a.role != 'CUSTOMER'
      AND (:role IS NULL OR a.role = :role)
      AND (
          :keyword IS NULL 
          OR :keyword = '' 
          OR LOWER(a.name)     LIKE CONCAT('%', LOWER(:keyword), '%')
          OR LOWER(a.username) LIKE CONCAT('%', LOWER(:keyword), '%')
          OR LOWER(a.phone)    LIKE CONCAT('%', LOWER(:keyword), '%')
          OR LOWER(a.email)    LIKE CONCAT('%', LOWER(:keyword), '%')
      )
    ORDER BY a.name ASC
    """)
    Page<StaffInfo> findStaffByRole(String keyword, AccountRoles role, Pageable pageable);
}