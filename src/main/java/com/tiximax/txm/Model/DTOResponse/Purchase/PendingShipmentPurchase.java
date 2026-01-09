package com.tiximax.txm.Model.DTOResponse.Purchase;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter

public class PendingShipmentPurchase {

    Long purchaseId;

    String purchaseCode;

    String orderCode;

    LocalDateTime purchaseTime;

    BigDecimal finalPriceOrder;

    String note;

    String purchaseImage;

    List<String> pendingTrackingCodes;

}
