package com.training.marketplace.repository.projection;

import java.math.BigDecimal;

/**
 * Active SKU price range aggregated at product/SPU level.
 */
public interface ProductPriceRangeProjection {

    Long getProductId();

    BigDecimal getMinPrice();

    BigDecimal getMaxPrice();
}
