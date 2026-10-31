package com.ecommerce.ecommerce_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")   // Uses application-test.properties
class EcommerceBackendApplicationTests {

	@Test
	void contextLoads() {
		// Just checks if the application context loads successfully
	}
}