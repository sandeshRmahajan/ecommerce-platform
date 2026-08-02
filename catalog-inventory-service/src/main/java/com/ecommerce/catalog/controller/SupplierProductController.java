package com.ecommerce.catalog.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.catalog.dto.request.CreateProductRequest;
import com.ecommerce.catalog.dto.request.UpdateProductRequest;
import com.ecommerce.catalog.dto.response.ProductResponse;
import com.ecommerce.catalog.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/supplier")
public class SupplierProductController {

    private final ProductService productService;

    public SupplierProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@AuthenticationPrincipal UUID supplierId,
                                         @RequestBody @Valid CreateProductRequest request) {
        return productService.createProduct(supplierId, request);
    }

    @PatchMapping("/products/{id}")
    public ProductResponse updateProduct(@AuthenticationPrincipal UUID supplierId,
                                         @PathVariable UUID id,
                                         @RequestBody @Valid UpdateProductRequest request) {
        return productService.updateProduct(supplierId, id, request);
    }

    @GetMapping("/products")
    public Page<ProductResponse> listSupplierProducts(@AuthenticationPrincipal UUID supplierId,
                                                      @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return productService.listSupplierProducts(supplierId, pageable);
    }
}
