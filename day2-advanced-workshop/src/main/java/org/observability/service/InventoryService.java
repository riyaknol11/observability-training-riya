package org.observability.service;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final Tracer tracer;
    private final Random random = new Random();

    /**
     * Calculates inventory for the given item.
     * - Creates a custom nested span with a custom attribute item.id
     * - Simulates random delay (Day 1 requirement)
     * - Randomly throws a RuntimeException to simulate HTTP 500 (Day 1 requirement)
     */
    public int calculateInventory(String itemId) {
        log.info("Starting inventory calculation for itemId={}", itemId);

        // ── Custom Span (Day 2 Requirement) ──────────────────────────────────
        Span customSpan = tracer.nextSpan()
                .name("calculateInventory")
                .tag("item.id", itemId)   // Custom attribute with path variable
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(customSpan.start())) {

            // ── Random Delay (Day 1 Requirement) ─────────────────────────────
            simulateDelay();

            // ── Random Error Simulation (Day 1 Requirement) ──────────────────
            simulateError(itemId);

            int quantity = random.nextInt(100) + 1;
            log.info("Inventory calculated for itemId={}, quantity={}", itemId, quantity);
            return quantity;

        } catch (Exception e) {
            // Mark span as error so Tempo shows it as red
            customSpan.tag("error", "true");
            customSpan.error(e);
            log.error("Error calculating inventory for itemId={}: {}", itemId, e.getMessage(), e);
            throw e;
        } finally {
            customSpan.end();
        }
    }

    /**
     * Validates stock availability for the given item.
     */
    public boolean validateStock(String itemId, int requestedQuantity) {
        log.info("Validating stock for itemId={}, requestedQuantity={}", itemId, requestedQuantity);

        Span stockSpan = tracer.nextSpan()
                .name("validateStock")
                .tag("item.id", itemId)
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(stockSpan.start())) {
            int available = calculateInventory(itemId);
            boolean inStock = available >= requestedQuantity;
            log.info("Stock validation result for itemId={}: inStock={}", itemId, inStock);
            stockSpan.tag("stock.available", String.valueOf(available));
            stockSpan.tag("stock.sufficient", String.valueOf(inStock));
            return inStock;
        } catch (Exception e) {
            stockSpan.tag("error", "true");
            stockSpan.error(e);
            throw e;
        } finally {
            stockSpan.end();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void simulateDelay() {
        try {
            // Random delay between 50ms and 500ms
            long delay = 50 + random.nextInt(450);
            log.debug("Simulating processing delay of {}ms", delay);
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateError(String itemId) {
        // ~20% chance of error — triggers HTTP 500 in controller
        if (random.nextInt(5) == 0) {
            throw new RuntimeException(
                    "Simulated inventory service failure for itemId=" + itemId
                            + ". Upstream data source unavailable.");
        }
    }
}