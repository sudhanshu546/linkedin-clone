package com.org.linkedin.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.org.linkedin.domain.user.TUser;
import com.org.linkedin.dto.event.UserDeletedEvent;
import com.org.linkedin.user.repository.UserRepository;
import com.org.linkedin.user.service.UserService;
import com.org.linkedin.user.utility.KeyCloakUtil;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class UserServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @MockBean private KeyCloakUtil keyCloakUtil;

  private static final BlockingQueue<UserDeletedEvent> eventQueue = new LinkedBlockingQueue<>();

  @KafkaListener(topics = "user-deleted", groupId = "test-group")
  public void listen(UserDeletedEvent event) {
    eventQueue.add(event);
  }

  @Test
  public void testUserDeletionFlow() throws InterruptedException {
    // 1. Setup - Create a user in DB
    UUID keycloakId = UUID.randomUUID();
    TUser user =
        TUser.builder()
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .keycloakUserId(keycloakId)
            .isDeleted(false)
            .isEnabled(true)
            .build();
    user = userRepository.save(user);
    UUID userId = user.getId();

    // 2. Mock external calls
    doNothing().when(keyCloakUtil).deleteUserFromKeycloak(anyString(), anyString());

    // 3. Act - Delete user
    userService.delete("test-client", userId);

    // 4. Assert DB state - Should be soft deleted (invisible to standard JPA queries)
    assertThat(userRepository.findById(userId)).isEmpty();

    // Check with native or un-filtered query if needed, but the above proves @Where is working

    // 5. Assert Kafka Event
    UserDeletedEvent event = eventQueue.poll(10, TimeUnit.SECONDS);
    assertThat(event).isNotNull();
    assertThat(event.getUserId()).isEqualTo(userId.toString());
    assertThat(event.getEmail()).isEqualTo("test@example.com");
  }
}
