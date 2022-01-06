package com.mattwelke.packtbookbot;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.owextendedruntimes.actiontest.Action;
import com.stackoverflow.smile.TwitterOauthHeaderGenerator;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Hello world!
 *
 */
public class Function extends Action {
    private static final String freeLearningURL = "https://www.packtpub.com/free-learning";
    private static final String twitterURL = "https://api.twitter.com/2/tweets";

    // CSS selectors
    private static final String freeBookPageTitleSelector = "h3.product-info__title";
    private static final String productPageAuthorsSelector = ".product-info__author";
    private static final String productPageDatalistSelector = ".overview__datalist";
    private static final String productPageDatalistNameSelector = ".datalist__name";
    private static final String productPageDatalistValueSelector = ".datalist__value";

    // HTTP client settings for web scraping
    // long timeout is because Packt's site can sometimes be slow.
    private static final int httpTimeoutSec = 60;
    private static HttpClient httpClient;

    /**
     * Implementation of action invoke method.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) {
        try {
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(httpTimeoutSec * 1000)
                    .setConnectionRequestTimeout(httpTimeoutSec * 1000)
                    .setSocketTimeout(httpTimeoutSec * 1000).build())
                    .build();

            Document freeBookDoc = Jsoup.connect(freeLearningURL).get();
            Elements freeBookEls = freeBookDoc.select(freeBookPageTitleSelector);

            if (freeBookEls.size() == 0) {
                System.err.println("Selector (" + freeBookPageTitleSelector + ") found no elements. Cannot continue.");
            }
            if (freeBookEls.size() > 1) {
                System.err.println(
                        "Selector (" + freeBookPageTitleSelector + ") found multiple elements. Cannot continue.");
            }

            String freeBookTitle = freeBookEls.first().text().replace("Free eBook - ", "");

            System.out.println("Done parsing free book page. Title is " + freeBookTitle + ".");

            String freeBookURL = productPageURL(freeBookTitle);

            Document productPageDoc = Jsoup.connect(freeBookURL).get();

            // Author(s)
            Element authorsEl = productPageDoc.select(productPageAuthorsSelector).first();
            String authors = authorsEl.html().trim().replace("By ", "").replace(" , ", ", ");
            String authorsStrBase = authors.contains(",") ? "Authors" : "Author";
            String authorsStr = String.format("%s: %s", authorsStrBase, authors);

            // Publication date
            Element datalistEl = productPageDoc.select(productPageDatalistSelector).first();
            String pubDate = datalistEl.children().stream()
                    .filter((Element el) -> el.select(productPageDatalistNameSelector).html().toLowerCase()
                            .contains("publication date"))
                    .findFirst().get().select(productPageDatalistValueSelector).html();

            String tweet = String.format(
                    "%s (%s, %s) is the free eBook of the day from Packt!\\n\\nVisit %s to claim the eBook and %s for more info about the title.",
                    freeBookTitle, pubDate, authorsStr, freeLearningURL, freeBookURL);
            System.out.println("Finished tweet = " + tweet);

            postToTwitter(tweet, new TwitterSecrets(
                    (String) params.get("twitterConsumerKey"),
                    (String) params.get("twitterConsumerSecret"),
                    (String) params.get("twitterToken"),
                    (String) params.get("twitterTokenSecret")));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error: " + e.getMessage());
        }

        // Nothing to return to OpenWhisk.
        return Map.of();
    }

    /**
     * Given a tweet body, tweets it.
     * 
     * @param tweetBody The tweet body.
     * @throws URISyntaxException
     * @throws IOException
     */
    private void postToTwitter(String tweetBody, TwitterSecrets secrets) throws URISyntaxException, IOException {
        final String json = "application/json";

        final String consumerKey = (String) clusterContext.get("twitterConsumerKey");
        final String consumerSecret = (String) clusterContext.get("twitterConsumerSecret");
        final String token = (String) clusterContext.get("twitterToken");
        final String tokenSecret = (String) clusterContext.get("twitterTokenSecret");

        HttpPost request = new HttpPost(twitterURL);

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                new TwitterOauthHeaderGenerator(consumerKey, consumerSecret, token, tokenSecret)
                        .generateHeader("POST", twitterURL, new HashMap<String, String>()));
        request.addHeader(HttpHeaders.CONTENT_TYPE, json);

        request.setEntity(new StringEntity("{\"text\":\"" + tweetBody + "\"}"));

        HttpResponse response = httpClient.execute(request);

        System.out.println("Finished posting to Twitter. Response code from API request: " +
                response.getStatusLine().getStatusCode());
    }

    /**
     * Given a Packt book title, returns a URL that can be expected to be the
     * product page URL where data such as author(s), publication date, can be
     * found.
     * 
     * @param title The Packt book title.
     * @return The URL that can be expected to be the product page.
     * @throws IOException
     */
    private static String productPageURL(String title) throws IOException {
        Document searchDoc = Jsoup.connect(googleSearchURL(title)).get();
        Elements searchEls = searchDoc.select("h3");

        // Find the h3 that represents the search result for the title
        Element titleSearchResult = searchEls.stream()
                .filter((Element res) -> res.html().contains(String.format("%s | Packt", title)))
                .findFirst().get();

        // Get the link from the search result - parent element is <a>
        return titleSearchResult.parent().attr("href");
    }

    /**
     * Given a Packt book title, returns a URL that can be used to perform a Google
     * search for the title where the top result can be expected to be the product
     * page for the title.
     * 
     * @param title The Packt book title.
     * @return The URL to use to search for the product page.
     */
    private static String googleSearchURL(String title) {
        String googleSearchQuery = String.format("\"%s\" site:packtpub.com -site:subscription.packtpub.com", title);
        return String.format("https://google.com/search?q=%s",
                URLEncoder.encode(googleSearchQuery, StandardCharsets.UTF_8));
    }
}
