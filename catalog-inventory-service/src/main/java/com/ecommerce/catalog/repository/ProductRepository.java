package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.enums.ApprovalStatus;
import com.ecommerce.catalog.entity.enums.ProductStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySku(String sku);

    Page<Product> findByApprovalStatusAndStatus(ApprovalStatus approvalStatus, ProductStatus status,
            Pageable pageable);

    Page<Product> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    Page<Product> findBySupplierId(UUID supplierId, Pageable pageable);
}
