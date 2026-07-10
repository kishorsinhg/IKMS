package com.ikms.review;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-queue")
public class ReviewQueueController {

  private final ReviewQueueService reviewQueueService;

  public ReviewQueueController(ReviewQueueService reviewQueueService) {
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

  @PostMapping("/{itemId}/link-client")
  public ReviewContracts.ReviewQueueItemResponse linkClient(
      @PathVariable UUID itemId,
      @Valid @RequestBody ReviewContracts.LinkClientRequest request) {
    return reviewQueueService.linkClient(itemId, request.clientId());
  }

  @PatchMapping("/{itemId}/metadata")
  public ReviewContracts.ReviewQueueItemResponse correctMetadata(
      @PathVariable UUID itemId,
      @Valid @RequestBody ReviewContracts.CorrectMetadataRequest request) {
    return reviewQueueService.correctMetadata(itemId, request.title());
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
}
