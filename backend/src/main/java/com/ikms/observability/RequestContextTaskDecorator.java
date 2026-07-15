package com.ikms.observability;

import java.util.Map;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@Component
public class RequestContextTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    Map<String, String> snapshot = RequestContextHolder.snapshot();
    return () -> {
      try (RequestContextHolder.Scope ignored = RequestContextHolder.restoreSnapshot(snapshot)) {
        runnable.run();
      }
    };
  }
}
