package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PackingStatus;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Model.PackingCheckResponse;
import com.tiximax.txm.Model.PackingEligibleOrder;
import com.tiximax.txm.Model.PackingExport;
import com.tiximax.txm.Model.PackingInWarehouse;
import com.tiximax.txm.Model.PackingRequest;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PackingService {

    @Autowired
    private PackingRepository packingRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AccountUtils accountUtils;

    public Page<PackingEligibleOrder> getEligibleOrdersForPacking(Pageable pageable) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff == null || staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("The current employee has not been assigned a warehouse location!");
        }
        List<OrderStatus> statuses = Arrays.asList(OrderStatus.CHO_DONG_GOI, OrderStatus.DANG_XU_LY);
        Page<Orders> ordersPage = ordersRepository.findByStatusInAndWarehouses_Location_LocationId(
                statuses, staff.getWarehouseLocation().getLocationId(), pageable);

        List<PackingEligibleOrder> eligibleOrders = ordersPage.getContent().stream()
                .filter(order -> order.getWarehouses().stream()
                        .anyMatch(warehouse -> warehouse.getPacking() == null &&
                                warehouse.getOrderLinks().stream()
                                        .anyMatch(orderLink -> orderLink.getStatus() == OrderLinkStatus.DA_NHAP_KHO_NN)))
                .map(order -> {
                    Map<String, Integer> trackingToCount = new HashMap<>();
                    Set<Warehouse> warehouses = order.getWarehouses();
                    for (Warehouse warehouse : warehouses) {
                        if (warehouse.getPacking() == null) {
                            int productCount = (int) warehouse.getOrderLinks().stream()
                                    .filter(orderLink -> orderLink.getStatus() == OrderLinkStatus.DA_NHAP_KHO_NN)
                                    .count();
                            if (productCount > 0) {
                                trackingToCount.put(warehouse.getTrackingCode(), productCount);
                            }
                        }
                    }
                    PackingEligibleOrder eligibleOrder = new PackingEligibleOrder();
                    eligibleOrder.setOrderCode(order.getOrderCode());
                    eligibleOrder.setTrackingCodeToProductCount(trackingToCount);
                    return eligibleOrder;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(eligibleOrders, pageable, ordersPage.getTotalElements());
    }

    @Transactional
    public Packing createPacking(PackingRequest request) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("The current employee has not been assigned a warehouse location!");
        }

          PackingCheckResponse check =
            checkCreatePacking(request.getShipmentCodes());
            validateBeforeCreatePacking(check);

        List<String> filteredCodes = request.getShipmentCodes().stream()
        .filter(code -> code != null && !code.trim().isEmpty())
        .toList();
        if (filteredCodes.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mã vận đơn không được để trống!");
        }

        List<Warehouse> warehouses = warehouseRepository.findByTrackingCodeInWithOrdersAndLinks(filteredCodes);
        if (warehouses.isEmpty()) {
            throw new IllegalArgumentException("The tracking code you provided was not found!");
        }
        for (Warehouse warehouse : warehouses) {
            if (warehouse.getPacking() != null) {
                throw new IllegalArgumentException("Warehouse with tracking code " + warehouse.getTrackingCode() + " already has packing, cannot continue!");
            }
        }

        Set<Orders> orders = warehouses.stream()
                .map(Warehouse::getOrders)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (orders.isEmpty()) {
            throw new IllegalArgumentException("No orders found related to the tracking codes you provided!");
        }

        for (Orders order : orders) {
            if (!(order.getStatus().equals(OrderStatus.CHO_DONG_GOI) || order.getStatus().equals(OrderStatus.DANG_XU_LY))) {
                throw new IllegalArgumentException("Order " + order.getOrderCode() + " is not eligible for packaging!");
            }
        }

        Destination destination = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> new IllegalArgumentException("This destination was not found!"));
        for (Warehouse warehouse : warehouses) {
            if (!warehouse.getOrders().getDestination().getDestinationId().equals(destination.getDestinationId())) {
                throw new IllegalArgumentException("The tracking code " + warehouse.getTrackingCode() + " does not have the same destination!");
            }
        }

        Set<String> packingListSet = new HashSet<>();
        for (Warehouse warehouse : warehouses) {
            for (OrderLinks orderLink : warehouse.getOrderLinks()) {
                if (!orderLink.getShipmentCode().isEmpty()){
                    orderLink.setStatus(OrderLinkStatus.DA_DONG_GOI);
                    packingListSet.add(orderLink.getShipmentCode());
                }
            }
        }

        List<String> packingList = new ArrayList<>(packingListSet);
        if (packingList.isEmpty()) {
            throw new IllegalArgumentException("There is no tracking code to create packing list!");
        }

        Packing packing = new Packing();
        String location = ((Staff) accountUtils.getAccountCurrent()).getWarehouseLocation().getName();
        packing.setPackingCode(generatePackingCode(location, destination.getDestinationName()));
        packing.setDestination(destination);
        packing.setPackingList(packingList);
        packing.setPackedDate(LocalDateTime.now());
        packing.setStaff(staff);
        packing.setStatus(PackingStatus.CHO_BAY);

        packing = packingRepository.save(packing);

        packing.setWarehouses(new HashSet<>(warehouses));

        for (Warehouse w : warehouses) {
            w.setPacking(packing);
        }
        warehouseRepository.saveAll(warehouses);

        orders.forEach(order -> order.setStatus(OrderStatus.DANG_XU_LY));
        ordersRepository.saveAll(new ArrayList<>(orders));

        ordersService.addProcessLog(null, packing.getPackingCode(), ProcessLogAction.DA_DONG_GOI);

        return packing;
    }
   public PackingCheckResponse checkCreatePacking(List<String> shipmentCodes) {

    PackingCheckResponse response = new PackingCheckResponse();
    response.setCanCreate(false);

    if (shipmentCodes == null || shipmentCodes.isEmpty()) {
        response.setMessage("Shipment code list must not be empty.");
        return response;
    }

    List<String> codes = shipmentCodes.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .toList();

    response.setTotalCodes(codes.size());

    if (codes.isEmpty()) {
        response.setMessage("Invalid shipment code list.");
        return response;
    }

    List<Warehouse> warehouses =
            warehouseRepository.findByTrackingCodeInWithOrdersAndLinks(codes);

    Set<String> warehouseCodes = warehouses.stream()
            .map(Warehouse::getTrackingCode)
            .collect(Collectors.toSet());

    response.setWarehouseCount(warehouses.size());

    List<String> notInWarehouse = codes.stream()
            .filter(code -> !warehouseCodes.contains(code))
            .toList();

    List<String> invalidCodes = new ArrayList<>();
    List<String> notImportedCodes = new ArrayList<>();

    if (!notInWarehouse.isEmpty()) {
        List<OrderLinks> orderLinks =
                orderLinksRepository.findByShipmentCodeIn(notInWarehouse);

        Set<String> orderLinkCodes = orderLinks.stream()
                .map(OrderLinks::getShipmentCode)
                .collect(Collectors.toSet());

        invalidCodes = notInWarehouse.stream()
                .filter(code -> !orderLinkCodes.contains(code))
                .toList();

        notImportedCodes = orderLinks.stream()
                .filter(ol -> ol.getWarehouse() == null)
                .map(OrderLinks::getShipmentCode)
                .distinct()
                .toList();
    }

    response.setInvalidCodes(invalidCodes);
    response.setNotImportedCodes(notImportedCodes);

    List<String> alreadyPackedCodes = warehouses.stream()
            .filter(w -> w.getPacking() != null)
            .map(Warehouse::getTrackingCode)
            .distinct()
            .toList();

    response.setAlreadyPackedCodes(alreadyPackedCodes);

    boolean canCreate =
            invalidCodes.isEmpty()
            && notImportedCodes.isEmpty()
            && alreadyPackedCodes.isEmpty()
            && !warehouses.isEmpty();

    response.setCanCreate(canCreate);

    if (canCreate) {
    response.setMessage("Shipment codes are valid. Packing can be created.");
} else {

    StringBuilder messageBuilder = new StringBuilder("Invalid shipment codes:");

    if (!invalidCodes.isEmpty()) {
        messageBuilder.append("\n- Not found: ")
                .append(String.join(", ", invalidCodes));
    }

    if (!notImportedCodes.isEmpty()) {
        messageBuilder.append("\n- Not imported to warehouse yet: ")
                .append(String.join(", ", notImportedCodes));
    }

    if (!alreadyPackedCodes.isEmpty()) {
        messageBuilder.append("\n- Already packed: ")
                .append(String.join(", ", alreadyPackedCodes));
    }

    response.setMessage(messageBuilder.toString());
}

    return response;
}


    public Page<PackingInWarehouse> getPackingsInWarehouse(Pageable pageable) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff == null || staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("The current employee has not been assigned a warehouse location!");
        }

        Page<Packing> packingsPage = packingRepository.findByFlightCodeIsNullAndWarehouses_Location_LocationId(staff.getWarehouseLocation().getLocationId(), pageable);

        return packingsPage.map(packing -> {
            Map<String, Integer> trackingToCount = new HashMap<>();
            Set<Warehouse> warehouses = packing.getWarehouses();
            for (Warehouse warehouse : warehouses) {
                String trackingCode = warehouse.getTrackingCode();
                int productCount = warehouse.getOrderLinks().size();
                trackingToCount.put(trackingCode, productCount);
            }

            PackingInWarehouse piw = new PackingInWarehouse();
            piw.setPackingCode(packing.getPackingCode());
            piw.setPackedDate(packing.getPackedDate());
            piw.setTrackingCodeToProductCount(trackingToCount);
            return piw;
        });
    }

    public Packing getPackingById(Long id) {
    return packingRepository.findById(id).orElse(null);
}

    public Page<Packing> getPackingsAwaitingFlight(Pageable pageable) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff == null || staff.getWarehouseLocation() == null) {
            throw new IllegalArgumentException("The current employee has not been assigned a warehouse location!");
        }
        return packingRepository.findByFlightCodeIsNullAndWarehouses_Location_LocationId(staff.getWarehouseLocation().getLocationId(), pageable);
    }

    @Transactional
