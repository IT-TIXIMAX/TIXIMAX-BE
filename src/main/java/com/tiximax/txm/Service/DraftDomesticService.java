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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.DraftDomestic;
import com.tiximax.txm.Entity.DraftDomesticShipment;
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
import com.tiximax.txm.Model.Projections.ShipCodeBasicProjection;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Repository.DraftDomesticRepository;
import com.tiximax.txm.Repository.DraftDomesticShipmentRepository;
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
 @Autowired
 private DraftDomesticShipmentRepository draftDomesticShipmentRepository; 
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

   @Transactional
public DraftDomesticResponse addDraftDomestic(
        DraftDomesticRequest request
) {

    Customer customer = customerRepository
            .findByCustomerCode(request.getCustomerCode())
            .orElseThrow(() ->
                    new NotFoundException("Không tìm thấy khách hàng")
            );

    // 1️⃣ Validate shipmentCodes
    if (request.getShippingList() == null
            || request.getShippingList().isEmpty()) {
        throw new BadRequestException(
                "DraftDomestic phải có ít nhất 1 shipmentCode"
        );
    }

    Set<String> shipmentCodes =
            request.getShippingList().stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());

    if (shipmentCodes.isEmpty()) {
        throw new BadRequestException(
                "Danh sách shipmentCode không hợp lệ"
        );
    }

    validateAllTrackingCodesExist(new ArrayList<>(shipmentCodes));

    // 2️⃣ Tính weight
    Double sumWeight =
            warehouseRepository
                    .sumWeightByTrackingCodes(new ArrayList<>(shipmentCodes));

    Double weight = calculateAndRoundWeight(sumWeight);

    // 3️⃣ Đếm tổng kiện khả dụng
    int totalAvailableCount =
            warehouseRepository.countAvailableByCustomerCode(
                    customer.getCustomerCode(),
                    AVAILABLE_STATUSES
            );

    // 4️⃣ Generate shipCode
    String shipCode = generateShipCode(
            customer.getCustomerCode(),
            shipmentCodes.size(),
            totalAvailableCount
    );

    // 5️⃣ Tạo DraftDomestic (KHÔNG shipment)
    DraftDomestic draftDomestic =
            mapToEntity(request, customer);

    draftDomestic.setWeight(weight);
    draftDomestic.setShipCode(shipCode);

    draftDomesticRepository.save(draftDomestic);

    // 6️⃣ Tạo DraftDomesticShipment
    List<DraftDomesticShipment> shipments = new ArrayList<>();

    for (String code : shipmentCodes) {
        DraftDomesticShipment shipment = new DraftDomesticShipment();
        shipment.setDraftDomestic(draftDomestic);
        shipment.setShipmentCode(code);
        shipments.add(shipment);
    }

    draftDomesticShipmentRepository.saveAll(shipments);

    return new DraftDomesticResponse(draftDomestic);
}

    public ShipCodePayment getShipCodePayment(String shipCode) {

    List<DraftDomesticShipment> shipments =
            draftDomesticShipmentRepository.findByShipCode(shipCode);

    if (shipments.isEmpty()) {
        throw new RuntimeException("Không tìm thấy DraftDomestic hoặc shipment");
    }

    DraftDomestic draft = shipments.get(0).getDraftDomestic();
    Customer customer = draft.getCustomer();

    // 1️⃣ Lấy tracking codes
    List<String> trackingCodes =
            shipments.stream()
                    .map(DraftDomesticShipment::getShipmentCode)
                    .toList();

    // 2️⃣ Lấy warehouse ships
    Map<String, WarehouseShip> warehouseShipMap =
            warehouseRepository
                    .findWarehouseShips(trackingCodes)
                    .stream()
                    .collect(Collectors.toMap(
                            WarehouseShip::getShipmentCode,
                            ws -> ws,
                            (a, b) -> a
                    ));

    List<WarehouseShip> warehouseShips =
            trackingCodes.stream()
                    .map(warehouseShipMap::get)
                    .filter(Objects::nonNull)
                    .toList();

    // 3️⃣ Tính phí ship
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

    // 4️⃣ Build response
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

        String keywordLike =
        (keyword == null || keyword.isBlank())
                ? null
                : "%" + keyword.trim() + "%";

    Page<ShipCodeBasicProjection> shipCodePage =
            draftDomesticShipmentRepository.findShipCodesByStaff(
                    staffId,
                    keywordLike,
                    payment,
                    pageable
            );

    if (shipCodePage.isEmpty()) {
        return List.of();
    }

    List<String> shipCodes =
            shipCodePage.stream()
                    .map(ShipCodeBasicProjection::getShipCode)
                    .toList();

    // 1️⃣ Shipments
    List<DraftDomesticShipment> shipments =
            draftDomesticShipmentRepository.findByShipCodes(shipCodes);

    Map<String, List<DraftDomesticShipment>> shipCodeShipmentMap =
            shipments.stream()
                    .collect(Collectors.groupingBy(
                            s -> s.getDraftDomestic().getShipCode()
                    ));

    // 2️⃣ Warehouse
    List<String> trackingCodes =
            shipments.stream()
                    .map(DraftDomesticShipment::getShipmentCode)
                    .distinct()
                    .toList();

    Map<String, WarehouseShip> warehouseShipMap =
            warehouseRepository.findWarehouseShips(trackingCodes)
                    .stream()
                    .collect(Collectors.toMap(
                            WarehouseShip::getShipmentCode,
                            w -> w
                    ));

    // 3️⃣ Fee
    Map<String, List<String>> shipCodeTrackingMap =
            shipCodeShipmentMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue()
                                  .stream()
                                  .map(DraftDomesticShipment::getShipmentCode)
                                  .toList()
                    ));

    Map<String, BigDecimal> feeMap =
            partialShipmentService
                    .calculateFeeByShipCodeAllowMultiRoute(shipCodeTrackingMap);

    List<ShipCodePayment> result = new ArrayList<>();

    for (ShipCodeBasicProjection base : shipCodePage) {

        String shipCode = base.getShipCode();

        List<WarehouseShip> warehouseShips =
                shipCodeShipmentMap.get(shipCode)
                        .stream()
                        .map(s -> warehouseShipMap.get(s.getShipmentCode()))
                        .filter(Objects::nonNull)
                        .toList();

        ShipCodePayment item = new ShipCodePayment();
        item.setShipCode(shipCode);
        item.setCustomerId(base.getCustomerId().toString());
        item.setCustomerName(base.getCustomerName());
        item.setWarehouseShips(warehouseShips);
        item.setTotalPriceShip(
                feeMap.getOrDefault(shipCode, BigDecimal.ZERO)
        );

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

public Slice<DraftDomesticResponse> getAllDraftDomestic(
        String customerCode,
        String shipmentCode,
        DraftDomesticStatus status,
        Carrier carrier,
        Pageable pageable
) {
    Account account = accountUtils.getAccountCurrent();
    Long staffId = null;

    if (account instanceof Staff staff &&
        (staff.getRole() == AccountRoles.STAFF_SALE
         || staff.getRole() == AccountRoles.LEAD_SALE)) {
        staffId = staff.getAccountId();
    }

    Slice<DraftDomesticResponse> slice =
            draftDomesticRepository.findDraftDomesticSlice(
                    customerCode,
                    status,
                    carrier,
                    staffId,
                    shipmentCode,
                    pageable
            );

    if (slice.isEmpty()) return slice;

    List<Long> draftIds = slice.getContent()
            .stream()
            .map(DraftDomesticResponse::getId)
            .toList();

    Map<Long, List<String>> shipmentMap =
            draftDomesticRepository
                    .findShipmentCodesByDraftIds(draftIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            r -> (Long) r[0],
                            Collectors.mapping(
                                    r -> (String) r[1],
                                    Collectors.toList()
                            )
                    ));

    slice.forEach(d ->
            d.setShippingList(
                    shipmentMap.getOrDefault(d.getId(), List.of())
            )
    );

    return slice;
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
            .orElseThrow(() ->
                    new NotFoundException("Không tìm thấy đơn mẫu vận chuyển nội địa"));

    if (shippingCodes == null || shippingCodes.isEmpty()) {
        throw new BadRequestException("Danh sách mã vận đơn không được rỗng");
    }

    checkDraftEditable(draft);
    validateAllTrackingCodesExist(shippingCodes);

    // 1️⃣ Lấy shipmentCode đã tồn tại (DB, không load entity nặng)
    List<String> existingCodes =
            draftDomesticShipmentRepository.findCodesByDraftId(draftId);

    Set<String> existingSet = existingCodes.stream()
            .map(String::trim)
            .collect(Collectors.toSet());

    // 2️⃣ Thêm shipment mới
    List<DraftDomesticShipment> newShipments = new ArrayList<>();

    for (String code : shippingCodes) {
        String trimmed = code.trim();
        if (!existingSet.contains(trimmed)) {
            DraftDomesticShipment shipment = new DraftDomesticShipment();
            shipment.setDraftDomestic(draft);
            shipment.setShipmentCode(trimmed);
            newShipments.add(shipment);
        }
    }

    if (!newShipments.isEmpty()) {
        draftDomesticShipmentRepository.saveAll(newShipments);
    }

    // 3️⃣ Tính lại weight (query trực tiếp, không qua entity)
    List<String> allCodes = new ArrayList<>(existingSet);
    newShipments.forEach(s -> allCodes.add(s.getShipmentCode()));

    Double sumWeight =
            warehouseRepository.sumWeightByTrackingCodes(allCodes);

    Double weight = calculateAndRoundWeight(sumWeight);
    draft.setWeight(weight);

    // 4️⃣ Update shipCode
    draft.setShipCode(
            draft.getCustomer().getCustomerCode() + "-" + allCodes.size()
    );

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
            .orElseThrow(() ->
                    new NotFoundException("Không tìm thấy mẫu vận chuyển nội địa"));

    checkDraftEditable(draft);

    if (shippingCodes == null || shippingCodes.isEmpty()) {
        throw new BadRequestException("Danh sách mã vận đơn cần xóa không được rỗng");
    }

    Set<String> removeSet = shippingCodes.stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());

    if (removeSet.isEmpty()) {
        throw new BadRequestException("Danh sách mã vận đơn không hợp lệ");
    }

    // 1️⃣ Xóa shipment trong DB
    int deleted =
            draftDomesticShipmentRepository
                    .deleteByDraftIdAndCodes(draftId, removeSet);

    if (deleted == 0) {
        throw new BadRequestException("Không có mã vận đơn nào được xóa");
    }

    // 2️⃣ Kiểm tra còn shipment không
    long remainCount =
            draftDomesticShipmentRepository.countByDraftDomesticId(draftId);

    // ❗ Không còn shipment → xoá draft
    if (remainCount == 0) {
        draftDomesticRepository.deleteById(draftId);
        return null;
    }

    // 3️⃣ Còn shipment → cập nhật lại weight + shipCode
    List<String> remainCodes =
            draftDomesticShipmentRepository.findCodesByDraftId(draftId);

    Double sumWeight =
            warehouseRepository.sumWeightByTrackingCodes(remainCodes);

    draft.setWeight(calculateAndRoundWeight(sumWeight));
    draft.setShipCode(
            draft.getCustomer().getCustomerCode()
                    + "-" + remainCodes.size()
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
    draftDomesticShipmentRepository.deleteByDraftDomesticId(draftId);
    draftDomesticRepository.delete(draft);
    return true;
}


