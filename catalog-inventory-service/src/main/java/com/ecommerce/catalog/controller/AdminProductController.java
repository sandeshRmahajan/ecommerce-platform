package com.ecommerce.catalog.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.catalog.dto.request.RejectProductRequest;
import com.ecommerce.catalog.dto.response.ProductResponse;
import com.ecommerce.catalog.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products/pending")
    public Page<ProductResponse> listPendingProducts(@org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return productService.listPendingProducts(pageable);
    }

    @PostMapping("/products/{id}/approve")
    public ProductResponse approveProduct(@PathVariable UUID id) {
        return productService.approveProduct(id);
    }

    @PostMapping("/products/{id}/reject")
    public ProductResponse rejectProduct(@PathVariable UUID id,
                                         @RequestBody @Valid RejectProductRequest request) {
        return productService.rejectProduct(id, request);
    }
}
