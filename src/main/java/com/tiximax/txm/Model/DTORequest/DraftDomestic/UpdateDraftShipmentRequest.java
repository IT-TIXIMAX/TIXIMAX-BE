package com.tiximax.txm.Model.DTORequest.DraftDomestic;

import java.util.List;
import lombok.Data;

@Data
public class UpdateDraftShipmentRequest {
    private List<String> shippingCodes;
}
