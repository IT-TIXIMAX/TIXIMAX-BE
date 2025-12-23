package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter

public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    private String name;

    private String shipTime;

    private BigDecimal unitDepositPrice;

    private BigDecimal unitBuyingPrice;

    private BigDecimal exchangeRate;

    private BigDecimal differenceRate;

    @Column(nullable = false)
    private boolean isUpdateAuto;

    private String note;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Orders> orders;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<AccountRoute> accountRoutes;

    @ManyToMany(mappedBy = "applicableRoutes")
    @JsonIgnore
    private Set<Voucher> vouchers = new HashSet<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<RouteExchangeRate> routeExchangeRates = new HashSet<>();

}
