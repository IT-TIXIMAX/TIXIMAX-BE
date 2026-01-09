package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter

public class OrderByShipmentResponse {
    private Orders order;
    private List<OrderLinks> orderLinks;

    public OrderByShipmentResponse(Orders order, List<OrderLinks> links) {
        this.order = order;
        this.orderLinks = links;
    }
}
