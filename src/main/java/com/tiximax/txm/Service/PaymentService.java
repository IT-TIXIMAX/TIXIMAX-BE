package com.tiximax.txm.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTORequest.Payment.SmsRequest;
import com.tiximax.txm.Model.DTOResponse.Payment.PaymentAuctionResponse;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


@Slf4j
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private AutoPaymentService autoPaymentService;
    @Autowired
    private PartialShipmentRepository partialShipmentRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private DraftDomesticService draftDomesticService;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private PurchasesRepository purchasesRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Value("${sms.secret}")
    private String secret;

    public List<Payment> getPaymentsByOrderCode(String orderCode) {
        List<Payment> payments = paymentRepository.findByOrdersOrderCode(orderCode);
        if (payments == null){
            throw new NotFoundException("Không tìm thấy đơn hàng này!");
        }
        return payments;
    }

    public Optional<Payment> getPaymentByCode(String paymentCode) {
        Optional<Payment> payment = paymentRepository.findByPaymentCode(paymentCode);
        if (payment.isEmpty()){
            throw new NotFoundException("Không tìm thấy giao dịch này!");
        }
        return paymentRepository.findByPaymentCode(paymentCode);
    }

    public String generatePaymentCode() {
        String paymentCode;
        do {
            paymentCode = "TXMGD" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (paymentRepository.existsByPaymentCode(paymentCode));
        return paymentCode;
    }
    @Transactional
    public Payment confirmedPayment(String paymentCode) {
        Optional<Payment> paymentOptional = paymentRepository.findByPaymentCode(paymentCode);
        if (paymentOptional.isEmpty()) {
            throw new NotFoundException("Không tìm thấy giao dịch này!");
        }
        Payment payment = paymentOptional.get();
        if (!payment.getStatus().equals(PaymentStatus.CHO_THANH_TOAN)) {
            throw new BadRequestException("Trạng thái đơn hàng không phải chờ thanh toán!");
        }
        payment.setStatus(PaymentStatus.DA_THANH_TOAN);
        payment.setCollectedAmount(payment.getAmount());
        payment.setPaidTime(LocalDateTime.now());
      if (payment.getIsMergedPayment()) {
            Set<Orders> orders = payment.getRelatedOrders();

            for (Orders order : orders) {
                if (order.getStatus() == OrderStatus.CHO_THANH_TOAN_DAU_GIA) {
                    order.getPurchases().forEach(purchase -> {
                        purchase.setPurchased(true);
                        purchasesRepository.save(purchase);
                    });
                    order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
                } else {
                    order.setStatus(OrderStatus.CHO_MUA);
                }
                ordersRepository.save(order);
                ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
            }

            } else {
                Orders order = payment.getOrders();

                if (order.getStatus() == OrderStatus.CHO_THANH_TOAN_DAU_GIA) {
                    order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
                } else {
                    order.setStatus(OrderStatus.CHO_MUA);
                }

                ordersRepository.save(order);
                ordersService.addProcessLog(order, payment.getPaymentCode(), ProcessLogAction.DA_THANH_TOAN);
            }
            messagingTemplate.convertAndSend(
                    "/topic/Tiximax",
                    Map.of(
                            "event", "UPDATE",
                            "paymentCode", paymentCode,
                            "message", "Đã xác nhận thanh toán hàng!"
                    )
            );
        return paymentRepository.save(payment);
    }

    public Payment createMergedPaymentShipping(Set<String> orderCodes, boolean isUseBalance, long bankId, BigDecimal priceShipDos, Long customerVoucherId) {
        if (orderCodes == null || orderCodes.isEmpty()) {
            throw new NotFoundException("Không tìm thấy đơn hàng nào!");
        }

        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
        if (ordersList.size() != orderCodes.size()) {
            throw new NotFoundException("Một hoặc một số đơn hàng không được tìm thấy!");
        }
        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DA_DU_HANG))) {
            throw new BadRequestException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
        }

        Customer commonCustomer = ordersList.get(0).getCustomer();
        if (ordersList.stream().anyMatch(o -> !o.getCustomer().equals(commonCustomer))) {
            throw new BadRequestException("Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp!");
        }

        BigDecimal unitPrice = ordersList.get(0).getPriceShip();

        boolean hasNullWeight = ordersList.stream()
                .flatMap(order -> order.getWarehouses().stream())
                .anyMatch(warehouse -> warehouse != null && warehouse.getNetWeight() == null);
        if (hasNullWeight) {
            throw new BadRequestException("Một hoặc nhiều đơn hàng chưa được cân, vui lòng kiểm tra lại!");
        }

        BigDecimal rawTotalWeight = ordersList.stream()
                .flatMap(order -> order.getWarehouses().stream())
                .filter(warehouse -> warehouse != null && warehouse.getNetWeight() != null)
                .map(Warehouse::getNetWeight)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeight;
        if (rawTotalWeight.compareTo(BigDecimal.ONE) < 0) {
            if (ordersList.get(0).getRoute().getMinWeight().compareTo(new BigDecimal("0.50")) == 0) {
                if (rawTotalWeight.compareTo(new BigDecimal("0.5")) <= 0) {
                    totalWeight = new BigDecimal("0.5");
                } else {
                    totalWeight = BigDecimal.ONE;
                }
            } else {
                totalWeight = BigDecimal.ONE;
            }
        } else {
            totalWeight = rawTotalWeight.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = totalWeight.multiply(unitPrice).setScale(0, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO;
        CustomerVoucher customerVoucher = null;
        Voucher voucher = null;
        if (customerVoucherId != null) {
            customerVoucher = customerVoucherRepository.findById(customerVoucherId).orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));
            voucher = customerVoucher.getVoucher();
            if (customerVoucher.isUsed()) {
                throw new BadRequestException("Voucher đã sử dụng!");
            }
            if (voucher.getEndDate() != null && LocalDateTime.now().isAfter(voucher.getEndDate())) {
                throw new BadRequestException("Voucher đã hết hạn!");
            }
            if (voucher.getMinOrderValue() != null && totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new BadRequestException("Tổng giá trị đơn hàng chưa đạt yêu cầu của voucher!");
            }
            Set<Route> applicableRoutes = voucher.getApplicableRoutes();
            if (!applicableRoutes.isEmpty()) {
                boolean allRoutesMatch = ordersList.stream()
                        .allMatch(order -> applicableRoutes.contains(order.getRoute()));
                if (!allRoutesMatch) {
                    throw new BadRequestException("Voucher không áp dụng cho tuyến của một số đơn hàng!");
                }
            }
            if (voucher.getType() == VoucherType.PHAN_TRAM) {
                discount = totalAmount.multiply(voucher.getValue()).divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            } else if (voucher.getType() == VoucherType.CO_DINH) {
                discount = voucher.getValue();
            }
            if (voucherService.usedVoucher(commonCustomer.getAccountId(), voucher.getVoucherId())){
                totalAmount = totalAmount.subtract(discount);
            } else {
                throw new BadRequestException("Lỗi khi áp voucher!");
            }
        }

        BigDecimal totalDebt = ordersList.stream()
                .map(Orders::getLeftoverMoney)
                .filter(leftover -> leftover != null && leftover.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal collect = totalAmount.add(totalDebt).add(priceShipDos).setScale(0, RoundingMode.HALF_UP);

        BigDecimal qrAmount = collect;
        BigDecimal balance = (commonCustomer.getBalance() != null) ? commonCustomer.getBalance() : BigDecimal.ZERO;
        BigDecimal usedBalance = BigDecimal.ZERO;
        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
            usedBalance = balance.min(collect);
            commonCustomer.setBalance(balance.subtract(usedBalance));
            qrAmount = collect.subtract(usedBalance).max(BigDecimal.ZERO);
        }

        Payment payment = new Payment();
        payment.setPaymentCode(generateMergedPaymentCode());
        payment.setContent(String.join(", ", orderCodes) + " + " + priceShipDos + " ship" + " - " + usedBalance + " số dư" + " - " + discount + " discount");
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(totalAmount);
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN_SHIP);
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(commonCustomer);
        payment.setPurpose(PaymentPurpose.THANH_TOAN_VAN_CHUYEN);
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setIsMergedPayment(true);
        payment.setRelatedOrders(new HashSet<>(ordersList));
        payment.setOrders(null);

        payment.setCollectedAmount(qrAmount);

        BankAccount bankAccount = bankAccountService.getAccountById(bankId);
        if (bankAccount == null) {
            throw new BadRequestException("Thông tin thẻ ngân hàng không được tìm thấy!");
        }

        String qrCodeUrl = "https://img.vietqr.io/image/" + bankAccount.getBankName() + "-" + bankAccount.getAccountNumber() + "-print.png?amount=" + qrAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankAccount.getAccountHolder();
        payment.setQrCode(qrCodeUrl);

        Payment savedPayment = paymentRepository.save(payment);

        for (Orders order : ordersList) {
            order.setLeftoverMoney(BigDecimal.ZERO);
            order.setStatus(OrderStatus.CHO_THANH_TOAN_SHIP);
            ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
            ordersRepository.save(order);
        }

        if (isUseBalance && usedBalance.compareTo(BigDecimal.ZERO) > 0) {
            authenticationRepository.save(commonCustomer);
        }

        if (qrAmount.compareTo(BigDecimal.ZERO) == 0) {
            confirmedPaymentShipment(payment.getPaymentCode());
            savedPayment = paymentRepository.save(savedPayment);
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
                        "message", "Thanh toán gộp mới được tạo!"
                )
        );
        return savedPayment;
    }
