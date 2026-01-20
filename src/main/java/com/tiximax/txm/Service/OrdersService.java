package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.Order.*;
import com.tiximax.txm.Model.DTORequest.OrderLink.ConsignmentLinkRequest;
import com.tiximax.txm.Model.DTORequest.OrderLink.OrderLinkRequest;
import com.tiximax.txm.Model.DTOResponse.Customer.CustomerBalanceAndOrders;
import com.tiximax.txm.Model.DTOResponse.Domestic.ShipLinkForegin;
import com.tiximax.txm.Model.DTOResponse.Domestic.ShipLinks;
import com.tiximax.txm.Model.DTOResponse.Order.OrderByShipmentResponse;
import com.tiximax.txm.Model.DTOResponse.Order.OrderDetail;
import com.tiximax.txm.Model.DTOResponse.Order.OrderPayment;
import com.tiximax.txm.Model.DTOResponse.Order.OrderWithLinks;
import com.tiximax.txm.Model.DTOResponse.Order.OrdersPendingShipment;
import com.tiximax.txm.Model.DTOResponse.Order.RefundResponse;
import com.tiximax.txm.Model.DTOResponse.Order.ShipmentGroup;
import com.tiximax.txm.Model.DTOResponse.OrderLink.InfoShipmentCode;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkWithStaff;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinksShip;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinksShipForeign;
import com.tiximax.txm.Model.DTOResponse.Warehouse.WareHouseOrderLink;
import com.tiximax.txm.Model.EnumFilter.ShipStatus;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class OrdersService {

    private static final List<OrderLinkStatus> DEFAULT_SHIP_STATUSES = List.of(
            OrderLinkStatus.DA_NHAP_KHO_VN,
            OrderLinkStatus.CHO_GIAO,
            OrderLinkStatus.CHO_TRUNG_CHUYEN,
            OrderLinkStatus.DANG_GIAO
    );

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private OrdersRepository ordersRepository;
    
    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private AccountUtils accountUtils;
    
    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProcessLogRepository processLogRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private PartialShipmentService partialShipmentService;

    @Autowired
    private AccountRouteRepository accountRouteRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Lazy
    @Autowired
    private PaymentService paymentService;

    public Orders addOrder(String customerCode, Long routeId, Long addressId, OrdersRequest ordersRequest) throws IOException {
    OrderValidation ctx = validateAndGetOrder(customerCode,routeId,addressId, ordersRequest.getDestinationId());
    Route route = ctx.getRoute();
        if (ordersRequest.getPriceShip().compareTo(route.getUnitBuyingPrice()) < 0){
            throw new IllegalArgumentException("Giá cước không được nhỏ hơn giá cố định, liên hệ quản lý để được hỗ trợ thay đổi giá cước!");
        }
        if (ordersRequest.getExchangeRate().compareTo(route.getExchangeRate()) < 0){
            throw new IllegalArgumentException("Tỉ giá không được nhỏ hơn giá cố định, liên hệ quản lý để được hỗ trợ thay đổi tỉ giá!");
        }
         Orders order = createBaseOrder(
            ctx.getCustomer(),
            ctx.getRoute(),
            ctx.getAddress(),
            ctx.getDestination(),
            ordersRequest.getOrderType(),
            OrderStatus.DA_XAC_NHAN,
            ordersRequest.getPriceShip(),
            ordersRequest.getCheckRequired()
    );
        order.setExchangeRate(ordersRequest.getExchangeRate());

         List<OrderLinks> links =
            buildOrderLinks(order, ordersRequest.getOrderLinkRequests());

        BigDecimal priceBeforeFee =
            links.stream()
                    .map(OrderLinks::getPriceWeb)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

    order.setPriceBeforeFee(priceBeforeFee);
    Orders saved = finalizeOrder(order, links);

    messagingTemplate.convertAndSend(
            "/topic/Tiximax",
            Map.of(
                    "event", "INSERT",
                    "orderCode", saved.getOrderCode(),
                    "customerCode", customerCode,
                    "message", "Đơn hàng mới được thêm!"
            )
    );
    return saved;
}
    

   @Transactional
public Orders addConsignment(
        String customerCode,
        Long routeId,
        Long addressId,
        ConsignmentRequest request
) throws IOException {

    OrderValidation ctx =
            validateAndGetOrder(
                    customerCode,
                    routeId,
                    addressId,
                    request.getDestinationId()
            );

    Orders order = createBaseOrder(
            ctx.getCustomer(),
            ctx.getRoute(),
            ctx.getAddress(),
            ctx.getDestination(),
            request.getOrderType(),
            OrderStatus.CHO_NHAP_KHO_NN,
            request.getPriceShip(),
            request.getCheckRequired()
    );

    List<OrderLinks> links =
            buildConsignmentLinks(
                    order,
                    request.getConsignmentLinkRequests()
            );

    return finalizeOrder(order, links);
}


    public Orders updateShipFee(Long orderId, BigDecimal shipFee) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng này!"));
   //     order.setShipFee(shipFee);
        ordersRepository.save(order);
        return order;
    }

    public Orders updateStatusOrderLink(Long OrderId,Long orderLinkId) {
        Orders order = ordersRepository.findById(OrderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng này!"));
        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng link"));
        
        if (orderLink.getStatus() == OrderLinkStatus.DA_HUY){
            throw new BadRequestException("Đơn hàng link đã bị hủy, không thể hủy lại!");
        }
        if (!order.getStatus().equals(OrderStatus.CHO_XAC_NHAN) &&
            !order.getStatus().equals(OrderStatus.DA_XAC_NHAN) &&
            !order.getStatus().equals(OrderStatus.CHO_THANH_TOAN)){

            BigDecimal currentLeftover = order.getLeftoverMoney() != null ? order.getLeftoverMoney() : BigDecimal.ZERO;
            order.setLeftoverMoney(currentLeftover.subtract(orderLink.getFinalPriceVnd()));
        }
        orderLink.setStatus(OrderLinkStatus.DA_HUY);
        orderLinksRepository.save(orderLink);
        ordersRepository.save(order);
        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
       
        long countNhapKhoVN = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                .count();
        long countDamua = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_MUA)
                .count();
      
        long countCancel = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_HUY)
                .count();
        if (countDamua > 0 && (countDamua + countCancel == allOrderLinks.size())) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
        }
        if (countNhapKhoVN > 0 && (countNhapKhoVN + countCancel == allOrderLinks.size())) {
            
            allOrderLinks.stream()
                    .filter(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                    .forEach(link -> {
                        link.setStatus(OrderLinkStatus.CHO_GIAO);
                        orderLinksRepository.save(link);
                    });
            order.setStatus(OrderStatus.DA_DU_HANG);
            ordersRepository.save(order);
        } else if (countCancel == allOrderLinks.size()) {
            order.setStatus(OrderStatus.DA_HUY);
            ordersRepository.save(order);
        }
        addProcessLog(order, orderLink.getTrackingCode(), ProcessLogAction.DA_HUY);
        return order; 
    }
  
    public String generateOrderCode(OrderType orderType) {
        String orderCode;
        do {
            if (orderType.equals(OrderType.MUA_HO)){
                orderCode = "MH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
            } else if (orderType.equals(OrderType.KY_GUI)) {
                orderCode = "KG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
            } else if (orderType.equals(OrderType.DAU_GIA)) {
                orderCode = "DG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
            } else if (orderType.equals(OrderType.CHUYEN_TIEN)) {
                orderCode = "CT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
            } else {
                throw new BadRequestException("Không có kiểu đơn hàng " + orderType);
            }
        } while (ordersRepository.existsByOrderCode(orderCode));
        return orderCode;
    }

    public String generateOrderLinkCode() {
        String orderLinkCode;
        do {
            orderLinkCode = "DH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
        } while (orderLinksRepository.existsByTrackingCode(orderLinkCode));
        return orderLinkCode;
    }

    public void addProcessLog(Orders orders, String actionCode, ProcessLogAction processLogAction){
        OrderProcessLog orderProcessLog = new OrderProcessLog();
        orderProcessLog.setOrders(orders);
        orderProcessLog.setStaff((Staff) accountUtils.getAccountCurrent());
        orderProcessLog.setAction(processLogAction);
        orderProcessLog.setActionCode(actionCode);
        orderProcessLog.setTimestamp(LocalDateTime.now());
        orderProcessLog.setRoleAtTime(((Staff) accountUtils.getAccountCurrent()).getRole());
        processLogRepository.save(orderProcessLog);
    }

    public Page<Orders> getAllOrdersPaging(Pageable pageable, String shipmentCode, String customerCode, String orderCode) {
    Account currentAccount = accountUtils.getAccountCurrent();
    
    if (currentAccount.getRole().equals(AccountRoles.ADMIN) 
            || currentAccount.getRole().equals(AccountRoles.MANAGER)) {
        return ordersRepository.findAllWithFilters(shipmentCode, customerCode, orderCode, pageable);
    } else if (currentAccount.getRole().equals(AccountRoles.STAFF_SALE)) {
        return ordersRepository.findByStaffAccountIdWithFilters(currentAccount.getAccountId(), shipmentCode, customerCode, orderCode, pageable);
    } else if (currentAccount.getRole().equals(AccountRoles.LEAD_SALE)) {
        List<AccountRoute> accountRoutes = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());
        Set<Long> routeIds = accountRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        if (routeIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return ordersRepository.findByRouteRouteIdInWithFilters(routeIds, shipmentCode, customerCode, orderCode, pageable);
    } else {
        throw new AccessDeniedException("Vai trò không hợp lệ!");
    }
}

    public Page<Orders> getOrdersPaging(Pageable pageable, OrderStatus status) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole().equals(AccountRoles.ADMIN) || currentAccount.getRole().equals(AccountRoles.MANAGER)) {
            return ordersRepository.findByStatus(status, pageable);
        } else if (currentAccount.getRole().equals(AccountRoles.STAFF_SALE)) {
            return ordersRepository.findByStaffAccountIdAndStatus(currentAccount.getAccountId(), status, pageable);
        } else if (currentAccount.getRole().equals(AccountRoles.LEAD_SALE)) {
            List<AccountRoute> accountRoutes = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());
            Set<Long> routeIds = accountRoutes.stream()
                    .map(AccountRoute::getRoute)
                    .map(Route::getRouteId)
                    .collect(Collectors.toSet());
            if (routeIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return ordersRepository.findByRouteRouteIdInAndStatus(routeIds, status, pageable);
        } else {
            throw new AccessDeniedException("Vai trò không hợp lệ!");
        }
    }

    public List<Orders> getOrdersForCurrentStaff() {

        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new AccessDeniedException("Tài khoản hiện tại không phải là nhân viên!");
        }

        List<AccountRoute> accountRoutes = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());
        if (accountRoutes.isEmpty()) {
            return List.of();
        }

        List<Long> routeIds = accountRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toList());

        return ordersRepository.findAll().stream()
                .filter(order -> order.getStatus().equals(OrderStatus.CHO_MUA))
                .filter(order -> routeIds.contains(order.getRoute().getRouteId()))
                .collect(Collectors.toList());
    }

    public Page<OrderPayment> getOrdersForPayment(Pageable pageable, OrderStatus status,String orderCode ) {
    Account current = accountUtils.getAccountCurrent();
    Long staffId = current.getAccountId();
    AccountRoles role = current.getRole(); // 👈 lấy role

    List<OrderStatus> validStatuses = Arrays.asList(
            OrderStatus.DA_XAC_NHAN,
            OrderStatus.CHO_THANH_TOAN,
            OrderStatus.DA_DU_HANG,
            OrderStatus.DAU_GIA_THANH_CONG,
            OrderStatus.CHO_THANH_TOAN_DAU_GIA,
            OrderStatus.CHO_THANH_TOAN_SHIP
    );

    if (status == null || !validStatuses.contains(status)) {
        throw new BadRequestException("Trạng thái không hợp lệ!");
    }

     if (orderCode != null && orderCode.trim().isEmpty()) {
            orderCode = null;
        }

    Page<Orders> ordersPage;
    if (role == AccountRoles.MANAGER) {
      
        ordersPage = ordersRepository.findByStatusForPayment(status,orderCode ,pageable);
    } else {
       
        ordersPage = ordersRepository.findByStaffAccountIdAndStatusForPayment(staffId, status, pageable);
    }
        
        return ordersPage.map(order -> {
            OrderPayment orderPayment = new OrderPayment(order);
          if (status == OrderStatus.CHO_THANH_TOAN_DAU_GIA) {

    Optional<Payment> payment = paymentRepository.findPaymentForOrder(
            order.getOrderId(),
            PaymentStatus.CHO_THANH_TOAN.name()
    );
    orderPayment.setPaymentCode(payment.map(Payment::getPaymentCode).orElse(null));
    return orderPayment;
}

            if (status == OrderStatus.DA_DU_HANG || status == OrderStatus.CHO_THANH_TOAN_SHIP) {
                BigDecimal totalNetWeight = order.getWarehouses().stream()
                        .map(warehouse -> BigDecimal.valueOf(warehouse.getNetWeight()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

                if (totalNetWeight.compareTo(BigDecimal.valueOf(0.5)) < 0) {
                    totalNetWeight = BigDecimal.valueOf(0.5);
                } else if (totalNetWeight.compareTo(BigDecimal.valueOf(0.5)) >= 0 && totalNetWeight.compareTo(BigDecimal.ONE) < 0) {
                    totalNetWeight = BigDecimal.ONE;
                }

                orderPayment.setTotalNetWeight(totalNetWeight);
                if (order.getExchangeRate() != null) {
                    BigDecimal calculatedPrice = totalNetWeight.multiply(order.getPriceShip()).setScale(2, RoundingMode.HALF_UP);
                    orderPayment.setFinalPriceOrder(calculatedPrice);
                } else {
                    orderPayment.setFinalPriceOrder(null);
                }
            }
            if (status == OrderStatus.CHO_THANH_TOAN || status == OrderStatus.CHO_THANH_TOAN_SHIP) {
                Optional<Payment> payment = order.getPayments().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.CHO_THANH_TOAN || p.getStatus() == PaymentStatus.CHO_THANH_TOAN_SHIP)
                        .findFirst();

                
                 if (payment.isPresent()) {
                orderPayment.setPaymentCode(payment.get().getPaymentCode());
            } else {
                // ⭐ CHỈ THÊM DÒNG NÀY
                orderPayment.setPaymentCode(
                        resolvePaymentCode(order)
                );
            }
        } else {
            orderPayment.setPaymentCode(null);
        }

        return orderPayment;
    });
    }

    @Transactional(readOnly = true)
    public OrderDetail getOrderDetail(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng này!"));

        Hibernate.initialize(order.getOrderLinks());
        order.getOrderLinks().forEach(link -> {
            if (link.getWarehouse() != null) {
                Hibernate.initialize(link.getWarehouse());
            }
            if (link.getPurchase() != null) {
                Hibernate.initialize(link.getPurchase());
            }
        });

        Hibernate.initialize(order.getPurchases());
        Hibernate.initialize(order.getOrderProcessLogs());
        Hibernate.initialize(order.getShipmentTrackings());

        Set<Payment> allPayments = new HashSet<>();

        if (order.getPayments() != null) {
            allPayments.addAll(order.getPayments());
        }

        List<Payment> mergedPayments = paymentRepository.findByRelatedOrdersContaining(order);
        allPayments.addAll(mergedPayments);

        OrderDetail detail = new OrderDetail(order);
        detail.setPayments(allPayments);

        return detail;
    }

    public Page<OrderWithLinks> getOrdersWithLinksForPurchaser(
        Pageable pageable,
        OrderType orderType,
        String orderCode,
        String customerCode
) {
    Account currentAccount = accountUtils.getAccountCurrent();

    if (!currentAccount.getRole().equals(AccountRoles.STAFF_PURCHASER)) {
        throw new AccessDeniedException("Chỉ nhân viên mua hàng mới có quyền truy cập!");
    }

    List<AccountRoute> accountRoutes =
            accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());

    Set<Long> routeIds = accountRoutes.stream()
            .map(AccountRoute::getRoute)
            .map(Route::getRouteId)
            .collect(Collectors.toSet());

    if (routeIds.isEmpty()) {
        return Page.empty(pageable);
    }

    if (orderCode != null && orderCode.trim().isEmpty()) {
        orderCode = null;
    }
    if (customerCode != null && customerCode.trim().isEmpty()) {
        customerCode = null;
    }

    Sort sort = Sort.by(Sort.Order.desc("pinnedAt").nullsLast())
            .and(Sort.by(Sort.Order.desc("createdAt")));

    Pageable customPageable =
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

    Page<Orders> ordersPage =
            ordersRepository.findByRouteAndStatusAndTypeWithSearch(
                    routeIds,
                    OrderStatus.CHO_MUA,
                    orderType,
                    orderCode,
                    customerCode,
                    customPageable
            );

    return ordersPage.map(orders -> {
        OrderWithLinks dto = new OrderWithLinks(orders);

        List<OrderLinks> sortedLinks = new ArrayList<>(orders.getOrderLinks());
        sortedLinks.sort(
                Comparator.comparing((OrderLinks link) -> {
                    if (link.getStatus() == OrderLinkStatus.CHO_MUA) return 0;
                    if (link.getStatus() == OrderLinkStatus.DA_MUA) return 1;
                    return 2;
                }).thenComparing(
                        OrderLinks::getGroupTag,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
        );

        dto.setOrderLinks(sortedLinks);
        dto.setPinnedAt(orders.getPinnedAt());
        return dto;
    });
}

    public OrderLinkWithStaff getOrderLinkById(Long orderLinkId) {
//        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
//                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm này!"));
//        return orderLink;
        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm này!"));

        Staff staff = orderLink.getOrders().getStaff();

        Customer customer = orderLink.getOrders().getCustomer();

        if (staff == null) {
            throw new NotFoundException("Không tìm thấy thông tin nhân viên liên quan!");
        }
        OrderLinkWithStaff orderLinkWithStaff = new OrderLinkWithStaff();
        orderLinkWithStaff.setOrderLink(orderLink);
        orderLinkWithStaff.setStaff(staff);
        orderLinkWithStaff.setCustomer(customer);
        return orderLinkWithStaff;
    }
    
    public Map<String, Long> getOrderStatusStatistics() {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new AccessDeniedException("Chỉ nhân viên mới có quyền truy cập thống kê này!");
        }
        Long staffId = currentAccount.getAccountId();

        List<OrderStatus> statusesToCount = Arrays.asList(
                OrderStatus.DA_XAC_NHAN,
                OrderStatus.CHO_THANH_TOAN,
                OrderStatus.CHO_THANH_TOAN_DAU_GIA,
                OrderStatus.DA_DU_HANG,
                OrderStatus.CHO_THANH_TOAN_SHIP
        );

        Map<String, Long> statistics = new HashMap<>();
        for (OrderStatus status : statusesToCount) {
            long count = ordersRepository.countByStaffAccountIdAndStatus(staffId, status);
            statistics.put(status.name(), count);
        }

        return statistics;
    }

    public List<OrderPayment> getOrdersByCustomerCode(String customerCode) {
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new NotFoundException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
        }

        List<Orders> orders = ordersRepository.findByCustomerCodeAndStatus(customerCode, OrderStatus.DA_XAC_NHAN);

        return orders.stream()
                .map(order -> {
                    OrderPayment orderPayment = new OrderPayment(order);
                    return orderPayment;
                })
                .collect(Collectors.toList());
    }

    public List<OrderPayment> getAfterPaymentAuctionsByCustomerCode(String customerCode) {
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new NotFoundException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
        }

        List<Orders> orders = ordersRepository.findByCustomerCodeAndStatus(customerCode, OrderStatus.DAU_GIA_THANH_CONG);

        return orders.stream()
                .map(order -> {
                    OrderPayment orderPayment = new OrderPayment(order);
                    return orderPayment;
                })
                .collect(Collectors.toList());
    }

