package com.tiximax.txm.Model.DTORequest.Payment;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class SmsRequest {
    private String sender;
    private BigDecimal amount;
    private String content;
    private String sms;
    private long timestamp;
    private String deviceId;
}