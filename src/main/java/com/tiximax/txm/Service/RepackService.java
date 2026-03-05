package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Repack;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.RepackStatus;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Repository.RepackRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

@Service

public class RepackService {

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RepackRepository repackRepository;

//    @Transactional
//    public Repack createEmptyRepack(String customerCode) {
//        Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
//
//        Customer customer = customerRepository.findByCustomerCode(customerCode)
//                .orElseThrow(() -> new BadRequestException("Customer not found with code: " + customerCode));
//
//        Long customerId = customer.getAccountId();
//
//        long count = repackRepository.countByCustomerCustomerId(customerId);
//        String repackCode = customer.getCustomerCode() + "RP" + String.format("%03d", count + 1);
//
//        if (repackRepository.existsByRepackCode(repackCode)) {
//            throw new BadRequestException("Package code already exists, please try again");
//        }
//
//        Repack repack = new Repack();
//        repack.setRepackCode(repackCode);
//        repack.setCustomer(customer);
//        repack.setStaff(currentStaff);
//        repack.setStatus(RepackStatus.DANG_THUC_HIEN);
//        repack.setCreatedAt(LocalDateTime.now());
//        repack.setRepackList(new ArrayList<>());
//        repack.setRelatedWarehouses(new HashSet<>());
//
//        return repackRepository.save(repack);
//    }

    public void deleteEmptyRepack(Long repackId) {
    }
}
