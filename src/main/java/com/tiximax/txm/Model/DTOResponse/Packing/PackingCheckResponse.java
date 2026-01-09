package com.tiximax.txm.Model.DTOResponse.Packing;

import java.util.List;
import lombok.Data;
@Data
public class PackingCheckResponse {

    private boolean canCreate;
    private int totalCodes;
    private int warehouseCount;
    private List<String> invalidCodes;
    private List<String> notImportedCodes;
    private List<String> alreadyPackedCodes;
    private String message;
}
