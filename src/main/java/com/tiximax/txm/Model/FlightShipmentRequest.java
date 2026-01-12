package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter

public class FlightShipmentRequest {
    private String flightCode;
    private String awbFilePath;
    private String invoiceFilePath;
    private BigDecimal totalVolumeWeight;
    private BigDecimal airFreightCost = BigDecimal.ZERO;
    private BigDecimal customsClearanceCost = BigDecimal.ZERO;
    private BigDecimal airportShippingCost = BigDecimal.ZERO;
    private BigDecimal otherCosts = BigDecimal.ZERO;
    private LocalDateTime arrivalDate;
    private boolean airFreightPaid = false;
    private LocalDateTime airFreightPaidDate;
    private boolean customsPaid = false;
    private LocalDateTime customsPaidDate;
    private Long staffId;
}
