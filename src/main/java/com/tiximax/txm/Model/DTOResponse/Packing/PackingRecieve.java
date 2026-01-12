package com.tiximax.txm.Model.DTOResponse.Packing;

import java.util.List;

import lombok.Data;
@Data
public class PackingRecieve {
    public String packingCode;
    public List<String> trackingCode;
}
