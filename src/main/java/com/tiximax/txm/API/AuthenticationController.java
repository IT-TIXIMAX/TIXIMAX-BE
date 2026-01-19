package com.tiximax.txm.API;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.AccountRoles;
import com.tiximax.txm.Model.DTORequest.Auth.ChangePasswordRequest;
import com.tiximax.txm.Model.DTORequest.Auth.ForgotPasswordRequest;
import com.tiximax.txm.Model.DTORequest.Auth.LoginRequest;
import com.tiximax.txm.Model.DTORequest.Auth.RegisterCustomerRequest;
import com.tiximax.txm.Model.DTORequest.Auth.RegisterStaffRequest;
import com.tiximax.txm.Model.DTORequest.Auth.ResetPasswordRequest;
import com.tiximax.txm.Model.DTORequest.Auth.StaffPatchRequest;
import com.tiximax.txm.Model.DTORequest.Auth.VerifyAccountRequest;
import com.tiximax.txm.Model.DTORequest.Customer.CustomerPatchRequest;
import com.tiximax.txm.Model.DTOResponse.Auth.EmailDetail;
import com.tiximax.txm.Model.DTOResponse.Customer.CustomerResponseDTO;
import com.tiximax.txm.Model.DTOResponse.DashBoard.SaleStats;
import com.tiximax.txm.Model.DTOResponse.DashBoard.StaffPerformance;
import com.tiximax.txm.Service.*;
import com.tiximax.txm.Utils.AccountUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import net.minidev.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.json.JSONObject;

@RestController
@CrossOrigin
@RequestMapping("/accounts")
@SecurityRequirement(name = "bearerAuth")

