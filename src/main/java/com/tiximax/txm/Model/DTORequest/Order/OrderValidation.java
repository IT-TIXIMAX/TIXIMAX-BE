package com.tiximax.txm.Model.DTORequest.Order;


import com.tiximax.txm.Entity.Address;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Destination;
import com.tiximax.txm.Entity.Route;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class OrderValidation {
   private Customer customer;
    private Route route;
    private Address address;
    private Destination destination;
}
