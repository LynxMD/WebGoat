package org.owasp.webgoat.container.templates;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.plugins.LessonTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The main page (start.mvc, template main_new) is the first page after login or registration. It
 * must render with the Thymeleaf version that Spring Boot ships, including the GitHub link in the
 * header.
 */
class MainPageTemplateTest extends LessonTest {

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  void mainPageRendersWithGitHubLink() throws Exception {
    mockMvc
        .perform(get("/start.mvc"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("https://github.com/WebGoat/WebGoat")));
  }
}
