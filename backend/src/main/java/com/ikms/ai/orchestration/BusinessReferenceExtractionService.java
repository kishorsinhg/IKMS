package com.ikms.ai.orchestration;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class BusinessReferenceExtractionService {

  private static final Pattern POLICY_NUMBER_PATTERN = Pattern.compile(
      "(?:policy(?:\\s+number)?|pol)\\s*[:#-]?\\s*([A-Z]{2,}[A-Z0-9-]*\\d[A-Z0-9-]*)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern CLAIM_NUMBER_PATTERN = Pattern.compile(
      "(?:claim(?:\\s+number)?|clm)\\s*[:#-]?\\s*([A-Z]{2,}[A-Z0-9-]*\\d[A-Z0-9-]*)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern INSURER_PATTERN = Pattern.compile(
      "(?:insurer|carrier)\\s*[:#-]?\\s*([A-Za-z0-9&.,'\\- ]{3,})",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern POLICY_TYPE_PATTERN = Pattern.compile(
      "(?:policy\\s*type|line\\s*of\\s*business)\\s*[:#-]?\\s*([A-Za-z][A-Za-z0-9&/\\- ]{2,})",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern BROKER_REFERENCE_PATTERN = Pattern.compile(
      "(?:broker\\s*(?:reference|ref))\\s*[:#-]?\\s*([A-Z0-9-]{3,})",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern EXTERNAL_REFERENCE_PATTERN = Pattern.compile(
      "(?:external\\s*(?:reference|ref)|source\\s*reference)\\s*[:#-]?\\s*([A-Z0-9-]{3,})",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern DATE_PATTERN = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");

  public EnterpriseAiContracts.BusinessReferenceFields extract(
      String prompt,
      Map<String, Object> parameters) {
    String safePrompt = normalizePrompt(prompt);
    Map<String, Object> safeParameters = parameters == null ? Map.of() : parameters;

    String policyNumber = stringValue(safeParameters.get("policyNumber"));
    if (isBlank(policyNumber)) {
      policyNumber = firstMatch(POLICY_NUMBER_PATTERN, safePrompt);
    }

    String claimNumber = stringValue(safeParameters.get("claimNumber"));
    if (isBlank(claimNumber)) {
      claimNumber = firstMatch(CLAIM_NUMBER_PATTERN, safePrompt);
    }

    String insurer = stringValue(safeParameters.get("insurer"));
    if (isBlank(insurer)) {
      insurer = extractInsurer(safePrompt);
    }

    String policyType = fallback(stringValue(safeParameters.get("policyType")), firstMatch(POLICY_TYPE_PATTERN, safePrompt));
    String effectiveDate = fallback(stringValue(safeParameters.get("effectiveDate")), extractNamedDate("effective", safePrompt));
    String expiryDate = fallback(stringValue(safeParameters.get("expiryDate")), extractNamedDate("expiry", safePrompt));
    String renewalDate = fallback(stringValue(safeParameters.get("renewalDate")), extractNamedDate("renewal", safePrompt));
    String brokerReference = fallback(stringValue(safeParameters.get("brokerReference")), firstMatch(BROKER_REFERENCE_PATTERN, safePrompt));
    String externalReference = fallback(stringValue(safeParameters.get("externalReference")), firstMatch(EXTERNAL_REFERENCE_PATTERN, safePrompt));

    return new EnterpriseAiContracts.BusinessReferenceFields(
        blankToNull(policyNumber),
        blankToNull(claimNumber),
        blankToNull(insurer),
        blankToNull(policyType),
        blankToNull(effectiveDate),
        blankToNull(expiryDate),
        blankToNull(renewalDate),
        blankToNull(brokerReference),
        blankToNull(externalReference));
  }

  private static String extractInsurer(String prompt) {
    Matcher matcher = INSURER_PATTERN.matcher(prompt);
    if (!matcher.find()) {
      return null;
    }
    return matcher.group(1).trim().replaceAll("\\s{2,}", " ");
  }

  private static String extractNamedDate(String label, String prompt) {
    int labelIndex = prompt.toLowerCase(Locale.ROOT).indexOf(label.toLowerCase(Locale.ROOT));
    if (labelIndex < 0) {
      return null;
    }
    Matcher matcher = DATE_PATTERN.matcher(prompt.substring(labelIndex));
    return matcher.find() ? matcher.group(1) : null;
  }

  private static String firstMatch(Pattern pattern, String prompt) {
    Matcher matcher = pattern.matcher(prompt);
    return matcher.find() ? matcher.group(1).trim() : null;
  }

  private static String stringValue(Object value) {
    return value instanceof String string ? string.trim() : null;
  }

  private static String fallback(String preferred, String fallback) {
    return isBlank(preferred) ? fallback : preferred;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String blankToNull(String value) {
    return isBlank(value) ? null : value;
  }

  private static String normalizePrompt(String prompt) {
    String normalized = prompt == null ? "" : prompt.trim();
    return normalized
        .replace('\u2013', '-')
        .replace('\u2014', '-')
        .replace('\u2212', '-')
        .replaceAll("(?i)po1icy", "policy")
        .replaceAll("(?i)p0licy", "policy")
        .replaceAll("(?i)c1aim", "claim")
        .replaceAll("(?i)nurnber", "number")
        .replaceAll("\\s{2,}", " ");
  }
}
