package com.tiximax.txm.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.DraftDomestic;
import com.tiximax.txm.Enums.WarehouseStatus;
import com.tiximax.txm.Model.DTORequest.DraftDomesticRequest;
import com.tiximax.txm.Model.DTOResponse.DraftDomesticResponse;
import com.tiximax.txm.Repository.CustomerRepository;
import com.tiximax.txm.Repository.DraftDomesticRepository;
import com.tiximax.txm.Repository.WarehouseRepository;

@Service
public class DraftDomesticService {
 @Autowired
 private DraftDomesticRepository draftDomesticRepository;
 @Autowired 
 private CustomerRepository customerRepository;
 @Autowired
 private WarehouseRepository warehouseRepository;

//  public DraftDomesticResponse addDraftDomestic(DraftDomesticRequest draft){
//     var customer = customerRepository.findByCustomerCode(draft.getCustomerCode());
//     if(customer == null){
//         throw new IllegalArgumentException("Không tìm thấy khách hàng");
//     }
//     validateAllTrackingCodesExist(draft.getShippingList());
//     var draftDomestic = mapToEntity(draft, customer.get());
    
//     draftDomesticRepository.save(draftDomestic);
//     return new DraftDomesticResponse(draftDomestic);
//  } 
//      public Page<DraftDomesticResponse> getAllDraftDomestic(
//         String customerCode,
//         String shipmentCode,
//         Pageable pageable
// ) {
//     Page<DraftDomestic> page =
//             draftDomesticRepository.findAllWithFilter(
//                     customerCode,
//                     shipmentCode,
//                     pageable
//             );
//     return page.map(this::mapToResponse);
// }

//   private DraftDomestic mapToEntity(
//             DraftDomesticRequest request,
//             Customer customer
//     ) {
//         DraftDomestic draft = new DraftDomestic();
//         draft.setCustomer(customer);
//         draft.setPhoneNumber(request.getPhoneNumber());
//         draft.setAddress(request.getAddress());
//         draft.setShippingList(request.getShippingList());
//         return draft;
//     }
//     private DraftDomesticResponse mapToResponse(DraftDomestic entity) {
//     return new DraftDomesticResponse(entity);
// }

//    private void validateAllTrackingCodesExist(List<String> shippingList) {

//     if (shippingList == null || shippingList.isEmpty()) {
//         throw new IllegalArgumentException("Danh sách mã vận đơn không được để trống");
//     }

//     Set<String> inputCodes = shippingList.stream()
//             .map(String::trim)
//             .filter(s -> !s.isEmpty())
//             .collect(Collectors.toSet());

//     List<String> validCodes =
//             warehouseRepository.findExistingTrackingCodesByStatus(
//                     new ArrayList<>(inputCodes),
//                     WarehouseStatus.CHO_GIAO
//             );

//     if (validCodes.size() != inputCodes.size()) {

//         Set<String> invalidCodes = new HashSet<>(inputCodes);
//         invalidCodes.removeAll(validCodes);

//         throw new IllegalArgumentException(
//                 "Các mã vận đơn không hợp lệ hoặc không ở trạng thái CHO_GIAO: "
//                         + invalidCodes
//         );
//     }
// }

 }
