package com.tiximax.txm.Model.DTOResponse.Domestic;

import java.util.List;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinksShip;

import lombok.Data;

@Data


public class ShipLinks {
    private String customerCode;
    private String customerName;
    private String customerPhone;
    private String staffName;
    private Long orderId;
    private String orderCode;
    private List<OrderLinksShip> orderLinks;
    private double NetWeight ;


    public ShipLinks(Orders order, List<OrderLinksShip> orderLinksShips, List<Warehouse> warehouses) {
        this.customerCode = order.getCustomer().getCustomerCode();
        this.customerName = order.getCustomer().getName();
        this.customerPhone = order.getCustomer().getPhone();
        this.staffName = order.getStaff().getName();
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderLinks = orderLinksShips;
        this.NetWeight = sumNetWeight(warehouses);
    }


    public double sumNetWeight(List<Warehouse> warehouses) {
          return warehouses.stream()
            .map(Warehouse::getNetWeight)
            .mapToDouble(Double::doubleValue)
            .sum();
    }
}  
