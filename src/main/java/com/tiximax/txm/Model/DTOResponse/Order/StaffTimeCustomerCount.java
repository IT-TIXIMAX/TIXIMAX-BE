package com.tiximax.txm.Model.DTOResponse.Order;

import lombok.Data;

@Data

public class StaffTimeCustomerCount {
    private String staffName;
    private Long   customerCount;

    public StaffTimeCustomerCount(String staffName, Long customerCount) {
        this.staffName = staffName ;
        this.customerCount = customerCount;
    }
}
