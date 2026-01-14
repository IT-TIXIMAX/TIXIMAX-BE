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
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.DraftDomesticRequest;
import com.tiximax.txm.Model.DTORequest.DraftDomestic.UpdateDraftDomesticInfoRequest;
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
 private AccountUtils accountUtils;

public DraftDomesticResponse addDraftDomestic(DraftDomesticRequest draft) {

    var customer = customerRepository
            .findByCustomerCode(draft.getCustomerCode())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng"));

    validateAllTrackingCodesExist(draft.getShippingList());
    Double sumWeight = warehouseRepository.sumWeightByTrackingCodes(draft.getShippingList());
    Double weight = calculateAndRoundWeight(sumWeight);

    DraftDomestic draftDomestic = mapToEntity(draft, customer);
    draftDomestic.setWeight(weight);

    String shipCode = generateShipCode(
            customer.getCustomerCode(),
            draft.getShippingList().size()
    );

    draftDomestic.setShipCode(shipCode);
    draftDomesticRepository.save(draftDomestic);

    return new DraftDomesticResponse(draftDomestic);
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
        Boolean lock,
        Boolean isExported,
        Pageable pageable
) {
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
            lock,
            isExported,
            staffId,
            pageable
    );

    return page.map(draft -> {
        DraftDomesticResponse response = new DraftDomesticResponse(draft);

        // 🔥 làm tròn weight khi trả về
        response.setWeight(roundWeight(draft.getWeight()));

        return response;
    });
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
    checkDraftDomesticLocked(draftId);
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
                        WarehouseStatus.DA_NHAP_KHO,
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
                         WarehouseStatus.DA_NHAP_KHO,
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
    checkDraftDomesticLocked(draftId);
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

    checkDraftDomesticLocked(draftId);
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
    if (Boolean.TRUE.equals(draft.getIsLocked())) {
        throw new BadRequestException(
                "Mẫu vận chuyển nội địa đã bị khóa, không thể xóa"
        );
    }


    draftDomesticRepository.delete(draft);
    return true;
}


@Transactional
public Boolean lockDraftDomestic(List<Long> draftIds) {

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

        if (Boolean.TRUE.equals(draft.getIsExported())) {
            throw new BadRequestException(
                "DraftDomestic ID " + draft.getId() + " đã xuất file, không thể khóa"
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

    drafts.forEach(d -> d.setIsExported(true));
    draftDomesticRepository.saveAll(drafts);

    return true;
 }


public List<DraftDomesticResponse> getLockedDraftNotExported(
        LocalDate endDate
) {

    if (endDate == null) {
        endDate = LocalDate.now();
    }

    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
    LocalDateTime startDateTime = endDateTime.minusDays(7);

    return draftDomesticRepository
        .findLockedNotExportedBetween(startDateTime, endDateTime)
        .stream()
        .map(DraftDomesticResponse::new)
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
                draft.setIsLocked(true);
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
        draft.setIsVNpost(request.getIsVNpost());
        draft.setStaff(staff);
        draft.setIsLocked(false);
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
private void checkDraftDomesticLocked(Long draftId) {

    Boolean isLocked = draftDomesticRepository.isDraftLocked(draftId);
    if (isLocked != null && isLocked) {
        throw new BadRequestException(
            "Mẫu vận chuyển nội địa đã bị khoá, không thể chỉnh sửa"
        );
    }
}

private String generateShipCode(
        String customerCode,
        int packageCount
) {

    String baseCode = customerCode + "-" + packageCount;

    // Lấy tất cả shipCode dạng C00001-1-*
    List<String> existingCodes =
            draftDomesticRepository.findShipCodesByBaseCode(baseCode);

    if (existingCodes.isEmpty()) {
        return baseCode + "-A";
    }

    // Lấy suffix lớn nhất
    char maxSuffix = 'A' - 1;

    for (String code : existingCodes) {
        char suffix = code.charAt(code.length() - 1);
        if (suffix > maxSuffix) {
            maxSuffix = suffix;
        }
    }

    return baseCode + "-" + (char) (maxSuffix + 1);
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




}
