package com.tiximax.txm.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
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
import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.Carrier;
import com.tiximax.txm.Enums.DraftDomesticStatus;
import com.tiximax.txm.Enums.OrderLinkStatus;
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
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.RouteRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DraftDomesticService {
 @Autowired
 private DraftDomesticRepository draftDomesticRepository;
 @Autowired 
 private CustomerRepository customerRepository;
 @Autowired
 private WarehouseRepository warehouseRepository;
 @Autowired
 private  OrderLinksRepository orderLinksRepository;
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

    // 1. Lấy DraftDomestic
    DraftDomestic draft = draftDomesticRepository
            .findByShipCode(shipCode)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy DraftDomestic")
            );

    List<String> trackingCodes = draft.getShippingList();
    if (trackingCodes == null || trackingCodes.isEmpty()) {
        throw new RuntimeException("DraftDomestic chưa có shippingList");
    }

    List<WarehouseShip> warehouseShips =
            warehouseRepository.findWarehouseShips(trackingCodes);

    Map<String, List<String>> shipCodeTrackingMap =
            Map.of(shipCode, trackingCodes);

    BigDecimal totalPriceShip =
            partialShipmentService
                    .calculateFeeByShipCodeAllowMultiRoute(
                            shipCodeTrackingMap
                    )
                    .get(shipCode);

    if (totalPriceShip == null ||
        totalPriceShip.compareTo(BigDecimal.ZERO) <= 0) {
        throw new RuntimeException("Không thể tính phí vận chuyển");
    }

    Customer customer = draft.getCustomer();

    ShipCodePayment shipCodePayment = new ShipCodePayment();
    shipCodePayment.setShipCode(shipCode);
    shipCodePayment.setCustomerId(customer.getAccountId().toString());
    shipCodePayment.setCustomerName(customer.getName());
    shipCodePayment.setWarehouseShips(warehouseShips);
    shipCodePayment.setTotalPriceShip(totalPriceShip);
    shipCodePayment.setPayment(draft.isPayment());

    return shipCodePayment;
}

  public List<ShipCodePayment> getAllShipByStaff(
        Long staffId,
        String keyword,
        Boolean payment,   
        Pageable pageable
) {

    Page<DraftDomestic> drafts =
            (keyword == null || keyword.isBlank())
                    ? draftDomesticRepository.findByStaff(
                            staffId,
                            payment,
                            pageable
                      )
                    : draftDomesticRepository.searchByStaffAndKeyword(
                            staffId,
                            keyword,
                            payment,
                            pageable
                      );

    if (drafts.isEmpty()) {
        return List.of();
    }

    List<DraftDomestic> validDrafts =
            drafts.stream()
                    .filter(d ->
                            d.getShippingList() != null &&
                            !d.getShippingList().isEmpty()
                    )
                    .toList();

    if (validDrafts.isEmpty()) {
        return List.of();
    }

    Map<String, DraftDomestic> shipCodeDraftMap =
            validDrafts.stream()
                    .collect(Collectors.toMap(
                            DraftDomestic::getShipCode,
                            d -> d,
                            (a, b) -> a 
                    ));

    Map<String, List<String>> shipCodeTrackingMap =
            validDrafts.stream()
                    .collect(Collectors.groupingBy(
                            DraftDomestic::getShipCode,
                            Collectors.flatMapping(
                                    d -> d.getShippingList().stream(),
                                    Collectors.toList()
                            )
                    ));

    List<String> allTrackingCodes =
            shipCodeTrackingMap.values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .toList();

    List<WarehouseShip> allWarehouseShips =
            warehouseRepository.findWarehouseShips(allTrackingCodes);

    Map<String, WarehouseShip> warehouseShipMap =
            allWarehouseShips.stream()
                    .collect(Collectors.toMap(
                            WarehouseShip::getShipmentCode,
                            ws -> ws,
                            (a, b) -> a
                    ));

    Map<String, BigDecimal> feeMap =
            partialShipmentService
                    .calculateFeeByShipCodeAllowMultiRoute(
                            shipCodeTrackingMap
                    );

    // 5️⃣ build response
    List<ShipCodePayment> result = new ArrayList<>();

    for (Map.Entry<String, List<String>> entry : shipCodeTrackingMap.entrySet()) {

        String shipCode = entry.getKey();
        List<String> trackingCodes = entry.getValue();

        DraftDomestic draft = shipCodeDraftMap.get(shipCode);
        Customer customer = draft.getCustomer();

        List<WarehouseShip> warehouseShips =
                trackingCodes.stream()
                        .map(warehouseShipMap::get)
                        .filter(Objects::nonNull)
                        .toList();

        BigDecimal totalPriceShip = feeMap.getOrDefault(
                shipCode,
                BigDecimal.ZERO
        );

        ShipCodePayment item = new ShipCodePayment();
        item.setShipCode(shipCode);
        item.setCustomerId(customer.getAccountId().toString());
        item.setCustomerName(customer.getName());
        item.setWarehouseShips(warehouseShips);
        item.setTotalPriceShip(totalPriceShip);

        result.add(item);
    }

    return result;
}

