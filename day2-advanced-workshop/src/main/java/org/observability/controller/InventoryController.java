package org.observability.controller;

import org.observability.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * GET /api/inventory/{itemId}
     * Returns inventory count. Uses itemId path variable for custom span attribute.
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<?> getInventory(@PathVariable String itemId) {
        log.info("Received inventory request for itemId={}", itemId);
        try {
            int quantity = inventoryService.calculateInventory(itemId);
            return ResponseEntity.ok(Map.of(
                    "itemId", itemId,
                    "quantity", quantity,
                    "status", "available"
            ));
        } catch (RuntimeException e) {
            log.error("Failed to retrieve inventory for itemId={}", itemId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "itemId", itemId,
                            "error", e.getMessage(),
                            "status", "error"
                    ));
        }
    }

    /**
     * GET /api/inventory/{itemId}/validate?quantity=10
     * Validates if enough stock is available.
     */
    @GetMapping("/{itemId}/validate")
    public ResponseEntity<?> validateStock(
            @PathVariable String itemId,
            @RequestParam(defaultValue = "1") int quantity) {
        log.info("Received stock validation request for itemId={}, quantity={}", itemId, quantity);
        try {
            boolean inStock = inventoryService.validateStock(itemId, quantity);
            return ResponseEntity.ok(Map.of(
                    "itemId", itemId,
                    "requestedQuantity", quantity,
                    "inStock", inStock
            ));
        } catch (RuntimeException e) {
            log.error("Failed to validate stock for itemId={}", itemId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "itemId", itemId,
                            "error", e.getMessage(),
                            "status", "error"
                    ));
        }
    }

    /**
     * GET /api/inventory/health
     * Simple health-check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "inventory-service"));
    }
}