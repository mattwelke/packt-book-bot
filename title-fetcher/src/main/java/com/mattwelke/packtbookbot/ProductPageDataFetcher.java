package com.mattwelke.packtbookbot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;

public class ProductPageDataFetcher {
    private final Document doc;

    /**
     * Given a URL for the product page, creates an instance of the fetcher.
     *
     * @throws IOException when Jsoup fails to load the provided URL.
     */
    ProductPageDataFetcher(String url) throws IOException {
        doc = Jsoup.connect(url).get();
    }

    /**
     * Parses the complete list of author names from the product page contents.
     * @return The authors of the book.
     */
    List<String> authors() {
        return doc.select("h5.accordion__title").stream()
                .map(Element::html).map(String::trim).toList();
    }

    /**
     * For local testing
     */
    public static void main(String[] args) throws IOException {
        // Product page with four authors
        new ProductPageDataFetcher("https://www.packtpub.com/product/practical-mobile-forensics-fourth-edition/9781838647520").authors();
    }
}
