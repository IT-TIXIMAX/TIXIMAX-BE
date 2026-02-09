package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.AccountRoute;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository

public interface AccountRouteRepository extends JpaRepository<AccountRoute, Long> {

    @Query("SELECT ar.route FROM AccountRoute ar WHERE ar.account.accountId = :accountId")
    List<Route> findRoutesByStaffId(Long accountId);

    @Query("SELECT o FROM Orders o WHERE o.status = :status AND o.route.routeId IN :routeIds")
    List<Orders> findByStatusAndRouteIdsIn(OrderStatus status, List<Long> routeIds);

    List<AccountRoute> findByAccountAccountId(Long accountId);

    List<AccountRoute> findByAccount_AccountId(Long accountId);

            @Query("""
            SELECT ar.route.routeId
            FROM AccountRoute ar
            WHERE ar.account.accountId = :accountId
        """)
        List<Long> findRouteIdsByAccountId(Long accountId);


    List<AccountRoute> findByRoute_RouteIdIn(Set<Long> routeIds);

    

}

