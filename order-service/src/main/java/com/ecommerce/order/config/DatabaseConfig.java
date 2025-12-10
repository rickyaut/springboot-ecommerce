package com.ecommerce.order.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import javax.sql.DataSource;

@Configuration
@org.springframework.context.annotation.Profile("!test")
public class DatabaseConfig {

    @Autowired(required = false)
    private VaultTemplate vaultTemplate;

    @Value("${spring.datasource.url}")
    private String url;

    @Bean
    @Primary
    public DataSource dataSource() {
        String username = getFromVaultOrDefault("spring.datasource.username", "order");
        String password = getFromVaultOrDefault("spring.datasource.password", "password");
        
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    private String getFromVaultOrDefault(String key, String defaultValue) {
        if (vaultTemplate == null) {
            return defaultValue;
        }
        
        try {
            VaultResponse response = vaultTemplate.read("secret/data/order-service");
            if (response != null && response.getData() != null) {
                Object data = response.getData().get("data");
                if (data instanceof java.util.Map) {
                    String value = (String) ((java.util.Map<?, ?>) data).get(key);
                    return value != null ? value : defaultValue;
                }
            }
        } catch (Exception e) {
            System.out.println("Vault unavailable, using default for " + key);
        }
        return defaultValue;
    }
}