public void updatePayment(DraftDomestic draft){
    draft.setPayment(true);
    draftDomesticRepository.save(draft);
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
        throw new BadRequestException("Mẫu vận chuyển chưa có mã vận chuyển nào");
    }

    if (shippingCodes == null || shippingCodes.isEmpty()) {
        throw new BadRequestException("Danh sách mã vận đơn cần xóa không được rỗng");
    }

    Set<String> removeSet = shippingCodes.stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());

    // 1️⃣ Xóa shippingCode
    draft.getShippingList().removeIf(removeSet::contains);

    // 2️⃣ Nếu KHÔNG CÒN shippingCode → XÓA CỨNG draft
    if (draft.getShippingList().isEmpty()) {
        draftDomesticRepository.deleteById(draftId);
        return null;
    }
          
    // 3️⃣ Còn shippingCode → cập nhật lại draft
    Double sumWeight =
            warehouseRepository.sumWeightByTrackingCodes(draft.getShippingList());

    draft.setWeight(calculateAndRoundWeight(sumWeight));
    draft.setShipCode(
            draft.getCustomer().getCustomerCode()
                    + "-" + draft.getShippingList().size()
    );

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
        Long staffId,     // có thể null
        Carrier carrier
) {

    if (endDate == null) {
        endDate = LocalDate.now();
    }

    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
    LocalDateTime startDateTime = endDateTime.minusDays(7);

    return draftDomesticRepository
        .findLockedBetween(
            DraftDomesticStatus.LOCKED,
            carrier,
            staffId, 
            startDateTime,
            endDateTime
        )
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


    public DraftDomestic getDraftDomesticByShipCode (String shipCode) {
        var draft = draftDomesticRepository.findByShipCode(shipCode);
        if (draft.isEmpty()) {
            throw new NotFoundException("vận đơn mẫu không tồn tại");
        }
        return draft.get();
    }

     @Transactional
    public void syncAndLockDraftDomestic() {

        List<DraftDomestic> drafts =
                draftDomesticRepository.findByStatus(DraftDomesticStatus.DRAFT);

        if (drafts.isEmpty()) return;

        for (DraftDomestic draft : drafts) {

            if (draft.getShippingList() == null
                    || draft.getShippingList().isEmpty()) {
                continue;
            }

            // 1. Extract shipmentCode từ shippingList
            List<String> shipmentCodes = draft.getShippingList().stream()
                    .map(this::extractShipmentCode)
                    .toList();

            // 2. Load OrderLinks
            List<OrderLinks> links =
                    orderLinksRepository.findByShipmentCodeIn(shipmentCodes);

            if (links.size() != shipmentCodes.size()) {
                continue;
            }

            // 3. Check ALL điều kiện
            boolean allReady = links.stream().allMatch(link ->
                    link.getStatus() == OrderLinkStatus.CHO_GIAO
                    && link.getWarehouse() != null
                    && link.getWarehouse().getStatus() == WarehouseStatus.CHO_GIAO
            );

            if (!allReady) continue;

            // 4. Đồng bộ lại shippingList
            List<String> newShippingList = links.stream()
                    .map(link ->
                            link.getShipmentCode() + " = " +
                            link.getWarehouse().getTrackingCode()
                    )
                    .toList();

            draft.setShippingList(new ArrayList<>(newShippingList));
            draft.setStatus(DraftDomesticStatus.LOCKED);

            draftDomesticRepository.save(draft);

            log.info("🔒 DraftDomestic {} LOCKED by nightly job",
                    draft.getId());
        }
    }

    private String extractShipmentCode(String entry) {
        return entry.split("=")[0].trim();
    }

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
private String roundWeight(Double weight) {
    if (weight == null) return "0.000";

    DecimalFormat df = new DecimalFormat("0.000");
    df.setRoundingMode(RoundingMode.HALF_UP);
    return df.format(weight);
}


private DraftDomesticResponse mapToResponseWithRoundedWeight(DraftDomestic draft) {
    DraftDomesticResponse response = new DraftDomesticResponse(draft);
    response.setWeight(roundWeight(draft.getWeight()));
    return response;
}



}
