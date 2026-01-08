package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.ExpenseRequest;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.ExpenseStatus;
import com.tiximax.txm.Enums.PaymentMethod;
import com.tiximax.txm.Model.CreateExpenseRequest;
import com.tiximax.txm.Repository.ExpenseRequestRepository;
import com.tiximax.txm.Utils.AccountUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service

public class ExpenseRequestService {

    @Autowired
    private AccountUtils accountUtils;

    @Autowired
    private ExpenseRequestRepository expenseRequestRepository;

    public ExpenseRequest createExpenseRequest(CreateExpenseRequest request) {
        Staff currentStaff = (Staff) accountUtils.getAccountCurrent();

        if (!currentStaff.getCanRequestExpenses()) {
            throw new IllegalArgumentException("Bạn không có quyền gửi đề nghị thanh toán");
        }

        if (request.getPaymentMethod() == PaymentMethod.CHUYEN_KHOAN && (request.getBankInfo() == null || request.getBankInfo().isBlank())) {
            throw new IllegalArgumentException("Vui lòng cung cấp thông tin tài khoản ngân hàng khi chọn chuyển khoản");
        }

        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription(request.getDescription());
        expenseRequest.setQuantity(request.getQuantity());
        expenseRequest.setUnitPrice(request.getUnitPrice());
        expenseRequest.setTotalAmount(expenseRequest.getUnitPrice().multiply(BigDecimal.valueOf(expenseRequest.getQuantity())));
        expenseRequest.setNote(request.getNote());
        expenseRequest.setPaymentMethod(request.getPaymentMethod());
        expenseRequest.setBankInfo(request.getBankInfo());
        expenseRequest.setVatStatus(request.getVatStatus());
        expenseRequest.setVatInfo(request.getVatInfo());
        expenseRequest.setStatus(ExpenseStatus.CHO_DUYET);
        expenseRequest.setRequester(currentStaff);
        expenseRequest.setDepartment(currentStaff.getDepartment());
        expenseRequest.setCreatedAt(LocalDateTime.now());
        return expenseRequestRepository.save(expenseRequest);
    }

    public ExpenseRequest approveExpenseRequest(Long requestId, String image) {
        Staff currentStaff = (Staff) accountUtils.getAccountCurrent();

        if (!currentStaff.getCanApproveExpenses()) {
            throw new IllegalArgumentException("Bạn không có quyền phê duyệt đề nghị thanh toán");
        }

        ExpenseRequest request = expenseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề nghị thanh toán"));

        if (request.getStatus() != ExpenseStatus.CHO_DUYET) {
            throw new IllegalStateException("Chỉ có thể duyệt đề nghị đang chờ xử lý");
        }

        request.setStatus(ExpenseStatus.DA_DUYET);
        request.setApprover(currentStaff);

        if (request.getPaymentMethod() == PaymentMethod.CHUYEN_KHOAN) {
            if (image == null) {
                throw new IllegalArgumentException("Vui lòng cung cấp ảnh chuyển khoản khi duyệt thanh toán chuyển khoản");
            }
            request.setTransferImage(image);
        }

        return expenseRequestRepository.save(request);
    }

    public ExpenseRequest cancelExpenseRequest(Long requestId) {
        Staff currentStaff = (Staff) accountUtils.getAccountCurrent();

        ExpenseRequest request = expenseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề nghị"));

        if (!request.getRequester().getAccountId().equals(currentStaff.getAccountId())) {
            throw new IllegalAccessError("Bạn không có quyền hủy đề nghị này. Chỉ người tạo yêu cầu mới được quyền hủy");
        }

        if (request.getStatus() != ExpenseStatus.CHO_DUYET) {
            throw new IllegalStateException("Chỉ có thể hủy đề nghị đang chờ duyệt");
        }

        request.setStatus(ExpenseStatus.DA_HUY);

        return expenseRequestRepository.save(request);
    }

    public Page<ExpenseRequest> getAllExpenseRequests(ExpenseStatus status, Pageable pageable) {
        Staff currentStaff = (Staff) accountUtils.getAccountCurrent();
        Long staffId = currentStaff.getAccountId();

        if (currentStaff.getCanApproveExpenses()) {
            return expenseRequestRepository.findAllForApprover(status, pageable);
        } else if (currentStaff.getCanRequestExpenses()) {
            return expenseRequestRepository.findOwnForRequester(staffId, status, pageable);
        } else {
            throw new RuntimeException("Bạn không có quyền xem đề nghị thanh toán");
        }
    }

    public ExpenseRequest getExpenseRequestById(Long requestId) {
        return expenseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề nghị"));
    }

    public ExpenseRequest rejectExpenseRequest(Long requestId, String reason) {
        Staff currentStaff = (Staff) accountUtils.getAccountCurrent();

        if (!currentStaff.getCanApproveExpenses()) {
            throw new IllegalArgumentException("Bạn không có quyền phê duyệt đề nghị thanh toán");
        }

        ExpenseRequest request = expenseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề nghị thanh toán"));

        if (request.getStatus() != ExpenseStatus.CHO_DUYET) {
            throw new IllegalStateException("Chỉ có thể duyệt đề nghị đang chờ xử lý");
        }

        request.setStatus(ExpenseStatus.TU_CHOI);
        request.setApprover(currentStaff);
        request.setCancelReason(reason);

        return expenseRequestRepository.save(request);
    }
}
