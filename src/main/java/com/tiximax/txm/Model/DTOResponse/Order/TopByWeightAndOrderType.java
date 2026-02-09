package com.tiximax.txm.Model.DTOResponse.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TopByWeightAndOrderType {
    private Long customerId;
    private String customerName;
    private Long staffId;
    private String staffName;
    private String orderType;
    private BigDecimal totalWeight;
    private Integer rank;
}
