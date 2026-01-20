package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CustomerInventoryQuantity {
    private String customerCode;
    private String customerName;
    private String staffCode;
    private String staffName;
    private InventoryQuantity inventoryQuantity;
}
