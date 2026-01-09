package com.tiximax.txm.Model.DTORequest.OrderLink;

import com.tiximax.txm.Enums.OrderLinkProductType;
import com.tiximax.txm.Enums.OrderLinkStatus;
import com.tiximax.txm.Enums.OrderLinkWebsite;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class OrderLinkRequest {

    private String productLink;
 
    private Integer quantity;

    private BigDecimal priceWeb;

    private BigDecimal shipWeb;

    private String productName;

    private BigDecimal purchaseFee;

    private BigDecimal extraCharge;

    private String purchaseImage;

    private String classify;

    private String website;

    private Long productTypeId;

    private String note;

    private String groupTag;

}
