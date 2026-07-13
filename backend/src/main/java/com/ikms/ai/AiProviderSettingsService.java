package com.ikms.ai;

import com.ikms.config.domain.AiProviderSetting;
import com.ikms.config.domain.AiProviderSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiProviderSettingsService {

  private final AiProviderSettingRepository aiProviderSettingRepository;

  public AiProviderSettingsService(AiProviderSettingRepository aiProviderSettingRepository) {
    this.aiProviderSettingRepository = aiProviderSettingRepository;
  }

  public ProviderSettings current() {
    AiProviderSetting setting = aiProviderSettingRepository.findAll().stream()
        .findFirst()
        .orElseGet(this::defaultSetting);
    return new ProviderSettings(
        setting.getProviderName(),
        setting.getModelName(),
        setting.getApiBaseUrl(),
        setting.getApiKey(),
        setting.getOcrProvider(),
        setting.isActive());
  }

  private AiProviderSetting defaultSetting() {
    AiProviderSetting setting = new AiProviderSetting();
    setting.setProviderName("mistral");
    setting.setModelName("mistral-small");
    setting.setApiBaseUrl("");
    setting.setOcrProvider("tesseract");
    setting.setActive(true);
    return setting;
  }

  public record ProviderSettings(
      String providerName,
      String modelName,
      String apiBaseUrl,
      String apiKey,
      String ocrProvider,
      boolean active) {
  }
}
