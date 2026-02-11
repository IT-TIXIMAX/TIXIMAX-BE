package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;

@Data
public class LocationSummary {

    private Long locationId;
    private String locationName;
    private Long warehouses;
    private Long orders;
    private Long countPackages;
    private Long orderLinks;
    private Long totalQuantity;
    private Double weight;
    private Double netWeight;

    public LocationSummary(Long locationId, String locationName, Long warehouses,
                           Double weight, Double netWeight) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.warehouses = warehouses != null ? warehouses : 0L;
        this.orders = 0L;
        this.countPackages = 0L;
        this.orderLinks = 0L;
        this.totalQuantity = 0L;
        this.weight = weight != null ? weight : 0.0;
        this.netWeight = netWeight != null ? netWeight : 0.0;
    }

    public LocationSummary(Long locationId, String locationName, Long warehouses,
                           Long orders, Double weight, Double netWeight) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.warehouses = warehouses;
        this.orders = orders;
        this.countPackages = 0L;
        this.orderLinks = 0L;
        this.totalQuantity = 0L;
        this.weight = weight != null ? weight : 0.0;
        this.netWeight = netWeight != null ? netWeight : 0.0;
    }

    public LocationSummary(Long locationId, String locationName, Long warehouses,
                           Long orders, Long countPackages, Long orderLinks, Double weight, Double netWeight) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.warehouses = warehouses;
        this.orders = orders != null ? orders : 0L;
        this.countPackages = countPackages != null ? countPackages : 0L;
        this.orderLinks = orderLinks != null ? orderLinks : 0L;
        this.totalQuantity = 0L;
        this.weight = weight != null ? weight : 0.0;
        this.netWeight = netWeight != null ? netWeight : 0.0;
    }

    public LocationSummary() {}
}