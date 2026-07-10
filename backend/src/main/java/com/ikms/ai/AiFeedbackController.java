package com.ikms.ai;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-interactions")
public class AiFeedbackController {

  private final AiInteractionRepository aiInteractionRepository;

  public AiFeedbackController(AiInteractionRepository aiInteractionRepository) {
    this.aiInteractionRepository = aiInteractionRepository;
  }

  @PostMapping("/{interactionId}/feedback")
  public void feedback(@PathVariable UUID interactionId, @Valid @RequestBody AiContracts.AiFeedbackRequest request) {
    AiInteraction interaction = aiInteractionRepository.findById(interactionId)
        .orElseThrow(() -> new IllegalArgumentException("AI interaction not found: " + interactionId));
    interaction.setHelpfulFeedback(request.helpful());
    interaction.setFeedbackComment(request.comment());
    aiInteractionRepository.save(interaction);
  }
}
