package com.ikms.email;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class EmailController {

  private final EmailRepository emailRepository;

  public EmailController(EmailRepository emailRepository) {
    this.emailRepository = emailRepository;
  }

  @GetMapping("/api/clients/{clientId}/emails")
  public List<EmailContracts.EmailSummaryResponse> listClientEmails(@PathVariable UUID clientId) {
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
        .toList();
  }
}
