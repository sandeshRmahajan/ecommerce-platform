package com.ecommerce.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CatalogInventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogInventoryServiceApplication.class, args);
	}

}
