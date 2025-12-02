package com.cinema.hub.backend.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class YoutubeEmbedHelper {

    private YoutubeEmbedHelper() {
    }

    public static String normalize(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String trimmed = rawUrl.trim();
        trimmed = extractIframeSrc(trimmed);
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(trimmed);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null) {
                return buildEmbedFromVideoId(trimmed);
            }
            String normalizedHost = host.toLowerCase();
            if (normalizedHost.contains("youtu.be")) {
                String videoId = path == null ? "" : path.replaceFirst("^/", "");
                return videoId.isEmpty() ? null : buildEmbedFromVideoId(videoId);
            }
            if (normalizedHost.contains("youtube.")) {
                if (path != null && path.startsWith("/embed/")) {
                    return ensureHttps(trimmed);
                }
                if ("/watch".equals(path)) {
                    String videoId = extractQueryParam(uri.getQuery(), "v");
                    return videoId == null ? ensureHttps(trimmed) : buildEmbedFromVideoId(videoId);
                }
            }
            return ensureHttps(trimmed);
        } catch (URISyntaxException ex) {
            return buildEmbedFromVideoId(trimmed);
        }
    }

    private static String extractIframeSrc(String value) {
        String lower = value.toLowerCase();
        int srcIndex = lower.indexOf("src=");
        boolean hasIframe = lower.contains("<iframe");
        if (!hasIframe && srcIndex == -1) {
            return value;
        }
        if (srcIndex == -1) {
            return stripHtmlTags(value);
        }
        int start = srcIndex + 4;
        while (start < value.length() && Character.isWhitespace(value.charAt(start))) {
            start++;
        }
        if (start >= value.length()) {
            return stripHtmlTags(value);
        }
        char quote = value.charAt(start);
        if (quote == '"' || quote == '\'') {
            int end = value.indexOf(quote, start + 1);
            if (end > start) {
                return value.substring(start + 1, end);
            }
        }
        int end = value.indexOf(' ', start);
        if (end == -1) {
            end = value.indexOf('>', start);
        }
        if (end == -1) {
            end = value.length();
        }
        return value.substring(start, end).replaceAll("[\"'>]", "").trim();
    }

    private static String stripHtmlTags(String input) {
        return input.replaceAll("<[^>]*>", "").trim();
    }

    private static String buildEmbedFromVideoId(String videoId) {
        if (videoId == null || videoId.isBlank()) {
            return null;
        }
        String cleaned = videoId.trim();
        if (cleaned.startsWith("https://") || cleaned.startsWith("http://")) {
            return cleaned;
        }
        return "https://www.youtube.com/embed/" + cleaned;
    }

    private static String ensureHttps(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return "https://" + url.replaceFirst("^https?://", "");
    }

    private static String extractQueryParam(String query, String key) {
        if (query == null || key == null) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            String paramKey = idx > -1 ? pair.substring(0, idx) : pair;
            if (key.equals(paramKey)) {
                return idx > -1 ? pair.substring(idx + 1) : "";
            }
        }
        return null;
    }
}
