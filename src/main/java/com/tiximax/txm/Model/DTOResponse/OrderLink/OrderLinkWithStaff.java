package com.tiximax.txm.Model.DTOResponse.OrderLink;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Staff;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class OrderLinkWithStaff {

    private OrderLinks orderLink;

    private Staff staff;

    private Customer customer;

}

