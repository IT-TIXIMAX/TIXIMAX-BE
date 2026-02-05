package com.tiximax.txm.Model.DTOResponse.Customer;

import lombok.Data;

import java.time.LocalDateTime;

@Data

public class InactiveCustomerProjection {
    private Long customerId;
    private String customerName;
    private Long staffId;
    private String staffName;
    private LocalDateTime lastOrderDate;
    private Long numberOfOrders;

    public InactiveCustomerProjection(Long customerId,
                                    String customerName,
                                    Long staffId,
                                    String staffName,
                                    LocalDateTime lastOrderDate,
                                    Long numberOfOrders){
        this.customerId = customerId;
        this.customerName = customerName;
        this.staffId = staffId;
        this.staffName = staffName;
        this.lastOrderDate = lastOrderDate;
        this.numberOfOrders = numberOfOrders;
    }
}