@Transactional
public Payment confirmedPaymentShipment(String paymentCode) {


    Payment payment = paymentRepository.findByPaymentCode(paymentCode)
            .orElseThrow(() ->
                    new BadRequestException("Không tìm thấy giao dịch này!")
            );


    if (payment.getStatus() != PaymentStatus.CHO_THANH_TOAN_SHIP) {
        throw new BadRequestException(
                "Trạng thái giao dịch không phải chờ thanh toán ship!"
        );
    }

    LocalDateTime now = LocalDateTime.now();
    Set<String> paidShipmentCodes = new HashSet<>();

    partialShipmentRepository.updateAllByPaymentId(
            payment.getPaymentId(),
            OrderStatus.CHO_GIAO,
            now
    );

    List<PartialShipment> partialShipments =
            partialShipmentRepository.findDetailByPaymentId(
                    payment.getPaymentId()
            );

    for (PartialShipment shipment : partialShipments) {

        // ---- OrderLinks của PartialShipment ----
        for (OrderLinks link : shipment.getReadyLinks()) {

            if (link.getStatus() == OrderLinkStatus.DA_HUY) continue;

            link.setStatus(OrderLinkStatus.CHO_GIAO);
            link.setPartialShipment(shipment);
            orderLinksRepository.save(link);

            if (link.getShipmentCode() != null) {
                paidShipmentCodes.add(link.getShipmentCode());
            }
        }

        // ---- Orders ----
        Orders order = shipment.getOrders();

        boolean allLinksReady = order.getOrderLinks().stream()
                .allMatch(l ->
                        l.getStatus() == OrderLinkStatus.CHO_GIAO
                                || l.getStatus() == OrderLinkStatus.DA_GIAO
                                || l.getStatus() == OrderLinkStatus.DA_HUY
                );

        if (allLinksReady) {
            order.setStatus(OrderStatus.CHO_GIAO);
        }

        ordersRepository.save(order);
        ordersService.addProcessLog(
                order,
                payment.getPaymentCode(),
                ProcessLogAction.DA_THANH_TOAN_SHIP
        );
    }

    orderLinksRepository.flush();
    ordersRepository.flush();

    for (String shipmentCode : paidShipmentCodes) {
        syncWarehouseIfAllLinksReady(shipmentCode);
    }

    warehouseRepository.flush();

    draftDomesticService
            .checkAndLockDraftDomesticByShipmentCodes(paidShipmentCodes);

    payment.setStatus(PaymentStatus.DA_THANH_TOAN_SHIP);
    payment.setPaidTime(now);
    paymentRepository.save(payment);

    messagingTemplate.convertAndSend(
            "/topic/Tiximax",
            Map.of(
                    "event", "UPDATE",
                    "paymentCode", paymentCode,
                    "message", "Đã xác nhận thanh toán ship!"
            )
    );

    return payment;
}



    public Optional<Payment> getPaymentsById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public List<PaymentAuctionResponse> getPaymentByStaffandStatus() {
        Staff staff = (Staff) accountUtils.getAccountCurrent();

        List<Payment> payments = paymentRepository
                .findAllByStaffAndOrderStatusAndPaymentStatusOrderByActionAtDesc(
                        staff,
                        OrderStatus.CHO_THANH_TOAN_DAU_GIA,
                        PaymentStatus.CHO_THANH_TOAN
                );

        return payments.stream()
                .map(PaymentAuctionResponse::new)
                .toList();
}

    public List<Payment> getPaymentsByPartialStatus() {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        return paymentRepository.findPaymentsByStaffAndPartialStatus(staff.getAccountId(), OrderStatus.CHO_THANH_TOAN_SHIP);
    }

    public Optional<Payment> getPendingPaymentByOrderId(Long orderId) {
        if (!ordersRepository.existsById(orderId)) {
            throw new RuntimeException("Không tìm thấy đơn hàng này!");
        }
        return paymentRepository.findFirstByOrdersOrderIdAndStatus(orderId, PaymentStatus.CHO_THANH_TOAN);
    }

    public Payment createMergedPayment(Set<String> orderCodes, Integer depositPercent, boolean isUseBalance, long bankId) {
        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
        if (ordersList.size() != orderCodes.size()) {
            throw new NotFoundException("Một hoặc một số đơn hàng không được tìm thấy!");
        }
        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DA_XAC_NHAN))) {
            throw new BadRequestException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
        }

        Customer commonCustomer = ordersList.get(0).getCustomer();
        if (ordersList.stream().anyMatch(o -> !o.getCustomer().equals(commonCustomer))) {
            throw new BadRequestException("Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp!");
        }

        BigDecimal totalAmount = ordersList.stream()
                .map(Orders::getFinalPriceOrder)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal depositRate = BigDecimal.valueOf(depositPercent / 100.00);
