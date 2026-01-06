package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Enums.PurchaseFilter;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class PurchaseService {

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private PurchasesRepository purchasesRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private AccountRouteRepository accountRouteRepository;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private AccountUtils accountUtils;

    public Purchases createPurchase(String orderCode, PurchaseRequest purchaseRequest) {
        Orders order = ordersRepository.findByOrderCode(orderCode);

        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng!");
        }

        List<OrderLinks> orderLinks = orderLinksRepository.findByTrackingCodeIn(purchaseRequest.getTrackingCode());
        if (orderLinks.size() != purchaseRequest.getTrackingCode().size()) {
            throw new IllegalArgumentException("Một hoặc nhiều mã không được tìm thấy!");
        }
        BigDecimal priceLinks = BigDecimal.ZERO;

        for (OrderLinks ol : orderLinks){
            priceLinks = priceLinks.add(ol.getTotalWeb());
        }

        if (!(order.getStatus().equals(OrderStatus.CHO_MUA) || order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN))){
            throw new RuntimeException("Đơn hàng chưa đủ điều kiện để mua hàng!");
        }

        boolean allBelongToOrder = orderLinks.stream()
                .allMatch(link -> link.getOrders().getOrderId().equals(order.getOrderId()));
        if (!allBelongToOrder) {
            throw new IllegalArgumentException("Tất cả mã phải thuộc cùng đơn hàng " + orderCode);
        }

        if(purchaseRequest.getShipmentCode() != ""){
            if (orderLinksRepository.existsByShipmentCode(purchaseRequest.getShipmentCode())) {
                throw new IllegalArgumentException("Một hoặc nhiều mã đã có mã vận đơn, không thể mua lại!");
            }
        }

        boolean allActive = orderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.CHO_MUA || link.getStatus() == OrderLinkStatus.MUA_SAU);
        if (!allActive) {
            throw new IllegalArgumentException("Tất cả mã phải ở trạng thái chờ mua!");
        }
        Purchases purchase = new Purchases();
        purchase.setPurchaseCode(generatePurchaseCode());
        purchase.setPurchaseTime(LocalDateTime.now());
        purchase.setStaff((Staff) accountUtils.getAccountCurrent());
        purchase.setOrders(order);
        purchase.setPurchased(true);
        purchase.setNote(purchaseRequest.getNote());
        purchase.setExchangeRate(purchaseRequest.getExchangeRate());
        purchase.setPurchaseImage(purchaseRequest.getImage());
        purchase.setFinalPriceOrder(purchaseRequest.getPurchaseTotal());

        for (OrderLinks orderLink : orderLinks) {
            orderLink.setPurchase(purchase);
            orderLink.setStatus(OrderLinkStatus.DA_MUA);
            orderLink.setShipmentCode(purchaseRequest.getShipmentCode());
        }
        purchase.setOrderLinks(Set.copyOf(orderLinks));
        purchase = purchasesRepository.save(purchase);
        orderLinksRepository.saveAll(orderLinks);
        ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DA_MUA_HANG);

        List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());

        long countcountDamua = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_MUA)
                .count();
        long countCancel = allOrderLinks.stream()
                .filter(link -> link.getStatus() == OrderLinkStatus.DA_HUY)
                .count();
        if (countcountDamua > 0 && (countcountDamua + countCancel == allOrderLinks.size())) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
}
        boolean allOrderLinksArePurchased = allOrderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_MUA);

        if (allOrderLinksArePurchased && !allOrderLinks.isEmpty()) {
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            ordersRepository.save(order);
        }
    
        return purchase;
    
    }

    public Purchases createMoneyExchange(String orderCode, ExchangeRequest exchangeRequest) {
         Orders order = ordersRepository.findByOrderCode(orderCode);
    if (order == null) {
        throw new IllegalArgumentException("Không tìm thấy đơn hàng!");
    }
      if (!order.getStatus().equals(OrderStatus.CHO_MUA)) {
        throw new RuntimeException("Đơn hàng chưa đủ điều kiện để mua hàng!");
    }
    Purchases purchase = new Purchases();
    purchase.setPurchaseCode(generatePurchaseCode());
    purchase.setPurchaseTime(LocalDateTime.now());
    purchase.setStaff((Staff) accountUtils.getAccountCurrent());
    purchase.setOrders(order);
    purchase.setPurchased(false);
    purchase.setFinalPriceOrder(exchangeRequest.getTotal());
    purchase.setNote(exchangeRequest.getNote());
    purchase.setPurchaseImage(exchangeRequest.getImage());
    purchase = purchasesRepository.save(purchase);
    List<OrderLinks> orderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());
    for (OrderLinks orderLink : orderLinks) {
        orderLink.setPurchase(purchase);
        orderLink.setStatus(OrderLinkStatus.DA_GIAO);
    }
    orderLinksRepository.saveAll(orderLinks);
    order.setStatus(OrderStatus.DA_GIAO);
    ordersRepository.save(order);
    ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DA_GIAO);
    return purchase;
    }
    
  public Purchases createAuction(String orderCode, AuctionRequest purchaseRequest) {

    Orders order = ordersRepository.findByOrderCode(orderCode);
    if (order == null) {
        throw new IllegalArgumentException("Không tìm thấy đơn hàng!");
    }

    List<OrderLinks> orderLinks = orderLinksRepository.findByTrackingCodeIn(purchaseRequest.getTrackingCode());
    if (orderLinks.size() != purchaseRequest.getTrackingCode().size()) {
        throw new IllegalArgumentException("Một hoặc nhiều mã không được tìm thấy!");
    }

    boolean anyPurchased = orderLinks.stream().anyMatch(link -> link.getPurchase() != null);
    if (anyPurchased) {
        throw new IllegalArgumentException("Một hoặc nhiều mã đã được mua, không thể mua lại!");
    }

    if (!order.getStatus().equals(OrderStatus.CHO_MUA)) {
        throw new RuntimeException("Đơn hàng chưa đủ điều kiện để mua hàng!");
    }

    boolean allBelongToOrder = orderLinks.stream()
            .allMatch(link -> link.getOrders().getOrderId().equals(order.getOrderId()));
    if (!allBelongToOrder) {
        throw new IllegalArgumentException("Tất cả mã phải thuộc cùng đơn hàng " + orderCode);
    }

    boolean allActive = orderLinks.stream()
            .allMatch(link -> link.getStatus() == OrderLinkStatus.CHO_MUA ||
                              link.getStatus() == OrderLinkStatus.DAU_GIA_THANH_CONG);
    if (!allActive) {
        throw new IllegalArgumentException("Tất cả mã phải ở trạng thái HOẠT ĐỘNG!");
    }

    Purchases purchase = new Purchases();
    purchase.setPurchaseCode(generatePurchaseCode());
    purchase.setPurchaseTime(LocalDateTime.now());
    purchase.setStaff((Staff) accountUtils.getAccountCurrent());
    purchase.setOrders(order);
    purchase.setPurchased(false);
    purchase.setNote(purchaseRequest.getNote());
    purchase.setPurchaseImage(purchaseRequest.getImage());
    purchase.setFinalPriceOrder(purchaseRequest.getPurchaseTotal());

    purchase = purchasesRepository.save(purchase);

    for (OrderLinks link : orderLinks) {
        link.setPurchase(purchase);
        link.setShipWeb(purchaseRequest.getShipWeb());
        link.setPurchaseFee(purchaseRequest.getPurchaseFee());
        link.setPriceWeb(purchaseRequest.getPurchaseTotal());
        link.setStatus(OrderLinkStatus.DAU_GIA_THANH_CONG);
        link.setShipmentCode(purchaseRequest.getShipmentCode());
    }

    orderLinksRepository.saveAll(orderLinks);

    purchase.setOrderLinks(new HashSet<>(orderLinks));

    ordersService.addProcessLog(order, purchase.getPurchaseCode(), ProcessLogAction.DAU_GIA_THANH_CONG);

    List<OrderLinks> allOrderLinks = orderLinksRepository.findByOrdersOrderId(order.getOrderId());

    boolean allValid = allOrderLinks.stream()
            .allMatch(link -> link.getStatus() == OrderLinkStatus.DA_MUA ||
                              link.getStatus() == OrderLinkStatus.DAU_GIA_THANH_CONG ||
                              link.getStatus() == OrderLinkStatus.DA_HUY);

    if (allValid && !allOrderLinks.isEmpty()) {
    BigDecimal purchaseTotal = purchaseRequest.getPurchaseTotal();
    BigDecimal priceBeforeFee = order.getPriceBeforeFee();
    BigDecimal exchange = order.getExchangeRate();
    BigDecimal total = purchaseTotal.add(purchaseRequest.getShipWeb());
    BigDecimal feePercent = purchaseRequest.getPurchaseFee()
        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    BigDecimal fee = total.multiply(feePercent);

    if (total.compareTo(priceBeforeFee) > 0) {
        BigDecimal diff = total.subtract(priceBeforeFee); 
        BigDecimal totalCNY = diff.add(fee);
        BigDecimal paymentAfterAuction = totalCNY.multiply(exchange);
        paymentAfterAuction = round1(paymentAfterAuction);
        order.setPaymentAfterAuction(paymentAfterAuction);
        order.setLeftoverMoney(BigDecimal.ZERO);
        order.setStatus(OrderStatus.DAU_GIA_THANH_CONG);
        purchase.setPurchased(false);
    }

    else if (priceBeforeFee.compareTo(total) > 0) {
        BigDecimal leftoverCNY = priceBeforeFee.subtract(total);
        BigDecimal totalfee = fee;
        if(leftoverCNY.compareTo(totalfee) > 0){
        BigDecimal leftoverFinal = leftoverCNY.subtract(totalfee);
        BigDecimal leftoverVND = leftoverFinal.multiply(exchange).negate();
        leftoverVND = round1(leftoverVND);
        order.setPaymentAfterAuction(BigDecimal.ZERO);
        order.setLeftoverMoney(leftoverVND);
        order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
        purchase.setPurchased(true); 
        }
        else if(leftoverCNY.compareTo(totalfee) < 0) {
            BigDecimal leftoverFinal = totalfee.subtract(leftoverCNY);
            BigDecimal leftoverVND = leftoverFinal.multiply(exchange); 
            leftoverVND = round1(leftoverVND);
            order.setPaymentAfterAuction(BigDecimal.ZERO);
            order.setLeftoverMoney(leftoverVND);
            order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
            purchase.setPurchased(false);
        }
    }

    else {
        BigDecimal totalCNY = fee.add(purchaseRequest.getShipWeb());
        System.out.println("totalCNY (fee + shipWeb): " + totalCNY);
        order.setPaymentAfterAuction(BigDecimal.ZERO);
        order.setLeftoverMoney(round1(totalCNY.multiply(exchange)));
        order.setStatus(OrderStatus.CHO_NHAP_KHO_NN);
        purchase.setPurchased(true);
    }

    purchasesRepository.save(purchase);
    ordersRepository.save(order);
}
    return purchase;
}

    private String generatePurchaseCode() {
          String PurchaseCode;
        do {
            PurchaseCode = "MM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();;
        } while (purchasesRepository.existsByPurchaseCode(PurchaseCode));
        return PurchaseCode;
    }

    public Page<Purchases> getAllPurchases(Pageable pageable) {
        return purchasesRepository.findAll(pageable);
    }

    public PurchaseDetail getPurchaseById(Long purchaseId) {
        Purchases purchases = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng này!"));
        return new PurchaseDetail(purchases);
    }

    public void deletePurchase(Long id) {
        Purchases purchase = purchasesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua này!"));

        if (!purchase.getOrderLinks().isEmpty()) {

        }

        purchasesRepository.delete(purchase);
    }

    public List<PendingShipmentPurchase> getPurchasesWithPendingShipment() {
        List<OrderLinks> pendingLinks = orderLinksRepository.findPendingShipmentLinks();

        return pendingLinks.stream()
                .filter(link -> link.getPurchase() != null)
                .collect(Collectors.groupingBy(OrderLinks::getPurchase))
                .entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> {
                    Purchases p = entry.getKey();
                    List<String> trackingCode = entry.getValue().stream()
                            .map(OrderLinks::getTrackingCode)
                            .toList();

                    PendingShipmentPurchase dto = new PendingShipmentPurchase();
                    dto.setPurchaseId(p.getPurchaseId());
                    dto.setPurchaseCode(p.getPurchaseCode());
                    dto.setOrderCode(p.getOrders().getOrderCode());
                    dto.setPurchaseTime(p.getPurchaseTime());
                    dto.setFinalPriceOrder(p.getFinalPriceOrder());
                    dto.setNote(p.getNote());
                    dto.setPurchaseImage(p.getPurchaseImage());
                    dto.setPendingTrackingCodes(trackingCode);
                    return dto;
                })
                .toList();
    }

    public Purchases updateShipmentForPurchase(Long purchaseId, String shipmentCode) {
         System.out.println("=== METHOD CALLED ===");
        if (shipmentCode == null || shipmentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vận đơn không được để trống!");
        }
        shipmentCode = shipmentCode.trim();

        System.out.println("Checking for existing shipment code: " + shipmentCode);

        if (orderLinksRepository.existsByShipmentCode(shipmentCode)) {
            throw new IllegalArgumentException("Mã vận đơn '" + shipmentCode + "' đã tồn tại!");
        }

        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua!"));

        for (OrderLinks link : purchase.getOrderLinks()) {
            link.setShipmentCode(shipmentCode);
        }

        orderLinksRepository.saveAll(purchase.getOrderLinks());
        return purchase;
    }

    public Purchases updateShipmentForPurchaseAndShipFee(Long purchaseId, String shipmentCode, BigDecimal shipFee) {
        if (shipmentCode == null || shipmentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vận đơn không được để trống!");
        }
        shipmentCode = shipmentCode.trim();

         System.out.println("Checking for existing shipment code: " + shipmentCode);

        if (orderLinksRepository.existsByShipmentCode(shipmentCode)) {
            throw new IllegalArgumentException("Mã vận đơn '" + shipmentCode + "' đã tồn tại!");
        }

        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua!"));

        var exchangeRate = purchase.getOrders().getExchangeRate();
        var fee = shipFee.multiply(exchangeRate);

        var order = purchase.getOrders();
        order.setLeftoverMoney(
        (order.getLeftoverMoney() == null ? BigDecimal.ZERO : order.getLeftoverMoney())
            .add(fee)
        );

        for (OrderLinks link : purchase.getOrderLinks()) {
            link.setShipmentCode(shipmentCode);
            link.setShipWeb(shipFee);
        }

        orderLinksRepository.saveAll(purchase.getOrderLinks());

        return purchase;
    }

    public Page<PurchasePendingShipment> getPendingShipmentPurchases(Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();

        Set<Long> routeIds = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId())
                .stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        if (routeIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Purchases> purchasesPage = purchasesRepository.findPurchasesWithPendingShipmentByRoutes(routeIds, pageable);

        return purchasesPage.map(purchase -> {
            List<OrderLinkPending> pendingLinks = purchase.getOrderLinks().stream()
                    .filter(link -> link.getShipmentCode() == null || link.getShipmentCode().trim().isEmpty())
                    .map(OrderLinkPending::new)
                    .collect(Collectors.toList());

            return new PurchasePendingShipment(purchase, pendingLinks);
        });
    }

    public Page<PurchasePendingShipment> getFullPurchases(
        PurchaseFilter status,
        String orderCode,
        String customerCode,
        Pageable pageable
) {
    Account currentAccount = accountUtils.getAccountCurrent();

    Set<Long> routeIds = accountRouteRepository
            .findByAccountAccountId(currentAccount.getAccountId())
            .stream()
            .map(AccountRoute::getRoute)
            .map(Route::getRouteId)
            .collect(Collectors.toSet());

    if (routeIds.isEmpty()) {
        return Page.empty(pageable);
    }

    String statusValue = (status == null ? null : status.name());

    orderCode = normalize(orderCode);
    customerCode = normalize(customerCode);

    Page<Purchases> purchasesPage =
            purchasesRepository.findPurchasesSortedByPendingShipment(
                    routeIds,
                    statusValue,
                    orderCode,
                    customerCode,
                    pageable
            );

    return purchasesPage.map(purchase -> {
        List<OrderLinkPending> pendingLinks = purchase.getOrderLinks()
                .stream()
                .map(OrderLinkPending::new)
                .toList();

        return new PurchasePendingShipment(purchase, pendingLinks);
    });
}

