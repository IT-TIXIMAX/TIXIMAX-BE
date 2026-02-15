package com.tiximax.txm.Model.DTOResponse.Order;

import lombok.Data;

@Data

public class CustomerSegment {
    private String segment;
    private long customers;
    private Double retentionPercent;

    public CustomerSegment(String segment, long customers, Double retentionPercent) {
        this.segment = segment;
        this.customers = customers;
        this.retentionPercent = retentionPercent;
    }
}
