package com.tiximax.txm.Model.DTORequest.Route;

import com.tiximax.txm.Enums.PaymentMethod;
import com.tiximax.txm.Enums.VatStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter

public class CreateExpenseRequest {
    private String description;
    private int quantity;
    private BigDecimal unitPrice;
    private String note;
    private PaymentMethod paymentMethod;
    private String bankInfo;
    private VatStatus vatStatus;
    private String vatInfo;
}
