package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class OrderWithLinks {

    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private Boolean checkRequired;
    private LocalDateTime pinnedAt;

    private List<OrderLinks> orderLinks;

    public OrderWithLinks() {
    }

    public OrderWithLinks(Orders order) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.exchangeRate = order.getExchangeRate();
        this.finalPriceOrder = order.getFinalPriceOrder();
        this.checkRequired = order.getCheckRequired();
        this.pinnedAt = order.getPinnedAt();
        this.orderLinks = new ArrayList<>(order.getOrderLinks());
    }

       public OrderWithLinks(OrderSummaryDTO order) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.exchangeRate = order.getExchangeRate();
        this.finalPriceOrder = order.getFinalPriceOrder();
        this.checkRequired = order.getCheckRequired();
        this.pinnedAt = order.getPinnedAt();
    }

    // ✅ MỚI: setter cho DTO
    public void setOrderLinkSummaries(List<OrderLinkSummaryDTO> links) {
        if (links == null) {
            this.orderLinks = null;
            return;
        }

        // map DTO -> Entity-lite (KHÔNG attach Hibernate)
        this.orderLinks =
                links.stream()
                        .map(dto -> {
                            OrderLinks ol = new OrderLinks();
                            ol.setLinkId(dto.getLinkId());
                            ol.setProductName(dto.getProductName());
                            ol.setQuantity(dto.getQuantity());
                            ol.setPriceWeb(dto.getPriceWeb());
                            ol.setTotalWeb(dto.getTotalWeb());
                            ol.setPurchaseFee(dto.getPurchaseFee());
                            ol.setExtraCharge(dto.getExtraCharge());
                            ol.setFinalPriceVnd(dto.getFinalPriceVnd());
                            ol.setShipmentCode(dto.getShipmentCode());
                            ol.setProductLink(dto.getProductLink());
                            ol.setNote(dto.getNote());
                            ol.setShipWeb(dto.getShipWeb());
                            ol.setWebsite(dto.getWebsite());
                            ol.setClassify(dto.getClassify());
                            ol.setPurchaseImage(dto.getPurchaseImage());
                            ol.setTrackingCode(dto.getTrackingCode());
                            ol.setStatus(dto.getStatus());
                            ol.setGroupTag(dto.getGroupTag());
                            return ol;
                        })
                        .toList();
    }
}
