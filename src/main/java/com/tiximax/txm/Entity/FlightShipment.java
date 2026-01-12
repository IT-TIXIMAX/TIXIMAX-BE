package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.FlightStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter

public class FlightShipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_shipment_id")
    private Long flightShipmentId;

    @Column(nullable = false, unique = true)
    private String flightCode;  // Mã chuyến bay, e.g., INDO47...

    private String awbFilePath;  // Đường dẫn file AWB upload

    private String invoiceFilePath;  // Đường dẫn file Invoice upload

    private BigDecimal totalVolumeWeight;  // (A) Tổng khối lượng tính cước (chargeable weight)

    private BigDecimal airFreightCost;  // (1) Tổng cước bay (VND)

    private BigDecimal customsClearanceCost;  // (2) Tổng cước thông quan (VND)

    private BigDecimal airportShippingCost;  // (3) Phí ship ra sân bay (VND)

    private BigDecimal otherCosts;  // (4) Chi phí khác (VND)

    private BigDecimal totalCost;  // = (1)+(2)+(3)+(4) - "Tổng cước bay của chuyến"

    private BigDecimal originCostPerKg;  // Chi phí gốc = totalCost / totalVolumeWeight

    private BigDecimal grossProfit;  // Lợi nhuận chuyến = Tổng thu khách - totalCost (tính từ các Orders liên kết)

    private LocalDateTime arrivalDate;  // Ngày chuyến đến (để phân tuyến)

    @Enumerated(EnumType.STRING)
    private FlightStatus status;  // Enum: ARRIVED, CLEARED, COMPLETED,...

    // Trạng thái thanh toán công nợ
    private boolean airFreightPaid;  // Checkbox: Thanh toán cước bay

    private LocalDateTime airFreightPaidDate;

    private boolean customsPaid;  // Checkbox: Thanh toán cước thông quan

    private LocalDateTime customsPaidDate;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @OneToMany(mappedBy = "flightShipment", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Warehouse> warehouses;  // Các kiện hàng thuộc chuyến này

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
