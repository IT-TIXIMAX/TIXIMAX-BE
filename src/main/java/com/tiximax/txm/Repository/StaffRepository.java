package com.tiximax.txm.Repository;
import com.tiximax.txm.Entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;



@Repository

public interface StaffRepository extends JpaRepository<Staff, Long> {
    boolean existsByStaffCode(String staffCode);

    @Query("SELECT s.staffCode FROM Staff s ORDER BY s.staffCode DESC LIMIT 1")
    String findLatestStaffCode();
}