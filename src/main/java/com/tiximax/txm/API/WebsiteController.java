package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Websites;
import com.tiximax.txm.Service.WebsiteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/website")
@SecurityRequirement(name = "bearerAuth")

public class WebsiteController {

    @Autowired
    private WebsiteService websiteService;

    @PostMapping
    public ResponseEntity<Websites> createWebsite(@RequestBody Websites website) {
        Websites savedWebsite = websiteService.createWebsite(website);
        return ResponseEntity.ok(savedWebsite);
    }

    @GetMapping("/{websiteId}")
    public ResponseEntity<Websites> getWebsiteById(@PathVariable Long websiteId) {
        Optional<Websites> website = websiteService.getWebsiteById(websiteId);
        return website.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{websiteId}")
    public ResponseEntity<Websites> updateWebsite(@PathVariable Long websiteId, @RequestBody Websites websiteDetails) {
        Websites updatedWebsite = websiteService.updateWebsite(websiteId, websiteDetails);
        return ResponseEntity.ok(updatedWebsite);
    }

    @DeleteMapping("/{websiteId}")
    public ResponseEntity<Void> deleteWebsite(@PathVariable Long websiteId) {
        websiteService.deleteWebsite(websiteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Websites>> searchWebsites(@RequestParam String keyword) {
        List<Websites> websites = websiteService.searchWebsitesByName(keyword);
        return ResponseEntity.ok(websites);
    }

    @GetMapping
    public ResponseEntity<List<Websites>> getAllWebsites() {
        List<Websites> websites = websiteService.getAllWebsites();
        return ResponseEntity.ok(websites);
    }

}
