package com.ikms.config.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderSettingRepository extends JpaRepository<AiProviderSetting, UUID> {
}
