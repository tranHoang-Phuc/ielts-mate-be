package com.fptu.sep490.sample.controller;

import com.fptu.sep490.sample.service.ProductService;
import com.fptu.sep490.sample.viewmodel.ProductGetVm;
import com.fptu.sep490.sample.viewmodel.ProductPostVm;
import jakarta.annotation.security.PermitAll;
import jakarta.websocket.server.PathParam;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ProductController {

    ProductService productService;

    @PostMapping
    @PermitAll
    public ResponseEntity<ProductGetVm> addProduct(@RequestBody ProductPostVm product) {
        var productGetVm = productService.createProduct(product);
        URI location = URI.create("/api/v1/products/" + productGetVm.id());
        return ResponseEntity.created(location).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductGetVm> getProduct(@PathVariable Long id) {
        var productGetVm = productService.getProductById(id);
        return ResponseEntity.ok(productGetVm);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductGetVm> updateProduct(@PathVariable("id")Long id, @RequestBody ProductPostVm product) {
        var productGetVm = productService.updateProduct(id, product);
        return ResponseEntity.ok(productGetVm);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
