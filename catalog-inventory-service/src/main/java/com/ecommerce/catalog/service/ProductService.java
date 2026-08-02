package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.request.CreateProductRequest;
import com.ecommerce.catalog.dto.request.RejectProductRequest;
import com.ecommerce.catalog.dto.request.UpdateProductRequest;
import com.ecommerce.catalog.dto.response.AvailabilityResponse;
import com.ecommerce.catalog.dto.response.ProductResponse;
import com.ecommerce.catalog.entity.Inventory;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.enums.ApprovalStatus;
import com.ecommerce.catalog.entity.enums.ProductStatus;
import com.ecommerce.catalog.exception.CategoryNotFoundException;
import com.ecommerce.catalog.exception.ProductAccessDeniedException;
import com.ecommerce.catalog.exception.ProductNotFoundException;
import com.ecommerce.catalog.exception.SkuAlreadyExistsException;
import com.ecommerce.catalog.repository.CategoryRepository;
import com.ecommerce.catalog.repository.InventoryRepository;
import com.ecommerce.catalog.repository.ProductRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
            InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public ProductResponse createProduct(UUID supplierId, CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new SkuAlreadyExistsException(request.sku());
        }

        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        Product product = new Product(
                request.sku(),
                request.name(),
                request.description(),
                category,
                supplierId,
                request.priceCents(),
                request.currency());

        // saveAndFlush is required here (not plain save) because @CreationTimestamp on
        // Product.createdAt is only populated by Hibernate at flush time, which is normally
        // deferred until transaction commit; since this method immediately maps the saved product
        // to a response DTO via toProductResponse, we must force the flush now so createdAt is
        // populated before it's read, otherwise the API response would incorrectly show createdAt
        // as null even though the database row has the correct value (same bug and fix as
        // AuthService.register() in auth-service).
        Product savedProduct = productRepository.saveAndFlush(product);

        // These two saves happen inside the same @Transactional method deliberately: a Product should
        // never exist without a corresponding Inventory row, so if the Inventory save somehow failed,
        // the whole transaction (including the Product save) rolls back rather than leaving an
        // inconsistent half-created product.
        inventoryRepository.save(new Inventory(savedProduct.getId(), request.initialQty()));

        return toProductResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(UUID supplierId, UUID productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // This ownership check belongs in the service layer rather than only relying on a route-level role check,
        // because "is this supplier allowed to edit ANY product" and "is this supplier allowed to edit THIS SPECIFIC product"
        // are two different authorization questions.
        if (!product.isOwnedBy(supplierId)) {
            throw new ProductAccessDeniedException(productId);
        }

        // The Product entity still lacks dedicated update setters, so this service uses a small
        // domain method to support partial updates until Product gets a fuller updateDetails API.
        product.updateDetails(request.name(), request.description(), request.priceCents());

        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listSupplierProducts(UUID supplierId, Pageable pageable) {
        return productRepository.findBySupplierId(supplierId, pageable).map(this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listPendingProducts(Pageable pageable) {
        return productRepository.findByApprovalStatus(ApprovalStatus.PENDING, pageable).map(this::toProductResponse);
    }

    @Transactional
    public ProductResponse approveProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.approve();

        // In a later iteration, this method would also publish a catalog.product.created Kafka event
        // via the outbox pattern once Kafka is wired into this service — not implemented yet.
        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse rejectProduct(UUID productId, RejectProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.reject(request.reason());
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listPublicProducts(Pageable pageable) {
        // This is the only product listing method reachable by unauthenticated/public API consumers,
        // which is why it is hardcoded to only ever return APPROVED + ACTIVE products regardless of
        // what filters a caller might try to pass.
        return productRepository.findByApprovalStatusAndStatus(ApprovalStatus.APPROVED, ProductStatus.ACTIVE, pageable)
                .map(this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(UUID productId, int qty) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return new AvailabilityResponse(inventory.hasAvailableStock(qty), inventory.getAvailableQty());
    }

    private ProductResponse toProductResponse(Product product) {
        // This triggers a lazy-load of the category association for the response mapping, which is
        // safe here because the method runs inside an active @Transactional context.
        var category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                category.getId(),
                category.getName(),
                product.getSupplierId(),
                product.getPriceCents(),
                product.getCurrency(),
                product.getStatus().name(),
                product.getApprovalStatus().name(),
                product.getRejectionReason(),
                product.getCreatedAt());
    }
}
