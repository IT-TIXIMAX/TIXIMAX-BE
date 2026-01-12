package com.tiximax.txm.Model.DTOResponse.Domestic;
import java.util.List;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinksShipForeign;

import lombok.Data;
@Data
public class ShipLinkForegin {
    private String customerCode;
    private Long orderId;
    private String orderCode;
    private List<OrderLinksShipForeign> orderLinks;
    public ShipLinkForegin(Orders order, List<OrderLinksShipForeign> orderLinksShips) {
        this.customerCode = order.getCustomer().getCustomerCode();
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderLinks = orderLinksShips;
    }
}
