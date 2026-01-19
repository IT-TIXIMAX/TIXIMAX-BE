package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class DomesticDetail {
    private String customerName;
    private String customerCode;
    private Long totalPackagesShipCount;
    private Double totalPackagesShipWeight;
    private Long totalPackagesReturnCount;
    private Double totalPackagesReturnWeight;

}
