package com.tiximax.txm.Model.DTOResponse.Customer;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter

public class CustomerRegistered {
    private String customerCode;
    private BigDecimal balance;
    private String source;
    private String email;
    private String name;
    private String phone;
    private LocalDateTime createdAt;

    public CustomerRegistered(String customerCode, BigDecimal balance, String source, String email, String name, String phone, LocalDateTime createdAt) {
        this.customerCode = customerCode;
        this.balance = balance;
        this.source = source;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.createdAt = createdAt;
    }
}