public void assignFlightCode(List<Long> packingIds, String flightCode) {

    List<Packing> packings = packingRepository.findAllById(packingIds)
            .stream()
            .filter(p -> p.getFlightCode() == null)
            .peek(p -> {
                p.setFlightCode(flightCode);
                p.setStatus(PackingStatus.DA_BAY);
            })
            .toList();

    if (packings.isEmpty()) {
        throw new IllegalArgumentException(
                "Không tìm thấy packing nào phù hợp hoặc đã được gán chuyến bay!"
        );
    }

    // 🔹 Collect ALL shipmentCodes (1 lần)
    Set<String> allShipmentCodes = packings.stream()
            .flatMap(p -> p.getPackingList().stream())
            .filter(code -> code != null && !code.trim().isEmpty())
            .collect(Collectors.toSet());

    
    Map<String, List<OrderLinks>> orderLinksByShipment =
            allShipmentCodes.isEmpty()
                    ? Map.of()
                    : orderLinksRepository
                        .findByShipmentCodeIn(new ArrayList<>(allShipmentCodes))
                        .stream()
                        .collect(Collectors.groupingBy(OrderLinks::getShipmentCode));

    
    for (Packing packing : packings) {

        packing.getPackingList().stream()
                .filter(code -> code != null && !code.trim().isEmpty())
                .flatMap(code ->
                        orderLinksByShipment
                                .getOrDefault(code, List.of())
                                .stream()
                )
                .forEach(ol ->
                        ol.setStatus(OrderLinkStatus.DANG_CHUYEN_VN)
                );

        ordersService.addProcessLog(
                null,
                packing.getPackingCode(),
                ProcessLogAction.DA_BAY
        );
    }

    // 🔹 Save 1 lần
    packingRepository.saveAll(packings);
    orderLinksRepository.saveAll(
            orderLinksByShipment.values()
                    .stream()
                    .flatMap(List::stream)
                    .toList()
    );
}


    private String generatePackingCode(String location, String destinationName) {
        String monthYear = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"));
        String[] words = destinationName.split("\\s+");
        String shortDestination = "";
        for (String word : words) {
            if (!word.isEmpty()) {
                shortDestination += word.charAt(0);
            }
        }
        String baseCode = location + monthYear + shortDestination;

        String finalCode = baseCode + "1";
        boolean exists;
        int sequence = 1 ;

          do {
        exists = packingRepository.existsByPackingCode(finalCode);
        if (exists) {
            sequence++;
            finalCode = baseCode + sequence;
        }
        } while (exists);

        return finalCode;
    }

    private int getNextSequence(String baseCode) {
        List<Packing> existingPackings = packingRepository.findByPackingCodeStartingWith(baseCode);
        if (existingPackings.isEmpty()) {
            return 1;
        }
        Packing lastPacking = existingPackings.get(existingPackings.size() - 1);
        String lastCode = lastPacking.getPackingCode();
        if (lastCode != null && lastCode.startsWith(baseCode)) {
            try {
                String seqStr = lastCode.substring(baseCode.length());
                int seq = Integer.parseInt(seqStr);
                return seq + 1;
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    public Page<Packing> getPackingsWithFlightStatus(Pageable pageable) {
        Page<Packing> packings = packingRepository.findByStatus(PackingStatus.DA_BAY, pageable);
        packings.getContent().forEach(packing -> {
            List<String> filteredList = packing.getPackingList().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            packing.setPackingList(filteredList);
        });
        return packings;
    }

    public Packing removeShipmentFromPacking(String packingCode, List<String> shipmentCodesToRemove) {
        Packing packing = packingRepository.findByPackingCode(packingCode)
                .orElseThrow(() -> new IllegalArgumentException("Not found " + packingCode));

        if (packing.getStatus() == PackingStatus.DA_BAY) {
            throw new IllegalArgumentException("Items cannot be removed from packaging that has been shipped!");
        }

        List<Warehouse> warehousesToRemove = warehouseRepository.findByPackingPackingIdAndTrackingCodeIn(
                packing.getPackingId(), shipmentCodesToRemove);

        if (warehousesToRemove.isEmpty()) {
            throw new IllegalArgumentException("No tracking code found to remove!");
        }

        List<String> currentPackingList = new ArrayList<>(packing.getPackingList());
        currentPackingList.removeAll(shipmentCodesToRemove);
        packing.setPackingList(currentPackingList);

        Set<Orders> affectedOrders = new HashSet<>();

        for (Warehouse warehouse : warehousesToRemove) {
            warehouse.setPacking(null);
            for (OrderLinks orderLink : warehouse.getOrderLinks()) {
                orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_NN);
            }

            affectedOrders.add(warehouse.getOrders());
            warehouseRepository.save(warehouse);
        }
        for (Orders order : affectedOrders) {
            boolean hasPackedItems = order.getWarehouses().stream()
                    .anyMatch(w -> w.getPacking() != null);

            if (!hasPackedItems) {
                order.setStatus(OrderStatus.DANG_XU_LY);
            }
            ordersRepository.save(order);
        }

        if (currentPackingList.isEmpty()) {
            packingRepository.delete(packing);
            return null;
        }

        packingRepository.save(packing);

        ordersService.addProcessLog(
                null,
                packing.getPackingCode(),
                ProcessLogAction.DA_CHINH_SUA
        );

        return packing;
    }

    public List<String> getPackingListByCode(String packingCode) {
        List<String> list = packingRepository.findPackingListByCode(packingCode);
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("Packing List not found for code:" + packingCode);
        }
        return list;
    }

    public List<PackingExport> getPackingExportByIdsChoBay(List<Long> packingIds) {
        if (packingIds == null || packingIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Packing> packings = packingRepository.findAllChoBayWithWarehouses(packingIds);

        Set<String> trackingCodes = packings.stream()
                .flatMap(p -> p.getWarehouses().stream())
                .map(Warehouse::getTrackingCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, List<OrderLinks>> orderLinksMap = trackingCodes.isEmpty()
                ? Collections.emptyMap()
                : orderLinksRepository.findByShipmentCodeIn(trackingCodes).stream()
                .collect(Collectors.groupingBy(OrderLinks::getShipmentCode, Collectors.toList()));

        return packings.stream()
                .flatMap(packing -> packing.getWarehouses().stream()
                        .map(warehouse -> buildPackingExport(packing, warehouse, orderLinksMap)))
                .collect(Collectors.toList());
    }

    private PackingExport buildPackingExport(Packing packing, Warehouse warehouse,
                                             Map<String, List<OrderLinks>> orderLinksMap) {
        PackingExport dto = new PackingExport();

        dto.setPackingCode(packing.getPackingCode());
        dto.setFlightCode(packing.getFlightCode());
        dto.setTrackingCode(warehouse.getTrackingCode());

        List<OrderLinks> orderLinks = orderLinksMap.getOrDefault(warehouse.getTrackingCode(), List.of());

        List<String> productNames = new ArrayList<>();
        List<String> productLinks = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        BigDecimal totalPurchasedPrice = BigDecimal.ZERO;

        if (!orderLinks.isEmpty()) {
            dto.setClassify(orderLinks.get(0).getClassify());

            for (OrderLinks link : orderLinks) {
                if (link.getProductName() != null && !link.getProductName().isBlank()) {
                    productNames.add(link.getProductName().trim());
                }

                if (link.getProductLink() != null && !link.getProductLink().isBlank()) {
                    productLinks.add(link.getProductLink().trim());
                }

                if (link.getFinalPriceVnd() != null) {
                    totalPurchasedPrice = totalPurchasedPrice.add(link.getFinalPriceVnd());
                }

                Integer qty = link.getQuantity(); // hoặc getQty(), tùy entity của bạn
                if (qty != null && qty > 0) {
                    quantities.add(qty);
                } else {
                    quantities.add(0);
                }
            }
        }

        Purchases purchase = warehouse.getPurchase();
        if (purchase != null) {
            if (purchase.getFinalPriceOrder() != null) {
                totalPurchasedPrice = purchase.getFinalPriceOrder();
            }
        }

            dto.setProductNames(productNames);
            dto.setProductLink(productLinks);
            dto.setQuantities(quantities);
            dto.setHeight(warehouse.getHeight());
            dto.setLength(warehouse.getLength());
            dto.setWidth(warehouse.getWidth());
            dto.setDim(warehouse.getDim());
            dto.setWeight(warehouse.getWeight());
            dto.setNetWeight(warehouse.getNetWeight());
            dto.setPrice(totalPurchasedPrice);

            Orders order = warehouse.getOrders();
            if (order != null) {
                dto.setOrderCode(order.getOrderCode());
                dto.setDestination(order.getDestination() != null ? order.getDestination().getDestinationName() : null);
                if (order.getCustomer() != null) {
                    dto.setCustomerCode(order.getCustomer().getCustomerCode());
                    dto.setCustomerName(order.getCustomer().getName());
                }
                dto.setStaffName(order.getStaff() != null ? order.getStaff().getName() : null);
            }

            return dto;
        }

    public List<Warehouse> getWarehousesByPackingId(Long packingId) {
        Packing packing = packingRepository.findById(packingId)
                .orElseThrow(() -> new IllegalArgumentException("Not found this packing list!"));
        return new ArrayList<>(packing.getWarehouses());
    }
    private void validateBeforeCreatePacking(PackingCheckResponse check) {
    if (!check.isCanCreate()) {
        throw new IllegalArgumentException(check.getMessage());
    }
}

    @Transactional
    public Packing addShipmentsToPacking(String packingCode, List<String> shipmentCodes) {
        if (shipmentCodes == null || shipmentCodes.isEmpty()) {
            throw new IllegalArgumentException("The list of added waybill codes cannot be empty!");
        }

        Packing packing = packingRepository.findByPackingCode(packingCode)
                .orElseThrow(() -> new IllegalArgumentException("Packing not found with code: " + packingCode));

        if (packing.getStatus() == PackingStatus.DA_BAY ||
                packing.getStatus() == PackingStatus.DA_NHAP_KHO_VN) {
            throw new IllegalArgumentException("Cannot add packages to packing that have already been flown or imported into Vietnam warehouse!");
        }

        List<String> filteredCodes = shipmentCodes.stream()
                .filter(code -> code != null && !code.trim().isEmpty())
                .map(String::trim)
                .toList();

        if (filteredCodes.isEmpty()) {
            throw new IllegalArgumentException("No valid tracking code to add!");
        }

        List<Warehouse> warehousesToAdd = warehouseRepository
                .findByTrackingCodeInAndPackingIsNullWithOrdersAndLinks(filteredCodes);

        if (warehousesToAdd.size() != filteredCodes.size()) {
            Set<String> foundCodes = warehousesToAdd.stream()
                    .map(Warehouse::getTrackingCode)
                    .collect(Collectors.toSet());

            List<String> missingOrPacked = filteredCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .toList();

            throw new IllegalArgumentException(
                    "The following tracking codes do not exist or have been packed:" + missingOrPacked);
        }

        Destination packingDestination = packing.getDestination();

        Set<Orders> affectedOrders = new HashSet<>();
        Set<String> newShipmentCodes = new HashSet<>();

        for (Warehouse warehouse : warehousesToAdd) {
            Orders order = warehouse.getOrders();
            if (!order.getDestination().getDestinationId().equals(packingDestination.getDestinationId())) {
                throw new IllegalArgumentException(
                        "The tracking code " + warehouse.getTrackingCode() +
                                " has a different destination than the current packing!");
            }

            if (!(order.getStatus() == OrderStatus.CHO_DONG_GOI ||
                    order.getStatus() == OrderStatus.DANG_XU_LY)) {
                throw new IllegalArgumentException(
                        "The order " + order.getOrderCode() + " not in a suitable state for packaging!");
            }

            warehouse.setPacking(packing);

            for (OrderLinks link : warehouse.getOrderLinks()) {
                if (link.getShipmentCode() != null && !link.getShipmentCode().trim().isEmpty()) {
                    link.setStatus(OrderLinkStatus.DA_DONG_GOI);
                    newShipmentCodes.add(link.getShipmentCode());
                }
            }

            affectedOrders.add(order);
            warehouseRepository.save(warehouse);
        }

        List<String> currentList = new ArrayList<>(packing.getPackingList());
        currentList.addAll(newShipmentCodes);
        packing.setPackingList(currentList);

        Set<Warehouse> warehouses = packing.getWarehouses();
        if (warehouses == null) {
            warehouses = new HashSet<>();
            packing.setWarehouses(warehouses);
        }
        warehouses.addAll(warehousesToAdd);

        for (Orders order : affectedOrders) {
            boolean hadPackedBefore = order.getWarehouses().stream()
                    .anyMatch(w -> w.getPacking() != null && !warehousesToAdd.contains(w));

            if (!hadPackedBefore) {
                order.setStatus(OrderStatus.DANG_XU_LY);
                ordersRepository.save(order);
            }
        }

        packingRepository.save(packing);

        ordersService.addProcessLog(
                null,
                packing.getPackingCode() + " - Thêm " + newShipmentCodes.size() + " mã vận đơn",
                ProcessLogAction.DA_CHINH_SUA);

        return packing;
    }
}