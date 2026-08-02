package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.request.CreateCategoryRequest;
import com.ecommerce.catalog.dto.response.CategoryResponse;
import com.ecommerce.catalog.entity.Category;
import com.ecommerce.catalog.exception.CategoryNotFoundException;
import com.ecommerce.catalog.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.parentId()));
        }

        Category category = new Category(request.name(), parent);
        Category savedCategory = categoryRepository.save(category);
        return toCategoryResponse(savedCategory);
    }

    // @Transactional(readOnly = true) is used here (rather than plain @Transactional) as a hint to Hibernate that no writes will occur in this method, which allows some database/driver-level optimizations and also documents intent clearly for anyone reading the method signature.
    @Transactional(readOnly = true)
    public List<CategoryResponse> listTopLevelCategories() {
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    private CategoryResponse toCategoryResponse(Category category) {
        // This triggers a lazy-load of the parent Category if one exists (since Category.parent is FetchType.LAZY) — this is safe here specifically because we are still inside the @Transactional method's active Hibernate session when this mapping happens, so the lazy proxy can resolve successfully; if this mapping were done outside a transaction it would throw LazyInitializationException.
        Category parent = category.getParent();
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                parent == null ? null : parent.getId(),
                parent == null ? null : parent.getName()
        );
    }
}
