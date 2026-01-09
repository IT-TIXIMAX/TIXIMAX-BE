package com.tiximax.txm.Model.DTORequest.Payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class SmsRequest {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    private List<SmsItem> data;

    @Data
    @NoArgsConstructor
    public static class SmsItem {  // Inner class gọn
        @JsonProperty("amount")
        private long amount;

        @JsonProperty("content")
        private String content;
    }
}