package com.ikms.ai.provider;

public interface LlmStreamingHandler {

  LlmStreamingHandler NO_OP = new LlmStreamingHandler() {
  };

  default void onStart() {
  }

  default void onDelta(String delta) {
  }

  default void onComplete(String content) {
  }

  default void onError(String message) {
  }

  default boolean isCancelled() {
    return false;
  }

  default boolean isTimedOut() {
    return false;
  }
}
