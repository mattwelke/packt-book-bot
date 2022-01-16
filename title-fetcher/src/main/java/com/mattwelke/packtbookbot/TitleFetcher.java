package com.mattwelke.packtbookbot;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.owextendedruntimes.actiontest.Action;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

    /**
     * Implementation of action invoke method.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) {
        try {
            Document freeBookDoc = Jsoup.connect(freeLearningURL).get();

            var title = title(freeBookDoc);
            var pubDate = pubDate(freeBookDoc);
            var authors = authors(freeBookDoc);

            System.out.println(String.format(
                    "Done parsing free learning page. Title = %s. Publication date = %s. Author(s) = %s.",
                    title, pubDate, authors));

            var titleData = Map.of(
                    "title", title,
                    "pubDateMonth", pubDate.month(),
                    "pubDateYear", pubDate.year(),
                    "authors", authors);

            return titleData;
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed to fetch title data (error: %s).", ex.getMessage()), ex);
        }
    }

    /**
     * Given a JSoup document representing the free learning page, parses out the
     * name of the title on the page.
     * 
     * @param page The JSoup document for the page.
     * @return The name of the title.
     */
    private static String title(Document page) {
        Elements freeBookPageTitleEls = page.select(freeBookPageTitleSelector);
        String freeBookTitle = freeBookPageTitleEls.first().text()
                .replace("Free eBook - ", "");
        return freeBookTitle;
    }

    /**
     * Given a JSoup document representing the free learning page, parses out the
     * publication date (which is broken into month and year) of the title on the
     * page.
     * 
     * @param page The JSoup document for the page.
     * @return The publication date of the title.
     */
    private static PublicationDate pubDate(Document page) {
        Elements freeBookPagePubDateEls = page.select(freeBookPagePubDateSelector);
        String shortPubDateStr = freeBookPagePubDateEls.first().children().first().html()
                .split("Publication date: ")[1];
        String[] shortPubDateMonthSplit = shortPubDateStr.split(" ");

        String shortPubDateMonth = shortPubDateMonthSplit[0];
        String pubDateMonth = shortPubDateMonth.length() == 3
                ? PublicationDateMonths.monthName(shortPubDateMonth)
                : shortPubDateMonth;

        String pubDateYear = shortPubDateMonthSplit[1];

        return new PublicationDate(pubDateMonth, pubDateYear);
    }

    /**
     * Given a JSoup document representing the free learning page, parses out the
     * authors of the title on the page.
     * 
     * @param page The JSoup document for the page.
     * @return The authors of the title.
     */
    private static List<String> authors(Document page) {
        String authorsStr = page.select("span.product-info__author").first()
                .html().trim().split("By ")[1];
        return Arrays.asList(authorsStr.replace(" ,", ",").replace(", ", ",").split(","));
    }

    private static record PublicationDate(String month, String year) {
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