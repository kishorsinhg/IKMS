package com.ikms.worker.extract;

import com.ikms.ai.AiProviderClient;
import com.ikms.ai.AiProviderSettingsService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private static final Pattern ASSET_PLACEHOLDER = Pattern.compile("!\\[[^\\]]+\\]\\([^)]+\\)|\\[[^\\]]+\\]\\([^)]+\\)");
  private final AiProviderSettingsService aiProviderSettingsService;
  private final AiProviderClient aiProviderClient;

  public TextExtractionService(
      AiProviderSettingsService aiProviderSettingsService,
      AiProviderClient aiProviderClient) {
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.aiProviderClient = aiProviderClient;
  }

  public ExtractionResult extract(ExtractionRequest request) {
    var providerSettings = aiProviderSettingsService.current();
    List<PageSegment> segments = extractSegments(request);
    String provider = nativeProvider(request.mimeType());
    BigDecimal extractionConfidence = null;

    if (shouldUseExternalOcr(request, segments)) {
      provider = providerSettings.ocrProvider();
      var ocrResult = aiProviderClient.ocr(providerSettings, request.mimeType(), request.content());
      if (ocrResult.isPresent()) {
        provider = ocrResult.get().model();
        segments = ocrSegments(ocrResult.get());
        extractionConfidence = averageConfidence(ocrResult.get());
      } else {
        extractionConfidence = new BigDecimal("0.2000");
      }
    }

    String extractedText = segments.stream()
        .map(PageSegment::text)
        .filter(text -> text != null && !text.isBlank())
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("")
        .trim();

    if (extractedText.isBlank()) {
      extractedText = fallbackText(request.filename(), request.mimeType());
      segments = List.of(new PageSegment(null, extractedText));
      extractionConfidence = extractionConfidence == null ? new BigDecimal("0.2000") : extractionConfidence;
    }

    String language = extractedText.toLowerCase().contains(" der ") || extractedText.toLowerCase().contains(" und ")
        ? "de"
        : "en";

    return new ExtractionResult(extractedText, language, provider, segments, extractionConfidence);
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

  private static String nativeProvider(String mimeType) {
    if ("application/pdf".equalsIgnoreCase(mimeType)) {
      return "pdfbox";
    }
    if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(mimeType)) {
      return "apache-poi";
    }
    return "plain-text";
  }

  private static boolean shouldUseExternalOcr(ExtractionRequest request, List<PageSegment> segments) {
    if (!"application/pdf".equalsIgnoreCase(request.mimeType())) {
      return false;
    }
    return segments.isEmpty();
  }

  private static List<PageSegment> ocrSegments(AiProviderClient.OcrResult result) {
    return result.pages().stream()
        .map(page -> new PageSegment(page.pageNumber(), sanitizeOcrMarkdown(page.markdown())))
        .filter(segment -> segment.text() != null && !segment.text().isBlank())
        .toList();
  }

  private static BigDecimal averageConfidence(AiProviderClient.OcrResult result) {
    double average = result.pages().stream()
        .map(AiProviderClient.OcrPage::averageConfidenceScore)
        .filter(value -> value != null)
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.2d);
    return BigDecimal.valueOf(Math.max(0d, Math.min(1d, average))).setScale(4, RoundingMode.HALF_UP);
  }

  private static String sanitizeOcrMarkdown(String markdown) {
    String normalized = markdown == null ? "" : markdown;
    normalized = ASSET_PLACEHOLDER.matcher(normalized).replaceAll(" ");
    normalized = normalized.replace('\u00a0', ' ');
    normalized = normalized.replaceAll("[ \t]+", " ");
    normalized = normalized.replaceAll("(?m)^\\s*[-*#]+\\s*", "");
    return sanitize(normalized).trim();
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
      List<PageSegment> segments,
      BigDecimal extractionConfidence) {
  }

  public record PageSegment(
      Integer pageNumber,
      String text) {
  }
}
