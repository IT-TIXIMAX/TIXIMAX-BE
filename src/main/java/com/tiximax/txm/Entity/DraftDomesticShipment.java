package com.tiximax.txm.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;



@Entity
@Table(name = "draft_domestic_shipping_list")
@Getter
@Setter
public class DraftDomesticShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_domestic_id", nullable = false)
    private DraftDomestic draftDomestic;

    @Column(name = "shipping_list", nullable = false)
    private String shipmentCode;
    
}


