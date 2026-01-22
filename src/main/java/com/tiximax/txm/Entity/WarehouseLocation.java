package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.LocationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter

public class WarehouseLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long locationId;

    private String name;

    private LocationType type;

    private String country;

    private String province;

    private String address;

    private boolean isActive;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Warehouse> warehouses;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Domestic> domestics;

    @OneToMany(mappedBy = "warehouseLocation", cascade = CascadeType.ALL)
    Set<Staff> staff;

    @ManyToMany(mappedBy = "warehouseLocations")
    @JsonIgnore
    private Set<Route> routes = new HashSet<>();

}
