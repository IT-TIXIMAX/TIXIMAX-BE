package com.tiximax.txm.Model.DTORequest.Auth;

import com.tiximax.txm.Enums.AccountStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class StaffPatchRequest {
    private String email;
    private String phone;
    private String name;
    private AccountStatus status;
    private String staffCode;
    private String department;
    private String location;
    private Long warehouseLocationId;
}
