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
    private static final String titleSelector = "h3.product-info__title";

    // HTTP client settings for web scraping
    // long timeout is because Packt's site can sometimes be slow.
    private static final int httpTimeoutSec = 60;
    private static HttpClient httpClient;

    @Override
    public Map<String, Object> invoke(Map<String, Object> input) {
        try {
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(httpTimeoutSec * 1000)
                    .setConnectionRequestTimeout(httpTimeoutSec * 1000)
                    .setSocketTimeout(httpTimeoutSec * 1000).build())
                    .build();

            Document freeBookDoc = Jsoup.connect(freeLearningURL).get();
            Elements freeBookEls = freeBookDoc.select(titleSelector);

            if (freeBookEls.size() == 0) {
                System.err.println("Selector (" + titleSelector + ") found no elements. Cannot continue.");
            }
            if (freeBookEls.size() > 1) {
                System.err.println("Selector (" + titleSelector + ") found multiple elements. Cannot continue.");
            }

            var freeBookTitle = freeBookEls.first().text().replace("Free eBook - ", "");

            var author = freeBookDoc.select(".free_learning__author").first().html().trim().replace("By ", "");

            System.out.println("Done parsing. Free book of the day is " + freeBookTitle + ".");

            var freeBookURL = productPageURL(freeBookTitle);

            var productPageDoc = Jsoup.connect(freeBookURL).get();

            // Author(s)
            var authorsEl = productPageDoc.select(".product-info__author").first();
            var authors = authorsEl.html().trim().replace("By ", "").replace(" , ", ", ");
            var authorsStrBase = authors.contains(",") ? "Authors" : "Author";
            var authorsStr = String.format("%s: %s", authorsStrBase, authors);

            // Publication date
            var datalistEl = productPageDoc.select(".overview__datalist").first();
            String pubDate = datalistEl.children().stream()
                    .filter((Element el) -> el.select(".datalist__name").html().toLowerCase()
                            .contains("publication date"))
                    .findFirst().get().select(".datalist__value").html();

            String tweet = String.format(
                    "%s (%s, %s) is the free eBook of the day from Packt!\\n\\nVisit %s to claim the eBook and %s for more info about the title.",
                    freeBookTitle, pubDate, authorsStr, freeLearningURL, freeBookURL);
            System.out.println("Finished tweet = " + tweet);

            postToTwitter(tweet);

            System.out.println("Finished posting to twitter.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error: " + e.getMessage());
        }

        return Map.of("done", (Object) "ye");
    }

    private void postToTwitter(String tweetBody) throws URISyntaxException, IOException {
        final var json = "application/json";
        
        final var consumerKey = (String) clusterContext.get("twitterConsumerKey");
        final var consumerSecret = (String) clusterContext.get("twitterConsumerSecret");
        final var token = (String) clusterContext.get("twitterToken");
        final var tokenSecret = (String) clusterContext.get("twitterSecret");

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

    private static String productPageURL(String title) throws IOException {
        Document searchDoc = Jsoup.connect(googleSearchURL(title)).get();
        Elements searchEls = searchDoc.select("h3");

        // Find the h3 that represents the search result for the title
        Element titleSearchResult = searchEls.stream()
                .filter((Element res) -> res.html().contains(String.format("%s | Packt", title)))
                .findFirst().get();

        // Get the link from the search result - parent element is <a>
        // return titleSearchResult.parent().attr("href");
        return "https://www.packtpub.com/product/microsoft-365-word-tips-and-tricks/9781800565432";
    }

    private static String googleSearchURL(String title) {
        var googleSearchQuery = String.format("\"%s\" site:packtpub.com -site:subscription.packtpub.com", title);
        return String.format("https://google.com/search?q=%s",
                URLEncoder.encode(googleSearchQuery, StandardCharsets.UTF_8));
    }

    public static void main(String[] args) {
        new Function().invoke(Map.of());
    }

}