public List<WareHouseOrderLink> getLinksInWarehouseByCustomer(String customerCode) {

    Customer customer = authenticationRepository.findByCustomerCode(customerCode);
    if (customer == null) {
        throw new NotFoundException("Mã khách hàng không được tìm thấy!");
    }

    if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
        throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng này!");
    }

    List<OrderLinks> orderLinks =
            orderLinksRepository.findLinksInWarehouseWithoutPartialShipment(
                    customerCode,
                    OrderLinkStatus.DA_NHAP_KHO_VN
            );

    if (orderLinks.isEmpty()) {
        return Collections.emptyList();
    }

    orderLinks.forEach(l -> Hibernate.initialize(l.getWarehouse()));

    // === LẤY WAREHOUSE KHÔNG TRÙNG TRACKING ===
    Map<String, Warehouse> warehouseMap = new LinkedHashMap<>();
    for (OrderLinks link : orderLinks) {
        Warehouse wh = link.getWarehouse();
        warehouseMap.putIfAbsent(wh.getTrackingCode(), wh);
    }

    List<Warehouse> warehouses = new ArrayList<>(warehouseMap.values());

    // === TÍNH TỔNG PHÍ SHIP (CHUẨN NHƯ CREATE) ===
    BigDecimal totalShippingFee = partialShipmentService.calculateTotalShippingFee(
            warehouses.stream()
                    .map(Warehouse::getTrackingCode)
                    .toList()
    );

    BigDecimal totalNetWeight = warehouses.stream()
            .map(w -> BigDecimal.valueOf(w.getNetWeight()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // === ĐÁNH DẤU SHIPMENT ĐÃ TÍNH ===
    Set<String> calculatedShipmentCodes = new HashSet<>();

    return orderLinks.stream()
            .map(link -> {

                Warehouse wh = link.getWarehouse();
                String shipmentCode = link.getShipmentCode();

                BigDecimal finalShip = BigDecimal.ZERO;

                if (!calculatedShipmentCodes.contains(shipmentCode)) {

                    BigDecimal ratio = BigDecimal.valueOf(wh.getNetWeight())
                            .divide(totalNetWeight, 6, RoundingMode.HALF_UP);

                   finalShip = roundToHundreds(totalShippingFee.multiply(ratio));


                    calculatedShipmentCodes.add(shipmentCode);
                }

                WareHouseOrderLink dto = new WareHouseOrderLink();

                // warehouse
                dto.setWarehouseId(wh.getWarehouseId());
                dto.setLength(wh.getLength());
                dto.setWidth(wh.getWidth());
                dto.setHeight(wh.getHeight());
                dto.setWeight(wh.getWeight());
                dto.setDim(wh.getDim());
                dto.setNetWeight(wh.getNetWeight());

                // link
                dto.setLinkId(link.getLinkId());
                dto.setProductLink(link.getProductLink());
                dto.setProductName(link.getProductName());
                dto.setQuantity(link.getQuantity());
                dto.setPriceWeb(link.getPriceWeb());
                dto.setShipWeb(link.getShipWeb());
                dto.setTotalWeb(link.getTotalWeb());
                dto.setPurchaseFee(link.getPurchaseFee());
                dto.setExtraCharge(link.getExtraCharge());
                dto.setFinalPriceVnd(link.getFinalPriceVnd());

                // ✅ ship đồng bộ 100%
                dto.setFinalPriceShip(finalShip);

                dto.setTrackingCode(link.getTrackingCode());
                dto.setShipmentCode(shipmentCode);
                dto.setStatus(link.getStatus());
                dto.setNote(link.getNote());
                dto.setGroupTag(link.getGroupTag());

                return dto;
            })
            .collect(Collectors.toList());
}


    public List<OrderPayment> getOrdersShippingByCustomerCode(String customerCode) {
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new NotFoundException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
        }

        List<Orders> orders = ordersRepository.findByCustomerCodeAndStatus(customerCode, OrderStatus.DA_DU_HANG);

        return orders.stream()
                .map(order -> {
                    OrderPayment orderPayment = new OrderPayment(order);

                    BigDecimal rawTotalWeight = order.getWarehouses() != null && !order.getWarehouses().isEmpty()
                            ? order.getWarehouses().stream()
                            .map(Warehouse::getNetWeight)
                            .filter(Objects::nonNull)
                            .map(BigDecimal::valueOf)                 // ← an toàn tuyệt đối
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            : BigDecimal.ZERO;

                    BigDecimal totalWeight;
                    if (rawTotalWeight.compareTo(BigDecimal.ONE) < 0) {
                        if (orders.get(0).getRoute().getName().equals("JPY")){
                            if (rawTotalWeight.compareTo(new BigDecimal("0.5")) <= 0) {
                                totalWeight = new BigDecimal("0.5");
                            } else {
                                totalWeight = BigDecimal.ONE;
                            }
                        } else {
                            totalWeight = BigDecimal.ONE;
                        }
                    } else {
                        totalWeight = rawTotalWeight.setScale(1, RoundingMode.HALF_UP);
                    }

                    orderPayment.setTotalNetWeight(rawTotalWeight);

                    BigDecimal unitPrice = order.getPriceShip();
                    BigDecimal finalPriceOrder = totalWeight.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    orderPayment.setFinalPriceOrder(finalPriceOrder);
                    orderPayment.setLeftoverMoney(order.getLeftoverMoney());
                    return orderPayment;
                })
                .collect(Collectors.toList());
    }

    public Orders updateOrderLinkToBuyLater(Long orderId, Long orderLinkId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng này!"));

        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy link sản phẩm!"));

        if (!orderLink.getStatus().equals(OrderLinkStatus.CHO_MUA)) {
            throw new BadRequestException("Chỉ có thể chuyển sang MUA SAU nếu trạng thái hiện tại là CHỜ MUA!");
        }
        orderLink.setStatus(OrderLinkStatus.MUA_SAU);
        orderLinksRepository.save(orderLink);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());

        long countMuaSau = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.MUA_SAU)
                .count();

        long countDaHuy = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_HUY)
                .count();

        long countDaMua = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_MUA)
                .count();

        if (countMuaSau + countDaHuy == allOrderLinks.size()) {
            order.setStatus(OrderStatus.CHO_MUA);
            ordersRepository.save(order);
        } else if (countDaMua > 0 && (countDaMua + countDaHuy + countMuaSau == allOrderLinks.size())) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
        }

