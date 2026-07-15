package com.ikms.ai.context;

import com.ikms.ai.orchestration.EnterpriseAiContracts;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TokenBudgetManager {

  public BudgetAllocation allocate(
      EnterpriseAiContracts.QueryPlan plan,
      String prompt,
      List<String> conversationHistory,
      List<EnterpriseAiContracts.RetrievedEvidence> evidence,
      Map<String, Object> metadata) {
    int totalBudget = Math.max(plan.tokenBudget(), 1200);
    int reserveCompletion = Math.max(250, totalBudget / 5);
    int systemBudget = Math.min(320, totalBudget / 6);
    int userBudget = Math.min(420, Math.max(180, estimateTokens(prompt)));
    int metadataBudget = Math.min(280, Math.max(90, estimateTokens(metadata)));
    int remaining = Math.max(totalBudget - reserveCompletion - systemBudget - userBudget - metadataBudget, 240);
    int conversationBudget = Math.min(remaining / 3, Math.max(120, estimateTokens(conversationHistory)));
    int evidenceBudget = Math.max(remaining - conversationBudget, 120);
    return new BudgetAllocation(
        totalBudget,
        systemBudget,
        userBudget,
        conversationBudget,
        evidenceBudget,
        metadataBudget,
        reserveCompletion);
  }

  public int estimateTokens(String text) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    return Math.max(1, text.length() / 4);
  }

  public int estimateTokens(Map<String, Object> metadata) {
    return estimateTokens(metadata == null ? "" : metadata.toString());
  }

  public int estimateTokens(List<String> content) {
    return estimateTokens(content == null ? "" : String.join("\n", content));
  }

  public record BudgetAllocation(
      int totalBudget,
      int systemBudget,
      int userBudget,
      int conversationBudget,
      int evidenceBudget,
      int metadataBudget,
      int reserveCompletionBudget) {
  }
}
