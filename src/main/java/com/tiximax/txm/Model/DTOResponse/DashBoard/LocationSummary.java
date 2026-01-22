package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;

@Data
public class LocationSummary {

    private Long locationId;
    private String locationName;
    private long warehouses;
    private long orders;
    private long countPackages;
    private long orderLinks;
    private long totalQuantity;
    private Double weight;
    private Double netWeight;

    public LocationSummary(Long locationId, String locationName, long warehouses,
                           Double weight, Double netWeight) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.warehouses = warehouses;
        this.orders = 0;
        this.countPackages = 0;
        this.orderLinks = 0;
        this.totalQuantity = 0;
        this.weight = weight != null ? weight : 0.0;
        this.netWeight = netWeight != null ? netWeight : 0.0;
    }

    public LocationSummary(Long locationId, String locationName, long warehouses,
                           long orders, Double weight, Double netWeight) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.warehouses = warehouses;
        this.orders = orders;
        this.countPackages = 0;
        this.orderLinks = 0;
        this.totalQuantity = 0;
        this.weight = weight != null ? weight : 0.0;
        this.netWeight = netWeight != null ? netWeight : 0.0;
    }

    public LocationSummary(Long locationId, String locationName,
                           long countPackages, long orderLinks, long orders,
                           Double weight, Double netWeight, long totalQuantity) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.countPackages = countPackages;
        this.orderLinks = orderLinks;
        this.orders = orders;
        this.totalQuantity = totalQuantity;
        this.warehouses = 0;
        this.weight = weight != null ? weight : 0.0;
        this.netWeight = netWeight != null ? netWeight : 0.0;
    }

    public LocationSummary() {}
}
