package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Enums.AccountRoles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface AuthenticationRepository extends JpaRepository<Account, Long> {
    Account findByUsername(String username);

    Customer findByCustomerCode(String customerCode);

    Account findByPhone(String phone);

    Account findByEmail(String email);

    Page<Account> findByRoleIn(List<AccountRoles> roles, Pageable pageable);
}
