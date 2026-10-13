package com.marketflow.api.infrastructure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a mutating endpoint that requires an {@code Idempotency-Key} header. The first response is stored in
 * Redis and replayed for retries with the same key (see {@link IdempotencyInterceptor}).
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    boolean required() default true;
}
