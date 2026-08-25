package com.lovecube.backend.services;

import com.lovecube.backend.entity.UserNotification;
import com.lovecube.backend.entity.UserNotificationSetting;
import com.lovecube.backend.notification.NotificationCatalog;
import com.lovecube.backend.repository.UserNotificationRepository;
import com.lovecube.backend.repository.UserNotificationSettingRepository;
import com.lovecube.backend.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通知系统唯一入口：业务侧创建与变更用户消息须通过本类，禁止绕过本类直接写 {@code user_notifications}。
 * <p>对外创建请优先使用 {@link #createNotification} 或 {@link #send}；微信占位逻辑委托
 * {@link NotificationDispatchService#applyPushInMemory}，由本类统一 {@code save}。</p>
 */
@Service
@SuppressWarnings("deprecation") // UserNotificationRepository：框架层唯一合法注入点
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final int ANNOUNCEMENT_USER_BATCH = 500;
    private static final int ANNOUNCEMENT_MAX_USERS = 5000;

    private final UserNotificationRepository userNotificationRepository;
    private final UserNotificationSettingRepository settingRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final UserRepository userRepository;

    public NotificationService(
            UserNotificationRepository userNotificationRepository,
            UserNotificationSettingRepository settingRepository,
            NotificationDispatchService notificationDispatchService,
            UserRepository userRepository
    ) {
        this.userNotificationRepository = userNotificationRepository;
        this.settingRepository = settingRepository;
        this.notificationDispatchService = notificationDispatchService;
        this.userRepository = userRepository;
    }

    /**
     * 联谊资料审核结果：管理端审核流接入后在此调用或直接使用 {@link #createNotification}。
     */
    @Transactional
    public void notifyMatchProfileReviewResult(Long userId, boolean approved, String reason) {
        if (approved) {
            createNotification(userId, NotificationCatalog.TYPE_MATCH_PROFILE_REVIEW_PASSED,
                    "联谊资料审核通过",
                    "你的资料已通过审核，可在匹配中正常展示。",
                    "/fellowship/profile",
                    "USER",
                    String.valueOf(userId));
        } else {
            String r = reason != null && !reason.isBlank() ? reason : "请根据提示修改后重新提交。";
            createNotification(userId, NotificationCatalog.TYPE_MATCH_PROFILE_REVIEW_REJECTED,
                    "联谊资料审核未通过",
                    "审核未通过：" + r,
                    "/fellowship/profile/edit",
                    "USER",
                    String.valueOf(userId));
        }
    }

    /**
     * 业务通知创建<strong>唯一入口</strong>（含 linkUrl）；站内开关关闭时不落库。
     */
    @Transactional
    public UserNotification createNotification(Long userId, String type, String title, String content,
                                               String linkUrl, String relatedType, String relatedId) {
        if (userId == null || type == null || title == null || content == null) {
            return null;
        }
        UserNotificationSetting setting = settingRepository.findByUserIdAndType(userId, type).orElse(null);
        boolean siteOn = setting == null ? NotificationCatalog.defaultSiteEnabled(type) : Boolean.TRUE.equals(setting.getSiteEnabled());
        if (!siteOn) {
            return null;
        }
        UserNotification n = new UserNotification();
        n.setUserId(userId);
        n.setType(type);
        n.setLevel(NotificationCatalog.resolveLevel(type));
        n.setTitle(title);
        n.setContent(content);
        n.setLinkUrl(linkUrl);
        n.setRelatedType(relatedType);
        n.setRelatedId(relatedId);
        n.setIsRead(false);
        n.setPushChannel(NotificationCatalog.CHANNEL_SITE);
        notificationDispatchService.applyPushInMemory(n);
        UserNotification saved = userNotificationRepository.save(n);
        try {
            notificationDispatchService.scheduleExternalDispatch(saved.getId());
        } catch (Exception e) {
            log.warn("外发调度提交失败 notificationId={} userId={}: {}", saved.getId(), userId, e.getMessage());
        }
        return saved;
    }

    /**
     * 渠道设置页「发送测试通知」：创建一条系统测试站内消息并触发邮件 / PushPlus 投递。
     */
    @Transactional
    public UserNotification sendChannelTestNotification(Long userId) {
        return createNotification(
                userId,
                NotificationCatalog.TYPE_NOTIFICATION_CHANNEL_TEST,
                "测试通知",
                "这是一条测试通知，用于验证邮件与 PushPlus 渠道是否配置正确。",
                "/messages",
                "SYSTEM",
                "CHANNEL_TEST"
        );
    }

    /**
     * 业务通知创建<strong>唯一入口</strong>（兼容旧代码，无 linkUrl）。
     */
    @Transactional
    public void send(Long userId, String type, String title, String content, String relatedType, String relatedId) {
        createNotification(userId, type, title, content, null, relatedType, relatedId);
    }

    @Transactional(readOnly = true)
    public boolean hasNotification(Long userId, String type, String relatedType, String relatedId) {
        if (userId == null || type == null || relatedType == null || relatedId == null) {
            return false;
        }
        return userNotificationRepository.existsByUserIdAndTypeAndRelatedTypeAndRelatedId(
                userId, type, relatedType, relatedId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        userNotificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId) && !Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                n.setReadAt(LocalDateTime.now());
                userNotificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        userNotificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    public long getUnreadCount(Long userId) {
        return userNotificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public Page<UserNotification> getUserNotifications(Long userId, int page, int size, Boolean read, String tab, String singleType) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (singleType != null && !singleType.isBlank()) {
            Specification<UserNotification> spec = (root, query, cb) -> {
                List<Predicate> ps = new ArrayList<>();
                ps.add(cb.equal(root.get("userId"), userId));
                ps.add(cb.equal(root.get("type"), singleType.trim()));
                if (read != null) {
                    ps.add(cb.equal(root.get("isRead"), read));
                }
                return cb.and(ps.toArray(Predicate[]::new));
            };
            return userNotificationRepository.findAll(spec, pageable);
        }
        Specification<UserNotification> spec = buildSpec(userId, read, tab);
        return userNotificationRepository.findAll(spec, pageable);
    }

    @Transactional
    public boolean deleteForUser(Long userId, Long id) {
        return userNotificationRepository.deleteForUser(id, userId) > 0;
    }

    @Transactional
    public int deleteByTypeForUser(Long userId, String type) {
        if (userId == null || type == null || type.isBlank()) {
            return 0;
        }
        return userNotificationRepository.deleteByUserIdAndType(userId, type.trim());
    }

    /**
     * 平台重要公告：向活跃用户批量发送站内消息（单请求有上限）。
     */
    @Transactional
    public int broadcastPlatformAnnouncement(String title, String summary, String announcementId, String linkUrl) {
        return broadcastToActiveUsers(
                NotificationCatalog.TYPE_PLATFORM_ANNOUNCEMENT,
                title,
                summary,
                linkUrl != null ? linkUrl : "/platform/announcements",
                "ANNOUNCEMENT",
                announcementId
        );
    }

    /**
     * 平台活动发布：向活跃用户推送站内活动通知（报名入口指向活动详情）。
     */
    @Transactional
    public int broadcastPlatformEventPublished(String title, String summary, String eventId, String linkUrl) {
        return broadcastToActiveUsers(
                NotificationCatalog.TYPE_PLATFORM_EVENT_PUBLISHED,
                title,
                summary,
                linkUrl != null ? linkUrl : "/events/" + eventId,
                "EVENT",
                eventId
        );
    }

    private int broadcastToActiveUsers(
            String type,
            String title,
            String summary,
            String linkUrl,
            String relatedType,
            String relatedId
    ) {
        int sent = 0;
        int pageIdx = 0;
        String body = summary != null && !summary.isBlank() ? summary : title;
        while (sent < ANNOUNCEMENT_MAX_USERS) {
            Page<Long> ids = userRepository.findActiveUserIds(PageRequest.of(pageIdx++, ANNOUNCEMENT_USER_BATCH));
            if (!ids.hasContent()) break;
            for (Long uid : ids.getContent()) {
                if (sent >= ANNOUNCEMENT_MAX_USERS) break;
                createNotification(uid, type, title, body, linkUrl, relatedType, relatedId);
                sent++;
            }
            if (!ids.hasNext()) break;
        }
        return sent;
    }

    private Specification<UserNotification> buildSpec(Long userId, Boolean read, String tab) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("userId"), userId));
            if (read != null) {
                ps.add(cb.equal(root.get("isRead"), read));
            }
            String t = tab == null ? "ALL" : tab.trim();
            if (!t.isEmpty() && !"ALL".equalsIgnoreCase(t)) {
                if ("UNREAD".equalsIgnoreCase(t)) {
                    ps.add(cb.isFalse(root.get("isRead")));
                } else {
                    Predicate tabPred = tabPredicate(root, cb, t.toUpperCase(Locale.ROOT));
                    if (tabPred != null) ps.add(tabPred);
                }
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }

    private Predicate tabPredicate(Root<UserNotification> root, jakarta.persistence.criteria.CriteriaBuilder cb, String tabUpper) {
        jakarta.persistence.criteria.Path<String> typePath = root.get("type");
        jakarta.persistence.criteria.Path<String> levelPath = root.get("level");
        return switch (tabUpper) {
            case "GROUP" -> cb.or(
                    cb.equal(typePath, NotificationCatalog.TYPE_GROUP_JOIN_REQUEST),
                    cb.equal(typePath, NotificationCatalog.TYPE_GROUP_APPLICATION_APPROVED),
                    cb.equal(typePath, NotificationCatalog.TYPE_GROUP_APPLICATION_REJECTED),
                    cb.equal(typePath, "GROUP_POST_CREATED"),
                    cb.like(typePath, "GROUP_CHECKIN%"),
                    cb.like(typePath, "GROUP_TASK%"),
                    cb.like(typePath, "GROUP_ACTIVITY%")
            );
            case "REVIEW" -> cb.or(
                    cb.like(typePath, "%MODERATION%"),
                    cb.like(typePath, "%MATCH_PROFILE_REVIEW%"),
                    cb.equal(typePath, NotificationCatalog.TYPE_GROUP_APPLICATION_REJECTED)
            );
            case "INTERACTION" -> cb.or(
                    cb.equal(typePath, NotificationCatalog.TYPE_PROFILE_LIKED),
                    cb.equal(typePath, NotificationCatalog.TYPE_PROFILE_VIEWED),
                    cb.equal(typePath, NotificationCatalog.TYPE_CONTENT_LIKED),
                    cb.equal(typePath, NotificationCatalog.TYPE_CONTENT_COMMENTED),
                    cb.equal(typePath, NotificationCatalog.TYPE_USER_FOLLOWED),
                    cb.equal(typePath, "GROUP_POST_LIKED"),
                    cb.equal(typePath, "GROUP_POST_COMMENTED"),
                    cb.equal(typePath, "LIKE"),
                    cb.equal(typePath, NotificationCatalog.TYPE_MUTUAL_MATCH)
            );
            case "ANNOUNCEMENT" -> cb.equal(typePath, NotificationCatalog.TYPE_PLATFORM_ANNOUNCEMENT);
            case "SYSTEM" -> cb.or(
                    cb.equal(levelPath, NotificationCatalog.LEVEL_SYSTEM),
                    cb.equal(typePath, "REPORT_HANDLED"),
                    cb.equal(typePath, "BANNED"),
                    cb.equal(typePath, "SYSTEM"),
                    cb.equal(typePath, "MESSAGE")
            );
            default -> null;
        };
    }

    /** 管理端工作台：通知体量汇总（只读统计）。 */
    public Map<String, Long> getAdminDashboardNotificationStats(LocalDateTime todayStart) {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("totalNotifications", userNotificationRepository.count());
        m.put("unreadNotifications", userNotificationRepository.countByIsReadFalse());
        m.put("todayNotifications", userNotificationRepository.countByCreatedAtGreaterThanEqual(todayStart));
        return m;
    }
}
