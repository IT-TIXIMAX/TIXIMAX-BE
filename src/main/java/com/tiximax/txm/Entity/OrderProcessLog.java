package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.ProcessLogAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter

public class OrderProcessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Enumerated(EnumType.STRING)
    private AccountRoles roleAtTime;

    @Enumerated(EnumType.STRING)
    private ProcessLogAction action;

    @Column(nullable = false, updatable = false)
    private String actionCode;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = true)
    @JsonIgnore
    Staff staff;

    @ManyToOne
    @JoinColumn(name="order_id", nullable = true)
    @JsonIgnore
    Orders orders;
}