//        BigDecimal totalCollect = totalAmount.multiply(depositRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCollect = BigDecimal.ZERO;
        for (Orders order : ordersList) {
            BigDecimal orderFinalPrice = order.getFinalPriceOrder();
            BigDecimal orderCollect = orderFinalPrice.multiply(depositRate).setScale(0, RoundingMode.HALF_UP);
            BigDecimal orderLeftover = orderFinalPrice.subtract(orderCollect).setScale(0, RoundingMode.HALF_UP);
            order.setLeftoverMoney(orderLeftover);
            ordersRepository.save(order);
            totalCollect = totalCollect.add(orderCollect);
        }

        BigDecimal qrAmount = totalCollect;
        BigDecimal balance = (commonCustomer.getBalance() != null) ? commonCustomer.getBalance() : BigDecimal.ZERO;
        BigDecimal usedBalance = BigDecimal.ZERO;
        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
            usedBalance = balance.min(totalCollect);
            commonCustomer.setBalance(balance.subtract(usedBalance));
            qrAmount = totalCollect.subtract(usedBalance);
        }

        Payment payment = new Payment();
        payment.setPaymentCode(generateMergedPaymentCode());
        payment.setContent(String.join(" ", orderCodes) + " - " + usedBalance + " số dư");
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setAmount(totalAmount);
        payment.setCollectedAmount(totalCollect);
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        payment.setDepositPercent(depositPercent);
        payment.setActionAt(LocalDateTime.now());
        payment.setCustomer(commonCustomer);
        payment.setPurpose(PaymentPurpose.THANH_TOAN_DON_HANG);
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setIsMergedPayment(true);
        payment.setRelatedOrders(new HashSet<>(ordersList));

        payment.setCollectedAmount(qrAmount);

        BankAccount bankAccount = bankAccountService.getAccountById(bankId);
        if (bankAccount == null){
            throw new RuntimeException("Thông tin thẻ ngân hàng không được tìm thấy!");
        }
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankAccount.getBankName() + "-" + bankAccount.getAccountNumber() + "-print.png?amount=" + qrAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankAccount.getAccountHolder();
        payment.setQrCode(qrCodeUrl);

        Payment savedPayment = paymentRepository.save(payment);

        for (Orders order : ordersList) {
            ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
            order.setStatus(OrderStatus.CHO_THANH_TOAN);
            ordersRepository.save(order);
        }

        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
            authenticationRepository.save(commonCustomer);
        } else if (commonCustomer.getBalance() != null) {
            authenticationRepository.save(commonCustomer);
        }

        if (qrAmount.compareTo(BigDecimal.ZERO) == 0 && depositPercent >= 100) {
            savedPayment.setStatus(PaymentStatus.DA_THANH_TOAN);
            savedPayment = paymentRepository.save(savedPayment);
            for (Orders order : ordersList) {
                order.setStatus(OrderStatus.CHO_MUA);
                ordersRepository.save(order);
            }
        }
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "UPDATE",
                        "paymentCode", savedPayment.getPaymentCode(),
                        "customerCode", commonCustomer.getCustomerCode(),
                        "message", "Thanh toán gộp mới được tạo!"
                )
        );
        return savedPayment;
    }

    public Payment createMergedPaymentAfterAuction(Set<String> orderCodes, Integer depositPercent, boolean isUseBalance, long bankId) {
        List<Orders> ordersList = ordersRepository.findAllByOrderCodeIn(new ArrayList<>(orderCodes));
        if (ordersList.size() != orderCodes.size()) {
            throw new NotFoundException("Một hoặc một số đơn hàng không được tìm thấy!");
        }
        if (ordersList.stream().anyMatch(o -> !o.getStatus().equals(OrderStatus.DAU_GIA_THANH_CONG))) {
            throw new BadRequestException("Một hoặc một số đơn hàng chưa đủ điều kiện để thanh toán!");
        }

        Customer commonCustomer = ordersList.get(0).getCustomer();
        if (ordersList.stream().anyMatch(o -> !o.getCustomer().equals(commonCustomer))) {
            throw new BadRequestException("Các đơn hàng phải thuộc cùng một khách hàng để thanh toán gộp!");
        }

        BigDecimal totalAmount = ordersList.stream()
                .map(Orders::getPaymentAfterAuction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal depositRate = BigDecimal.valueOf(depositPercent / 100.00);
//        BigDecimal totalCollect = totalAmount.multiply(depositRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCollect = BigDecimal.ZERO;
        for (Orders order : ordersList) {
            BigDecimal orderFinalPrice = order.getPaymentAfterAuction();
            BigDecimal orderCollect = orderFinalPrice.multiply(depositRate).setScale(0, RoundingMode.HALF_UP);
            BigDecimal orderLeftover = orderFinalPrice.subtract(orderCollect).setScale(0, RoundingMode.HALF_UP);
            order.setLeftoverMoney(orderLeftover);
            ordersRepository.save(order);
            totalCollect = totalCollect.add(orderCollect);
        }

        Payment payment = new Payment();
        payment.setPaymentCode(generateMergedPaymentCode());
        payment.setContent(String.join(" ", orderCodes));
        payment.setPaymentType(PaymentType.MA_QR);
        payment.setPaymentCode(generateMergedPaymentCode());
        payment.setAmount(totalAmount);
        payment.setCollectedAmount(totalCollect);
        payment.setStatus(PaymentStatus.CHO_THANH_TOAN);
        payment.setDepositPercent(depositPercent);
        payment.setActionAt(LocalDateTime.now());
        payment.setPurpose(PaymentPurpose.THANH_TOAN_DON_HANG);
        payment.setCustomer(commonCustomer);
        payment.setStaff((Staff) accountUtils.getAccountCurrent());
        payment.setIsMergedPayment(true);
        payment.setRelatedOrders(new HashSet<>(ordersList));

        BigDecimal qrAmount = totalCollect;
        BigDecimal balance = (commonCustomer.getBalance() != null) ? commonCustomer.getBalance() : BigDecimal.ZERO;
        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usedBalance = balance.min(totalCollect);
            commonCustomer.setBalance(balance.subtract(usedBalance));
            qrAmount = totalCollect.subtract(usedBalance);
            
        }
        payment.setCollectedAmount(qrAmount);
        BankAccount bankAccount = bankAccountService.getAccountById(bankId);
        if (bankAccount == null){
            throw new NotFoundException("Thông tin thẻ ngân hàng không được tìm thấy!");
        }
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankAccount.getBankName() + "-" + bankAccount.getAccountNumber() + "-print.png?amount=" + qrAmount + "&addInfo=" + payment.getPaymentCode() + "&accountName=" + bankAccount.getAccountHolder();
        payment.setQrCode(qrCodeUrl);

        Payment savedPayment = paymentRepository.save(payment);

        for (Orders order : ordersList) {
            ordersService.addProcessLog(order, savedPayment.getPaymentCode(), ProcessLogAction.TAO_THANH_TOAN_HANG);
            order.setStatus(OrderStatus.CHO_THANH_TOAN_DAU_GIA);
            order.getPurchases().forEach(purchase -> purchase.setPurchased(true));
            ordersRepository.save(order);
        }

        if (isUseBalance && balance.compareTo(BigDecimal.ZERO) > 0) {
            authenticationRepository.save(commonCustomer);
        } else if (commonCustomer.getBalance() != null) {
            authenticationRepository.save(commonCustomer);
        }
        if (qrAmount.compareTo(BigDecimal.ZERO) == 0 && depositPercent >= 100) {
            savedPayment.setStatus(PaymentStatus.DA_THANH_TOAN);
            savedPayment = paymentRepository.save(savedPayment);
            for (Orders order : ordersList) {
                order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
                ordersRepository.save(order);
            }
        }
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "UPDATE",
                        "paymentCode", savedPayment.getPaymentCode(),
                        "customerCode", commonCustomer.getCustomerCode(),
                        "message", "Thanh toán gộp sau đấu giá mới được tạo!"
                )
        );
        return savedPayment;
    }

    public String generateMergedPaymentCode() {
        String paymentCode;
        do {
            paymentCode = "TXMGD" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (paymentRepository.existsByPaymentCode(paymentCode));
        return paymentCode;
    }

    public SmsRequest getSmsFromExternalApi() throws Exception {
        String url = "https://bank-sms.hidden-sunset-f690.workers.dev/";
        String jsonResponse = restTemplate.getForObject(url, String.class);

        if (jsonResponse == null || jsonResponse.isEmpty()) {
            throw new RuntimeException("Empty response");
        }
        return objectMapper.readValue(jsonResponse, SmsRequest.class);  // Parse nhanh
    }

   public void verifyRaw(String rawBody, String signature) {
    try {
        String expected = hmac(rawBody);

        if (!expected.equals(signature)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid signature"
            );
        }

        SmsRequest req =
            new ObjectMapper().readValue(rawBody, SmsRequest.class);

        long now = System.currentTimeMillis();
        if (Math.abs(now - req.getTimestamp()) > 300_000) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Expired request"
            );
        }

    String txmCode = extractTXMGD(req.getContent());

    if (txmCode == null) {
    
        return;
    }

    Payment payment = paymentRepository.findByPaymentCode(txmCode).get();
    if (payment == null) {
        return;
    }
    if (payment.getCollectedAmount().compareTo(req.getAmount()) != 0){
        return;
    }
    autoPaymentService.create(req.getAmount(), txmCode , payment.getPurpose());

    if(payment.getPurpose() == PaymentPurpose.THANH_TOAN_DON_HANG){
         log.info("[VERIFY_RAW] Calling confirmedPayment({})", txmCode);
        confirmedPayment(txmCode);
    } else {
         log.info("[VERIFY_RAW] Calling confirmedPaymentShipment({})", txmCode);
        confirmedPaymentShipment(txmCode);
    }
      log.info("[VERIFY_RAW] End verifyRaw OK");

    } catch (JsonProcessingException e) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid JSON payload"
        );
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}

