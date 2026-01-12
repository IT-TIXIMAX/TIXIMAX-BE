package com.tiximax.txm.API;

import com.tiximax.txm.Entity.Address;
import com.tiximax.txm.Model.DTORequest.Address.AddressRequest;
import com.tiximax.txm.Service.AddressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@CrossOrigin
@RequestMapping("/addresses")
@SecurityRequirement(name = "bearerAuth")

public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping("/customer-addresses/{customerCode}")
    public ResponseEntity<Set<Address>> getCustomerAddresses(@PathVariable String customerCode) {
        Set<Address> addresses = addressService.getAddressesByCustomerCode(customerCode);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/{customerCode}")
    public ResponseEntity<Address> createAddress(@PathVariable String customerCode, @RequestBody AddressRequest request) {
        Address address = addressService.createAddress(customerCode, request);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{customerCode}/{addressId}")
    public ResponseEntity<Address> updateAddress(@PathVariable String customerCode,
                                                 @PathVariable Long addressId,
                                                 @RequestBody String addressName) {
        Address address = addressService.updateAddress(customerCode, addressId, addressName);
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{customerCode}/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable String customerCode, @PathVariable Long addressId) {
        addressService.deleteAddress(customerCode, addressId);
        return ResponseEntity.ok("Xóa địa chỉ thành công!");
    }

}
