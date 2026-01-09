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

public class RefundResponse {
    private Orders order;
    private List<OrderLinks> cancelledLinks;
}
