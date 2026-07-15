package com.ikms.search;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class SearchSupport {

  private SearchSupport() {
  }

  static double score(String normalizedQuery, Set<String> queryTokens, String text) {
    String normalizedText = nullSafe(text).toLowerCase(Locale.ROOT);
    if (normalizedText.isBlank()) {
      return 0d;
    }
    double score = !normalizedQuery.isBlank() && normalizedText.contains(normalizedQuery) ? 2d : 0d;
    Set<String> textTokens = tokenize(normalizedText);
    long overlap = queryTokens.stream().filter(textTokens::contains).count();
    return score + overlap;
  }

  static Set<String> tokenize(String value) {
    return java.util.Arrays.stream(value.split("[^\\p{L}\\p{N}]+"))
        .map(String::trim)
        .filter(token -> !token.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  static String excerpt(String preferredText, String text, String fallback, String query) {
    String value = nullSafe(preferredText).trim();
    if (value.isBlank()) {
      value = nullSafe(text).trim();
    }
    if (value.isBlank()) {
      return fallback;
    }
    if (query == null || query.isBlank()) {
      return truncate(value);
    }
    int index = value.toLowerCase(Locale.ROOT).indexOf(query);
    if (index < 0) {
      return truncate(value);
    }
    int start = Math.max(0, index - 40);
    int end = Math.min(value.length(), index + query.length() + 80);
    return value.substring(start, end).trim();
  }

  static String truncate(String value) {
    return value.length() <= 160 ? value : value.substring(0, 160).trim() + "...";
  }

  static String nullSafe(String value) {
    return value == null ? "" : value;
  }

  static String citationQuality(String sourceType, Integer pageNumber, String sourceSection) {
    boolean hasLocation = pageNumber != null || (sourceSection != null && !sourceSection.isBlank());
    if ("DOCUMENT".equals(sourceType)) {
      return hasLocation ? "HIGH" : "LOW";
    }
    return hasLocation ? "HIGH" : "MEDIUM";
  }
}
