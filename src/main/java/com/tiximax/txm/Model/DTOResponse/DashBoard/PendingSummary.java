package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class PendingSummary {
    private long packages;
    private long orderLinks;
    private long orders;

    public PendingSummary(long packages, long orderLinks, long orders) {
        this.packages = packages;
        this.orderLinks = orderLinks;
        this.orders = orders;
    }
}