private static final Pattern TXMGD_PATTERN =
        Pattern.compile("\\bTXMGD[0-9A-F]{8}\\b");



public static String extractTXMGD(String content) {
    if (content == null || content.isBlank()) return null;

    Matcher matcher = TXMGD_PATTERN.matcher(content.toUpperCase());
    return matcher.find() ? matcher.group() : null;
}



    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes()));
    }


   

    public Payment refundFromBalance(Long customerId, Double amount, String imageUrl) {
        Customer customer = customerRepository.getCustomerById(customerId);
        if (customer == null){
            throw new NotFoundException("Không tìm thấy khách hàng này!");
        }
        if (customer.getBalance().equals(BigDecimal.ZERO)){
            throw new BadRequestException("Số dư phải lớn hơn 0 thì mới được sử dụng tính năng này!");
        }
        if (BigDecimal.valueOf(amount).compareTo(BigDecimal.ZERO) < 0){
            throw new BadRequestException("Không cho phép truyền vào số âm!");
        }
        if (customer.getBalance().compareTo(BigDecimal.valueOf(amount)) >= 0){
            customer.setBalance(customer.getBalance().subtract(BigDecimal.valueOf(amount)));
            customerRepository.save(customer);
        } else {
            throw new BadRequestException("Số tiền bạn chuyển lớn hơn số tiền trong ví của khách!");
        }

        Payment refundPayment = new Payment();
        refundPayment.setPaymentCode(generatePaymentCode());
        refundPayment.setContent("Hoàn tiền từ ví, số dư còn lại " + customer.getBalance());
        refundPayment.setPaymentType(PaymentType.MA_QR);
        refundPayment.setAmount(BigDecimal.valueOf(amount).negate());
        refundPayment.setCollectedAmount(BigDecimal.valueOf(amount).negate());
        refundPayment.setStatus(PaymentStatus.DA_HOAN_TIEN);
        refundPayment.setQrCode(imageUrl);
        refundPayment.setActionAt(LocalDateTime.now());
        refundPayment.setCustomer(customer);
        refundPayment.setStaff((Staff) accountUtils.getAccountCurrent());
        refundPayment.setIsMergedPayment(false);
        paymentRepository.save(refundPayment);
        return refundPayment;
    }

  private void syncWarehouseIfAllLinksReady(String shipmentCode) {

    if (shipmentCode == null) return;

    List<OrderLinks> links =
            orderLinksRepository.findByShipmentCode(shipmentCode);

    if (links.isEmpty()) return;

    boolean allReady = links.stream()
            .filter(l -> l.getStatus() != OrderLinkStatus.DA_HUY)
            .allMatch(l ->
                    l.getStatus() == OrderLinkStatus.CHO_GIAO ||
                    l.getStatus() == OrderLinkStatus.DA_GIAO
            );

    if (!allReady) return;

    warehouseRepository.findByTrackingCode(shipmentCode)
            .ifPresent(w -> {
                if (w.getStatus() != WarehouseStatus.CHO_GIAO) {
                    w.setStatus(WarehouseStatus.CHO_GIAO);
                    warehouseRepository.save(w);
                }
            });
}


}
