package com.tiximax.txm.Model;

import com.tiximax.txm.Enums.MediaPosition;
import com.tiximax.txm.Enums.MediaStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data

public class MarketingMediaRequest {
    private String title;
    private String mediaUrl;
    private String linkUrl;
    private MediaStatus status;
    private Integer sorting;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String description;
    private MediaPosition position;
}
