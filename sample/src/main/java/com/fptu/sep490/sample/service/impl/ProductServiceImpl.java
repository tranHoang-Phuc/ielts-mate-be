package com.fptu.sep490.sample.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.NotFoundException;
import com.fptu.sep490.sample.constants.Constants;
import com.fptu.sep490.sample.mapper.ProductMapper;
import com.fptu.sep490.sample.repository.ProductRepository;
import com.fptu.sep490.sample.service.ProductService;
import com.fptu.sep490.sample.viewmodel.ProductGetVm;
import com.fptu.sep490.sample.viewmodel.ProductPostVm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    ProductRepository productRepository;
    ProductMapper productMapper;
    @Override
    public ProductGetVm getProductById(Long id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(Constants.ErrorCode.PRODUCT_NOT_FOUND, id));
        return productMapper.toProductGetVm(product);
    }

    @Override
    public ProductGetVm updateProduct(Long id, ProductPostVm product) {
        var existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(Constants.ErrorCode.PRODUCT_NOT_FOUND, id));
        existingProduct.setName(product.name());
        existingProduct.setShortDescription(product.shortDescription());

        productRepository.save(existingProduct);
        return productMapper.toProductGetVm(existingProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        var existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(Constants.ErrorCode.PRODUCT_NOT_FOUND, id));
        productRepository.delete(existingProduct);
    }

    @Override
    public ProductGetVm createProduct(ProductPostVm product) {
        var newProduct = productMapper.toModel(product);
        var productCreated =   productRepository.save(newProduct);
        return productMapper.toProductGetVm(productCreated);
    }

    @Override
    public Page<ProductGetVm> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAllBy(pageable)
                .map(productMapper::toProductGetVm);
    }
}
