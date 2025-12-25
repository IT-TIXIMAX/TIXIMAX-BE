package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class StaffNewCustomerSummary {
    private String staffName;
    private Long newCustomerCount;

    public StaffNewCustomerSummary(String staffName, Long newCustomerCount) {
        this.staffName = staffName != null ? staffName : "Không xác định";
        this.newCustomerCount = newCustomerCount != null ? newCustomerCount : 0L;
    }
}
