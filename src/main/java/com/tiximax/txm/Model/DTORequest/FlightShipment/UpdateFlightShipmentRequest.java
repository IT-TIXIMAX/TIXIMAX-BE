package com.tiximax.txm.Model.DTORequest.FlightShipment;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
@Data
@Getter
@Setter
public class UpdateFlightShipmentRequest {
    private String awbFilePath;
    private String exportLicensePath;
    private String singleInvoicePath;
    private String invoiceFilePath;
    private String packingListPath;
    private BigDecimal totalVolumeWeight;
    private BigDecimal airFreightCost ;
    private BigDecimal customsClearanceCost ;
    private BigDecimal airportShippingCost ;
    private BigDecimal otherCosts;
    private LocalDate arrivalDate;
    private Boolean airFreightPaid;
    private LocalDate airFreightPaidDate;
    private Boolean customsPaid;
    private LocalDate customsPaidDate;
}
