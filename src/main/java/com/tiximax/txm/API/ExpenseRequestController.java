package com.tiximax.txm.API;

import com.tiximax.txm.Entity.ExpenseRequest;
import com.tiximax.txm.Enums.ExpenseStatus;
import com.tiximax.txm.Model.CreateExpenseRequest;
import com.tiximax.txm.Service.ExpenseRequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/expense-request")
@SecurityRequirement(name = "bearerAuth")

public class ExpenseRequestController {

    @Autowired
    private ExpenseRequestService expenseRequestService;

    @PostMapping
    public ResponseEntity<ExpenseRequest> createExpenseRequest(
            @RequestBody CreateExpenseRequest request) {
        ExpenseRequest created = expenseRequestService.createExpenseRequest(request);
        return ResponseEntity.ok(created);
    }

    @PatchMapping("/approve/{requestId}")
    public ResponseEntity<ExpenseRequest> approve(
            @PathVariable Long requestId,
            @RequestBody String image) {
        ExpenseRequest approved = expenseRequestService.approveExpenseRequest(requestId, image);
        return ResponseEntity.ok(approved);
    }

    @PatchMapping("/reject/{requestId}")
    public ResponseEntity<ExpenseRequest> reject(
            @PathVariable Long requestId,
            @RequestBody String reason) {
        ExpenseRequest rejected = expenseRequestService.rejectExpenseRequest(requestId, reason);
        return ResponseEntity.ok(rejected);
    }

    @PatchMapping("/cancel/{requestId}")
    public ResponseEntity<ExpenseRequest> cancel(
            @PathVariable Long requestId) {
        ExpenseRequest cancelled = expenseRequestService.cancelExpenseRequest(requestId);
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping("/{page}/{size}/{status}")
    public ResponseEntity<Page<ExpenseRequest>> getList(
            @PathVariable int page,
            @PathVariable int size,
            @RequestParam(required = false) ExpenseStatus status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ExpenseRequest> requestPage = expenseRequestService.getAllExpenseRequests(status, pageable);
        return ResponseEntity.ok(requestPage);
    }

    @GetMapping("/detail/{requestId}")
    public ResponseEntity<ExpenseRequest> getDetail(@PathVariable Long requestId) {
        ExpenseRequest request = expenseRequestService.getExpenseRequestById(requestId);
        return ResponseEntity.ok(request);
    }
}
