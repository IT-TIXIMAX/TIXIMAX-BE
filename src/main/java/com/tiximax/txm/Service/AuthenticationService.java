package com.tiximax.txm.Service;

import com.tiximax.txm.Config.SecurityConfig;
import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.*;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Model.*;
import com.tiximax.txm.Model.DTORequest.Auth.ChangePasswordRequest;
import com.tiximax.txm.Model.DTORequest.Auth.LoginRequest;
import com.tiximax.txm.Model.DTORequest.Auth.RegisterCustomerRequest;
import com.tiximax.txm.Model.DTORequest.Auth.RegisterStaffRequest;
import com.tiximax.txm.Model.DTORequest.Auth.StaffPatchRequest;
import com.tiximax.txm.Model.DTORequest.Customer.CustomerPatchRequest;
import com.tiximax.txm.Model.DTOResponse.Auth.StaffReponse;
import com.tiximax.txm.Model.DTOResponse.Customer.CustomerReponse;
import com.tiximax.txm.Model.DTOResponse.DashBoard.SaleStats;
import com.tiximax.txm.Model.DTOResponse.DashBoard.StaffPerformance;
import com.tiximax.txm.Repository.*;
import com.tiximax.txm.Utils.AccountUtils;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.security.core.Authentication;


import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

@Service