//        addProcessLog(order, order.getOrderCode(), ProcessLogAction.CAP_NHAT_TRANG_THAI_LINK);
        return order;
    }

    public void pinOrder(Long orderId, boolean pin) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng!"));
        order.setPinnedAt(pin ? LocalDateTime.now() : null);
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", pin ? "PIN" : "UNPIN",
                        "orderCode", order.getOrderCode(),
                        "customerCode", order.getCustomer().getCustomerCode(),
                        "message", pin ? "Đơn hàng đã được ghim!" : "Đơn hàng đã được bỏ ghim!"
                )
        );
        ordersRepository.save(order);
    }

    public List<OrderPayment> getReadyOrdersForPartial(Pageable pageable) {
        List<OrderStatus> statuses = Arrays.asList(OrderStatus.DA_DU_HANG, OrderStatus.DANG_XU_LY);
        Page<Orders> ordersPage = ordersRepository.findByStatusIn(statuses, pageable);

        return ordersPage.getContent().stream()
                .filter(order -> order.getOrderLinks().stream()
                        .anyMatch(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN))
                .map(order -> {
                    OrderPayment orderPayment = new OrderPayment(order);

                    BigDecimal totalNetWeight = order.getOrderLinks().stream()
                            .filter(link -> link.getStatus() == OrderLinkStatus.DA_NHAP_KHO_VN)
                            .map(OrderLinks::getWarehouse)
                            .filter(Objects::nonNull)
                            .map(Warehouse::getNetWeight)
                            .filter(Objects::nonNull)
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    orderPayment.setTotalNetWeight(totalNetWeight);

                    Route route = order.getRoute();
                    BigDecimal unitPrice = (order.getOrderType() == OrderType.KY_GUI && route.getUnitDepositPrice() != null)
                            ? route.getUnitDepositPrice()
                            : route.getUnitBuyingPrice() != null ? route.getUnitBuyingPrice() : BigDecimal.ZERO;

                    BigDecimal finalPriceOrder = totalNetWeight.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    orderPayment.setFinalPriceOrder(finalPriceOrder);
                    orderPayment.setLeftoverMoney(order.getLeftoverMoney());

                    return orderPayment;
                })
                .collect(Collectors.toList());
    }

