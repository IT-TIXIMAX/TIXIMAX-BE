package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.MarketingMedia;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Enums.MediaPosition;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.MarketingMediaRequest;
import com.tiximax.txm.Repository.MarketingMediaRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException.NotFound;

@Service

public class MarketingMediaService {

    @Autowired
    private MarketingMediaRepository marketingMediaRepository;

    @Autowired
    private AccountUtils accountUtils;

    public MarketingMedia create(MarketingMediaRequest request) {
        Account account = accountUtils.getAccountCurrent();
        if (!account.getRole().equals(AccountRoles.MARKETING)){
            throw new AccessDeniedException("Chỉ có nhân viên marketing mới được thực hiện được tính năng này!");
        }
        MarketingMedia entity = new MarketingMedia();
        mapRequestToEntity(request, entity);
        entity.setStaff((Staff) account);
        return marketingMediaRepository.save(entity);
    }

    public MarketingMedia update(Long id, MarketingMediaRequest request) {
        Account account = accountUtils.getAccountCurrent();
        if (!account.getRole().equals(AccountRoles.MARKETING)){
            throw new AccessDeniedException("Chỉ có nhân viên marketing mới được thực hiện được tính năng này!");
        }
        MarketingMedia entity = marketingMediaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Media not found"));

        if (request.getTitle() != null){entity.setTitle(request.getTitle());}
        if (request.getMediaUrl() != null){entity.setMediaUrl(request.getMediaUrl());}
        if (request.getLinkUrl() != null){entity.setLinkUrl(request.getLinkUrl());}
        if (request.getStatus() != null){entity.setStatus(request.getStatus());}
        if (request.getSorting() != null){entity.setSorting(request.getSorting());}
        if (request.getStartDate() != null){entity.setStartDate(request.getStartDate());}
        if (request.getEndDate() != null){entity.setEndDate(request.getEndDate());}
        if (request.getDescription() != null){entity.setDescription(request.getDescription());}
        if (request.getPosition() != null){entity.setPosition(request.getPosition());}

        return marketingMediaRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public MarketingMedia getById(Long id) {
        return marketingMediaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Media not found"));
    }

    @Transactional(readOnly = true)
    public Page<MarketingMedia> getByPosition(MediaPosition position, Pageable pageable) {
        if (position == null) {
            return marketingMediaRepository.findAll(pageable);
        }
        return marketingMediaRepository.findByPosition(position, pageable);
    }

    public void delete(Long id) {
        Account account = accountUtils.getAccountCurrent();
        if (!account.getRole().equals(AccountRoles.MARKETING)){
            throw new AccessDeniedException("Chỉ có nhân viên marketing mới được thực hiện được tính năng này!");
        }
        if (!marketingMediaRepository.existsById(id)) {
            throw new NotFoundException("Media not found");
        }
        marketingMediaRepository.deleteById(id);
    }

    private void mapRequestToEntity(MarketingMediaRequest request, MarketingMedia entity) {
        entity.setTitle(request.getTitle());
        entity.setMediaUrl(request.getMediaUrl());
        entity.setLinkUrl(request.getLinkUrl());
        entity.setStatus(request.getStatus());
        entity.setSorting(request.getSorting() != null ? request.getSorting() : 0);
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setDescription(request.getDescription());
        entity.setPosition(request.getPosition());
    }
}
