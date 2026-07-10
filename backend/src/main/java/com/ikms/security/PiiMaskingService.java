package com.ikms.security;

import com.ikms.client.ClientContracts;
import com.ikms.email.EmailContracts;
import com.ikms.security.domain.Permission;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PiiMaskingService {

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "\\b([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");
  private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9][0-9()\\-\\s]{5,}[0-9]");

  public boolean canViewPii(Set<Permission> permissions) {
    return permissions.contains(Permission.VIEW_PII);
  }

  public ClientContracts.ClientProfileResponse maskClientProfile(
      ClientContracts.ClientProfileResponse profile,
      Set<Permission> permissions) {
    if (canViewPii(permissions)) {
      return profile;
    }
    return new ClientContracts.ClientProfileResponse(
        profile.id(),
        profile.clientId(),
        profile.clientIdTemporary(),
        profile.clientType(),
        profile.status(),
        profile.displayName(),
        profile.legalName(),
        maskEmail(profile.primaryEmail()),
        maskPhone(profile.primaryPhone()),
        maskName(profile.contactPerson()),
        profile.createdAt(),
        profile.updatedAt());
  }

  public EmailContracts.EmailSummaryResponse maskEmailSummary(
      EmailContracts.EmailSummaryResponse email,
      Set<Permission> permissions) {
    if (canViewPii(permissions)) {
      return email;
    }
    return new EmailContracts.EmailSummaryResponse(
        email.id(),
        email.clientId(),
        email.subject(),
        maskEmail(email.sender()),
        maskDelimitedEmails(email.recipients()),
        email.processingStatus(),
        email.reviewStatus(),
        email.receivedAt());
  }

  public String trimFreeText(String value, Set<Permission> permissions) {
    if (value == null || canViewPii(permissions)) {
      return value;
    }

    String maskedEmails = EMAIL_PATTERN.matcher(value).replaceAll(match -> {
      String localFirst = match.group(1);
      String domain = match.group(2);
      return localFirst + "***@" + domain;
    });
    return PHONE_PATTERN.matcher(maskedEmails).replaceAll(match -> maskPhone(match.group()));
  }

  public String maskEmail(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    int atIndex = value.indexOf('@');
    if (atIndex <= 0) {
      return "***";
    }
    return value.charAt(0) + "***" + value.substring(atIndex);
  }

  public String maskPhone(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String digits = value.replaceAll("\\D", "");
    if (digits.length() < 4) {
      return "***";
    }
    return "***-***-" + digits.substring(digits.length() - 4);
  }

  public String maskName(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return value.charAt(0) + "***";
  }

  private String maskDelimitedEmails(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return java.util.Arrays.stream(value.split(","))
        .map(String::trim)
        .map(this::maskEmail)
        .reduce((left, right) -> left + ", " + right)
        .orElse(value);
  }
}
