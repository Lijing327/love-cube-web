package com.lovecube.backend.growth;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum GrowthCampaignTemplate {
    VERIFY_NOW(
            "VERIFY_NOW",
            "完成认证，获得优先推荐",
            "认证后你将获得专属认证标识，并提升推荐曝光，让更多合适的人看到你。",
            "/fellowship/verify"
    ),
    COMPLETE_PROFILE(
            "COMPLETE_PROFILE",
            "完善资料，提升推荐曝光",
            "补全资料后，你的推荐力会提升，还能解锁成长经验、等级和专属徽章。",
            "/fellowship/me"
    ),
    ENABLE_FELLOWSHIP(
            "ENABLE_FELLOWSHIP",
            "开通联谊，进入推荐池",
            "开通联谊后，你将进入推荐池，有机会被更多用户看到。",
            "/fellowship/me"
    ),
    UPLOAD_PHOTOS(
            "UPLOAD_PHOTOS",
            "上传照片，提升吸引力",
            "上传 3 张照片后可获得照片达人徽章，并提升个人主页可信度。",
            "/fellowship/me"
    ),
    CITY_REMINDER(
            "CITY_REMINDER",
            "填写地区，提升同城匹配",
            "填写地区后，系统可以更准确地为你推荐同城或附近用户。",
            "/fellowship/profile/edit"
    ),
    NEARLY_COMPLETE(
            "NEARLY_COMPLETE",
            "再补一项，解锁资料达人",
            "你的资料已经接近完善，再补全缺失项即可获得资料达人徽章和更多推荐权益。",
            "/fellowship/me"
    );

    private final String code;
    private final String title;
    private final String content;
    private final String actionUrl;

    GrowthCampaignTemplate(String code, String title, String content, String actionUrl) {
        this.code = code;
        this.title = title;
        this.content = content;
        this.actionUrl = actionUrl;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    /** 站内通知 linkUrl：去掉 hash 前缀，供 Vue Router 直接 push */
    public String getNotificationLinkUrl() {
        if (actionUrl != null && actionUrl.startsWith("/#/")) {
            return actionUrl.substring(2);
        }
        return actionUrl;
    }

    public static Optional<GrowthCampaignTemplate> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.code.equals(normalized))
                .findFirst();
    }

    public boolean isGoalMet(Map<String, Object> before, Map<String, Object> after) {
        if (before == null || after == null) {
            return false;
        }
        return switch (this) {
            case VERIFY_NOW -> {
                String tier = String.valueOf(after.getOrDefault("verificationTier", "none"));
                yield !"none".equals(tier);
            }
            case COMPLETE_PROFILE -> {
                int beforeRate = toInt(before.get("completionRate"));
                int afterRate = toInt(after.get("completionRate"));
                yield afterRate >= 80 || afterRate > beforeRate;
            }
            case ENABLE_FELLOWSHIP -> Boolean.TRUE.equals(after.get("fellowshipEnabled"));
            case UPLOAD_PHOTOS -> toInt(after.get("photoCount")) >= 3;
            case CITY_REMINDER -> {
                String city = String.valueOf(after.getOrDefault("city", "")).trim();
                yield !city.isEmpty();
            }
            case NEARLY_COMPLETE -> toInt(after.get("completionRate")) >= 100;
        };
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
