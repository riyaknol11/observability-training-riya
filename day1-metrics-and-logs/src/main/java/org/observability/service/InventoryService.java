package org.observability.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class InventoryService {

    private final Counter successCounter;
    private final Counter errorCounter;

    private final Random random = new Random();

    public InventoryService(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("inventory.requests.total")
                .description("Total number of inventory requests")
                .tag("service", "inventory-service")
                .tag("status", "success")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("inventory.requests.total")
                .description("Total number of inventory requests")
                .tag("service", "inventory-service")
                .tag("status", "error")
                .register(meterRegistry);
    }

    public int calculateInventory(String itemId) {
        log.info("Starting inventory calculation for itemId={}", itemId);

        try {
            simulateDelay();

            simulateError(itemId);

            int quantity = random.nextInt(100) + 1;
            log.info("Inventory calculated successfully for itemId={}, quantity={}", itemId, quantity);

            // Increment success counter
            successCounter.increment();
            return quantity;

        } catch (RuntimeException e) {
            log.error("Error calculating inventory for itemId={}: {}", itemId, e.getMessage(), e);

            // Increment error counter
            errorCounter.increment();
            throw e;
        }
    }

    public boolean validateStock(String itemId, int requestedQuantity) {
        log.info("Validating stock for itemId={}, requestedQuantity={}", itemId, requestedQuantity);
        int available = calculateInventory(itemId);
        boolean inStock = available >= requestedQuantity;
        log.info("Stock validation result for itemId={}: available={}, requested={}, inStock={}",
                itemId, available, requestedQuantity, inStock);
        return inStock;
    }

    private void simulateDelay() {
        try {
            long delay = 50 + random.nextInt(450);
            log.debug("Simulating processing delay of {}ms", delay);
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateError(String itemId) {
        if (random.nextInt(5) == 0) {
            throw new RuntimeException(
                    "Simulated inventory service failure for itemId=" + itemId
                            + ". Upstream data source unavailable.");
        }
    }
}
