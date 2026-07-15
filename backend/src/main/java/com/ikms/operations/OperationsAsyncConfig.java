package com.ikms.operations;

import java.util.concurrent.Executor;
import com.ikms.observability.RequestContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class OperationsAsyncConfig {

  @Bean
  public Executor operationsExecutor(RequestContextTaskDecorator taskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ikms-ops-");
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(32);
    executor.setTaskDecorator(taskDecorator);
    executor.initialize();
    return executor;
  }
}
