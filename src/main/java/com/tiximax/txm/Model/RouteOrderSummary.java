package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class RouteOrderSummary {
    private String routeName;
    private Long totalOrders;
    private Long totalLinks;

    public RouteOrderSummary(String routeName, Long totalOrders, Long totalLinks) {
        this.routeName = routeName;
        this.totalOrders = totalOrders;
        this.totalLinks = totalLinks;
    }
}
