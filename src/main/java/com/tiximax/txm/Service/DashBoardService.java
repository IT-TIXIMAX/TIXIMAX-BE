package com.tiximax.txm.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Model.DTOResponse.DashBoard.*;
import com.tiximax.txm.Model.DTOResponse.DashBoard.WarehouseSummary;
import com.tiximax.txm.Model.DTOResponse.Purchase.PurchaseProfitResult;
import com.tiximax.txm.Model.Projections.CustomerInventoryProjection;
import com.tiximax.txm.Model.DTOResponse.Warehouse.*;
import com.tiximax.txm.Model.Projections.WarehouseStatisticRow;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashBoardService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderLinksRepository orderLinksRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private PackingRepository packingRepository;

    @Autowired
    private PurchasesRepository purchasesRepository;

    @Autowired
    private RouteExchangeRateRepository routeExchangeRateRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public DashboardResponse getDashboard(LocalDate startDate, LocalDate endDate, DashboardFilterType filterType) {
        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (startDate != null) ? startDate : dateRange.getStartDate();
        LocalDate finalEnd = (endDate != null) ? endDate : dateRange.getEndDate();

        LocalDateTime start = (finalStart != null) ? finalStart.atStartOfDay() : null;
        LocalDateTime end = (finalEnd != null) ? finalEnd.plusDays(1).atStartOfDay() : null;

        long totalOrders = ordersRepository.countByCreatedAtBetween(start, end);
        BigDecimal totalPurchase = paymentRepository.sumPurchaseBetween(start, end).setScale(0, RoundingMode.HALF_UP);;
        BigDecimal totalShip = paymentRepository.sumShipRevenueBetween(start, end).setScale(0, RoundingMode.HALF_UP);;
        BigDecimal totalRevenue = totalPurchase.add(totalShip);
        long newCustomers = customerRepository.countByCreatedAtBetween(start, end);
        long totalLinks = orderLinksRepository.countByOrdersCreatedAtBetween(start, end);
        Double totalWeight = warehouseRepository.sumWeightByCreatedAtBetween(start, end);
        Double totalNetWeight = warehouseRepository.sumNetWeightByCreatedAtBetween(start, end);

        DashboardResponse response = new DashboardResponse();
        response.setTotalRevenue(totalRevenue);
        response.setTotalPurchase(totalPurchase);
        response.setTotalShip(totalShip);
        response.setTotalOrders(totalOrders);
        response.setNewCustomers(newCustomers);
        response.setTotalLinks(totalLinks);
        response.setTotalWeight(new BigDecimal(totalWeight).setScale(1, RoundingMode.HALF_UP).doubleValue());
        response.setTotalNetWeight(new BigDecimal(totalNetWeight).setScale(1, RoundingMode.HALF_UP).doubleValue());
        return response;
    }

    private LocalDateTime start() { return LocalDate.now().atStartOfDay(); }

    private LocalDateTime end()   { return LocalDate.now().plusDays(1).atStartOfDay(); }

    public Map<String, Long> getOrderCounts() {
        Map<String, Long> map = new HashMap<>();
        map.put("newOrders", ordersRepository.countByCreatedAtBetween(start(), end()));
        map.put("newOrderLinks", orderLinksRepository.countByOrders_CreatedAtBetween(start(), end()));
        return map;
    }

    public Map<String, Long> getCustomerCount() {
        Map<String, Long> map = new HashMap<>();
        map.put("newCustomers", customerRepository.countByCreatedAtBetween(start(), end()));
        return map;
    }

    public Map<String, BigDecimal> getPaymentSummary() {
        Map<String, BigDecimal> map = new HashMap<>();

        BigDecimal hang = paymentRepository.sumCollectedAmountByStatusesAndActionAtBetween(
                List.of(PaymentStatus.DA_THANH_TOAN, PaymentStatus.DA_HOAN_TIEN),
                start(), end());

        BigDecimal ship = paymentRepository.sumCollectedAmountByStatusAndActionAtBetween(
                PaymentStatus.DA_THANH_TOAN_SHIP, start(), end());

        map.put("totalCollectedAmount", hang != null ? hang.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        map.put("totalShipAmount", ship != null ? ship.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return map;
    }

    public Map<String, Double> getWeightSummary() {
        Map<String, Double> map = new HashMap<>();
        Double n = warehouseRepository.sumNetWeightByCreatedAtBetween(start(), end());
        Double w = warehouseRepository.sumWeightByCreatedAtBetween(start(), end());
        map.put("totalNetWeight", n != null ? new BigDecimal(n).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0);
        map.put("totalWeight", w != null ? new BigDecimal(w).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0);
        return map;
    }

    public List<Customer> getCustomerDetail(Pageable pageable) {
        return customerRepository.findByCreatedAtBetween(pageable,start(), end());
    }

    public List<MonthlyStatsOrder> getYearlyStatsOrder(int year) {
        Map<Integer, MonthlyStatsOrder> monthStats = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthStats.put(m, new MonthlyStatsOrder(m));
        }

        List<Object[]> ordersData = ordersRepository.countOrdersByMonth(year);
        for (Object[] row : ordersData) {
            int month = (Integer) row[0];
            long count = (Long) row[1];
            monthStats.get(month).setTotalOrders(count);
        }

        List<Object[]> linksData = orderLinksRepository.countLinksByMonth(year);
        for (Object[] row : linksData) {
            int month = (Integer) row[0];
            long count = (Long) row[1];
            monthStats.get(month).setTotalLinks(count);
        }
        return monthStats.values().stream().sorted(Comparator.comparingInt(MonthlyStatsOrder::getMonth)).toList();
    }

    public List<MonthlyStatsPayment> getYearlyStatsPayment(int year) {
        Map<Integer, MonthlyStatsPayment> monthStats = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthStats.put(m, new MonthlyStatsPayment(m));
        }
        List<Object[]> revenueData = paymentRepository.sumRevenueByMonth(year);
        for (Object[] row : revenueData) {
            int month = (Integer) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            monthStats.get(month).setTotalRevenue(sum.setScale(0, RoundingMode.HALF_UP));
        }
        List<Object[]> shipData = paymentRepository.sumShipByMonth(year);
        for (Object[] row : shipData) {
            int month = (Integer) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            monthStats.get(month).setTotalShip(sum.setScale(0, RoundingMode.HALF_UP));
        }
        return monthStats.values().stream().sorted(Comparator.comparingInt(MonthlyStatsPayment::getMonth)).toList();
    }

    public List<MonthlyStatsCustomer> getYearlyStatsCustomer(int year) {
        Map<Integer, MonthlyStatsCustomer> monthStats = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthStats.put(m, new MonthlyStatsCustomer(m));
        }
        List<Object[]> customersData = customerRepository.countNewCustomersByMonth(year);
        for (Object[] row : customersData) {
            int month = (Integer) row[0];
            long count = (Long) row[1];
            monthStats.get(month).setNewCustomers(count);
        }
        return monthStats.values().stream().sorted(Comparator.comparingInt(MonthlyStatsCustomer::getMonth)).toList();
    }

    public List<MonthlyStatsWarehouse> getYearlyStatsWarehouse(int year) {
        Map<Integer, MonthlyStatsWarehouse> monthStats = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthStats.put(m, new MonthlyStatsWarehouse(m));
        }
        List<Object[]> weightData = warehouseRepository.sumWeightByMonth(year);
        List<Object[]> netWeightData = warehouseRepository.sumNetWeightByMonth(year);
        for (Object[] row : weightData) {
            int month = (Integer) row[0];
            Double sum = (Double) row[1];
            monthStats.get(month).setTotalWeight(new BigDecimal(sum).setScale(1, RoundingMode.HALF_UP).doubleValue());
        }
        for (Object[] row : netWeightData) {
            int month = (Integer) row[0];
            Double sum = (Double) row[1];
            monthStats.get(month).setTotalNetWeight(new BigDecimal(sum).setScale(1, RoundingMode.HALF_UP).doubleValue());
        }
        return monthStats.values().stream().sorted(Comparator.comparingInt(MonthlyStatsWarehouse::getMonth)).toList();
    }

    public StartEndDate getDateStartEnd(DashboardFilterType filterType){
        LocalDate startDate = null;
        LocalDate endDate = null;
        LocalDate now = LocalDate.now();
        switch (filterType) {
            case DAY -> {
                startDate = now;
                endDate = now;
            }
            case WEEK -> {
                startDate = now.with(DayOfWeek.MONDAY);
                if (startDate.isAfter(now)) {
                    startDate = startDate.minusWeeks(1);
                }
                endDate = startDate.plusDays(6);
            }
            case MONTH -> {
                startDate = now.withDayOfMonth(1);
                endDate = now.withDayOfMonth(now.lengthOfMonth());
            }
            case QUARTER -> {
                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int startMonth = (currentQuarter - 1) * 3 + 1;
                startDate = LocalDate.of(now.getYear(), startMonth, 1);
                endDate = startDate.plusMonths(3).minusDays(1);
            }
            case HALF_YEAR -> {
                startDate = now.minusMonths(6).withDayOfMonth(1);
                endDate = now;
            }
            case CUSTOM -> {
                if (startDate != null || endDate != null && (startDate.isBefore(endDate))) {
                    startDate = now.minusMonths(6).withDayOfMonth(1);
                    endDate = now;
                }
            }
        }
        return new StartEndDate(startDate, endDate);
    }

    public Map<String, BigDecimal> getDebtSummary(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalReceivable;
        BigDecimal totalPayable;

        if (startDate == null || endDate == null) {
            totalReceivable = ordersRepository.sumLeftoverMoneyPositiveAll();
            totalPayable = ordersRepository.sumLeftoverMoneyNegativeAll();
        } else {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();

            totalReceivable = ordersRepository.sumLeftoverMoneyPositive(start, end);
            totalPayable = ordersRepository.sumLeftoverMoneyNegative(start, end);
        }

        totalReceivable = (totalReceivable == null) ? BigDecimal.ZERO : totalReceivable;
        totalPayable = (totalPayable == null) ? BigDecimal.ZERO : totalPayable;

        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("totalReceivable", totalReceivable.setScale(0, RoundingMode.HALF_UP));
        summary.put("totalPayable", totalPayable.setScale(0, RoundingMode.HALF_UP));
        return summary;
    }

    public Map<String, BigDecimal> calculateFlightRevenueWithMinWeight(
            String flightCode,
            BigDecimal inputCost) {

        if (flightCode == null || flightCode.isBlank()) {
            throw new BadRequestException("Mã chuyến bay không được để trống");
        }
        boolean flightExists = packingRepository.existsByFlightCode(flightCode);
        if (!flightExists) {
            throw new BadRequestException("Mã chuyến bay '" + flightCode + "' không tồn tại trong hệ thống!"
            );
        }

        if (inputCost == null || inputCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Chi phí nhập vào phải phải từ 0 trở lên!");
        }

        BigDecimal effectiveMinWeight = packingRepository.findRouteMinWeightViaWarehouse(flightCode);

        List<Object[]> rawData = warehouseRepository.sumNetWeightAndPriceShipByCustomer(flightCode);

        Map<Long, BigDecimal> customerGrossWeight = new HashMap<>();
        Map<Long, BigDecimal> customerNetWeight = new HashMap<>();
        Map<Long, BigDecimal> customerPriceShip = new HashMap<>();

        for (Object[] row : rawData) {
            Long customerId = (Long) row[0];
            Double grossDouble = (Double) row[1];
            Double netDouble = (Double) row[2];
            BigDecimal priceShip = (BigDecimal) row[3];
            if (priceShip == null) priceShip = BigDecimal.ZERO;

            BigDecimal gross = BigDecimal.valueOf(grossDouble != null ? grossDouble : 0.0)
                    .setScale(3, RoundingMode.HALF_UP);
            BigDecimal net = BigDecimal.valueOf(netDouble != null ? netDouble : 0.0)
                    .setScale(3, RoundingMode.HALF_UP);

            customerGrossWeight.merge(customerId, gross, BigDecimal::add);
            customerNetWeight.merge(customerId, net, BigDecimal::add);
            customerPriceShip.merge(customerId, priceShip, BigDecimal::max);
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalChargeableWeight = BigDecimal.ZERO;
        BigDecimal totalActualGrossWeight = BigDecimal.ZERO;

        for (Long customerId : customerGrossWeight.keySet()) {
            BigDecimal grossWeight = customerGrossWeight.getOrDefault(customerId, BigDecimal.ZERO);
            BigDecimal netWeight = customerNetWeight.getOrDefault(customerId, BigDecimal.ZERO);
            BigDecimal priceShip = customerPriceShip.getOrDefault(customerId, BigDecimal.ZERO);

            BigDecimal chargeableWeight = netWeight.max(effectiveMinWeight)
                    .setScale(1, RoundingMode.HALF_UP);

            BigDecimal customerRevenue = chargeableWeight.multiply(priceShip)
                    .setScale(0, RoundingMode.HALF_UP);

            totalRevenue = totalRevenue.add(customerRevenue);
            totalChargeableWeight = totalChargeableWeight.add(chargeableWeight);
            totalActualGrossWeight = totalActualGrossWeight.add(grossWeight);
        }

        BigDecimal netProfit = totalRevenue.subtract(inputCost).setScale(0, RoundingMode.HALF_UP);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("totalActualGrossWeight", totalActualGrossWeight.setScale(1, RoundingMode.HALF_UP));
        result.put("totalChargeableWeight", totalChargeableWeight.setScale(1, RoundingMode.HALF_UP));
        result.put("totalRevenue", totalRevenue);
        result.put("inputCost", inputCost.setScale(0, RoundingMode.HALF_UP));
        result.put("netProfit", netProfit);

        return result;
    }

    @Transactional(readOnly = true)
    public PurchaseProfitResult calculateEstimatedPurchaseProfit(LocalDate startDate, LocalDate endDate, Long routeId) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BadRequestException("Ngày không hợp lệ!");
        }

        List<RouteExchangeRate> rates = routeExchangeRateRepository.findApplicableRates(routeId, startDate, endDate);

        if (rates.isEmpty()) {
            throw new BadRequestException("Không có mốc tỷ giá nào cover khoảng thời gian yêu cầu cho tuyến này!");
        }

        BigDecimal totalProfit = BigDecimal.ZERO;
        LocalDate cursor = startDate;

        RouteExchangeRate firstRate = rates.get(0);
        if (startDate.isBefore(firstRate.getStartDate())) {
            throw new BadRequestException(
                    "Ngày bắt đầu " + startDate + " nằm ngoài phạm vi mốc tỷ giá! " +
                            "Mốc sớm nhất bắt đầu từ " + firstRate.getStartDate()
            );
        }

        RouteExchangeRate lastRate = rates.get(rates.size() - 1);
        LocalDate lastEnd = (lastRate.getEndDate() == null) ? LocalDate.MAX : lastRate.getEndDate();
        if (endDate.isAfter(lastEnd)) {
            throw new BadRequestException(
                    "Ngày kết thúc " + endDate + " nằm ngoài phạm vi mốc tỷ giá! " +
                            "Mốc muộn nhất kết thúc vào " + lastEnd
            );
        }

        for (RouteExchangeRate rate : rates) {
            if (cursor.isBefore(rate.getStartDate())) {
                throw new BadRequestException(
                        "Phát hiện khoảng thời gian bị gián đoạn từ " + cursor +
                                " đến " + rate.getStartDate().minusDays(1) +
                                " không được cover bởi bất kỳ mốc tỷ giá nào!"
                );
            }
            LocalDate segStart = cursor.isBefore(rate.getStartDate()) ? rate.getStartDate() : cursor;
            LocalDate segEnd = (rate.getEndDate() == null || rate.getEndDate().isAfter(endDate))
                    ? endDate : rate.getEndDate();

            if (segStart.isAfter(segEnd)) continue;

            LocalDateTime s = segStart.atStartOfDay();
            LocalDateTime e = segEnd.plusDays(1).atStartOfDay();

            BigDecimal profit = purchasesRepository.calculateActualPurchaseProfitByRoute(s, e, routeId);
            totalProfit = totalProfit.add(profit != null ? profit : BigDecimal.ZERO);

            cursor = segEnd.plusDays(1);
        }

        if (cursor.isBefore(endDate.plusDays(1))) {
            throw new BadRequestException(
                    "Có khoảng thời gian gap từ " + cursor + " đến " + endDate + " không được cover bởi RouteExchangeRate!"
            );
        }

        return new PurchaseProfitResult("ActualProfit", totalProfit.setScale(0, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public PurchaseProfitResult calculateActualPurchaseProfit(LocalDate startDate, LocalDate endDate, Long routeId) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BadRequestException("Ngày không hợp lệ!");
        }

        List<RouteExchangeRate> rates = routeExchangeRateRepository.findApplicableRates(routeId, startDate, endDate);

        if (rates.isEmpty()) {
            throw new BadRequestException("Không có mốc tỷ giá nào cover khoảng thời gian yêu cầu cho tuyến này!");
        }

        BigDecimal totalProfit = BigDecimal.ZERO;
        LocalDate cursor = startDate;

        RouteExchangeRate firstRate = rates.get(0);
        if (startDate.isBefore(firstRate.getStartDate())) {
            throw new BadRequestException(
                    "Ngày bắt đầu " + startDate + " nằm ngoài phạm vi mốc tỷ giá! " +
                            "Mốc sớm nhất bắt đầu từ " + firstRate.getStartDate()
            );
        }

        RouteExchangeRate lastRate = rates.get(rates.size() - 1);
        LocalDate lastEnd = (lastRate.getEndDate() == null) ? LocalDate.MAX : lastRate.getEndDate();
        if (endDate.isAfter(lastEnd)) {
            throw new BadRequestException(
                    "Ngày kết thúc " + endDate + " nằm ngoài phạm vi mốc tỷ giá! " +
                            "Mốc muộn nhất kết thúc vào " + lastEnd
            );
        }

        for (RouteExchangeRate rate : rates) {
            if (cursor.isBefore(rate.getStartDate())) {
                throw new BadRequestException(
                        "Phát hiện khoảng thời gian bị gián đoạn từ " + cursor +
                                " đến " + rate.getStartDate().minusDays(1) +
                                " không được cover bởi bất kỳ mốc tỷ giá nào!"
                );
            }
            LocalDate segStart = cursor.isBefore(rate.getStartDate()) ? rate.getStartDate() : cursor;
            LocalDate segEnd = (rate.getEndDate() == null || rate.getEndDate().isAfter(endDate))
                    ? endDate : rate.getEndDate();

            if (segStart.isAfter(segEnd)) continue;

            LocalDateTime s = segStart.atStartOfDay();
            LocalDateTime e = segEnd.plusDays(1).atStartOfDay();

//            totalProfit = totalProfit.add(
//                    purchasesRepository.calculateActualPurchaseProfitByRoute(s, e, routeId)
//            );

            BigDecimal profit = purchasesRepository.calculateActualPurchaseProfitByRoute(s, e, routeId);
            totalProfit = totalProfit.add(profit != null ? profit : BigDecimal.ZERO);

            cursor = segEnd.plusDays(1);
        }

        if (cursor.isBefore(endDate.plusDays(1))) {
            throw new BadRequestException(
                    "Có khoảng thời gian gap từ " + cursor + " đến " + endDate + " không được cover bởi RouteExchangeRate!"
            );
        }

        return new PurchaseProfitResult("ActualProfit", totalProfit.setScale(0, RoundingMode.HALF_UP));
    }

    public List<RoutePaymentSummary> getRevenueByRoute(
            LocalDate startDate,
            LocalDate endDate,
            DashboardFilterType filterType,
            PaymentStatus status) {

        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (startDate != null) ? startDate : dateRange.getStartDate();
        LocalDate finalEnd = (endDate != null) ? endDate : dateRange.getEndDate();

        LocalDateTime start = finalStart.atStartOfDay();
        LocalDateTime end = finalEnd.plusDays(1).atStartOfDay();

        if (status == null) {
            throw new BadRequestException("Hãy chọn một loại thanh toán!");
        }

        List<Object[]> rawResults = paymentRepository.sumCollectedAmountByRouteNativeRaw(status.name(), start, end);

        return rawResults.stream()
                .map(row -> new RoutePaymentSummary(
                        (String) row[0],
                        (BigDecimal) row[1]
                ))
                .collect(Collectors.toList());
    }

    public List<RouteOrderSummary> getOrdersAndLinksByRoute(
            LocalDate startDate,
            LocalDate endDate,
            DashboardFilterType filterType) {

        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (startDate != null) ? startDate : dateRange.getStartDate();
        LocalDate finalEnd = (endDate != null) ? endDate : dateRange.getEndDate();

        LocalDateTime start = finalStart.atStartOfDay();
        LocalDateTime end = finalEnd.plusDays(1).atStartOfDay();

        List<Object[]> rawResults = ordersRepository.sumOrdersAndLinksByRouteNativeRaw(start, end);

        return rawResults.stream()
                .map(row -> new RouteOrderSummary(
                        (String) row[0],
                        (Long) row[1],
                        (Long) row[2]
                ))
                .collect(Collectors.toList());
    }

    public List<StaffNewCustomerSummary> getNewCustomersByStaff(
            LocalDate startDate,
            LocalDate endDate,
            DashboardFilterType filterType) {

        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (startDate != null) ? startDate : dateRange.getStartDate();
        LocalDate finalEnd = (endDate != null) ? endDate : dateRange.getEndDate();

        LocalDateTime start = finalStart.atStartOfDay();
        LocalDateTime end = finalEnd.plusDays(1).atStartOfDay();

        List<Object[]> rawResults = customerRepository.sumNewCustomersByStaffNativeRaw(start, end);

        return rawResults.stream()
                .map(row -> new StaffNewCustomerSummary(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());
    }

    public List<RouteWeightSummary> getWeightByRoute(
            LocalDate startDate,
            LocalDate endDate,
            DashboardFilterType filterType) {

        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (startDate != null) ? startDate : dateRange.getStartDate();
        LocalDate finalEnd = (endDate != null) ? endDate : dateRange.getEndDate();

        LocalDateTime start = (finalStart != null) ? finalStart.atStartOfDay() : null;
        LocalDateTime end = (finalEnd != null) ? finalEnd.plusDays(1).atStartOfDay() : null;

        List<Object[]> rawResults = warehouseRepository.sumWeightByRouteNativeRaw(start, end);

        return rawResults.stream()
                .map(row -> new RouteWeightSummary(
                        (String) row[0],
                        new BigDecimal(((Number) row[1]).doubleValue()).setScale(1, RoundingMode.HALF_UP),
                        new BigDecimal(((Number) row[2]).doubleValue()).setScale(1, RoundingMode.HALF_UP)
                ))
                .collect(Collectors.toList());
    }

    public RouteInventorySummary getInventorySummaryByRoute(Long routeId) {
        Object rawResult = warehouseRepository.sumCurrentStockWeightByRoute(routeId);
        Object[] result = (Object[]) rawResult;

        Number weight = (Number) result[0];
        Number netWeight = (Number) result[1];

        double totalWeight = weight != null ? weight.doubleValue() : 0;
        double totalNetWeight = netWeight != null ? netWeight.doubleValue() : 0;
        return new RouteInventorySummary(totalWeight, totalNetWeight);
    }

    public Map<String, RouteStaffPerformance> getStaffPerformanceByRouteGrouped(
            LocalDate start, LocalDate end, DashboardFilterType filterType, Long routeId) {

        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole() != AccountRoles.ADMIN &&
                currentAccount.getRole() != AccountRoles.MANAGER) {
            throw new BadRequestException("Bạn không có quyền xem thống kê hiệu suất theo tuyến!");
        }

        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (start != null) ? start : dateRange.getStartDate();
        LocalDate finalEnd = (end != null) ? end : dateRange.getEndDate();

        LocalDateTime startDateTime = (finalStart != null) ? finalStart.atStartOfDay() : null;
        LocalDateTime endDateTime = (finalEnd != null) ? finalEnd.plusDays(1).atStartOfDay() : null;

        List<Object[]> aggregates = ordersRepository.aggregateStaffKPIByRoute(
                startDateTime, endDateTime, routeId);

        Map<String, Map<String, StaffPerformanceKPI>> tempMap = new HashMap<>();

        for (Object[] row : aggregates) {
            String routeName = (String) row[0];
            if (routeName == null) routeName = "Không xác định";
            String staffCode = (String) row[1];
            String staffName = (String) row[2];
            BigDecimal totalGoods = row[3] == null ? BigDecimal.ZERO
                            : BigDecimal.valueOf(((Number) row[3]).doubleValue());

            Double totalNetWeight = row[4] == null ? 0.0 : ((Number) row[4]).doubleValue();

            Double totalPartial = row[5] == null ? 0.0 : ((Number) row[5]).doubleValue();

            StaffPerformanceKPI kpi = tempMap
                    .computeIfAbsent(routeName, k -> new HashMap<>())
                    .computeIfAbsent(staffCode, k -> {
                        StaffPerformanceKPI newKpi = new StaffPerformanceKPI();
                        newKpi.setStaffCode(staffCode);
                        newKpi.setName(staffName);
                        newKpi.setTotalGoods(BigDecimal.ZERO);
                        newKpi.setTotalNetWeight(0.0);
                        return newKpi;
                    });

            kpi.setTotalGoods(totalGoods != null ? totalGoods : BigDecimal.ZERO);
            kpi.setTotalNetWeight(Math.round((totalNetWeight + totalPartial) * 100.0) / 100.0);
        }

        Map<String, RouteStaffPerformance> result = new TreeMap<>();

        tempMap.forEach((routeName, staffMap) -> {
            RouteStaffPerformance routePerf = new RouteStaffPerformance();
            routePerf.setRouteName(routeName);

            List<StaffPerformanceKPI> staffList = new ArrayList<>(staffMap.values());
            staffList.sort(Comparator.comparing(StaffPerformanceKPI::getTotalGoods).reversed());

            routePerf.setStaffPerformances(staffList);
            result.put(routeName, routePerf);
        });

        return result;
    }

    public Map<String, StaffPerformanceSummary> getPerformanceSummary(LocalDate start, LocalDate end, DashboardFilterType filterType, Long routeId) {
        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (start != null) ? start : dateRange.getStartDate();
        LocalDate finalEnd = (end != null) ? end : dateRange.getEndDate();

        LocalDateTime startDateTime = (finalStart != null) ? finalStart.atStartOfDay() : null;
        LocalDateTime endDateTime = (finalEnd != null) ? finalEnd.plusDays(1).atStartOfDay() : null;

        Staff staff = (Staff) accountUtils.getAccountCurrent();

        List<Object[]> results = ordersRepository.getOrdersSummary(staff.getAccountId() ,startDateTime, endDateTime, routeId);
        Map<String, StaffPerformanceSummary> resultMap = new LinkedHashMap<>();

        for (Object[] row : results) {
            String routeName = (String) row[0];
            Long totalOrders = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            Long completedOrders = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            Long totalParcels = row[3] != null ? ((Number) row[3]).longValue() : 0L;

            double completionRate = totalOrders > 0 ? Math.round((completedOrders * 100.0 / totalOrders) * 100.0) / 100.0 : 0.0;

            resultMap.put(routeName, new StaffPerformanceSummary(
                    staff.getStaffCode(),
                    staff.getName(),
                    staff.getDepartment(),
                    totalOrders,
                    completedOrders,
                    completionRate,
                    totalParcels
            ));
        }
        return resultMap;
    }

    public Map<String, GoodsAndWeight> getGoodsAndWeight(
            LocalDate start,
            LocalDate end,
            DashboardFilterType filterType,
            Long routeId
    ) {
        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (start != null) ? start : dateRange.getStartDate();
        LocalDate finalEnd = (end != null) ? end : dateRange.getEndDate();

        LocalDateTime startDateTime = (finalStart != null) ? finalStart.atStartOfDay() : null;
        LocalDateTime endDateTime = (finalEnd != null) ? finalEnd.plusDays(1).atStartOfDay() : null;

        Staff staff = (Staff) accountUtils.getAccountCurrent();

        List<Object[]> goodsResults =
                ordersRepository.getGoodsValue(staff.getAccountId(), startDateTime, endDateTime, routeId);
        Map<String, BigDecimal> goodsMap = new HashMap<>();
        for (Object[] row : goodsResults) {
            String routeName = (String) row[0];
            BigDecimal totalGoods =
                    row[1] instanceof BigDecimal
                            ? (BigDecimal) row[1]
                            : row[1] instanceof Number
                            ? BigDecimal.valueOf(((Number) row[1]).doubleValue())
                            : BigDecimal.ZERO;

            goodsMap.put(routeName, totalGoods);
        }
        List<Object[]> weightResults =
                ordersRepository.getShippingWeight(staff.getAccountId(), startDateTime, endDateTime, routeId);
        Map<String, Double> weightMap = new HashMap<>();
        for (Object[] row : weightResults) {
            String routeName = (String) row[0];
            Double shippingWeight =
                    row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            Double partialWeight =
                    row.length > 2 && row[2] != null
                            ? ((Number) row[2]).doubleValue()
                            : 0.0;
            Double totalNetWeight =
                    Math.round((shippingWeight + partialWeight) * 10.0) / 10.0;
            weightMap.put(routeName, totalNetWeight);
        }
        Map<String, GoodsAndWeight> resultMap = new LinkedHashMap<>();
        Set<String> allRoutes = new HashSet<>();
        allRoutes.addAll(goodsMap.keySet());
        allRoutes.addAll(weightMap.keySet());
        for (String routeName : allRoutes) {
            BigDecimal goods = goodsMap.getOrDefault(routeName, BigDecimal.ZERO);
            Double weight = weightMap.getOrDefault(routeName, 0.0);
            resultMap.put(routeName, new GoodsAndWeight(goods, weight));
        }
        return resultMap;
    }

    public Map<String, Long> getBadFeedback(LocalDate start, LocalDate end, DashboardFilterType filterType, Long routeId) {
        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (start != null) ? start : dateRange.getStartDate();
        LocalDate finalEnd = (end != null) ? end : dateRange.getEndDate();

        LocalDateTime startDateTime = (finalStart != null) ? finalStart.atStartOfDay() : null;
        LocalDateTime endDateTime = (finalEnd != null) ? finalEnd.plusDays(1).atStartOfDay() : null;

        Staff staff = (Staff) accountUtils.getAccountCurrent();

        List<Object[]> badFeedbackRows = ordersRepository.getBadFeedbacks(
                staff.getAccountId(), startDateTime, endDateTime, routeId);

        long totalBad = 0L;

        for (Object[] row : badFeedbackRows) {
            totalBad += row[1] != null ? ((Number) row[1]).longValue() : 0L;
        }
        Map<String, Long> totalBadFeedback = new LinkedHashMap<>();
        totalBadFeedback.put("badFeedback", totalBad);
    return totalBadFeedback;
    }

    public Map<String, Long> getNewCustomers(LocalDate start, LocalDate end, DashboardFilterType filterType) {
        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = (start != null) ? start : dateRange.getStartDate();
        LocalDate finalEnd = (end != null) ? end : dateRange.getEndDate();

        LocalDateTime startDateTime = (finalStart != null) ? finalStart.atStartOfDay() : null;
        LocalDateTime endDateTime = (finalEnd != null) ? finalEnd.plusDays(1).atStartOfDay() : null;

        Staff staff = (Staff) accountUtils.getAccountCurrent();

        Object[] newCustomer = ordersRepository.getNewCustomers(
                staff.getAccountId(), startDateTime, endDateTime);

        Map<String, Long> totalCustomer = new LinkedHashMap<>();
        totalCustomer.put("totalCustomer", newCustomer != null && newCustomer[0] != null
                ? ((Number) newCustomer[0]).longValue()
                : 0L);
        return totalCustomer;
    }

    public WarehouseSummary getWarehouseDashboard(
            DashboardFilterType filterType,
            LocalDate start,
            LocalDate end,
            Long routeId
    ) {

        LocalDateTime customFrom = (start != null)
                ? start.atStartOfDay()
                : null;

        LocalDateTime customTo = (end != null)
                ? end.atTime(LocalTime.MAX)
                : null;

        Pair<LocalDateTime, LocalDateTime> range =
                resolveTimeRange(filterType, customFrom, customTo);

        LocalDateTime fromDate = range.getLeft();
        LocalDateTime toDate = range.getRight();

        WarehouseSummary summary = new WarehouseSummary();
        summary.setInStock(
                toDTO(
                        warehouseRepository.inStock(routeId)
                )
        );

        summary.setUNPAID_SHIPPING(
                toDTO(
                        warehouseRepository.unpaidShipping(routeId)
                )
        );

        summary.setPAID_SHIPPING(
                toDTO(
                        warehouseRepository.paidShipping(routeId)
                )
        );
        summary.setExportByVnPost(
                toDTO(
                        warehouseRepository.exportByCarrierWithDate(
                                Carrier.VNPOST.name(),
                                fromDate,
                                toDate,
                                routeId
                        )
                )
        );

        summary.setExportByOther(
                toDTO(
                        warehouseRepository.exportByCarrierWithDate(
                                Carrier.OTHER.name(),
                                fromDate,
                                toDate,
                                routeId
                        )
                )
        );

        return summary;
    }

    private WarehouseStatistic toDTO(WarehouseStatisticRow p) {
        return new WarehouseStatistic(
                p.getTotalCodes(),
                p.getTotalWeight(),
                p.getTotalCustomers()
        );
    }

    private Pair<LocalDateTime, LocalDateTime> resolveTimeRange(
            DashboardFilterType type,
            LocalDateTime customFrom,
            LocalDateTime customTo
    ) {
        LocalDate today = LocalDate.now();
        LocalDateTime from;
        LocalDateTime to = LocalDateTime.now();

        switch (type) {
            case DAY -> from = today.atStartOfDay();

            case WEEK -> from = today
                    .with(DayOfWeek.MONDAY)
                    .atStartOfDay();

            case MONTH -> from = today
                    .withDayOfMonth(1)
                    .atStartOfDay();

            case QUARTER -> {
                int quarter = (today.getMonthValue() - 1) / 3;
                int firstMonth = quarter * 3 + 1;
                from = LocalDate
                        .of(today.getYear(), firstMonth, 1)
                        .atStartOfDay();
            }

            case HALF_YEAR -> {
                int firstMonth = today.getMonthValue() <= 6 ? 1 : 7;
                from = LocalDate
                        .of(today.getYear(), firstMonth, 1)
                        .atStartOfDay();
            }

            case CUSTOM -> {
                if (customFrom == null || customTo == null) {
                    throw new BadRequestException(
                            "CUSTOM filter requires fromDate and toDate"
                    );
                }
                return Pair.of(customFrom, customTo);
            }

            default -> throw new BadRequestException(
                    "Unexpected TimeFilterType: " + type
            );
        }

        return Pair.of(from, to);
    }

    private RouteInventorySummary extractSummary(List<Object[]> results) {
        double totalWeight = 0.0;
        double totalNetWeight = 0.0;

        if (results != null && !results.isEmpty()) {
            Object[] row = results.get(0);  // aggregate chỉ trả về 1 hàng

            Object weightObj = row.length > 0 ? row[0] : null;
            Object netWeightObj = row.length > 1 ? row[1] : null;

            totalWeight = (weightObj instanceof Number n) ? n.doubleValue() : 0.0;
            totalNetWeight = (netWeightObj instanceof Number n) ? n.doubleValue() : 0.0;
        }

        return new RouteInventorySummary(totalWeight, totalNetWeight);
    }

    public RouteInventorySummary getUnpackedInventorySummaryByRoute(Long routeId) {
        List<Object[]> results = warehouseRepository.sumUnpackedStockWeightByRoute(routeId);
        return extractSummary(results);
    }

    public Page<CustomerTop> getTopCustomers(CustomerTopType customerTopType, String customerCode, Pageable pageable) {
        return switch (customerTopType) {
            case TOTAL_ORDERS -> customerRepository.findTopByTotalOrders(customerCode, pageable);
            case TOTAL_WEIGHT -> customerRepository.findTopByTotalWeight(customerCode, pageable);
            case TOTAL_AMOUNT -> customerRepository.findTopByTotalAmount(customerCode, pageable);
            case BALANCE      -> customerRepository.findTopByBalance(customerCode, pageable);
        };
    }

    public List<CustomerInventoryQuantity> getDashboardInventory(Long routeId) {

    List<CustomerInventoryProjection> rows =
            warehouseRepository.dashboardInventory(routeId);

    return rows.stream().map(r -> {

        InventoryQuantity iq = new InventoryQuantity();
        iq.setExportedCode(
                r.getExportedCode() == null ? 0D : r.getExportedCode().doubleValue()
        );
        iq.setExportedWeightKg(
                r.getExportedWeight() == null ? 0D : r.getExportedWeight()
        );
        iq.setRemainingCode(
                r.getRemainingCode() == null ? 0D : r.getRemainingCode().doubleValue()
        );
        iq.setRemainingWeightKg(
                r.getRemainingWeight() == null ? 0D : r.getRemainingWeight()
        );

        CustomerInventoryQuantity dto = new CustomerInventoryQuantity();
        dto.setCustomerCode(r.getCustomerCode());
        dto.setCustomerName(r.getCustomerName());
        dto.setStaffCode(r.getStaffCode());
        dto.setStaffName(r.getStaffName());
        dto.setInventoryQuantity(iq);

        return dto;
    }).toList();
}


    public InventoryDaily getDailyInventory(LocalDate start, LocalDate end, DashboardFilterType filterType, Long routeId) {
        Staff staff = (Staff) accountUtils.getAccountCurrent();
        Long forcedLocationId = null;

        if (staff.getRole().equals(AccountRoles.STAFF_WAREHOUSE_FOREIGN)) {
            forcedLocationId = staff.getWarehouseLocation().getLocationId();
            if (forcedLocationId == null) {
                throw new BadRequestException("Nhân viên kho chưa được gán location.");
            }
            routeId = null;
        } else if (!staff.getRole().equals(AccountRoles.MANAGER)){
            throw new BadRequestException("Vai trò không được phép xem dashboard này.");
        }

        StartEndDate dateRange = getDateStartEnd(filterType);
        LocalDate finalStart = start != null ? start : dateRange.getStartDate();
        LocalDate finalEnd = end != null ? end : dateRange.getEndDate();

        LocalDateTime startDt = finalStart != null ? finalStart.atStartOfDay() : null;
        LocalDateTime endDt = finalEnd != null ? finalEnd.plusDays(1).atStartOfDay() : null;

        PendingSummary pending;
        StockSummary stock;
        PackedSummary packed;
        PackedSummary awaitFlight;
        List<LocationSummary> pendingByLoc = null;
        List<LocationSummary> stockByLoc = null;
        List<LocationSummary> packedByLoc = null;
        List<LocationSummary> awaitFlightByLoc = null;

        if (staff.getRole().equals(AccountRoles.STAFF_WAREHOUSE_FOREIGN)) {
            pending = getPendingByLocation(forcedLocationId);
            stock   = getStockByLocation(forcedLocationId);
            packed  = getPackedByLocation(startDt, endDt, forcedLocationId);
            awaitFlight = getAwaitingFlightPackedByLocation(startDt, endDt, forcedLocationId);

            LocationSummary loc = buildLocationSummaryForStaff(forcedLocationId, stock, packed);

            pendingByLoc = List.of(loc);
            stockByLoc   = List.of(loc);
            packedByLoc  = List.of(loc);
            awaitFlightByLoc = List.of(loc);
        } else {
            if (routeId != null) {
                pending = ordersRepository.getPendingSummary(routeId);
                stock   = warehouseRepository.getStockSummary(routeId);
                packed  = packingRepository.getPackedSummary(startDt, endDt, routeId);
                awaitFlight = packingRepository.getAwaitFlightSummary(startDt, endDt, routeId);
            } else {
                pendingByLoc = ordersRepository.getPendingSummaryByLocation();
                stockByLoc   = warehouseRepository.getStockSummaryByLocation();
                packedByLoc  = packingRepository.getPackedSummaryByLocation(startDt, endDt);
                awaitFlightByLoc = packingRepository.getAwaitingFlightPackedSummaryByLocation(startDt, endDt);

                pending = ordersRepository.getPendingSummary(null);
                stock   = warehouseRepository.getStockSummary(null);
                packed  = packingRepository.getPackedSummary(startDt, endDt, null);
                awaitFlight = packingRepository.getAwaitFlightSummary(startDt, endDt, null);
            }
        }

        return new InventoryDaily(pending, stock, packed, awaitFlight, pendingByLoc, stockByLoc, packedByLoc, awaitFlightByLoc);
    }

    private PendingSummary getPendingByLocation(Long locId) {
        return ordersRepository.getPendingSummaryByLocationId(locId);
    }

    private StockSummary getStockByLocation(Long locId) {
        return warehouseRepository.getStockSummaryByLocationId(locId);
    }

    private PackedSummary getPackedByLocation(LocalDateTime s, LocalDateTime e, Long locId) {
        return packingRepository.getPackedSummaryByLocationId(s, e, locId);
    }

    private PackedSummary getAwaitingFlightPackedByLocation(LocalDateTime s, LocalDateTime e, Long locId) {
        return packingRepository.getAwaitingFlightSummaryByLocation(s, e, locId);
    }

    private LocationSummary buildLocationSummaryForStaff(Long locId, StockSummary stock, PackedSummary packed) {
        LocationSummary ls = new LocationSummary();
        ls.setLocationId(locId);
        ls.setLocationName("Kho của bạn");
        ls.setWarehouses(stock.getWarehouses());
        ls.setWeight(stock.getWeight());
        ls.setNetWeight(stock.getNetWeight());
        return ls;
    }


    public PerformanceWHResponse getLocationOverview(Long locationId, String month, Integer lastDays) {
        LocalDateTime[] range = getDateRange(month, lastDays);
        List<Object[]> raw = warehouseRepository.findDailyStatsByLocation(locationId, range[0], range[1]);

        Map<LocalDate, PerformanceWHDaily> map = raw.stream().collect(Collectors.toMap(
                row -> ((java.sql.Date) row[0]).toLocalDate(),
                row -> PerformanceWHDaily.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .inboundCount((Long) row[1])
                        .inboundKg((Double) row[2])
                        .inboundNetKg((Double) row[3])
                        .packedCount((Long) row[4])
                        .packedKg((Double) row[5])
                        .build(),
                (old, neu) -> old
        ));

        List<PerformanceWHDaily> dailyList = new ArrayList<>();
        LocalDate current = range[0].toLocalDate();
        LocalDate endDate = range[1].toLocalDate().minusDays(1);

        while (!current.isAfter(endDate)) {
            dailyList.add(map.getOrDefault(current, PerformanceWHDaily.builder()
                    .date(current)
                    .inboundCount(0L)
                    .inboundKg(0.0)
                    .inboundNetKg(0.0)
                    .packedCount(0L)
                    .packedKg(0.0)
                    .build()));
            current = current.plusDays(1);
        }

        long totalInboundCount = dailyList.stream()
                .mapToLong(PerformanceWHDaily::getInboundCount)
                .sum();

        double totalInboundKg = dailyList.stream()
                .mapToDouble(PerformanceWHDaily::getInboundKg)
                .sum();

        double totalInboundNetKg = dailyList.stream()
                .mapToDouble(PerformanceWHDaily::getInboundNetKg)
                .sum();

        long totalPackedCount = dailyList.stream()
                .mapToLong(PerformanceWHDaily::getPackedCount)
                .sum();

        double totalPackedKg = dailyList.stream()
                .mapToDouble(PerformanceWHDaily::getPackedKg)
                .sum();

        PerformanceWHSummary totals = PerformanceWHSummary.builder()
                .totalInboundCount(totalInboundCount)
                .totalInboundKg(totalInboundKg)
                .totalInboundNetKg(totalInboundNetKg)
                .totalPackedCount(totalPackedCount)
                .totalPackedKg(totalPackedKg)
                .build();

        return PerformanceWHResponse.builder()
                .dailyData(dailyList)
                .totals(totals)
                .build();
    }

    public List<StaffWHPerformanceSummary> getStaffPerformancesInLocation(Long locationId, String month, Integer lastDays) {
        LocalDateTime[] range = getDateRange(month, lastDays);
        List<Object[]> raw = warehouseRepository.findStaffSummaryByLocation(locationId, range[0], range[1]);

        List<StaffWHPerformanceSummary> list = new ArrayList<>();
        for (Object[] row : raw) {
            list.add(StaffWHPerformanceSummary.builder()
                    .staffId((Long) row[0])
                    .staffCode((String) row[1])
                    .name((String) row[2])
                    .department((String) row[3])
                    .totals(PerformanceWHSummary.builder()
                            .totalInboundCount((Long) row[4])
                            .totalInboundKg((Double) row[5])
                            .totalInboundNetKg((Double) row[6])
                            .totalPackedCount((Long) row[7])
                            .totalPackedKg((Double) row[8])
                            .build())
                    .build());
        }
        return list;
    }

    private LocalDateTime[] getDateRange(String month, Integer lastDays) {
        LocalDateTime start, end;
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            YearMonth ym = YearMonth.parse(month);
            start = ym.atDay(1).atStartOfDay();
            end = ym.plusMonths(1).atDay(1).atStartOfDay();
        } else {
            end = LocalDate.now().plusDays(1).atStartOfDay();
            start = end.minusDays(lastDays != null ? lastDays : 30);
        }
        return new LocalDateTime[]{start, end};
    }

}
