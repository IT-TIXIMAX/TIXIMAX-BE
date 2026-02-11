package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.Domestic.ScanToShip;
import com.tiximax.txm.Model.DTOResponse.Domestic.CheckInDomestic;
import com.tiximax.txm.Model.DTOResponse.Domestic.DomesticDelivery;
import com.tiximax.txm.Model.DTOResponse.Domestic.DomesticRecieve;
import com.tiximax.txm.Model.DTOResponse.Domestic.DomesticResponse;
import com.tiximax.txm.Model.DTOResponse.Domestic.DomesticSend;
import com.tiximax.txm.Model.EnumFilter.DeliveryStatus;
import com.tiximax.txm.Model.Projections.CustomerDeliveryRow;
import com.tiximax.txm.Model.Projections.CustomerShipmentRow;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Repository.DomesticRepository;
import com.tiximax.txm.Repository.DraftDomesticRepository;
import com.tiximax.txm.Repository.DraftDomesticShipmentRepository;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PackingRepository;
import com.tiximax.txm.Repository.PartialShipmentRepository;
import com.tiximax.txm.Repository.WarehouseLocationRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DomesticService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private DomesticRepository domesticRepository;
    @Autowired
    private PackingRepository packingRepository;
    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private DraftDomesticRepository draftDomesticRepository;
    @Autowired
    private DraftDomesticService draftDomesticService;
    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;
    @Autowired
    private PartialShipmentRepository partialShipmentRepository;
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private DraftDomesticShipmentRepository draftDomesticShippingListRepository;
    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Transactional
    public DomesticRecieve createDomesticForWarehousing(List<String> packingCodes, String note) {
    Staff staff = getCurrentStaffWithLocation();
    List<Packing> packings = packingRepository.findAllByPackingCodeIn(packingCodes);
    if (packings.isEmpty()) {
        throw new BadRequestException("Không tìm thấy packing nào trong danh sách cung cấp!");
    }

    for (Packing packing : packings) {
        if (packing.getStatus() != PackingStatus.DA_BAY) {
            throw new BadRequestException("Packing " + packing.getPackingCode() + " chưa đúng trạng thái nhập kho!");
        }
    }
    Packing firstPacking = packings.get(0);
    Set<Warehouse> warehouses = firstPacking.getWarehouses();
    if (warehouses.isEmpty()) {
        throw new BadRequestException("Packing " + firstPacking.getPackingCode() + " không được liên kết với kho nước ngoài!");
    }
    Warehouse firstWarehouse = warehouses.iterator().next();
    WarehouseLocation fromLocation = firstWarehouse.getLocation();
    if (fromLocation == null) {
        throw new BadRequestException("Kho nước ngoài của packing " + firstPacking.getPackingCode() + " không được tìm thấy!");
    }
        List<String> shipmentCodes = packings.stream()
                .flatMap(p -> p.getPackingList().stream())
                .filter(code -> code != null && !code.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCodeIn(shipmentCodes);
    for (OrderLinks orderLink : orderLinks) {
        if (orderLink.getStatus() == OrderLinkStatus.DANG_CHUYEN_VN) {
           orderLink.setStatus(OrderLinkStatus.CHO_NHAP_KHO_VN);
    }
}
    orderLinksRepository.saveAll(orderLinks);
    updateOrderStatusIfAllLinksReady(orderLinks);

    for (Packing packing : packings) {
        packing.setStatus(PackingStatus.DA_NHAP_KHO_VN);

    }
    packingRepository.saveAll(packings);
    Domestic domestic = createDomestic(
            fromLocation,
            staff.getWarehouseLocation(),
            DomesticStatus.NHAN_HANG,
            staff,
            note,
            new HashSet<>(packings),
            packings.stream()
                    .map(Packing::getPackingCode)
                    .toList()
    );
    ordersService.addProcessLog(
            null,
            domestic.getDomesticId().toString(),
            ProcessLogAction.DA_NHAP_KHO_HN
    );
     return new DomesticRecieve(packings);
}
    public Domestic TranferPackingToWarehouse(List<String> packingCodes, Long toLocationId, String note) {
     Staff staff = getCurrentStaffWithLocation();

    WarehouseLocation toLocation = warehouseLocationRepository.findById(toLocationId)
        .orElseThrow(() -> new BadRequestException("Địa điểm kho đích không tồn tại!"));

    List<Packing> packings = packingRepository.findAllByPackingCodeIn(packingCodes);
    if (packings.isEmpty()) {
        throw new BadRequestException("Không tìm thấy packing nào trong danh sách cung cấp!");
    }

    Packing firstPacking = packings.get(0);
    Set<Warehouse> warehouses = firstPacking.getWarehouses();
    if (warehouses.isEmpty()) {
        throw new BadRequestException("Packing " + firstPacking.getPackingCode() + " không được liên kết với kho nước ngoài!");
    }
    Warehouse firstWarehouse = warehouses.iterator().next();
    WarehouseLocation fromLocation = firstWarehouse.getLocation();
    if (fromLocation == null) {
        throw new BadRequestException("Kho nước ngoài của packing " + firstPacking.getPackingCode() + " không được tìm thấy!");
    }

    List<String> shipmentCodes = packings.stream()
            .flatMap(p -> p.getPackingList().stream())
            .distinct()
            .collect(Collectors.toList());

    List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCodeIn(shipmentCodes);
     for (OrderLinks orderLink : orderLinks) {
    if (orderLink.getOrders()
        .getDestination()
        .getDestinationName()
        .toLowerCase()
        .contains(toLocation.getName().toLowerCase())) {
            throw new BadRequestException("Packing " + orderLink.getShipmentCode() + " thuộc đơn hàng có điểm đến Sài Gòn, vui lòng sử dụng chức năng chuyển kho Sài Gòn!");
        }
    } 
    Domestic domestic = createDomestic(
            fromLocation,
            toLocation,
            DomesticStatus.CHUYEN_KHO,
            staff,
            note,
            new HashSet<>(packings),
            packings.stream()
                    .map(Packing::getPackingCode)
                    .toList()
    );

    ordersService.addProcessLog(
            null,
            domestic.getDomesticId().toString(),
            ProcessLogAction.DA_NHAP_KHO_SG
    );

    return domestic;
}
    public Domestic RecievedPackingFromWarehouse(Long domesticId) {
    Staff staff = getCurrentStaffWithLocation();
    var domestic = domesticRepository.findById(domesticId).orElseThrow(() -> new BadRequestException("Domestic không tồn tại!"));
    List<String> shipmentCodes = domestic.getShippingList();
    List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCodeIn(shipmentCodes);
    if (orderLinks.isEmpty()) {
        throw new BadRequestException("Không tìm thấy đơn hàng trong danh sách cung cấp!"); 
    }
    for (OrderLinks orderLink : orderLinks) {
        if (orderLink.getStatus() == OrderLinkStatus.CHO_TRUNG_CHUYEN) {
            orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_VN);
        }
    }
    orderLinksRepository.saveAll(orderLinks);
    updateOrderStatusIfAllLinksReady(orderLinks);
    domestic.setStatus(DomesticStatus.SAN_SANG_GIAO);
    domestic = domesticRepository.save(domestic);
    return domestic;
    }
 @Transactional
