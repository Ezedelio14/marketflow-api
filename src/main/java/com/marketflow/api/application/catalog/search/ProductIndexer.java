package com.marketflow.api.application.catalog.search;

import java.util.UUID;

/**
 * Output port that keeps the search index in sync with the relational catalog.
 */
public interface ProductIndexer {

    /** Rebuilds the document from the database; removes it when the product is not published. */
    void sync(UUID productId);

    void remove(UUID productId);
}
