package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.RepackStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter

public class Repack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repack_id")
    private Long repackId;

    @Column(nullable = false, unique = true)
    private String repackCode;

    @Column(nullable = false)
    private List<String> repackList = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepackStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    @JsonIgnore
    private Staff staff;

    @OneToMany(mappedBy = "repack", cascade = CascadeType.MERGE, orphanRemoval = false)
    @JsonIgnore
    private Set<Warehouse> relatedWarehouses = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY, optional = true, cascade = CascadeType.MERGE)
    @JoinColumn(name = "resulting_packing_id", nullable = true, unique = true)
    @JsonIgnore
    private Packing resultingPacking;
}
