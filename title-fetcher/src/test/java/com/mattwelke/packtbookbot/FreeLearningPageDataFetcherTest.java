package com.mattwelke.packtbookbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

class FreeLearningPageDataFetcherTest {

    @Test
    void parseAuthorsString0() {
        assertEquals(new Authors(List.of("John Doe"), false), FreeLearningPageDataFetcher.parseAuthorsString("John Doe"));
    }

    @Test
    void parseAuthorsString1() {
        assertEquals(new Authors(List.of("John Doe"), true), FreeLearningPageDataFetcher.parseAuthorsString("John Doe and 1 more"));
    }
}