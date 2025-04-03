package com.fptu.sep490.sample.controller;

import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.sample.service.ProductService;
import com.fptu.sep490.sample.viewmodel.ProductGetVm;
import com.fptu.sep490.sample.viewmodel.ProductPostVm;
import jakarta.annotation.security.PermitAll;
import jakarta.websocket.server.PathParam;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ProductController {

    ProductService productService;

    @GetMapping
    @PermitAll
    public ResponseEntity<BaseResponse<List<ProductGetVm>>> getAllProducts(
            @RequestParam(value = "page", defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size) {
        var productGetVmPage = productService.findAll(page, size);
        var pagination = new Pagination(
                productGetVmPage.getNumber() + 1,
                productGetVmPage.getTotalPages(),
                productGetVmPage.getSize(),
                (int) productGetVmPage.getTotalElements(),
                productGetVmPage.hasNext(),
                productGetVmPage.hasPrevious()
        );
        return ResponseEntity.ok(BaseResponse.<List<ProductGetVm>>builder()
                .data(productGetVmPage.getContent())
                .pagination(pagination)
                .build());
    }

    @PostMapping
    @PermitAll
    public ResponseEntity<BaseResponse<ProductGetVm>> addProduct(@RequestBody ProductPostVm product) {
        var productGetVm = productService.createProduct(product);
        return ResponseEntity.ok(BaseResponse.<ProductGetVm>builder()
                .data(productGetVm)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ProductGetVm>> getProduct(@PathVariable Long id) {
        var productGetVm = productService.getProductById(id);
        return ResponseEntity.ok(BaseResponse.<ProductGetVm>builder()
                .data(productGetVm)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<ProductGetVm>> updateProduct(@PathVariable("id") Long id,
                                                                    @RequestBody ProductPostVm product) {
        var productGetVm = productService.updateProduct(id, product);
        return ResponseEntity.ok(BaseResponse.<ProductGetVm>builder()
                .data(productGetVm)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> testAdmin() {
        return ResponseEntity.ok("Hello admin");
    }

    @GetMapping("/test/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> testUser() {
        return ResponseEntity.ok("Hello user");
    }

}
