package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.CustomerVoucher;
import com.tiximax.txm.Entity.Route;
import com.tiximax.txm.Entity.Voucher;
import com.tiximax.txm.Enums.AssignType;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Model.DTORequest.Order.VoucherCreateRequest;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Repository.CustomerVoucherRepository;
import com.tiximax.txm.Repository.RouteRepository;
import com.tiximax.txm.Repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service

public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private CustomerRepository customerRepository;

//    public Voucher createVoucher(Voucher voucher, Set<Long> routeIds) {
//        if (voucher.getCode() == null || voucherRepository.findByCode(voucher.getCode()) != null) {
//            throw new IllegalArgumentException("Mã voucher đã tồn tại hoặc không hợp lệ!");
//        }
//        if (voucher.getAssignType() == AssignType.DAT_CHI_TIEU && voucher.getThresholdAmount() == null) {
//            throw new IllegalArgumentException("Cần nhập số ký chỉ tiêu cho loại voucher cần chỉ tiêu như này!");
//        }
//
//        if (routeIds != null && !routeIds.isEmpty()) {
//            Set<Route> routes = new HashSet<>();
//            for (Long routeId : routeIds) {
//                Route route = routeRepository.findById(routeId).orElseThrow(() -> new IllegalArgumentException("Kiểm tra lại tuyến vì có tuyến không tồn tại!"));
//                routes.add(route);
//            }
//            voucher.setApplicableRoutes(routes);
//        }
//
//        return voucherRepository.save(voucher);
//    }

    public Voucher createVoucher(VoucherCreateRequest request) {
        Voucher voucher = new Voucher();
        voucher.setCode(request.getCode());
        voucher.setType(request.getType());
        voucher.setValue(request.getValue());
        voucher.setDescription(request.getDescription());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setMinOrderValue(request.getMinOrderValue());
//        voucher.setMaxUses(request.getMaxUses());
        voucher.setAssignType(request.getAssignType());
        voucher.setThresholdAmount(request.getThresholdAmount());

        if (voucher.getCode() == null || voucherRepository.findByCode(voucher.getCode()) != null) {
            throw new BadRequestException("Mã voucher đã tồn tại hoặc không hợp lệ!");
        }
        if (voucher.getAssignType() == AssignType.DAT_CHI_TIEU && voucher.getThresholdAmount() == null) {
            throw new BadRequestException("Cần nhập số ký chỉ tiêu cho loại voucher cần chỉ tiêu như này!");
        }

        if (request.getRouteIds() != null && !request.getRouteIds().isEmpty()) {
            Set<Route> routes = new HashSet<>();
            for (Long routeId : request.getRouteIds()) {
                Route route = routeRepository.findById(routeId).orElseThrow(() -> new IllegalArgumentException("Kiểm tra lại tuyến vì có tuyến không tồn tại!"));
                routes.add(route);
            }
            voucher.setApplicableRoutes(routes);
        }

        return voucherRepository.save(voucher);
    }

    public Page<Voucher> getAllVouchers(Pageable pageable) {
        return voucherRepository.findAll(pageable);
    }

    public Voucher getVoucherById(Long voucherId) {
        return voucherRepository.findById(voucherId).orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại."));
    }

    public Voucher updateVoucher(Long voucherId, VoucherCreateRequest request) {
        Voucher existing = getVoucherById(voucherId);
        if (request.getCode() != null) existing.setCode(request.getCode());
        if (request.getType() != null) existing.setType(request.getType());
        if (request.getValue() != null) existing.setValue(request.getValue());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getStartDate() != null) existing.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) existing.setEndDate(request.getEndDate());
        if (request.getMinOrderValue() != null) existing.setMinOrderValue(request.getMinOrderValue());
        if (request.getAssignType() != null) existing.setAssignType(request.getAssignType());
        if (request.getThresholdAmount() != null) existing.setThresholdAmount(request.getThresholdAmount());

        Set<Long> routeIds = request.getRouteIds();
        if (routeIds != null && !routeIds.isEmpty()) {
            Set<Route> routes = new HashSet<>();
            for (Long routeId : routeIds) {
                Route route = routeRepository.findById(routeId).orElseThrow(() -> new IllegalArgumentException("Kiểm tra lại tuyến vì có tuyến không tồn tại!"));
                routes.add(route);
            }
            existing.setApplicableRoutes(routes);
        }

        return voucherRepository.save(existing);
    }

    public void deleteVoucher(Long voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        voucherRepository.delete(voucher);
    }

    public void assignVoucherToCustomer(Customer customer, Voucher voucher) {
        if (customerVoucherRepository.existsByCustomerAndVoucher(customer, voucher)) {
            return;
        }
        CustomerVoucher cv = new CustomerVoucher();
        cv.setCustomer(customer);
        cv.setVoucher(voucher);
        cv.setAssignedDate(LocalDateTime.now());
        cv.setUsesRemaining(1);
        cv.setUsed(false);
        customerVoucherRepository.save(cv);
    }

    public void assignOnRegisterVouchers(Customer customer) {
        List<Voucher> vouchers = voucherRepository.findByAssignType(AssignType.DANG_KI_TK);
        for (Voucher v : vouchers) {
            assignVoucherToCustomer(customer, v);
        }
    }

    public void checkAndAssignOnWeightThreshold(Customer customer) {
        List<Voucher> vouchers = voucherRepository.findByAssignType(AssignType.DAT_CHI_TIEU);
        for (Voucher v : vouchers) {
            if (v.getThresholdAmount() != null && customer.getTotalWeight() >= v.getThresholdAmount() &&
                    !customerVoucherRepository.existsByCustomerAndVoucher(customer, v)) {
                assignVoucherToCustomer(customer, v);
            }
        }
    }

    public List<CustomerVoucher> getUnusedVouchersByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new RuntimeException("Khách này không tồn tại!"));
        return customerVoucherRepository.findByCustomerAndIsUsedFalse(customer);
    }

    public boolean usedVoucher(Long customerId, Long voucherId) {
        Optional<CustomerVoucher> customerVoucher = customerVoucherRepository.findByCustomer_AccountIdAndVoucher_VoucherId(customerId, voucherId);
        if (customerVoucher.isPresent()){
            customerVoucher.get().setUsed(true);
            customerVoucher.get().setUsedDate(LocalDateTime.now());
            customerVoucherRepository.save(customerVoucher.get());
            return true;
        }
        return false;
    }
}
