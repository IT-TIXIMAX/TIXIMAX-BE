package com.tiximax.txm.Model.DTORequest.Order;


import com.tiximax.txm.Enums.OrderType;
import com.tiximax.txm.Model.DTORequest.OrderLink.OrderLinkRequest;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
public class OrdersRequest {

    private OrderType orderType;

    private Long destinationId;

    private BigDecimal exchangeRate;

    private BigDecimal priceShip;

    private Boolean checkRequired;

    private Long address; 

    private List<OrderLinkRequest> orderLinkRequests;

}
