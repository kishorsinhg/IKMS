package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiProviderClientTest {

  private MockRestServiceServer server;
  private AiProviderClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new AiProviderClient(new ObjectMapper().findAndRegisterModules(), builder.build());
  }

  @Test
  void validateShouldReturnReadyWhenChatAndEmbeddingsSucceed() {
    server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
        .andExpect(method(POST))
        .andRespond(withSuccess("""
            {"choices":[{"message":{"content":"OK"}}]}
            """, MediaType.APPLICATION_JSON));
    server.expect(requestTo("https://api.openai.com/v1/embeddings"))
        .andExpect(method(POST))
        .andRespond(withSuccess("""
            {"data":[{"embedding":[0.12,0.34,0.56]}]}
            """, MediaType.APPLICATION_JSON));

    var result = client.validate(new AiProviderSettingsService.ProviderSettings(
        "openai",
        "gpt-5-mini",
        "text-embedding-3-large",
        "https://api.openai.com/v1",
        "secret-key",
        "tesseract",
        true));

    assertThat(result.valid()).isTrue();
    assertThat(result.chatModelReachable()).isTrue();
    assertThat(result.embeddingModelReachable()).isTrue();
    assertThat(result.ocrProviderSupported()).isTrue();
    assertThat(result.status()).isEqualTo("READY");
    server.verify();
  }

  @Test
  void validateShouldFailWhenConfigurationIsIncomplete() {
    var result = client.validate(new AiProviderSettingsService.ProviderSettings(
        "openai",
        "gpt-5-mini",
        "text-embedding-3-large",
        "",
        "",
        "unknown-ocr",
        true));

    assertThat(result.valid()).isFalse();
    assertThat(result.chatModelReachable()).isFalse();
    assertThat(result.embeddingModelReachable()).isFalse();
    assertThat(result.ocrProviderSupported()).isFalse();
    assertThat(result.status()).isEqualTo("INVALID_CONFIGURATION");
  }

  @Test
  void ocrShouldReturnPageAwareMarkdownAndConfidence() {
    server.expect(requestTo("https://api.mistral.ai/v1/ocr"))
        .andExpect(method(POST))
        .andRespond(withSuccess("""
            {
              "model":"mistral-ocr-latest",
              "pages":[
                {"index":0,"markdown":"# Page One","confidence_scores":{"average_page_confidence_score":0.97}},
                {"index":1,"markdown":"Page Two","confidence_scores":{"average_page_confidence_score":0.91}}
              ]
            }
            """, MediaType.APPLICATION_JSON));

    var result = client.ocr(new AiProviderSettingsService.ProviderSettings(
        "mistral",
        "mistral-small-latest",
        "mistral-embed",
        "https://api.mistral.ai/v1",
        "secret-key",
        "mistral-ocr-latest",
        true), "application/pdf", "pdf".getBytes());

    assertThat(result).isPresent();
    assertThat(result.get().model()).isEqualTo("mistral-ocr-latest");
    assertThat(result.get().pages()).hasSize(2);
    assertThat(result.get().pages().getFirst().pageNumber()).isEqualTo(1);
    assertThat(result.get().pages().getFirst().averageConfidenceScore()).isEqualTo(0.97d);
    server.verify();
  }

  @Test
  void answerWithEvidenceShouldReturnCompletionText() {
    server.expect(requestTo("https://api.mistral.ai/v1/chat/completions"))
        .andExpect(method(POST))
        .andRespond(withSuccess("""
            {"choices":[{"message":{"content":"The renewal is due next month based on the policy schedule."}}]}
            """, MediaType.APPLICATION_JSON));

    var result = client.answerWithEvidence(
        new AiProviderSettingsService.ProviderSettings(
            "mistral",
            "mistral-small-latest",
            "mistral-embed",
            "https://api.mistral.ai/v1",
            "secret-key",
            "mistral-ocr-latest",
            true),
        "What is due next month?",
        List.of(new AiProviderClient.EvidenceSnippet(
            "DOCUMENT",
            "Policy Schedule",
            "Page 3",
            "renewal due next month")),
        false);

    assertThat(result).hasValue("The renewal is due next month based on the policy schedule.");
    server.verify();
  }
}
