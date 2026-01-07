package com.tiximax.txm.Entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DraftDomestic{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_domestic_id")
    private Long id;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @ElementCollection
    private List<String> shippingList ;

    @Column(nullable = true)
    private String weight;

    @Column(nullable = true)
    private String VNPostTrackingCode;

    @ManyToOne
    @JoinColumn(name="customer_id", nullable = false)
    @JsonManagedReference
    Customer customer;
}
