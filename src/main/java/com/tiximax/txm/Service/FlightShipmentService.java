package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.FlightShipment;
import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.FlightStatus;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.FlightShipment.FlightShipmentRequest;
import com.tiximax.txm.Model.DTORequest.FlightShipment.UpdateFlightShipmentRequest;
import com.tiximax.txm.Model.DTOResponse.FlightShipment.FlightShipmentResponse;
import com.tiximax.txm.Repository.FlightShipmentRepository;
import com.tiximax.txm.Repository.PackingRepository;
import com.tiximax.txm.Repository.StaffRepository;
import static com.tiximax.txm.Utils.Helpper.UpdateHelper.*;

import com.tiximax.txm.Utils.AccountUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service

public class FlightShipmentService {
    @Autowired
    private FlightShipmentRepository flightShipmentRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private PackingRepository packingRepository;


    @Transactional
    public void createFlight(String flightCode){
        if (flightShipmentRepository.existsByFlightCode(flightCode)) {
            throw new BadRequestException("Mã chuyến bay đã tồn tại: " + flightCode);
        }
        List<Packing> packings = packingRepository.findByFlightCode(flightCode);
        for (Packing packing : packings){
            packing.setStatusFlight(true);
        }
        FlightShipment entity = new FlightShipment();
        entity.setFlightCode(flightCode);
        entity.setStatus(FlightStatus.DANG_CHO);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setStaff((Staff) accountUtils.getAccountCurrent());
        entity = flightShipmentRepository.save(entity);
    }
    @Transactional
public FlightShipmentResponse updateFlightShipment(Long id, UpdateFlightShipmentRequest request) {

    FlightShipment entity = flightShipmentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy chuyến bay"));
    applyIfPresent(request.getAwbFilePath(), entity::setAwbFilePath);
    applyIfPresent(request.getExportLicensePath(), entity::setExportLicensePath);
    applyIfPresent(request.getSingleInvoicePath(), entity::setSingleInvoicePath);
    applyIfPresent(request.getInvoiceFilePath(), entity::setInvoiceFilePath);
    applyIfPresent(request.getPackingListPath(), entity::setPackingListPath);
    applyIfPresent(request.getTotalVolumeWeight(), entity::setTotalVolumeWeight);
    applyIfPresent(request.getAirFreightCost(), entity::setAirFreightCost);
    applyIfPresent(request.getCustomsClearanceCost(), entity::setCustomsClearanceCost);
    applyIfPresent(request.getAirportShippingCost(), entity::setAirportShippingCost);
    applyIfPresent(request.getOtherCosts(), entity::setOtherCosts);
    applyIfPresent(request.getAirFreightPaid(), entity::setAirFreightPaid);
    applyIfPresent(request.getCustomsPaid(), entity::setCustomsPaid);
    calculateCostsAndProfit(entity);
    calculateProfitIfNeeded(entity);
    return mapToResponse(entity);
}

    @Transactional
    public FlightShipment createFlightShipment(FlightShipmentRequest request) {
        if (flightShipmentRepository.existsByFlightCode(request.getFlightCode())) {
            throw new BadRequestException("Mã chuyến bay đã tồn tại: " + request.getFlightCode());
        }
        List<Packing> packings = packingRepository.findByFlightCode(request.getFlightCode());
        for (Packing packing : packings){
            packing.setStatusFlight(true);
        }
        FlightShipment entity = new FlightShipment();
        mapRequestToEntity(request, entity);
        calculateCostsAndProfit(entity);
        calculateProfitIfNeeded(entity);
        entity.setCreatedAt(LocalDateTime.now());
        entity = flightShipmentRepository.save(entity);
        return entity;
    }

