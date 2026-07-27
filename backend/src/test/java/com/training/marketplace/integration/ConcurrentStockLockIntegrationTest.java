package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.entity.Category;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.StockLevel;
import com.training.marketplace.entity.Warehouse;
import com.training.marketplace.repository.CategoryRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.StockLevelRepository;
import com.training.marketplace.repository.WarehouseRepository;
import com.training.marketplace.service.InventoryFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No-oversell test at the authoritative point (InventoryFacade.reserve → pessimistic lock on
 * stock_levels). 10 threads each reserve 1 unit of a variant that has quantity=10 in one warehouse.
 * Exactly 10 must succeed, none oversell, and available (quantity - reserved) must land at 0.
 */
class ConcurrentStockLockIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StockLevelRepository stockLevelRepository;
    @Autowired private InventoryFacade inventoryFacade;

    @Test
    void testConcurrentReserve_PreventsOverselling() throws InterruptedException {
        Category category = categoryRepository.save(Category.builder()
                .name("Concurrent Test Category").code("CONC").slug("concurrent-test-cat").build());

        Product product = productRepository.save(Product.builder()
                .name("Concurrent Lock Item").slug("concurrent-lock-item")
                .categoryId(category.getId()).unit("PCS").active(true).build());

        ProductVariant variant = variantRepository.save(ProductVariant.builder()
                .productId(product.getId()).sku("CONC-SKU-1").variantName("Default")
                .price(new BigDecimal("99.99")).active(true).build());

        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
                .name("Concurrent Warehouse").code("WH-CONC").active(true).build());

        stockLevelRepository.save(StockLevel.builder()
                .variantId(variant.getId()).warehouseId(warehouse.getId())
                .quantity(10).reservedQuantity(0).version(0L).build());

        final Long variantId = variant.getId();
        final Long warehouseId = warehouse.getId();
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    inventoryFacade.reserve(warehouseId, Map.of(variantId, 1));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        executorService.shutdown();

        StockLevel updated = stockLevelRepository
                .findByWarehouseIdAndVariantId(warehouseId, variantId).orElseThrow();
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(0);
        assertThat(updated.getQuantity() - updated.getReservedQuantity()).isEqualTo(0);
    }
}
