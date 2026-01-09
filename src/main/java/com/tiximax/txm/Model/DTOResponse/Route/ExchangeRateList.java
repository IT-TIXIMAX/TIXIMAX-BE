package com.tiximax.txm.Model.DTOResponse.Route;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@XmlRootElement(name = "ExrateList")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExchangeRateList {

    @XmlElement(name = "DateTime")
    private String dateTime;

    @XmlElement(name = "Exrate")
    private List<Exrate> exrates;

//    @XmlElement(name = "Source")
//    private String source;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Exrate {

        @XmlAttribute(name = "CurrencyCode")
        private String currencyCode;

//        @XmlAttribute(name = "CurrencyName")
//        private String currencyName;

//        @XmlAttribute(name = "Buy")
//        private String buy;
//
//        @XmlAttribute(name = "Transfer")
//        private String transfer;

        @XmlAttribute(name = "Sell")
        private String sell;

        // Default constructor required for JAXB
        public Exrate() {}
    }

    // Default constructor required for JAXB
    public ExchangeRateList() {}
}