package com.tiximax.txm.Model.DTOResponse.Warehouse;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Getter
@Setter

public class DateRange {
    LocalDateTime start;
    LocalDateTime end;
    LocalDate startDate;
    LocalDate endDate;
    String period;
    String filterType;

    public DateRange(LocalDateTime start, LocalDateTime end, LocalDate startDate, LocalDate endDate,
              String period, String filterType) {
        this.start = start;
        this.end = end;
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period;
        this.filterType = filterType;
    }
}
