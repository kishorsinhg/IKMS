package com.ikms.governance;

public enum InformationClassification {
  PUBLIC,
  INTERNAL,
  CONFIDENTIAL,
  RESTRICTED,
  HIGHLY_RESTRICTED;

  public boolean satisfies(InformationClassification required) {
    return rank(this) >= rank(required);
  }

  public static int rank(InformationClassification value) {
    if (value == null) {
      return 0;
    }
    return switch (value) {
      case PUBLIC -> 0;
      case INTERNAL -> 1;
      case CONFIDENTIAL -> 2;
      case RESTRICTED -> 3;
      case HIGHLY_RESTRICTED -> 4;
    };
  }

  public static InformationClassification parse(String value, InformationClassification fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return InformationClassification.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return fallback;
    }
  }
}
