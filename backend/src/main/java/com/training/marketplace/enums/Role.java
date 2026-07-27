package com.training.marketplace.enums;

/**
 * Merged role model for the Marketplace.
 * CUSTOMER — storefront shopper (was OrderFlow's USER).
 * STAFF / MANAGER — warehouse back-office (from StockPulse).
 * ADMIN — full access.
 */
public enum Role {
    ADMIN,
    MANAGER,
    STAFF,
    CUSTOMER
}
