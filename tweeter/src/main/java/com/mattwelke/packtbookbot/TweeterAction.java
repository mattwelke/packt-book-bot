package com.mattwelke.packtbookbot;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.owextendedruntimes.actiontest.Action;
import com.stackoverflow.smile.TwitterOauthHeaderGenerator;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.mattwelke.packtbookbot.Urls.FREE_LEARNING;

/**
 * Given data about the Packt free book of the day, tweets it using the Packt
 * Book Bot Twitter account.
 */
public class TweeterAction extends Action {
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

    private final Logger logger = Logger.getLogger(TweeterAction.class.getName());

    /**
     * Given the title and URL of the product page for the book of the day, Tweets
     * the book of the day.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) throws RuntimeException {
        TitleData data = TitleData.of(params);
        String tweet;

        if (data.productPageUrl().isPresent()) {
            logger.log(Level.INFO, "Using detailed tweet because product page URL was available.");
            tweet = String.format(
                    tweetTemplateDetailed,
                    data.title(),
                    formatPublicationDate(data),
                    formatAuthors(data),
                    FREE_LEARNING,
                    data.productPageUrl().get());
        } else {
            logger.log(Level.INFO,"Falling back to minimal tweet because product page URL was not available");
            tweet = String.format(
                    tweetTemplateMinimal,
                    data.title(),
                    formatPublicationDate(data),
                    formatAuthors(data),
                    FREE_LEARNING);
        }

        logger.log(Level.INFO, "Finished tweet = \"{}\".", tweet);

        try {
            HttpResponse response = postToTwitter(tweet, TwitterSecrets.of(params));
            logger.log(Level.INFO, "Tweeted. Response from Twitter: {0}.", response);
            return Map.of("tweetResponse", response);
        } catch (Exception ex) {
            throw new RuntimeException("Could not tweet.", ex);
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
        List<String> authors = data.authors().names();

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
        return String.format("%s %s", data.pubDate().month(), data.pubDate().year());
    }

    /**
     * Given a tweet and secrets for the Twitter API, tweets the tweet.
     *
     * @param tweetBody The tweet to be tweeted.
     * @param secrets   The secrets.
     * @throws IOException when the HTTP request to Twitter fails.
     */
    private HttpResponse postToTwitter(String tweetBody, TwitterSecrets secrets)
            throws IOException {
        final String json = "application/json";

        HttpPost request = new HttpPost(twitterURL);

        TwitterOauthHeaderGenerator headerGenerator = new TwitterOauthHeaderGenerator(
                secrets.consumerKey(),
                secrets.consumerSecret(),
                secrets.token(),
                secrets.tokenSecret());

        String header = headerGenerator.generateHeader("POST", twitterURL, new HashMap<>());

        request.addHeader(HttpHeaders.AUTHORIZATION, header);
        request.addHeader(HttpHeaders.CONTENT_TYPE, json);

        request.setEntity(new StringEntity("{\"text\":\"" + tweetBody + "\"}"));

        return httpClient.execute(request);
    }

    /**
     * For local testing.
     *
     * @param args args
     */
    public static void main(String[] args) {
        // Test of a title that has been known to fail the fetch product page URL step
        // before.
        new TweeterAction().invoke(Map.of(
                "title", "Mastering Microsoft Dynamics 365 Customer Engagement - Second Edition",
                "pubDateMonth", "February",
                "pubDateYear", "2019",
                "authors", List.of("Deepesh Somani"),
                "twitterConsumerKey", System.getenv("TWITTER_CONSUMER_KEY"),
                "twitterConsumerSecret", System.getenv("TWITTER_CONSUMER_SECRET"),
                "twitterToken", System.getenv("TWITTER_TOKEN"),
                "twitterTokenSecret", System.getenv("TWITTER_TOKEN_SECRET")));
    }
}
