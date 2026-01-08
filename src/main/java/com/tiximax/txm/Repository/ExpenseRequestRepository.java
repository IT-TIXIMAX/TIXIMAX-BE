package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.ExpenseRequest;
import com.tiximax.txm.Enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRequestRepository  extends JpaRepository<ExpenseRequest, Long> {
    @Query("SELECT e FROM ExpenseRequest e " +
            "WHERE (:status IS NULL OR e.status = :status)")
    Page<ExpenseRequest> findAllForApprover(
            @Param("status") ExpenseStatus status,
            Pageable pageable);

    @Query("SELECT e FROM ExpenseRequest e " +
            "WHERE e.requester.accountId = :staffId " +
            "AND (:status IS NULL OR e.status = :status)")
    Page<ExpenseRequest> findOwnForRequester(
            @Param("staffId") Long staffId,
            @Param("status") ExpenseStatus status,
            Pageable pageable);
}