public List<DomesticResponse> transferByCustomerCode(
        String customerCode,
        String VNPostTrackingCode
) {

    List<Warehouse> warehouses =
            warehouseRepository.findWarehousesByOrderLinkStatus(
                    OrderLinkStatus.CHO_GIAO,
                    customerCode
            );

    if (warehouses.isEmpty()) {
        return Collections.emptyList();
    }

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
    WarehouseLocation currentLocation =
            currentStaff != null ? currentStaff.getWarehouseLocation() : null;

    Map<Address, List<Warehouse>> byAddress = warehouses.stream()
            .filter(w -> w.getOrders().getAddress() != null)
            .collect(Collectors.groupingBy(w -> w.getOrders().getAddress()));

    List<Domestic> domestics = new ArrayList<>();

    for (Map.Entry<Address, List<Warehouse>> entry : byAddress.entrySet()) {
        Address address = entry.getKey();
        List<Warehouse> ws = entry.getValue();

        Set<Packing> packings = ws.stream()
                .map(Warehouse::getPacking)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> shippingList = ws.stream()
                .map(Warehouse::getTrackingCode)
                .toList();

        Domestic domestic = createDomestic(
                currentLocation,
                null,
                DomesticStatus.DA_GIAO,
                currentStaff,
                "Giao hàng cho khách (Mã: " + customerCode + ")",
                packings,
                shippingList
        );

        domestic.setToAddress(address);
        domestic.setCarrier(Carrier.VNPOST);
        domestic.setCarrierTrackingCode(VNPostTrackingCode);
        domesticRepository.save(domestic);

        updateOrderLinksAndOrders(shippingList, domestic);

        warehouseRepository.updateWarehouseStatusByTrackingCodes(
                WarehouseStatus.DA_GIAO,
                shippingList
        );

        domestics.add(domestic);
    }

    return domestics.stream()
            .map(DomesticResponse::fromEntity)
            .toList();
}

    public List<DomesticSend> previewTransferByCustomerCode(String customerCode) {

   List<Warehouse> warehouses =
            warehouseRepository.findWarehousesByOrderLinkStatus(
                    OrderLinkStatus.CHO_GIAO,
                    customerCode
            );

    if (warehouses.isEmpty()) {
        return Collections.emptyList();
    }

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();

    Map<Address, List<Warehouse>> byAddress =
            warehouses.stream()
                    .filter(w -> w.getOrders().getAddress() != null)
                    .collect(Collectors.groupingBy(w -> w.getOrders().getAddress()));

    List<DomesticSend> result = new ArrayList<>();

    for (Map.Entry<Address, List<Warehouse>> entry : byAddress.entrySet()) {
        Address address = entry.getKey();
        List<Warehouse> ws = entry.getValue();

        List<String> trackingCodes = ws.stream()
                .map(Warehouse::getTrackingCode)
                .toList();
        Domestic domestic = new Domestic();
        domestic.setToAddress(address);
        domestic.setShippingList(trackingCodes);
        domestic.setStaff(currentStaff);
        domestic.setTimestamp(LocalDateTime.now());
        domestic.setNote("Xem trước giao hàng - CHỜ GIAO");
        result.add(new DomesticSend(domestic));
    }

    return result;
}

    private void updateOrderStatusIfAllLinksReady(List<OrderLinks> orderLinks) {
        Map<Orders, List<OrderLinks>> orderToLinksMap = orderLinks.stream()
                .collect(Collectors.groupingBy(OrderLinks::getOrders));

        for (Map.Entry<Orders, List<OrderLinks>> entry : orderToLinksMap.entrySet()) {
            Orders order = entry.getKey();

            Set<OrderLinks> allOrderLinks = order.getOrderLinks();

            boolean allLinksReady = allOrderLinks.stream()
                    .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN);

            if (allLinksReady) {
                order.setStatus(OrderStatus.DA_DU_HANG);
                ordersRepository.save(order);
            }
        }
    }

    public Optional<Domestic> getDomesticById(Long id) {
        return domesticRepository.findById(id);
    }
   public List<DomesticResponse> getDomesticDeliveredOnDaily() {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

    List<Domestic> domestics = domesticRepository.findDeliveredToday(
        DomesticStatus.DA_GIAO, startOfDay, endOfDay
    );

    return domestics.stream()
            .map(DomesticResponse::fromEntity)
            .collect(Collectors.toList());
}
  
 @Transactional