private String normalize(String value) {
    if (value == null) return null;
    value = value.trim();
    return value.isEmpty() ? null : value;
}

public Page<PurchasePendingShipment> getPurchasesWithFilteredOrderLinks(
        PurchaseFilter status,
        String orderCode,
        String customerCode,
        String shipmentCode,
        Pageable pageable
) {
    Account currentAccount = accountUtils.getAccountCurrent();


    Set<Long> routeIds = accountRouteRepository
            .findByAccountAccountId(currentAccount.getAccountId())
            .stream()
            .map(AccountRoute::getRoute)
            .map(Route::getRouteId)
            .collect(Collectors.toSet());

    if (routeIds.isEmpty()) {
        return Page.empty(pageable);
    }

    // 🔹 Normalize input (KHÔNG gán lại biến gốc)
    final String normalizedOrderCode = normalize(orderCode);
    final String normalizedCustomerCode = normalize(customerCode);
    final String normalizedShipmentCode = normalize(shipmentCode);
    final String statusValue = status == null ? null : status.name();

    // 🔹 Query DB
    Page<Purchases> purchasesPage =
            purchasesRepository.findPurchasesWithFilteredOrderLinks(
                    routeIds,
                    statusValue,
                    normalizedOrderCode,
                    normalizedCustomerCode,
                    normalizedShipmentCode,
                    pageable
            );

    // 🔹 Map sang DTO
    return purchasesPage.map(purchase -> {

        List<OrderLinkPending> pendingLinks = purchase.getOrderLinks()
                .stream()
                .filter(ol ->
                        // filter status
                        (statusValue == null || ol.getStatus().name().equals(statusValue))
                        // filter shipmentCode
                        && (normalizedShipmentCode == null
                            || (ol.getShipmentCode() != null
                                && ol.getShipmentCode()
                                     .toLowerCase()
                                     .contains(normalizedShipmentCode)))
                )
                .map(OrderLinkPending::new)
                .toList();

        return new PurchasePendingShipment(purchase, pendingLinks);
    });
}

  @Transactional
    public Purchases updatePurchase(Long purchaseId, UpdatePurchaseRequest request) {

        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua!"));

        if (request.getFinalPriceOrder() != null) {
            purchase.setFinalPriceOrder(request.getFinalPriceOrder());
        }
        if (request.getNote() != null) {
            purchase.setNote(request.getNote());
        }
        if (request.getImagePurchased() != null && !request.getImagePurchased().isEmpty()) {
            purchase.setPurchaseImage(request.getImagePurchased());
        }

        if (request.getShipmentCode() != null) {
            String newShipmentCode = request.getShipmentCode().trim();
          
            String shipmentCode = request.getShipmentCode().trim();
        

            Set<OrderLinks> orderLinks = purchase.getOrderLinks();
            if (orderLinks == null || orderLinks.isEmpty()) {
                throw new IllegalArgumentException("Giao dịch mua chưa có OrderLinks để cập nhật mã vận đơn!");
            }

        if (shipmentCode.isEmpty()) {
            for (OrderLinks link : orderLinks) {
                link.setShipmentCode(""); 
            }
            orderLinksRepository.saveAll(orderLinks);
        }
        // 🔑 CÓ GIÁ TRỊ → CHECK TRÙNG & UPDATE
        else {
            OrderLinks firstLink = orderLinks.iterator().next();
            Orders order = firstLink.getOrders();
            if (order == null) {
                throw new IllegalArgumentException("OrderLinks chưa gắn Orders!");
            }

            Long currentOrderId = order.getOrderId();

            if (orderLinksRepository
                    .existsByShipmentCodeAndOrders_OrderIdNot(shipmentCode, currentOrderId)) {
                throw new IllegalArgumentException(
                        "Mã vận đơn '" + shipmentCode + "' đã tồn tại ở đơn khác!"
                );
            }

            for (OrderLinks link : orderLinks) {
                link.setShipmentCode(shipmentCode);
            }
            orderLinksRepository.saveAll(orderLinks);
        }
    }

    return purchasesRepository.save(purchase);
}

    private BigDecimal round1(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO;
        return v.setScale(1, RoundingMode.HALF_UP);
    }

}
