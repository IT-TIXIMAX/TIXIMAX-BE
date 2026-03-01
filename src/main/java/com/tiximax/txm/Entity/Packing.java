package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.DomesticStatus;
import com.tiximax.txm.Enums.PackingDestination;
import com.tiximax.txm.Enums.PackingStatus;
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

public class Packing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "packing_id")
    private Long packingId;

    @Column(nullable = true)
    private String flightCode;

    @Column(nullable = false)
    private String packingCode;

    @Column(nullable = false)
    private List<String> packingList = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime packedDate;

    @Column(nullable = true)
    private LocalDateTime flyTime;
    
    @Enumerated(EnumType.STRING)
    private PackingStatus status;

    private Boolean statusFlight;

    private String routeCode;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;

    @OneToMany(mappedBy = "packing", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Warehouse> warehouses;

    @ManyToOne
    @JoinColumn(name="domestic_id", nullable = true)
    @JsonIgnore
    private Domestic domestic;

    @ManyToMany(fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Orders> relatedOrders;

    @ManyToOne
    @JoinColumn(name="destination_id", nullable = false)
    @JsonIgnore
    Destination destination;

}