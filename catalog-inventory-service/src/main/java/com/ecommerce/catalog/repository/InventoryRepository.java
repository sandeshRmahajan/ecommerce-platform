package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.Inventory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    // JpaRepository already gives us findById(productId), which is sufficient because Inventory's ID is the productId.
}
