package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.DomesticStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter

public class Domestic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "domestic_id")
    private Long domesticId;

    @ManyToOne
    @JoinColumn(name = "from_location_id", nullable = false)
    @JsonIgnore
    private WarehouseLocation fromLocation;

    @ManyToOne
    @JoinColumn(name = "to_location_id", nullable = true)
    @JsonIgnore
    private WarehouseLocation toLocation;

    @ManyToOne
    @JoinColumn(name = "to_address_id")
    @JsonIgnore
    private Address toAddress;

    @Enumerated(EnumType.STRING)
    private DomesticStatus status;

    private String note;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private List<String> shippingList = new ArrayList<>();

    @Column(nullable = true)
    private String VNPostTrackingCode;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;

    @ManyToOne
    @JoinColumn(name="location_id", nullable = false)
    @JsonIgnore
    WarehouseLocation location;

//    @ManyToOne
//    @JoinColumn(name="order_id", nullable = false)
//    @JsonIgnore
//    Orders orders;

    @ManyToMany(mappedBy = "domestics")
    @JsonIgnore
    private Set<Packing> packings;

}
