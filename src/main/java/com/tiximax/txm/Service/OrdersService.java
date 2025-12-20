package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Model.*;
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
            OrderLinkStatus.DANG_GIAO,
            OrderLinkStatus.DA_GIAO
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
    private ImageStorageService imageStorageService;

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
        if (customerCode == null){
            throw new IllegalArgumentException("Bạn phải nhập mã khách hàng để thực hiện hành động này!");
        }
        if (routeId == null){
            throw new IllegalArgumentException("Bạn phải chọn tuyến hàng để tiếp tục!");
        }
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }
      
        Route route = routeRepository.findById(routeId).orElseThrow(() -> new RuntimeException("Route not found for ID: " + routeId));
        Optional<Destination> destination = destinationRepository.findById(ordersRequest.getDestinationId());

        if (destination.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy điểm đến!");
        }

        Optional<Address> address = addressRepository.findById(addressId);
        if (address.isEmpty()){
            throw new IllegalArgumentException("Địa chỉ giao hàng cho khách không phù hợp!");
        }

        if (ordersRequest.getPriceShip().compareTo(route.getUnitBuyingPrice()) < 0){
            throw new IllegalArgumentException("Giá cước không được nhỏ hơn giá cố định, liên hệ quản lý để được hỗ trợ thay đổi giá cước!");
        }

        if (ordersRequest.getExchangeRate().compareTo(route.getExchangeRate()) < 0){
            throw new IllegalArgumentException("Tỉ giá không được nhỏ hơn giá cố định, liên hệ quản lý để được hỗ trợ thay đổi tỉ giá!");
        }
        Orders order = new Orders();
        order.setCustomer(customer);
        order.setAddress(address.get());
        order.setOrderCode(generateOrderCode(ordersRequest.getOrderType()));
        order.setOrderType(ordersRequest.getOrderType());
        order.setStatus(OrderStatus.DA_XAC_NHAN);
        order.setCreatedAt(LocalDateTime.now());
        order.setExchangeRate(ordersRequest.getExchangeRate());
        order.setDestination(destination.get());
        order.setCheckRequired(ordersRequest.getCheckRequired());
        order.setPriceShip(ordersRequest.getPriceShip());
        order.setRoute(route);
        order.setStaff((Staff) accountUtils.getAccountCurrent());
        order.setAddress(address.get());
        BigDecimal totalPriceVnd = BigDecimal.ZERO;
        BigDecimal priceBeforeFee = BigDecimal.ZERO;

        List<OrderLinks> orderLinksList = new ArrayList<>();
        if (ordersRequest.getOrderLinkRequests() != null) {
            for (OrderLinkRequest linkRequest : ordersRequest.getOrderLinkRequests()) {
                OrderLinks orderLink = new OrderLinks();
                orderLink.setOrders(order);
                orderLink.setProductLink(linkRequest.getProductLink());
                orderLink.setQuantity(linkRequest.getQuantity());
                orderLink.setPriceWeb(linkRequest.getPriceWeb());
                orderLink.setShipWeb(linkRequest.getShipWeb());
//                orderLink.setTotalWeb((linkRequest.getPriceWeb().add(linkRequest.getShipWeb())).multiply(new BigDecimal(linkRequest.getQuantity())).setScale(2, RoundingMode.HALF_UP).add(linkRequest.getPurchaseFee()));
                orderLink.setTotalWeb(linkRequest.getPriceWeb().multiply(new BigDecimal(linkRequest.getQuantity())).add(linkRequest.getShipWeb()).setScale(2, RoundingMode.HALF_UP));
                orderLink.setPurchaseFee(linkRequest.getPurchaseFee());
                orderLink.setProductName(linkRequest.getProductName());
                ProductType productType = productTypeRepository.findById(linkRequest.getProductTypeId())
                        .orElseThrow(() -> new IllegalArgumentException("Kiểu sản phẩm không được tìm thấy!"));

        orderLink.setFinalPriceVnd(
        orderLink.getTotalWeb().multiply(order.getExchangeRate())
        .add(
            linkRequest.getExtraCharge()
                .multiply(new BigDecimal(linkRequest.getQuantity())) 
        )
        .add(
            linkRequest.getPurchaseFee()
                .multiply(new BigDecimal("0.01"))     
                .multiply(orderLink.getTotalWeb())      
                .multiply(order.getExchangeRate())      
                .setScale(2, RoundingMode.HALF_UP)
        )
);
                orderLink.setWebsite(String.valueOf(linkRequest.getWebsite()));
                orderLink.setProductType(productType);
                orderLink.setClassify(linkRequest.getClassify());
                orderLink.setStatus(OrderLinkStatus.CHO_MUA);
                orderLink.setNote(linkRequest.getNote());
                orderLink.setGroupTag(linkRequest.getGroupTag());
                orderLink.setTrackingCode(generateOrderLinkCode());
                orderLink.setPurchaseImage(linkRequest.getPurchaseImage());
                orderLink.setExtraCharge(linkRequest.getExtraCharge());
                orderLinksList.add(orderLink);
                BigDecimal finalPrice = orderLink.getFinalPriceVnd();
                if (finalPrice != null) {
                    totalPriceVnd = totalPriceVnd.add(finalPrice);
                    priceBeforeFee = priceBeforeFee.add(orderLink.getPriceWeb());
                }
            }
        }
        order.setOrderLinks(new HashSet<>(orderLinksList));
        order.setFinalPriceOrder(totalPriceVnd);
        order.setPriceBeforeFee(priceBeforeFee);
        order = ordersRepository.save(order);
        orderLinksRepository.saveAll(orderLinksList);
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

    public Orders addConsignment(String customerCode, Long routeId, Long addressId, ConsignmentRequest consignmentRequest) throws IOException {
        if (customerCode == null){
            throw new IllegalArgumentException("Bạn phải nhập mã khách hàng để thực hiện hành động này!");
        }
        if (routeId == null){
            throw new IllegalArgumentException("Bạn phải chọn tuyến hàng để tiếp tục!");
        }
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        Route route = routeRepository.findById(routeId).orElseThrow(() -> new RuntimeException("Route not found for ID: " + routeId));
        Optional<Destination> destination = destinationRepository.findById(consignmentRequest.getDestinationId());

//        if (ordersRequest.getPriceShip().compareTo(route.getUnitBuyingPrice()) < 0){
        // if (consignmentRequest.getPriceShip().compareTo(route.getUnitDepositPrice()) < 0){
        //     throw new IllegalArgumentException("Giá cước không được nhỏ hơn giá cố định, liên hệ quản lý để được hỗ trợ thay đổi giá cước!");
        // }

        if (destination.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy điểm đến!");
        }

        Optional<Address> address = addressRepository.findById(addressId);
        if (address.isEmpty()){
            throw new IllegalArgumentException("Địa chỉ giao hàng cho khách không phù hợp!");
        }

        Orders order = new Orders();
        order.setCustomer(customer);
        order.setOrderCode(generateOrderCode(consignmentRequest.getOrderType()));
        order.setOrderType(consignmentRequest.getOrderType());
        order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
        order.setCreatedAt(LocalDateTime.now());
        order.setPriceShip(consignmentRequest.getPriceShip());
        order.setDestination(destination.get());
        order.setCheckRequired(consignmentRequest.getCheckRequired());
        order.setRoute(route);
        order.setStaff((Staff) accountUtils.getAccountCurrent());
        order.setAddress(address.get());
        BigDecimal totalPriceVnd = BigDecimal.ZERO;

        List<OrderLinks> orderLinksList = new ArrayList<>();
if (consignmentRequest.getConsignmentLinkRequests() != null) {
    for (ConsignmentLinkRequest linkRequest : consignmentRequest.getConsignmentLinkRequests()) {

        OrderLinks orderLink = new OrderLinks();
        orderLink.setOrders(order);
        orderLink.setQuantity(linkRequest.getQuantity());
        orderLink.setProductName(linkRequest.getProductName());

        ProductType productType = productTypeRepository.findById(linkRequest.getProductTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Kiểu sản phẩm không được tìm thấy"));
        orderLink.setProductType(productType);

        orderLink.setStatus(OrderLinkStatus.DA_MUA);

        String trackingCode = generateOrderLinkCode();
        orderLink.setTrackingCode(trackingCode);

        String shipmentCode = linkRequest.getShipmentCode();
        if (shipmentCode == null || shipmentCode.trim().isEmpty()) {
            shipmentCode = trackingCode;
        }
        orderLink.setShipmentCode(shipmentCode);

        orderLink.setFinalPriceVnd(
                linkRequest.getExtraCharge()
                        .add(linkRequest.getDifferentFee())
                        .setScale(2, RoundingMode.HALF_UP)
        );

        orderLink.setNote(linkRequest.getNote());
        orderLink.setPurchaseImage(linkRequest.getPurchaseImage());

        orderLinksList.add(orderLink);

        BigDecimal finalPrice = orderLink.getFinalPriceVnd();
        if (finalPrice != null) {
            totalPriceVnd = totalPriceVnd.add(finalPrice);
        }
    }
}

        order.setOrderLinks(new HashSet<>(orderLinksList));
        order.setFinalPriceOrder(totalPriceVnd);
        order = ordersRepository.save(order);
        orderLinksRepository.saveAll(orderLinksList);
        addProcessLog(order, order.getOrderCode(), ProcessLogAction.XAC_NHAN_DON);
//        messagingTemplate.convertAndSend("/topic/orders", order);
        return order;
    }

    public Orders updateShipFee(Long orderId, BigDecimal shipFee) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));
   //     order.setShipFee(shipFee);
        ordersRepository.save(order);
        return order;
    }

    public Orders updateStatusOrderLink(Long OrderId,Long orderLinkId) {
        Orders order = ordersRepository.findById(OrderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));
        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng link"));
        
        if(orderLink.getStatus() == OrderLinkStatus.DA_HUY){
            throw new IllegalArgumentException("Đơn hàng link đã bị hủy, không thể hủy lại!");
        }
        orderLink.setStatus(OrderLinkStatus.DA_HUY);
        BigDecimal currentLeftover = order.getLeftoverMoney() != null ? order.getLeftoverMoney() : BigDecimal.ZERO;
        order.setLeftoverMoney(currentLeftover.subtract(orderLink.getFinalPriceVnd()));
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
//        addProcessLog(order, order.getOrderCode(), ProcessLogAction.DA_HUY_LINK);
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
                throw new IllegalStateException("Không có kiểu đơn hàng " + orderType);
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
        throw new IllegalStateException("Vai trò không hợp lệ!");
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
            throw new IllegalStateException("Vai trò không hợp lệ!");
        }
    }

    public List<Orders> getOrdersForCurrentStaff() {

        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff)) {
            throw new IllegalStateException("Tài khoản hiện tại không phải là nhân viên!");
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
        throw new IllegalArgumentException("Trạng thái không hợp lệ!");
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
                    BigDecimal calculatedPrice = totalNetWeight.multiply(order.getRoute().getUnitBuyingPrice()).setScale(2, RoundingMode.HALF_UP);
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
                    Optional<Payment> mergedPayment = paymentRepository.findMergedPaymentByOrderIdAndStatus(order.getOrderId(), PaymentStatus.CHO_THANH_TOAN);
                    if (!mergedPayment.isPresent()) {
                        mergedPayment = paymentRepository.findMergedPaymentByOrderIdAndStatus(order.getOrderId(), PaymentStatus.CHO_THANH_TOAN_SHIP);
                    }
                    orderPayment.setPaymentCode(mergedPayment.map(Payment::getPaymentCode).orElse(null));
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));

        // 1. Load tất cả cần thiết để tránh Lazy
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

        // 2. LẤY ĐỦ 2 LOẠI PAYMENT
        Set<Payment> allPayments = new HashSet<>();

        // Payment trực tiếp (order_id không null)
        if (order.getPayments() != null) {
            allPayments.addAll(order.getPayments());
        }

        // Payment gộp (qua bảng payment_orders)
        List<Payment> mergedPayments = paymentRepository.findByRelatedOrdersContaining(order);
        allPayments.addAll(mergedPayments);


        // 3. Tạo OrderDetail và truyền thêm allPayments nếu cần
        OrderDetail detail = new OrderDetail(order);
        detail.setPayments(allPayments); // ← Quan trọng!

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
        throw new IllegalStateException("Chỉ nhân viên mua hàng mới có quyền truy cập!");
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm này!"));

        Staff staff = orderLink.getOrders().getStaff();

        Customer customer = orderLink.getOrders().getCustomer();

        if (staff == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên liên quan!");
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
            throw new IllegalStateException("Chỉ nhân viên mới có quyền truy cập thống kê này!");
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
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
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
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
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
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
        }
        
        var orderLinks =  orderLinksRepository.findLinksInWarehouseWithoutPartialShipment(
                customerCode,
                OrderLinkStatus.DA_NHAP_KHO_VN
        );
             for (OrderLinks link : orderLinks) {
            Hibernate.initialize(link.getWarehouse()); // Ensures the warehouse is fetched
        }

        return orderLinks.stream()
    .map(link -> {
        WareHouseOrderLink warehouseOrderLink = new WareHouseOrderLink();
        warehouseOrderLink.setWarehouseId(link.getWarehouse().getWarehouseId());
        warehouseOrderLink.setLength(link.getWarehouse().getLength());
        warehouseOrderLink.setWidth(link.getWarehouse().getWidth());
        warehouseOrderLink.setHeight(link.getWarehouse().getHeight());
        warehouseOrderLink.setWeight(link.getWarehouse().getWeight());
        warehouseOrderLink.setDim(link.getWarehouse().getDim());
        warehouseOrderLink.setNetWeight(link.getWarehouse().getNetWeight());
        warehouseOrderLink.setLinkId(link.getLinkId());
        warehouseOrderLink.setProductLink(link.getProductLink());
        warehouseOrderLink.setProductName(link.getProductName());
        warehouseOrderLink.setQuantity(link.getQuantity());
        warehouseOrderLink.setPriceWeb(link.getPriceWeb());
        warehouseOrderLink.setShipWeb(link.getShipWeb());
        warehouseOrderLink.setTotalWeb(link.getTotalWeb());
        warehouseOrderLink.setPurchaseFee(link.getPurchaseFee());
        warehouseOrderLink.setExtraCharge(link.getExtraCharge());
        warehouseOrderLink.setFinalPriceVnd(link.getFinalPriceVnd());

        if (link.getWarehouse().getLength() != null && link.getWarehouse().getWidth() != null && link.getWarehouse().getHeight() != null) {
            BigDecimal volume = BigDecimal.valueOf(link.getWarehouse().getLength())
                .multiply(BigDecimal.valueOf(link.getWarehouse().getWidth()))
                .multiply(BigDecimal.valueOf(link.getWarehouse().getHeight()));

            BigDecimal finalPriceShip = volume.divide(BigDecimal.valueOf(6000), 2, RoundingMode.HALF_UP);
            BigDecimal intPart = finalPriceShip.setScale(0, RoundingMode.FLOOR); 
            BigDecimal decimalPart = finalPriceShip.subtract(intPart); 

        if (decimalPart.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            finalPriceShip = intPart.add(BigDecimal.valueOf(0.5)); 
        } else {
            finalPriceShip = intPart.add(decimalPart.setScale(1, RoundingMode.FLOOR)); 
        }
        warehouseOrderLink.setFinalPriceShip(finalPriceShip.multiply(orderLinks.get(0).getOrders().getPriceShip()));
        }
        warehouseOrderLink.setTrackingCode(link.getTrackingCode());
        warehouseOrderLink.setClassify(link.getClassify());
        warehouseOrderLink.setPurchaseImage(link.getPurchaseImage());
        warehouseOrderLink.setWebsite(link.getWebsite());
        warehouseOrderLink.setShipmentCode(link.getShipmentCode());
        warehouseOrderLink.setStatus(link.getStatus());
        warehouseOrderLink.setNote(link.getNote());
        warehouseOrderLink.setGroupTag(link.getGroupTag());
        return warehouseOrderLink;
    })
    .collect(Collectors.toList());
    }

    public List<OrderPayment> getOrdersShippingByCustomerCode(String customerCode) {
        Customer customer = authenticationRepository.findByCustomerCode(customerCode);
        if (customer == null) {
            throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
        }

        if (!customer.getStaffId().equals(accountUtils.getAccountCurrent().getAccountId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn hàng của khách hàng này!");
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));

        OrderLinks orderLink = orderLinksRepository.findById(orderLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy link sản phẩm!"));

        if (!orderLink.getStatus().equals(OrderLinkStatus.CHO_MUA)) {
            throw new IllegalArgumentException("Chỉ có thể chuyển sang MUA SAU nếu trạng thái hiện tại là CHỜ MUA!");
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));
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
            throw new IllegalStateException("Chỉ nhân viên mới có quyền truy cập danh sách đơn hàng này!");
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
            throw new IllegalStateException("Vai trò không hợp lệ!");
        }

        // Map sang RefundResponse, chỉ lấy các link DA_HUY
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));

        if (order.getLeftoverMoney() == null || order.getLeftoverMoney().compareTo(BigDecimal.ZERO) >= 0) {
            throw new IllegalArgumentException("Đơn hàng này không có tiền hoàn trả!");
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
            throw new IllegalStateException("Chỉ nhân viên mua hàng mới có quyền truy cập!");
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
        throw new IllegalStateException("Chỉ nhân viên kho nước ngoài mới có quyền truy cập!");
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
        throw new IllegalStateException("Chỉ nhân viên kho mới có quyền truy cập!");
    }

    List<OrderLinkStatus> statuses =
            (status == null) ? DEFAULT_SHIP_STATUSES : List.of(convert(status));

    // gọi repository đã thêm search
    Page<Orders> ordersPage = ordersRepository.filterOrdersByLinkStatus(
            statuses,
            (shipmentCode == null || shipmentCode.isBlank()) ? null : shipmentCode,
            (customerCode == null || customerCode.isBlank()) ? null : customerCode,
            pageable
    );

    List<ShipLinks> result = new ArrayList<>();

    for (Orders order : ordersPage.getContent()) {

        List<OrderLinks> validLinks = order.getOrderLinks().stream()
                .filter(link -> statuses.contains(link.getStatus()))
                .filter(link -> shipmentCode == null 
                        || link.getShipmentCode() != null 
                        || link.getShipmentCode().contains(shipmentCode)) // nếu muốn lọc lần 2
                .sorted(Comparator.comparing(
                        OrderLinks::getGroupTag,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        if (validLinks.isEmpty())
            continue;

        List<OrderLinksShip> orderLinksShips = validLinks.stream()
                .map(link -> {

                    Warehouse wh = link.getWarehouse();
                    String packingCode = (wh != null && wh.getPacking() != null)
                            ? wh.getPacking().getPackingCode()
                            : null;

                    return new OrderLinksShip(link, wh, packingCode);
                })
                .toList();

        ShipLinks dto = new ShipLinks(order, orderLinksShips, order.getWarehouses().stream().toList());
        result.add(dto);
    }

    return new PageImpl<>(result, pageable, ordersPage.getTotalElements());
}

    public InfoShipmentCode inforShipmentCode(String shipmentCode) {
        List<OrderLinks> orderLinks = orderLinksRepository.findByShipmentCode(shipmentCode);
        InfoShipmentCode infoShipmentCode = new InfoShipmentCode();
        if (!orderLinks.isEmpty()){
            infoShipmentCode.setOrders(orderLinks.get(0).getOrders());
            infoShipmentCode.setPrice(orderLinks.get(0).getPurchase() != null ? orderLinks.get(0).getPurchase().getFinalPriceOrder() : BigDecimal.ZERO);
            infoShipmentCode.setDestinationName(infoShipmentCode.getOrders().getDestination().getDestinationName());
        } else {
            throw new IllegalStateException("Không tìm thấy mã vận đơn này, vui lòng thử lại!");
        }
        return infoShipmentCode;
    }

    public CustomerBalanceAndOrders getOrdersWithNegativeLeftoverByCustomerCode(String customerCode) {
        Customer customer = customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new RuntimeException("Customer not found with code: " + customerCode));

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
            throw new IllegalArgumentException("Không tìm thấy mã vận đơn: " + shipmentCode);
        }

        Orders order = links.get(0).getOrders();

        return new OrderByShipmentResponse(order, links);
    }

    @Transactional
    public List<Orders> updateDestinationByShipmentCodes(List<String> shipmentCodes, Long newDestinationId) {
        if (shipmentCodes == null || shipmentCodes.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mã vận đơn không được để trống!");
        }
        
        Destination newDestination = destinationRepository.findById(newDestinationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy điểm đến với ID: " + newDestinationId));
        
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff staff)) {
            throw new IllegalStateException("Chỉ nhân viên mới được phép thực hiện thao tác này!");
        }
        Set<AccountRoles> allowedRoles = Set.of(AccountRoles.ADMIN, AccountRoles.MANAGER, AccountRoles.STAFF_SALE, AccountRoles.LEAD_SALE);
        if (!allowedRoles.contains(staff.getRole())) {
            throw new IllegalStateException("Bạn không có quyền thay đổi điểm đến!");
        }
        
        List<OrderLinks> allLinks = orderLinksRepository.findAllByShipmentCodeIn(shipmentCodes);

        if (allLinks.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy bất kỳ mã vận đơn nào trong danh sách!");
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
            throw new IllegalArgumentException("Không thể cập nhật điểm đến cho các đơn đã giao/hủy: " + String.join(", ", invalidCodes));
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));

        OrderLinks link = order.getOrderLinks().stream()
                .filter(l -> l.getLinkId().equals(orderLinkId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy link với ID: " + orderLinkId));

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
            throw new IllegalStateException("Chỉ nhân viên mới được phép thực hiện thao tác này!");
        }

        Set<AccountRoles> allowedRoles = Set.of(AccountRoles.ADMIN, AccountRoles.MANAGER, AccountRoles.STAFF_SALE, AccountRoles.LEAD_SALE);
        if (!allowedRoles.contains(staff.getRole())) {
            throw new IllegalStateException("Bạn không có quyền xóa đơn hàng!");
        }

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!order.getStatus().equals(OrderStatus.DA_XAC_NHAN)) {
            throw new IllegalStateException("Không được xóa đơn hàng ở giai đoạn này!");
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
        throw new IllegalArgumentException("Bạn phải nhập mã khách hàng để thực hiện hành động này!");
    }

    if (routeId == null) {
        throw new IllegalArgumentException("Bạn phải chọn tuyến hàng để tiếp tục!");
    }

    Customer customer = authenticationRepository.findByCustomerCode(customerCode);
    if (customer == null) {
        throw new IllegalArgumentException("Mã khách hàng không được tìm thấy, vui lòng thử lại!");
    }

    Route route = routeRepository.findById(routeId)
        .orElseThrow(() -> new RuntimeException("Route not found for ID: " + routeId));
   Destination destination = destinationRepository.findById(17L)
        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy điểm đến!"));

    if (ordersRequest.getExchangeRate().compareTo(route.getExchangeRate()) < 0) {
        throw new IllegalArgumentException("Tỉ giá không được nhỏ hơn giá cố định, liên hệ quản lý để được hỗ trợ thay đổi tỉ giá!");
    }

    // Tạo đối tượng Orders mới
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
    order.setStaff((Staff) accountUtils.getAccountCurrent());

    // Khởi tạo các giá trị tính toán
    BigDecimal totalPriceVnd = BigDecimal.ZERO;
    BigDecimal priceBeforeFee = BigDecimal.ZERO;


    OrderLinks orderLink = new OrderLinks();
    orderLink.setOrders(order);
    orderLink.setProductLink("N/A"); 
    orderLink.setQuantity(1); 
    orderLink.setPriceWeb(BigDecimal.ZERO); 
    orderLink.setShipWeb(BigDecimal.ZERO); 
    orderLink.setTotalWeb(ordersRequest.getMoneyExChange()); 
    orderLink.setPurchaseFee(ordersRequest.getFee()); 
    orderLink.setProductName("Money Exchange"); 
   
    orderLink.setWebsite("N/A"); 
    orderLink.setProductType(null);
    orderLink.setClassify("Money Exchange"); 
    orderLink.setStatus(OrderLinkStatus.CHO_MUA);
    orderLink.setNote("Chuyển tiền cho khách hàng"); 
    orderLink.setGroupTag("Chuyển tiền"); 
    orderLink.setTrackingCode(generateOrderLinkCode()); 
    orderLink.setPurchaseImage(null);
    orderLink.setExtraCharge(BigDecimal.ZERO); 
    orderLink.setFinalPriceVnd(
        orderLink.getTotalWeb().add(orderLink.getPurchaseFee()).multiply(order.getExchangeRate()) 
    );
    totalPriceVnd =   orderLink.getTotalWeb().add(orderLink.getPurchaseFee()).multiply(order.getExchangeRate()); 
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

    public List<ShipmentGroup> getShipmentsByCustomerPhone(String phone) {
        Account account = authenticationRepository.findByPhone(phone);
        if (account == null || !(account instanceof Customer customer)) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng với số điện thoại: " + phone);
        }

        List<OrderLinks> links = orderLinksRepository.findByCustomerWithShipment(customer);

        Map<String, ShipmentGroup> groups = new LinkedHashMap<>();

        for (OrderLinks link : links) {
            String code = link.getShipmentCode().trim();
            groups.computeIfAbsent(code, k ->
                            new ShipmentGroup(link.getOrders().getOrderCode(), code, link.getStatus()))
                    .addProduct(link.getProductName(), link.getProductLink());
        }

        return new ArrayList<>(groups.values());
    }

}