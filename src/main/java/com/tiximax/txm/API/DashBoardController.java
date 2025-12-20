package com.tiximax.txm.API;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tiximax.txm.Enums.DashboardFilterType;
import com.tiximax.txm.Service.DashBoardService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@CrossOrigin
@RequestMapping("/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashBoardController {

    @Autowired
    private DashBoardService dashBoardService;

    @GetMapping
    public DashboardResponse getDashboard(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType
    ) {
//        LocalDate now = LocalDate.now();
//
//        switch (filterType) {
//            case DAY -> {
//                startDate = now;
//                endDate = now;
//            }
//            case MONTH -> {
//                startDate = now.withDayOfMonth(1);
//                endDate = now.withDayOfMonth(now.lengthOfMonth());
//            }
//            case QUARTER -> {
//                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
//                int startMonth = (currentQuarter - 1) * 3 + 1;
//                startDate = LocalDate.of(now.getYear(), startMonth, 1);
//                endDate = startDate.plusMonths(3).minusDays(1);
//            }
//            case HALF_YEAR -> {
//                startDate = now.minusMonths(6).withDayOfMonth(1);
//                endDate = now;
//            }
//            case CUSTOM -> {
//                // nếu không nhập startDate / endDate thì mặc định 6 tháng gần nhất
//                if (startDate == null || endDate == null) {
//                    startDate = now.minusMonths(6).withDayOfMonth(1);
//                    endDate = now;
//                }
//            }
//        }
        StartEndDate startEndDate = dashBoardService.getDateStartEnd(filterType);
        return dashBoardService.getDashboard(startEndDate.getStartDate(), startEndDate.getEndDate());
    }

    @GetMapping("admin/orders")
    public Map<String, Long> getAdminOrders(){
        return dashBoardService.getOrderCounts();
    }

    @GetMapping("admin/customers")
    public Map<String, Long> getAdminCustomers()  {
        return dashBoardService.getCustomerCount();
    }

    @GetMapping("admin/payments")
    public Map<String, BigDecimal> getAdminPayments()   {
        return dashBoardService.getPaymentSummary();
    }

    @GetMapping("admin/weights")
    public Map<String, Double> getAdminWeights()    {
        return dashBoardService.getWeightSummary();
    }

    @GetMapping("customer-detail/{page}/{size}")
    public List<Customer> getCustomerDetail(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return dashBoardService.getCustomerDetail(pageable);
    }

    @GetMapping("/yearly-order/{year}")
    public ResponseEntity<List<MonthlyStatsOrder>> getYearlyStatsOrder(@PathVariable int year) {
        return ResponseEntity.ok(dashBoardService.getYearlyStatsOrder(year));
    }

    @GetMapping("/yearly-payment/{year}")
    public ResponseEntity<List<MonthlyStatsPayment>> getYearlyStatsPayment(@PathVariable int year) {
        return ResponseEntity.ok(dashBoardService.getYearlyStatsPayment(year));
    }

    @GetMapping("/yearly-customer/{year}")
    public ResponseEntity<List<MonthlyStatsCustomer>> getYearlyStatsCustomer(@PathVariable int year) {
        return ResponseEntity.ok(dashBoardService.getYearlyStatsCustomer(year));
    }

    @GetMapping("/yearly-warehouse/{year}")
    public ResponseEntity<List<MonthlyStatsWarehouse>> getYearlyStatsWarehouse(@PathVariable int year) {
        return ResponseEntity.ok(dashBoardService.getYearlyStatsWarehouse(year));
    }

    @GetMapping("/routes/revenue-summary")
    public ResponseEntity<List<RoutePaymentSummary>> getRevenueByRoute(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType,
            @RequestParam(required = false) PaymentStatus status) {

        List<RoutePaymentSummary> result = dashBoardService.getRevenueByRoute(
                startDate, endDate, filterType, status);

        return ResponseEntity.ok(result);
    }

    @GetMapping("admin/debts-total")
    public Map<String, BigDecimal> getAdminDebtsTotal() {
        return dashBoardService.getDebtSummary(null, null);
    }

    @GetMapping("/admin/flight-revenue")
    public ResponseEntity<Map<String, BigDecimal>> getFlightRevenue(
            @RequestParam String flightCode,
            @RequestParam BigDecimal inputCost,
            @RequestParam(required = false, defaultValue = "0.0") Double minWeight) {

        Map<String, BigDecimal> result = dashBoardService
                .calculateFlightRevenueWithMinWeight(flightCode, inputCost, minWeight);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/purchase-profit")
    public ResponseEntity<BigDecimal> getPurchaseProfit(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam BigDecimal exchangeRate,
            @RequestParam(required = false) Long routeId) {

        BigDecimal profit = dashBoardService.calculatePurchaseProfit(startDate, endDate, exchangeRate, routeId);

        return ResponseEntity.ok(profit);
    }
}
