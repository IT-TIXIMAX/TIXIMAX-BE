package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeMoneySummary {
    private Long totalOrderlinks;
    private Long waitingExchange;
    private Long exchanged;
    public ExchangeMoneySummary(
            Long totalOrderlinks,
            Long waitingExchange,
            Long exchanged
    ) {
        this.totalOrderlinks = totalOrderlinks;
        this.waitingExchange = waitingExchange;
        this.exchanged = exchanged;
    }
}
