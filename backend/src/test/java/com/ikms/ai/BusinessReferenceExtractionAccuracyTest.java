package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.ai.orchestration.BusinessReferenceExtractionService;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class BusinessReferenceExtractionAccuracyTest {

  private final BusinessReferenceExtractionService service = new BusinessReferenceExtractionService();

  @Test
  void extractionShouldMeetFixturePrecisionAndRecallTargets() {
    List<Fixture> fixtures = List.of(
        new Fixture(
            "Customer renewal for policy number POL-12345 claim number CLM-9988 insurer Northwind Mutual",
            Map.of(),
            new Expected("POL-12345", "CLM-9988", "Northwind Mutual", null, null, null, null, null, null)),
        new Fixture(
            "po1icy nurnber POL-54321 renewal 2026-11-01 insurer Blue Tide",
            Map.of(),
            new Expected("POL-54321", null, "Blue Tide", null, null, null, "2026-11-01", null, null)),
        new Fixture(
            "claim nurnber CLM-77777 effective 2026-01-01 expiry 2026-12-31 policy type Commercial Auto",
            Map.of(),
            new Expected(null, "CLM-77777", null, "Commercial Auto", "2026-01-01", "2026-12-31", null, null, null)),
        new Fixture(
            "Broker reference BR-4242 external reference EXT-77 insurer Harbor Mutual",
            Map.of(),
            new Expected(null, null, "Harbor Mutual", null, null, null, null, "BR-4242", "EXT-77")),
        new Fixture(
            "Compare policy POL-10001 with policy POL-10002 and claim CLM-50001",
            Map.of(),
            new Expected("POL-10001", "CLM-50001", null, null, null, null, null, null, null)),
        new Fixture(
            "Malformed identifier policy number P and claim number ?? should not match",
            Map.of(),
            new Expected(null, null, null, null, null, null, null, null, null)),
        new Fixture(
            "No business references are present in this email thread.",
            Map.of(),
            new Expected(null, null, null, null, null, null, null, null, null)));

    Metrics policyNumber = metrics(fixtures, Expected::policyNumber, actual -> actual.policyNumber());
    Metrics claimNumber = metrics(fixtures, Expected::claimNumber, actual -> actual.claimNumber());
    Metrics insurer = metrics(fixtures, Expected::insurer, actual -> actual.insurer());
    Metrics policyType = metrics(fixtures, Expected::policyType, actual -> actual.policyType());
    Metrics effectiveDate = metrics(fixtures, Expected::effectiveDate, actual -> actual.effectiveDate());
    Metrics expiryDate = metrics(fixtures, Expected::expiryDate, actual -> actual.expiryDate());
    Metrics renewalDate = metrics(fixtures, Expected::renewalDate, actual -> actual.renewalDate());
    Metrics brokerReference = metrics(fixtures, Expected::brokerReference, actual -> actual.brokerReference());
    Metrics externalReference = metrics(fixtures, Expected::externalReference, actual -> actual.externalReference());

    assertThat(policyNumber.precision()).isEqualTo(1.0d);
    assertThat(policyNumber.recall()).isGreaterThanOrEqualTo(1.0d);
    assertThat(claimNumber.precision()).isEqualTo(1.0d);
    assertThat(claimNumber.recall()).isGreaterThanOrEqualTo(1.0d);
    assertThat(insurer.precision()).isGreaterThanOrEqualTo(1.0d);
    assertThat(insurer.recall()).isGreaterThanOrEqualTo(1.0d);
    assertThat(policyType.precision()).isGreaterThanOrEqualTo(1.0d);
    assertThat(policyType.recall()).isGreaterThanOrEqualTo(1.0d);
    assertThat(effectiveDate.precision()).isGreaterThanOrEqualTo(1.0d);
    assertThat(effectiveDate.recall()).isGreaterThanOrEqualTo(1.0d);
    assertThat(expiryDate.precision()).isGreaterThanOrEqualTo(1.0d);
    assertThat(expiryDate.recall()).isGreaterThanOrEqualTo(1.0d);
    assertThat(renewalDate.precision()).isGreaterThanOrEqualTo(1.0d);
    assertThat(renewalDate.recall()).isGreaterThanOrEqualTo(1.0d);
    assertThat(brokerReference.precision()).isGreaterThanOrEqualTo(1.0d);
    assertThat(brokerReference.recall()).isGreaterThanOrEqualTo(1.0d);
    assertThat(externalReference.precision()).isGreaterThanOrEqualTo(1.0d);
    assertThat(externalReference.recall()).isGreaterThanOrEqualTo(1.0d);
  }

  @Test
  void extractionShouldPreferStructuredParameterOverrides() {
    EnterpriseAiContracts.BusinessReferenceFields extracted = service.extract(
        "policy number POL-12345 insurer Northwind Mutual",
        Map.of(
            "policyNumber", "POL-99999",
            "insurer", "Blue Tide",
            "brokerReference", "BR-009"));

    assertThat(extracted.policyNumber()).isEqualTo("POL-99999");
    assertThat(extracted.insurer()).isEqualTo("Blue Tide");
    assertThat(extracted.brokerReference()).isEqualTo("BR-009");
  }

  private Metrics metrics(
      List<Fixture> fixtures,
      Function<Expected, String> expectedExtractor,
      Function<EnterpriseAiContracts.BusinessReferenceFields, String> actualExtractor) {
    int truePositives = 0;
    int falsePositives = 0;
    int falseNegatives = 0;
    for (Fixture fixture : fixtures) {
      EnterpriseAiContracts.BusinessReferenceFields extracted = service.extract(fixture.prompt(), fixture.parameters());
      String expected = normalize(expectedExtractor.apply(fixture.expected()));
      String actual = normalize(actualExtractor.apply(extracted));
      if (expected == null && actual == null) {
        continue;
      }
      if (expected != null && expected.equals(actual)) {
        truePositives++;
      } else if (expected == null) {
        falsePositives++;
      } else if (actual == null) {
        falseNegatives++;
      } else {
        falsePositives++;
        falseNegatives++;
      }
    }
    return new Metrics(truePositives, falsePositives, falseNegatives);
  }

  private static String normalize(String value) {
    return value == null ? null : value.trim();
  }

  private record Fixture(
      String prompt,
      Map<String, Object> parameters,
      Expected expected) {
  }

  private record Expected(
      String policyNumber,
      String claimNumber,
      String insurer,
      String policyType,
      String effectiveDate,
      String expiryDate,
      String renewalDate,
      String brokerReference,
      String externalReference) {
  }

  private record Metrics(int truePositives, int falsePositives, int falseNegatives) {
    double precision() {
      int denominator = truePositives + falsePositives;
      return denominator == 0 ? 1.0d : (double) truePositives / denominator;
    }

    double recall() {
      int denominator = truePositives + falseNegatives;
      return denominator == 0 ? 1.0d : (double) truePositives / denominator;
    }
  }
}
