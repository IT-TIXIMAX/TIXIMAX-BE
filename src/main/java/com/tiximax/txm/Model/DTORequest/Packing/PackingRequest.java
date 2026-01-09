package com.tiximax.txm.Model.DTORequest.Packing;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter

public class PackingRequest {

    private Long destinationId;

    private List<String> shipmentCodes;

}
