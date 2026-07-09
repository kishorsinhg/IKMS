package com.ikms.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DatabaseAppSettingService implements AppSettingService {

  private final AppSettingRepository repository;

  public DatabaseAppSettingService(AppSettingRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> get(String key) {
    return repository.findById(key).map(AppSetting::getValue);
  }

  @Override
  @Transactional(readOnly = true)
  public String getRequired(String key) {
    return get(key).orElseThrow(() -> new IllegalArgumentException("Missing application setting: " + key));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, String> getAll() {
    Map<String, String> settings = new LinkedHashMap<>();
    repository.findAll().forEach(setting -> settings.put(setting.getKey(), setting.getValue()));
    return settings;
  }

  @Override
  public void put(String key, String value, String description) {
    AppSetting setting = repository.findById(key).orElseGet(AppSetting::new);
    setting.setKey(key);
    setting.setValue(value);
    setting.setDescription(description);
    repository.save(setting);
  }
}
