package com.ikms.worker.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ikms.ai.AiProviderClient;
import com.ikms.ai.AiProviderSettingsService;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextExtractionServiceTest {

  private TextExtractionService service;
  private AiProviderClient aiProviderClient;

  @BeforeEach
  void setUp() {
    AiProviderSettingsService settingsService = mock(AiProviderSettingsService.class);
    when(settingsService.current()).thenReturn(new AiProviderSettingsService.ProviderSettings(
        "mistral",
        "mistral-small-latest",
        "mistral-embed",
        "https://api.mistral.ai/v1",
        "secret",
        "mistral-ocr-latest",
        true));
    aiProviderClient = mock(AiProviderClient.class);
    when(aiProviderClient.ocr(any(), any(), any())).thenReturn(java.util.Optional.empty());
    service = new TextExtractionService(settingsService, aiProviderClient);
  }

  @Test
  void extractsPdfText() throws Exception {
    byte[] content;
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        stream.newLineAtOffset(100, 700);
        stream.showText("Policy renewal due on 01 August");
        stream.endText();
      }
      document.save(outputStream);
      content = outputStream.toByteArray();
    }

    var result = service.extract(new TextExtractionService.ExtractionRequest(
        "renewal.pdf",
        "application/pdf",
        content,
        "tesseract"));

    assertThat(result.extractedText()).contains("Policy renewal due on 01 August");
    assertThat(result.provider()).isEqualTo("pdfbox");
  }

  @Test
  void extractsDocxText() throws Exception {
    byte[] content;
    try (XWPFDocument document = new XWPFDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      XWPFParagraph paragraph = document.createParagraph();
      paragraph.createRun().setText("Carrier contact updated for policy 42.");
      document.write(outputStream);
      content = outputStream.toByteArray();
    }

    var result = service.extract(new TextExtractionService.ExtractionRequest(
        "policy.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        content,
        "tesseract"));

    assertThat(result.extractedText()).contains("Carrier contact updated for policy 42.");
    assertThat(result.language()).isEqualTo("en");
    assertThat(result.provider()).isEqualTo("apache-poi");
  }

  @Test
  void usesOcrForScannedPdfWhenNativeExtractionIsBlank() {
    when(aiProviderClient.ocr(any(), any(), any())).thenReturn(java.util.Optional.of(
        new AiProviderClient.OcrResult(
            "mistral-ocr-latest",
            List.of(
                new AiProviderClient.OcrPage(1, "# OCR Title\n\nScanned policy text", 0.93),
                new AiProviderClient.OcrPage(2, "Follow-up page text", 0.89)))));

    var result = service.extract(new TextExtractionService.ExtractionRequest(
        "scanned.pdf",
        "application/pdf",
        "%PDF-scan".getBytes(),
        "mistral-ocr-latest"));

    assertThat(result.provider()).isEqualTo("mistral-ocr-latest");
    assertThat(result.extractedText()).contains("OCR Title").contains("Scanned policy text").contains("Follow-up page text");
    assertThat(result.segments()).hasSize(2);
    assertThat(result.segments().getFirst().pageNumber()).isEqualTo(1);
    assertThat(result.extractionConfidence()).isEqualByComparingTo(new BigDecimal("0.9100"));
  }
}
