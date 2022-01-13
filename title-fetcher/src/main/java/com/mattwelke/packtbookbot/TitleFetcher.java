package com.mattwelke.packtbookbot;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.owextendedruntimes.actiontest.Action;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Gets data about the Packt free book of the day and returns it to the action
 * invoker.
 */
public class TitleFetcher extends Action {
    private static final String freeLearningURL = "https://www.packtpub.com/free-learning";

    // CSS selectors
    private static final String freeBookPageTitleSelector = "h3.product-info__title";
    private static final String freeBookPagePubDateSelector = "div.free_learning__product_pages_date";
    private static final String productPageAuthorsSelector = ".product-info__author";
    private static final String productPageDatalistSelector = ".overview__datalist";
    private static final String productPageDatalistNameSelector = ".datalist__name";
    private static final String productPageDatalistValueSelector = ".datalist__value";

    /**
     * Implementation of action invoke method.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) {
        try {
            Document freeBookDoc = Jsoup.connect(freeLearningURL).get();

            Elements freeBookPageTitleEls = freeBookDoc.select(freeBookPageTitleSelector);
            String freeBookTitle = freeBookPageTitleEls.first().text()
                    .replace("Free eBook - ", "");

            Elements freeBookPagePubDateEls = freeBookDoc.select(freeBookPagePubDateSelector);
            String shortPubDateStr = freeBookPagePubDateEls.first().children().first().html()
                    .split("Publication date: ")[1];

            System.out.println("Done parsing free learning page. Title is \"" + freeBookTitle + "\".");

            String freeBookURL = productPageURL(freeBookTitle, shortPubDateStr);

            Document productPageDoc = Jsoup.connect(freeBookURL).get();

            // Author(s)
            Element authorsEl = productPageDoc.select(productPageAuthorsSelector).first();
            String[] authorsArr = authorsEl.html().trim().replace("By ", "")
                    .replace(" , ", ",").split(",");
            List<String> authors = Arrays.asList(authorsArr);

            // Publication date
            Element datalistEl = productPageDoc.select(productPageDatalistSelector).first();
            String pubDate = datalistEl.children().stream()
                    .filter((Element el) -> el.select(productPageDatalistNameSelector).html()
                            .toLowerCase()
                            .contains("publication date"))
                    .findFirst().get().select(productPageDatalistValueSelector).html();
            String[] pubDateSplit = pubDate.split(" ");
            String pubDateMonth = pubDateSplit[0];
            String pubDateYear = pubDateSplit[1];

            Map<String, Object> titleData = Map.of(
                    "title", (Object) freeBookTitle,
                    "freeBookURL", (Object) freeBookURL,
                    "authors", (Object) authors,
                    "pubDateMonth", (Object) pubDateMonth,
                    "pubDateYear", (Object) pubDateYear);

            System.out.println("Finished obtaining data. Data = " + titleData);
            return titleData;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error: " + e.getMessage());
        }

        // Nothing to return to OpenWhisk.
        return Map.of();
    }

    /**
     * Given a Packt book title, returns a URL that can be expected to be the
     * product page URL where data such as author(s), publication date, can be
     * found.
     * 
     * @param title      The Packt book title.
     * @param pubDateStr The publication date of the title in the format "Jan 1970"
     *                   etc. Used to prevent the wrong title from appearing at the
     *                   top of the Google search results. This can happen when
     *                   there are multiple editions of the same title.
     * @return The URL that can be expected to be the product page.
     * @throws IOException
     */
    private static String productPageURL(String title, String pubDateStr) throws IOException {
        // Create Google search URL
        String googleSearchQuery = String.format("\"%s\" %s site:packtpub.com", title, pubDateStr);
        String googleSearchURL = String.format("https://google.com/search?q=%s",
                URLEncoder.encode(googleSearchQuery, StandardCharsets.UTF_8));

        // Use Google search URL to get a page of search results.
        Document searchDoc = Jsoup.connect(googleSearchURL).get();

        // Assume that the top search result is the product page because we made the
        // search query as strict as possible.
        Stream<Element> matchingSearchEls = searchDoc.select("h3").stream()
                .filter((Element res) -> res.html().contains(title));

        Element titleSearchResult = matchingSearchEls.findFirst().get();

        // Get the link from the search result - parent element is <a>
        return titleSearchResult.parent().attr("href");
    }

    /**
     * For local testing.
     * 
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Fetched data for title = " + new TitleFetcher().invoke(Map.of()));
    }
}