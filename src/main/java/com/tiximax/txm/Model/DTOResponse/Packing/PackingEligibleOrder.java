package com.tiximax.txm.Model.DTOResponse.Packing;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Data
@Getter
@Setter

public class PackingEligibleOrder {

    private String orderCode;

    private Map<String, Integer> trackingCodeToProductCount;

}
