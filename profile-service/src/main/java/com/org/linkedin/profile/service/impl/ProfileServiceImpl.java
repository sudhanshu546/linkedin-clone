package com.org.linkedin.profile.service.impl;

import com.org.linkedin.domain.Profile;
import com.org.linkedin.dto.ProfileDTO;
import com.org.linkedin.dto.event.ProfileViewedEvent;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.profile.mapper.ProfileMapper;
import com.org.linkedin.profile.repo.ProfileRepo;
import com.org.linkedin.profile.service.ProfileService;
import com.org.linkedin.utility.client.UserService;
import com.org.linkedin.utility.service.KafkaEventPublisher;
import com.org.linkedin.utility.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileMapper profileMapper;
    private final ProfileRepo profileRepo;
    private final UserService userService;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final FileStorageService fileStorageService;

    @Value("${kafka.topics.profile-viewed}")
    private String profileViewedTopic;

    @Override
    public ProfileDTO getByUserId(UUID userId) {
        Profile profile = profileRepo.findByUserId(userId);

        // Publish Profile Viewed Event
        String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUserName != null && !currentUserName.equals("anonymousUser")) {
             try {
                 UUID viewerId = userService.getUserByKeyCloakId(UUID.fromString(currentUserName)).getBody().getResult().getId();
                 if (!viewerId.equals(userId)) {
                     ProfileViewedEvent event = ProfileViewedEvent.builder()
                             .viewerId(viewerId)
                             .profileOwnerId(userId)
                             .timestamp(LocalDateTime.now())
                             .build();
                     kafkaEventPublisher.publishEvent(profileViewedTopic, event);
                 }
             } catch (Exception e) {
                 // Log error but don't fail the request
             }
        }

        return profileMapper.toDto(profile);
    }

    @Override
    public ProfileDTO saveOrUpdate(UUID userId, ProfileDTO profileDTO) {
        TUserDTO tUserDTO  = userService.getUserByKeyCloakId(userId).getBody().getResult();
        Profile existingProfile = profileRepo.findByUserId(tUserDTO.getId());
        Profile profile;
        if (existingProfile != null) {
            profileMapper.partialUpdate(existingProfile, profileDTO);
            profile = existingProfile;
        } else {
            profile = profileMapper.toEntity(profileDTO);
            profile.setUserId(tUserDTO.getId());
        }
        profile = profileRepo.save(profile);
        return profileMapper.toDto(profile);
    }

    @Override
    public List<ProfileDTO> searchProfiles(String query, String city, String state, String company, String headline, String sortBy, String sortOrder) {
        return profileRepo.searchProfiles(query, city, state, company, headline, sortBy)
                .stream()
                .map(profileMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public String updateCoverImage(UUID userId, org.springframework.web.multipart.MultipartFile image) throws IOException {
        TUserDTO tUserDTO  = userService.getUserByKeyCloakId(userId).getBody().getResult();
        Profile profile = profileRepo.findByUserId(tUserDTO.getId());
        if (profile == null) {
            throw new RuntimeException("Profile not found");
        }
        String imageUrl = fileStorageService.storeFile(image);
        profile.setCoverImageUrl(imageUrl);
        profileRepo.save(profile);
        return imageUrl;
    }
}