//    public Page<RefundResponse> getOrdersWithNegativeLeftoverMoney(Pageable pageable) {
//        Account currentAccount = accountUtils.getAccountCurrent();
//        if (!(currentAccount instanceof Staff)) {
//            throw new IllegalStateException("Chỉ nhân viên mới có quyền truy cập danh sách đơn hàng này!");
//        }
//        Staff staff = (Staff) currentAccount;
//        Long staffId = staff.getAccountId();
//
////        List<OrderStatus> statuses = Arrays.asList(OrderStatus.DA_HUY, OrderStatus.DA_GIAO);
//
//        AccountRoles role = staff.getRole();
//
//        if (AccountRoles.MANAGER.equals(role)) {
//            return ordersRepository.findByLeftoverMoneyLessThan(BigDecimal.ZERO, pageable);
//        } else if (AccountRoles.STAFF_SALE.equals(role) || AccountRoles.LEAD_SALE.equals(role)) {
//            return ordersRepository.findByStaffAccountIdAndLeftoverMoneyLessThan(staffId, BigDecimal.ZERO, pageable);
//        } else {
//            throw new IllegalStateException("Vai trò không hợp lệ!");
//        }
//    }

    public Page<RefundResponse> getOrdersWithNegativeLeftoverMoney(Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new AccessDeniedException("Chỉ nhân viên mới có quyền truy cập danh sách đơn hàng này!");
        }
        Staff staff = (Staff) currentAccount;
        Long staffId = staff.getAccountId();
        AccountRoles role = staff.getRole();

        Page<Orders> ordersPage;

        if (AccountRoles.MANAGER.equals(role)) {
            ordersPage = ordersRepository.findOrdersWithRefundableCancelledLinks(
                    BigDecimal.ZERO, pageable);
        } else if (AccountRoles.STAFF_SALE.equals(role) || AccountRoles.LEAD_SALE.equals(role)) {
            ordersPage = ordersRepository.findByStaffIdAndRefundableCancelledLinks(
                    staffId, BigDecimal.ZERO, pageable);
        } else {
            throw new AccessDeniedException("Vai trò không hợp lệ!");
        }

        Page<RefundResponse> result = ordersPage.map(order -> {
            RefundResponse response = new RefundResponse();
            response.setOrder(order);

            List<OrderLinks> cancelledLinks = order.getOrderLinks().stream()
                    .filter(link -> link.getStatus() == OrderLinkStatus.DA_HUY)
                    .toList();

            response.setCancelledLinks(cancelledLinks);
            return response;
        });

        return result;
    }

    public Orders processNegativeLeftoverMoney(Long orderId, String image, boolean refundToCustomer) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng này!"));

        if (order.getLeftoverMoney() == null || order.getLeftoverMoney().compareTo(BigDecimal.ZERO) >= 0) {
            throw new BadRequestException("Đơn hàng này không có tiền hoàn trả!");
        }

        BigDecimal amountToProcess = order.getLeftoverMoney().abs();
        Customer customer = order.getCustomer();

        Payment refundPayment = new Payment();
        refundPayment.setPaymentCode(paymentService.generatePaymentCode());
        refundPayment.setPaymentType(PaymentType.MA_QR);
        refundPayment.setAmount(amountToProcess.negate());
        refundPayment.setStatus(PaymentStatus.DA_HOAN_TIEN);
        refundPayment.setActionAt(LocalDateTime.now());
        refundPayment.setCustomer(customer);
        refundPayment.setStaff((Staff) accountUtils.getAccountCurrent());
        refundPayment.setOrders(order);
        refundPayment.setIsMergedPayment(false);

        if (refundToCustomer) {
            refundPayment.setContent("Hoàn tiền cho đơn " + order.getOrderCode());
            refundPayment.setQrCode(image);
            refundPayment.setCollectedAmount(amountToProcess.negate());
            paymentRepository.save(refundPayment);
        } else {
            customer.setBalance(customer.getBalance().add(amountToProcess));
            refundPayment.setContent("Chuyển vào số dư cho đơn " + order.getOrderCode());
            refundPayment.setCollectedAmount(BigDecimal.ZERO);
            paymentRepository.save(refundPayment);
        }

        order.setLeftoverMoney(BigDecimal.ZERO);

        authenticationRepository.save(customer);
        ordersRepository.save(order);

        addProcessLog(order, order.getOrderCode(), ProcessLogAction.HOAN_TIEN);
        return order;
    }

    public Page<OrderWithLinks> getOrdersWithBuyLaterLinks(Pageable pageable, OrderType orderType) {
        Account currentAccount = accountUtils.getAccountCurrent();

        if (!currentAccount.getRole().equals(AccountRoles.STAFF_PURCHASER)) {
            throw new AccessDeniedException("Chỉ nhân viên mua hàng mới có quyền truy cập!");
        }

        List<AccountRoute> accountRoutes = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());
        Set<Long> routeIds = accountRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());
        if (routeIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Sort sort = Sort.by(
                Sort.Order.desc("pinnedAt").nullsLast(),
                Sort.Order.desc("createdAt")
        );

        Pageable customPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        Page<Orders> ordersPage = ordersRepository.findProcessingOrdersWithBuyLaterLinks(
                routeIds, orderType, customPageable
        );

        return ordersPage.map(orders -> {
            OrderWithLinks dto = new OrderWithLinks(orders);
            List<OrderLinks> buyLaterLinks = orders.getOrderLinks().stream()
                    .filter(link -> link.getStatus() == OrderLinkStatus.MUA_SAU)
                    .sorted(Comparator.comparing(
                            OrderLinks::getGroupTag,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ))
                    .collect(Collectors.toList());
            dto.setOrderLinks(buyLaterLinks);
            return dto;
        });
    }

    public Page<ShipLinkForegin> getOrderLinksForForeignWarehouse(
        Pageable pageable,
        String shipmentCode,
        String customerCode
    ) {
    Account currentAccount = accountUtils.getAccountCurrent();
    if (!currentAccount.getRole().equals(AccountRoles.STAFF_WAREHOUSE_FOREIGN)) {
        throw new AccessDeniedException("Chỉ nhân viên kho nước ngoài mới có quyền truy cập!");
    }
      List<AccountRoute> accountRoutes = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId());
        Set<Long> routeIds = accountRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        if (routeIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<OrderLinkStatus> statuses = new ArrayList<>();
        statuses.add(OrderLinkStatus.DA_MUA);

         Page<Orders> ordersPage = ordersRepository.filterOrdersByLinkStatusAndRoutes(
            statuses,
            (shipmentCode == null || shipmentCode.isBlank()) ? null : shipmentCode,
            (customerCode == null || customerCode.isBlank()) ? null : customerCode,
            routeIds,
            pageable
    );
    List<ShipLinkForegin> result = new ArrayList<>();

    for (Orders order : ordersPage.getContent()) {

        List<OrderLinks> validLinks = order.getOrderLinks().stream()
                .filter(link -> statuses.contains(link.getStatus()))
                .filter(link -> shipmentCode == null 
                        || link.getShipmentCode() != null 
                        || link.getShipmentCode().contains(shipmentCode)) 
                .sorted(Comparator.comparing(
                        OrderLinks::getGroupTag,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        if (validLinks.isEmpty())
            continue;
         List<OrderLinksShipForeign> orderLinksShips = validLinks.stream()
                .map(link -> {
                    return new OrderLinksShipForeign(link);
                })
                .toList();
        ShipLinkForegin dto = new ShipLinkForegin(order,orderLinksShips);
        result.add(dto);
    }
    return new PageImpl<>(result, pageable, ordersPage.getTotalElements());
}

    public Page<ShipLinks> getOrderLinksForWarehouse(
        Pageable pageable,
        ShipStatus status,
        String shipmentCode,
        String customerCode
) {
    Account currentAccount = accountUtils.getAccountCurrent();
    if (!currentAccount.getRole().equals(AccountRoles.STAFF_WAREHOUSE_DOMESTIC)) {
        throw new AccessDeniedException("Chỉ nhân viên kho mới có quyền truy cập!");
    }

    List<OrderLinkStatus> statuses =
            (status == null) ? DEFAULT_SHIP_STATUSES : List.of(convert(status));

    Page<Orders> ordersPage = ordersRepository.filterOrdersByLinkStatus(
            statuses,
            (shipmentCode == null || shipmentCode.isBlank()) ? null : shipmentCode,
            (customerCode == null || customerCode.isBlank()) ? null : customerCode,
            pageable
    );

    List<ShipLinks> result = new ArrayList<>();

    Set<String> seenShipmentCodes = new HashSet<>();

    for (Orders order : ordersPage.getContent()) {

        List<OrderLinks> validLinks = order.getOrderLinks().stream()
                .filter(link -> statuses.contains(link.getStatus()))
                .filter(link ->
                        shipmentCode == null
                                || (link.getShipmentCode() != null
                                && link.getShipmentCode().contains(shipmentCode))
                )
                
                .filter(link -> {
                    String code = link.getShipmentCode();
                    if (code == null) return false;
                    return seenShipmentCodes.add(code); 
                })
                .sorted(Comparator.comparing(
                        OrderLinks::getGroupTag,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        if (validLinks.isEmpty()) continue;

        List<OrderLinksShip> orderLinksShips = validLinks.stream()
                .map(link -> {
                    Warehouse wh = link.getWarehouse();
                    String packingCode =
                            (wh != null && wh.getPacking() != null)
                                    ? wh.getPacking().getPackingCode()
                                    : null;

                    return new OrderLinksShip(link, wh, packingCode);
                })
                .toList();

        ShipLinks dto = new ShipLinks(
                order,
                orderLinksShips,
                order.getWarehouses().stream().toList()
        );

        result.add(dto);
    }

    return new PageImpl<>(result, pageable, ordersPage.getTotalElements());
}

    public InfoShipmentCode inforShipmentCode(String shipmentCode) {
        List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
        InfoShipmentCode infoShipmentCode = new InfoShipmentCode();
        if (!orderLinks.isEmpty()){
            Orders orders = orderLinks.get(0).getOrders();
            String customerCode = orders.getStaff().getStaffCode() + "-" + orders.getCustomer().getCustomerCode();
            infoShipmentCode.setOrders(orderLinks.get(0).getOrders());
            infoShipmentCode.setPrice(orderLinks.get(0).getPurchase() != null ? orderLinks.get(0).getPurchase().getFinalPriceOrder() : BigDecimal.ZERO);
            infoShipmentCode.setDestinationName(infoShipmentCode.getOrders().getDestination().getDestinationName());
            infoShipmentCode.setCustomerCode(customerCode);
        } else {
            throw new NotFoundException("Không tìm thấy mã vận đơn này, vui lòng thử lại!");
        }
        return infoShipmentCode;
    }

    public CustomerBalanceAndOrders getOrdersWithNegativeLeftoverByCustomerCode(String customerCode) {
        Customer customer = customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new NotFoundException("không tìm thấy khách hàng với mã khách hàng: " + customerCode));

        List<Orders> orders = ordersRepository.findByCustomerAndLeftoverMoneyGreaterThan(
                customer, BigDecimal.ZERO);

        BigDecimal balance = customer.getBalance() != null ? customer.getBalance() : BigDecimal.ZERO;

        List<OrderPayment> orderPayments = orders.stream()
                .map(this::convertToOrderPayment)
                .collect(Collectors.toList());

        return new CustomerBalanceAndOrders(balance, orderPayments);
    }

    private OrderPayment convertToOrderPayment(Orders order) {
        OrderPayment payment = new OrderPayment(order);
        payment.setOrderId(order.getOrderId());
        payment.setLeftoverMoney(order.getLeftoverMoney());
        return payment;
    }

    public OrderByShipmentResponse getOrderByShipmentCode(String shipmentCode) {
        List<OrderLinks> links = orderLinksRepository.findByShipmentCode(shipmentCode);

        if (links.isEmpty()) {
            throw new NotFoundException("Không tìm thấy mã vận đơn: " + shipmentCode);
        }

        Orders order = links.get(0).getOrders();

        return new OrderByShipmentResponse(order, links);
    }

    @Transactional
    public List<Orders> updateDestinationByShipmentCodes(List<String> shipmentCodes, Long newDestinationId) {
        if (shipmentCodes == null || shipmentCodes.isEmpty()) {
            throw new BadRequestException("Danh sách mã vận đơn không được để trống!");
        }
        
        Destination newDestination = destinationRepository.findById(newDestinationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy điểm đến với ID: " + newDestinationId));
        
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff staff)) {
            throw new AccessDeniedException("Chỉ nhân viên mới được phép thực hiện thao tác này!");
        }
        Set<AccountRoles> allowedRoles = Set.of(AccountRoles.ADMIN, AccountRoles.MANAGER, AccountRoles.STAFF_SALE, AccountRoles.LEAD_SALE);
        if (!allowedRoles.contains(staff.getRole())) {
            throw new AccessDeniedException("Bạn không có quyền thay đổi điểm đến!");
        }
        
        List<OrderLinks> allLinks = orderLinksRepository.findAllByShipmentCodeIn(shipmentCodes);

        if (allLinks.isEmpty()) {
            throw new NotFoundException("Không tìm thấy bất kỳ mã vận đơn nào trong danh sách!");
        }
        
        Map<Orders, List<OrderLinks>> orderToLinksMap = allLinks.stream()
                .collect(Collectors.groupingBy(OrderLinks::getOrders));

        List<Orders> updatedOrders = new ArrayList<>();
        List<String> invalidCodes = new ArrayList<>();

        for (Map.Entry<Orders, List<OrderLinks>> entry : orderToLinksMap.entrySet()) {
            Orders order = entry.getKey();
            
            Destination oldDestination = order.getDestination();
            order.setDestination(newDestination);

            updatedOrders.add(order);

        }
        
        if (!invalidCodes.isEmpty()) {
            throw new BadRequestException("Không thể cập nhật điểm đến cho các đơn đã giao/hủy: " + String.join(", ", invalidCodes));
        }
        return ordersRepository.saveAll(updatedOrders);
    }

    private OrderLinkStatus convert(ShipStatus s) {
        return OrderLinkStatus.valueOf(s.name());
    }

    public Page<OrdersPendingShipment> getMyOrdersWithoutShipmentCode(Pageable pageable) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();

        Page<Orders> ordersPage = ordersRepository.findOrdersWithEmptyShipmentCodeByStaff(
                staff.getAccountId(), pageable);

        return ordersPage.map(order -> {
            List<OrderLinkPending> pendingLinks = order.getOrderLinks().stream()
                    .filter(link -> link.getShipmentCode() == null || link.getShipmentCode().trim().isEmpty())
                    .map(OrderLinkPending::new)
                    .toList();

            return new OrdersPendingShipment(order, pendingLinks);
        });
    }

    @Transactional
    public OrderWithLinks updateShipmentCode(Long orderId, Long orderLinkId, String shipmentCode) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff staff)) {
            throw new IllegalStateException("Chỉ nhân viên mới được thực hiện thao tác này!");
        }
    
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng: " + orderId));

        OrderLinks link = order.getOrderLinks().stream()
                .filter(l -> l.getLinkId().equals(orderLinkId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy link với ID: " + orderLinkId));

        String oldCode = link.getShipmentCode();
        link.setShipmentCode(shipmentCode);

        addProcessLog(order,
                "Cập nhật mã vận đơn: " + oldCode + " → " + shipmentCode +
                        " (Link: " + link.getProductName() + ")",
                ProcessLogAction.DA_CHINH_SUA);

        messagingTemplate.convertAndSend("/topic/Tiximax", Map.of(
                "event", "UPDATE_SHIPMENT",
                "orderCode", order.getOrderCode(),
                "linkId", orderLinkId,
                "shipmentCode", shipmentCode
        ));

        OrderWithLinks dto = new OrderWithLinks(order);
        List<OrderLinkPending> pendingLinks = order.getOrderLinks().stream()
                .filter(l -> l.getShipmentCode() == null || l.getShipmentCode().trim().isEmpty())
                .map(OrderLinkPending::new)
                .toList();
        return dto;
    }

    public Page<OrderWithLinks> searchOrdersByKeyword(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(pageable);
        }

        Staff staff = (Staff) accountUtils.getAccountCurrent();
        boolean isAdminOrManager = Set.of(AccountRoles.ADMIN, AccountRoles.MANAGER)
                .contains(staff.getRole());

        String cleanKeyword = keyword.trim();

        Page<Orders> ordersPage = ordersRepository.searchOrdersByCodeOrShipment(
                cleanKeyword,
                staff.getAccountId(),
                isAdminOrManager,
                pageable
        );

        return ordersPage.map(order -> {
            OrderWithLinks dto = new OrderWithLinks(order);

            List<OrderLinks> allLinks = order.getOrderLinks().stream()
                    .toList();

            dto.setOrderLinks(allLinks);
            return dto;
        });
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff staff)) {
            throw new AccessDeniedException("Chỉ nhân viên mới được phép thực hiện thao tác này!");
        }

        Set<AccountRoles> allowedRoles = Set.of(AccountRoles.ADMIN, AccountRoles.MANAGER, AccountRoles.STAFF_SALE, AccountRoles.LEAD_SALE);
        if (!allowedRoles.contains(staff.getRole())) {
            throw new AccessDeniedException("Bạn không có quyền xóa đơn hàng!");
        }

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));
        if (!order.getStatus().equals(OrderStatus.DA_XAC_NHAN)) {
            throw new BadRequestException("Không được xóa đơn hàng ở giai đoạn này!");
        }

        if (order.getOrderProcessLogs() != null) {
            processLogRepository.deleteAll(order.getOrderProcessLogs());
        }

        if (order.getOrderLinks() != null) {
            orderLinksRepository.deleteAll(order.getOrderLinks());
        }

        ordersRepository.delete(order);

        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "DELETE",
                        "orderCode", order.getOrderCode(),
                        "customerCode", order.getCustomer().getCustomerCode(),
                        "message", "Đơn hàng đã bị xóa!"
                )
        );
    }

   public Orders MoneyExchange(String customerCode, Long routeId, MoneyExchangeRequest ordersRequest) throws IOException {
    if (customerCode == null) {
        throw new BadRequestException("Bạn phải nhập mã khách hàng để thực hiện hành động này!");
    }

    if (routeId == null) {
        throw new BadRequestException("Bạn phải chọn tuyến hàng để tiếp tục!");
    }

    Customer customer = authenticationRepository.findByCustomerCode(customerCode);
    if (customer == null) {
        throw new NotFoundException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
    }

    Route route = routeRepository.findById(routeId)
        .orElseThrow(() -> new NotFoundException("không tìm thấy tuyến với id : " + routeId));
   Destination destination = destinationRepository.findById(1L)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy điểm đến!"));

    if (ordersRequest.getExchangeRate().compareTo(route.getExchangeRate()) < 0) {
        throw new BadRequestException("Tỉ giá không được nhỏ hơn giá cố định, liên hệ quản lý để được hỗ trợ thay đổi tỉ giá!");
    }

    Orders order = new Orders();
    order.setCustomer(customer);
    order.setOrderCode(generateOrderCode(OrderType.CHUYEN_TIEN)); 
    order.setOrderType(OrderType.CHUYEN_TIEN);
    order.setDestination(destination); 
    order.setStatus(OrderStatus.DA_XAC_NHAN);
    order.setCreatedAt(LocalDateTime.now());
    order.setExchangeRate(ordersRequest.getExchangeRate());
    order.setCheckRequired(false);
    order.setPriceShip(BigDecimal.ZERO);
    order.setRoute(route);
    order.setAddress(null);
    order.setStaff((Staff) accountUtils.getAccountCurrent());

    // Khởi tạo các giá trị tính toán
    BigDecimal totalPriceVnd = BigDecimal.ZERO;
    BigDecimal priceBeforeFee = BigDecimal.ZERO;

    OrderLinks orderLink = new OrderLinks();
    orderLink.setOrders(order);
    orderLink.setProductLink(null);
    orderLink.setQuantity(1); 
    orderLink.setPriceWeb(BigDecimal.ZERO); 
    orderLink.setShipWeb(BigDecimal.ZERO); 
    orderLink.setTotalWeb(ordersRequest.getMoneyExChange()); 
    orderLink.setPurchaseFee(ordersRequest.getFee()); 
    orderLink.setProductName("Money Exchange"); 
    orderLink.setPurchaseImage(ordersRequest.getImage());
    orderLink.setWebsite(null);
    orderLink.setProductType(null);
    orderLink.setClassify("Money Exchange"); 
    orderLink.setStatus(OrderLinkStatus.CHO_MUA);
    orderLink.setNote(ordersRequest.getNote());
    orderLink.setGroupTag("ME");
    orderLink.setTrackingCode(generateOrderLinkCode()); 
    orderLink.setExtraCharge(BigDecimal.ZERO); 
    orderLink.setFinalPriceVnd(
        orderLink.getTotalWeb().add(orderLink.getPurchaseFee()).multiply(order.getExchangeRate()) 
    );
    totalPriceVnd = orderLink.getTotalWeb().add(orderLink.getPurchaseFee()).multiply(order.getExchangeRate());
    priceBeforeFee = orderLink.getTotalWeb();

    Set<OrderLinks> orderLinksList = new HashSet<>();
    orderLinksList.add(orderLink);

    order.setOrderLinks(orderLinksList);
    order.setFinalPriceOrder(totalPriceVnd); 
    order.setPriceBeforeFee(priceBeforeFee); 
    order = ordersRepository.save(order); 
    orderLinksRepository.save(orderLink); 

    addProcessLog(order, order.getOrderCode(), ProcessLogAction.XAC_NHAN_DON);
    messagingTemplate.convertAndSend(
        "/topic/Tiximax",
        Map.of(
            "event", "INSERT",
            "orderCode", order.getOrderCode(),
            "customerCode", customerCode,
            "message", "Đơn hàng mới được thêm!"
        )
    );

    return order;
}

