package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Data
@Getter
@Setter

public class CustomerPatchRequest {
    private String email;
    private String phone;
    private String name;
}