    public FlightShipmentResponse getFlightShipmentId(Long id) {
        FlightShipment entity = flightShipmentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy chuyến bay"));
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
                .orElseThrow(() -> new BadRequestException("Không tìm thấy chuyến bay"));
        if (request.getFlightCode() != null){entity.setFlightCode(request.getFlightCode());}
        if (request.getAwbFilePath() != null){entity.setAwbFilePath(request.getAwbFilePath());}
        if (request.getExportLicensePath() != null){entity.setExportLicensePath(request.getExportLicensePath());}
        if (request.getSingleInvoicePath() != null){entity.setSingleInvoicePath(request.getSingleInvoicePath());}
        if (request.getInvoiceFilePath() != null){entity.setInvoiceFilePath(request.getInvoiceFilePath());}
        if (request.getPackingListPath() != null){entity.setPackingListPath(request.getPackingListPath());}
        if (request.getTotalVolumeWeight() != null){entity.setTotalVolumeWeight(request.getTotalVolumeWeight());}
        if (request.getAirFreightCost() != null){entity.setAirFreightCost(request.getAirFreightCost());}
        if (request.getCustomsClearanceCost() != null){entity.setCustomsClearanceCost(request.getCustomsClearanceCost());}
        if (request.getAirportShippingCost() != null){entity.setAirportShippingCost(request.getAirportShippingCost());}
        if (request.getOtherCosts() != null){entity.setOtherCosts(request.getOtherCosts());}
        if (request.getArrivalDate() != null){entity.setArrivalDate(request.getArrivalDate().atStartOfDay());}
        if (request.getAirFreightPaid() != null){entity.setAirFreightPaid(request.getAirFreightPaid());}
        if (request.getAirFreightPaidDate() != null){entity.setAirFreightPaidDate(request.getAirFreightPaidDate().atStartOfDay());}
        if (request.getCustomsPaid() != null){entity.setCustomsPaid(request.getCustomsPaid());}
        if (request.getCustomsPaidDate() != null){entity.setCustomsPaidDate(request.getCustomsPaidDate().atStartOfDay());}
        entity.setStaff((Staff) accountUtils.getAccountCurrent());

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
        entity.setAwbFilePath(request.getAwbFilePath() != null ? request.getFlightCode() : "");
        entity.setExportLicensePath(request.getExportLicensePath() != null ? request.getFlightCode() : "");
        entity.setSingleInvoicePath(request.getSingleInvoicePath() != null ? request.getFlightCode() : "");
        entity.setInvoiceFilePath(request.getInvoiceFilePath() != null ? request.getFlightCode() : "");
        entity.setPackingListPath(request.getPackingListPath() != null ? request.getFlightCode() : "");
        entity.setTotalVolumeWeight(request.getTotalVolumeWeight());
        entity.setAirFreightCost(request.getAirFreightCost());
        entity.setCustomsClearanceCost(request.getCustomsClearanceCost());
        entity.setAirportShippingCost(request.getAirportShippingCost());
        entity.setOtherCosts(request.getOtherCosts());
        entity.setArrivalDate(request.getArrivalDate().atStartOfDay());
        entity.setAirFreightPaid(request.getAirFreightPaid());
        entity.setAirFreightPaidDate(request.getAirFreightPaidDate().atStartOfDay());
        entity.setCustomsPaid(request.getCustomsPaid());
        entity.setCustomsPaidDate(request.getCustomsPaidDate().atStartOfDay());
        entity.setStaff((Staff) accountUtils.getAccountCurrent());
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
        FlightShipmentResponse response = new FlightShipmentResponse();

        response.setFlightShipmentId(entity.getFlightShipmentId());
        response.setFlightCode(entity.getFlightCode());
        response.setAwbFilePath(entity.getAwbFilePath());
        response.setExportLicensePath(entity.getExportLicensePath());
        response.setSingleInvoicePath(entity.getSingleInvoicePath());
        response.setInvoiceFilePath(entity.getInvoiceFilePath());
        response.setPackingListPath(entity.getPackingListPath());

        response.setTotalVolumeWeight(entity.getTotalVolumeWeight());
        response.setAirFreightCost(entity.getAirFreightCost());
        response.setCustomsClearanceCost(entity.getCustomsClearanceCost());
        response.setAirportShippingCost(entity.getAirportShippingCost());
        response.setOtherCosts(entity.getOtherCosts());
        response.setTotalCost(entity.getTotalCost());
        response.setOriginCostPerKg(entity.getOriginCostPerKg());

        response.setGrossProfit(entity.getGrossProfit() != null ? entity.getGrossProfit() : BigDecimal.ZERO);

        response.setArrivalDate(entity.getArrivalDate());
        response.setCreatedAt(entity.getCreatedAt());
        response.setAirFreightPaidDate(entity.getAirFreightPaidDate());
        response.setCustomsPaidDate(entity.getCustomsPaidDate());
        if (entity.getStaff() != null) {
            response.setStaffName(entity.getStaff().getName());
        }
        return response;
    }

    private void calculateProfitIfNeeded(FlightShipment entity) {
        BigDecimal totalRevenueFromCustomers = flightShipmentRepository.calculateProfitForFlight(entity.getFlightCode());
        BigDecimal totalCost = entity.getTotalCost() != null ? entity.getTotalCost() : BigDecimal.ZERO;

        BigDecimal grossProfit = totalRevenueFromCustomers.subtract(totalCost);
        entity.setGrossProfit(grossProfit);
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
       public List<String> getAvailableFlightCodesByRoute(Long routeId) {

        LocalDateTime fromDate = LocalDateTime.now().minusMonths(3);

        return packingRepository.findAvailableFlightCodesByRouteLast3Months(
                routeId,
                fromDate
        );
}
}