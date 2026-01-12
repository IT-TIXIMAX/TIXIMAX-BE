package com.tiximax.txm.API;

import com.tiximax.txm.Entity.CustomerVoucher;
import com.tiximax.txm.Entity.Voucher;
import com.tiximax.txm.Model.VoucherCreateRequest;
import com.tiximax.txm.Service.VoucherService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/vouchers")
@SecurityRequirement(name = "bearerAuth")

public class VoucherController {
    @Autowired
    private VoucherService voucherService;

    @PostMapping
    public ResponseEntity<Voucher> createVoucher(@RequestBody VoucherCreateRequest request) {
        Voucher created = voucherService.createVoucher(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{page}/{size}/paging")
    public ResponseEntity<Page<Voucher>> getAllVouchers(@PathVariable int page, @PathVariable int size) {
        Sort sort = Sort.by("startDate").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Voucher> vouchersPage = voucherService.getAllVouchers(pageable);
        return ResponseEntity.ok(vouchersPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Voucher> getVoucherById(@PathVariable Long id) {
        Voucher voucher = voucherService.getVoucherById(id);
        return ResponseEntity.ok(voucher);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Voucher> updateVoucher(@PathVariable Long id, @RequestBody VoucherCreateRequest request) {
        Voucher updated = voucherService.updateVoucher(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{voucherId}/assign/{customerId}")
    public ResponseEntity<Void> assignVoucherToCustomer(@PathVariable Long voucherId, @PathVariable Long customerId) {
        // Giả sử bạn có CustomerService để get customer by id
        // Customer customer = customerService.getCustomerById(customerId);
        // Voucher voucher = voucherService.getVoucherById(voucherId);
        // voucherService.assignVoucherToCustomer(customer, voucher);
        // return new ResponseEntity<>(HttpStatus.OK);
        // Note: Cần inject CustomerService và implement nếu dùng.
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<CustomerVoucher>> getUnusedCustomerVouchers(@PathVariable Long customerId) {
        List<CustomerVoucher> unusedVouchers = voucherService.getUnusedVouchersByCustomerId(customerId);
        return new ResponseEntity<>(unusedVouchers, HttpStatus.OK);
    }

    @GetMapping("/test/customer/{customerId}/{voucherId}")
    public ResponseEntity<Boolean> gettest(@PathVariable Long customerId, @PathVariable Long voucherId) {
        return ResponseEntity.ok(voucherService.usedVoucher(customerId, voucherId));
    }
}
