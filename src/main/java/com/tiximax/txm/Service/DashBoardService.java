package com.tiximax.txm.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    public DashboardResponse getDashboard(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        long totalOrders = ordersRepository.countByCreatedAtBetween(start, end);
        BigDecimal totalRevenue = paymentRepository.sumCollectedAmountBetween(start, end).setScale(0, RoundingMode.HALF_UP);;
        BigDecimal totalPurchase = paymentRepository.sumPurchaseBetween(start, end).setScale(0, RoundingMode.HALF_UP);;
        BigDecimal totalShip = paymentRepository.sumShipRevenueBetween(start, end).setScale(0, RoundingMode.HALF_UP);;
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
        BigDecimal hang = paymentRepository.sumCollectedAmountByStatusAndActionAtBetween(
                PaymentStatus.DA_THANH_TOAN, start(), end());
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
            throw new RuntimeException("Mã chuyến bay không được để trống");
        }
        boolean flightExists = packingRepository.existsByFlightCode(flightCode);
        if (!flightExists) {
            throw new RuntimeException("Mã chuyến bay '" + flightCode + "' không tồn tại trong hệ thống!"
            );
        }

        if (inputCost == null || inputCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Chi phí nhập vào phải phải từ 0 trở lên!");
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

            System.out.println(
                    "CustomerId=" + customerId +
                            " | Gross=" + grossWeight +
                            " | Net=" + netWeight +
                            " | MinWeight=" + effectiveMinWeight +
                            " | PriceShip=" + priceShip
            );

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
            throw new RuntimeException("Ngày không hợp lệ!");
        }

        List<RouteExchangeRate> rates = routeExchangeRateRepository.findApplicableRates(routeId, startDate, endDate);

        if (rates.isEmpty()) {
            throw new RuntimeException("Không có mốc tỷ giá nào cover khoảng thời gian yêu cầu cho tuyến này!");
        }

        BigDecimal totalProfit = BigDecimal.ZERO;
        LocalDate cursor = startDate;

        RouteExchangeRate firstRate = rates.get(0);
        if (startDate.isBefore(firstRate.getStartDate())) {
            throw new RuntimeException(
                    "Ngày bắt đầu " + startDate + " nằm ngoài phạm vi mốc tỷ giá! " +
                            "Mốc sớm nhất bắt đầu từ " + firstRate.getStartDate()
            );
        }

        RouteExchangeRate lastRate = rates.get(rates.size() - 1);
        LocalDate lastEnd = (lastRate.getEndDate() == null) ? LocalDate.MAX : lastRate.getEndDate();
        if (endDate.isAfter(lastEnd)) {
            throw new RuntimeException(
                    "Ngày kết thúc " + endDate + " nằm ngoài phạm vi mốc tỷ giá! " +
                            "Mốc muộn nhất kết thúc vào " + lastEnd
            );
        }

        for (RouteExchangeRate rate : rates) {
            if (cursor.isBefore(rate.getStartDate())) {
                throw new RuntimeException(
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
            throw new RuntimeException(
                    "Có khoảng thời gian gap từ " + cursor + " đến " + endDate + " không được cover bởi RouteExchangeRate!"
            );
        }

        return new PurchaseProfitResult("ActualProfit", totalProfit.setScale(0, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public PurchaseProfitResult calculateActualPurchaseProfit(LocalDate startDate, LocalDate endDate, Long routeId) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new RuntimeException("Ngày không hợp lệ!");
        }

        List<RouteExchangeRate> rates = routeExchangeRateRepository.findApplicableRates(routeId, startDate, endDate);

        if (rates.isEmpty()) {
            throw new RuntimeException("Không có mốc tỷ giá nào cover khoảng thời gian yêu cầu cho tuyến này!");
        }

        BigDecimal totalProfit = BigDecimal.ZERO;
        LocalDate cursor = startDate;

        RouteExchangeRate firstRate = rates.get(0);
        if (startDate.isBefore(firstRate.getStartDate())) {
            throw new RuntimeException(
                    "Ngày bắt đầu " + startDate + " nằm ngoài phạm vi mốc tỷ giá! " +
                            "Mốc sớm nhất bắt đầu từ " + firstRate.getStartDate()
            );
        }

        RouteExchangeRate lastRate = rates.get(rates.size() - 1);
        LocalDate lastEnd = (lastRate.getEndDate() == null) ? LocalDate.MAX : lastRate.getEndDate();
        if (endDate.isAfter(lastEnd)) {
            throw new RuntimeException(
                    "Ngày kết thúc " + endDate + " nằm ngoài phạm vi mốc tỷ giá! " +
                            "Mốc muộn nhất kết thúc vào " + lastEnd
            );
        }

        for (RouteExchangeRate rate : rates) {
            if (cursor.isBefore(rate.getStartDate())) {
                throw new RuntimeException(
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
            throw new RuntimeException(
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
            throw new RuntimeException("Hãy chọn một loại thanh toán!");
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
                        (String) row[0],
                        ((Number) row[1]).longValue()
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

    public Map<String, RouteStaffPerformance> getStaffPerformanceByRouteGrouped(
            LocalDate start, LocalDate end, DashboardFilterType filterType, Long routeId) {

        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole() != AccountRoles.ADMIN &&
                currentAccount.getRole() != AccountRoles.MANAGER) {
            throw new SecurityException("Bạn không có quyền xem thống kê hiệu suất theo tuyến!");
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
            kpi.setTotalNetWeight(Math.round(totalNetWeight * 100.0) / 100.0);
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
}
