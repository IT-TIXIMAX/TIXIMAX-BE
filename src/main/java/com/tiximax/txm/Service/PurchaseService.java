package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import com.tiximax.txm.Enums.ProcessLogAction;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Model.DTORequest.Order.AuctionRequest;
import com.tiximax.txm.Model.DTORequest.Purchase.ExchangeRequest;
import com.tiximax.txm.Model.DTORequest.Purchase.PurchaseRequest;
import com.tiximax.txm.Model.DTORequest.Purchase.UpdatePurchaseRequest;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending;
import com.tiximax.txm.Model.DTOResponse.Purchase.PendingShipmentPurchase;
import com.tiximax.txm.Model.DTOResponse.Purchase.PurchaseDetail;
import com.tiximax.txm.Model.DTOResponse.Purchase.PurchasePendingShipment;
import com.tiximax.txm.Model.EnumFilter.PurchaseFilter;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import java.sql.Timestamp;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private OrdersService ordersService;
    @Autowired
    private AccountRouteRepository accountRouteRepository;
    @Autowired
    private AccountUtils accountUtils;

    public Purchases createPurchase(String orderCode, PurchaseRequest purchaseRequest) {
        Orders order = ordersRepository.findByOrderCode(orderCode);

        if (order == null) {
            throw new NotFoundException("Không tìm thấy đơn hàng!");
        }

        List<OrderLinks> orderLinks = orderLinksRepository.findByTrackingCodeIn(purchaseRequest.getTrackingCode());
        if (orderLinks.size() != purchaseRequest.getTrackingCode().size()) {
            throw new NotFoundException("Một hoặc nhiều mã không được tìm thấy!");
        }
        BigDecimal priceLinks = BigDecimal.ZERO;

        for (OrderLinks ol : orderLinks){
            priceLinks = priceLinks.add(ol.getTotalWeb());
        }

        if (!(order.getStatus().equals(OrderStatus.CHO_MUA) || order.getStatus().equals(OrderStatus.CHO_NHAP_KHO_NN))){
            throw new BadRequestException("Đơn hàng chưa đủ điều kiện để mua hàng!");
        }

        boolean allBelongToOrder = orderLinks.stream()
                .allMatch(link -> link.getOrders().getOrderId().equals(order.getOrderId()));
        if (!allBelongToOrder) {
            throw new BadRequestException("Tất cả mã phải thuộc cùng đơn hàng " + orderCode);
        }

        if(purchaseRequest.getShipmentCode() != ""){
            if (orderLinksRepository.existsByShipmentCode(purchaseRequest.getShipmentCode())) {
                throw new BadRequestException("Một hoặc nhiều mã đã có mã vận đơn, không thể mua lại!");
            }
        }

        boolean allActive = orderLinks.stream()
                .allMatch(link -> link.getStatus() == OrderLinkStatus.CHO_MUA || link.getStatus() == OrderLinkStatus.MUA_SAU);
        if (!allActive) {
            throw new BadRequestException("Tất cả mã phải ở trạng thái chờ mua!");
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
        throw new NotFoundException("Không tìm thấy đơn hàng!");
    }
      if (!order.getStatus().equals(OrderStatus.CHO_MUA)) {
        throw new BadRequestException("Đơn hàng chưa đủ điều kiện để mua hàng!");
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
        throw new NotFoundException("Không tìm thấy đơn hàng!");
    }

    List<OrderLinks> orderLinks = orderLinksRepository.findByTrackingCodeIn(purchaseRequest.getTrackingCode());
    if (orderLinks.size() != purchaseRequest.getTrackingCode().size()) {
        throw new NotFoundException("Một hoặc nhiều mã không được tìm thấy!");
    }

    boolean anyPurchased = orderLinks.stream().anyMatch(link -> link.getPurchase() != null);
    if (anyPurchased) {
        throw new BadRequestException("Một hoặc nhiều mã đã được mua, không thể mua lại!");
    }

    if (!order.getStatus().equals(OrderStatus.CHO_MUA)) {
        throw new BadRequestException("Đơn hàng chưa đủ điều kiện để mua hàng!");
    }

    boolean allBelongToOrder = orderLinks.stream()
            .allMatch(link -> link.getOrders().getOrderId().equals(order.getOrderId()));
    if (!allBelongToOrder) {
        throw new BadRequestException("Tất cả mã phải thuộc cùng đơn hàng " + orderCode);
    }

    boolean allActive = orderLinks.stream()
            .allMatch(link -> link.getStatus() == OrderLinkStatus.CHO_MUA ||
                              link.getStatus() == OrderLinkStatus.DAU_GIA_THANH_CONG);
    if (!allActive) {
        throw new BadRequestException("Tất cả mã phải ở trạng thái HOẠT ĐỘNG!");
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
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng này!"));
        return new PurchaseDetail(purchases);
    }

    public void deletePurchase(Long id) {
        Purchases purchase = purchasesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch mua này!"));

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
            throw new BadRequestException("Mã vận đơn không được để trống!");
        }
        shipmentCode = shipmentCode.trim();

        System.out.println("Checking for existing shipment code: " + shipmentCode);

        if (orderLinksRepository.existsByShipmentCode(shipmentCode)) {
            throw new BadRequestException("Mã vận đơn '" + shipmentCode + "' đã tồn tại!");
        }

        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch mua!"));
        for (OrderLinks link : purchase.getOrderLinks()) {
            link.setShipmentCode(shipmentCode);
        }

        orderLinksRepository.saveAll(purchase.getOrderLinks());
        return purchase;
    }

    public Purchases updateShipmentForPurchaseAndShipFee(Long purchaseId, String shipmentCode, BigDecimal shipFee) {
        if (shipmentCode == null || shipmentCode.trim().isEmpty()) {
            throw new BadRequestException("Mã vận đơn không được để trống!");
        }
        shipmentCode = shipmentCode.trim();

         System.out.println("Checking for existing shipment code: " + shipmentCode);

        if (orderLinksRepository.existsByShipmentCode(shipmentCode)) {
            throw new BadRequestException("Mã vận đơn '" + shipmentCode + "' đã tồn tại!");
        }

        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch mua!"));
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

    // public Page<PurchasePendingShipment> getPendingShipmentPurchases(Pageable pageable) {
    //     Account currentAccount = accountUtils.getAccountCurrent();

    //     Set<Long> routeIds = accountRouteRepository.findByAccountAccountId(currentAccount.getAccountId())
    //             .stream()
    //             .map(AccountRoute::getRoute)
    //             .map(Route::getRouteId)
    //             .collect(Collectors.toSet());

    //     if (routeIds.isEmpty()) {
    //         return Page.empty(pageable);
    //     }

    //     Page<Purchases> purchasesPage = purchasesRepository.findPurchasesWithPendingShipmentByRoutes(routeIds, pageable);

    //     return purchasesPage.map(purchase -> {
    //         List<OrderLinkPending> pendingLinks = purchase.getOrderLinks().stream()
    //                 .filter(link -> link.getShipmentCode() == null || link.getShipmentCode().trim().isEmpty())
    //                 .map(OrderLinkPending::new)
    //                 .collect(Collectors.toList());

    //         return new PurchasePendingShipment(purchase, pendingLinks);
    //     });
    // }


public Page<PurchasePendingShipment> getFullPurchases(
        PurchaseFilter status,
        String orderCode,
        String customerCode,
        Pageable pageable
) {
    Account currentAccount = accountUtils.getAccountCurrent();

    Set<Long> routeIds =
            accountRouteRepository
                    .findByAccountAccountId(currentAccount.getAccountId())
                    .stream()
                    .map(ar -> ar.getRoute().getRouteId())
                    .collect(Collectors.toSet());

    if (routeIds.isEmpty()) {
        return Page.empty(pageable);
    }

    // normalize filter
    String statusValue = (status == null ? null : status.name());
    orderCode = normalize(orderCode);
    customerCode = normalize(customerCode);

    // 1️⃣ PAGE ID (ĐÃ FILTER)
    Page<Long> pageIds =
            purchasesRepository.findPurchaseIdsPendingShipment(
                    routeIds,
                    statusValue,
                    orderCode,
                    customerCode,
                    pageable
            );

    if (pageIds.isEmpty()) {
        return Page.empty(pageable);
    }

    Set<Long> purchaseIds = new HashSet<>(pageIds.getContent());

    // 2️⃣ HEADER
    Map<Long, PurchasePendingShipment> dtoMap = new HashMap<>();
    List<Object[]> rows =
            purchasesRepository.findPurchaseHeadersRaw(purchaseIds);

    for (Object[] r : rows) {
        PurchasePendingShipment dto =
                new PurchasePendingShipment(
                        ((Number) r[0]).longValue(),                 // purchaseId
                        (String) r[1],                                // purchaseCode
                        ((Timestamp) r[2]).toLocalDateTime(),         // purchaseTime
                        (String) r[3],                                // purchaseImage
                        (BigDecimal) r[4],                            // finalPriceOrder
                        ((Number) r[5]).longValue(),                  // orderId
                        (String) r[6],                                // orderCode
                        (String) r[7],                                // staffName
                        (String) r[8]                                 // note
                );
        dtoMap.put(dto.getPurchaseId(), dto);
    }

    // 3️⃣ ORDER LINKS (DTO)
    List<OrderLinkPending> links =
            orderLinksRepository.findPendingLinksDTO(purchaseIds);

    links.forEach(link -> {
        PurchasePendingShipment dto = dtoMap.get(link.getPurchaseId());
        if (dto != null) {
            dto.getPendingLinks().add(link);
        }
    });

    // 4️⃣ GIỮ ĐÚNG THỨ TỰ PAGE
    List<PurchasePendingShipment> content =
            pageIds.getContent()
                   .stream()
                   .map(dtoMap::get)
                   .filter(Objects::nonNull)
                   .toList();

    return new PageImpl<>(
            content,
            pageable,
            pageIds.getTotalElements()
    );
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

    Set<Long> routeIds =
            accountRouteRepository
                    .findByAccountAccountId(currentAccount.getAccountId())
                    .stream()
                    .map(AccountRoute::getRoute)
                    .map(Route::getRouteId)
                    .collect(Collectors.toSet());

    if (routeIds.isEmpty()) {
        return Page.empty(pageable);
    }

    String normalizedOrderCode = normalize(orderCode);
    String normalizedCustomerCode = normalize(customerCode);
    String normalizedShipmentCode = normalize(shipmentCode);

if (normalizedShipmentCode != null) {
    normalizedShipmentCode = normalizedShipmentCode.toUpperCase(Locale.ROOT);
}

    String statusValue = status == null ? null : status.name();

    Page<Long> purchaseIdPage =
            purchasesRepository.findPurchaseIdsFiltered(
                    routeIds,
                    statusValue,
                    normalizedOrderCode,
                    normalizedCustomerCode,
                    normalizedShipmentCode,
                    pageable
            );

    if (purchaseIdPage.isEmpty()) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    List<Long> purchaseIds = purchaseIdPage.getContent();

    // 1️⃣ Load purchase DTO
    List<PurchasePendingShipment> purchases =
            purchasesRepository.findPurchasePendingDTO(purchaseIds);

    Map<Long, PurchasePendingShipment> purchaseMap =
            purchases.stream()
                    .collect(Collectors.toMap(
                            PurchasePendingShipment::getPurchaseId,
                            p -> p
                    ));

    OrderLinkStatus linkStatus =
            status == null ? null : OrderLinkStatus.valueOf(status.name());

   List<OrderLinkPending> links;

    if (normalizedShipmentCode == null) {
        links = orderLinksRepository.findOrderLinkPendingWithoutShipmentCode(
                purchaseIds,
                linkStatus
        );
    } else {
        links = orderLinksRepository.findOrderLinkPendingDTO(
                purchaseIds,
                linkStatus,
                normalizedShipmentCode
        );
    }

    // 3️⃣ Gán links vào purchase
    links.forEach(link -> {
        PurchasePendingShipment p =
                purchaseMap.get(link.getPurchaseId());
        if (p != null) {
            p.getPendingLinks().add(link);
        }
    });

    return new PageImpl<>(
            purchases,
            pageable,
            purchaseIdPage.getTotalElements()
    );
}
  @Transactional
    public Purchases updatePurchase(Long purchaseId, UpdatePurchaseRequest request) {

        Purchases purchase = purchasesRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch mua!"));

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
                throw new BadRequestException("Giao dịch mua chưa có OrderLinks để cập nhật mã vận đơn!");
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
                throw new BadRequestException("OrderLinks chưa gắn Orders!");
            }

            Long currentOrderId = order.getOrderId();

            if (orderLinksRepository
                    .existsByShipmentCodeAndOrders_OrderIdNot(shipmentCode, currentOrderId)) {
                throw new BadRequestException(
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
