package com.tiximax.txm.Repository;
import com.tiximax.txm.Entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository

public interface StaffRepository extends JpaRepository<Staff, Long> {
    boolean existsByStaffCode(String staffCode);
}