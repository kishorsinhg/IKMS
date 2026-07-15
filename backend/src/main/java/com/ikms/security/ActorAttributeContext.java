package com.ikms.security;

import com.ikms.governance.InformationClassification;
import java.util.LinkedHashMap;
import java.util.Map;

public record ActorAttributeContext(
    String businessUnit,
    String department,
    String region,
    String country,
    String brokerOffice,
    String employmentRole,
    InformationClassification securityClearance) {

  public static ActorAttributeContext from(AppUserPrincipal principal) {
    return new ActorAttributeContext(
        principal.businessUnit(),
        principal.department(),
        principal.region(),
        principal.country(),
        principal.brokerOffice(),
        principal.employmentRole(),
        principal.securityClearance());
  }

  public static ActorAttributeContext fromParameters(Map<String, Object> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return empty();
    }
    return new ActorAttributeContext(
        stringValue(parameters.get("auth.businessUnit")),
        stringValue(parameters.get("auth.department")),
        stringValue(parameters.get("auth.region")),
        stringValue(parameters.get("auth.country")),
        stringValue(parameters.get("auth.brokerOffice")),
        stringValue(parameters.get("auth.employmentRole")),
        InformationClassification.parse(stringValue(parameters.get("auth.securityClearance")), InformationClassification.INTERNAL));
  }

  public static ActorAttributeContext empty() {
    return new ActorAttributeContext(null, null, null, null, null, null, InformationClassification.INTERNAL);
  }

  public Map<String, String> asMap() {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("businessUnit", emptyToNull(businessUnit));
    values.put("department", emptyToNull(department));
    values.put("region", emptyToNull(region));
    values.put("country", emptyToNull(country));
    values.put("brokerOffice", emptyToNull(brokerOffice));
    values.put("employmentRole", emptyToNull(employmentRole));
    values.put("securityClearance", securityClearance == null ? InformationClassification.INTERNAL.name() : securityClearance.name());
    return Map.copyOf(values);
  }

  public Map<String, Object> asParameterMap() {
    Map<String, Object> values = new LinkedHashMap<>();
    asMap().forEach((key, value) -> {
      if (value != null) {
        values.put("auth." + key, value);
      }
    });
    return Map.copyOf(values);
  }

  private static String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
