package com.tiximax.txm.Repository;

import com.tiximax.txm.Entity.MarketingMedia;
import com.tiximax.txm.Enums.MediaPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface MarketingMediaRepository extends JpaRepository<MarketingMedia, Long> {
    Page<MarketingMedia> findByPosition(MediaPosition position, Pageable pageable);

    Page<MarketingMedia> findAll(Pageable pageable);
}
