package com.tiximax.txm.Model.DTOResponse.Customer;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Enums.AccountStatus;
import com.tiximax.txm.Model.DTOResponse.Address.AddressDTO;
import com.tiximax.txm.Entity.Address;

@Data
public class CustomerResponseDTO {
    private Long accountId;
    private String customerCode;
    private String name;
    private String phone;
    private String email;
    private String source;
    private Integer totalOrders;
    private Double totalWeight;
    private BigDecimal totalAmount;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private String staffName;

    public CustomerResponseDTO(
            Long accountId,
            String customerCode,
            String name,
            String phone,
            String email,
            String source,
            Integer totalOrders,
            Double totalWeight,
            BigDecimal totalAmount,
            BigDecimal balance,
            LocalDateTime createdAt,
            String staffName
    ) {
        this.accountId = accountId;
        this.customerCode = customerCode;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.source = source;
        this.totalOrders = totalOrders;
        this.totalWeight = totalWeight;
        this.totalAmount = totalAmount;
        this.balance = balance;
        this.createdAt = createdAt;
        this.staffName = staffName;
    }

}