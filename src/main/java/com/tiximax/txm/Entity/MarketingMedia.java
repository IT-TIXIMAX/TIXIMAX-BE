package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.MediaPosition;
import com.tiximax.txm.Enums.MediaStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter

public class MarketingMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long mediaId;

    private String title;

    private String mediaUrl;

    private String linkUrl;

    @Enumerated(EnumType.STRING)
    private MediaStatus status;

    private int sorting;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LocalDateTime createdDate;

    private String description;

    @Enumerated(EnumType.STRING)
    private MediaPosition position;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;
}
