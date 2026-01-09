package com.tiximax.txm.API;

import com.tiximax.txm.Entity.BankAccount;
import com.tiximax.txm.Service.BankAccountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/bankAccounts")
@SecurityRequirement(name = "bearerAuth")

public class BankAccountController {
    @Autowired
    private BankAccountService bankAccountService;
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PostMapping
    public ResponseEntity<BankAccount> createBankAccount(@RequestBody BankAccount bankAccount) {
        BankAccount savedAccount = bankAccountService.save(bankAccount);
        return ResponseEntity.ok(savedAccount);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAccount> getBankAccountById(@PathVariable Long id) {
        Optional<BankAccount> account = bankAccountService.findById(id);
        return account.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<BankAccount>> getAllBankAccounts() {
        List<BankAccount> accounts = bankAccountService.findAll();
        return ResponseEntity.ok(accounts);
    }
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<BankAccount> updateBankAccount(@PathVariable Long id, @RequestBody BankAccount bankAccount) {
        Optional<BankAccount> existingAccount = bankAccountService.findById(id);
        if (existingAccount.isPresent()) {
            bankAccount.setId(id);
            BankAccount updatedAccount = bankAccountService.save(bankAccount);
            return ResponseEntity.ok(updatedAccount);
        }
        return ResponseEntity.notFound().build();
    }
    @PreAuthorize("hasAnyRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long id) {
        if (bankAccountService.findById(id).isPresent()) {
            bankAccountService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/filter/proxy")
    public ResponseEntity<List<BankAccount>> getBankAccountsByProxy(){
        List<BankAccount> accounts;
        accounts = bankAccountService.findByProxy();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/filter/revenue")
    public ResponseEntity<List<BankAccount>> getBankAccountsByRevenue(){
        List<BankAccount> accounts;
        accounts = bankAccountService.findByRevenue();
        return ResponseEntity.ok(accounts);
    }
}