package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.FlightShipment;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Model.FlightShipmentRequest;
import com.tiximax.txm.Model.FlightShipmentResponse;
import com.tiximax.txm.Repository.FlightShipmentRepository;
import com.tiximax.txm.Repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service

public class FlightShipmentService {
    @Autowired
    private FlightShipmentRepository flightShipmentRepository;

    @Autowired
    private StaffRepository staffRepository;

    public FlightShipmentResponse createFlightShipment(FlightShipmentRequest request) {
        if (flightShipmentRepository.existsByFlightCode(request.getFlightCode())) {
            throw new IllegalArgumentException("Mã chuyến bay đã tồn tại: " + request.getFlightCode());
        }

        FlightShipment entity = new FlightShipment();
        mapRequestToEntity(request, entity);
        calculateCostsAndProfit(entity);
        entity.setCreatedAt(LocalDateTime.now());
        entity = flightShipmentRepository.save(entity);
        return mapToResponse(entity);
    }

    public FlightShipmentResponse getFlightShipmentId(Long id) {
        FlightShipment entity = flightShipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến bay"));
        calculateProfitIfNeeded(entity);
        return mapToResponse(entity);
    }

    public List<FlightShipmentResponse> getAllFlightShipment() {
        return flightShipmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public FlightShipmentResponse updateFlightShipment(Long id, FlightShipmentRequest request) {
        FlightShipment entity = flightShipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến bay"));

        // Cập nhật các field
        entity.setFlightCode(request.getFlightCode());
        entity.setAwbFilePath(request.getAwbFilePath());
        entity.setInvoiceFilePath(request.getInvoiceFilePath());
        entity.setTotalVolumeWeight(request.getTotalVolumeWeight());
        entity.setAirFreightCost(request.getAirFreightCost());
        entity.setCustomsClearanceCost(request.getCustomsClearanceCost());
        entity.setAirportShippingCost(request.getAirportShippingCost());
        entity.setOtherCosts(request.getOtherCosts());
        entity.setArrivalDate(request.getArrivalDate());
        entity.setAirFreightPaid(request.isAirFreightPaid());
        entity.setAirFreightPaidDate(request.getAirFreightPaidDate());
        entity.setCustomsPaid(request.isCustomsPaid());
        entity.setCustomsPaidDate(request.getCustomsPaidDate());

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        entity.setStaff(staff);

        calculateCosts(entity);
        entity.setUpdatedAt(LocalDateTime.now());

        entity = flightShipmentRepository.save(entity);
        return mapToResponse(entity);
    }

    public void deleteFlightShipment(Long id) {
        flightShipmentRepository.deleteById(id);
    }

    private void mapRequestToEntity(FlightShipmentRequest request, FlightShipment entity) {
        entity.setFlightCode(request.getFlightCode());
        entity.setAwbFilePath(request.getAwbFilePath());
        entity.setInvoiceFilePath(request.getInvoiceFilePath());
        entity.setTotalVolumeWeight(request.getTotalVolumeWeight());
        entity.setAirFreightCost(request.getAirFreightCost());
        entity.setCustomsClearanceCost(request.getCustomsClearanceCost());
        entity.setAirportShippingCost(request.getAirportShippingCost());
        entity.setOtherCosts(request.getOtherCosts());
        entity.setArrivalDate(request.getArrivalDate());
        entity.setAirFreightPaid(request.isAirFreightPaid());
        entity.setAirFreightPaidDate(request.getAirFreightPaidDate());
        entity.setCustomsPaid(request.isCustomsPaid());
        entity.setCustomsPaidDate(request.getCustomsPaidDate());

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên ID: " + request.getStaffId()));
        entity.setStaff(staff);
    }

    private void calculateCostsAndProfit(FlightShipment entity) {
        BigDecimal total = entity.getAirFreightCost()
                .add(entity.getCustomsClearanceCost())
                .add(entity.getAirportShippingCost())
                .add(entity.getOtherCosts());
        entity.setTotalCost(total);

        if (entity.getTotalVolumeWeight() != null && entity.getTotalVolumeWeight().compareTo(BigDecimal.ZERO) > 0) {
            entity.setOriginCostPerKg(total.divide(entity.getTotalVolumeWeight(), 4, RoundingMode.HALF_UP));
        } else {
            entity.setOriginCostPerKg(BigDecimal.ZERO);
        }
        entity.setGrossProfit(BigDecimal.ZERO);
    }

    private FlightShipmentResponse mapToResponse(FlightShipment entity) {
        FlightShipmentResponse request = new FlightShipmentResponse();
        return request;
    }

    private void calculateProfitIfNeeded(FlightShipment entity) {
        // Ví dụ: query tổng tiền thu khách từ các Order → trừ totalCost
        // Để chính xác nhất, nên chạy batch job định kỳ hoặc trigger khi Order thay đổi
    }

    private void calculateCosts(FlightShipment entity) {
        BigDecimal total = entity.getAirFreightCost()
                .add(entity.getCustomsClearanceCost())
                .add(entity.getAirportShippingCost())
                .add(entity.getOtherCosts());
        entity.setTotalCost(total);

        if (entity.getTotalVolumeWeight() != null && entity.getTotalVolumeWeight().compareTo(BigDecimal.ZERO) > 0) {
            entity.setOriginCostPerKg(total.divide(entity.getTotalVolumeWeight(), 2, RoundingMode.HALF_UP));
        } else {
            entity.setOriginCostPerKg(BigDecimal.ZERO);
        }
    }
}
