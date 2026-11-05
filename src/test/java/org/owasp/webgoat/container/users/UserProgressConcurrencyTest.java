package org.owasp.webgoat.container.users;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.lessons.Assignment;
import org.owasp.webgoat.container.lessons.Category;
import org.owasp.webgoat.container.lessons.Lesson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two requests of the same user that record the first attempt of a lesson at the same time must not
 * create two progress rows for that lesson. With two rows, the lesson overview and the progress
 * tracker can pick different rows, so a solved assignment shows as unsolved.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("webgoat-test")
// Every repository call runs in its own transaction, like two separate HTTP requests.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserProgressConcurrencyTest {

  private static final String USER = "race-user";

  private static class RaceLesson extends Lesson {

    @Override
    public Category getDefaultCategory() {
      return Category.CHALLENGE;
    }

    @Override
    public String getTitle() {
      return "race";
    }

    @Override
    public List<Assignment> getAssignments() {
      return List.of(new Assignment("RaceAssignment", "/race/1", List.of()));
    }
  }

  @Autowired private UserProgressRepository userProgressRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @AfterEach
  void cleanUp() {
    userProgressRepository.deleteAll();
  }

  @Test
  void concurrentFirstAttemptsCreateOnlyOneLessonProgress() {
    userProgressRepository.saveAndFlush(new UserProgress(USER));
    var lesson = new RaceLesson();

    // Both requests load the progress before either of them has saved.
    var firstRequest = userProgressRepository.findByUser(USER);
    var secondRequest = userProgressRepository.findByUser(USER);

    firstRequest.assignmentFailed(lesson);
    userProgressRepository.saveAndFlush(firstRequest);

    secondRequest.assignmentFailed(lesson);
    try {
      userProgressRepository.saveAndFlush(secondRequest);
    } catch (ObjectOptimisticLockingFailureException expected) {
      // The losing request fails, as for concurrent updates of one lesson progress.
    }

    assertThat(countLessonProgress(lesson.getId())).isEqualTo(1);
  }

  private long countLessonProgress(String lessonName) {
    try (var em = entityManagerFactory.createEntityManager()) {
      return em.createQuery(
              "select count(lp) from UserProgress u join u.lessonProgress lp"
                  + " where u.user = :user and lp.lessonName = :lesson",
              Long.class)
          .setParameter("user", USER)
          .setParameter("lesson", lessonName)
          .getSingleResult();
    }
  }
}
