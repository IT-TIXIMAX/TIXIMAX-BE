package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.Domestic;
import com.tiximax.txm.Entity.Packing;
import com.tiximax.txm.Enums.DomesticStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class DomesticResponse {
    private Long domesticId;
    private DomesticStatus status;
    private String note;
    private LocalDateTime timestamp;
    private String fromLocationName;
    private String toLocationName;
    private String toAddressName;

    private List<String> shippingList;

    // ✅ Constructor tiện lợi để map từ entity
    public static DomesticResponse fromEntity(Domestic domestic) {
        DomesticResponse response = new DomesticResponse();
        response.setDomesticId(domestic.getDomesticId());
        response.setStatus(domestic.getStatus());
        response.setNote(domestic.getNote());
        response.setTimestamp(domestic.getTimestamp());

        if (domestic.getFromLocation() != null) {
            response.setFromLocationName(domestic.getFromLocation().getName());
        }
        if (domestic.getToLocation() != null) {
            response.setToLocationName(domestic.getToLocation().getName());
        }
        if (domestic.getToAddress() != null) {
            response.setToAddressName(domestic.getToAddress().getAddressName());
        }
        response.setShippingList(domestic.getShippingList());
        return response;
    }
}
