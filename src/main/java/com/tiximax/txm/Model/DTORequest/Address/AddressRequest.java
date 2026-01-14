package com.tiximax.txm.Model.DTORequest.Address;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class AddressRequest {
    @NotBlank(message = "Tên địa chỉ không được để trống")
    private String addressName;

}