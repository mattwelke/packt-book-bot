package com.mattwelke.packtbookbot;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Responsible for trying to fetch the product page URL given a title. Used to
 * get more data for the tweet if possible so that it renders nicer.
 */
public class ProductPageUrlFetcher {
    private String title;
    private String pubDateMonth;
    private String pubDateYear;

    /**
     * Creates a new fetcher.
     * 
     * @param title      The Packt book title.
     * @param pubDateStr The publication date of the title in the format "Jan 1970"
     *                   etc. Used to prevent the wrong title from appearing at the
     *                   top of the Google search results. This can happen when
     *                   there are multiple editions of the same title.
     */
    public ProductPageUrlFetcher(String title, String pubDateStr) {
        this.title = title;

        String[] pubDateStrSplit = pubDateStr.split(" ");
        pubDateMonth = pubDateStrSplit[0];
        pubDateYear = pubDateStrSplit[1];
    }

    /**
     * Given a Packt book title, returns a URL that can be expected to be the
     * product page URL where data such as author(s), publication date, can be
     * found.
     * 
     * @throws CouldNotFetchException
     */
    public String fetch() throws CouldNotFetchException {
        try {
            // Create Google search URL
            String googleSearchQuery = String.format("\"%s\" %s %s site:packtpub.com -site:subscription.packtpub.com",
                    title, pubDateMonth, pubDateYear);
            String googleSearchURL = String.format("https://google.com/search?q=%s",
                    URLEncoder.encode(googleSearchQuery, StandardCharsets.UTF_8));

            // Use Google search URL to get a page of search results.
            Document searchDoc = Jsoup.connect(googleSearchURL).get();

            // Assume that the top search result is the product page because we made the
            // search query as strict as possible.
            Elements h3s = searchDoc.select("h3");
            Stream<Element> h3sWithTitle = h3s.stream().filter((Element res) -> res.html().contains(title));
            Optional<Element> firstH3WithTitle = h3sWithTitle.findFirst();
            Element titleH3 = firstH3WithTitle.get();
            Element titleH3Parent = titleH3.parent();

            // Get the link from the search result - parent element is <a>
            String hrefValue = titleH3Parent.attr("href");

            if (hrefValue.equals("")) {
                throw new IllegalArgumentException("href value on Google search results page was an empty string");
            }

            return hrefValue;
        } catch (Exception ex) {
            throw new CouldNotFetchException(
                    String.format("failed to fetch product page URL for title %s: %s", title, ex.getMessage()), ex);
        }
    }
}
