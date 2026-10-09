package com.wildlife.wildlife_conservationbackend.utility;

public final class EndPoint {
    public static final String BASE = "/api/v1";
    public static final String LOGIN = "/auth/login";
    public static final String REGISTER = "/auth/register";
    public static final String REGISTRATION_PARKS = "/auth/registration-parks";
    public static final String CHANGE_PASSWORD = "/auth/change-password";
    public static final String PROFILE = "/auth/me";
    public static final String PARKS = "/parks";
    public static final String USERS = "/users";
    public static final String ROUTES = "/patrol-routes";
    public static final String ASSIGNMENTS = "/patrol-assignments";
    public static final String PATROLS = "/patrols";
    public static final String MEDIA = "/media";
    public static final String INCIDENTS = "/incidents";
    public static final String ALERTS = "/alerts";
    public static final String ANALYTICS = "/analytics/summary";
    public static final String REPORTS = "/reports";
    public static final String COMMUNITY_REPORTS = "/community-reports";
    public static final String CAMERA_TRAPS = "/camera-traps";
    public static final String CAMERA_IMAGES = "/camera-trap-images";

    private EndPoint() { }
}
