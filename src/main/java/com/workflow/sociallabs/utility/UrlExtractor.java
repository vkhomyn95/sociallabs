package com.workflow.sociallabs.utility;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlExtractor {

    private UrlExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String extractUrl(String text) throws URISyntaxException {
        return new URI(text).getHost();
    }
}
