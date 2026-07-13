package com.ikms.worker.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ikms.ai.AiProviderSettingsService;
import java.io.ByteArrayOutputStream;
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

  @BeforeEach
  void setUp() {
    AiProviderSettingsService settingsService = mock(AiProviderSettingsService.class);
    when(settingsService.current()).thenReturn(new AiProviderSettingsService.ProviderSettings(
        "openai",
        "gpt-5-mini",
        "https://api.openai.com/v1",
        "secret",
        "tesseract",
        true));
    service = new TextExtractionService(settingsService);
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
    assertThat(result.provider()).isEqualTo("tesseract");
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
  }
}
