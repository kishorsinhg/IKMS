package com.ikms.demo;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class IkmsDemoDataRunner implements ApplicationRunner {

  private final ConfigurableApplicationContext applicationContext;
  private final IkmsDemoDataSeeder seeder;
  private final String datasourceUrl;
  private final String configuredMode;
  private final boolean exitAfterRun;
  private final String namespace;
  private final LocalDate referenceDate;

  public IkmsDemoDataRunner(
      ConfigurableApplicationContext applicationContext,
      IkmsDemoDataSeeder seeder,
      @Value("${spring.datasource.url}") String datasourceUrl,
      @Value("${ikms.demo.mode:seed}") String configuredMode,
      @Value("${ikms.demo.exit-after-run:false}") boolean exitAfterRun,
      @Value("${ikms.demo.namespace:ikms-demo-2026-07}") String namespace,
      @Value("${ikms.demo.reference-date:2026-07-18}") LocalDate referenceDate) {
    this.applicationContext = applicationContext;
    this.seeder = seeder;
    this.datasourceUrl = datasourceUrl;
    this.configuredMode = configuredMode;
    this.exitAfterRun = exitAfterRun;
    this.namespace = namespace;
    this.referenceDate = referenceDate;
  }

  @Override
  public void run(ApplicationArguments args) {
    SafetyInfo safetyInfo = parseSafetyInfo(datasourceUrl);
    String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
    String profileSummary = activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);

    System.out.println("IKMS demo data environment check");
    System.out.println("  active profile(s): " + profileSummary);
    System.out.println("  database host: " + safetyInfo.host());
    System.out.println("  database name: " + safetyInfo.databaseName());
    System.out.println("  namespace: " + namespace);
    System.out.println("  reference date: " + referenceDate);

    if (appearsProduction(profileSummary, safetyInfo)) {
      throw new IllegalStateException("Refusing to seed demo data because the environment appears production-like.");
    }

    String mode = configuredMode.trim().toLowerCase(Locale.ROOT);
    IkmsDemoDataSeeder.SeedReport report = switch (mode) {
      case "seed" -> seeder.seed(namespace, referenceDate);
      case "reset" -> seeder.reset(namespace);
      case "reset-and-seed" -> {
        seeder.reset(namespace);
        yield seeder.seed(namespace, referenceDate);
      }
      default -> throw new IllegalArgumentException("Unsupported ikms.demo.mode: " + configuredMode);
    };

    System.out.println("IKMS demo data action: " + report.mode());
    report.counts().forEach((key, value) -> System.out.println("  " + key + ": " + value));

    if (exitAfterRun) {
      int exitCode = org.springframework.boot.SpringApplication.exit(applicationContext, () -> 0);
      System.exit(exitCode);
    }
  }

  private boolean appearsProduction(String profileSummary, SafetyInfo safetyInfo) {
    String normalizedProfiles = profileSummary.toLowerCase(Locale.ROOT);
    String normalizedHost = safetyInfo.host().toLowerCase(Locale.ROOT);
    String normalizedDatabase = safetyInfo.databaseName().toLowerCase(Locale.ROOT);
    return normalizedProfiles.contains("prod")
        || normalizedHost.contains("prod")
        || normalizedDatabase.contains("prod");
  }

  private SafetyInfo parseSafetyInfo(String jdbcUrl) {
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      return new SafetyInfo("unknown", "unknown");
    }
    if (jdbcUrl.startsWith("jdbc:postgresql://")) {
      URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
      String host = uri.getHost() == null ? "unknown" : uri.getHost();
      String path = uri.getPath() == null ? "" : uri.getPath();
      String databaseName = path.startsWith("/") ? path.substring(1) : path;
      return new SafetyInfo(host, databaseName.isBlank() ? "unknown" : databaseName);
    }
    return new SafetyInfo("unknown", jdbcUrl);
  }

  private record SafetyInfo(String host, String databaseName) {
  }
}
