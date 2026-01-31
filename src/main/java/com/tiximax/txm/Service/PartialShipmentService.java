package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentPurpose;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.VoucherType;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.OrderLink.ShipmentCodesRequest;
import com.tiximax.txm.Model.DTOResponse.Payment.PartialPayment;
import com.tiximax.txm.Repository.AuthenticationRepository;
import com.tiximax.txm.Repository.CustomerVoucherRepository;
import com.tiximax.txm.Repository.OrderLinksRepository;
import com.tiximax.txm.Repository.OrdersRepository;
import com.tiximax.txm.Repository.PartialShipmentRepository;
import com.tiximax.txm.Repository.PaymentRepository;
import com.tiximax.txm.Repository.WarehouseRepository;
import com.tiximax.txm.Utils.AccountUtils;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.aspectj.weaver.ast.Not;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private WarehouseRepository warehouseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;
    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;
    @Autowired 
    private OrdersService ordersService;
    @Autowired
    private DraftDomesticService draftDomesticService;
    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private PartialShipmentRepository partialShipmentRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;



@Transactional
public List<PartialShipment> createPartialShipment(
        ShipmentCodesRequest trackingCodesRequest,
        boolean isUseBalance,
        long bankId,
        BigDecimal priceShipDos,
        Long customerVoucherId
) {

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();

    List<String> allTrackingCodes = trackingCodesRequest.getSelectedShipmentCodes();
    if (allTrackingCodes == null || allTrackingCodes.isEmpty()) {
        throw new NotFoundException("Không có mã vận đơn nào được chọn!");
    }

    List<PartialShipment> createdPartials = new ArrayList<>();

    // === Lấy OrderLinks ===
    List<OrderLinks> allLinks =
            orderLinksRepository.findByShipmentCodeIn(allTrackingCodes);

    if (allLinks.isEmpty()) {
        throw new NotFoundException("Không tìm thấy link nào cho các mã vận đơn đã chọn!");
    }

    Map<Orders, List<OrderLinks>> orderToLinksMap =
            allLinks.stream()
                    .collect(Collectors.groupingBy(OrderLinks::getOrders));

    List<Orders> ordersList =
            new ArrayList<>(orderToLinksMap.keySet());

    if (ordersList.isEmpty()) {
        throw new NotFoundException("Không tìm thấy đơn hàng nào hợp lệ!");
    }

    // === Kiểm tra cùng khách hàng ===
    Customer commonCustomer = ordersList.get(0).getCustomer();
    if (ordersList.stream().anyMatch(o -> !o.getCustomer().equals(commonCustomer))) {
        throw new BadRequestException(
                "Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp phí ship!"
        );
    }

    // === Kiểm tra trạng thái đơn ===
    if (ordersList.stream().anyMatch(o ->
            !Set.of(
                    OrderStatus.DA_DU_HANG,
                    OrderStatus.DANG_XU_LY,
                    OrderStatus.CHO_THANH_TOAN_SHIP
            ).contains(o.getStatus())
    )) {
        throw new BadRequestException(
                "Một số đơn hàng chưa đủ điều kiện để tạo thanh toán phí ship!"
        );
    }

    // === Tạo PartialShipment cho từng Order ===
    for (Map.Entry<Orders, List<OrderLinks>> entry : orderToLinksMap.entrySet()) {

        Orders order = entry.getKey();
        List<OrderLinks> orderLinks = entry.getValue();

        PartialShipment partial = new PartialShipment();
        partial.setOrders(order);
        partial.setReadyLinks(new HashSet<>(orderLinks));
        partial.setPartialAmount(
                orderLinks.stream()
                        .map(OrderLinks::getFinalPriceVnd)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        partial.setShipmentDate(LocalDateTime.now());
        partial.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        partial.setStaff(currentStaff);

        orderLinks.forEach(link -> link.setPartialShipment(partial));

        PartialShipment savedPartial =
                partialShipmentRepository.save(partial);

        orderLinksRepository.saveAll(orderLinks);
        createdPartials.add(savedPartial);
    }

    // =====================================================
    // === TÍNH PHÍ SHIP MULTI ROUTE (ĐIỂM QUAN TRỌNG) ===
    // =====================================================

    Map<String, List<String>> fakeShipCodeMap =
            Map.of("MERGED", allTrackingCodes);

    BigDecimal totalShippingFee =
            calculateFeeByShipCodeAllowMultiRoute(fakeShipCodeMap)
                    .get("MERGED");

    if (totalShippingFee == null || totalShippingFee.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BadRequestException("Không thể tính phí vận chuyển!");
    }

    BigDecimal finalAmount = roundUp(totalShippingFee);
    BigDecimal discount = BigDecimal.ZERO;

    // === XỬ LÝ VOUCHER (KHÔNG CHECK ROUTE) ===
    CustomerVoucher customerVoucher = null;

    if (customerVoucherId != null) {

        customerVoucher =
                customerVoucherRepository.findById(customerVoucherId)
                        .orElseThrow(() ->
                                new NotFoundException("Voucher không tồn tại!")
                        );

        Voucher voucher = customerVoucher.getVoucher();

        if (customerVoucher.isUsed()) {
            throw new BadRequestException("Voucher đã sử dụng!");
        }

        if (voucher.getEndDate() != null &&
                LocalDateTime.now().isAfter(voucher.getEndDate())) {
            throw new BadRequestException("Voucher đã hết hạn!");
        }

        if (voucher.getMinOrderValue() != null &&
                totalShippingFee.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException(
                    "Tổng phí ship chưa đạt yêu cầu của voucher!"
            );
        }

        if (voucher.getType() == VoucherType.PHAN_TRAM) {
            discount =
                    totalShippingFee.multiply(voucher.getValue())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = voucher.getValue();
        }

        finalAmount =
                totalShippingFee.subtract(discount)
                        .max(BigDecimal.ZERO)
                        .setScale(0, RoundingMode.HALF_UP);
    }

    // === CỘNG NỢ CŨ ===
    BigDecimal totalDebt =
            ordersList.stream()
                    .map(Orders::getLeftoverMoney)
                    .filter(m -> m != null && m.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(0, RoundingMode.HALF_UP);

    BigDecimal collect =
            finalAmount
                    .add(totalDebt)
                    .add(priceShipDos)
                    .setScale(0, RoundingMode.HALF_UP);

    // === TRỪ SỐ DƯ ===
    BigDecimal balance =
            commonCustomer.getBalance() != null
                    ? commonCustomer.getBalance()
                    : BigDecimal.ZERO;

    BigDecimal usedBalance = BigDecimal.ZERO;
    BigDecimal qrAmount = collect;

    if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
        usedBalance = balance.min(collect);
        commonCustomer.setBalance(balance.subtract(usedBalance));
        qrAmount = collect.subtract(usedBalance);
    }

    // === TẠO PAYMENT ===
    Payment payment = new Payment();
    payment.setPaymentCode(paymentService.generatePaymentCode());
    payment.setContent(
            "Phí ship gộp: " +
            String.join(", ", allTrackingCodes) +
            " - " + usedBalance + " số dư + " +
            priceShipDos + "k ship"
    );
    payment.setPaymentType(PaymentType.MA_QR);
    payment.setAmount(finalAmount);
    payment.setCollectedAmount(qrAmount);
    payment.setStatus(PaymentStatus.CHO_THANH_TOAN_SHIP);
    payment.setActionAt(LocalDateTime.now());
    payment.setCustomer(commonCustomer);
    payment.setPurpose(PaymentPurpose.THANH_TOAN_VAN_CHUYEN);
    payment.setStaff(currentStaff);
    payment.setIsMergedPayment(true);
    payment.setPartialShipments(new HashSet<>(createdPartials));

    BankAccount bankAccount =
            bankAccountService.getAccountById(bankId);

    if (bankAccount == null) {
        throw new NotFoundException("Thông tin thẻ ngân hàng không được tìm thấy!");
    }

    String qrCodeUrl =
            "https://img.vietqr.io/image/" +
            bankAccount.getBankName() + "-" +
            bankAccount.getAccountNumber() +
            "-print.png?amount=" + qrAmount +
            "&addInfo=" + payment.getPaymentCode() +
            "&accountName=" + bankAccount.getAccountHolder();

    payment.setQrCode(qrCodeUrl);

    Payment savedPayment =
            paymentRepository.save(payment);

    createdPartials.forEach(p -> {
        p.setPayment(savedPayment);
        partialShipmentRepository.save(p);
    });

    // === UPDATE ORDER ===
    for (Orders order : ordersList) {

        order.setLeftoverMoney(BigDecimal.ZERO);

        if (order.getStatus() == OrderStatus.DA_DU_HANG) {
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        }

        ordersService.addProcessLog(
                order,
                savedPayment.getPaymentCode(),
                ProcessLogAction.TAO_THANH_TOAN_HANG
        );

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
public Map<String, BigDecimal> calculateFeeByShipCodeAllowMultiRoute(
            Map<String, List<String>> shipCodeTrackingMap
    ) {

        // 1. gom toàn bộ trackingCodes (1 query)
        List<String> allTrackingCodes =
                shipCodeTrackingMap.values().stream()
                        .flatMap(List::stream)
                        .distinct()
                        .toList();

        List<Warehouse> warehouses =
                warehouseRepository
                        .findByTrackingCodeInFetchOrders(allTrackingCodes);

        if (warehouses.isEmpty()) {
            throw new NotFoundException("Không tìm thấy kiện hàng");
        }

        // 2. map trackingCode -> Warehouse
        Map<String, Warehouse> warehouseMap =
                warehouses.stream()
                        .collect(Collectors.toMap(
                                Warehouse::getTrackingCode,
                                w -> w
                        ));

        Map<String, BigDecimal> result = new HashMap<>();

        // 3. tính phí theo từng shipCode
        for (Map.Entry<String, List<String>> entry : shipCodeTrackingMap.entrySet()) {

            String shipCode = entry.getKey();
            List<String> trackingCodes = entry.getValue();

            List<Warehouse> ws = trackingCodes.stream()
                    .map(warehouseMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            BigDecimal totalFee = BigDecimal.ZERO;

            // 4. GROUP THEO ROUTE
            Map<Long, List<Warehouse>> routeMap =
                    ws.stream()
                            .collect(Collectors.groupingBy(
                                    w -> w.getOrders().getRoute().getRouteId()
                            ));

            for (List<Warehouse> routeWarehouses : routeMap.values()) {

                Route route =
                        routeWarehouses.get(0).getOrders().getRoute();

                BigDecimal priceShip =
                        routeWarehouses.get(0).getOrders().getPriceShip();

                // validate priceShip trong cùng tuyến
                boolean samePrice = routeWarehouses.stream()
                        .allMatch(w ->
                                w.getOrders().getPriceShip()
                                        .compareTo(priceShip) == 0
                        );

                if (!samePrice) {
                    throw new BadRequestException(
                            "ShipCode " + shipCode +
                            " có priceShip khác nhau trong cùng tuyến"
                    );
                }

                // tổng netWeight theo route
                BigDecimal totalNetWeight =
                        routeWarehouses.stream()
                                .map(w -> {
                                    if (w.getNetWeight() == null) {
                                        throw new IllegalArgumentException(
                                                "Thiếu netWeight: " +
                                                w.getTrackingCode()
                                        );
                                    }
                                    return BigDecimal.valueOf(w.getNetWeight());
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // min weight theo tuyến
                BigDecimal chargeableWeight;

                if ("Tuyến Nhật".equals(route.getNote())) {
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

                BigDecimal routeFee =
                        chargeableWeight.multiply(priceShip);

                totalFee = totalFee.add(roundToHundreds(routeFee));
            }

            result.put(shipCode, totalFee);
        }

        return result;
    }

 

private BigDecimal roundUp(BigDecimal value) {
    return value
            .divide(BigDecimal.valueOf(500), 0, RoundingMode.CEILING)  
            .multiply(BigDecimal.valueOf(500));  
}
private BigDecimal roundToHundreds(BigDecimal amount) {
    if (amount == null) return BigDecimal.ZERO;

    return amount
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
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


@Transactional
public List<PartialShipment> createPartialShipmentByShipCode(
        String shipCode,
        boolean isUseBalance,
        long bankId,
        BigDecimal priceShipDos,
        Long customerVoucherId
) {

    Staff currentStaff = (Staff) accountUtils.getAccountCurrent();

    DraftDomestic draftDomestic =
            draftDomesticService.getDraftDomesticByShipCode(shipCode);

    List<String> allTrackingCodes = draftDomestic.getShippingList();
    if (allTrackingCodes == null || allTrackingCodes.isEmpty()) {
        throw new NotFoundException("Không có mã vận đơn nào được chọn!");
    }

    List<PartialShipment> createdPartials = new ArrayList<>();

    // === Lấy OrderLinks ===
    List<OrderLinks> allLinks =
            orderLinksRepository.findByShipmentCodeIn(allTrackingCodes);

    if (allLinks.isEmpty()) {
        throw new NotFoundException(
                "Không tìm thấy link nào cho các mã vận đơn đã chọn!"
        );
    }

    Map<Orders, List<OrderLinks>> orderToLinksMap =
            allLinks.stream()
                    .collect(Collectors.groupingBy(OrderLinks::getOrders));

    List<Orders> ordersList =
            new ArrayList<>(orderToLinksMap.keySet());

    if (ordersList.isEmpty()) {
        throw new NotFoundException("Không tìm thấy đơn hàng nào hợp lệ!");
    }

    // === KIỂM TRA CÙNG KHÁCH HÀNG ===
    Customer commonCustomer = ordersList.get(0).getCustomer();
    if (ordersList.stream().anyMatch(o ->
            !o.getCustomer().equals(commonCustomer)
    )) {
        throw new BadRequestException(
                "Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp phí ship!"
        );
    }

    // === KIỂM TRA TRẠNG THÁI ĐƠN ===
    if (ordersList.stream().anyMatch(o ->
            !Set.of(
                    OrderStatus.DA_DU_HANG,
                    OrderStatus.DANG_XU_LY,
                    OrderStatus.CHO_THANH_TOAN_SHIP
            ).contains(o.getStatus())
    )) {
        throw new BadRequestException(
                "Một số đơn hàng chưa đủ điều kiện để tạo thanh toán phí ship!"
        );
    }

    // === TẠO PARTIAL SHIPMENT THEO ORDER ===
    for (Map.Entry<Orders, List<OrderLinks>> entry : orderToLinksMap.entrySet()) {

        Orders order = entry.getKey();
        List<OrderLinks> orderLinks = entry.getValue();

        PartialShipment partial = new PartialShipment();
        partial.setOrders(order);
        partial.setReadyLinks(new HashSet<>(orderLinks));
        partial.setPartialAmount(
                orderLinks.stream()
                        .map(OrderLinks::getFinalPriceVnd)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        partial.setShipmentDate(LocalDateTime.now());
        partial.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        partial.setStaff(currentStaff);

        orderLinks.forEach(link ->
                link.setPartialShipment(partial)
        );

        PartialShipment savedPartial =
                partialShipmentRepository.save(partial);

        orderLinksRepository.saveAll(orderLinks);
        createdPartials.add(savedPartial);
    }

    
    Map<String, List<String>> fakeShipCodeMap =
            Map.of(shipCode, allTrackingCodes);

    BigDecimal totalShippingFee =
            calculateFeeByShipCodeAllowMultiRoute(fakeShipCodeMap)
                    .get(shipCode);

    if (totalShippingFee == null ||
        totalShippingFee.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BadRequestException("Không thể tính phí vận chuyển!");
    }

    BigDecimal finalAmount = roundUp(totalShippingFee);
    BigDecimal discount = BigDecimal.ZERO;
    CustomerVoucher customerVoucher = null;

    // === XỬ LÝ VOUCHER (KHÔNG CHECK ROUTE) ===
    if (customerVoucherId != null) {

        customerVoucher =
                customerVoucherRepository.findById(customerVoucherId)
                        .orElseThrow(() ->
                                new NotFoundException("Voucher không tồn tại!")
                        );

        Voucher voucher = customerVoucher.getVoucher();

        if (customerVoucher.isUsed()) {
            throw new BadRequestException("Voucher đã sử dụng!");
        }

        if (voucher.getEndDate() != null &&
                LocalDateTime.now().isAfter(voucher.getEndDate())) {
            throw new BadRequestException("Voucher đã hết hạn!");
        }

        if (voucher.getMinOrderValue() != null &&
                totalShippingFee.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException(
                    "Tổng phí ship chưa đạt yêu cầu của voucher!"
            );
        }

        if (voucher.getType() == VoucherType.PHAN_TRAM) {
            discount =
                    totalShippingFee.multiply(voucher.getValue())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = voucher.getValue();
        }

        finalAmount =
                totalShippingFee.subtract(discount)
                        .max(BigDecimal.ZERO)
                        .setScale(0, RoundingMode.HALF_UP);
    }

    // === CỘNG NỢ CŨ ===
    BigDecimal totalDebt =
            ordersList.stream()
                    .map(Orders::getLeftoverMoney)
                    .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(0, RoundingMode.HALF_UP);

    BigDecimal collect =
            finalAmount
                    .add(totalDebt)
                    .add(priceShipDos)
                    .setScale(0, RoundingMode.HALF_UP);

    // === TRỪ SỐ DƯ ===
    BigDecimal balance =
            commonCustomer.getBalance() != null
                    ? commonCustomer.getBalance()
                    : BigDecimal.ZERO;

    BigDecimal usedBalance = BigDecimal.ZERO;
    BigDecimal qrAmount = collect;

    if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
        usedBalance = balance.min(collect);
        commonCustomer.setBalance(balance.subtract(usedBalance));
        qrAmount = collect.subtract(usedBalance);
    }

    // === TẠO PAYMENT ===
    Payment payment = new Payment();
    payment.setPaymentCode(paymentService.generatePaymentCode());
    payment.setContent(
            "Phí ship gộp (" + shipCode + "): " +
            " - " + usedBalance + " số dư + " +
            priceShipDos + "k ship"
    );
    payment.setPaymentType(PaymentType.MA_QR);
    payment.setAmount(finalAmount);
    payment.setCollectedAmount(qrAmount);
    payment.setStatus(PaymentStatus.CHO_THANH_TOAN_SHIP);
    payment.setActionAt(LocalDateTime.now());
    payment.setCustomer(commonCustomer);
    payment.setPurpose(PaymentPurpose.THANH_TOAN_VAN_CHUYEN);
    payment.setStaff(currentStaff);
    payment.setIsMergedPayment(true);
    payment.setPartialShipments(new HashSet<>(createdPartials));

    BankAccount bankAccount =
            bankAccountService.getAccountById(bankId);

    if (bankAccount == null) {
        throw new NotFoundException(
                "Thông tin thẻ ngân hàng không được tìm thấy!"
        );
    }

    String qrCodeUrl =
            "https://img.vietqr.io/image/" +
            bankAccount.getBankName() + "-" +
            bankAccount.getAccountNumber() +
            "-print.png?amount=" + qrAmount +
            "&addInfo=" + payment.getPaymentCode() +
            "&accountName=" + bankAccount.getAccountHolder();

    payment.setQrCode(qrCodeUrl);

    Payment savedPayment =
            paymentRepository.save(payment);

    createdPartials.forEach(p -> {
        p.setPayment(savedPayment);
        partialShipmentRepository.save(p);
    });

    // === UPDATE ORDER ===
    for (Orders order : ordersList) {

        order.setLeftoverMoney(BigDecimal.ZERO);

        if (order.getStatus() == OrderStatus.DA_DU_HANG) {
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
        }

        ordersService.addProcessLog(
                order,
                savedPayment.getPaymentCode(),
                ProcessLogAction.TAO_THANH_TOAN_HANG
        );

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

    draftDomesticService.updatePayment(draftDomestic);

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


}