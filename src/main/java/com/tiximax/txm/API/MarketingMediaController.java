package com.tiximax.txm.API;

import com.tiximax.txm.Entity.MarketingMedia;
import com.tiximax.txm.Enums.MediaPosition;
import com.tiximax.txm.Model.MarketingMediaRequest;
import com.tiximax.txm.Service.MarketingMediaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/marketing-media")
@SecurityRequirement(name = "bearerAuth")

public class MarketingMediaController {
    @Autowired
    private MarketingMediaService marketingMediaService;

    @PostMapping
    public ResponseEntity<MarketingMedia> create(@RequestBody MarketingMediaRequest request) {
        return ResponseEntity.ok(marketingMediaService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MarketingMedia> update(@PathVariable Long id,
                                                 @RequestBody MarketingMediaRequest request) {
        return ResponseEntity.ok(marketingMediaService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketingMedia> getById(@PathVariable Long id) {
        return ResponseEntity.ok(marketingMediaService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<MarketingMedia>> getList(
            @RequestParam(required = false) MediaPosition position,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("sorting").descending());
        Page<MarketingMedia> result = marketingMediaService.getByPosition(position, pageable);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        marketingMediaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
