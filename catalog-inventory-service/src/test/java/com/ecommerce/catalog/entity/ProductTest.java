package com.ecommerce.catalog.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void updateDetailsOnlyOverwritesProvidedFields() {
        Category category = new Category("Electronics", null);
        Product product = new Product(
                "SKU-1",
                "Old Name",
                "Old Description",
                category,
                UUID.randomUUID(),
                1000L,
                "USD");

        product.updateDetails("New Name", null, 2500L);

        assertEquals("New Name", product.getName());
        assertEquals("Old Description", product.getDescription());
        assertEquals(2500L, product.getPriceCents());
    }

    @Test
    void updateDetailsLeavesExistingValuesWhenInputIsBlankOrNull() {
        Category category = new Category("Electronics", null);
        Product product = new Product(
                "SKU-2",
                "Old Name",
                "Old Description",
                category,
                UUID.randomUUID(),
                1000L,
                "USD");

        product.updateDetails("   ", null, null);

        assertEquals("Old Name", product.getName());
        assertEquals("Old Description", product.getDescription());
        assertEquals(1000L, product.getPriceCents());
        assertNull(product.getRejectionReason());
    }
}
