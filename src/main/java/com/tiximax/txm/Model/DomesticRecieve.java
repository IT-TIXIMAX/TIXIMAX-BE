package com.tiximax.txm.Model;

import java.util.List;
import com.tiximax.txm.Entity.Packing;
import lombok.Data;
@Data
public class DomesticRecieve {
    public List<PackingRecieve> packingRecieveList;

    public DomesticRecieve(List<Packing> packing) {
       this.packingRecieveList = packing.stream().map(p -> {
           PackingRecieve pr = new PackingRecieve();
           pr.packingCode = p.getPackingCode();
           pr.trackingCode = p.getPackingList();
           return pr;
       }).toList();
    }
}


