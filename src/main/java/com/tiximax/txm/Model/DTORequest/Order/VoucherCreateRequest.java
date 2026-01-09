package com.tiximax.txm.Model.DTORequest.Order;

import com.tiximax.txm.Enums.AssignType;
import com.tiximax.txm.Enums.VoucherType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class VoucherCreateRequest {

    private String code;

    private VoucherType type;

    private BigDecimal value;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private BigDecimal minOrderValue;

//    private Integer maxUses;

    private AssignType assignType;

    private Double thresholdAmount;

    private Set<Long> routeIds;
}
