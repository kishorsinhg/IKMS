package com.ikms;

import com.ikms.client.ClientRepository;
import com.ikms.ai.AiInteractionRepository;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.ikms.config.AppSettingRepository;
import com.ikms.email.EmailRepository;
import com.ikms.note.NoteRepository;
import com.ikms.review.ReviewQueueRepository;
import com.ikms.security.domain.AppUserRepository;
import com.ikms.storage.FileStorageService;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class IkmsApplicationTests {

  @MockBean
  private AppUserRepository appUserRepository;

  @MockBean
  private AppSettingRepository appSettingRepository;

  @MockBean
  private ClientRepository clientRepository;

  @MockBean
  private NoteRepository noteRepository;

  @MockBean
  private DocumentRepository documentRepository;

  @MockBean
  private DocumentVersionRepository documentVersionRepository;

  @MockBean
  private EmailRepository emailRepository;

  @MockBean
  private ReviewQueueRepository reviewQueueRepository;

  @MockBean
  private FileStorageService fileStorageService;

  @MockBean
  private EmbeddingChunkRepository embeddingChunkRepository;

  @MockBean
  private AiInteractionRepository aiInteractionRepository;

  @Test
  void contextLoads() {
  }
}
