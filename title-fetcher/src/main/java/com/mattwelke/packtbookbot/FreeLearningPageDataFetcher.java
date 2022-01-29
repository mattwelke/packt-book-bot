package com.mattwelke.packtbookbot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.mattwelke.packtbookbot.Urls.FREE_LEARNING;

public class FreeLearningPageDataFetcher {
    private static final String freeLearningURL = FREE_LEARNING;
    private Document doc;

    /**
     * Creates an instance of the fetcher.
     *
     * @throws IOException when Jsoup fails to load the free learning page URL.
     */
    FreeLearningPageDataFetcher() throws IOException {
        doc = Jsoup.connect(freeLearningURL).get();
    }

    /**
     * Parses the title from the fetched free learning page.
     *
     * @return The title of the book.
     */
    String title() {
        Elements freeBookPageTitleEls = doc.select("h3.product-info__title");
        return freeBookPageTitleEls.first().text().replace("Free eBook - ", "");
    }

    /**
     * Parses the publication date from the fetched free learning page.
     *
     * @return The publication date of the book.
     */
    PublicationDate pubDate() {
        Elements freeBookPagePubDateEls = doc.select("div.free_learning__product_pages_date");
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
     * Parses whichever authors are available from the free learning page. Sometimes, the free learning page doesn't
     * display all the authors. Instead, it displays a string like "Rohit Tamma, Oleg Skulkin, Heather Mahalik and 1
     * more". When this happens, this method returns more: true in the returned Authors record.
     *
     * @return The authors this fetcher was able to parse from the free learning page.
     */
    Authors authors() {
        String authorsStr = doc.select("span.product-info__author")
                .first().html().trim()
                .split("By ")[1];

        return parseAuthorsString(authorsStr);
    }

    /**
     * Isolating code used to parse the authors data from the authors string on the free learning page.
     *
     * @param authorsStr The authors string.
     * @return The parsed authors data.
     */
    @SuppressWarnings("GrazieInspection")
    static Authors parseAuthorsString(String authorsStr) {
        List<String> authorNames = new ArrayList<>(Arrays.asList(authorsStr.replace(" ,", ",")
                .replace(", ", ",").split(",")));

        String lastAuthorName = authorNames.get(authorNames.size() - 1);

        boolean more = lastAuthorName.contains("and");

        if (more) {
            // Sanitize final author name
            authorNames.remove(authorNames.size() - 1);
            lastAuthorName = lastAuthorName.split("and")[0];
            authorNames.add(lastAuthorName);
        }

        return new Authors(authorNames.stream().map(String::trim).collect(Collectors.toList()), more);
    }
}
