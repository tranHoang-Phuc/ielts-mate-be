package com.fptu.sep490.sample.controller;

import com.fptu.sep490.sample.service.ProductService;
import com.fptu.sep490.sample.viewmodel.ProductGetVm;
import com.fptu.sep490.sample.viewmodel.ProductPostVm;
import jakarta.annotation.security.PermitAll;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ProductController {

    ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProductGetVm> addProduct(@RequestBody ProductPostVm product) {
        var productGetVm = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(productGetVm);
    }

    @GetMapping("/test")
    @PermitAll
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Test successful");
    }

    @GetMapping("/test/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> testUser() {
        return ResponseEntity.ok("Test user successful");
    }

    @GetMapping("/test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> testAdmin() {
        return ResponseEntity.ok("Test admin successful");
    }
}
