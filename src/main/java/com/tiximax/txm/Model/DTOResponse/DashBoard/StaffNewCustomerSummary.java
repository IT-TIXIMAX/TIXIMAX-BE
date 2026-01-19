package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class StaffNewCustomerSummary {
    private Long staffId;
    private String staffName;
    private Long newCustomerCount;

    public StaffNewCustomerSummary(Long staffId, String staffName, Long newCustomerCount) {
        this.staffId = staffId;
        this.staffName = staffName != null ? staffName : "Không xác định";
        this.newCustomerCount = newCustomerCount != null ? newCustomerCount : 0L;
    }
}
