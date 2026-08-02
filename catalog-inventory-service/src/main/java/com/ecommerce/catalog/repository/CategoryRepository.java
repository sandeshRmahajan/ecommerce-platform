package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.Category;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByParentIsNull();
}
