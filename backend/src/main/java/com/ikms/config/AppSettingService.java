package com.ikms.config;

import java.util.Map;
import java.util.Optional;

public interface AppSettingService {

  Optional<String> get(String key);

  String getRequired(String key);

  Map<String, String> getAll();

  void put(String key, String value, String description);
}
