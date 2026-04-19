package com.org.linkedin.search.consumer;

import com.org.linkedin.dto.event.JobUpdatedEvent;
import com.org.linkedin.dto.event.PostCreatedEvent;
import com.org.linkedin.dto.event.PostDeletedEvent;
import com.org.linkedin.dto.event.UserUpdatedEvent;
import com.org.linkedin.search.document.JobDocument;
import com.org.linkedin.search.document.PostDocument;
import com.org.linkedin.search.document.UserDocument;
import com.org.linkedin.search.repository.JobRepository;
import com.org.linkedin.search.repository.PostRepository;
import com.org.linkedin.search.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import static com.org.linkedin.utility.ProjectConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchConsumer {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final PostRepository postRepository;
    private final com.org.linkedin.search.repository.HashtagRepository hashtagRepository;

    @KafkaListener(topics = TOPIC_POST_HASHTAGS, groupId = GROUP_SEARCH)
    public void consumeHashtags(java.util.List<String> tags) {
        log.info("Received hashtags: {}", tags);
        for (String tag : tags) {
            com.org.linkedin.search.document.HashtagDocument doc = hashtagRepository.findById(tag)
                    .orElse(com.org.linkedin.search.document.HashtagDocument.builder()
                            .tag(tag)
                            .count(0L)
                            .build());
            doc.setCount(doc.getCount() + 1);
            doc.setLastUpdated(System.currentTimeMillis());
            hashtagRepository.save(doc);
        }
    }

    @KafkaListener(topics = "post-created", groupId = GROUP_SEARCH)
    public void consumePostCreated(@Payload PostCreatedEvent event) {
        log.info("Received PostCreatedEvent: {}", event.getPostId());
        PostDocument doc = PostDocument.builder()
                .id(event.getPostId())
                .authorId(event.getUserId())
                .authorName(event.getUserName())
                .userProfileImageUrl(event.getUserProfileImageUrl())
                .content(event.getContent())
                .imageUrl(event.getImageUrl())
                .imageUrls(event.getImageUrls())
                .isPoll(event.isPoll())
                .pollQuestion(event.getPollQuestion())
                .pollOptions(event.getPollOptions())
                .build();
        postRepository.save(doc);
        log.info("Indexed post document: {}", doc.getId());
    }

    @KafkaListener(topics = "post-deleted", groupId = GROUP_SEARCH)
    public void consumePostDeleted(@Payload PostDeletedEvent event) {
        log.info("Received PostDeletedEvent: {}", event);
        postRepository.deleteById(event.getPostId());
        log.info("Deleted post document: {}", event.getPostId());
    }

    @KafkaListener(topics = TOPIC_USER_UPDATED, groupId = GROUP_SEARCH)
    public void consumeUserUpdated(UserUpdatedEvent event) {
        log.info("Received UserUpdatedEvent for user: {} with action: {}", event.getId(), event.getAction());
        if (ACTION_DELETE.equals(event.getAction())) {
            userRepository.deleteById(event.getId().toString());
            log.info("Deleted user document: {}", event.getId());
        } else {
            UserDocument doc = UserDocument.builder()
                    .id(event.getId().toString())
                    .firstName(event.getFirstName())
                    .lastName(event.getLastName())
                    .email(event.getEmail())
                    .headline(event.getHeadline())
                    .skills(event.getSkills())
                    .city(event.getCity())
                    .state(event.getState())
                    .currentCompany(event.getCurrentCompany())
                    .designation(event.getDesignation())
                    .profileImageUrl(event.getProfileImageUrl())
                    .build();
            userRepository.save(doc);
            log.info("Indexed user document: {}", doc.getId());
        }
    }

    @KafkaListener(topics = TOPIC_JOB_UPDATED, groupId = GROUP_SEARCH)
    public void consumeJobUpdated(JobUpdatedEvent event) {
        log.info("Received JobUpdatedEvent for job: {}", event.getJob().getId());
        if (ACTION_DELETE.equals(event.getAction())) {
            jobRepository.deleteById(event.getJob().getId().toString());
            log.info("Deleted job document: {}", event.getJob().getId());
        } else {
            JobDocument doc = JobDocument.builder()
                    .id(event.getJob().getId().toString())
                    .title(event.getJob().getTitle())
                    .description(event.getJob().getDescription())
                    .company(event.getJob().getCompany())
                    .location(event.getJob().getLocation())
                    .jobType(event.getJob().getJobType())
                    .postedDate(event.getJob().getPostedDate())
                    .build();
            jobRepository.save(doc);
            log.info("Indexed job document: {}", doc.getId());
        }
    }
}
