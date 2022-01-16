package com.mattwelke.packtbookbot;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
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

/**
 * Given data about the Packt free book of the day, tweets it using the Packt
 * Book Bot Twitter account.
 */
public class Tweeter extends Action {
    private static final String freeLearningURL = "https://www.packtpub.com/free-learning";
    private static final String twitterURL = "https://api.twitter.com/2/tweets";

    // HTTP client settings for web scraping
    // long timeout is because Packt's site can sometimes be slow.
    private static final int httpTimeoutSec = 60;
    private static final HttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(httpTimeoutSec * 1000)
                    .setConnectionRequestTimeout(httpTimeoutSec * 1000)
                    .setSocketTimeout(httpTimeoutSec * 1000).build())
            .build();

    private static final String tweetTemplateDetailed = "%s (%s, %s) is the free eBook of the day from Packt!\\n\\nVisit %s to claim the eBook and %s for more info about the title.";
    private static final String tweetTemplateMinimal = "%s (%s, %s) is the free eBook of the day from Packt!\\n\\nVisit %s to claim the eBook.";

    /**
     * Given the title and URL of the product page for the book of the day, Tweets
     * the book of the day.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) throws RuntimeException {
        var data = TitleData.of(params);

        String tweet;

        // Use detailed tweet if product page URL fetch succeeds. Otherwise, use minimal
        // tweet.
        try {
            String productPageURL = new ProductPageURLFetcher(data.title(),
                    String.format("%s %s", data.pubDateMonth(), data.pubDateYear())).fetch();

            System.out.println("Using detailed tweet because product page URL fetch succeeded.");

            tweet = String.format(
                    tweetTemplateDetailed,
                    data.title(),
                    formatPublicationDate(data),
                    formatAuthors(data),
                    freeLearningURL,
                    productPageURL);
        } catch (CouldNotFetchException ex) {
            System.out.println(
                    String.format("Falling back to minimal tweet because failed to fetch product page URL (error: %s).",
                            ex.getMessage()));

            tweet = String.format(
                    tweetTemplateMinimal,
                    data.title(),
                    formatPublicationDate(data),
                    formatAuthors(data),
                    freeLearningURL);
        }

        System.out.println(String.format("Finished tweet = \"%s\".", tweet));

        try {
            HttpResponse response = postToTwitter(tweet, TwitterSecrets.of(params));
            return Map.of("tweetResponseStatus", response.getStatusLine());
        } catch (Exception ex) {
            throw new RuntimeException(String.format("failed to tweet: %s", ex.getMessage()), ex);
        }
    }

    /**
     * Given title data, from the list of author(s) for the title, creates a
     * formatted string suitable for display in a tweet.
     * 
     * @param data The title data.
     * @return The formatted author(s) string.
     */
    private String formatAuthors(TitleData data) {
        List<String> authors = data.authors();

        if (authors.size() == 1) {
            return String.format("Author: %s", authors.get(0));
        }

        return String.format("Authors: %s", String.join(", ", authors));
    }

    /**
     * Given title data, from the publication date component strings, creates a
     * formatted string suitable for display in a tweet.
     * 
     * @param data The title data.
     * @return The formatted publication date string.
     */
    private String formatPublicationDate(TitleData data) {
        return String.format("%s %s", data.pubDateMonth(), data.pubDateYear());
    }

    /**
     * Given a tweet and secrets for the Twitter API, tweets the tweet.
     * 
     * @param tweetBody The tweet to be tweeted.
     * @param secrets   The secrets.
     * @throws URISyntaxException
     * @throws IOException
     */
    private HttpResponse postToTwitter(String tweetBody, TwitterSecrets secrets)
            throws URISyntaxException, IOException {
        final String json = "application/json";

        HttpPost request = new HttpPost(twitterURL);

        TwitterOauthHeaderGenerator headerGenerator = new TwitterOauthHeaderGenerator(
                secrets.consumerKey(),
                secrets.consumerSecret(),
                secrets.token(),
                secrets.tokenSecret());

        String header = headerGenerator.generateHeader("POST", twitterURL, new HashMap<String, String>());

        request.addHeader(HttpHeaders.AUTHORIZATION, header);
        request.addHeader(HttpHeaders.CONTENT_TYPE, json);

        request.setEntity(new StringEntity("{\"text\":\"" + tweetBody + "\"}"));

        return httpClient.execute(request);
    }

    /**
     * For local testing.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // Test of a title that has been known to fail the fetch product page URL step
        // before.
        new Tweeter().invoke(Map.of(
                "title", "Hands-On Software Engineering with Python",
                "pubDateMonth", "October",
                "pubDateYear", "2018",
                "authors", List.of("Brian Allbee"),
                "consumerKey", (Object) System.getenv("TWITTER_CONSUMER_KEY"),
                "consumerSecret", (Object) System.getenv("TWITTER_CONSUMER_SECRET"),
                "token", (Object) System.getenv("TWITTER_TOKEN"),
                "tokenSecret", (Object) System.getenv("TWITTER_TOKEN_SECRET")));
    }
}
