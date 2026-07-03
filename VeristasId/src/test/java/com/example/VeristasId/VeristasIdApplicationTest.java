package com.example.VeristasId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        // Disable blockchain so test doesn't need Ganache running
        "blockchain.wallet.private.key=",
        // Use a fake OPA URL — OPA not needed for context load test
        "opa.url=http://localhost:9999/fake"
})
class VeristasIdApplicationTest {

    @Test
    void contextLoads() {
        // This test has NO assertions.
        // If the Spring context fails to start for ANY reason:
        //   - Missing @Bean
        //   - Bad @Value injection
        //   - DB connection failure
        //   - Circular dependency between beans
        // ...this test FAILS. It's your canary in the coal mine.
    }
}
