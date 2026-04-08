package com.org.linkedin.utility;

/** Global project constants to avoid hardcoded strings in service layers. */
public interface ProjectConstants {

  // User Roles
  String ROLE_USER = "USER";
  String ROLE_ADMIN = "ADMIN";

  // Kafka Topics
  String TOPIC_POST_HASHTAGS = "post-hashtags";
  String TOPIC_USER_UPDATED = "user-updated-topic";
  String TOPIC_JOB_UPDATED = "job-updated-topic";
  String TOPIC_JOB_APP_STATUS_UPDATED = "job-application-status-updated";

  // Kafka Groups
  String GROUP_SEARCH = "search-group";
  String GROUP_NOTIFICATION = "notification-service-group";
  String GROUP_PROFILE_FEED = "profile-service-feed-group";

  // STOMP Destinations
  String DEST_PRESENCE = "/topic/presence";
  String DEST_MESSAGES = "/queue/messages";
  String DEST_EVENTS = "/queue/events";

  // Event Actions
  String ACTION_CREATE = "CREATE";
  String ACTION_UPDATE = "UPDATE";
  String ACTION_DELETE = "DELETE";

  // Default Labels
  String DEFAULT_DESIGNATION = "LinkedIn Member";
  String DEFAULT_JOB_TITLE = "Unknown Job";

  // Statuses
  String STATUS_PENDING = "PENDING";
  String STATUS_ACCEPTED = "ACCEPTED";
  String STATUS_REJECTED = "REJECTED";

  // Error Messages
  String ERROR_USER_NOT_FOUND = "User not found";
  String ERROR_POST_NOT_FOUND = "Post not found";
  String ERROR_COMMENT_NOT_FOUND = "Comment not found";
  String ERROR_PARENT_COMMENT_NOT_FOUND = "Parent comment not found";
  String ERROR_OPTION_NOT_FOUND = "Option not found";
  String ERROR_NOT_A_POLL = "Post is not a poll";
  String ERROR_POLL_EXPIRED = "Poll has expired";
  String ERROR_ALREADY_VOTED = "User already voted";
  String ERROR_OPTION_MISMATCH = "Option does not belong to this poll";
  String ERROR_UNAUTHORIZED_DELETE_COMMENT = "Unauthorized to delete this comment";
  String ERROR_UNAUTHORIZED_DELETE_POST = "Unauthorized to delete this post";
  String ERROR_UNAUTHORIZED_UPDATE_POST = "Unauthorized to update this post";
  String ERROR_CANNOT_BLOCK_SELF = "Cannot block yourself";
  String ERROR_EMAIL_NULL = "Email cannot be null";
  String ERROR_JOB_NOT_FOUND = "Job not found";
  String ERROR_APP_NOT_FOUND = "Application not found";
}
