package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Destination;
import com.tiximax.txm.Entity.Orders;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class InfoShipmentCode {

    Orders orders;

    String destinationName;

    BigDecimal price;

    String customerCode;
}
