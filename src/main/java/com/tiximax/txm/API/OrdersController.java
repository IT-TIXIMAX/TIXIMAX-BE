package com.tiximax.txm.API;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderDestination;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Model.EnumFilter.ShipStatus;
import com.tiximax.txm.Service.OrdersService;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/orders")
@SecurityRequirement(name = "bearerAuth")

public class OrdersController {

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OrdersService ordersService;

    @PostMapping("/{customerCode}/{routeId}/{addressId}")
    public ResponseEntity<Orders> createdReview(@PathVariable String customerCode,
                                                @PathVariable long routeId,
                                                @PathVariable long addressId,
                                                @RequestBody OrdersRequest ordersRequest) throws IOException {
        Orders orders = ordersService.addOrder(customerCode, routeId, addressId,ordersRequest);
        return ResponseEntity.ok(orders);
    }
    @PostMapping("/money-exchange/{customerCode}/{routeId}")
    public ResponseEntity<Orders> moneyExchange(
            @PathVariable String customerCode,
            @PathVariable Long routeId,
            @RequestBody MoneyExchangeRequest moneyExchangeRequest
    ) throws IOException {

        Orders order = ordersService.MoneyExchange(
                customerCode,
                routeId,
                moneyExchangeRequest
        );

        return ResponseEntity.ok(order);
    }


