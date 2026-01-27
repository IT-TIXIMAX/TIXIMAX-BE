package com.tiximax.txm.Model.DTOResponse.Staff;

import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@AllArgsConstructor

public class StaffInfo {
    private Long accountId;
    private String name;
    private String username;
    private String staffCode;
    private String department;
    private AccountRoles role;
    private String email;
    private String phone;
    private AccountStatus status;
    private LocalDateTime createdAt;
}
