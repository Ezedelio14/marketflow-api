package com.marketflow.api.application.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

public final class SlugGenerator {

    private SlugGenerator() {
    }

    public static String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? UUID.randomUUID().toString().substring(0, 8) : slug;
    }

    /** Returns a slug not matched by {@code exists}, appending a short random suffix on collision. */
    public static String unique(String input, Predicate<String> exists) {
        String base = slugify(input);
        String candidate = base;
        while (exists.test(candidate)) {
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return candidate;
    }
}
