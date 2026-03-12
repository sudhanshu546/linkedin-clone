package com.org.linkedin.constants;

/** Application constants. */
public final class Constants {

  public static final String LOGIN_REGEX =
      "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$";
  public static final String PASSWORD_CRITERIA =
      "Password must contain at least 1 uppercase letter, 1 digit, and 1 special character";
  public static final String PASSWORD_LENGTH_MESSAGE =
      "Password length should be between 6 and 15 characters";
  public static final String OPERATION_SUCCESSFUL = "OPERATION_SUCCESSFUL";
  public static final String POLICY_PREFIX = "POLICY_";
  public static final String PERMISSION_PREFIX = "PERMISSION_";
  public static final String TOKEN_AUTHORIZATION_FIELD = "authorization";
  public static final String TOKEN_RESOURCE_FIELD = "rsname";
  public static final String TOKEN_PERMISSION_FIELD = "permissions";
  public static final String TOKEN_SCOPE_FIELD = "scopes";
  public static final String AUTHORITY_SCOPE_FIELD = "SCOPE_";
  public static final String AUTHORITY_RESOURCE_FIELD = "RESOURCE_";
  public static final String SERVICE_TIMEZONE = "UTC";
  public static final String TOKEN_SUBJECT = "sub";
  public static final String TENANT_ID = "tenant_Id";

  public static final String USER_ROLE = "ROLE_User";

  public static final String USER_TENANT = "ROLE_Tenant";

  public static final String DEVICE_KEY = "DEVICE";

  public static final String REDIS_KEY = "REDIS-KEY";

  public static final String JASPER_TEMPLATE_FORMAT = ".jrxml";
  public static final String FORWARD_SLASH = "/";
  public static final String TEMPLATE_FOLDER = "templates";

  public static final String TENANT = "tenant";

  public static final String VOLUME_TOTALIZER = "VOLUME_TOTALIZER";

  public static final String AMOUNT_TOTALIZER = "AMOUNT_TOTALIZER";

  private Constants() {}
}
