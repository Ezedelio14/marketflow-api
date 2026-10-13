package com.marketflow.api.infrastructure.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexInitializer {

    private final ElasticsearchOperations operations;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexIfMissing() {
        try {
            IndexOperations index = operations.indexOps(ProductDocument.class);
            if (!index.exists()) {
                index.createWithMapping();
                log.info("Created Elasticsearch index 'products'");
            }
        } catch (RuntimeException e) {
            log.warn("Elasticsearch not reachable, index creation skipped: {}", e.getMessage());
        }
    }
}
