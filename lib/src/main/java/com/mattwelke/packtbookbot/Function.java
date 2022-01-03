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
import org.jsoup.select.Elements;

/**
 * Hello world!
 *
 */
public class Function extends Action {
    private static final String freeLearningURL = "https://www.packtpub.com/free-learning";
    private static final String twitterURL = "https://api.twitter.com/2/tweets";
    private static final String titleSelector = "h3.product-info__title";
    private static final String productLinkSelector = "a.product-item-link";

    // Secrets - rotate before making repo public
    private static final String consumerKey = "YPn8qTcTKp983e5cjoiJ733xa";
    private static final String consumerSecret = "aI6HTUIfXB7rhP0fwCne5gQQhrGZ3vhjRc6JLrEOQoJBAwA7WV";
    private static final String token = "1477507964298739712-VFdoDZug9ifkvA9GX89rEqoywZnblu";
    private static final String tokenSecret = "amKCrk4X7XKNEqwdDR901sBRy2WhgXr5CrxKWc5bhRuBf";

    // HTTP client settings for web scraping
    // long timeout is because Packt's search function can sometimes be very slow
    private static final int httpTimeoutSec = 60;
    private static HttpClient httpClient;

    @Override
    public Map<String, Object> invoke(Map<String, Object> input) {
        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(httpTimeoutSec * 1000)
                    .setConnectionRequestTimeout(httpTimeoutSec * 1000)
                    .setSocketTimeout(httpTimeoutSec * 1000).build();

            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

            Document freeBookDoc = Jsoup.connect(freeLearningURL).get();
            Elements freeBookEls = freeBookDoc.select(titleSelector);

            if (freeBookEls.size() == 0) {
                System.err.println("Selector (" + titleSelector + ") found no elements. Cannot continue.");
            }
            if (freeBookEls.size() > 1) {
                System.err.println("Selector (" + titleSelector + ") found multiple elements. Cannot continue.");
            }

            var freeBookTitle = freeBookEls.first().text().replace("Free eBook - ", "");

            System.out.println("Done parsing. Free book of the day is " + freeBookTitle + ".");

            // Use title to get link to product page
            String searchURL = "https://www.packtpub.com/catalogsearch/result/?q=%22"
                    + URLEncoder.encode(freeBookTitle, StandardCharsets.UTF_8) + "%22";

            Document searchDoc = Jsoup.connect(searchURL).get();
            Elements searchEls = searchDoc.select(productLinkSelector);

            String freeBookURL = searchEls.first().attr("href");

            String tweet = String.format(
                    "%s is the free eBook of the day from Packt!\\n\\Visit %s to claim the eBook and %s for more info about the title.",
                    freeBookTitle, freeLearningURL, freeBookURL);
            System.out.println("Finished tweet = " + tweet);

            postToTwitter(tweet);

            System.out.println("Done posting to twitter.");
        }
         catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error: " + e.getMessage());
        }

        return Map.of("done", (Object) "ye");
    }

    private static void postToTwitter(String tweetBody) throws URISyntaxException, IOException {
        final var json = "application/json";

        HttpPost request = new HttpPost(twitterURL);

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                new TwitterOauthHeaderGenerator(consumerKey, consumerSecret, token, tokenSecret)
                        .generateHeader("POST", twitterURL, new HashMap<String, String>()));
        request.addHeader(HttpHeaders.CONTENT_TYPE, json);

        request.setEntity(new StringEntity("{\"text\":\"" + tweetBody + "\"}"));

        HttpResponse response = httpClient.execute(request);
        System.out.println("Twitter API request complete. Response status = " +
                response.getStatusLine());
    }
}
