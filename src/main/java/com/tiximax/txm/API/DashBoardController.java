package com.tiximax.txm.API;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Enums.CustomerTopType;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Model.DTOResponse.Customer.InactiveCustomerProjection;
import com.tiximax.txm.Model.DTOResponse.DashBoard.*;
import com.tiximax.txm.Model.DTOResponse.Order.OrderInfo;
import com.tiximax.txm.Model.DTOResponse.Order.TopByWeightAndOrderType;
import com.tiximax.txm.Model.DTOResponse.Payment.DailyPaymentRevenue;
import com.tiximax.txm.Model.DTOResponse.Purchase.PurchaseProfitResult;
import com.tiximax.txm.Model.DTOResponse.Warehouse.PerformanceWHResponse;
import com.tiximax.txm.Model.DTOResponse.Warehouse.StaffWHPerformanceSummary;
import com.tiximax.txm.Repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tiximax.txm.Enums.DashboardFilterType;
import com.tiximax.txm.Enums.OrderType;
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
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType) {
        StartEndDate startEndDate = dashBoardService.getDateStartEnd(filterType);
        return dashBoardService.getDashboard(startEndDate.getStartDate(), startEndDate.getEndDate(), filterType);
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

    @GetMapping("admin/debts-total")
    public Map<String, BigDecimal> getAdminDebtsTotal() {
        return dashBoardService.getDebtSummary(null, null);
    }

    @GetMapping("/admin/flight-revenue")
    public ResponseEntity<Map<String, BigDecimal>> getFlightRevenue(
            @RequestParam String flightCode,
            @RequestParam BigDecimal inputCost) {

        Map<String, BigDecimal> result = dashBoardService
                .calculateFlightRevenueWithMinWeight(flightCode, inputCost);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/estimated-profit")
    public ResponseEntity<PurchaseProfitResult> getEstimatedProfit(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Long routeId) {

        PurchaseProfitResult profit = dashBoardService.calculateEstimatedPurchaseProfit(startDate, endDate, routeId);

        return ResponseEntity.ok(profit);
    }

    @GetMapping("/admin/actual-profit")
    public ResponseEntity<PurchaseProfitResult> getActualProfit(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Long routeId) {

        PurchaseProfitResult profit = dashBoardService.calculateActualPurchaseProfit(startDate, endDate, routeId);
        return ResponseEntity.ok(profit);
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

    @GetMapping("/routes/orders-summary")
    public ResponseEntity<List<RouteOrderSummary>> getOrdersByRoute(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType) {

        List<RouteOrderSummary> result = dashBoardService.getOrdersAndLinksByRoute(startDate, endDate, filterType);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/staff/customers-summary")
    public ResponseEntity<List<StaffNewCustomerSummary>> getNewCustomersByStaff(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType) {

        List<StaffNewCustomerSummary> result = dashBoardService.getNewCustomersByStaff(startDate, endDate, filterType);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/routes/weight-summary")
    public ResponseEntity<List<RouteWeightSummary>> getWeightByRoute(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType) {

        List<RouteWeightSummary> result = dashBoardService.getWeightByRoute(startDate, endDate, filterType);

        return ResponseEntity.ok(result);
    }

   @GetMapping("/warehouse/domestic-summary")
    public ResponseEntity<WarehouseSummary> getWarehouseDashboard(
            @RequestParam DashboardFilterType filter,

            @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate start,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end,

            @RequestParam(required = false)
            Long routeId
    ) {
        WarehouseSummary summary =
                dashBoardService.getWarehouseDashboard(
                        filter,
                        start,
                        end,
                        routeId
                );

        return ResponseEntity.ok(summary);
    }


    @GetMapping("/summary-staff")
    public ResponseEntity<Map<String, StaffPerformanceSummary>> getPerformanceSummary(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType,
            @RequestParam(required = false) Long routeId) {

        Map<String, StaffPerformanceSummary> data =
                dashBoardService.getPerformanceSummary(startDate, endDate, filterType, routeId);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/goods-and-weight")
    public ResponseEntity<Map<String, GoodsAndWeight>> getGoodsAndWeight(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType,
            @RequestParam(required = false) Long routeId) {

        Map<String, GoodsAndWeight> data =
                dashBoardService.getGoodsAndWeight(startDate, endDate, filterType, routeId);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/feedbacks")
    public Map<String, Long> getBadFeedbacks(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType,
            @RequestParam(required = false) Long routeId) {

        return dashBoardService.getBadFeedback(startDate, endDate, filterType, routeId);
    }

    @GetMapping("/new-customers")
    public Map<String, Long> getNewCustomers(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType
    ) {

        return dashBoardService.getNewCustomers(startDate, endDate, filterType);
    }

    @GetMapping("/routes/kpi")
    public ResponseEntity<Map<String, RouteStaffPerformance>> getWeightByRouteKpi(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType,
            @RequestParam(required = false) Long routeId) {

        Map<String, RouteStaffPerformance> result = dashBoardService.getStaffPerformanceByRouteGrouped(startDate, endDate, filterType, routeId);

        return ResponseEntity.ok(result);
    }

     @GetMapping("/purchase-summary")
    public ResponseEntity<PurchaseDashboard> getPurchaseDashboard() {

        PurchaseDashboard result = dashBoardService.getPurchaseDashboard();

        return ResponseEntity.ok(result);
    }
    @GetMapping("/purchase-detail/{page}/{size}")
    public ResponseEntity<Page<PurchaseDetailDashboard>> getPurchaseDetail(@PathVariable int page, @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PurchaseDetailDashboard> result = dashBoardService.getPurchaseDetailDashboard(pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customers/top/{page}/{size}")
    public ResponseEntity<Page<CustomerTop>> getTopCustomers(
            @RequestParam(required = false, defaultValue = "TOTAL_ORDERS")CustomerTopType customerTopType,
            @RequestParam(required = false) String customerCode,
            @PathVariable int page,
            @PathVariable int size) {

        Sort sort;
        String sortField;

        switch (customerTopType) {
            case TOTAL_ORDERS:
                sortField = "totalOrders";
                break;
            case TOTAL_WEIGHT:
                sortField = "totalWeight";
                break;
            case TOTAL_AMOUNT:
                sortField = "totalAmount";
                break;
            case BALANCE:
                sortField = "balance";
                break;
            default:
                throw new BadRequestException("Không tìm thấy trường dữ liệu này!");
        }

        sort = Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CustomerTop> result = dashBoardService.getTopCustomers(customerTopType, customerCode, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/daily-inventory")
    public ResponseEntity<InventoryDaily> getDailyInventory(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "CUSTOM") DashboardFilterType filterType,
            @RequestParam(required = false) Long routeId) {

        return ResponseEntity.ok(dashBoardService.getDailyInventory(startDate, endDate, filterType, routeId));
    }
    @GetMapping("/customer/inventory/{page}/{size}")
    public ResponseEntity<Page<CustomerInventoryQuantity>> getInventoryDashboard(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Integer month,
            @PathVariable int page,
            @PathVariable int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                dashBoardService.getDashboardInventory(routeId,month,pageable)
        );
    }
    @GetMapping("/exported")
public ResponseEntity<List<ExportedQuantity>> getExportedDashboard(
    @RequestParam(required = false) Long routeId, 
    @RequestParam(required = false) Integer month
) {
    return ResponseEntity.ok(
        dashBoardService.getExportedDashboard(routeId, month)
    );
    }

    @GetMapping("/location/{locationId}/overview")
    public ResponseEntity<PerformanceWHResponse> getLocationOverview(
            @PathVariable Long locationId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false, defaultValue = "30") Integer lastDays) {

        return ResponseEntity.ok(dashBoardService.getLocationOverview(locationId, month, lastDays));
    }

    // Nhân viên trong kho
    @GetMapping("/location/{locationId}/staff")
    public ResponseEntity<List<StaffWHPerformanceSummary>> getStaffInLocation(
            @PathVariable Long locationId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false, defaultValue = "30") Integer lastDays) {

        return ResponseEntity.ok(dashBoardService.getStaffPerformancesInLocation(locationId, month, lastDays));
    }

      @GetMapping("/top-by-type")
    public ResponseEntity<Map<String, List<TopByWeightAndOrderType>>> getTopByWeightAndOrderType(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false, defaultValue = "MUA_HO") OrderType orderType,
            @RequestParam(defaultValue = "30") int limit) {

        Map<String, List<TopByWeightAndOrderType>> result =
                dashBoardService.getTopByWeightAndOrderType(month, orderType, limit);

        return ResponseEntity.ok(result);
    }
      @GetMapping("/inactive-customers/{page}/{size}")
    public ResponseEntity<Page<InactiveCustomerProjection>> getInactiveCustomers(
            @PathVariable int page,
            @PathVariable int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InactiveCustomerProjection> result = dashBoardService.getInactiveCustomersByStaff(pageable);

        return ResponseEntity.ok(result);
    }
        @GetMapping("/daily-payment-revenue")
    public ResponseEntity<List<DailyPaymentRevenue>> getDailyPaymentRevenue(
            @RequestParam(required = false) Integer month) {
        List<DailyPaymentRevenue> result = dashBoardService.getDailyPaymentRevenue(month);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/daily-payment-shipping")
    public ResponseEntity<List<DailyPaymentRevenue>> getDailyPaymentRShipping(
            @RequestParam(required = false) Integer month) {
        List<DailyPaymentRevenue> result = dashBoardService.getDailyPaymentShipping(month);
        return ResponseEntity.ok(result);
    }


//    @GetMapping("/inactive-customers/{page}/{size}")
//    public ResponseEntity<Page<InactiveCustomerProjection>> getInactiveCustomers(
//            @PathVariable int page,
//            @PathVariable int size
//    ) {
//        Pageable pageable = PageRequest.of(page, size);
//        Page<InactiveCustomerProjection> result = dashBoardService.getInactiveCustomersByStaff(pageable);
//
//        return ResponseEntity.ok(result);
//    }
}
