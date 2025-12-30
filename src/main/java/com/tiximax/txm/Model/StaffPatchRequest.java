package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class StaffPatchRequest {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String name;
    private String staffCode;
    private String department;
    private String location;
    private Long warehouseLocationId;
}
