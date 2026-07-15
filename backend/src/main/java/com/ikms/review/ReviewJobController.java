package com.ikms.review;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review/jobs")
public class ReviewJobController {

  private final ReviewQueueService reviewQueueService;

  public ReviewJobController(ReviewQueueService reviewQueueService) {
    this.reviewQueueService = reviewQueueService;
  }

  @GetMapping
  public List<ReviewContracts.ReviewQueueItemResponse> list(
      @RequestParam(name = "status", required = false) ReviewQueueStatus status,
      @RequestParam(name = "reason", required = false) ReviewQueueReason reason) {
    return reviewQueueService.list(status, reason);
  }

  @GetMapping("/{itemId}")
  public ReviewContracts.ReviewQueueItemResponse get(@PathVariable UUID itemId) {
    return reviewQueueService.get(itemId);
  }

  @PostMapping("/{itemId}/approve")
  public ReviewContracts.ReviewQueueItemResponse approve(@PathVariable UUID itemId) {
    return reviewQueueService.approve(itemId);
  }

  @PostMapping("/{itemId}/reject")
  public ReviewContracts.ReviewQueueItemResponse reject(
      @PathVariable UUID itemId,
      @Valid @RequestBody ReviewContracts.ReviewDecisionRequest request) {
    return reviewQueueService.reject(itemId, request.reason());
  }

  @PostMapping("/{itemId}/retry")
  public ReviewContracts.ReviewQueueItemResponse retry(
      @PathVariable UUID itemId,
      @Valid @RequestBody(required = false) ReviewContracts.RetryReviewJobRequest request) {
    return reviewQueueService.retry(itemId, request == null ? null : request.reviewerComment());
  }
}
