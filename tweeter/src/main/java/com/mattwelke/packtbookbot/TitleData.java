package com.mattwelke.packtbookbot;

import java.util.List;
import java.util.Map;

public record TitleData(
        String title,
        String productPageURL,
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

        if (!params.containsKey("freeBookURL") || ((String) params.get("freeBookURL")).length() < 1) {
            throw new IllegalArgumentException("missing param \"freeBookURL\"");
        }
        String productPageURL = (String) params.get("freeBookURL");

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

        return new TitleData(title, productPageURL, authors, pubDateMonth, pubDateYear);
    }
}
