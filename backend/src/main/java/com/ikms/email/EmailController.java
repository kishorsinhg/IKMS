package com.ikms.email;

import com.ikms.security.AppUserPrincipal;
import com.ikms.security.ContentSensitivityService;
import com.ikms.security.PiiMaskingService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class EmailController {

  private final EmailRepository emailRepository;
  private final ContentSensitivityService contentSensitivityService;
  private final PiiMaskingService piiMaskingService;

  public EmailController(
      EmailRepository emailRepository,
      ContentSensitivityService contentSensitivityService,
      PiiMaskingService piiMaskingService) {
    this.emailRepository = emailRepository;
    this.contentSensitivityService = contentSensitivityService;
    this.piiMaskingService = piiMaskingService;
  }

  @GetMapping("/api/clients/{clientId}/emails")
  public List<EmailContracts.EmailSummaryResponse> listClientEmails(
      @PathVariable UUID clientId,
      Authentication authentication) {
    return emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId).stream()
        .map(email -> new EmailContracts.EmailSummaryResponse(
            email.getId(),
            email.getClient() == null ? null : email.getClient().getId(),
            email.getSubject(),
            email.getSender(),
            email.getRecipients(),
            email.getProcessingStatus().name(),
            email.getReviewStatus().name(),
            email.getReceivedAt()))
        .map(email -> contentSensitivityService.emailContainsPii(email.id())
            ? piiMaskingService.maskEmailSummary(email, principal(authentication).permissions())
            : email)
        .toList();
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }
}
