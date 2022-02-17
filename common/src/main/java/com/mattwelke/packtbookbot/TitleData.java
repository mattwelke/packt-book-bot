package com.mattwelke.packtbookbot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The data for a title obtainable from the free learning URL. Used between actions, but the Java 17
 * runtime doesn't support serializing and deserializing java.util.Optional<T> and records, so we need
 * to manually serialize it to and deserialize it from strings and booleans.
 */
public record TitleData(
        String title,
        PublicationDate pubDate,
        Optional<String> productPageUrl,
        Authors authors) {
    /**
     * Creates a record from an OpenWhisk action params map.
     *
     * @param params Params map.
     * @return The record.
     */
    static TitleData of(Map<String, Object> params) throws IllegalArgumentException {
        // title
        if (!params.containsKey("title")) {
            throw new IllegalArgumentException("missing param \"title\"");
        }
        String title = (String) params.get("title");
        if (title.length() < 1) {
            throw new IllegalArgumentException("param \"title\" must be at least one character long");
        }

        // pub date
        if (!params.containsKey("pubDateMonth")) {
            throw new IllegalArgumentException("missing param \"pubDateMonth\"");
        }
        String pubDateMonth = (String) params.get("pubDateMonth");
        if (pubDateMonth.length() < 1) {
            throw new IllegalArgumentException("param \"pubDateMonth\" must be at least one character long");
        }
        if (!params.containsKey("pubDateYear")) {
            throw new IllegalArgumentException("missing param \"pubDateYear\"");
        }
        String pubDateYear = (String) params.get("pubDateYear");
        if (pubDateYear.length() < 1) {
            throw new IllegalArgumentException("param \"pubDateYear\" must be at least one character long");
        }

        // product page URL
        if (!params.containsKey("hasProductPageUrl")) {
            throw new IllegalArgumentException("missing param \"hasProductPageUrl\"");
        }
        boolean hasProductPageUrl = (boolean) params.get("hasProductPageUrl");
        String productPageUrl = null;
        if (hasProductPageUrl) {
            productPageUrl = (String) params.get("productPageUrl");
            if (productPageUrl.length() < 1) {
                throw new IllegalArgumentException("param \"productPageUrl\", if provided, must be at least one character long");
            }
        }

        // authors
        if (!params.containsKey("authorsNames")) {
            throw new IllegalArgumentException("missing param \"authorsNames\"");
        }
        // TODO: Investigate warning "Unchecked cast: 'java.lang.Object' to 'java.util.List<java.lang.String>'"
        List<String> authorsNames = (List<String>) params.get("authorsNames");
        if (authorsNames.size() < 1) {
            throw new IllegalArgumentException("param \"authorsNames\" needs at least one author");
        }
        for (String name : authorsNames) {
            if (name.length() < 1) {
                throw new IllegalArgumentException("each string in param \"authorsNames\" must be at least one character long");
            }
        }
        if (!params.containsKey("authorsMore")) {
            throw new IllegalArgumentException("missing param \"authorsMore\"");
        }
        boolean authorsMore = (boolean) params.get("authorsMore");

        return new TitleData(title, new PublicationDate(pubDateMonth, pubDateYear),
                Optional.ofNullable(productPageUrl), new Authors(authorsNames, authorsMore));
    }

    /**
     * Returns an OpenWhisk params map representation of the record.
     * @return The map.
     */
    Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("pubDateMonth", pubDate.month());
        map.put("pubDateYear", pubDate.year());
        map.put("hasProductPageUrl", productPageUrl.isPresent());
        map.put("productPageUrl", productPageUrl.orElse(null));
        map.put("authorsNames", authors.names());
        map.put("authorsMore", authors.more());
        return map;
    }
}
