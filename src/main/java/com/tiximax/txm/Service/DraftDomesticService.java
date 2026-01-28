package com.tiximax.txm.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.DraftDomestic;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.Carrier;
import com.tiximax.txm.Enums.DraftDomesticStatus;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.DraftDomesticRequest;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.UpdateDraftDomesticInfoRequest;
import com.tiximax.txm.Model.DTOResponse.Domestic.ShipCodePayment;
import com.tiximax.txm.Model.DTOResponse.Domestic.WarehouseShip;
import com.tiximax.txm.Model.DTOResponse.DraftDomestic.AvailableAddDarfDomestic;
import com.tiximax.txm.Model.DTOResponse.DraftDomestic.DraftDomesticResponse;
import com.tiximax.txm.Model.Projections.CustomerShipmentRow;
import com.tiximax.txm.Model.Projections.DraftDomesticDeliveryRow;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Repository.DraftDomesticRepository;
import com.tiximax.txm.Repository.RouteRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;

import jakarta.transaction.Transactional;

@Service
public class DraftDomesticService {
 @Autowired
 private DraftDomesticRepository draftDomesticRepository;
 @Autowired 
 private CustomerRepository customerRepository;
 @Autowired
 private WarehouseRepository warehouseRepository;
 @Autowired
 private RouteRepository routeRepository;
 @Autowired 
 private PartialShipmentService partialShipmentService;
 @Autowired
 private AccountUtils accountUtils;

    private static final List<WarehouseStatus> AVAILABLE_STATUSES = List.of(
            WarehouseStatus.DA_NHAP_KHO_NN,
            WarehouseStatus.DA_NHAP_KHO_VN
    );

   public DraftDomesticResponse addDraftDomestic(
            DraftDomesticRequest draft
    ) {

        Customer customer = customerRepository
                .findByCustomerCode(draft.getCustomerCode())
                .orElseThrow(() ->
                        new NotFoundException("Không tìm thấy khách hàng")
                );

        // 1. Validate tracking codes
        validateAllTrackingCodesExist(draft.getShippingList());

        // 2. Tính cân nặng
        Double sumWeight =
                warehouseRepository.sumWeightByTrackingCodes(
                        draft.getShippingList()
                );
        Double weight = calculateAndRoundWeight(sumWeight);

        // 3. Đếm tổng kiện khả dụng
        int totalAvailableCount =
                warehouseRepository.countAvailableByCustomerCode(
                        customer.getCustomerCode(),
                        AVAILABLE_STATUSES
                );

        // 4. Generate shipCode
        String shipCode = generateShipCode(
                customer.getCustomerCode(),
                draft.getShippingList().size(),
                totalAvailableCount
        );

        // 5. Map entity
        DraftDomestic draftDomestic =
                mapToEntity(draft, customer);

        draftDomestic.setWeight(weight);
        draftDomestic.setShipCode(shipCode);

        draftDomesticRepository.save(draftDomestic);

        return new DraftDomesticResponse(draftDomestic);
    }

       public ShipCodePayment getShipCodePayment(String shipCode) {

        // 1. Lấy draft domestic
        DraftDomestic draft = draftDomesticRepository
                .findByShipCode(shipCode)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy DraftDomestic"));

        // 2. Lấy danh sách trackingCode
        List<String> trackingCodes = draft.getShippingList();
        if (trackingCodes == null || trackingCodes.isEmpty()) {
            throw new RuntimeException("DraftDomestic chưa có shippingList");
        }

           List<WarehouseShip> warehouseShips =
            warehouseRepository.findWarehouseShips(
                    draft.getShippingList()
            );
        // 5. Tính tổng tiền ship
        BigDecimal totalPriceShip = partialShipmentService.calculateTotalShippingFee(trackingCodes);

        ShipCodePayment shipcodePayment = new ShipCodePayment();
        shipcodePayment.setShipCode(shipCode);
        shipcodePayment.setWarehouseShips(warehouseShips);
        shipcodePayment.setTotalPriceShip(totalPriceShip);

        return shipcodePayment;
    }

 
 public DraftDomesticResponse getDraftDomestic(Long id){
    var draftDomestic = draftDomesticRepository.findById(id).get();
    if(draftDomestic == null){
        throw new BadRequestException("Không tìm thấy đơn mẫu vận chuyển nội địa");
    }
    return new DraftDomesticResponse(draftDomestic);
 }

public Page<DraftDomesticResponse> getAllDraftDomestic(
        String customerCode,
        String shipmentCode,
        DraftDomesticStatus status,
        Carrier carrier,
        Pageable pageable
){
    Account account = accountUtils.getAccountCurrent();
    Long staffId = null;

    if (account instanceof Staff staff) {
        AccountRoles role = staff.getRole();
        if (role == AccountRoles.STAFF_SALE
                || role == AccountRoles.LEAD_SALE) {
            staffId = staff.getAccountId();
        }
    }

    Page<DraftDomestic> page = draftDomesticRepository.findAllWithFilter(
        customerCode,
        shipmentCode,
        status,
        carrier,
        staffId,
        pageable
);

    return page.map(this::mapToResponseWithRoundedWeight);
}


