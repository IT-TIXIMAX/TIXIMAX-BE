package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.VoucherType;
import com.tiximax.txm.Model.DTORequest.OrderLink.ShipmentCodesRequest;
import com.tiximax.txm.Model.DTOResponse.Payment.PartialPayment;
import com.tiximax.txm.Repository.AuthenticationRepository;
import com.tiximax.txm.Repository.CustomerVoucherRepository;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PartialShipmentRepository;
import com.tiximax.txm.Repository.PaymentRepository;
import com.tiximax.txm.Repository.RouteRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service

public class PartialShipmentService {

    @Value("${bank.name}")
    private String bankName;

    @Value("${bank.number}")
    private String bankNumber;

    @Value("${bank.owner}")
    private String bankOwner;

    @Autowired
    private OrdersRepository ordersRepository;
    
    @Autowired
    @Lazy
    private PaymentService paymentService;
    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private WarehouseRepository warehousereRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;
    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;
    @Autowired 
    private OrdersService ordersService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private PartialShipmentRepository partialShipmentRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;



@Transactional
public List<PartialShipment> createPartialShipment(ShipmentCodesRequest trackingCodesRequest,
                                                   boolean isUseBalance,
                                                   long bankId,
                                                   BigDecimal priceShipDos,
                                                   Long customerVoucherId) {

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
    List<String> allTrackingCodes = trackingCodesRequest.getSelectedShipmentCodes();
    if (allTrackingCodes == null || allTrackingCodes.isEmpty()) {
        throw new RuntimeException("Không có mã vận đơn nào được chọn!");
    }

    List<PartialShipment> createdPartials = new ArrayList<>();

    List<OrderLinks> allLinks = orderLinksRepository.findByShipmentCodeIn(allTrackingCodes);
    if (allLinks.isEmpty()) {
        throw new RuntimeException("Không tìm thấy link nào cho các mã vận đơn đã chọn!");
    }

    Map<Orders, List<OrderLinks>> orderToLinksMap = allLinks.stream()
            .collect(Collectors.groupingBy(OrderLinks::getOrders));

    List<Orders> ordersList = new ArrayList<>(orderToLinksMap.keySet());
    if (ordersList.isEmpty()) {
        throw new RuntimeException("Không tìm thấy đơn hàng nào hợp lệ!");
    }

    // Kiểm tra: tất cả đơn phải cùng khách hàng
    Customer commonCustomer = ordersList.get(0).getCustomer();
    if (ordersList.stream().anyMatch(o -> !o.getCustomer().equals(commonCustomer))) {
        throw new RuntimeException("Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp phí ship!");
    }

    // Kiểm tra: tất cả đơn phải cùng tuyến (Route)
    Route commonRoute = ordersList.get(0).getRoute();
    if (ordersList.stream().anyMatch(o -> !o.getRoute().equals(commonRoute))) {
        throw new RuntimeException("Các đơn hàng phải cùng tuyến đường để gộp phí vận chuyển!");
    }

    // Kiểm tra: tất cả đơn phải ở trạng thái phù hợp (ví dụ: đã có hàng, chưa thanh toán ship)
    if (ordersList.stream().anyMatch(o -> !Set.of(OrderStatus.DA_DU_HANG, OrderStatus.DANG_XU_LY,OrderStatus.CHO_THANH_TOAN_SHIP).contains(o.getStatus()))) {
        throw new RuntimeException("Một số đơn hàng chưa đủ điều kiện để tạo thanh toán phí ship!");
    }

    // Tạo PartialShipment cho từng đơn hàng
    for (Map.Entry<Orders, List<OrderLinks>> entry : orderToLinksMap.entrySet()) {
        Orders order = entry.getKey();
        List<OrderLinks> orderLinks = entry.getValue();

        PartialShipment partial = new PartialShipment();
        partial.setOrders(order);
        partial.setReadyLinks(new HashSet<>(orderLinks));
        partial.setPartialAmount(orderLinks.stream()
                .map(OrderLinks::getFinalPriceVnd)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        partial.setShipmentDate(LocalDateTime.now());
        partial.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        partial.setStaff(currentStaff);

        // Liên kết ngược
        orderLinks.forEach(link -> link.setPartialShipment(partial));

        PartialShipment savedPartial = partialShipmentRepository.save(partial);
        orderLinksRepository.saveAll(orderLinks);
        createdPartials.add(savedPartial);
    }

    // === Tính phí ship tổng ===
    BigDecimal totalShippingFee = calculateTotalShippingFee(allTrackingCodes);
    if (totalShippingFee == null || totalShippingFee.compareTo(BigDecimal.ZERO) <= 0) {
        throw new RuntimeException("Không thể tính phí vận chuyển!");
    }

    // === Xử lý Voucher ===
    BigDecimal discount = BigDecimal.ZERO;
    CustomerVoucher customerVoucher = null;
    BigDecimal finalAmount = roundUp(totalShippingFee);

    if (customerVoucherId != null) {
        customerVoucher = customerVoucherRepository.findById(customerVoucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        Voucher voucher = customerVoucher.getVoucher();
        if (customerVoucher.isUsed()) {
            throw new RuntimeException("Voucher đã sử dụng!");
        }
        if (voucher.getEndDate() != null && LocalDateTime.now().isAfter(voucher.getEndDate())) {
            throw new RuntimeException("Voucher đã hết hạn!");
        }
        if (voucher.getMinOrderValue() != null && totalShippingFee.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new RuntimeException("Tổng phí ship chưa đạt yêu cầu của voucher!");
        }

        Set<Route> applicableRoutes = voucher.getApplicableRoutes();
        if (!applicableRoutes.isEmpty() && !applicableRoutes.contains(commonRoute)) {
            throw new RuntimeException("Voucher không áp dụng cho tuyến này!");
        }

        if (voucher.getType() == VoucherType.PHAN_TRAM) {
            discount = totalShippingFee.multiply(voucher.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (voucher.getType() == VoucherType.CO_DINH) {
            discount = voucher.getValue();
        }

        finalAmount = totalShippingFee.subtract(discount).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    // === Tính tổng cần thu (cộng nợ cũ nếu có) ===
    BigDecimal totalDebt = ordersList.stream()
            .map(Orders::getLeftoverMoney)
            .filter(leftover -> leftover != null && leftover.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(0, RoundingMode.HALF_UP);

    BigDecimal collect = finalAmount.add(totalDebt).add(priceShipDos).setScale(0, RoundingMode.HALF_UP);

    // === Xử lý dùng số dư ===
    BigDecimal balance = (commonCustomer.getBalance() != null) ? commonCustomer.getBalance() : BigDecimal.ZERO;
    BigDecimal usedBalance = BigDecimal.ZERO;
    BigDecimal qrAmount = collect;

    if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
        usedBalance = balance.min(collect);
        commonCustomer.setBalance(balance.subtract(usedBalance));
        qrAmount = collect.subtract(usedBalance).max(BigDecimal.ZERO);
    }

    // === Tạo Payment gộp ===
    Payment payment = new Payment();
    payment.setPaymentCode(paymentService.generatePaymentCode()); // hoặc generatePaymentCode() tùy bạn
//    payment.setContent("Phí ship gộp: " + String.join(", ", allTrackingCodes) + " + " + priceShipDos + "k");
    payment.setContent("Phí ship gộp: " + String.join(", ", allTrackingCodes) + " - " + usedBalance + " số dư + "  + priceShipDos + "k ship");
    payment.setPaymentType(PaymentType.MA_QR);
    payment.setAmount(finalAmount);
    payment.setCollectedAmount(qrAmount);
    payment.setStatus( PaymentStatus.CHO_THANH_TOAN_SHIP);
    payment.setActionAt(LocalDateTime.now());
    payment.setCustomer(commonCustomer);
    payment.setStaff(currentStaff);
    payment.setIsMergedPayment(false);
    payment.setPartialShipments(new HashSet<>(createdPartials));

    // === Tạo QR ===
    BankAccount bankAccount = bankAccountService.getAccountById(bankId);
    if (bankAccount == null) {
        throw new RuntimeException("Thông tin thẻ ngân hàng không được tìm thấy!");
    }

    String qrCodeUrl = "https://img.vietqr.io/image/" +
            bankAccount.getBankName() + "-" + bankAccount.getAccountNumber() + "-print.png?amount=" +
            qrAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankAccount.getAccountHolder();
    payment.setQrCode(qrCodeUrl);

    Payment savedPayment = paymentRepository.save(payment);
    createdPartials.forEach(partial -> {
        partial.setPayment(savedPayment);
        partialShipmentRepository.save(partial);
    });

  for (Orders order : ordersList) {
    order.setLeftoverMoney(BigDecimal.ZERO);

    if (order.getStatus() == OrderStatus.DA_DU_HANG) {
        order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
    } else {
        ordersService.addProcessLog(order, savedPayment.getPaymentCode(),
                ProcessLogAction.TAO_THANH_TOAN_HANG);
    }

    ordersRepository.save(order);
}

    if (isUseBalance && usedBalance.compareTo(BigDecimal.ZERO) > 0) {
        authenticationRepository.save(commonCustomer);
    }

    if (customerVoucher != null) {
        customerVoucher.setUsed(true);
        customerVoucher.setUsedDate(LocalDateTime.now());
        customerVoucherRepository.save(customerVoucher);
    }

    // === Gửi thông báo WebSocket ===
    messagingTemplate.convertAndSend(
            "/topic/Tiximax",
            Map.of(
                    "event", "UPDATE",
                    "paymentCode", savedPayment.getPaymentCode(),
                    "customerCode", commonCustomer.getCustomerCode(),
                    "message", "Thanh toán phí ship gộp đã được tạo!"
            )
    );
    return createdPartials;
}
 public Optional<PartialShipment> getById(Long id) {
        return partialShipmentRepository.findById(id);
    }
    public BigDecimal calculateTotalShippingFee(List<String> selectedTrackingCodes) {

    List<Warehouse> warehouses =
            warehousereRepository.findByTrackingCodeIn(selectedTrackingCodes);

    if (warehouses.isEmpty()) {
        throw new IllegalArgumentException("Không tìm thấy kiện hàng");
    }

    Route baseRoute = warehouses.get(0).getOrders().getRoute();
    boolean sameRoute = warehouses.stream()
        .allMatch(w ->
            w.getOrders() != null &&
            w.getOrders().getRoute() != null &&
            w.getOrders().getRoute().getRouteId()
                .equals(baseRoute.getRouteId())
        );

    if (!sameRoute) {
        throw new IllegalArgumentException("Các order thuộc tuyến khác nhau");
    }

    BigDecimal basePriceShip = warehouses.get(0).getOrders().getPriceShip();

    boolean samePrice = warehouses.stream()
        .allMatch(w ->
            w.getOrders().getPriceShip() != null &&
            w.getOrders().getPriceShip().compareTo(basePriceShip) == 0
        );

    if (!samePrice) {
        throw new IllegalArgumentException(
            "Không thể gộp: các order có priceShip khác nhau"
        );
    }

    BigDecimal totalNetWeight = warehouses.stream()
        .map(w -> {
            if (w.getNetWeight() == null) {
                throw new IllegalArgumentException(
                    "Thiếu netWeight cho kiện " + w.getTrackingCode()
                );
            }
            return BigDecimal.valueOf(w.getNetWeight());
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 4️⃣ Áp min theo route
    BigDecimal chargeableWeight;

    if ("Tuyến Nhật".equals(baseRoute.getNote())) {
        chargeableWeight =
            totalNetWeight.compareTo(BigDecimal.valueOf(0.5)) < 0
                ? BigDecimal.valueOf(0.5)
                : totalNetWeight;
    } else {
        chargeableWeight =
            totalNetWeight.compareTo(BigDecimal.ONE) < 0
                ? BigDecimal.ONE
                : totalNetWeight;
    }

    return chargeableWeight.multiply(basePriceShip);
}
private BigDecimal roundUp(BigDecimal value) {
    return value
            .divide(BigDecimal.valueOf(500), 0, RoundingMode.CEILING)  
            .multiply(BigDecimal.valueOf(500));  
}
    public Page<PartialPayment> getPartialPayments(
        Pageable pageable,
        OrderStatus status,
        String orderCode
) {
    Account current = accountUtils.getAccountCurrent();
    Long staffId = current.getAccountId();
    AccountRoles role = current.getRole();

    if (orderCode != null && orderCode.trim().isEmpty()) {
        orderCode = null;
    }

    Page<PartialShipment> page;

    if (role == AccountRoles.MANAGER) {
        page = partialShipmentRepository.findForPartialPayment(
                status,
                orderCode,
                pageable
        );
    } else {
        page = partialShipmentRepository.findForPartialPaymentByStaff(
                staffId,
                status,
                pageable
        );
    }
    return page.map(PartialPayment::new);
}


}