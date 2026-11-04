package org.owasp.webgoat.container.users;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

// This slice replaces the datasource with a fresh embedded database (no Flyway), so Hibernate
// builds the schema. The full application and full-context tests keep ddl-auto=none and use Flyway.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("webgoat-test")
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  void userShouldBeSaved() {
    WebGoatUser user = new WebGoatUser("test", "password");
    userRepository.saveAndFlush(user);

    user = userRepository.findByUsername("test");

    Assertions.assertThat(user.getUsername()).isEqualTo("test");
    Assertions.assertThat(user.getPassword()).isEqualTo("password");
  }
}