    public Page<DraftDomesticResponse> getAvailableToShip(
        Long routeId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
        ) {

        Page<DraftDomestic> pageResult =
                draftDomesticRepository.getDraftToExport(
                        routeId,         
                        startDate,
                        endDate,
                        pageable
                );     

        return pageResult.map(DraftDomesticResponse::new);
    }


@Transactional
public DraftDomesticResponse addShipments(
        Long draftId,
        List<String> shippingCodes
) {
    DraftDomestic draft = draftDomesticRepository.findById(draftId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn mẫu vận chuyển nội địa"));

    if (shippingCodes == null || shippingCodes.isEmpty()) {
        throw new BadRequestException("Danh sách mã vận đơn không được rỗng");
    }
    checkDraftEditable(draft);
    validateAllTrackingCodesExist(shippingCodes);

    if (draft.getShippingList() == null) {
        draft.setShippingList(new ArrayList<>());
    }

    Set<String> existing = new HashSet<>(draft.getShippingList());
    for (String code : shippingCodes) {
        String trimmed = code.trim();
        if (!existing.contains(trimmed)) {
            draft.getShippingList().add(trimmed);
        }
    }
    Double sumWeight = warehouseRepository.sumWeightByTrackingCodes(draft.getShippingList());
    Double weight = calculateAndRoundWeight(sumWeight);
    draft.setWeight(weight);
    draft.setShipCode(draft.getCustomer().getCustomerCode() + "-" + draft.getShippingList().size());
    draftDomesticRepository.save(draft);
    return new DraftDomesticResponse(draft);
}

// Lấy danh sách cho staff thêm vào draft domestic
 public Page<AvailableAddDarfDomestic> getAvailableAddDraftDomestic(
       String customerCode,
        Long staffId,
        Long routeId,
        Pageable pageable
){
     String filterCustomerCode =
                (customerCode == null || customerCode.isBlank())
                        ? null
                        : customerCode.trim().toUpperCase();

        if (filterCustomerCode != null &&
            !customerRepository.existsByCustomerCode(filterCustomerCode)) {
            throw new NotFoundException("Mã khách hàng không tồn tại!");
        }
        
    Long filterRoute =
                (routeId == null || routeId == 0)
                        ? null : routeId;
        if (filterRoute != null &&
            !routeRepository.existsById(filterRoute)) {
            throw new NotFoundException("Tuyến này không tồn tại!");
        }

         Page<DraftDomesticDeliveryRow> customerPage =
                warehouseRepository.findDraftDomesticDelivery(
                        AVAILABLE_STATUSES,
                        staffId,
                        filterCustomerCode,
                        filterRoute,
                        pageable
                );

            
        if (customerPage.isEmpty()) {
            return Page.empty(pageable);
        }
        List<String> customerCodes = customerPage.getContent().stream()
                .map(DraftDomesticDeliveryRow::getCustomerCode)
                .filter(Objects::nonNull)
                .toList();

        List<CustomerShipmentRow> rows =
                warehouseRepository.findTrackingCodesByCustomerCodes(
                        AVAILABLE_STATUSES,
                        customerCodes,
                        staffId,
                        filterRoute
                );

        Map<String, List<String>> shipmentMap = new HashMap<>();
        for (CustomerShipmentRow r : rows) {
    shipmentMap
        .computeIfAbsent(r.getCustomerCode(), k -> new ArrayList<>())
        .add(r.getTrackingCode());
}

        List<AvailableAddDarfDomestic> result = new ArrayList<>();
        for (DraftDomesticDeliveryRow c : customerPage.getContent()) {
            result.add(new AvailableAddDarfDomestic(
                    c.getCustomerCode(),
                    c.getCustomerName(),
                    c.getPhoneNumber(),
                    c.getAddress(),
                    c.getRouteName(),
                    shipmentMap.getOrDefault(c.getCustomerCode(), List.of())
            ));
        }
        return new PageImpl<>(result, pageable, customerPage.getTotalElements());
    }


@Transactional
public DraftDomesticResponse updateDraftInfo(
        Long draftId,
        UpdateDraftDomesticInfoRequest request
) {
    DraftDomestic draft = draftDomesticRepository.findById(draftId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn mẫu vận chuyển nội địa"));
    checkDraftEditable(draft);
    boolean updated = false;

    if (request.getPhoneNumber() != null) {
        String phone = request.getPhoneNumber().trim();
        if (phone.isEmpty()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }
        draft.setPhoneNumber(phone);
        updated = true;
    }
    if (request.getAddress() != null) {
        String address = request.getAddress().trim();
        if (address.isEmpty()) {
            throw new BadRequestException("Địa chỉ không được để trống");
        }
        draft.setAddress(address);
        updated = true;
    }

    if (!updated) {
        throw new BadRequestException("Không có dữ liệu để cập nhật");
    }

    draftDomesticRepository.save(draft);
    return new DraftDomesticResponse(draft);
}

@Transactional
public DraftDomesticResponse removeShipments(
        Long draftId,
        List<String> shippingCodes
) {
    DraftDomestic draft = draftDomesticRepository.findById(draftId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy mẫu vận chuyển nội địa"));

    checkDraftEditable(draft);
    if (draft.getShippingList() == null || draft.getShippingList().isEmpty()) {
        throw new BadRequestException("mẫu vận chuyển chưa có mã vận chuyển nào");
    }

    if (shippingCodes == null || shippingCodes.isEmpty()) {
        throw new BadRequestException("Danh sách mã vận đơn cần xóa không được rỗng");
    }

    Set<String> removeSet = shippingCodes.stream()
            .map(String::trim)
            .collect(Collectors.toSet());

    draft.getShippingList()
            .removeIf(code -> removeSet.contains(code));
    Double sumWeight = warehouseRepository.sumWeightByTrackingCodes(draft.getShippingList());
    Double weight = calculateAndRoundWeight(sumWeight);
    draft.setWeight(weight);
    draft.setShipCode(draft.getCustomer().getCustomerCode() + "-" + draft.getShippingList().size());
    draftDomesticRepository.save(draft);
    return new DraftDomesticResponse(draft);
}
@Transactional
public Boolean deleteDraftDomestic(Long draftId) {

    DraftDomestic draft = draftDomesticRepository.findById(draftId)
            .orElseThrow(() ->
                    new NotFoundException("Không tìm thấy mẫu vận chuyển nội địa")
            );
     checkDraftEditable(draft);
    draftDomesticRepository.delete(draft);
    return true;
}


@Transactional
public Boolean ExportDraftDomestic(List<Long> draftIds) {

    if (draftIds == null || draftIds.isEmpty()) {
        throw new BadRequestException("Danh sách draftId không được rỗng");
    }

    List<DraftDomestic> drafts = draftDomesticRepository.findAllById(draftIds);
  if (drafts.size() != draftIds.size()) {
        throw new BadRequestException("Có mẫu vận chuyển nội địa không tồn tại");
    }

    // 2. Gom toàn bộ trackingCode
    Set<String> allTrackingCodes = new HashSet<>();

    for (DraftDomestic draft : drafts) {

        if (draft.getStatus() != DraftDomesticStatus.LOCKED) {
            throw new BadRequestException(
                "Draft " + draft.getId() + " không ở trạng thái LOCKED"
            );
        }
        List<String> shippingList = draft.getShippingList();

        if (shippingList == null || shippingList.isEmpty()) {
            throw new BadRequestException(
                "DraftDomestic ID " + draft.getId() + " có danh sách trackingCode trống"
            );
        }

        shippingList.forEach(code -> {
            if (code != null && !code.trim().isEmpty()) {
                allTrackingCodes.add(code.trim());
            }
        });
    }

    List<Warehouse> warehouses =
            warehouseRepository.findByTrackingCodeInAndStatus(
                    new ArrayList<>(allTrackingCodes),
                    WarehouseStatus.CHO_GIAO
            );

    if (warehouses.size() != allTrackingCodes.size()) {

        Set<String> validCodes = warehouses.stream()
                .map(Warehouse::getTrackingCode)
                .collect(Collectors.toSet());

        Set<String> invalidCodes = new HashSet<>(allTrackingCodes);
        invalidCodes.removeAll(validCodes); 

        throw new BadRequestException(
            "Các trackingCode không hợp lệ hoặc không ở CHO_GIAO: " + invalidCodes
        );
    }

    drafts.forEach(d -> d.setStatus(DraftDomesticStatus.EXPORTED));
    draftDomesticRepository.saveAll(drafts);

    return true;
 }

public List<DraftDomesticResponse> getLockedDraftNotExported(
        LocalDate endDate, 
        Carrier carrier     
) {

    if (endDate == null) {
        endDate = LocalDate.now();
    }

    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
    LocalDateTime startDateTime = endDateTime.minusDays(7);

    return draftDomesticRepository
        .findLockedBetween( DraftDomesticStatus.LOCKED,carrier,startDateTime, endDateTime )
        .stream()
        .map(this::mapToResponseWithRoundedWeight)
        .toList();
}


    public Page<DraftDomesticResponse> getDraftsToLock(
        Long routeId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    ) {
    return draftDomesticRepository
            .getDraftToExport(routeId, startDate, endDate, pageable)
            .map(DraftDomesticResponse::new);
    }

     @Transactional
    public void checkAndLockDraftDomesticByShipmentCodes(
            Set<String> paidShipmentCodes
    ) {

        if (paidShipmentCodes == null || paidShipmentCodes.isEmpty()) {
            return;
        }

        List<DraftDomestic> drafts =
                draftDomesticRepository.findDraftByShipmentCodes(paidShipmentCodes);

        for (DraftDomestic draft : drafts) {

            List<String> shippingList = draft.getShippingList();
            if (shippingList == null || shippingList.isEmpty()) {
                continue;
            }

            Set<String> trackingCodes = new HashSet<>(shippingList);

            long total =
                    warehouseRepository.countByTrackingCodeIn(trackingCodes);

            long ready =
                    warehouseRepository.countByTrackingCodeInAndStatus(
                            trackingCodes,
                            WarehouseStatus.CHO_GIAO
                    );

            if (total > 0 && total == ready) {
                draft.setStatus(DraftDomesticStatus.LOCKED);;
                draftDomesticRepository.save(draft);
            }
        }
    }

// IMPORT FILE VNPOST DRAFT DOMESTIC


//


  private DraftDomestic mapToEntity(
            DraftDomesticRequest request,
            Customer customer
    ) {

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        DraftDomestic draft = new DraftDomestic();
        draft.setCustomer(customer);
        draft.setPhoneNumber(request.getPhoneNumber());
        draft.setAddress(request.getAddress());
        draft.setShippingList(request.getShippingList());
        draft.setCarrier(request.getCarrier());
        draft.setStaff(staff);
        return draft;
    }
 
   private void validateAllTrackingCodesExist(List<String> shippingList) {

    if (shippingList == null || shippingList.isEmpty()) {
        throw new BadRequestException("Danh sách mã vận đơn không được để trống");
    }

    Set<String> inputCodes = shippingList.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

    List<String> validCodes =
            warehouseRepository.findExistingTrackingCodesByStatus(
                    new ArrayList<>(inputCodes)
                  //  WarehouseStatus.CHO_GIAO
            );
    if (validCodes.size() != inputCodes.size()) {

        Set<String> invalidCodes = new HashSet<>(inputCodes);
        invalidCodes.removeAll(validCodes);

        throw new BadRequestException(
                "Các mã vận đơn không hợp lệ hoặc không ở trạng thái CHO_GIAO: "
                        + invalidCodes
        );
    }
    List<String> existedInDraft =
            draftDomesticRepository.findExistingTrackingCodesInDraft(
                    new ArrayList<>(inputCodes)
            );
    if (!existedInDraft.isEmpty()) {
        throw new BadRequestException(
                "Mã đơn " + existedInDraft + "đã được thêm ở draft domestic khác: "
        );
    }
}
private void checkDraftEditable(DraftDomestic draft) {
    if (draft.getStatus() != DraftDomesticStatus.DRAFT) {
        throw new BadRequestException(
            "Mẫu vận chuyển đã " + draft.getStatus() + ", không thể chỉnh sửa"
        );
    }
}


  private String generateShipCode(
            String customerCode,
            int selectedCount,
            int totalAvailableCount
    ) {

        if (selectedCount == totalAvailableCount) {
            return customerCode + "-" + selectedCount;
        }

        List<String> existingCodes =
                draftDomesticRepository
                        .findShipCodesByCustomer(customerCode);

        char maxSuffix = 'A' - 1;

        for (String code : existingCodes) {
            if (code.matches(customerCode + "-\\d+-[A-Z]")) {
                char suffix = code.charAt(code.length() - 1);
                if (suffix > maxSuffix) {
                    maxSuffix = suffix;
                }
            }
        }

        return customerCode
                + "-" + selectedCount
                + "-" + (char) (maxSuffix + 1);
    }


private Double calculateAndRoundWeight(Double totalWeight) {
    if (totalWeight == null) return 0.0;

    return BigDecimal.valueOf(totalWeight)
            .multiply(BigDecimal.valueOf(0.9)) 
            .setScale(3, RoundingMode.HALF_UP)
            .doubleValue();
}
private Double roundWeight(Double weight) {
    if (weight == null) return 0.0;

    return BigDecimal.valueOf(weight)
            .setScale(3, RoundingMode.HALF_UP)
            .doubleValue();
}

private DraftDomesticResponse mapToResponseWithRoundedWeight(DraftDomestic draft) {
    DraftDomesticResponse response = new DraftDomesticResponse(draft);
    response.setWeight(roundWeight(draft.getWeight()));
    return response;
}



}