//    public List<ShipmentGroup> getShipmentsByCustomerPhone(String phone) {
//        Account account = authenticationRepository.findByPhone(phone);
//        if (account == null || !(account instanceof Customer customer)) {
//            throw new NotFoundException("Không tìm thấy khách hàng với số điện thoại: " + phone);
//        }
//
//        List<OrderLinks> links = orderLinksRepository.findByCustomerWithShipment(customer);
//
//        Map<String, ShipmentGroup> groups = new LinkedHashMap<>();
//
//        for (OrderLinks link : links) {
//            String code = link.getShipmentCode().trim();
//            groups.computeIfAbsent(code, k ->
//                            new ShipmentGroup(link.getOrders().getOrderCode(), code, link.getStatus()))
//                    .addProduct(link.getProductName(), link.getProductLink());
//        }
//
//        return new ArrayList<>(groups.values());
//    }

    public List<ShipmentGroup> getShipmentsByCustomerPhone(String phone) {
        Account account = authenticationRepository.findByPhone(phone);
        if (account == null || !(account instanceof Customer customer)) {
            throw new NotFoundException("Không tìm thấy khách hàng với số điện thoại: " + phone);
        }

        List<OrderLinks> links = orderLinksRepository.findByCustomerWithShipment(customer);

        Map<String, ShipmentGroup> groups = new LinkedHashMap<>();

        for (OrderLinks link : links) {
            String code = link.getShipmentCode().trim();

            ShipmentGroup group = groups.computeIfAbsent(code, k ->
                    new ShipmentGroup(link.getOrders().getOrderCode(), code, link.getStatus()));

            String productImage = null;
            String productImageCheck = null;

            Warehouse warehouse = link.getWarehouse();
            if (warehouse != null) {
                productImageCheck = warehouse.getImageCheck() != null
                        ? warehouse.getImageCheck()
                        : null;
                productImage = warehouse.getImage() != null
                        ? warehouse.getImage()
                        : null;
            }
            group.addProduct(
                    link.getProductName(),
                    link.getProductLink(),
                    productImage,
                    productImageCheck
            );
        }

        return new ArrayList<>(groups.values());
    }

    private String resolvePaymentCode(Orders order) {

    // 1️⃣ Payment gắn trực tiếp Order
    Optional<Payment> directPayment = order.getPayments().stream()
            .filter(p ->
                    p.getStatus() == PaymentStatus.CHO_THANH_TOAN
                            || p.getStatus() == PaymentStatus.CHO_THANH_TOAN_SHIP
            )
            .findFirst();

    if (directPayment.isPresent()) {
        return directPayment.get().getPaymentCode();
    }

    // 2️⃣ ⭐ Payment gắn qua PartialShipment
    Optional<Payment> partialPayment =
            paymentRepository.findPaymentByPartialShipment(
                    order.getOrderId(),
                    List.of(
                            PaymentStatus.CHO_THANH_TOAN,
                            PaymentStatus.CHO_THANH_TOAN_SHIP
                    )
            );

    if (partialPayment.isPresent()) {
        return partialPayment.get().getPaymentCode();
    }

    // 3️⃣ Merged payment (fallback)
    Optional<Payment> mergedPayment =
            paymentRepository.findMergedPaymentByOrderIdAndStatus(
                    order.getOrderId(),
                    PaymentStatus.CHO_THANH_TOAN
            );

    if (mergedPayment.isEmpty()) {
        mergedPayment =
                paymentRepository.findMergedPaymentByOrderIdAndStatus(
                        order.getOrderId(),
                        PaymentStatus.CHO_THANH_TOAN_SHIP
                );
    }

    return mergedPayment.map(Payment::getPaymentCode).orElse(null);
}

    private BigDecimal roundUp(BigDecimal value) {
        return value
                .divide(BigDecimal.valueOf(500), 0, RoundingMode.CEILING)
                .multiply(BigDecimal.valueOf(500));
    }
    private OrderValidation validateAndGetOrder(
            String customerCode,
            Long routeId,
            Long addressId,
            Long destinationId
    ) {
        if (customerCode == null || customerCode.isBlank()) {
            throw new BadRequestException(
                    "Bạn phải nhập mã khách hàng!"
            );
        }

        if (routeId == null) {
            throw new BadRequestException(
                    "Bạn phải chọn tuyến hàng!"
            );
        }

        if (addressId == null) {
            throw new BadRequestException(
                    "Bạn phải chọn địa chỉ giao hàng!"
            );
        }

        if (destinationId == null) {
            throw new BadRequestException(
                    "Bạn phải chọn điểm đến!"
            );
        }

        Customer customer =
                authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new NotFoundException(
                    "Mã khách hàng không được tìm thấy!"
            );
        }

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Không tìm thấy tuyến hàng!"
                        )
                );

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Địa chỉ giao hàng không phù hợp!"
                        )
                );

        Destination destination =
                destinationRepository.findById(destinationId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Không tìm thấy điểm đến!"
                                )
                        );

          return new OrderValidation(
                customer,
                route,
                address,
                destination
        );
    }
    private Orders createBaseOrder(
        Customer customer,
        Route route,
        Address address,
        Destination destination,
        OrderType orderType,
        OrderStatus status,
        BigDecimal priceShip,
        Boolean checkRequired
) {
    Orders order = new Orders();
    order.setCustomer(customer);
    order.setOrderCode(generateOrderCode(orderType));
    order.setOrderType(orderType);
    order.setStatus(status);
    order.setCreatedAt(LocalDateTime.now());
    order.setRoute(route);
    order.setDestination(destination);
    order.setAddress(address);
    order.setPriceShip(priceShip);
    order.setCheckRequired(checkRequired);
    order.setStaff((Staff) accountUtils.getAccountCurrent());
    return order;
}

