package com.example.VeristasId.Config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.security.Security;

@Configuration
public class BouncyCastleConfig {
    @PostConstruct
    public void registerProvider() {
        // This line registers the 'BC' provider globally so your
        // service can use the secp256k1 curve.
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}