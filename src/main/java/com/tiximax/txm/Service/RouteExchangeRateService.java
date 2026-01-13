package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Entity.RouteExchangeRate;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.DTOResponse.Route.EffectiveRateResponse;
import com.tiximax.txm.Repository.RouteExchangeRateRepository;
import com.tiximax.txm.Repository.RouteRepository;

import org.aspectj.weaver.ast.Not;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service

public class RouteExchangeRateService {
    @Autowired
    private RouteExchangeRateRepository routeExchangeRateRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Transactional
    public RouteExchangeRate addExchange(RouteExchangeRate request) {
        Long routeId = request.getId();
        if (routeId == null) {
            throw new BadRequestException("Mã tuyến bắt buộc phải được truyền vào!");
        }

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new NotFoundException("Tuyến này không tồn tại!"));
        if (request.getStartDate() == null) {
            throw new BadRequestException("Ngày bắt đầu là bắt buộc!");
        }
        if (request.getExchangeRate() == null) {
            throw new BadRequestException("Tỷ giá gốc bắt buộc phải được nhập!");
        }

        if (request.getStartDate().isAfter(request.getEndDate())){
           throw new BadRequestException("Ngày bắt đầu phải nhỏ hơn ngày kết thúc!");
        }

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        boolean overlap = routeExchangeRateRepository
                .existsByRoute_RouteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(routeId, start, end);

        if (overlap) {
            throw new BadRequestException("Khoảng thời gian này đã chồng lấn với bản ghi tỷ giá khác của cùng tuyến!");
        }

        RouteExchangeRate entity = new RouteExchangeRate();

        entity.setRoute(route);
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setExchangeRate(request.getExchangeRate());
        entity.setNote(request.getNote());
        return routeExchangeRateRepository.save(entity);
    }

    public List<RouteExchangeRate> findByRouteId(Long routeId) {
        return routeExchangeRateRepository.findAll().stream()
                .filter(r -> r.getRoute().getRouteId().equals(routeId))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public EffectiveRateResponse getEffectiveRate(Long routeId, LocalDate date) {
        if (date == null) date = LocalDate.now();

        LocalDate finalDate = date;
        RouteExchangeRate rate = routeExchangeRateRepository.findEffectiveRate(routeId, date)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tỷ giá hiệu lực cho tuyến này tại ngày " + finalDate));

        EffectiveRateResponse resp = new EffectiveRateResponse();
        resp.setRouteName(rate.getRoute().getName());
        resp.setExchangeRate(rate.getExchangeRate());
        resp.setStartDate(rate.getStartDate());
        resp.setEndDate(rate.getEndDate());
        resp.setNote(rate.getNote());
        return resp;
    }

    public void delete(Long id) {
        if (!routeExchangeRateRepository.existsById(id)) {
            throw new NotFoundException("Bản ghi không tồn tại");
        }
        routeExchangeRateRepository.deleteById(id);
    }

    private RouteExchangeRate toDTO(RouteExchangeRate entity) {
        RouteExchangeRate dto = new RouteExchangeRate();
        dto.setId(entity.getId());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setExchangeRate(entity.getExchangeRate());
        dto.setNote(entity.getNote());
        return dto;
    }
}
