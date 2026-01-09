package com.tiximax.txm.Model.DTORequest.Purchase;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExchangeRequest {
    private String image;
    private BigDecimal total;
    private String note;
}
