package com.ikms.review;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ikms.common.api.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ReviewQueueWorkflowTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new TestReviewQueueController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void reviewQueueShouldReturnWorkflowShape() throws Exception {
    mockMvc.perform(get("/api/review-queue"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].itemType").value("DOCUMENT"))
        .andExpect(jsonPath("$[0].reason").value("UNLINKED"))
        .andExpect(jsonPath("$[0].status").value("OPEN"));
  }

  @RestController
  @RequestMapping("/api/review-queue")
  static class TestReviewQueueController {

    @GetMapping
    List<ReviewContracts.ReviewQueueItemResponse> list() {
      return List.of(new ReviewContracts.ReviewQueueItemResponse(
          UUID.fromString("77777777-7777-7777-7777-777777777777"),
          ReviewQueueItemType.DOCUMENT,
          "33333333-3333-3333-3333-333333333333",
          ReviewQueueReason.UNLINKED,
          ReviewQueueStatus.OPEN,
          null,
          "Inbound renewal",
          null,
          null,
          java.util.Map.of("carrier", "Carrier A"),
          null));
    }
  }
}
