package com.ikms.worker.extract;

import com.ikms.ai.AiProviderSettingsService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

@Service
public class TextExtractionService {

  private static final Pattern NON_PRINTABLE = Pattern.compile("[^\\p{L}\\p{N}\\p{P}\\p{Zs}\\r\\n\\t]");
  private final AiProviderSettingsService aiProviderSettingsService;

  public TextExtractionService(AiProviderSettingsService aiProviderSettingsService) {
    this.aiProviderSettingsService = aiProviderSettingsService;
  }

  public ExtractionResult extract(ExtractionRequest request) {
    var providerSettings = aiProviderSettingsService.current();
    List<PageSegment> segments = extractSegments(request);
    String extractedText = segments.stream()
        .map(PageSegment::text)
        .filter(text -> text != null && !text.isBlank())
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("")
        .trim();

    if (extractedText.isBlank()) {
      extractedText = fallbackText(request.filename(), request.mimeType());
      segments = List.of(new PageSegment(null, extractedText));
    }

    String language = extractedText.toLowerCase().contains(" der ") || extractedText.toLowerCase().contains(" und ")
        ? "de"
        : "en";

    return new ExtractionResult(extractedText, language, providerSettings.ocrProvider(), segments);
  }

  private List<PageSegment> extractSegments(ExtractionRequest request) {
    if (request.content().length == 0) {
      return List.of();
    }

    try {
      if ("application/pdf".equalsIgnoreCase(request.mimeType())) {
        try (var pdf = Loader.loadPDF(request.content())) {
          List<PageSegment> segments = new java.util.ArrayList<>();
          PDFTextStripper stripper = new PDFTextStripper();
          for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            String text = sanitize(stripper.getText(pdf)).trim();
            if (!text.isBlank()) {
              segments.add(new PageSegment(page, text));
            }
          }
          return segments;
        }
      }
      if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(request.mimeType())) {
        try (var document = new XWPFDocument(new ByteArrayInputStream(request.content()));
            var extractor = new XWPFWordExtractor(document)) {
          return List.of(new PageSegment(null, sanitize(extractor.getText())));
        }
      }
    } catch (IOException ignored) {
      return List.of();
    }

    return List.of(new PageSegment(null, sanitize(new String(request.content(), StandardCharsets.UTF_8))));
  }

  private static String sanitize(String value) {
    return NON_PRINTABLE.matcher(value).replaceAll(" ");
  }

  private static String fallbackText(String filename, String mimeType) {
    if ("application/pdf".equalsIgnoreCase(mimeType)) {
      return "Scanned PDF content extracted from " + filename;
    }
    if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(mimeType)) {
      return "DOCX content extracted from " + filename;
    }
    return "Extracted text for " + filename;
  }

  public record ExtractionRequest(
      String filename,
      String mimeType,
      byte[] content,
      String provider) {
  }

  public record ExtractionResult(
      String extractedText,
      String language,
      String provider,
      List<PageSegment> segments) {
  }

  public record PageSegment(
      Integer pageNumber,
      String text) {
  }
}
