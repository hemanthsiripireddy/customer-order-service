package com.ecommerce.customerorder.config;

import com.ecommerce.customerorder.external.inventory.InventoryServiceClient;
import com.ecommerce.customerorder.external.inventory.StubInventoryServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InventoryServiceClient inventoryServiceClient() {
        return new StubInventoryServiceClient();
    }
}
