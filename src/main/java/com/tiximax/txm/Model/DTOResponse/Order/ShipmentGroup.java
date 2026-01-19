package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Model.DTOResponse.OrderLink.ShipmentProductInfo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter

public class ShipmentGroup {
    private String orderCode;
    private String shipmentCode;
    private OrderLinkStatus status;
    private List<ShipmentProductInfo> products = new ArrayList<>();

    public ShipmentGroup(String orderCode, String shipmentCode, OrderLinkStatus status) {
        this.orderCode = orderCode;
        this.shipmentCode = shipmentCode;
        this.status = status;
    }

    public void addProduct(String productName, String productLink, String productImage, String productImageCheck) {
        this.products.add(new ShipmentProductInfo(productName, productLink, productImage, productImageCheck));
    }
}
