package com.marketflow.api.infrastructure.messaging.consumer;

import com.marketflow.api.application.catalog.search.ProductIndexer;
import com.marketflow.api.application.event.Events.IndexOperation;
import com.marketflow.api.application.event.Events.InventoryReleasedEvent;
import com.marketflow.api.application.event.Events.InventoryReservedEvent;
import com.marketflow.api.application.event.Events.ProductIndexedEvent;
import com.marketflow.api.application.event.Events.StockLine;
import com.marketflow.api.application.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Keeps Elasticsearch eventually consistent: catalog changes ({@code product.indexed}) and stock movements
 * ({@code inventory.*}, which change the {@code inStock} flag) trigger a re-sync of the affected products.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexConsumer {

    private static final String GROUP = "marketflow-search-indexer";

    private final ProductIndexer indexer;

    @KafkaListener(topics = KafkaTopics.PRODUCT_INDEXED, groupId = GROUP)
    public void onProductIndexed(ProductIndexedEvent event) {
        if (event.operation() == IndexOperation.DELETE) {
            indexer.remove(event.productId());
        } else {
            indexer.sync(event.productId());
        }
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVED, groupId = GROUP)
    public void onInventoryReserved(InventoryReservedEvent event) {
        resync(event.lines());
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RELEASED, groupId = GROUP)
    public void onInventoryReleased(InventoryReleasedEvent event) {
        resync(event.lines());
    }

    private void resync(Collection<StockLine> lines) {
        lines.stream().map(StockLine::productId).distinct().forEach(indexer::sync);
    }
}
