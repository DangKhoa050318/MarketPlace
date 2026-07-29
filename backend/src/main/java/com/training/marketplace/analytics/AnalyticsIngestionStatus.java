package com.training.marketplace.analytics;

/**
 * Status returned after a raw analytics event has been durably stored.
 */
public enum AnalyticsIngestionStatus {
    ACCEPTED,
    DUPLICATE_IGNORED
}
