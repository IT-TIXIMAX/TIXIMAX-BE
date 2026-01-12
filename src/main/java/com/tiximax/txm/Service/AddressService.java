package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Address;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Model.DTORequest.Address.AddressRequest;
import com.tiximax.txm.Repository.AddressRepository;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service

public class AddressService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CustomerRepository customerRepository;

    public Set<Address> getAddressesByCustomerCode(String customerCode) {
        Account currentAccount = accountUtils.getAccountCurrent();

        Optional<Customer> customerOptional = customerRepository.findByCustomerCode(customerCode);

        if (!customerOptional.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng với mã: " + customerCode);
        }
        Customer customer = customerOptional.get();

        if (currentAccount instanceof Customer) {
            if (!customer.getCustomerCode().equals(((Customer) currentAccount).getCustomerCode())) {
                throw new SecurityException("Bạn không có quyền xem địa chỉ của khách hàng này!");
            }
        } else if (currentAccount instanceof Staff) {
            if (customer.getStaffId() == null || !customer.getStaffId().equals(currentAccount.getAccountId())) {
                throw new SecurityException("Bạn không có quyền xem địa chỉ của khách hàng này!");
            }
        } else {
            throw new SecurityException("Loại tài khoản không hợp lệ!");
        }

        if (customer.getAddresses() == null || customer.getAddresses().isEmpty()) {
            throw new IllegalArgumentException("Khách hàng này chưa có địa chỉ nào!");
        }

        return customer.getAddresses();
    }

    public Address createAddress(String customerCode, AddressRequest request) {
        Account currentAccount = accountUtils.getAccountCurrent();
        Optional<Customer> customerOptional = customerRepository.findByCustomerCode(customerCode);
        if (!customerOptional.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng với mã: " + customerCode);
        }
        Customer customer = customerOptional.get();

        if (request.getAddressName() == null || request.getAddressName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên địa chỉ không được để trống!");
        }

        Address address = new Address();
        address.setAddressName(request.getAddressName());
        address.setCustomer(customer);
        Address savedAddress = addressRepository.save(address);
        messagingTemplate.convertAndSend(
                "/topic/Tiximax",
                Map.of(
                        "event", "INSERT",
                        "address", savedAddress,
                        "customerCode", customerCode,
                        "message", "Địa chỉ mới được thêm!"
                )
        );
        return addressRepository.save(savedAddress);
    }

    public Address updateAddress(String customerCode, Long addressId, String addressName) {
        Account currentAccount = accountUtils.getAccountCurrent();
        Optional<Customer> customerOptional = customerRepository.findByCustomerCode(customerCode);
        if (!customerOptional.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng với mã: " + customerCode);
        }
        Customer customer = customerOptional.get();

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ này!"));
        if (!address.getCustomer().getCustomerCode().equals(customerCode)) {
            throw new IllegalArgumentException("Địa chỉ này không thuộc về khách hàng với mã: " + customerCode);
        }

        if (addressName == null || addressName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên địa chỉ không được để trống!");
        }

        address.setAddressName(addressName);
        return addressRepository.save(address);
    }

    public void deleteAddress(String customerCode, Long addressId) {
        Account currentAccount = accountUtils.getAccountCurrent();
        Optional<Customer> customerOptional = customerRepository.findByCustomerCode(customerCode);
        if (!customerOptional.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng với mã: " + customerCode);
        }
        Customer customer = customerOptional.get();

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ này!"));
        if (!address.getCustomer().getCustomerCode().equals(customerCode)) {
            throw new IllegalArgumentException("Địa chỉ này không thuộc về khách hàng với mã: " + customerCode);
        }

        addressRepository.deleteById(addressId);
    }

}
