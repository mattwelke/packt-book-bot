package com.mattwelke.packtbookbot;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The data for a title obtainable from the free learning URL.
 */
public record TitleData(
        String title,
        String pubDateMonth,
        String pubDateYear,
        Optional<String> productPageUrl,
        Authors authors) {
    /**
     * Creates a record from an OpenWhisk action params map.
     * 
     * @param params Params map.
     * @return The record.
     */
    static TitleData of(Map<String, Object> params) throws IllegalArgumentException {
        if (!params.containsKey("title")) {
            throw new IllegalArgumentException("missing param \"title\"");
        }
        String title = (String) params.get("title");
        if (title.length() < 1) {
            throw new IllegalArgumentException("param \"title\" must be at least one character long");
        }

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

        if (!params.containsKey("productPageUrl")) {
            throw new IllegalArgumentException("missing param \"productPageUrl\"");
        }
        // TODO: Learn what warning about unchecked cast means
        Optional<String> productPageUrl = (Optional<String>) params.get("productPageUrl");
        if (productPageUrl.isPresent() && productPageUrl.get().length() < 1) {
            throw new IllegalArgumentException("param \"productPageUrl\", if provided, must be at least one character long");
        }

        if (!params.containsKey("authors")) {
            throw new IllegalArgumentException("missing param \"freeBookURL\"");
        }
        Authors authors = (Authors) params.get("authors");
        if (authors.names().size() < 1) {
            throw new IllegalArgumentException("param \"authors\" needs at least one author");
        }
        for (String author : authors.names()) {
            if (author.length() < 1) {
                throw new IllegalArgumentException("each author in param \"authors\" must be at least one character long");
            }
        }

        return new TitleData(title, pubDateMonth, pubDateYear, productPageUrl, authors);
    }
}
