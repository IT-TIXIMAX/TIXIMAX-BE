package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.AccountRoute;
import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Repository.AccountRouteRepository;
import com.tiximax.txm.Repository.AuthenticationRepository;
import com.tiximax.txm.Repository.RouteRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.NotFound;

import java.util.List;

@Service
public class AccountRouteService {

    @Autowired
    private AccountRouteRepository accountRouteRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private AccountUtils accountUtils;

    public AccountRoute createAccountRoute(Long accountId, Long routeId) {
        Account account = authenticationRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản với ID: " + accountId));
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tuyến hàng với ID: " + routeId));

        AccountRoute accountRoute = new AccountRoute();
        accountRoute.setAccount(account);
        accountRoute.setRoute(route);
        return accountRouteRepository.save(accountRoute);
    }

    public AccountRoute getAccountRouteById(Long accountRouteId) {
        return accountRouteRepository.findById(accountRouteId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AccountRoute với ID: " + accountRouteId));
    }

    public void deleteAccountRoute(Long accountRouteId) {
        if (!accountRouteRepository.existsById(accountRouteId)) {
            throw new NotFoundException("Không tìm thấy AccountRoute với ID: " + accountRouteId);
        }
        accountRouteRepository.deleteById(accountRouteId);
    }

    public List<Route> getByStaffId() {
        return accountRouteRepository.findRoutesByStaffId(accountUtils.getAccountCurrent().getAccountId());
    }

}
