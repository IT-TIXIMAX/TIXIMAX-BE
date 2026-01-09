package com.tiximax.txm.Model.DTOResponse.Auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class EmailDetail {
    private String recipient;
    private String msgBody;
    private String subject;
    private String fullName;
    private String attachment;
    private String buttonValue;
    private String link;
    private String valuation;
    private String productName;
    private Date date;
    private long auctionId;
}
