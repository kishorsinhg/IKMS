package com.ikms.client;

import com.ikms.security.AppUserPrincipal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/clients")
public class ClientImportController {

  private final ClientImportService clientImportService;

  public ClientImportController(ClientImportService clientImportService) {
    this.clientImportService = clientImportService;
  }

  @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ClientImportService.ClientImportResult importClients(
      @RequestPart("file") MultipartFile file,
      Authentication authentication) throws IOException {
    AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
    return clientImportService.importCsv(file.getOriginalFilename(), content, principal.id());
  }
}