public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PaymentService paymentService;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseAnonKey;

    private final WebClient webClient = WebClient.create();

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequest loginRequest) {
        try {
        Object account = authenticationService.login(loginRequest);
        return ResponseEntity.ok(account);
         } catch (AuthenticationServiceException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
        }
    }
  
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {

       
        String token = authorizationHeader.replace("Bearer ", "").trim();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + "/auth/v1/user"))
                .header("Authorization", "Bearer " + token)
                .header("apikey", supabaseAnonKey)
                .GET()
                .build();
     
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            String email = json.optString("email", "");
            JSONObject metadata = json.optJSONObject("user_metadata");
            String name = metadata != null ? metadata.optString("full_name", "") : "";

            Account account = authenticationService.verifyAndSaveUser(email, name);

            String appJwt = tokenService.generateToken(account);

            return ResponseEntity.ok(Map.of(
                    "message", "Login success",
                    "jwt", appJwt,
                    "user", Map.of(
                        "id", account.getAccountId(),
                        "email", account.getEmail(),
                        "name", account.getName(),
                        "role", account.getRole()
                    )
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Supabase token"));
        }

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
}

    @PreAuthorize("hasAnyRole('MANAGER')")
    @PostMapping("/register/staff")
    public ResponseEntity<Staff> registerStaff(@RequestBody RegisterStaffRequest registerRequest) {
        Staff staff = authenticationService.registerStaff(registerRequest);
        return ResponseEntity.ok(staff);
    }

    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@RequestBody RegisterCustomerRequest registerRequest) {
    try {
        Customer customer = authenticationService.registerCustomer(registerRequest);
        return ResponseEntity.ok(customer);
    } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/customer/by-staff")
    public ResponseEntity<Customer> registerCustomerByStaff(@RequestBody RegisterCustomerRequest registerRequest) {
        Customer customer = authenticationService.registerCustomerByStaff(registerRequest);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/accountCurrent")
    public ResponseEntity<?> current() {
        Account account = accountUtils.getAccountCurrent();
        return ResponseEntity.ok(account);
    }

    @GetMapping("/send-mail")
    public void sendMail() {
        EmailDetail emailDetail = new EmailDetail();
        emailDetail.setRecipient("thinhnguyen.231003@gmail.com");
        emailDetail.setSubject("test123");
        emailDetail.setMsgBody("abc");
        emailService.sendMailTemplate(emailDetail);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        authenticationService.logout();
        return ResponseEntity.ok("Đăng xuất thành công!");
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchCustomers(@RequestParam(required = false) String keyword) {
        List<Customer> customers = authenticationService.searchCustomersByPhoneOrName(keyword);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/enum-account-role")
    public ResponseEntity<List<String>> getAccountrole() {
        List<String> accountRole = Arrays.stream(AccountRoles.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(accountRole);
    }

    @GetMapping("/staff/{page}/{size}")
    public ResponseEntity<Page<Staff>> getAllStaff(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Staff> staffPage = authenticationService.getAllStaff(pageable);
        return ResponseEntity.ok(staffPage);
    }

    @GetMapping("/customers/{page}/{size}")
    public ResponseEntity<Page<CustomerResponseDTO>> getAllCustomers(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Customer> customersPage = authenticationService.getAllCustomers(pageable);
        Page<CustomerResponseDTO> dtoPage = customersPage.map(CustomerResponseDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/my-customers/{page}/{size}")
    public ResponseEntity<Page<CustomerResponseDTO>> getCustomersByStaff(
            @PathVariable int page,
            @PathVariable int size,
            @RequestParam(required = false) String search
    ) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Customer> customersPage = authenticationService.getCustomersByStaff(search, pageable);
        Page<CustomerResponseDTO> dtoPage = customersPage.map(CustomerResponseDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/sale-lead-staff/{page}/{size}")
    public ResponseEntity<Page<Staff>> getSaleLeadStaff(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Staff> staffPage = authenticationService.getSaleAndLeadSaleStaff(pageable);
        return ResponseEntity.ok(staffPage);
    }

    @GetMapping("/sales-in-route/{page}/{size}")
    public ResponseEntity<Page<Staff>> getSalesInSameRoute(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Staff> salesPage = authenticationService.getSalesInSameRoute(pageable);
        return ResponseEntity.ok(salesPage);
    }

    @GetMapping("/sales-in-route/stats")
    public ResponseEntity<List<SaleStats>> getSalesStatsInSameRoute(
            @RequestParam(required = false) String timeFrame,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer week) {
        List<SaleStats> stats = authenticationService.getSalesStatsInSameRoute(timeFrame, year, month, week);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/login-google")
    public ResponseEntity<String> loginWithGoogle() {
        String url = supabaseUrl + "/auth/v1/authorize?provider=google";
        return ResponseEntity.ok(url);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) {

        String tokenUrl = supabaseUrl + "/auth/v1/token?grant_type=authorization_code";

        String body = "{ \"code\": \"" + code + "\", " +
                "\"redirect_to\": \"http://localhost:8080/auth/callback\" }";

        String response = webClient.post()
                .uri(tokenUrl)
                .header("apikey", supabaseAnonKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/userinfo")
    public ResponseEntity<String> getUserInfo(@RequestHeader("Authorization") String bearerToken) {

        String response = webClient.get()
                .uri(supabaseUrl + "/auth/v1/user")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", bearerToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-account")
    public ResponseEntity<?> verifyAccount(@RequestBody VerifyAccountRequest request) throws Exception {
       try {
            boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtpCode());

            if (isValid) {
                return ResponseEntity.ok("Tài khoản của bạn đã được xác minh thành công!");
            } else {
                return ResponseEntity.badRequest().body("Mã OTP không hợp lệ hoặc đã hết hạn!");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-performance/current-month")
    public ResponseEntity<Map<String, StaffPerformance>> getMyCurrentMonthPerformance() {
        Map<String, StaffPerformance> performanceMap =
                authenticationService.getMyCurrentMonthPerformanceMap();
        return ResponseEntity.ok(performanceMap);
    }

//    @GetMapping("/my-performance/{routeId}/{staffId}")
//    public ResponseEntity<Map<String, StaffPerformance>> getMyPerformance(
//            @RequestParam(required = false) Long routeId,
//            @PathVariable Long staffId,
//            @RequestParam String startDate,
//            @RequestParam String endDate) {
//
//        LocalDate start;
//        LocalDate end;
//        try {
//            start = LocalDate.parse(startDate);
//            end = LocalDate.parse(endDate);
//        } catch (DateTimeParseException e) {
//            throw new IllegalArgumentException("startDate và endDate phải đúng định dạng YYYY-MM-DD");
//        }
//
//        if (end.isBefore(start)) {
//            throw new IllegalArgumentException("endDate phải sau hoặc bằng startDate");
//        }
//
//        Map<String, StaffPerformance> performanceMap =
//                authenticationService.getMyPerformanceByDateRange(start, end, routeId);
//
//        return ResponseEntity.ok(performanceMap);
//    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            authenticationService.changePassword(request);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
                    
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Đã có lỗi xảy ra, vui lòng thử lại sau"));
        }
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody ForgotPasswordRequest request) throws Exception {
        authenticationService.sendForgotPasswordOtp(request.getEmail());
        return ResponseEntity.ok("OTP đã được gửi vào email!");
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest dto) {
        authenticationService.resetPasswordWithOtp(dto.getEmail(), dto.getOtp(), dto.getNewPassword());
        return ResponseEntity.ok("Mật khẩu đã được đặt lại thành công!");
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long customerId) {
        return ResponseEntity.ok(authenticationService.getCustomerById(customerId));
    }

    @PatchMapping("/customer/{customerId}")
    public ResponseEntity<Customer> partialUpdateCustomer(
            @PathVariable Long customerId,
            @RequestBody CustomerPatchRequest request) {
        Customer updated = authenticationService.partialUpdateCustomer(customerId, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/staff/{staffId}")
    public ResponseEntity<Staff> partialUpdateStaff(
            @PathVariable Long staffId,
            @RequestBody StaffPatchRequest request) {
        Staff updated = authenticationService.partialUpdateStaff(staffId, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/refund-balance/{customerId}")
    public ResponseEntity<Payment> processBalanceMoney(
            @PathVariable Long customerId,
            @RequestParam(required = true) String image,
            @RequestParam(required = true) BigDecimal amount) {
        Payment payment = paymentService.refundFromBalance(customerId, amount, image);
        return ResponseEntity.ok(payment);
    }
}