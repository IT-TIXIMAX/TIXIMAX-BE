package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.Warehouse.WarehouseRequest;
import com.tiximax.txm.Model.DTOResponse.DashBoard.ExportedQuantity;
import com.tiximax.txm.Model.DTOResponse.Warehouse.WarehouseSummary;
import com.tiximax.txm.Model.Projections.ExportedQuantityProjection;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Warehouse createWarehouseEntryByShipmentCode(String shipmentCode, WarehouseRequest warehouseRequest) {
        List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
        if (orderLinks.isEmpty()) {
            throw new NotFoundException("Không tìm thấy mã sản phẩm với mã vận đơn " + shipmentCode + "!");
        }
        Orders order = orderLinks.get(0).getOrders();
        if (order == null) {
            throw new NotFoundException("Không tìm thấy đơn hàng liên quan đến mã vận đơn này!");
        }

        if (!(order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN) ||
                order.getStatus().equals(OrderStatus.CHO_DONG_GOI) ||
                order.getStatus().equals(OrderStatus.DANG_XU_LY))) {
            throw new BadRequestException("Đơn hàng chưa đủ điều kiện để nhập kho!");
        }

        if (order.getCheckRequired() && warehouseRequest.getImageCheck().isEmpty()){
            throw new BadRequestException("Đơn hàng này cần được kiểm tra trước khi nhập kho!");
        }

        if (warehouseRepository.existsByTrackingCode(shipmentCode)) {
            throw new BadRequestException("Mục kho đã tồn tại cho mã vận đơn này!");
        }

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new BadRequestException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        WarehouseLocation location = new WarehouseLocation();
        location.setLocationId(staff.getWarehouseLocation().getLocationId());

        if (order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)) {
            order.setStatus(OrderStatus.CHO_DONG_GOI);
            ordersRepository.save(order);
        }

      Double dim = (warehouseRequest.getLength() * warehouseRequest.getWidth() * warehouseRequest.getHeight()) / 6000.0;

        BigDecimal dimRounded = new BigDecimal(dim)
                .setScale(1, RoundingMode.HALF_UP);

        double finalDim = dimRounded.doubleValue();

        Warehouse warehouse = new Warehouse();
            warehouse.setTrackingCode(shipmentCode);
            warehouse.setLength(warehouseRequest.getLength());
            warehouse.setWidth(warehouseRequest.getWidth());
            warehouse.setHeight(warehouseRequest.getHeight());
            warehouse.setDim(finalDim);
            warehouse.setWeight(warehouseRequest.getWeight());
            if (finalDim > warehouse.getWeight()){
                warehouse.setNetWeight(finalDim);
            } else {
                warehouse.setNetWeight(warehouse.getWeight());
            }
            warehouse.setImage(warehouseRequest.getImage());
            warehouse.setImageCheck(warehouseRequest.getImageCheck());  
            warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO_NN);
            warehouse.setCreatedAt(LocalDateTime.now());
            warehouse.setStaff(staff);
            warehouse.setLocation(location);
            warehouse.setOrders(order);
            warehouse.setPurchase(orderLinks.get(0).getPurchase());
            warehouse.setOrderLinks(new HashSet<>(orderLinks));
            warehouse.setPacking(null);

        orderLinks.forEach(orderLink -> {
            orderLink.setWarehouse(warehouse);
            orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_NN);
        });

        warehouseRepository.save(warehouse);
        orderLinksRepository.saveAll(orderLinks);
        ordersService.addProcessLog(order, shipmentCode, ProcessLogAction.DA_NHAP_KHO_NN);
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "INSERT",
                        "shipmentCode", shipmentCode,
                        "message", "Vừa nhập kho!"
                )
        );
        return warehouse;
    }

    public Optional<Warehouse> getWarehouseById(Long id) {
    return warehouseRepository.findById(id);
}

    public String createWarehouseEntryByListShipmentCodes(List<String> shipmentCodes) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new BadRequestException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        WarehouseLocation location = new WarehouseLocation();
        location.setLocationId(staff.getWarehouseLocation().getLocationId());

        for (String shipmentCode : shipmentCodes) {
            List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
            if (orderLinks.isEmpty()) {
                throw new NotFoundException("Không tìm thấy mã sản phẩm với mã vận đơn " + shipmentCode + "!");
            }

            Orders order = orderLinks.get(0).getOrders();
            if (order == null) {
                throw new NotFoundException("Không tìm thấy đơn hàng liên quan đến mã vận đơn này!");
            }

            if (!(order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN) ||
                    order.getStatus().equals(OrderStatus.CHO_DONG_GOI) ||
                    order.getStatus().equals(OrderStatus.DANG_XU_LY))) {
                throw new RuntimeException("Đơn hàng chưa đủ điều kiện để nhập kho!");
            }

            if (warehouseRepository.existsByTrackingCode(shipmentCode)) {
                throw new BadRequestException("Mục kho đã tồn tại cho mã vận đơn này!");
            }

            if (order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN)) {
                order.setStatus(OrderStatus.CHO_DONG_GOI);
                ordersRepository.save(order);
            }

            Warehouse warehouse = new Warehouse();
                warehouse.setTrackingCode(shipmentCode);
                warehouse.setLength(null);
                warehouse.setWidth(null);
                warehouse.setHeight(null);
                warehouse.setDim(null);
                warehouse.setWeight(null);
                warehouse.setNetWeight(null);
                warehouse.setImage(null);
                warehouse.setStatus(WarehouseStatus.DA_NHAP_KHO_NN);
                warehouse.setCreatedAt(LocalDateTime.now());
                warehouse.setStaff(staff);
                warehouse.setLocation(location);
                warehouse.setOrders(order);
                warehouse.setPurchase(orderLinks.get(0).getPurchase());
                warehouse.setOrderLinks(new HashSet<>(orderLinks));
                warehouse.setPacking(null);

            orderLinks.forEach(orderLink -> {
                orderLink.setWarehouse(warehouse);
                orderLink.setStatus(OrderLinkStatus.DA_NHAP_KHO_NN);
            });

            warehouseRepository.save(warehouse);
            orderLinksRepository.saveAll(orderLinks);

            ordersService.addProcessLog(order, shipmentCode, ProcessLogAction.DA_NHAP_KHO_NN);
        }

        return "Thành công nhập kho các mã vận đơn vừa truyền vào!";
    }

    public Page<WarehouseSummary> getWarehousesForPacking(Pageable pageable,  String trackingCode) {

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new BadRequestException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }
         WarehouseLocation location = new WarehouseLocation();
        location.setLocationId(staff.getWarehouseLocation().getLocationId());

    List<OrderLinkStatus> validStatuses = List.of(
            OrderLinkStatus.DA_NHAP_KHO_NN,
            OrderLinkStatus.DA_DONG_GOI
    );

     if (trackingCode != null && trackingCode.trim().isEmpty()) {
        trackingCode = null;
    }

    Page<Warehouse> warehousePage =
            warehouseRepository.findWarehousesForPacking(
                    WarehouseStatus.DA_NHAP_KHO_NN,
                    location.getLocationId(),
                    validStatuses,
                    trackingCode,
                    pageable
            );
                 List<WarehouseSummary> summaries = warehousePage.getContent().stream()
        .map(w -> {
            OrderLinkStatus status = w.getOrderLinks()
                    .stream()
                    .findFirst()
                    .map(OrderLinks::getStatus)
                    .orElse(null);

            return new WarehouseSummary(
                    w.getWarehouseId(),
                    w.getTrackingCode(),
                    w.getOrders().getOrderCode(),
                    w.getOrders().getCustomer().getCustomerCode(),
                    w.getOrders().getDestination().getDestinationName(),
                    w.getWeight(),
                    w.getNetWeight(),
                    w.getDim(),
                    w.getImage(),
                    w.getImageCheck(),
                    w.getCreatedAt(),
                    status
            );
        })
        .collect(Collectors.toList());
        return new PageImpl<>(summaries, pageable, warehousePage.getTotalElements());
    }

    public Map<String, Double> calculateWarehouseTotals() {
        List<Warehouse> warehouses = warehouseRepository.findAllByStatus(WarehouseStatus.DA_NHAP_KHO_NN);
        Map<String, Double> totals = new HashMap<>();
        totals.put("totalWeight", warehouses.stream().mapToDouble(Warehouse::getWeight).sum());
        totals.put("totalNetWeight", warehouses.stream().mapToDouble(Warehouse::getNetWeight).sum());
        totals.put("totalDim", warehouses.stream().mapToDouble(Warehouse::getDim).sum());
        return totals;
    }

    public Warehouse updateWarehouseNetWeight(String trackingCode, WarehouseRequest request) {
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByTrackingCode(trackingCode);
        if (warehouseOptional.isEmpty()) {
            throw new NotFoundException("Không tìm thấy kho với mã tracking: " + trackingCode);
        }
        Warehouse warehouse = warehouseOptional.get();

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new BadRequestException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        if (warehouse.getNetWeight() != null) {
            throw new BadRequestException("Kho này đã có số cân, không thể cập nhật!");
        }

        if (request.getLength() == null || request.getWidth() == null || request.getHeight() == null || request.getWeight() == null) {
            throw new BadRequestException("Vui lòng nhập đầy đủ kích thước và cân nặng!");
        }

        Double dim = (request.getLength() * request.getWidth() * request.getHeight()) / 6000;
        warehouse.setLength(request.getLength());
        warehouse.setWidth(request.getWidth());
        warehouse.setHeight(request.getHeight());
        warehouse.setDim(dim);
        warehouse.setWeight(request.getWeight());
        warehouse.setNetWeight(dim > request.getWeight() ? dim : request.getWeight());
        warehouse.setImage(request.getImage());

        return warehouseRepository.save(warehouse);
    }

    public boolean hasNetWeight(String trackingCode) {
        return warehouseRepository.findByTrackingCode(trackingCode)
                .map(w -> w.getNetWeight() != null)
                .orElse(false);
    }

    public List<String> suggestShipmentCodes(String keyword) {
        return orderLinksRepository.suggestShipmentCodes(keyword);
    }

    public Warehouse updateWarehouse(String trackingCode, WarehouseRequest request) {
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByTrackingCode(trackingCode);
        if (warehouseOptional.isEmpty()) {
            throw new NotFoundException("Không tìm thấy kho với mã tracking: " + trackingCode);
        }
        Warehouse warehouse = warehouseOptional.get();

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        if (staff.getWarehouseLocation() == null) {
            throw new BadRequestException("Nhân viên hiện tại chưa được gán địa điểm kho!");
        }

        boolean recalculateDim = false;

        if (request.getLength() != null) {
            warehouse.setLength(request.getLength());
            recalculateDim = true;
        }
        if (request.getWidth() != null) {
            warehouse.setWidth(request.getWidth());
            recalculateDim = true;
        }
        if (request.getHeight() != null) {
            warehouse.setHeight(request.getHeight());
            recalculateDim = true;
        }
        if (request.getWeight() != null) {
            warehouse.setWeight(request.getWeight());
        }
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            warehouse.setImage(request.getImage());
        }
        if (request.getImageCheck() != null && !request.getImageCheck().isEmpty()) {
            warehouse.setImageCheck(request.getImageCheck());
        }

        if (recalculateDim) {
            if (warehouse.getLength() == null || warehouse.getWidth() == null || warehouse.getHeight() == null) {
                throw new IllegalArgumentException("Không đủ kích thước để tính dim!");
            }
            Double dim = (warehouse.getLength() * warehouse.getWidth() * warehouse.getHeight()) / 6000;
            warehouse.setDim(dim);
        }

        if (warehouse.getWeight() != null && warehouse.getDim() != null) {
            warehouse.setNetWeight(Math.max(warehouse.getDim(), warehouse.getWeight()));
        } else if (warehouse.getWeight() != null || warehouse.getDim() != null) {
            throw new BadRequestException("Không đủ dữ liệu để tính netWeight!");
        }

        return warehouseRepository.save(warehouse);
    }

   

}