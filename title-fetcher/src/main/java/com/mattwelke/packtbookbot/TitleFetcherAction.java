package com.mattwelke.packtbookbot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.owextendedruntimes.actions.Action;


/**
 * Gets data about the Packt free book of the day and returns it to the action
 * invoker.
 */
public class TitleFetcherAction extends Action {
    private final Logger logger = Logger.getLogger(TitleFetcherAction.class.getName());

    /**
     * Implementation of action invoke method.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) {
        try {
            var freeLearningFetcher = new FreeLearningPageDataFetcher();

            String title = freeLearningFetcher.title();
            PublicationDate pubDate = freeLearningFetcher.pubDate();
            Authors authors = freeLearningFetcher.authors();

            // Fall back to not having a product page URL if the step to get it fails.
            Optional<String> productPageUrl = Optional.empty();

            try {
                productPageUrl = Optional.of(new ProductPageUrlFetcher(title,
                        String.format("%s %s", pubDate.month(), pubDate.year())).fetch());
                logger.log(Level.INFO, "Was able to fetch product page URL successfully. Can provide all author data.");
            } catch (CouldNotFetchException ex) {
                logger.log(Level.INFO, "Could not fetch product page URL. Falling back to providing data without the product page URL and potentially without all authors.", ex);
            }

            if (authors.more() && productPageUrl.isPresent()) {
                // Try to get the complete author data.
                List<String> completeAuthorNames = new ProductPageDataFetcher(productPageUrl.get()).authors();
                authors = new Authors(completeAuthorNames, false);
            }

            logger.log(Level.INFO, "Done parsing page(s) for data. Title = {0}. Publication date = {1}. Author(s) = {2}.", new Object[]{title, pubDate, authors});

            return Map.of(
                    "title", title,
                    "pubDateMonth", pubDate.month(),
                    "pubDateYear", pubDate.year(),
                    "productPageUrl", productPageUrl,
                    "authors", authors);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed to fetch title data (error: %s).", ex.getMessage()), ex);
        }
    }

    /**
     * For local testing.
     */
    public static void main(String[] args) {
        var data = new TitleFetcherAction().invoke(Map.of());
        System.out.println("Fetched data for title = " + data);
    }
}