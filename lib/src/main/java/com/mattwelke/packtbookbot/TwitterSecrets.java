package com.mattwelke.packtbookbot;

/**
 * Holds the secrets for the Twitter API needed to tweet.
 */
public record TwitterSecrets(
        String twitterConsumerKey,
        String twitterConsumerSecret,
        String twitterToken,
        String twitterTokenSecret) {
}
