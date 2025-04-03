package com.fptu.sep490.sample.service;

import com.fptu.sep490.sample.viewmodel.ProductGetVm;
import com.fptu.sep490.sample.viewmodel.ProductPostVm;
import org.springframework.data.domain.Page;

public interface ProductService {

    ProductGetVm getProductById(Long id);

    ProductGetVm updateProduct(Long id, ProductPostVm product);

    void deleteProduct(Long id);

    ProductGetVm createProduct(ProductPostVm product);

    Page<ProductGetVm> findAll(int page, int size);
}
