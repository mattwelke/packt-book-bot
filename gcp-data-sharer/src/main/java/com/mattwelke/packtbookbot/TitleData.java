package com.mattwelke.packtbookbot;

import java.util.List;
import java.util.Map;

/**
 * The data for a title obtainable from the free learning URL.
 */
public record TitleData(
        String title,
        List<String> authors,
        String pubDateMonth,
        String pubDateYear) {
    /**
     * Creates a record from an OpenWhisk action params map.
     * 
     * @param params Params map.
     * @return The record.
     */
    static TitleData of(Map<String, Object> params) throws IllegalArgumentException {
        if (!params.containsKey("title") || ((String) params.get("title")).length() < 1) {
            throw new IllegalArgumentException("missing param \"title\"");
        }
        String title = (String) params.get("title");

        if (!params.containsKey("authors")) {
            throw new IllegalArgumentException("missing param \"freeBookURL\"");
        }
        List<String> authors = (List<String>) params.get("authors");
        if (authors.size() < 1) {
            throw new IllegalArgumentException("param \"authors\" needs at least one author");
        }
        for (String author : authors) {
            if (author.length() < 1) {
                throw new IllegalArgumentException("each author in param \"authors\" must be at least one character long");
            }
        }

        if (!params.containsKey("pubDateMonth") || ((String) params.get("pubDateMonth")).length() < 1) {
            throw new IllegalArgumentException("missing param \"pubDateMonth\"");
        }
        String pubDateMonth = (String) params.get("pubDateMonth");

        if (!params.containsKey("pubDateYear") || ((String) params.get("pubDateYear")).length() < 1) {
            throw new IllegalArgumentException("missing param \"pubDateYear\"");
        }
        String pubDateYear = (String) params.get("pubDateYear");

        return new TitleData(title, authors, pubDateMonth, pubDateYear);
    }
}
