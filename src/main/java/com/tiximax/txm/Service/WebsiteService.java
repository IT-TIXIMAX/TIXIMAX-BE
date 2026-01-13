package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Websites;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Repository.WebsiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class WebsiteService {

    @Autowired
    private WebsiteRepository websiteRepository;

    public Websites createWebsite(Websites website) {
        return websiteRepository.save(website);
    }

    public Optional<Websites> getWebsiteById(Long websiteId) {
        return websiteRepository.findById(websiteId);
    }
    public Websites updateWebsite(Long websiteId, Websites websiteDetails) {
        Websites website = websiteRepository.findById(websiteId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy website này!"));
        website.setWebsiteName(websiteDetails.getWebsiteName());
        return websiteRepository.save(website);
    }
    public void deleteWebsite(Long websiteId) {
        Websites website = websiteRepository.findById(websiteId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy website này!"));
        websiteRepository.delete(website);
    }
    public List<Websites> searchWebsitesByName(String keyword) {
        return websiteRepository.findByWebsiteNameContaining(keyword);
    }
    public List<Websites> getAllWebsites() {
        return websiteRepository.findAll();
    }

}
