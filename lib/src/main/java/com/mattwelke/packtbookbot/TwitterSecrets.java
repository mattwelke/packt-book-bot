package com.mattwelke.packtbookbot;

/**
 * Holds the secrets for the Twitter API needed to tweet.
 */
public record TwitterSecrets(
        String consumerKey,
        String consumerSecret,
        String token,
        String tokenSecret) {
}
