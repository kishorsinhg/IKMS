package com.ikms;

import com.ikms.document.DocumentProcessingValidationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(DocumentProcessingValidationProperties.class)
public class IkmsApplication {

  public static void main(String[] args) {
    SpringApplication.run(IkmsApplication.class, args);
  }
}
