package com.marketflow.api.application.inventory;

/**
 * How inventory rows are protected while reserving stock.
 * <ul>
 *   <li>{@link #OPTIMISTIC}: plain read; conflicts surface on flush through {@code @Version} (cheap, no blocking).</li>
 *   <li>{@link #PESSIMISTIC}: {@code SELECT ... FOR UPDATE} — used as fallback under contention (hot products).</li>
 * </ul>
 */
public enum LockStrategy {
    OPTIMISTIC, PESSIMISTIC
}
