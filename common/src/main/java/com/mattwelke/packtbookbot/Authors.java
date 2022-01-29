package com.mattwelke.packtbookbot;

import java.util.List;

/**
 * Used to convey whether the data fetch step was able to fetch all the authors or just some of
 * them.
 */
public record Authors(List<String> names, boolean more) {
}
