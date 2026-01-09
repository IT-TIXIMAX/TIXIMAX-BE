package com.tiximax.txm.Model.DTORequest.Auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyAccountRequest {
    private String email;  
    private String otpCode;
}
