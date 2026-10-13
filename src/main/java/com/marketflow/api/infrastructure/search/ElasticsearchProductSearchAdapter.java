package com.marketflow.api.infrastructure.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.marketflow.api.application.catalog.search.ProductSearchCriteria;
import com.marketflow.api.application.catalog.search.ProductSearchHit;
import com.marketflow.api.application.catalog.search.ProductSearchPort;
import com.marketflow.api.application.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Storefront search on Elasticsearch: multi-field full-text with fuzziness and boosting, dynamic filters
 * (category, brand, seller, price range and variant attributes through nested queries, stock), pagination and
 * sorting by relevance, price or recency.
 */
@Component
@RequiredArgsConstructor
public class ElasticsearchProductSearchAdapter implements ProductSearchPort {

    private static final List<String> SEARCH_FIELDS = List.of("name^4", "brandName^2", "categoryName^2", "variantText^2", "description");

    private final ElasticsearchOperations operations;

    @Override
    public PageResponse<ProductSearchHit> search(ProductSearchCriteria criteria) {
        int page = criteria.pageOrDefault();
        int size = criteria.sizeOrDefault();

        NativeQuery query = NativeQuery.builder()
                .withQuery(buildQuery(criteria))
                .withPageable(PageRequest.of(page, size, sort(criteria)))
                .withTrackScores(true)
                .withTrackTotalHits(true)
                .build();

        SearchHits<ProductDocument> hits = operations.search(query, ProductDocument.class);
        List<ProductSearchHit> content = hits.getSearchHits().stream().map(ElasticsearchProductSearchAdapter::toHit).toList();
        return PageResponse.of(content, page, size, hits.getTotalHits());
    }

    private static Query buildQuery(ProductSearchCriteria c) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.filter(f -> f.term(t -> t.field("status").value("PUBLISHED")));

        if (c.q() != null && !c.q().isBlank()) {
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(c.q().trim())
                    .fields(SEARCH_FIELDS)
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                    .operator(Operator.And)));
        } else {
            bool.must(m -> m.matchAll(ma -> ma));
        }
        if (c.categoryIds() != null && !c.categoryIds().isEmpty()) {
            bool.filter(f -> f.terms(t -> t.field("categoryId").terms(v -> v.value(fieldValues(c.categoryIds())))));
        }
        if (c.brandIds() != null && !c.brandIds().isEmpty()) {
            bool.filter(f -> f.terms(t -> t.field("brandId").terms(v -> v.value(fieldValues(c.brandIds())))));
        }
        if (c.sellerId() != null) {
            bool.filter(f -> f.term(t -> t.field("sellerId").value(c.sellerId().toString())));
        }
        if (Boolean.TRUE.equals(c.inStock())) {
            bool.filter(f -> f.term(t -> t.field("inStock").value(true)));
        }
        if (c.minPrice() != null || c.maxPrice() != null) {
            // At least one variant priced within the range
            bool.filter(f -> f.nested(n -> n.path("variants").query(nq -> nq.range(r -> r.number(num -> {
                num.field("variants.price");
                if (c.minPrice() != null) num.gte(toDouble(c.minPrice()));
                if (c.maxPrice() != null) num.lte(toDouble(c.maxPrice()));
                return num;
            })))));
        }
        if (c.variant() != null && !c.variant().isBlank()) {
            bool.filter(f -> f.nested(n -> n.path("variants").query(nq -> nq.multiMatch(mm -> mm
                    .query(c.variant().trim())
                    .fields("variants.name", "variants.sku")
                    .operator(Operator.And)))));
        }
        return Query.of(q -> q.bool(bool.build()));
    }

    private static Sort sort(ProductSearchCriteria c) {
        return switch (c.sortOrDefault()) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "minPrice");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "minPrice");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAtEpochMs");
            case RELEVANCE -> Sort.unsorted(); // _score
        };
    }

    private static List<FieldValue> fieldValues(List<UUID> ids) {
        return ids.stream().map(id -> FieldValue.of(id.toString())).toList();
    }

    private static Double toDouble(BigDecimal value) {
        return value.doubleValue();
    }

    private static ProductSearchHit toHit(SearchHit<ProductDocument> hit) {
        ProductDocument d = hit.getContent();
        return new ProductSearchHit(UUID.fromString(d.getId()), d.getSlug(), d.getName(), d.getDescription(),
                d.getBrandName(), d.getCategoryName(), d.getSellerName(),
                d.getMinPrice() == null ? null : BigDecimal.valueOf(d.getMinPrice()),
                d.getMaxPrice() == null ? null : BigDecimal.valueOf(d.getMaxPrice()),
                d.isInStock(), d.getImageUrl(), Float.isNaN(hit.getScore()) ? null : hit.getScore());
    }
}