@Transactional
public Boolean exportDraftDomestic(List<Long> draftIds) {

    if (draftIds == null || draftIds.isEmpty()) {
        throw new BadRequestException("Danh sách draftId không được rỗng");
    }

    // 1️⃣ Lấy draft
    List<DraftDomestic> drafts =
            draftDomesticRepository.findAllById(draftIds);

    if (drafts.size() != draftIds.size()) {
        throw new BadRequestException("Có mẫu vận chuyển nội địa không tồn tại");
    }

    // 2️⃣ Check trạng thái LOCKED
    for (DraftDomestic draft : drafts) {
        if (draft.getStatus() != DraftDomesticStatus.LOCKED) {
            throw new BadRequestException(
                "Draft " + draft.getId() + " không ở trạng thái LOCKED"
            );
        }
    }

    // 3️⃣ Lấy toàn bộ trackingCode từ bảng shipment
    List<Object[]> rows =
            draftDomesticShipmentRepository
                    .findShipmentCodesByDraftIds(draftIds);

    if (rows.isEmpty()) {
        throw new BadRequestException(
            "Danh sách draft không có trackingCode"
        );
    }

    // Map draftId -> trackingCodes
    Map<Long, Set<String>> draftTrackingMap = new HashMap<>();
    Set<String> allTrackingCodes = new HashSet<>();

    for (Object[] row : rows) {
        Long draftId = (Long) row[0];
        String code = (String) row[1];

        draftTrackingMap
                .computeIfAbsent(draftId, k -> new HashSet<>())
                .add(code);

        allTrackingCodes.add(code);
    }

    // 4️⃣ Check draft nào KHÔNG có shipment
    for (Long draftId : draftIds) {
        if (!draftTrackingMap.containsKey(draftId)
            || draftTrackingMap.get(draftId).isEmpty()) {
            throw new BadRequestException(
                "DraftDomestic ID " + draftId + " có danh sách trackingCode trống"
            );
        }
    }

    // 5️⃣ Check Warehouse ở trạng thái CHO_GIAO
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
            "Các trackingCode không hợp lệ hoặc không ở CHO_GIAO: "
                + invalidCodes
        );
    }

    // 6️⃣ Update trạng thái draft → EXPORTED
    drafts.forEach(d ->
            d.setStatus(DraftDomesticStatus.EXPORTED)
    );

    draftDomesticRepository.saveAll(drafts);

    return true;
}

