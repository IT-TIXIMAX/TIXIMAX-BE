package com.tiximax.txm.Model.DTORequest.Auth;

import com.tiximax.txm.Enums.AccountRoles;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter

public class RegisterStaffRequest {

    private String username;

    private String password;

    private String email;

    private String phone;

    private String name;

    private AccountRoles role;

    private String department;

    private String location;

    private List<Long> routeIds;

}
