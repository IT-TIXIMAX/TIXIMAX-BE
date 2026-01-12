package com.tiximax.txm.Model.DTORequest.Domestic;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Data
@Getter
@Setter

public class CreateDomesticRequest {

    private List<String> packingCode;

    private String note;

}