public class AuthenticationService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AccountRouteRepository accountRouteRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OtpService otpService;
    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseAnonKey;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        Account account = authenticationRepository.findByUsername(username);
        if (account == null) {
            log.warn("Username not found: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return account;
    }

    public Object login(LoginRequest loginRequest) {
    try {
        if (loginRequest == null || loginRequest.getPassword().isEmpty() || loginRequest.getUsername().isEmpty()) {
            throw new BadCredentialsException("Vui lòng điền đầy đủ thông tin đăng nhập!");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        Account account = (Account) authentication.getPrincipal();

        if (account == null) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng!");
        }

        if (!account.getStatus().equals(AccountStatus.HOAT_DONG)) {
            throw new AuthenticationServiceException("Tài khoản bạn đã bị khóa!");
        }
        if (!account.isVerify()) {
          throw new AuthenticationServiceException("Tài khoản của bạn chưa được xác minh, vui lòng kiểm tra email để xác minh tài khoản!");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenService.generateToken(account);

        if (account instanceof Staff) {
            StaffReponse response = new StaffReponse();
            response.setAccountId(account.getAccountId());
            response.setUsername(account.getUsername());
            response.setEmail(account.getEmail());
            response.setPhone(account.getPhone());
            response.setName(account.getName());
            response.setRole(account.getRole());
            response.setStatus(account.getStatus());
            response.setCreatedAt(account.getCreatedAt());
            response.setStaffCode(((Staff) account).getStaffCode());
            response.setDepartment(((Staff) account).getDepartment());
            response.setLocation(((Staff) account).getLocation());
            response.setToken(token);
            return response;
        } else if (account instanceof Customer) {
            CustomerReponse response = new CustomerReponse();
            response.setAccountId(account.getAccountId());
            response.setUsername(account.getUsername());
            response.setEmail(account.getEmail());
            response.setPhone(account.getPhone());
            response.setName(account.getName());
            response.setRole(account.getRole());
            response.setStatus(account.getStatus());
            response.setCustomerCode(((Customer) account).getCustomerCode());
            response.setAddresses(((Customer) account).getAddresses());
            response.setSource(((Customer) account).getSource());
            response.setToken(token);
            return response;
        }

    } catch (AuthenticationException e) {     
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng!");      
    }

    return null;
}

    public Staff registerStaff(RegisterStaffRequest registerRequest) {
            if (authenticationRepository.findByUsername(registerRequest.getUsername()) != null){
                throw new BadCredentialsException("Tên đăng nhập bị trùng, vui lòng chọn một tên khác!");
            }
            if (authenticationRepository.findByPhone(registerRequest.getPhone()) != null){
                throw new BadCredentialsException("Số điện thoại bị trùng, vui lòng chọn một số khác!");
            }
            Staff staff = new Staff();
            staff.setUsername(registerRequest.getUsername());
            staff.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            staff.setEmail(registerRequest.getEmail());
            staff.setPhone(registerRequest.getPhone());
            staff.setName(registerRequest.getName());
            staff.setRole(registerRequest.getRole());
            staff.setStatus(AccountStatus.HOAT_DONG);
            staff.setCreatedAt(LocalDateTime.now());
            staff.setStaffCode(generateStaffCode());
            staff.setDepartment(registerRequest.getDepartment());
            staff.setLocation(registerRequest.getLocation());
            staff.setVerify(true);
            staff.setCanApproveExpenses(false);
            staff.setCanRequestExpenses(false);
            List<Long> routeIds = registerRequest.getRouteIds();
            if (routeIds != null && !routeIds.isEmpty()) {
                for (Long routeId : routeIds) {
                    Route route = routeRepository.findById(routeId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến hàng này!"));
                    AccountRoute accountRoute = new AccountRoute();
                    accountRoute.setAccount(staff);
                    accountRoute.setRoute(route);
                    staff = authenticationRepository.save(staff);
                    accountRouteRepository.save(accountRoute);
                }
            }
            return staff;
        }

    public Customer registerCustomer(RegisterCustomerRequest registerRequest) throws Exception {
        if (authenticationRepository.findByUsername(registerRequest.getUsername()) != null){
            throw new BadCredentialsException("Tên đăng nhập bị trùng, vui lòng chọn một tên khác!");
        }

        if (authenticationRepository.findByPhone(registerRequest.getPhone()) != null){
            throw new BadCredentialsException("Số điện thoại bị trùng, vui lòng chọn một số khác!");
        }

        if (authenticationRepository.findByEmail(registerRequest.getEmail()) != null){
            throw new BadCredentialsException("Email bị trùng, vui lòng chọn một email khác!");
        }

        Customer customer = new Customer();
        customer.setUsername(registerRequest.getUsername());
        customer.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        customer.setEmail(registerRequest.getEmail());
        customer.setPhone(registerRequest.getPhone());
        customer.setName(registerRequest.getName());
        customer.setRole(AccountRoles.CUSTOMER);
        customer.setStatus(AccountStatus.HOAT_DONG);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setCustomerCode(generateCustomerCode());
        customer.setBalance(BigDecimal.ZERO);

        Address address = new Address();
        address.setAddressName(registerRequest.getAddress().trim());
        address.setCustomer(customer);
        customer.setAddresses(new HashSet<>());
        customer.getAddresses().add(address);
        customer.setSource(registerRequest.getSource());
        customer = authenticationRepository.save(customer);
        voucherService.assignOnRegisterVouchers(customer);
        otpService.sendOtpToEmail(customer.getEmail());
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "INSERT",
                        "customerCode", customer.getCustomerCode(),
                        "email", customer.getEmail(),
                        "message", "Khách hàng mới được đăng ký!"
                )
        );
        return customer;
    }

    public String generateCustomerCode() {
    String lastCode = customerRepository.findLatestCustomerCode();

    if (lastCode == null) {
        return "C00001";
    }
    int number = Integer.parseInt(lastCode.substring(1));
    number++;
    if (number < 100000) {
        return String.format("C%05d", number);
    } else {
        return "C" + number;  
    }
}

    public String generateStaffCode() {
        String lastCode = staffRepository.findLatestStaffCode();

        if (lastCode == null) {
            return "S01";
        }
        int number = Integer.parseInt(lastCode.substring(1));
        number++;
        if (number < 100) {
            return String.format("S%05d", number);
        } else {
            return "S" + number;
        }
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

    public List<Customer> searchCustomersByPhoneOrName(String keyword) {
        return customerRepository.findByPhoneOrNameContainingAndStaffId(keyword, accountUtils.getAccountCurrent().getAccountId());
    }

    public Customer registerCustomerByStaff(RegisterCustomerRequest registerRequest) {
        if (authenticationRepository.findByPhone(registerRequest.getPhone()) != null && !registerRequest.getPhone().isEmpty()){
            throw new BadCredentialsException("Số điện thoại bị trùng, vui lòng chọn một số khác!");
        }

        if (authenticationRepository.findByEmail(registerRequest.getEmail()) != null && !registerRequest.getEmail().isEmpty()){
            throw new BadCredentialsException("Email bị trùng, vui lòng chọn một email khác!");
        }

        if (registerRequest.getAddress() == null || registerRequest.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ không được để trống!");
        }

        Customer customer = new Customer();
        customer.setUsername(registerRequest.getPhone());
        customer.setPassword(passwordEncoder.encode("123456"));
        customer.setEmail(registerRequest.getEmail());
        customer.setPhone(registerRequest.getPhone());
        customer.setName(registerRequest.getName());
        customer.setRole(AccountRoles.CUSTOMER);
        customer.setStatus(AccountStatus.HOAT_DONG);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setCustomerCode(generateCustomerCode());
        customer.setSource(registerRequest.getSource());
        customer.setBalance(BigDecimal.ZERO);
        customer.setStaffId(accountUtils.getAccountCurrent().getAccountId());

        Address address = new Address();
        address.setAddressName(registerRequest.getAddress().trim());
        address.setCustomer(customer);
        customer.setAddresses(new HashSet<>());
        customer.getAddresses().add(address);
        customer = authenticationRepository.save(customer);
        voucherService.assignOnRegisterVouchers(customer);
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "INSERT",
                        "customerCode", customer.getCustomerCode(),
                        "email", customer.getEmail(),
                        "message", "Khách hàng mới được đăng ký!"
                )
        );
        return customer;
    }

    public Page<Staff> getAllStaff(Pageable pageable) {
        return staffRepository.findAll(pageable);
    }

    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Page<Customer> getCustomersByStaff(String keyword, Pageable pageable) {
    Long staffId = null;
    Account account = accountUtils.getAccountCurrent();
    if (!account.getRole().equals(AccountRoles.MANAGER)){
        staffId = account.getAccountId();
    }
    if (keyword == null || keyword.trim().isEmpty()) {
        keyword = "";  
    }
    return customerRepository.searchByStaff(staffId, keyword, pageable);
}

    public Page<Staff> getSaleAndLeadSaleStaff(Pageable pageable) {
        List<AccountRoles> roles = Arrays.asList(AccountRoles.STAFF_SALE, AccountRoles.LEAD_SALE);
        Page<Account> accounts = authenticationRepository.findByRoleIn(roles, pageable);
        List<Staff> staffList = accounts.getContent().stream()
                .filter(account -> account instanceof Staff)
                .map(account -> (Staff) account)
                .collect(Collectors.toList());
        return new PageImpl<>(staffList, pageable, accounts.getTotalElements());
    }

    public Page<Staff> getSalesInSameRoute(Pageable pageable) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole() != AccountRoles.LEAD_SALE) {
            throw new AccessDeniedException("Vị trí của bạn không được phép cho chức năng này!");
        }

        List<AccountRoute> leadSaleRoutes = accountRouteRepository.findByAccount_AccountId(currentAccount.getAccountId());
        Set<Long> routeIds = leadSaleRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        List<AccountRoute> allAccountRoutes = accountRouteRepository.findByRoute_RouteIdIn(routeIds);

        List<Staff> sales = allAccountRoutes.stream()
                .map(AccountRoute::getAccount)
                .filter(account -> account instanceof Staff && account.getRole() == AccountRoles.STAFF_SALE)
                .map(account -> (Staff) account)
                .distinct()
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sales.size());
        List<Staff> pagedSales = sales.subList(start, end);
        return new PageImpl<>(pagedSales, pageable, sales.size());
    }

    public List<SaleStats> getSalesStatsInSameRoute(String timeFrame, Integer year, Integer month, Integer week) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (currentAccount.getRole() != AccountRoles.LEAD_SALE) {
            throw new SecurityException("Chỉ LEAD_SALE được phép truy cập dữ liệu này!");
        }

        // Lấy danh sách route của LEAD_SALE
        List<AccountRoute> leadSaleRoutes = accountRouteRepository.findByAccount_AccountId(currentAccount.getAccountId());
        Set<Long> routeIds = leadSaleRoutes.stream()
                .map(AccountRoute::getRoute)
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        // Lấy danh sách nhân viên SALE và LEAD_SALE trong cùng route
        List<AccountRoute> allAccountRoutes = accountRouteRepository.findByRoute_RouteIdIn(routeIds);
        List<Staff> sales = allAccountRoutes.stream()
                .map(AccountRoute::getAccount)
                .filter(account -> account instanceof Staff &&
                        (account.getRole() == AccountRoles.STAFF_SALE || account.getRole() == AccountRoles.LEAD_SALE))
                .map(account -> (Staff) account)
                .distinct()
                .collect(Collectors.toList());

        // Xác định khoảng thời gian
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (timeFrame != null && year != null && month != null) {
            if (timeFrame.equalsIgnoreCase("MONTH")) {
                if (month < 1 || month > 12) {
                    throw new BadRequestException("Tháng phải từ 1 đến 12");
                }
                startDate = LocalDateTime.of(year, month, 1, 0, 0);
                endDate = startDate.plusMonths(1);
            } else if (timeFrame.equalsIgnoreCase("WEEK")) {
                if (month < 1 || month > 12) {
                    throw new BadRequestException("Tháng phải từ 1 đến 12");
                }
                if (week == null || week < 1 || week > 5) {
                    throw new BadRequestException("Tuần phải từ 1 đến 5");
                }
                LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
                startDate = firstDayOfMonth.plusWeeks(week - 1).atStartOfDay();
                endDate = startDate.plusWeeks(1);
            } else {
                throw new BadRequestException("timeFrame không hợp lệ. Sử dụng MONTH hoặc WEEK");
            }
        } else if (timeFrame != null || year != null || month != null || week != null) {
            throw new BadRequestException("Thiếu tham số bắt buộc: MONTH cần timeFrame, year, month; WEEK cần thêm week");
        }

        // Tính thống kê cho từng nhân viên
        List<SaleStats> statsList = new ArrayList<>();
        for (Staff sale : sales) {
            // Lấy danh sách đơn hàng
            List<Orders> saleOrders;
            if (startDate != null && endDate != null) {
                saleOrders = ordersRepository.findByStaff_AccountIdAndRoute_RouteIdInAndCreatedAtBetween(
                        sale.getAccountId(), routeIds, startDate, endDate);
            } else {
                saleOrders = ordersRepository.findByStaff_AccountIdAndRoute_RouteIdIn(sale.getAccountId(), routeIds);
            }

            // Tổng số đơn hàng
            long totalOrders = saleOrders.size();

            // Tổng doanh thu
            BigDecimal totalRevenue = saleOrders.stream()
                    .map(Orders::getFinalPriceOrder)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Số khách hàng duy nhất
            long uniqueCustomers = saleOrders.stream()
                    .map(Orders::getCustomer)
                    .map(Customer::getAccountId)
                    .distinct()
                    .count();

            // Số khách hàng mới
            long newCustomerCount;
            if (startDate != null && endDate != null) {
                newCustomerCount = customerRepository.countByStaffIdAndCreatedAtBetween(sale.getAccountId(), startDate, endDate);
            } else {
                newCustomerCount = customerRepository.countByStaffId(sale.getAccountId());
            }

            // Giá trị trung bình đơn hàng
            BigDecimal averageOrderValue = totalOrders > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;

            // Tạo SaleStats
            SaleStats stats = new SaleStats();
            stats.setSaleId(sale.getAccountId());
            stats.setSaleName(sale.getName());
            stats.setTotalOrders(totalOrders);
            stats.setTotalRevenue(totalRevenue);
            stats.setUniqueCustomers(uniqueCustomers);
            stats.setNewCustomers(newCustomerCount);
            stats.setAverageOrderValue(averageOrderValue);

            statsList.add(stats);
        }

        // Sắp xếp theo tổng số đơn hàng giảm dần
        statsList.sort(Comparator.comparingLong(SaleStats::getTotalOrders).reversed());

        return statsList;
    }

    public Account findOrCreateGoogleAccount(String email, String name) {
        Account account = authenticationRepository.findByUsername(email);
        if (account != null) {
            System.out.println("Account found: " + email);
            return account;
        } else {
            Customer customer = new Customer();
            customer.setUsername(email);
            customer.setName(name);
            customer.setPassword("");
            customer.setRole(AccountRoles.CUSTOMER);
            customer.setStatus(AccountStatus.HOAT_DONG);
            customer.setCreatedAt(LocalDateTime.now());
            customer.setCustomerCode(generateCustomerCode());

            Address address = new Address();
            address.setAddressName("Default Address");
            address.setCustomer(customer);
            customer.setAddresses(new HashSet<>());
            customer.getAddresses().add(address);

            authenticationRepository.save(customer);
            return customer;
        }
    }

    public Account verifyAndSaveUser(String email, String name) {
        Account existingAccount = authenticationRepository.findByEmail(email);
        if (existingAccount != null) {
            return existingAccount;
        }
            Customer customer = new Customer();
            customer.setUsername(email);
            customer.setPassword(email);
            customer.setEmail(email);
            customer.setPhone("00000000000");
            customer.setName(name);
            customer.setRole(AccountRoles.CUSTOMER);
            customer.setStatus(AccountStatus.HOAT_DONG);
            customer.setCreatedAt(LocalDateTime.now());
            customer.setCustomerCode(generateCustomerCode());

            Address address = new Address();
            address.setAddressName(null);
            address.setCustomer(customer);
            customer.setAddresses(new HashSet<>());
            customer.getAddresses().add(address);

            customer.setSource("Google");
            customer.setVerify(true);
            customer = authenticationRepository.save(customer);
            return customer;
    }

    public Map<String, StaffPerformance> getMyCurrentMonthPerformanceMap() {
        Account currentAccount = accountUtils.getAccountCurrent();

        if (!(currentAccount instanceof Staff staff) ||
                (staff.getRole() != AccountRoles.STAFF_SALE && staff.getRole() != AccountRoles.LEAD_SALE)) {
            throw new AccessDeniedException("Bạn không có quyền xem hiệu suất cá nhân!");
        }

        LocalDate now = LocalDate.now();
        LocalDateTime startDate = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = LocalDateTime.now();

        StaffPerformance perf = new StaffPerformance();
        perf.setStaffCode(staff.getStaffCode());
        perf.setName(staff.getName());
        perf.setDepartment(staff.getDepartment());

        List<Orders> orders = ordersRepository.findByStaff_AccountIdAndCreatedAtBetween(
                staff.getAccountId(), startDate, endDate);

        long totalOrders = orders.size();
        perf.setTotalOrders(totalOrders);

        BigDecimal totalGoods = orders.stream()
                .map(Orders::getFinalPriceOrder)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        perf.setTotalGoods(totalGoods);

        BigDecimal totalShip = paymentRepository.findByStaff_AccountIdAndStatusAndActionAtBetween(
                        staff.getAccountId(),
                        PaymentStatus.DA_THANH_TOAN_SHIP,
                        startDate,
                        endDate
                ).stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

//        perf.setTotalShip(totalShip);

        long totalParcels = orders.stream()
                .flatMap(o -> o.getOrderLinks().stream())
                .count();
        perf.setTotalParcels(totalParcels);

        Double totalNetWeight = orders.stream()
                .flatMap(o -> o.getWarehouses().stream())
                .map(Warehouse::getNetWeight)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        perf.setTotalNetWeight(Math.round(totalNetWeight * 100.0) / 100.0);

        long completedOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DA_GIAO)
                .count();
        double completionRate = totalOrders > 0 ? (completedOrders * 100.0 / totalOrders) : 0.0;
        perf.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);

        long badFeedback = orders.stream()
                .map(Orders::getFeedback)
                .filter(Objects::nonNull)
                .filter(f -> f.getRating() < 3)
                .count();
        perf.setBadFeedbackCount(badFeedback);

        Map<String, StaffPerformance> result = new HashMap<>();
        result.put(staff.getStaffCode(), perf);

        return result;
    }

    public Map<String, StaffPerformance> getMyPerformanceByDateRange(LocalDate start, LocalDate end) {
        Account currentAccount = accountUtils.getAccountCurrent();
        if (!(currentAccount instanceof Staff staff) ||
                (staff.getRole() != AccountRoles.STAFF_SALE && staff.getRole() != AccountRoles.LEAD_SALE)) {
            throw new AccessDeniedException("Bạn không có quyền xem hiệu suất cá nhân!");
        }

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        List<Orders> orders = ordersRepository.findByStaffIdWithDetailsForPerformance(
                staff.getAccountId(), startDateTime, endDateTime);

        long totalOrders = orders.size();

        Set<OrderStatus> excludedStatuses = Set.of(
                OrderStatus.CHO_XAC_NHAN,
                OrderStatus.DA_XAC_NHAN,
                OrderStatus.CHO_THANH_TOAN,
                OrderStatus.DA_HUY
        );

        BigDecimal totalGoods = BigDecimal.ZERO;
        long totalParcels = 0;
        double totalNetWeight = 0.0;
        long completedOrders = 0;
        long badFeedback = 0;

        for (Orders order : orders) {
            if (order.getOrderLinks() != null) {
                totalParcels += order.getOrderLinks().size();
            }
            if (order.getWarehouses() != null) {
                for (Warehouse w : order.getWarehouses()) {
                    if (w.getNetWeight() != null) {
                        totalNetWeight += w.getNetWeight();
                    }
                }
            }

            if (order.getStatus() == OrderStatus.DA_GIAO) {
                completedOrders++;
            }
            if (order.getFeedback() != null && order.getFeedback().getRating() < 3) {
                badFeedback++;
            }

            if (!excludedStatuses.contains(order.getStatus()) && order.getOrderLinks() != null) {
                for (OrderLinks link : order.getOrderLinks()) {
                    if (link.getStatus() != OrderLinkStatus.DA_HUY && link.getFinalPriceVnd() != null) {
                        totalGoods = totalGoods.add(link.getFinalPriceVnd());
                    }
                }
            }
        }

        BigDecimal totalShip = paymentRepository.findByStaff_AccountIdAndStatusAndActionAtBetween(
                        staff.getAccountId(),
                        PaymentStatus.DA_THANH_TOAN_SHIP,
                        startDateTime,
                        endDateTime)
                .stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long newCustomersInPeriod = customerRepository.countByStaffIdAndCreatedAtBetween(
                staff.getAccountId(), startDateTime, endDateTime);

        double completionRate = totalOrders > 0 ? (completedOrders * 100.0 / totalOrders) : 0.0;

        StaffPerformance perf = new StaffPerformance();
        perf.setStaffCode(staff.getStaffCode());
        perf.setName(staff.getName());
        perf.setDepartment(staff.getDepartment());
        perf.setTotalOrders(totalOrders);
        perf.setTotalGoods(totalGoods);
//        perf.setTotalShip(totalShip);
        perf.setTotalParcels(totalParcels);
        perf.setTotalNetWeight(Math.round(totalNetWeight * 100.0) / 100.0);
        perf.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);
        perf.setNewCustomersInPeriod(newCustomersInPeriod);
        perf.setBadFeedbackCount(badFeedback);

        Map<String, StaffPerformance> result = new HashMap<>();
        result.put(staff.getStaffCode(), perf);

        return result;
    }

    public void changePassword(ChangePasswordRequest request) {
        Account currentAccount = accountUtils.getAccountCurrent();

        if (!passwordEncoder.matches(request.getOldPassword(), currentAccount.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác!");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp!");
        }

        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }


        if (passwordEncoder.matches(request.getNewPassword(), currentAccount.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu cũ!");
        }
        currentAccount.setPassword(passwordEncoder.encode(request.getNewPassword()));
        authenticationRepository.save(currentAccount);
    }

    public void sendForgotPasswordOtp(String email) throws Exception {
        Account account = authenticationRepository.findByEmail(email);
        if (account == null) {
            throw new BadCredentialsException("Email không tồn tại trong hệ thống!");
        }

        otpService.sendOtpToEmail(email);
    
    }

    public void resetPasswordWithOtp(String email, String otpCode, String newPassword) {
        Account account = authenticationRepository.findByEmail(email);
        if (account == null) {
            throw new BadCredentialsException("Email không tồn tại trong hệ thống!");
        }

        Otp otp = otpRepository.findByAccountAndCode(account, otpCode)
                .orElseThrow(() -> new BadCredentialsException("OTP không hợp lệ!"));

        if (otp.isExpired()) {
            throw new BadCredentialsException("OTP đã hết hạn!");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        authenticationRepository.save(account);

        otpRepository.delete(otp);
    }

    public Customer getCustomerById(Long customerId) {
        return customerRepository.getCustomerById(customerId);
    }

    public Customer partialUpdateCustomer(Long customerId, CustomerPatchRequest request) {
        Account account = accountUtils.getAccountCurrent();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng này không tồn tại!"));
        if (customer.getStaffId().equals(account.getAccountId()) || account.getRole().equals(AccountRoles.MANAGER)){
            if (request.getEmail() != null) customer.setEmail(request.getEmail());
            if (request.getPhone() != null) customer.setPhone(request.getPhone());
            if (request.getName() != null) customer.setName(request.getName());
            return customerRepository.save(customer);
        } else {
            throw new AccessDeniedException("Bạn không có quyền sửa thông tin khách hàng này!");
        }
    }

    public Staff partialUpdateStaff(Long staffId, StaffPatchRequest request) {
        Account account = accountUtils.getAccountCurrent();
        if (!account.getRole().equals(AccountRoles.MANAGER)){
            throw new AccessDeniedException("Chỉ manager mới được phép chỉnh sửa!");
        }
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Nhân viên này không tồn tại!"));
        if (request.getEmail() != null) staff.setEmail(request.getEmail());
        if (request.getPhone() != null) staff.setPhone(request.getPhone());
        if (request.getStatus() != null) staff.setStatus(request.getStatus());
        if (request.getName() != null) staff.setName(request.getName());
        if (request.getStaffCode() != null) staff.setStaffCode(request.getStaffCode());
        if (request.getDepartment() != null) staff.setDepartment(request.getDepartment());
        if (request.getLocation() != null) staff.setLocation(request.getLocation());
        if (request.getWarehouseLocationId() != null) {
            WarehouseLocation location = warehouseLocationRepository
                    .findById(request.getWarehouseLocationId())
                    .orElseThrow(() -> new BadRequestException("Địa điểm kho không tồn tại!"));
            staff.setWarehouseLocation(location);
        }

        return staffRepository.save(staff);
    }
}
