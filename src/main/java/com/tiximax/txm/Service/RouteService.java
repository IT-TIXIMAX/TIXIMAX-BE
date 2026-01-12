package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Model.DTORequest.Route.RouteRequest;
import com.tiximax.txm.Model.DTOResponse.Route.ExchangeRateList;
import com.tiximax.txm.Repository.RouteRepository;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RouteService {

    private final String VCB_API = "https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx";

    @Autowired
    private RouteRepository routeRepository;

    public Route createRoute(RouteRequest routeRequest) {
        if (routeRequest.getName() == null || routeRequest.getName().isEmpty()) {
            throw new IllegalArgumentException("Tên tuyến đường không được để trống!");
        }
        if (routeRepository.existsByName(routeRequest.getName())) {
            throw new IllegalArgumentException("Tuyến đường với tên " + routeRequest.getName() + " đã tồn tại!");
        }

        Route route = new Route();
        route.setName(routeRequest.getName());
        route.setShipTime(routeRequest.getShipTime());
        route.setUnitBuyingPrice(routeRequest.getUnitBuyingPrice());
        route.setUnitDepositPrice(routeRequest.getUnitDepositPrice());
        route.setExchangeRate(routeRequest.getExchangeRate());
        route.setDifferenceRate(routeRequest.getDifferenceRate());
        route.setUpdateAuto(routeRequest.isUpdateAuto());
        route.setNote(routeRequest.getNote());

        return routeRepository.save(route);
    }

    public Route getRouteById(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến đường này!"));
    }

    public List<Route> getAllRoutes() {
        List<Route> routes = routeRepository.findAll();
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy tuyến đường nào!");
        }
        return routes;
    }

    public Route updateRoute(Long routeId, RouteRequest routeRequest) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến đường này để cập nhật!"));

        if (routeRequest.getName() != null && !routeRequest.getName().isEmpty()) {
            if (!routeRequest.getName().equals(route.getName()) && routeRepository.existsByName(routeRequest.getName())) {
                throw new IllegalArgumentException("Tuyến đường với tên " + routeRequest.getName() + " đã tồn tại!");
            }
            route.setName(routeRequest.getName());
        }
        if (!routeRequest.getShipTime().isEmpty()) {
            route.setShipTime(routeRequest.getShipTime());
        }
        if (routeRequest.getUnitDepositPrice() != null) {
            route.setUnitDepositPrice(routeRequest.getUnitDepositPrice());
        }
        if (routeRequest.getUnitBuyingPrice() != null) {
            route.setUnitBuyingPrice(routeRequest.getUnitBuyingPrice());
        }
        if (routeRequest.getExchangeRate() != null) {
            route.setExchangeRate(routeRequest.getExchangeRate());
        }
        if (!routeRequest.getNote().isEmpty()) {
            route.setNote(routeRequest.getNote());
        }

        return routeRepository.save(route);
    }

    public void deleteRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến đường với ID: " + routeId));
        if (!route.getOrders().isEmpty()) {
            throw new IllegalStateException("Không thể xóa tuyến đường vì đã có đơn hàng liên kết!");
        }
        routeRepository.delete(route);
    }

    public ExchangeRateList getExchangeRate() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String xmlResponse = restTemplate.getForObject(VCB_API, String.class);

        JAXBContext jaxbContext = JAXBContext.newInstance(ExchangeRateList.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        return (ExchangeRateList) unmarshaller.unmarshal(new StringReader(xmlResponse));
    }

    @Scheduled(cron = "0 30 15 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void updateExchangeRate() throws Exception {
        ExchangeRateList exchangeRateList = getExchangeRate();
        List<ExchangeRateList.Exrate> exrates = exchangeRateList.getExrates();

        List<Route> routes = routeRepository.findAll();

        for (Route route : routes) {
            String routeName = route.getName();
            if (routeName != null && route.isUpdateAuto()) {
                for (ExchangeRateList.Exrate exrate : exrates) {
                    if (routeName.equals(exrate.getCurrencyCode())) {
                        String sellValue = exrate.getSell().replaceAll(",", "");
                        BigDecimal baseRate = new BigDecimal(sellValue);
                        BigDecimal newRate = baseRate.add(route.getDifferenceRate()).setScale(2, RoundingMode.HALF_UP);
                        route.setExchangeRate(newRate);
                        routeRepository.save(route);
                        break;
                    }
                }
            }
        }
    }
}