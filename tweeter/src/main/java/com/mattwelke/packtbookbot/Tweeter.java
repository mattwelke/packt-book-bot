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

    private static final String tweetTemplate = "%s (%s, %s) is the free eBook of the day from Packt!\\n\\nVisit %s to claim the eBook and %s for more info about the title.";

    /**
     * Given the title and URL of the product page for the book of the day, Tweets
     * the book of the day.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) {
        try {
            var data = TitleData.of(params);

            String tweet = String.format(
                    tweetTemplate,
                    data.title(),
                    formatPublicationDate(data),
                    formatAuthors(data),
                    freeLearningURL,
                    data.productPageURL());

            System.out.println("Finished tweet = \"" + tweet + "\".");
            
            postToTwitter(tweet, TwitterSecrets.of(params));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error: " + e.getMessage());
        }

        // Nothing to return to OpenWhisk.
        return Map.of();
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
    private void postToTwitter(String tweetBody, TwitterSecrets secrets) throws URISyntaxException, IOException {
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

        HttpResponse response = httpClient.execute(request);

        System.out.println("Finished posting to Twitter. Response code from API request: " +
                response.getStatusLine().getStatusCode());
    }

    /**
     * For local testing.
     * 
     * @param args
     */
    public static void main(String[] args) {
        new Tweeter().invoke(Map.of(
                "consumerKey", (Object) System.getenv("TWITTER_CONSUMER_KEY"),
                "consumerSecret", (Object) System.getenv("TWITTER_CONSUMER_SECRET"),
                "token", (Object) System.getenv("TWITTER_TOKEN"),
                "tokenSecret", (Object) System.getenv("TWITTER_TOKEN_SECRET")));
    }
}