private List<OrderLinks> buildOrderLinks(
        Orders order,
        List<OrderLinkRequest> requests
) {
    if (requests == null || requests.isEmpty()) {
        return List.of();
    }

    List<OrderLinks> result = new ArrayList<>();

    for (OrderLinkRequest r : requests) {
        ProductType productType =
                productTypeRepository.findById(r.getProductTypeId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Kiểu sản phẩm không được tìm thấy!"
                                )
                        );

        OrderLinks link = new OrderLinks();
        link.setOrders(order);
        link.setProductLink(r.getProductLink());
        link.setQuantity(r.getQuantity());
        link.setPriceWeb(r.getPriceWeb());
        link.setShipWeb(r.getShipWeb());
        link.setPurchaseFee(r.getPurchaseFee());
        link.setProductName(r.getProductName());
        link.setWebsite(String.valueOf(r.getWebsite()));
        link.setProductType(productType);
        link.setClassify(r.getClassify());
        link.setStatus(OrderLinkStatus.CHO_MUA);
        link.setNote(r.getNote());
        link.setGroupTag(r.getGroupTag());
        link.setTrackingCode(generateOrderLinkCode());
        link.setPurchaseImage(r.getPurchaseImage());
        link.setExtraCharge(r.getExtraCharge());

        BigDecimal totalWeb =
                r.getPriceWeb()
                        .multiply(BigDecimal.valueOf(r.getQuantity()))
                        .add(r.getShipWeb())
                        .setScale(2, RoundingMode.HALF_UP);

        link.setTotalWeb(totalWeb);

        BigDecimal finalPrice =
                totalWeb.multiply(order.getExchangeRate())
                        .add(
                                r.getExtraCharge()
                                        .multiply(BigDecimal.valueOf(r.getQuantity()))
                        )
                        .add(
                                r.getPurchaseFee()
                                        .multiply(BigDecimal.valueOf(0.01))
                                        .multiply(totalWeb)
                                        .multiply(order.getExchangeRate())
                        )
                        .setScale(2, RoundingMode.HALF_UP);

        link.setFinalPriceVnd(finalPrice);
        result.add(link);
    }
    return result;
}

