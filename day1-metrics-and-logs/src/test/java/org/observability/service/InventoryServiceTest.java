package org.observability.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InventoryServiceTest {

    private MeterRegistry meterRegistry;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        inventoryService = new InventoryService(meterRegistry);
    }

    @Test
    @DisplayName("Success counter increments on successful inventory calculation")
    void successCounter_incrementsOnSuccess() {
        int successCount = 0;
        for (int i = 0; i < 20; i++) {
            try {
                inventoryService.calculateInventory("test-item-" + i);
                successCount++;
            } catch (RuntimeException ignored) {}
        }

        Counter counter = meterRegistry.find("inventory.requests.total")
                .tag("status", "success")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(successCount);
    }

    @Test
    @DisplayName("Error counter increments when exception is thrown")
    void errorCounter_incrementsOnError() {
        // Run enough times to guarantee at least one error (~20% rate)
        int errorCount = 0;
        for (int i = 0; i < 20; i++) {
            try {
                inventoryService.calculateInventory("test-item-" + i);
            } catch (RuntimeException e) {
                errorCount++;
            }
        }

        Counter counter = meterRegistry.find("inventory.requests.total")
                .tag("status", "error")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(errorCount);
    }

    @Test
    @DisplayName("Counter has correct metric name and tags registered")
    void counter_hasCorrectNameAndTags() {
        Counter successCounter = meterRegistry.find("inventory.requests.total")
                .tag("status", "success")
                .tag("service", "inventory-service")
                .counter();

        Counter errorCounter = meterRegistry.find("inventory.requests.total")
                .tag("status", "error")
                .tag("service", "inventory-service")
                .counter();

        assertThat(successCounter).isNotNull();
        assertThat(errorCounter).isNotNull();
    }

    @Test
    @DisplayName("Success and error counters together equal total requests")
    void counters_successPlusErrorEqualsTotalRequests() {
        int total = 30;
        for (int i = 0; i < total; i++) {
            try {
                inventoryService.calculateInventory("item-" + i);
            } catch (RuntimeException ignored) {}
        }

        double successes = meterRegistry.find("inventory.requests.total")
                .tag("status", "success").counter().count();
        double errors = meterRegistry.find("inventory.requests.total")
                .tag("status", "error").counter().count();

        assertThat(successes + errors).isEqualTo(total);
    }

    @Test
    @DisplayName("calculateInventory returns positive quantity on success")
    void calculateInventory_returnsPositiveQuantity() {
        // Try until we get a success
        for (int i = 0; i < 50; i++) {
            try {
                int quantity = inventoryService.calculateInventory("item-success");
                assertThat(quantity).isGreaterThan(0).isLessThanOrEqualTo(100);
                return;
            } catch (RuntimeException ignored) {}
        }
    }
}