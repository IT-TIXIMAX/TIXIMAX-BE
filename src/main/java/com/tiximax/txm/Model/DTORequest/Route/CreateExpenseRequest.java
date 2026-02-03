package com.tiximax.txm.Model.DTORequest.Route;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.ExpenseStatus;
import com.tiximax.txm.Enums.PaymentMethod;
import com.tiximax.txm.Enums.VatStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String invoiceImage;
    private String transferImage;
    private String department;
}
