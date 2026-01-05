package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Model.CheckInDomestic;
import com.tiximax.txm.Model.DomesticDelivery;
import com.tiximax.txm.Model.DomesticRecieve;
import com.tiximax.txm.Model.DomesticResponse;
import com.tiximax.txm.Model.DomesticSend;
import com.tiximax.txm.Model.EnumFilter.DeliveryStatus;
import com.tiximax.txm.Repository.AddressRepository;
import com.tiximax.txm.Repository.DomesticRepository;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PackingRepository;
import com.tiximax.txm.Repository.PartialShipmentRepository;
import com.tiximax.txm.Repository.WarehouseLocationRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
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

    @Autowired
    private DomesticRepository domesticRepository;

    @Autowired
    private PackingRepository packingRepository;
    @Autowired
    private PartialShipmentService partialShipmentService;
    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;
    @Autowired
    private PartialShipmentRepository partialShipmentRepository;

    @Autowired
    private AddressRepository addressRepository; 
    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Transactional
    public DomesticRecieve createDomesticForWarehousing(List<String> packingCodes, String note) {
    Staff staff = (Staff) accountUtils.getAccountCurrent();
    if (staff == null || staff.getWarehouseLocation() == null) {
        throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
    }
    List<Packing> packings = packingRepository.findAllByPackingCodeIn(packingCodes);
    if (packings.isEmpty()) {
        throw new IllegalArgumentException("Không tìm thấy packing nào trong danh sách cung cấp!");
    }

    for (Packing packing : packings) {
        if (packing.getStatus() != PackingStatus.DA_BAY) {
            throw new IllegalArgumentException("Packing " + packing.getPackingCode() + " chưa đúng trạng thái nhập kho!");
        }
    }
    Packing firstPacking = packings.get(0);
    Set<Warehouse> warehouses = firstPacking.getWarehouses();
    if (warehouses.isEmpty()) {
        throw new IllegalArgumentException("Packing " + firstPacking.getPackingCode() + " không được liên kết với kho nước ngoài!");
    }
    Warehouse firstWarehouse = warehouses.iterator().next();
    WarehouseLocation fromLocation = firstWarehouse.getLocation();
    if (fromLocation == null) {
        throw new IllegalArgumentException("Kho nước ngoài của packing " + firstPacking.getPackingCode() + " không được tìm thấy!");
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

    Domestic domestic = new Domestic();
    domestic.setFromLocation(fromLocation);
    domestic.setToLocation(staff.getWarehouseLocation());
    domestic.setStatus(DomesticStatus.NHAN_HANG);
    domestic.setTimestamp(LocalDateTime.now());
    domestic.setStaff(staff);
    domestic.setLocation(staff.getWarehouseLocation());
    domestic.setNote(note);
    Set<Packing> packingSet = new HashSet<>(packings);
    domestic.setPackings(packingSet);
    domestic.setShippingList(packings.stream()
            .map(Packing::getPackingCode)
            .collect(Collectors.toList()));
    domestic = domesticRepository.save(domestic);
    ordersService.addProcessLog(null, domestic.getDomesticId().toString(), ProcessLogAction.DA_NHAP_KHO_HN);
    DomesticRecieve domesticRecieve = new DomesticRecieve(packings);
    return domesticRecieve;
}
    public Domestic TranferPackingToWarehouse(List<String> packingCodes, Long toLocationId, String note) {
     Staff staff = (Staff) accountUtils.getAccountCurrent();
    if (staff == null || staff.getWarehouseLocation() == null) {
        throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!");
    }
    WarehouseLocation toLocation = warehouseLocationRepository.findById(toLocationId)
        .orElseThrow(() -> new IllegalArgumentException("Địa điểm kho đích không tồn tại!"));

    List<Packing> packings = packingRepository.findAllByPackingCodeIn(packingCodes);
    if (packings.isEmpty()) {
        throw new IllegalArgumentException("Không tìm thấy packing nào trong danh sách cung cấp!");
    }

    Packing firstPacking = packings.get(0);
    Set<Warehouse> warehouses = firstPacking.getWarehouses();
    if (warehouses.isEmpty()) {
        throw new IllegalArgumentException("Packing " + firstPacking.getPackingCode() + " không được liên kết với kho nước ngoài!");
    }
    Warehouse firstWarehouse = warehouses.iterator().next();
    WarehouseLocation fromLocation = firstWarehouse.getLocation();
    if (fromLocation == null) {
        throw new IllegalArgumentException("Kho nước ngoài của packing " + firstPacking.getPackingCode() + " không được tìm thấy!");
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
            throw new IllegalArgumentException("Packing " + orderLink.getShipmentCode() + " thuộc đơn hàng có điểm đến Sài Gòn, vui lòng sử dụng chức năng chuyển kho Sài Gòn!");
        }
    } 
    Domestic domestic = new Domestic();
    domestic.setFromLocation(fromLocation);
    domestic.setToLocation(toLocation);
    domestic.setStatus(DomesticStatus.CHUYEN_KHO);
    domestic.setTimestamp(LocalDateTime.now());
    domestic.setStaff(staff);
    domestic.setLocation(staff.getWarehouseLocation());
    domestic.setNote(note);
    Set<Packing> packingSet = new HashSet<>(packings);
    domestic.setPackings(packingSet);
    domestic.setShippingList(packings.stream()
            .map(Packing::getPackingCode)
            .collect(Collectors.toList()));
    domestic = domesticRepository.save(domestic);
    ordersService.addProcessLog(null, domestic.getDomesticId().toString(), ProcessLogAction.DA_NHAP_KHO_SG);
    return domestic;
}
    public Domestic RecievedPackingFromWarehouse(Long domesticId) {
    Staff staff = (Staff) accountUtils.getAccountCurrent();    
    if (staff == null || staff.getWarehouseLocation() == null) {
        throw new IllegalArgumentException("Nhân viên hiện tại chưa được gán địa điểm kho!"); 
    }
    var domestic = domesticRepository.findById(domesticId).orElseThrow(() -> new IllegalArgumentException("Domestic không tồn tại!"));
    List<String> shipmentCodes = domestic.getShippingList();
    List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCodeIn(shipmentCodes);
    if (orderLinks.isEmpty()) {
        throw new IllegalArgumentException("Không tìm thấy đơn hàng trong danh sách cung cấp!"); 
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

    public List<Map<String, Object>> getReadyForDeliveryOrdersByCustomerCode(String customerCode) {
    List<Map<String, Object>> allReadyData = getReadyForDeliveryOrders(Pageable.unpaged());
    if (allReadyData.isEmpty()) {
        return Collections.emptyList();
    }

    return allReadyData.stream()
            .filter(customerData -> {
                String name = (String) customerData.get("customerName");
                List<Map<String, Object>> addresses = (List<Map<String, Object>>) customerData.get("addresses");
                if (addresses == null || addresses.isEmpty()) return false;
                
                List<Map<String, Object>> packings = (List<Map<String, Object>>) addresses.get(0).get("packings");
                if (packings == null || packings.isEmpty()) return false;

                String firstPackingCode = (String) packings.get(0).get("packingCode");

                return packingRepository.findByPackingCode(firstPackingCode)
                        .flatMap(packing -> packing.getWarehouses().stream().findFirst())
                        .map(Warehouse::getOrders)
                        .map(Orders::getCustomer)
                        .map(Customer::getCustomerCode)
                        .map(code -> code.equalsIgnoreCase(customerCode))
                        .orElse(false);
            })
            .collect(Collectors.toList());
}
public List<DomesticResponse> transferByShipmentCode(List<String> shipmentCodes) {
    // Lấy dữ liệu đơn hàng sẵn sàng giao
    List<Map<String, Object>> readyData = getReadyForDeliveryOrdersToShip(shipmentCodes);
    if (readyData.isEmpty()) {
        return Collections.emptyList();  
    }

    Optional<Map<String, Object>> customerDataOpt = readyData.stream()
            .filter(data -> isCustomerMatching(data))
            .findFirst();  

    if (customerDataOpt.isEmpty()) {
        return Collections.emptyList();  
    }

    Map<String, Object> customerData = customerDataOpt.get();
    String customerName = (String) customerData.get("customerName");
    List<Map<String, Object>> addresses = (List<Map<String, Object>>) customerData.get("addresses");

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
    WarehouseLocation currentLocation = currentStaff != null ? currentStaff.getWarehouseLocation() : null;
    
    List<Domestic> createdDomestics = new ArrayList<>();

    for (Map<String, Object> addressData : addresses) {
        Long addressId = ((Number) addressData.get("addressId")).longValue();
        Address address = addressRepository.findById(addressId).orElse(null);
        if (address == null) continue;  

        List<Map<String, Object>> packingsData = (List<Map<String, Object>>) addressData.get("packings");
        if (packingsData == null || packingsData.isEmpty()) continue;  

        Set<Packing> packingSet = new HashSet<>();
        List<String> shippingList = new ArrayList<>();

        for (Map<String, Object> packingData : packingsData) {
            String packingCode = (String) packingData.get("packingCode");
            Packing packing = packingRepository.findByPackingCode(packingCode).orElse(null);
            if (packing == null) continue;  

            packingSet.add(packing);
            Set<String> trackingCodes = (Set<String>) packingData.get("trackingCodes");
            if (trackingCodes != null) {
                shippingList.addAll(trackingCodes);  
            }
        }

        if (packingSet.isEmpty()) continue;

        // Tạo một entry Domestic mới
        Domestic domestic = new Domestic();
        domestic.setPackings(packingSet);
        domestic.setShippingList(shippingList);
        domestic.setStatus(DomesticStatus.DA_GIAO);  
        domestic.setTimestamp(LocalDateTime.now());
        domestic.setNote("Giao hàng cho khách: " + customerName + " (Mã: " + customerData.get("customerCode") + ")");
        domestic.setToAddress(address);
        domestic.setStaff(currentStaff);
        domestic.setFromLocation(currentLocation);
        domestic.setLocation(currentLocation);
        domestic.setToLocation(null);
        domestic = domesticRepository.save(domestic);
        createdDomestics.add(domestic);

        updateOrderLinksAndOrders(shippingList, domestic);
    }

    return createdDomestics.stream()
            .map(DomesticResponse::fromEntity)
            .collect(Collectors.toList());
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

    // group theo địa chỉ giao
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

        Domestic domestic = new Domestic();
        domestic.setPackings(packings);
        domestic.setShippingList(shippingList);
        domestic.setStatus(DomesticStatus.DA_GIAO);
        domestic.setTimestamp(LocalDateTime.now());
        domestic.setNote("Giao hàng cho khách (Mã: " + customerCode + ")");
        domestic.setToAddress(address);
        domestic.setStaff(currentStaff);
        domestic.setFromLocation(currentLocation);
        domestic.setLocation(currentLocation);
        domestic.setVNPostTrackingCode(VNPostTrackingCode);

        domestic = domesticRepository.save(domestic);
        domestics.add(domestic);

        updateOrderLinksAndOrders(shippingList, domestic);
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



 public List<DomesticResponse> TransferToCustomer() {
    List<Map<String, Object>> dataList = getReadyForDeliveryOrders(Pageable.unpaged());
    List<Domestic> domesticList = new ArrayList<>();

    if (dataList == null || dataList.isEmpty()) return Collections.emptyList();

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
    WarehouseLocation currentLocation = currentStaff != null ? currentStaff.getWarehouseLocation() : null;

    for (Map<String, Object> customerData : dataList) {
        String customerName = (String) customerData.get("customerName");
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) customerData.get("addresses");
        if (addresses == null || addresses.isEmpty()) continue;

        for (Map<String, Object> addressData : addresses) {
            Long addressId = ((Number) addressData.get("addressId")).longValue();

            Address address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ ID: " + addressId));

            System.out.println("📦 Đang tạo Domestic cho địa chỉ: " + address.getAddressName());

            List<Map<String, Object>> packings = (List<Map<String, Object>>) addressData.get("packings");
            if (packings == null || packings.isEmpty()) continue;

            List<String> shippingList = new ArrayList<>();
            Set<Packing> packingSet = new HashSet<>();

            for (Map<String, Object> packingData : packings) {
                String packingCode = (String) packingData.get("packingCode");
                Optional<Packing> optionalPacking = packingRepository.findByPackingCode(packingCode);
                if (optionalPacking.isPresent()) {
                    Packing packingEntity = optionalPacking.get();
                    packingSet.add(packingEntity);

                    Set<String> trackingCodes = (Set<String>) packingData.get("trackingCodes");
                    if (trackingCodes != null) shippingList.addAll(trackingCodes);
                }
            }
            if (packingSet.isEmpty()) continue;

            Domestic domestic = new Domestic();
            domestic.setPackings(packingSet);
            domestic.setShippingList(shippingList);
            domestic.setStatus(DomesticStatus.DA_GIAO);
            domestic.setTimestamp(LocalDateTime.now());
            domestic.setNote("Giao hàng cho khách hàng: " + customerName);
            domestic.setToAddress(address);
            if (currentStaff != null) {
                domestic.setStaff(currentStaff);
                domestic.setFromLocation(currentLocation);
                domestic.setLocation(currentLocation);
            }
            domestic.setToLocation(null);

            domestic = domesticRepository.save(domestic);
            domesticList.add(domestic);

           for (String shipmentCode : shippingList) {
    List<OrderLinks> links = orderLinksRepository.findByShipmentCode(shipmentCode);
        
        for (OrderLinks link : links) {
            link.setStatus(OrderLinkStatus.DA_GIAO);
        }
        orderLinksRepository.saveAll(links);

        Set<Orders> relatedOrders = links.stream()
                .map(OrderLinks::getOrders)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<PartialShipment> relatedPartials = links.stream()
                .map(OrderLinks::getPartialShipment)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Orders order : relatedOrders) {
            Set<OrderLinks> allLinks = order.getOrderLinks();

            boolean allDeliveredOrCanceled = allLinks.stream().allMatch(l ->
                    l.getStatus() == OrderLinkStatus.DA_GIAO ||
                    l.getStatus() == OrderLinkStatus.DA_HUY
            );

            if (allDeliveredOrCanceled && order.getStatus() != OrderStatus.DA_GIAO) {
                order.setStatus(OrderStatus.DA_GIAO);
                ordersRepository.save(order);
                ordersService.addProcessLog(
                        order,
                        domestic.getDomesticId().toString(),
                        ProcessLogAction.DA_GIAO
                );
            }
        }

        for (PartialShipment partial : relatedPartials) {
            Set<OrderLinks> partialLinks = partial.getReadyLinks();

            boolean allLinksDeliveredOrCanceled = partialLinks.stream().allMatch(link ->
                    link.getStatus() == OrderLinkStatus.DA_GIAO ||
                    link.getStatus() == OrderLinkStatus.DA_HUY
            );

            if (allLinksDeliveredOrCanceled && partial.getStatus() != OrderStatus.DA_GIAO) {
                partial.setStatus(OrderStatus.DA_GIAO);
                partialShipmentRepository.save(partial); 

                ordersService.addProcessLog(
                        partial.getOrders(), 
                        "PartialShipment#" + partial.getId(),
                        ProcessLogAction.DA_GIAO
                );
            }
                }
            }
        }
    }

    return domesticList.stream()
            .map(DomesticResponse::fromEntity)
            .collect(Collectors.toList());
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

    public List<Map<String, Object>> getReadyForDeliveryOrdersToShip(List<String> shipmentCodes) {
   
    List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCodeIn(shipmentCodes);

    Set<Orders> ordersSet = orderLinks.stream()
            .map(OrderLinks::getOrders) 
            .collect(Collectors.toSet()); 

    Map<Customer, List<Orders>> customerToOrdersMap = ordersSet.stream()
            .collect(Collectors.groupingBy(Orders::getCustomer));

    List<Map<String, Object>> result = new ArrayList<>();

    for (Map.Entry<Customer, List<Orders>> customerEntry : customerToOrdersMap.entrySet()) {
        Customer customer = customerEntry.getKey();

        Map<String, Object> customerData = new HashMap<>();
        customerData.put("customerName", customer.getName());
        customerData.put("customerCode", customer.getCustomerCode());
        customerData.put("customerPhone", customer.getPhone());

        Map<Address, List<Orders>> addressToOrdersMap = customerEntry.getValue().stream()
                .filter(order -> order.getAddress() != null)
                .collect(Collectors.groupingBy(Orders::getAddress));

        List<Map<String, Object>> addressDataList = new ArrayList<>();

        for (Map.Entry<Address, List<Orders>> addressEntry : addressToOrdersMap.entrySet()) {
            Address address = addressEntry.getKey();

            Map<String, Object> addressData = new HashMap<>();
            addressData.put("addressId", address.getAddressId());
            addressData.put("addressName", address.getAddressName());

            Map<Packing, List<Warehouse>> packingToWarehousesMap = addressEntry.getValue().stream()
                    .flatMap(order -> order.getWarehouses().stream())
                    .filter(warehouse -> warehouse.getPacking() != null)
                    .collect(Collectors.groupingBy(Warehouse::getPacking));

            List<Map<String, Object>> packingsData = new ArrayList<>();

            for (Map.Entry<Packing, List<Warehouse>> packingEntry : packingToWarehousesMap.entrySet()) {
                Packing packing = packingEntry.getKey();

                Set<String> trackingCodes = packingEntry.getValue().stream()
                        .filter(warehouse -> warehouse.getOrderLinks().stream()
                                .anyMatch(orderLink -> orderLink.getStatus() == OrderLinkStatus.CHO_GIAO))
                        .map(Warehouse::getTrackingCode)
                        .collect(Collectors.toSet());

                if (trackingCodes.isEmpty()) continue;

                List<Map<String, Object>> orderLinksList = packingEntry.getValue().stream()
                        .flatMap(warehouse -> warehouse.getOrderLinks().stream())
                        .filter(orderLink -> orderLink.getStatus() == OrderLinkStatus.CHO_GIAO)
                        .map(orderLink -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("orderLinkId", orderLink.getLinkId());
                            map.put("trackingCode", orderLink.getTrackingCode());
                            map.put("price", orderLink.getPriceWeb());
                            map.put("status", orderLink.getStatus());
                            return map;
                        })
                        .collect(Collectors.toList());

                Map<String, Object> packingData = new HashMap<>();
                packingData.put("packingCode", packing.getPackingCode());
                packingData.put("trackingCodes", trackingCodes);
                packingData.put("orderLinks", orderLinksList);

                packingsData.add(packingData);
            }

            if (packingsData.isEmpty()) continue;

            addressData.put("packings", packingsData);
            addressDataList.add(addressData);
        }

        if (addressDataList.isEmpty()) continue;

        customerData.put("addresses", addressDataList);

        result.add(customerData);
    }

    return result;
}

public List<Map<String, Object>> getReadyForDeliveryOrders(Pageable pageable) {
    List<OrderStatus> statuses = Arrays.asList(OrderStatus.CHO_GIAO, OrderStatus.DANG_XU_LY);
    Page<Orders> ordersPage = ordersRepository.findByStatuses(statuses, pageable);

    Map<Customer, List<Orders>> customerToOrdersMap = ordersPage.getContent().stream()
            .collect(Collectors.groupingBy(Orders::getCustomer));

    List<Map<String, Object>> result = new ArrayList<>();

    for (Map.Entry<Customer, List<Orders>> customerEntry : customerToOrdersMap.entrySet()) {
        Customer customer = customerEntry.getKey();
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("customerName", customer.getName());
        customerData.put("customerCode", customer.getCustomerCode());
        customerData.put("customerPhone", customer.getPhone());

        Map<Address, List<Orders>> addressToOrdersMap = customerEntry.getValue().stream()
                .filter(order -> order.getAddress() != null)
                .collect(Collectors.groupingBy(Orders::getAddress));

        List<Map<String, Object>> addressDataList = new ArrayList<>();

        for (Map.Entry<Address, List<Orders>> addressEntry : addressToOrdersMap.entrySet()) {
            Address address = addressEntry.getKey();
            Map<String, Object> addressData = new HashMap<>();
            addressData.put("addressId", address.getAddressId());
            addressData.put("addressName", address.getAddressName());

            Map<Packing, List<Warehouse>> packingToWarehousesMap = addressEntry.getValue().stream()
                    .flatMap(order -> order.getWarehouses().stream())
                    .filter(warehouse -> warehouse.getPacking() != null)
                    .collect(Collectors.groupingBy(Warehouse::getPacking));

            List<Map<String, Object>> packingsData = new ArrayList<>();

            for (Map.Entry<Packing, List<Warehouse>> packingEntry : packingToWarehousesMap.entrySet()) {
    Packing packing = packingEntry.getKey();

    // trackingCodes
    Set<String> trackingCodes = packingEntry.getValue().stream()
            .filter(warehouse -> warehouse.getOrderLinks().stream()
                    .anyMatch(orderLink -> orderLink.getStatus() == OrderLinkStatus.CHO_GIAO))
            .map(Warehouse::getTrackingCode)
            .collect(Collectors.toSet());

    if (trackingCodes.isEmpty()) continue;

    // orderLinks
    List<Map<String, Object>> orderLinksList = packingEntry.getValue().stream()
            .flatMap(warehouse -> warehouse.getOrderLinks().stream())
            .filter(orderLink -> orderLink.getStatus() == OrderLinkStatus.CHO_GIAO)
            .map(orderLink -> {
                Map<String, Object> map = new HashMap<>();
                map.put("orderLinkId", orderLink.getLinkId());
                map.put("trackingCode", orderLink.getTrackingCode());
                map.put("price", orderLink.getPriceWeb());
                map.put("status", orderLink.getStatus());
                return map;
            })
            .collect(Collectors.toList());

            Map<String, Object> packingData = new HashMap<>();
            packingData.put("packingCode", packing.getPackingCode());
            packingData.put("trackingCodes", trackingCodes);
            packingData.put("orderLinks", orderLinksList);
            packingsData.add(packingData);
        }

            if (packingsData.isEmpty()) continue;

            addressData.put("packings", packingsData);
            addressDataList.add(addressData);
        }

        if (addressDataList.isEmpty()) continue;

        customerData.put("addresses", addressDataList);
        result.add(customerData);
    }

    return result;
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
private boolean isCustomerMatching(Map<String, Object> data) {
    List<Map<String, Object>> addresses = (List<Map<String, Object>>) data.get("addresses");
    if (addresses == null || addresses.isEmpty()) return false;

    List<Map<String, Object>> packings = (List<Map<String, Object>>) addresses.get(0).get("packings");
    if (packings == null || packings.isEmpty()) return false;

    String firstPackingCode = (String) packings.get(0).get("packingCode");
    String customerCode = (String) data.get("customerCode");

    // Kiểm tra mã khách hàng với packing code
    return packingRepository.findByPackingCode(firstPackingCode)
            .map(p -> p.getWarehouses())
            .flatMap(warehouses -> warehouses.stream().findFirst())
            .map(Warehouse::getOrders)
            .map(Orders::getCustomer)
            .map(Customer::getCustomerCode)
            .map(code -> code.equalsIgnoreCase(customerCode))
            .orElse(false);
}

    public Boolean scanImportToDomestic(String shipmentCode) {
        List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);

        if (orderLinks.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng trong danh sách cung cấp!");
        }

        boolean updated = false;

        for (OrderLinks orderLink : orderLinks) {
            if (orderLink.getStatus() == OrderLinkStatus.CHO_NHAP_KHO_VN) {
                orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_VN);
                updated = true;
            }
        }
        if (updated) {
            orderLinksRepository.saveAll(orderLinks);
            updateOrderStatusIfAllLinksReady(orderLinks);
        }
        return true;
}

 public CheckInDomestic getCheckInDomestic(String shipmentCode) {

    List<OrderLinks> orderLinks =
            orderLinksRepository.findByShipmentCode(shipmentCode);

    if (orderLinks.isEmpty()) {
        throw new IllegalArgumentException(
                "Không tìm thấy đơn hàng trong danh sách cung cấp!"
        );
    }
    OrderLinks orderLink = orderLinks.get(0);
    if (orderLink.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN) {
        throw new IllegalStateException(
                "Đơn hàng đã được nhập kho Việt Nam!"
        );
    }
    if (orderLink.getStatus() == OrderLinkStatus.DANG_CHUYEN_VN) {
        throw new IllegalStateException(
                "Đơn hàng đã đến Việt Nam nhưng bạn chưa nhận thùng hàng vào kho!"
        );
    }
    Warehouse warehouse = orderLink.getWarehouse();
    if (warehouse == null) {
        throw new IllegalStateException("OrderLink chưa được gán kho!");
    }
    Packing packing = warehouse.getPacking();
    if (packing == null) {
        throw new IllegalStateException("Warehouse chưa thuộc packing nào!");
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

public Page<DomesticDelivery> getDomesticDelivery(
        DeliveryStatus status,
        String customerCode,
        Pageable pageable
) {
    OrderLinkStatus orderLinkStatus =
            (status == DeliveryStatus.CHUA_DU_DIEU_KIEN)
                    ? OrderLinkStatus.DA_NHAP_KHO_VN
                    : OrderLinkStatus.CHO_GIAO;

    String filterCustomerCode = null;
    if (customerCode != null && !customerCode.trim().isEmpty()) {
        filterCustomerCode = customerCode.trim().toUpperCase();
    }

    Page<Warehouse> warehousePage =
            warehouseRepository.findByOrderLinkStatusAndCustomerCode(
                    orderLinkStatus,
                    filterCustomerCode,
                    pageable
            );

    if (warehousePage.isEmpty()) {
        return Page.empty(pageable);
    }

    List<DomesticDelivery> result = new ArrayList<>();

    Map<String, DomesticDelivery> map = new LinkedHashMap<>();

    for (Warehouse w : warehousePage.getContent()) {
        Orders order = w.getOrders();
        if (order == null || order.getCustomer() == null) continue;

        String cCode = order.getCustomer().getCustomerCode();

        DomesticDelivery delivery =
                map.computeIfAbsent(cCode, k -> {
                    DomesticDelivery d = new DomesticDelivery();
                    d.setCustomerCode(cCode);
                    d.setCustomerName(order.getCustomer().getName());
                    d.setPhoneNumber(order.getCustomer().getPhone());
                    d.setAddress(
                            order.getAddress() != null
                                    ? order.getAddress().getAddressName()
                                    : null
                    );
                    d.setStaffName(order.getStaff().getName());
                    d.setStatus(status.name());
                    d.setShipmemtCode(new ArrayList<>());
                    return d;
                });

        if (w.getOrderLinks() != null) {
            w.getOrderLinks().stream()
                    .map(OrderLinks::getShipmentCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .forEach(code -> {
                        if (!delivery.getShipmemtCode().contains(code)) {
                            delivery.getShipmemtCode().add(code);
                        }
                    });
        }
    }

    result.addAll(map.values());

    return new PageImpl<>(
            result,
            pageable,
            warehousePage.getTotalElements()
    );
}

    
 }

