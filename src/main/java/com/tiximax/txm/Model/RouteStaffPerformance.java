package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter

public class RouteStaffPerformance {
    private String routeName;
    private List<StaffPerformanceKPI> staffPerformances = new ArrayList<>();
}
