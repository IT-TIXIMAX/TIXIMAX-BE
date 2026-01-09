package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Orders;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class OrderWithCustomer {

    private Orders order;

    private Customer customer;

}
