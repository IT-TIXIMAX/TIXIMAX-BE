package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.ProductType;
import com.tiximax.txm.Exception.BadRequestException;
import com.tiximax.txm.Exception.NotFoundException;
import com.tiximax.txm.Repository.ProductTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.BadRequest;

import java.util.List;
import java.util.Optional;

@Service

public class ProductTypeService {

    @Autowired
    private ProductTypeRepository productTypeRepository;

    public ProductType createProductType(ProductType productType) {
        if (productTypeRepository.findByProductTypeName(productType.getProductTypeName()).isPresent()) {
            throw new BadRequestException("Loại sản phẩm đã tồn tại!");
        }
        return productTypeRepository.save(productType);
    }

    public List<ProductType> getAllProductTypes() {
        return productTypeRepository.findAll();
    }

    public Optional<ProductType> getProductTypeById(Long productTypeId) {
        return productTypeRepository.findById(productTypeId);
    }

    public ProductType updateProductType(Long productTypeId, ProductType productTypeDetails) {
        ProductType productType = productTypeRepository.findById(productTypeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại sản phẩm với ID: " + productTypeId));

        productType.setProductTypeName(productTypeDetails.getProductTypeName());
        productType.setFee(productTypeDetails.isFee());
        return productTypeRepository.save(productType);
    }

    public void deleteProductType(Long productTypeId) {
        ProductType productType = productTypeRepository.findById(productTypeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại sản phẩm với ID: " + productTypeId));
        productTypeRepository.delete(productType);
    }

}
