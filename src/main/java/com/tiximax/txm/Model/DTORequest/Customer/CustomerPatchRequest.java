package com.tiximax.txm.Model.DTORequest.Customer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class CustomerPatchRequest {
    private String email;
    private String phone;
    private String name;
}
