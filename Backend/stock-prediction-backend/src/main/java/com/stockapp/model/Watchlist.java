package com.stockapp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Entity
@Table(name = "watchlists", indexes = {
        @Index(name = "idx_user_ticker", columnList = "user_id, ticker")
})
@Data
@NoArgsConstructor
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;

    @Column(name = "added_at", nullable = false)
    private Timestamp addedAt = new Timestamp(System.currentTimeMillis());
}