package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Repack;
import com.tiximax.txm.Service.RepackService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/purchases")
@SecurityRequirement(name = "bearerAuth")

public class RepackController {
    @Autowired
    private RepackService repackService;

//    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
//    @PostMapping("/create")
//    public ResponseEntity<Repack> createRepackForCustomer(@RequestBody String customerCode) {
//        Repack repack = repackService.createEmptyRepack(customerCode);
//        return ResponseEntity.ok(repack);
//    }

//    @PreAuthorize("hasAnyRole('STAFF_WAREHOUSE_FOREIGN')")
//    @DeleteMapping("/{repackId}")
//    public ResponseEntity<String> deleteEmptyRepack(@PathVariable Long repackId) {
//        repackService.deleteEmptyRepack(repackId);
//        return ResponseEntity.ok("Đã xóa thùng repack thành công");
//    }
}
