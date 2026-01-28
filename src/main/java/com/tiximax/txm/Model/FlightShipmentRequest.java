package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Getter
@Setter

public class FlightShipmentRequest {
    private String flightCode;
    private String awbFilePath;
    private String exportLicensePath;
    private String singleInvoicePath;
    private String invoiceFilePath;
    private String packingListPath;
    private BigDecimal totalVolumeWeight;
    private BigDecimal airFreightCost = BigDecimal.ZERO;
    private BigDecimal customsClearanceCost = BigDecimal.ZERO;
    private BigDecimal airportShippingCost = BigDecimal.ZERO;
    private BigDecimal otherCosts = BigDecimal.ZERO;
    private LocalDate arrivalDate;
    private Boolean airFreightPaid;
    private LocalDate airFreightPaidDate;
    private Boolean customsPaid;
    private LocalDate customsPaidDate;
}
