package com.ecommerce.catalog.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.catalog.dto.response.AvailabilityResponse;
import com.ecommerce.catalog.dto.response.CategoryResponse;
import com.ecommerce.catalog.dto.response.ProductResponse;
import com.ecommerce.catalog.service.CategoryService;
import com.ecommerce.catalog.service.ProductService;

@RestController
@RequestMapping("/api/v1")
public class PublicProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public PublicProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/products")
    public Page<ProductResponse> listPublicProducts(@org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return productService.listPublicProducts(pageable);
    }

    @GetMapping("/products/{id}/availability")
    public AvailabilityResponse checkAvailability(@PathVariable UUID id,
                                                 @RequestParam(defaultValue = "1") int qty) {
        return productService.checkAvailability(id, qty);
    }

    @GetMapping("/categories")
    public java.util.List<CategoryResponse> listTopLevelCategories() {
        return categoryService.listTopLevelCategories();
    }
}