    @PostMapping("/deposit/{customerCode}/{routeId}/{addressId}")
    public ResponseEntity<Orders> createdConsignment(@PathVariable String customerCode,
                                                     @PathVariable long routeId,
                                                     @PathVariable long addressId,
                                                     @RequestBody ConsignmentRequest consignmentRequest) throws IOException {
        Orders orders = ordersService.addConsignment(customerCode, routeId, addressId,consignmentRequest);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("order-link/cancel/{orderId}/{orderLinkId}")
    public ResponseEntity<Orders> CancelOrderLink(@PathVariable Long orderId, @PathVariable Long orderLinkId) {
        Orders orders = ordersService.updateStatusOrderLink(orderId, orderLinkId);  
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/enum-order-types")
    public ResponseEntity<List<String>> getOrderTypes() {
        List<String> orderTypes = Arrays.stream(OrderType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(orderTypes);
    }

    @GetMapping("/enum-order-destination")
    public ResponseEntity<List<String>> getOrderDestination() {
        List<String> orderDestination = Arrays.stream(OrderDestination.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(orderDestination);
    }

    @GetMapping("/enum-order-type")
    public ResponseEntity<List<String>> getOrderType() {
        List<String> orderType = Arrays.stream(OrderType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(orderType);
    }

   @GetMapping("/{page}/{size}")
public ResponseEntity<Page<Orders>> getAllOrders(
        @PathVariable int page,
        @PathVariable int size,
        @RequestParam(required = false) String shipmentCode,
        @RequestParam(required = false) String customerCode, 
        @RequestParam(required = false) String orderCode
) {
    Sort sort = Sort.by("createdAt").descending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Orders> ordersPage = ordersService.getAllOrdersPaging(pageable, shipmentCode, customerCode, orderCode); // Pass filter params
    return ResponseEntity.ok(ordersPage);
}


    @GetMapping("/{page}/{size}/{status}/paging")
    public ResponseEntity<Page<Orders>> getOrdersPaging(@PathVariable int page, int size, @PathVariable(required = false) OrderStatus status) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Orders> ordersPage = ordersService.getOrdersPaging(pageable, status);
        return ResponseEntity.ok(ordersPage);
    }

    @GetMapping("/list-for-purchase")
    public ResponseEntity<List<Orders>> getOrdersForCurrentStaff() {
        List<Orders> orders = ordersService.getOrdersForCurrentStaff();
        return ResponseEntity.ok(orders);
    }

     @GetMapping("/for-payment/{page}/{size}/{status}")
    public ResponseEntity<Page<OrderPayment>> getOrdersForPayment(
            @PathVariable int page,
            @PathVariable int size,
            @PathVariable OrderStatus status,
            @RequestParam(required = false) String orderCode
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<OrderPayment> ordersPage =
            ordersService.getOrdersForPayment(pageable, status, orderCode);
        return ResponseEntity.ok(ordersPage);
    }

    @GetMapping("/detail/{orderId}")
    public ResponseEntity<OrderDetail> getOrderDetail(@PathVariable Long orderId) {
        OrderDetail orderDetail = ordersService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

@GetMapping("/with-links/{page}/{size}")
public ResponseEntity<Page<OrderWithLinks>> getOrdersWithLinksForPurchaser(
        @PathVariable int page,
        @PathVariable int size,
        @RequestParam OrderType orderType,
        @RequestParam(required = false) String orderCode,
        @RequestParam(required = false) String customerCode
) {
    Pageable pageable = PageRequest.of(page, size);

    Page<OrderWithLinks> ordersPage =
            ordersService.getOrdersWithLinksForPurchaser(
                    pageable,
                    orderType,
                    orderCode,
                    customerCode
            );

    return ResponseEntity.ok(ordersPage);
}


    @GetMapping("/orderLink/{orderLinkId}")
    public ResponseEntity<OrderLinkWithStaff> getOrderLinkById(@PathVariable Long orderLinkId) {
        OrderLinkWithStaff orderLink = ordersService.getOrderLinkById(orderLinkId);
        return ResponseEntity.ok(orderLink);
    }

    @GetMapping("/statistics/for-payment")
    public ResponseEntity<Map<String, Long>> getOrderStatusStatistics() {
        Map<String, Long> statistics = ordersService.getOrderStatusStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/orders/by-customer/{customerCode}")
    public ResponseEntity<List<OrderPayment>> getOrdersByCustomer(@PathVariable String customerCode) {
        List<OrderPayment> orders = ordersService.getOrdersByCustomerCode(customerCode);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/payment-auction/by-customer/{customerCode}")
    public ResponseEntity<List<OrderPayment>> getAuctionByCustomer(@PathVariable String customerCode) {
        List<OrderPayment> orders = ordersService.getAfterPaymentAuctionsByCustomerCode(customerCode);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders-shipping/by-customer/{customerCode}")
    public ResponseEntity<List<OrderPayment>> getOrdersShippingByCustomer(@PathVariable String customerCode) {
        List<OrderPayment> orders = ordersService.getOrdersShippingByCustomerCode(customerCode);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/partial-for-customer/{customerCode}")
    public ResponseEntity<List<WareHouseOrderLink>> getLinksByCustomer(@PathVariable String customerCode) {
        List<WareHouseOrderLink> links = ordersService.getLinksInWarehouseByCustomer(customerCode);
        return ResponseEntity.ok(links);
    }
    @GetMapping("/warehouse-links/{page}/{size}")
        public ResponseEntity<Page<ShipLinks>> getOrderLinksForWarehouse(
        @PathVariable int page,
        @PathVariable int size,
        @RequestParam(required = false) ShipStatus status,
        @RequestParam(required = false) String shipmentCode,
        @RequestParam(required = false) String customerCode
) {
    Pageable pageable = PageRequest.of(page, size);

    Page<ShipLinks> result  = ordersService.getOrderLinksForWarehouse(
            pageable,
            status,
            shipmentCode,
            customerCode
    );
      return ResponseEntity.ok(result);
}

 @GetMapping("/warehouse-foreign-links/{page}/{size}")
public ResponseEntity<Page<ShipLinkForegin>> getOrderLinksForWarehouseForeign(
        @PathVariable int page,
        @PathVariable int size,
        @RequestParam(required = false) String shipmentCode,
        @RequestParam(required = false) String customerCode
) {
    Pageable pageable = PageRequest.of(page, size);

    Page<ShipLinkForegin> result = ordersService.getOrderLinksForForeignWarehouse(
            pageable,   
            shipmentCode,
            customerCode      
    );
    return ResponseEntity.ok(result);
}

    @PutMapping("/buy-later/{orderId}/links/{orderLinkId}")
    public ResponseEntity<Orders> updateOrderLinkToBuyLater(@PathVariable Long orderId, @PathVariable Long orderLinkId) {
        Orders updatedOrder = ordersService.updateOrderLinkToBuyLater(orderId, orderLinkId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/pin/{orderId}")
    public ResponseEntity<Void> pinOrder(@PathVariable Long orderId, @RequestParam boolean pin) {
        ordersService.pinOrder(orderId, pin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ready-for-partial/{page}/{size}")
    public ResponseEntity<List<OrderPayment>> getReadyOrdersForPartial(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        List<OrderPayment> readyOrders = ordersService.getReadyOrdersForPartial(pageable);
        return ResponseEntity.ok(readyOrders);
    }

    @GetMapping("/refund/{page}/{size}")
    public ResponseEntity<Page<RefundResponse>> getOrdersWithNegativeLeftoverMoney(
            @PathVariable int page,
            @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<RefundResponse> ordersPage = ordersService.getOrdersWithNegativeLeftoverMoney(pageable);
        return ResponseEntity.ok(ordersPage);
    }

    @PutMapping("/refund-confirm/{orderId}")
    public ResponseEntity<Orders> processNegativeLeftoverMoney(
            @PathVariable Long orderId,
            @RequestParam(required = false) String image,
            @RequestParam boolean refundToCustomer) {
        Orders updatedOrder = ordersService.processNegativeLeftoverMoney(orderId, image, refundToCustomer);
        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/buy-later/{page}/{size}")
    public ResponseEntity<Page<OrderWithLinks>> getBuyLaterOrders(
            @PathVariable int page,
            @PathVariable int size,
            @RequestParam OrderType orderType) {

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderWithLinks> result = ordersService.getOrdersWithBuyLaterLinks(pageable, orderType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/info-shipment/{shipmentCode}")
    public ResponseEntity<InfoShipmentCode> inforShipmentCode(
            @PathVariable String shipmentCode) {
        return ResponseEntity.ok(ordersService.inforShipmentCode(shipmentCode));
    }

    @GetMapping("/leftover-positive/{customerCode}")
    public ResponseEntity<CustomerBalanceAndOrders> getOrdersWithPositiveLeftoverByCustomer(@PathVariable String customerCode) {
        CustomerBalanceAndOrders orders = ordersService.getOrdersWithNegativeLeftoverByCustomerCode(customerCode);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/by-shipment/{shipmentCode}")
    public ResponseEntity<OrderByShipmentResponse> getOrderByShipmentCode(@PathVariable String shipmentCode) {
        OrderByShipmentResponse response = ordersService.getOrderByShipmentCode(shipmentCode);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-destination/batch")
    public ResponseEntity<List<Orders>> updateDestinationBatch(
            @RequestBody UpdateDestinationBatchRequest request) {

        if (request.getShipmentCodes() == null || request.getShipmentCodes().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Orders> updatedOrders = ordersService.updateDestinationByShipmentCodes(
                request.getShipmentCodes(),
                request.getDestinationId()
        );

        return ResponseEntity.ok(updatedOrders);
    }

    @GetMapping("/without-shipment/{page}/{size}")
    public ResponseEntity<Page<OrdersPendingShipment>> getOrdersWithoutShipmentCode(
            @PathVariable int page,
            @PathVariable int size) {

        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrdersPendingShipment> result = ordersService.getMyOrdersWithoutShipmentCode(pageable);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/shipmentCode/{orderId}/{orderLinkId}/{shipmentCode}")
    public ResponseEntity<OrderWithLinks> updateShipmentCode(
            @PathVariable Long orderId,
            @PathVariable Long orderLinkId,
            @PathVariable String shipmentCode) {

        if (shipmentCode == null || shipmentCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        OrderWithLinks updated = ordersService.updateShipmentCode(orderId, orderLinkId, shipmentCode.trim());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/search-shipment/{keyword}/{page}/{size}")
    public ResponseEntity<Page<OrderWithLinks>> searchOrders(
            @PathVariable String keyword,
            @PathVariable int page,
            @PathVariable int size) {

        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderWithLinks> result = ordersService.searchOrdersByKeyword(keyword, pageable);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        ordersService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shipments-by-phone/{phone}")
    public ResponseEntity<List<ShipmentGroup>> getShipmentsByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(ordersService.getShipmentsByCustomerPhone(phone.trim()));
    }
}
