package com.mattwelke.packtbookbot;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
     * @throws CouldNotFetchException when it encounters any issue parsing HTML that prevents it from fetching the
     *                                product page URL.
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

            // Sometimes, the book in the Google search results doesn't have the "- nth Edition"
            // suffix, but we still want that result to be chosen. We trust that because we included
            // the publication month and day in the search query, the first result that matches will be
            // the edition intended, even if multiple editions of the book have been published.
            String titleForH3Match = title.replaceAll("(?i)- (Second|Third|Fourth|Fifth|Sixth) Edition", "");

            // Include only the h3s with the title, but exclude ones with "Images for" because
            // sometimes an h3 with that will come up before the actual search result links.
            Optional<Element> firstH3WithTitle = searchDoc.select("h3").stream()
                    .filter(e -> !e.text().toLowerCase().contains("images for"))
                    .filter(e -> e.html().contains(titleForH3Match))
                    .findFirst();

            if (firstH3WithTitle.isEmpty()) {
                throw new CouldNotFetchException("Google search results page did not have a usable h3 with the book's title.");
            }

            Element titleH3 = firstH3WithTitle.get();

            if (titleH3.parent() == null) {
                throw new CouldNotFetchException("Chosen h3 on Google search results page did not have a parent element.");
            }

            Element titleH3Parent = titleH3.parent();

            // Get the link from the search result - parent element is <a>
            String hrefValue = titleH3Parent.attr("href");

            if (hrefValue.equals("")) {
                throw new CouldNotFetchException("href value on parent element of chosen h3 on Google search results page was an empty string.");
            }

            return hrefValue;
        } catch (CouldNotFetchException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CouldNotFetchException(
                    String.format("Failed to fetch product page URL for title %s.", title), ex);
        }
    }
}
