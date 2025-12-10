package com.ecommerce.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

@Configuration
public class VaultConfig {

    @Value("${vault.uri:http://localhost:8200}")
    private String vaultUri;

    @Value("${vault.token:myroot}")
    private String vaultToken;

    @Bean
    public VaultTemplate vaultTemplate() {
        try {
            VaultEndpoint endpoint = VaultEndpoint.from(java.net.URI.create(vaultUri));
            TokenAuthentication authentication = new TokenAuthentication(vaultToken);
            return new VaultTemplate(endpoint, authentication);
        } catch (Exception e) {
            return null;
        }
    }
}