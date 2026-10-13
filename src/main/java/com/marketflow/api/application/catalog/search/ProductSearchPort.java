package com.marketflow.api.application.catalog.search;

import com.marketflow.api.application.common.PageResponse;

/**
 * Output port for full-text product search (implemented on Elasticsearch).
 */
public interface ProductSearchPort {

    PageResponse<ProductSearchHit> search(ProductSearchCriteria criteria);
}
