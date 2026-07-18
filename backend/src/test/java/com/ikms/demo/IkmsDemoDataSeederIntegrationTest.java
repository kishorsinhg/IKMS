package com.ikms.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.client.Client;
import com.ikms.client.ClientRepository;
import com.ikms.client.ClientStatus;
import com.ikms.client.ClientType;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.email.EmailRepository;
import com.ikms.note.NoteRepository;
import com.ikms.quality.KnowledgeQualityIssueRepository;
import com.ikms.review.ReviewQueueRepository;
import com.ikms.support.PostgresIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "ikms.demo.mode=seed",
    "ikms.demo.exit-after-run=false",
    "ikms.security.bootstrap.enabled=false"
})
@ActiveProfiles("demo")
class IkmsDemoDataSeederIntegrationTest extends PostgresIntegrationTest {

  @Autowired
  private IkmsDemoDataSeeder seeder;

  @Autowired
  private ClientRepository clientRepository;

  @Autowired
  private NoteRepository noteRepository;

  @Autowired
  private EmailRepository emailRepository;

  @Autowired
  private DocumentRepository documentRepository;

  @Autowired
  private DocumentVersionRepository documentVersionRepository;

  @Autowired
  private ReviewQueueRepository reviewQueueRepository;

  @Autowired
  private KnowledgeQualityIssueRepository knowledgeQualityIssueRepository;

  @Test
  void seedIsIdempotentAndScopedResetPreservesNonDemoData() {
    IkmsDemoDataSeeder.SeedReport firstRun = seeder.seed("ikms-demo-2026-07", LocalDate.of(2026, 7, 18));
    IkmsDemoDataSeeder.SeedReport secondRun = seeder.seed("ikms-demo-2026-07", LocalDate.of(2026, 7, 18));

    assertThat(firstRun.counts()).containsEntry("customers", 18L);
    assertThat(firstRun.counts()).containsEntry("documents", 80L);
    assertThat(firstRun.counts()).containsEntry("emails", 25L);
    assertThat(firstRun.counts()).containsEntry("notes", 25L);
    assertThat(firstRun.counts()).containsEntry("review_items", 20L);
    assertThat(firstRun.counts()).containsEntry("quality_issues", 15L);
    assertThat(firstRun.counts()).containsEntry("document_versions", 108L);

    assertThat(secondRun.counts()).isEqualTo(firstRun.counts());

    Client nonDemoClient = new Client();
    nonDemoClient.setId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    nonDemoClient.setClientId("NON-DEMO-001");
    nonDemoClient.setClientType(ClientType.BUSINESS);
    nonDemoClient.setStatus(ClientStatus.ACTIVE);
    nonDemoClient.setDisplayName("Non Demo Customer");
    nonDemoClient.setLegalName("Non Demo Customer");
    nonDemoClient.setPrimaryEmail("non-demo@example.test");
    nonDemoClient.setCreatedAt(Instant.parse("2026-07-18T00:00:00Z"));
    nonDemoClient.setUpdatedAt(Instant.parse("2026-07-18T00:00:00Z"));
    clientRepository.save(nonDemoClient);

    IkmsDemoDataSeeder.SeedReport reset = seeder.reset("ikms-demo-2026-07");
    assertThat(reset.counts()).containsEntry("customers", 0L);
    assertThat(reset.counts()).containsEntry("documents", 0L);

    assertThat(clientRepository.findById(nonDemoClient.getId())).isPresent();
    assertThat(documentRepository.count()).isZero();
    assertThat(documentVersionRepository.count()).isZero();
    assertThat(emailRepository.count()).isZero();
    assertThat(noteRepository.count()).isZero();
    assertThat(reviewQueueRepository.count()).isZero();
    assertThat(knowledgeQualityIssueRepository.count()).isZero();
  }
}
