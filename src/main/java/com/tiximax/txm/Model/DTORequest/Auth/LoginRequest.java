package com.tiximax.txm.Model.DTORequest.Auth;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class LoginRequest {
    String username;
    String password;
}