private Orders finalizeOrder(
        Orders order,
        List<OrderLinks> links
) {
    BigDecimal total =
            links.stream()
                    .map(OrderLinks::getFinalPriceVnd)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

    order.setFinalPriceOrder(total);
    order.setOrderLinks(new HashSet<>(links));

    Orders saved = ordersRepository.save(order);
    orderLinksRepository.saveAll(links);

    addProcessLog(
            saved,
            saved.getOrderCode(),
            ProcessLogAction.XAC_NHAN_DON
    );
    return saved;
}
private List<OrderLinks> buildConsignmentLinks(
        Orders order,
        List<ConsignmentLinkRequest> requests
) {
    if (requests == null || requests.isEmpty()) {
        return List.of();
    }

    List<OrderLinks> result = new ArrayList<>();

    for (ConsignmentLinkRequest r : requests) {
        ProductType productType =
                productTypeRepository.findById(r.getProductTypeId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Kiểu sản phẩm không được tìm thấy!"
                                )
                        );

        OrderLinks link = new OrderLinks();
        link.setOrders(order);
        link.setQuantity(r.getQuantity());
        link.setProductName(r.getProductName());
        link.setProductType(productType);
        link.setStatus(OrderLinkStatus.DA_MUA);

        String trackingCode = generateOrderLinkCode();
        link.setTrackingCode(trackingCode);

        link.setShipmentCode(
                (r.getShipmentCode() == null || r.getShipmentCode().isBlank())
                        ? trackingCode
                        : r.getShipmentCode()
        );

        link.setFinalPriceVnd(
                r.getExtraCharge()
                        .add(r.getDifferentFee())
                        .setScale(2, RoundingMode.HALF_UP)
        );

        link.setNote(r.getNote());
        link.setPurchaseImage(r.getPurchaseImage());
        result.add(link);
    }
    return result;
}
@Transactional
public void updateOrderStatusIfCompleted(Long orderId) {

        long totalLinks =
                orderLinksRepository.countAllByOrderId(orderId);

        if (totalLinks == 0) {
            return; 
        }

        Set<OrderLinkStatus> finishedStatuses = Set.of(
                OrderLinkStatus.DA_GIAO,
                OrderLinkStatus.DA_HUY
        );
        long finishedLinks =
                orderLinksRepository.countFinishedByOrderId(
                        orderId,
                        finishedStatuses
                );
        if (totalLinks == finishedLinks) {
            Orders order = ordersRepository.findById(orderId)
                    .orElseThrow(() -> new NotFoundException("Order không tồn tại"));
            if (order.getStatus() != OrderStatus.DA_GIAO) {
                order.setStatus(OrderStatus.DA_GIAO);
            } else {
                order.setStatus(OrderStatus.DANG_XU_LY);
            }
                ordersRepository.save(order);
            }
        }
    
    @Transactional
    public void updateOrdersStatusAfterDeliveryByShipmentCodes(
            List<String> shipmentCodes
    ) {
        List<Long> orderIds =
                orderLinksRepository.findOrderIdsByShipmentCodes(shipmentCodes);

        for (Long orderId : orderIds) {
            updateOrderStatusIfCompleted(orderId);
        }
    }
    private BigDecimal roundToHundreds(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;

        return amount
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    @Transactional
    public Orders partialUpdate(Long orderId, OrdersPatchRequest patch) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
        Staff staff = (Staff) accountUtils.getAccountCurrent();

        if (!order.getStatus().equals(OrderStatus.DA_XAC_NHAN)){
            throw new IllegalArgumentException("Trạng thái đơn hiện tại không được phép chỉnh sửa!");
        }

        if (patch.getPriceShip() != null) {
            if (!staff.getRole().equals(AccountRoles.MANAGER)){
                if (order.getOrderType().equals(OrderType.MUA_HO)){
                    if (patch.getPriceShip().compareTo(order.getRoute().getUnitBuyingPrice()) < 0) {
                        throw new IllegalArgumentException("Giá cước không được nhỏ hơn giá cố định!");
                    }
                } else if (order.getOrderType().equals(OrderType.KY_GUI)){
                    if (patch.getPriceShip().compareTo(order.getRoute().getUnitDepositPrice()) < 0) {
                        throw new IllegalArgumentException("Giá cước không được nhỏ hơn giá cố định!");
                    }
                }
            }

            order.setPriceShip(patch.getPriceShip());
        }

        if (patch.getExchangeRate() != null) {
            if (!staff.getRole().equals(AccountRoles.MANAGER)){
                if (patch.getExchangeRate().compareTo(order.getRoute().getExchangeRate()) < 0) {
                    throw new IllegalArgumentException("Tỉ giá không được nhỏ hơn giá cố định!");
                }
            }
            order.setExchangeRate(patch.getExchangeRate());
        }

        if (patch.getCheckRequired() != null) {
            order.setCheckRequired(patch.getCheckRequired());
        }

        if (patch.getOrderLinks() != null && !patch.getOrderLinks().isEmpty()) {
            Set<OrderLinks> existingLinks = order.getOrderLinks();

            for (OrderLinkPatch linkPatch : patch.getOrderLinks()) {
                Long linkId = linkPatch.getOrderLinkId();
                if (linkId == null) {
                    throw new IllegalArgumentException("Không tìm thấy orderLink!");
                }

                OrderLinks link = existingLinks.stream()
                        .filter(l -> l.getLinkId().equals(linkId))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Lỗi không tìm thấy link"));
                if (linkPatch.getProductLink() != null){
                    link.setProductLink(linkPatch.getProductLink());
                }
                if (linkPatch.getProductName() != null){
                    link.setProductName(linkPatch.getProductName());
                }
                if (linkPatch.getPurchaseImage() != null){
                    link.setPurchaseImage(linkPatch.getPurchaseImage());
                }
                if (linkPatch.getWebsite() != null){
                    link.setWebsite(linkPatch.getWebsite());
                }
                if (linkPatch.getGroupTag() != null){
                    link.setGroupTag(linkPatch.getGroupTag());
                }

                boolean recalculateNeeded = false;
                if (linkPatch.getPriceWeb() != null) {
                    link.setPriceWeb(linkPatch.getPriceWeb());
                    recalculateNeeded = true;
                }
                if (linkPatch.getShipWeb() != null) {
                    link.setShipWeb(linkPatch.getShipWeb());
                    recalculateNeeded = true;
                }
                if (linkPatch.getPurchaseFee() != null) {
                    link.setPurchaseFee(linkPatch.getPurchaseFee());
                    recalculateNeeded = true;
                }
                if (linkPatch.getExtraCharge() != null) {
                    link.setExtraCharge(linkPatch.getExtraCharge());
                    recalculateNeeded = true;
                }
                if (linkPatch.getQuantity() != null) {
                    link.setQuantity(linkPatch.getQuantity());
                    recalculateNeeded = true;
                }
                if (linkPatch.getNote() != null) {
                    link.setNote(linkPatch.getNote());
                }
                if (linkPatch.getClassify() != null) {
                    link.setClassify(linkPatch.getClassify());
                }

                if (recalculateNeeded) {
                    BigDecimal totalWeb = link.getPriceWeb()
                            .multiply(BigDecimal.valueOf(link.getQuantity()))
                            .add(link.getShipWeb())
                            .setScale(2, RoundingMode.HALF_UP);

                    link.setTotalWeb(totalWeb);

                    BigDecimal finalPrice = totalWeb.multiply(order.getExchangeRate())
                            .add(link.getExtraCharge().multiply(BigDecimal.valueOf(link.getQuantity())))
                            .add(link.getPurchaseFee()
                                    .multiply(BigDecimal.valueOf(0.01))
                                    .multiply(totalWeb)
                                    .multiply(order.getExchangeRate()))
                            .setScale(2, RoundingMode.HALF_UP);

                    link.setFinalPriceVnd(finalPrice);
                }
            }

            BigDecimal newPriceBeforeFee = existingLinks.stream()
                    .map(OrderLinks::getPriceWeb)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal newFinalPriceOrder = existingLinks.stream()
                    .map(OrderLinks::getFinalPriceVnd)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            order.setFinalPriceOrder(newFinalPriceOrder);
            order.setPriceBeforeFee(newPriceBeforeFee);
        }
        Orders saved = ordersRepository.save(order);
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "UPDATE",
                        "orderCode", saved.getOrderCode(),
                        "message", "Đơn hàng được cập nhật!"
                )
        );

        return saved;
    }

    public OrderWithLinks getOrderWithLinks(Long orderId) {
        Orders order = ordersRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        return new OrderWithLinks(order);
    }

}