package com.tiximax.txm.Model.DTORequest.Domestic;

import com.tiximax.txm.Enums.Carrier;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class ScanToShip {
    @NotBlank(message = "Tracking code không được để trống")
    private String trackingCode;
    @NotBlank(message = "Ship code không được để trống")
    private String shipCode;
    @NotBlank(message = "Carrier không được để trống") 
    private Carrier carrier;
}
