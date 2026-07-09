package com.ikms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.ikms.config.AppSettingRepository;
import com.ikms.security.domain.AppUserRepository;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class IkmsApplicationTests {

  @MockBean
  private AppUserRepository appUserRepository;

  @MockBean
  private AppSettingRepository appSettingRepository;

  @Test
  void contextLoads() {
  }
}
