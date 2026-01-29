package com.tiximax.txm.Helper;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tiximax.txm.Service.DraftDomesticService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DraftDomesticScheduler {

    private final DraftDomesticService syncService;

    // ⏰ 00:00 mỗi ngày
    @Scheduled(cron = "0 0 0 * * ?")
    public void runNightlySync() {
        log.info("🌙 Nightly DraftDomestic sync started");
        syncService.syncAndLockDraftDomestic();
        log.info("✅ Nightly DraftDomestic sync finished");
    }
}