public boolean scanImportToDomestic(String shipmentCode) {

    Warehouse warehouse = warehouseRepository
            .findByTrackingCode(shipmentCode)
            .orElseThrow(() ->
                    new NotFoundException("Không tìm thấy kiện hàng!")
            );

    List<OrderLinks> orderLinks =
            orderLinksRepository.findByShipmentCode(shipmentCode);

    if (orderLinks.isEmpty()) {
        throw new NotFoundException("Không tìm thấy đơn hàng trong kiện!");
    }

    // 1️⃣ Validate OrderLink status
    boolean hasInvalidOrderLinkStatus = orderLinks.stream()
            .anyMatch(ol ->
                    ol.getStatus() != OrderLinkStatus.CHO_NHAP_KHO_VN
                 && ol.getStatus() != OrderLinkStatus.CHO_GIAO
            );

    if (hasInvalidOrderLinkStatus) {
        throw new BadRequestException(
                "Có đơn hàng không hợp lệ để scan!"
        );
    }

    if (warehouse.getStatus() == WarehouseStatus.CHO_GIAO) {
        return true;
    }

  
    if (warehouse.getStatus() == WarehouseStatus.DA_NHAP_KHO_NN) {
        warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO_VN);

        orderLinks.forEach(ol -> {
            if (ol.getStatus() == OrderLinkStatus.CHO_NHAP_KHO_VN) {
                ol.setStatus(OrderLinkStatus.DA_NHAP_KHO_VN);
            }
        });

        warehouse.setArrivalTime(LocalDateTime.now());

        orderLinksRepository.saveAll(orderLinks);

        updateOrderStatusIfAllLinksReady(orderLinks);

        draftDomesticService.syncDraftDomesticStatus(shipmentCode);

        return true;
    }

    // 4️⃣ Các trạng thái warehouse khác → không cho scan
    throw new BadRequestException(
            "Trạng thái kiện hàng không hợp lệ để scan!"
    );
}



    public CheckInDomestic getCheckInDomestic(String shipmentCode) {

        List<OrderLinks> orderLinks =
                orderLinksRepository.findByShipmentCode(shipmentCode);

        if (orderLinks.isEmpty()) {
            throw new NotFoundException(
                    "Không tìm thấy đơn hàng trong danh sách cung cấp!"
            );
        }
        OrderLinks orderLink = orderLinks.get(0);
        if (orderLink.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN) {
            throw new BadRequestException(
                    "Đơn hàng đã được nhập kho Việt Nam!"
            );
        }
        if (orderLink.getStatus() == OrderLinkStatus.DANG_CHUYEN_VN) {
            throw new BadRequestException(
                    "Đơn hàng đã đến Việt Nam nhưng bạn chưa nhận thùng hàng vào kho!"
            );
        }
        Warehouse warehouse = orderLink.getWarehouse();
        if (warehouse == null) {
            throw new BadRequestException("OrderLink chưa được gán kho!");
        }
        Packing packing = warehouse.getPacking();
        if (packing == null) {
            throw new BadRequestException("Warehouse chưa thuộc packing nào!");
        }
        String flightCode = packing.getFlightCode();

        int totalWarehouseInFlight =
                warehouseRepository.countByFlightCode(flightCode);

        Orders order = orderLink.getOrders();
        Staff staff = order.getStaff();
        Customer customer = order.getCustomer();

        CheckInDomestic checkInDomestic = new CheckInDomestic();
        checkInDomestic.setOrderCode(order.getOrderCode());
        checkInDomestic.setShipmentCode(orderLink.getShipmentCode());
        checkInDomestic.setCustomerCode(
                staff.getStaffCode() + " - " + customer.getCustomerCode()
        );
        checkInDomestic.setDestinationName(
                order.getDestination().getDestinationName()
        );
        checkInDomestic.setWaitImport(
                warehouseRepository.countNotImportedByCustomerAndFlight(
                    customer.getAccountId(),
                    flightCode,
                    OrderLinkStatus.CHO_NHAP_KHO_VN
                )
        );
        checkInDomestic.setImported(
                warehouseRepository.countImportedByCustomerAndFlight(
                    customer.getAccountId(),
                    flightCode,
                    OrderLinkStatus.DA_NHAP_KHO_VN
                )
        );

        checkInDomestic.setInventory(
                warehouseRepository.countInventoryByCustomer(
                        customer.getAccountId(),
                        OrderLinkStatus.DA_NHAP_KHO_VN
                )
        );
        checkInDomestic.setTotalWarehouseInFlight(totalWarehouseInFlight);
        // updateOrderStatusIfAllLinksReady(orderLinks);
        return checkInDomestic;
    }

    public Page<DomesticDelivery> getDomesticDeliveryByCustomerPaged(
            DeliveryStatus status,
            String customerCode,
            Long staffId,
            Pageable pageable
    ) {
        OrderLinkStatus orderLinkStatus = switch (status) {
            case CHUA_DU_DIEU_KIEN -> OrderLinkStatus.DA_NHAP_KHO_VN;
            case DU_DIEU_KIEN -> OrderLinkStatus.CHO_GIAO;
        };

        String filterCustomerCode =
                (customerCode == null || customerCode.isBlank())
                        ? null
                        : customerCode.trim().toUpperCase();

        if (filterCustomerCode != null &&
            !customerRepository.existsByCustomerCode(filterCustomerCode)) {
            throw new BadRequestException("Mã khách hàng không tồn tại!");
        }

        Page<CustomerDeliveryRow> customerPage =
                warehouseRepository.findDomesticDelivery(
                        orderLinkStatus,
                        filterCustomerCode,
                        staffId,
                        pageable
                );

        if (customerPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<String> customerCodes = customerPage.getContent().stream()
                .map(CustomerDeliveryRow::getCustomerCode)
                .filter(Objects::nonNull)
                .toList();

        List<CustomerShipmentRow> rows =
                warehouseRepository.findShipmentCodesByCustomerCodes(
                        orderLinkStatus,
                        customerCodes,
                        staffId
                );

        Map<String, List<String>> shipmentMap = new HashMap<>();
        for (CustomerShipmentRow r : rows) {
            shipmentMap
                .computeIfAbsent(r.getCustomerCode(), k -> new ArrayList<>())
                .add(r.getTrackingCode());
        }

        List<DomesticDelivery> result = new ArrayList<>();
        for (CustomerDeliveryRow c : customerPage.getContent()) {
            result.add(new DomesticDelivery(
                    c.getCustomerCode(),
                    c.getCustomerName(),
                    c.getPhoneNumber(),
                    c.getAddress(),
                    c.getStaffName(),
                    c.getStaffCode(),
                    status.name(),
                    shipmentMap.getOrDefault(c.getCustomerCode(), List.of())
            ));
        }

        return new PageImpl<>(result, pageable, customerPage.getTotalElements());
    }


    @Transactional
public DomesticDelivery scanToShip(
        ScanToShip request
) {
    DraftDomestic draftDomestic = null;

    if (draftDomestic == null && request.getShipCode() != null && !request.getShipCode().isBlank()) {
        draftDomestic =
                draftDomesticRepository
                        .findByShipCode(request.getShipCode())
                        .orElse(null);
    }

    if (draftDomestic == null) {
        throw new NotFoundException(
                "Không tìm thấy Draft Domestic theo mã vận chuyển hoặc shipCode"
        );
    }
    if( draftDomestic.getCarrier() != request.getCarrier()) {
        throw new BadRequestException(
                "Đơn hàng này giao theo vận chuyển " + request.getCarrier()
        );
    }

    if (draftDomestic.getCarrier() != request.getCarrier()) {
        throw new BadRequestException(
                "Draft Domestic không thuộc carrier " + request.getCarrier()
        );
    }

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
    WarehouseLocation currentLocation =
            currentStaff != null ? currentStaff.getWarehouseLocation() : null;

    checkAndUpdateWarehousesAndOrderLinks(draftDomestic);

    Domestic domestic = new Domestic();
    domestic.setAddress(draftDomestic.getAddress());
    domestic.setPhoneNumber(draftDomestic.getPhoneNumber());
    domestic.setShippingList(draftDomestic.getShipments().stream()
            .map(DraftDomesticShipment::getShipmentCode)
            .toList());
    domestic.setFromLocation(currentLocation);
    domestic.setCarrier(draftDomestic.getCarrier());
    domestic.setCarrierTrackingCode(request.getTrackingCode());
    domestic.setShipCode(draftDomestic.getShipCode());
    domestic.setTimestamp(LocalDateTime.now());
    domestic.setNote(
            "Giao hàng cho khách bằng " + request.getCarrier() +
            (request.getTrackingCode() != null ? " - mã " + request.getTrackingCode() : "")
    );
    domestic.setStatus(DomesticStatus.DA_GIAO);
    domestic.setCustomer(draftDomestic.getCustomer());
    domestic.setStaff(getCurrentStaffWithLocation());

    domesticRepository.save(domestic);

    draftDomesticShippingListRepository.deleteByDraftDomesticId(draftDomestic.getId());
    draftDomesticRepository.deleteById(draftDomestic.getId());

    return new DomesticDelivery(
            domestic.getCustomer().getCustomerCode(),
            domestic.getCustomer().getName(),
            domestic.getPhoneNumber(),
            domestic.getAddress(),
            domestic.getStaff().getName(),
            domestic.getStaff().getStaffCode(),
            domestic.getStatus().name(),
            domestic.getShippingList()
    );
}

    private Staff getCurrentStaffWithLocation() {
    Staff staff = (Staff) accountUtils.getAccountCurrent();
    if (staff == null || staff.getWarehouseLocation() == null) {
        if (!staff.getRole().equals(AccountRoles.STAFF_SALE) && !staff.getRole().equals(AccountRoles.LEAD_SALE)){
            throw new BadRequestException(
                    "Nhân viên hiện tại chưa được gán địa điểm kho!"
            );
        }
    }
    return staff;
}

    private void checkAndUpdateWarehousesAndOrderLinks(DraftDomestic draftDomestic) {

    List<String> trackingCodes = draftDomestic.getShipments().stream()
            .map(DraftDomesticShipment::getShipmentCode)
            .toList();
    if (trackingCodes == null || trackingCodes.isEmpty()) {
        throw new BadRequestException("Danh sách trackingCode trống");
    }

    List<Warehouse> warehouses =
            warehouseRepository.findByTrackingCodeInAndStatus(
                    trackingCodes,
                    WarehouseStatus.CHO_GIAO
            );

    if (warehouses.size() != trackingCodes.size()) {

        Set<String> validCodes = warehouses.stream()
                .map(Warehouse::getTrackingCode)
                .collect(Collectors.toSet());

        Set<String> invalidCodes = new HashSet<>(trackingCodes);
        invalidCodes.removeAll(validCodes);

        throw new BadRequestException(
                "Warehouse không hợp lệ hoặc không ở CHO_GIAO: " + invalidCodes
        );
    }

    for (Warehouse w : warehouses) {
        if (!w.getOrders().getCustomer().equals(draftDomestic.getCustomer())) {
            throw new BadRequestException(
                    "Warehouse không thuộc khách hàng hiện tại: "
                            + w.getTrackingCode()
            );
        }
    }

    int updatedWarehouse =
            warehouseRepository.updateStatusByTrackingCodes(
                    trackingCodes,
                    WarehouseStatus.CHO_GIAO,
                    WarehouseStatus.DA_GIAO
            );

    if (updatedWarehouse != trackingCodes.size()) {
        throw new BadRequestException("Cập nhật Warehouse không đầy đủ");
    }
            orderLinksRepository.updateStatusByShipmentCodes(
                    trackingCodes,
                    OrderLinkStatus.CHO_GIAO,
                    OrderLinkStatus.DA_GIAO
            );
        //
        entityManager.flush();
        entityManager.clear();
        ordersService.updateOrdersStatusAfterDeliveryByShipmentCodes(trackingCodes);   

}

    private Domestic createDomestic(
            WarehouseLocation from,
            WarehouseLocation to,
            DomesticStatus status,
            Staff staff,
            String note,
            Set<Packing> packings,
            List<String> shippingList
    ) {
        Domestic domestic = new Domestic();
        domestic.setFromLocation(from);
        domestic.setToLocation(to);
        domestic.setStatus(status);
        domestic.setTimestamp(LocalDateTime.now());
        domestic.setStaff(staff);
        domestic.setLocation(staff.getWarehouseLocation());
        domestic.setNote(note);
        domestic.setPackings(packings);
        domestic.setShippingList(shippingList);
        return domesticRepository.save(domestic);
    }

    private void updateOrderLinksAndOrders(List<String> shipmentCodes, Domestic domestic) {
    for (String shipmentCode : shipmentCodes) {
        List<OrderLinks> links = orderLinksRepository.findByShipmentCode(shipmentCode);
        for (OrderLinks link : links) {
            link.setStatus(OrderLinkStatus.DA_GIAO);
        }
        orderLinksRepository.saveAll(links);

        Set<Orders> orders = links.stream()
                .map(OrderLinks::getOrders)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Orders order : orders) {
            boolean allDelivered = order.getOrderLinks().stream()
                    .allMatch(l -> l.getStatus() == OrderLinkStatus.DA_GIAO || l.getStatus() == OrderLinkStatus.DA_HUY);

            if (allDelivered && order.getStatus() != OrderStatus.DA_GIAO) {
                order.setStatus(OrderStatus.DA_GIAO);
                ordersRepository.save(order);
                ordersService.addProcessLog(order, domestic.getDomesticId().toString(), ProcessLogAction.DA_GIAO);
            }
        }

        // Xử lý PartialShipment tương tự
        Set<PartialShipment> partials = links.stream()
                .map(OrderLinks::getPartialShipment)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (PartialShipment partial : partials) {
            boolean allDone = partial.getReadyLinks().stream()
                    .allMatch(l -> l.getStatus() == OrderLinkStatus.DA_GIAO || l.getStatus() == OrderLinkStatus.DA_HUY);
            if (allDone && partial.getStatus() != OrderStatus.DA_GIAO) {
                partial.setStatus(OrderStatus.DA_GIAO);
                partialShipmentRepository.save(partial);
                ordersService.addProcessLog(partial.getOrders(), "PS#" + partial.getId(), ProcessLogAction.DA_GIAO);
            }
        }
    }
}

}



