package com.org.linkedin.profile.service;

import com.org.linkedin.dto.event.ProfileViewedEvent;
import com.org.linkedin.profile.repo.ProfileViewAnalyticsRepository;
import com.org.linkedin.utility.client.UserService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileViewAnalyticsService {

  private final ProfileViewAnalyticsRepository profileViewAnalyticsRepository;
  private final com.org.linkedin.profile.repo.ProfileViewRepository profileViewRepository;
  private final com.org.linkedin.profile.repo.DailyProfileViewRepository dailyProfileViewRepository;
  private final UserService userService;

  public void recordProfileView(ProfileViewedEvent event) {
    // Fetch viewer details for demographics
    var viewerRes = userService.getUserById(event.getViewerId());
    String designation = "LinkedIn Member";
    String company = "Unknown";

    if (viewerRes != null && viewerRes.getBody() != null && viewerRes.getBody().getData() != null) {
      // Here we'd ideally fetch profile details too, but for now we use designation from User if
      // available
      // Or we could have a Feign client for ProfileService itself or just use common logic
    }

    // Record individual view for history/trends
    com.org.linkedin.profile.domain.ProfileView view =
        com.org.linkedin.profile.domain.ProfileView.builder()
            .viewerId(event.getViewerId())
            .profileOwnerId(event.getProfileOwnerId())
            .viewedAt(LocalDateTime.now())
            .viewerDesignation(designation)
            .viewerCompany(company)
            .build();
    profileViewRepository.save(view);

    // Update daily views
    java.time.LocalDate today = java.time.LocalDate.now();
    dailyProfileViewRepository
        .findByProfileOwnerIdAndViewDate(event.getProfileOwnerId(), today)
        .ifPresentOrElse(
            dv -> {
              dv.setViewCount(dv.getViewCount() + 1);
              dailyProfileViewRepository.save(dv);
            },
            () -> {
              com.org.linkedin.profile.domain.DailyProfileView dv =
                  com.org.linkedin.profile.domain.DailyProfileView.builder()
                      .profileOwnerId(event.getProfileOwnerId())
                      .viewDate(today)
                      .viewCount(1L)
                      .build();
              dailyProfileViewRepository.save(dv);
            });

    // Update aggregate analytics
    profileViewAnalyticsRepository
        .findByProfileOwnerId(event.getProfileOwnerId())
        .ifPresentOrElse(
            analytics -> {
              analytics.setTotalViews(analytics.getTotalViews() + 1);
              analytics.setLastViewedAt(LocalDateTime.now());
              profileViewAnalyticsRepository.save(analytics);
              log.info(
                  "Updated profile view analytics for {}: totalViews={}",
                  event.getProfileOwnerId(),
                  analytics.getTotalViews());
            },
            () -> {
              com.org.linkedin.domain.ProfileViewAnalytics newAnalytics =
                  com.org.linkedin.domain.ProfileViewAnalytics.builder()
                      .profileOwnerId(event.getProfileOwnerId())
                      .totalViews(1L)
                      .lastViewedAt(LocalDateTime.now())
                      .build();
              profileViewAnalyticsRepository.save(newAnalytics);
              log.info(
                  "Created new profile view analytics for {}: totalViews=1",
                  event.getProfileOwnerId());
            });
  }
}
