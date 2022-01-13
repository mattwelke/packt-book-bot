package com.mattwelke.packtbookbot;

import java.util.Map;

/**
 * Holds the secrets for the Twitter API needed to tweet.
 */
public record TwitterSecrets(
        String consumerKey,
        String consumerSecret,
        String token,
        String tokenSecret) {
    /**
     * Creates a record from an OpenWhisk action params map.
     * 
     * @param params Params map.
     * @return The record.
     */
    static TwitterSecrets of(Map<String, Object> params) {
        if (!params.containsKey("twitterConsumerKey") || ((String) params.get("twitterConsumerKey")).length() < 1) {
            throw new IllegalArgumentException("missing param \"twitterConsumerKey\"");
        }

        if (!params.containsKey("twitterConsumerSecret") || ((String) params.get("twitterConsumerSecret")).length() < 1) {
            throw new IllegalArgumentException("missing param \"twitterConsumerSecret\"");
        }

        if (!params.containsKey("twitterToken") || ((String) params.get("twitterToken")).length() < 1) {
            throw new IllegalArgumentException("missing param \"twitterToken\"");
        }

        if (!params.containsKey("twitterTokenSecret") || ((String) params.get("twitterTokenSecret")).length() < 1) {
            throw new IllegalArgumentException("missing param \"twitterTokenSecret\"");
        }

        return new TwitterSecrets(
            (String) params.get("twitterConsumerKey"),
            (String) params.get("twitterConsumerSecret"),
            (String) params.get("twitterToken"),
            (String) params.get("twitterTokenSecret")
        );
    }
}
