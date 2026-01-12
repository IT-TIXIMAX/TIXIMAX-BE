package com.tiximax.txm.Model.DTORequest.DraftDomestic;


import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DraftDomesticRequest {
    @NotBlank(message = "customerCode không được để trống")
    private String customerCode;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
        regexp = "^(0|\\+84)[0-9]{9}$",
        message = "Số điện thoại không hợp lệ"
    )
    private String phoneNumber;
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private Boolean isVNpost; 

    private List<String> shippingList;

}
