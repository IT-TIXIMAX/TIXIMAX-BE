package com.tiximax.txm.Model.DTORequest.DraftDomestic;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
@Data
public class UpdateDraftDomesticInfoRequest {

    @Pattern(
        regexp = "^(0|\\+84)[0-9]{9}$",
        message = "Số điện thoại không hợp lệ"
    )
    private String phoneNumber;

    private String address;
}
