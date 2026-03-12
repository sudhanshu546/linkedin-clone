package com.org.linkedin.profile.service;

import com.org.linkedin.domain.ProfileViewAnalytics;
import com.org.linkedin.dto.event.ProfileViewedEvent;
import com.org.linkedin.profile.repo.ProfileViewAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileViewAnalyticsService {

    private final ProfileViewAnalyticsRepository profileViewAnalyticsRepository;
    private final com.org.linkedin.profile.repo.ProfileViewRepository profileViewRepository;

    public void recordProfileView(ProfileViewedEvent event) {
        // Record individual view for history/trends
        com.org.linkedin.profile.domain.ProfileView view = com.org.linkedin.profile.domain.ProfileView.builder()
                .viewerId(event.getViewerId())
                .profileOwnerId(event.getProfileOwnerId())
                .viewedAt(LocalDateTime.now())
                .build();
        profileViewRepository.save(view);

        // Update aggregate analytics
        profileViewAnalyticsRepository.findByProfileOwnerId(event.getProfileOwnerId())
                .ifPresentOrElse(
                        analytics -> {
                            analytics.setTotalViews(analytics.getTotalViews() + 1);
                            analytics.setLastViewedAt(LocalDateTime.now());
                            profileViewAnalyticsRepository.save(analytics);
                            log.info("Updated profile view analytics for {}: totalViews={}", event.getProfileOwnerId(), analytics.getTotalViews());
                        },
                        () -> {
                            com.org.linkedin.domain.ProfileViewAnalytics newAnalytics = com.org.linkedin.domain.ProfileViewAnalytics.builder()
                                    .profileOwnerId(event.getProfileOwnerId())
                                    .totalViews(1L)
                                    .lastViewedAt(LocalDateTime.now())
                                    .build();
                            profileViewAnalyticsRepository.save(newAnalytics);
                            log.info("Created new profile view analytics for {}: totalViews=1", event.getProfileOwnerId());
                        }
                );
    }
}