public List<DraftDomesticResponse> getLockedDraftNotExported(
        LocalDate endDate,
        Long staffId,
        Carrier carrier
) {
    if (endDate == null) {
        endDate = LocalDate.now();
    }

    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    LocalDateTime startDateTime = endDate.minusDays(30).atStartOfDay();

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

    // 1️⃣ Lấy các draft liên quan tới shipmentCodes
    List<DraftDomestic> drafts =
            draftDomesticRepository
                    .findDraftByShipmentCodes(paidShipmentCodes);

    if (drafts.isEmpty()) {
        return;
    }

    for (DraftDomestic draft : drafts) {

        // 2️⃣ Lấy shipmentCode của draft (query DB, không load collection)
        List<String> trackingCodes =
                draftDomesticShipmentRepository
                        .findCodesByDraftId(draft.getId());

        if (trackingCodes.isEmpty()) {
            continue;
        }

        // 3️⃣ Check warehouse
        long total =
                warehouseRepository
                        .countByTrackingCodeIn(trackingCodes);

        long ready =
                warehouseRepository
                        .countByTrackingCodeInAndStatus(
                                trackingCodes,
                                WarehouseStatus.CHO_GIAO
                        );

        // 4️⃣ Nếu tất cả đều CHO_GIAO → LOCK
        if (total > 0 && total == ready) {
            draft.setStatus(DraftDomesticStatus.LOCKED);
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
            draftDomesticRepository
                    .findByStatus(DraftDomesticStatus.DRAFT);

    if (drafts.isEmpty()) return;

    for (DraftDomestic draft : drafts) {

        // 1️⃣ Lấy shipmentCode từ DB (không dùng shippingList)
        List<DraftDomesticShipment> shipments =
                draftDomesticShipmentRepository
                        .findByDraftId(draft.getId());

        if (shipments.isEmpty()) {
            continue;
        }

        List<String> shipmentCodes =
                shipments.stream()
                        .map(DraftDomesticShipment::getShipmentCode)
                        .toList();

        // 2️⃣ Load OrderLinks
        List<OrderLinks> links =
                orderLinksRepository
                        .findByShipmentCodeIn(shipmentCodes);

        // Nếu thiếu link → bỏ qua
        if (links.size() != shipmentCodes.size()) {
            continue;
        }

        // 3️⃣ Check ALL điều kiện
        boolean allReady = links.stream().allMatch(link ->
                link.getStatus() == OrderLinkStatus.CHO_GIAO
                && link.getWarehouse() != null
                && link.getWarehouse().getStatus() == WarehouseStatus.CHO_GIAO
        );

        if (!allReady) {
            continue;
        }

        // 4️⃣ LOCK draft (KHÔNG sync string nữa)
        draft.setStatus(DraftDomesticStatus.LOCKED);
        draftDomesticRepository.save(draft);

        log.info(
            "🔒 DraftDomestic {} LOCKED by nightly job",
            draft.getId()
        );
    }
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
                "Các mã vận đơn không hợp lệ: "
                        + invalidCodes
        );
    }

        List<WarehouseStatus> statuses =
            warehouseRepository.findDistinctStatusesByTrackingCodes(
                    new ArrayList<>(inputCodes)
            );

    if (statuses.size() != 1) {
        throw new BadRequestException(
                "Các mã vận đơn không cùng trạng thái. Trạng thái tìm thấy: " + statuses
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
    if (draft.getStatus() != DraftDomesticStatus.WAIT_IMPORT) {
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

@Transactional
public void syncDraftDomesticStatus(String shipCode) {

    DraftDomestic draft = draftDomesticRepository
        .findByShipCode(shipCode)
        .orElse(null);

    List<String> shipmentCodes =
            draftDomesticShipmentRepository
                    .findShipmentCodesByShipCode(shipCode);

    if (shipmentCodes.isEmpty()) {
        return;
    }

    boolean existsNotImported =
            orderLinksRepository
                    .existsByShipmentCodeInAndStatusNot(
                            shipmentCodes,
                            OrderLinkStatus.DA_NHAP_KHO_VN
                    );

    if (!existsNotImported) {
        draft.setStatus(DraftDomesticStatus.DRAFT);
        draftDomesticRepository.save(draft);
    }
}




}
