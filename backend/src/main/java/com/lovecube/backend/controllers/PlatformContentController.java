package com.lovecube.backend.controllers;

import com.lovecube.backend.entity.Announcement;
import com.lovecube.backend.entity.Article;
import com.lovecube.backend.entity.EventSignup;
import com.lovecube.backend.entity.PlatformEvent;
import com.lovecube.backend.models.User;
import com.lovecube.backend.repository.AnnouncementRepository;
import com.lovecube.backend.repository.ArticleRepository;
import com.lovecube.backend.repository.DynamicRepository;
import com.lovecube.backend.repository.EventSignupRepository;
import com.lovecube.backend.repository.HomeConfigRepository;
import com.lovecube.backend.repository.PlatformEventRepository;
import com.lovecube.backend.repository.UserRepository;
import com.lovecube.backend.services.AdminAuthService;
import com.lovecube.backend.services.GrowthService;
import com.lovecube.backend.services.HomeConfigService;
import com.lovecube.backend.services.DatingEventService;
import com.lovecube.backend.services.EventEngagementService;
import com.lovecube.backend.services.EventSignupService;
import com.lovecube.backend.services.PlatformEventLifecycle;
import com.lovecube.backend.services.PositiveShareService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class PlatformContentController {
    private final AnnouncementRepository announcementRepository;
    private final ArticleRepository articleRepository;
    private final PlatformEventRepository platformEventRepository;
    private final EventSignupRepository eventSignupRepository;
    private final HomeConfigService homeConfigService;
    private final AdminAuthService adminAuthService;
    private final UserRepository userRepository;
    private final HomeConfigRepository homeConfigRepository;
    private final DynamicRepository dynamicRepository;
    private final GrowthService growthService;
    private final PositiveShareService positiveShareService;
    private final EventEngagementService eventEngagementService;
    private final DatingEventService datingEventService;
    private final EventSignupService eventSignupService;

    public PlatformContentController(
            AnnouncementRepository announcementRepository,
            ArticleRepository articleRepository,
            PlatformEventRepository platformEventRepository,
            EventSignupRepository eventSignupRepository,
            HomeConfigService homeConfigService,
            AdminAuthService adminAuthService,
            UserRepository userRepository,
            HomeConfigRepository homeConfigRepository,
            DynamicRepository dynamicRepository,
            GrowthService growthService,
            PositiveShareService positiveShareService,
            EventEngagementService eventEngagementService,
            DatingEventService datingEventService,
            EventSignupService eventSignupService
    ) {
        this.announcementRepository = announcementRepository;
        this.articleRepository = articleRepository;
        this.platformEventRepository = platformEventRepository;
        this.eventSignupRepository = eventSignupRepository;
        this.homeConfigService = homeConfigService;
        this.adminAuthService = adminAuthService;
        this.userRepository = userRepository;
        this.homeConfigRepository = homeConfigRepository;
        this.dynamicRepository = dynamicRepository;
        this.growthService = growthService;
        this.positiveShareService = positiveShareService;
        this.eventEngagementService = eventEngagementService;
        this.datingEventService = datingEventService;
        this.eventSignupService = eventSignupService;
    }

    /**
     * 平台「内容」页聚合：公告、资讯、活动、每日心声（已发布），各取最多 limit 条。
     */
    @GetMapping("/platform/content-feed")
    public Map<String, Object> getPlatformContentFeed(
            @RequestParam(defaultValue = "30") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<Announcement> announcements = announcementRepository.findByStatusPinnedFirst("published").stream()
                .limit(safeLimit)
                .toList();
        List<Article> articles = articleRepository.findByStatusPinnedFirst("published").stream()
                .limit(safeLimit)
                .toList();
        List<PlatformEvent> events = platformEventRepository.findByStatusPinnedFirst("published").stream()
                .limit(safeLimit)
                .toList();
        Long currentUserId = null;
        if (authHeader != null && !authHeader.isBlank()) {
            try {
                currentUserId = adminAuthService.requireUser(authHeader).getUserid();
            } catch (Exception ignored) {
            }
        }
        Map<String, Object> positiveShares = positiveShareService.listShares("latest", 1, safeLimit, currentUserId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("announcements", announcements);
        body.put("articles", articles);
        body.put("events", events);
        body.put("positiveShares", positiveShares);
        return body;
    }

    @GetMapping("/announcements")
    public List<Announcement> listAnnouncements(
            @RequestParam(defaultValue = "published") String status,
            @RequestParam(required = false) String category
    ) {
        List<Announcement> items = announcementRepository.findByStatusPinnedFirst(status);
        if (category != null && !category.isBlank()) {
            items = items.stream().filter(a -> category.equals(a.getCategory())).toList();
        }
        return items;
    }

    @GetMapping("/announcements/{id}")
    public Announcement getAnnouncement(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Announcement item = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公告不存在"));
        item.setViewCount((item.getViewCount() == null ? 0 : item.getViewCount()) + 1);
        announcementRepository.save(item);
        tryRecordViewAction(authHeader, "ANNOUNCEMENT_" + id);
        return item;
    }

    @GetMapping("/articles")
    public List<Article> listArticles(
            @RequestParam(defaultValue = "published") String status,
            @RequestParam(required = false) String category
    ) {
        List<Article> items = articleRepository.findByStatusPinnedFirst(status);
        if (category != null && !category.isBlank()) {
            items = items.stream().filter(a -> category.equals(a.getCategory())).toList();
        }
        return items;
    }

    @GetMapping("/articles/{id}")
    public Article getArticle(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Article item = articleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "资讯不存在"));
        item.setViewCount((item.getViewCount() == null ? 0 : item.getViewCount()) + 1);
        articleRepository.save(item);
        tryRecordViewAction(authHeader, "ARTICLE_" + id);
        return item;
    }

    @PostMapping("/articles/submissions")
    public Map<String, Object> submitArticle(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload
    ) {
        User user = adminAuthService.requireUser(authHeader);
        String title = String.valueOf(payload.getOrDefault("title", "")).trim();
        String summary = String.valueOf(payload.getOrDefault("summary", "")).trim();
        String content = String.valueOf(payload.getOrDefault("content", "")).trim();
        String category = String.valueOf(payload.getOrDefault("category", "平台资讯")).trim();
        if (category.isBlank() || "null".equalsIgnoreCase(category)) {
            category = "平台资讯";
        }
        String tag = resolveArticleTags(payload, category);
        String coverUrl = String.valueOf(payload.getOrDefault("coverUrl", "")).trim();
        if ("null".equalsIgnoreCase(coverUrl)) {
            coverUrl = "";
        }

        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能为空");
        }
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正文不能为空");
        }

        Article article = new Article();
        article.setId("article-" + UUID.randomUUID());
        article.setTitle(title);
        article.setSummary(summary);
        article.setContent(content);
        article.setCategory(category);
        article.setTag(tag.isBlank() ? category : tag);
        article.setCoverUrl(coverUrl);
        article.setStatus("draft");
        article.setPinned(false);
        article.setRecommended(false);
        article.setViewCount(0);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        Article saved = articleRepository.save(article);
        growthService.recordAction(user.getUserid(), "POST_CONTENT", "ARTICLE_SUBMISSION_" + saved.getId());

        return Map.of(
                "id", saved.getId(),
                "title", saved.getTitle(),
                "status", saved.getStatus(),
                "message", "投稿已提交，待管理员审核发布"
        );
    }

    @GetMapping("/events")
    public List<PlatformEvent> listEvents(
            @RequestParam(defaultValue = "published") String status,
            @RequestParam(required = false) String category
    ) {
        List<PlatformEvent> items = platformEventRepository.findByStatusPinnedFirst(status);
        if (category != null && !category.isBlank()) {
            items = items.stream().filter(e -> category.equals(e.getCategory())).toList();
        }
        items.forEach(this::stripOrganizerOnlyFields);
        return items;
    }

    @GetMapping("/events/{id}")
    public PlatformEvent getEvent(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        PlatformEvent item = platformEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "活动不存在"));
        item.setViewCount((item.getViewCount() == null ? 0 : item.getViewCount()) + 1);
        platformEventRepository.save(item);
        tryRecordViewAction(authHeader, "EVENT_" + id);
        stripOrganizerOnlyFields(item);
        return item;
    }

    private void stripOrganizerOnlyFields(PlatformEvent event) {
        if (event != null) {
            event.setCheckinCode(null);
        }
    }

    @PostMapping("/events/{id}/signup")
    @Transactional
    public Map<String, Object> signupEvent(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = adminAuthService.requireUser(authHeader);
        return eventSignupService.signup(user, id);
    }

    @GetMapping("/events/my-signups")
    public List<Map<String, Object>> getMyEventSignups(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = adminAuthService.requireUser(authHeader);
        List<EventSignup> signups = eventSignupRepository.findByUserIdOrderByCreatedAtDesc(user.getUserid());
        List<String> eventIds = signups.stream().map(EventSignup::getEventId).toList();
        Map<String, PlatformEvent> eventMap = platformEventRepository.findAllById(eventIds).stream()
                .collect(java.util.stream.Collectors.toMap(PlatformEvent::getId, e -> e));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EventSignup signup : signups) {
            PlatformEvent event = eventMap.get(signup.getEventId());
            if (event == null) {
                continue;
            }
            rows.add(buildSignupRow(event, signup, user.getUserid()));
        }
        return rows;
    }

    @PostMapping("/events/{id}/checkin")
    @Transactional
    public Map<String, Object> checkinEvent(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = adminAuthService.requireUser(authHeader);
        String code = body != null ? String.valueOf(body.getOrDefault("code", "")) : "";
        return eventEngagementService.checkin(user, id, code);
    }

    @GetMapping("/events/{id}/review-candidates")
    public List<Map<String, Object>> listEventReviewCandidates(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = adminAuthService.requireUser(authHeader);
        return eventEngagementService.listReviewCandidates(user, id);
    }

    @PostMapping("/events/{id}/reviews")
    @Transactional
    public Map<String, Object> submitEventReview(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = adminAuthService.requireUser(authHeader);
        Long targetUserId = parseLong(body.get("targetUserId"));
        Integer rating = body.get("rating") instanceof Number n ? n.intValue() : null;
        String comment = body != null ? String.valueOf(body.getOrDefault("comment", "")) : "";
        return eventEngagementService.submitReview(user, id, targetUserId, rating, comment);
    }

    @GetMapping("/platform/stats")
    public Map<String, Object> getPlatformStats() {
        long userCount = userRepository.count();
        long eventSignupCount = eventSignupRepository.count();
        long dynamicsCount = dynamicRepository.countByIsDeletedFalseAndSceneType("FELLOWSHIP");
        long articleViewCount = articleRepository.sumPublishedViewCount();
        long citiesCount = platformEventRepository.countDistinctPublishedLocations();
        return Map.of(
                "userCount", userCount,
                "eventSignupCount", eventSignupCount,
                "dynamicsCount", dynamicsCount,
                "articleViewCount", articleViewCount,
                "citiesCount", citiesCount
        );
    }

    @GetMapping("/articles/hot-topics")
    public List<Map<String, Object>> getHotTopics(
            @RequestParam(defaultValue = "5") int limit) {
        List<Object[]> rows = articleRepository.findHotTagsWithHeat(PageRequest.of(0, Math.min(limit, 20)));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", i + 1);
            item.put("name", row[0]);
            item.put("heatValue", row[1]);
            result.add(item);
        }
        return result;
    }

    @GetMapping("/users/recommended-authors")
    public List<Map<String, Object>> getRecommendedAuthors(
            @RequestParam(defaultValue = "4") int limit) {
        List<User> admins = userRepository.findByRoleInOrderByCreatedAtAsc(List.of("ADMIN", "SUPER_ADMIN", "ROOT"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : admins.subList(0, Math.min(limit, admins.size()))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getUserid());
            item.put("username", u.getUsername());
            item.put("badge", "官方");
            item.put("description", u.getBio() != null ? u.getBio() : "Love Cube 官方内容团队");
            item.put("avatarUrl", u.getProfilePhoto());
            result.add(item);
        }
        return result;
    }

    @GetMapping("/events/stats")
    public Map<String, Object> getEventsStats() {
        long totalEvents = platformEventRepository.countByStatus("published");
        long totalSignups = eventSignupRepository.count();
        long citiesCount = platformEventRepository.countDistinctPublishedLocations();
        long activeEvents = platformEventRepository.countByStatus("published");
        return Map.of(
                "totalEvents", totalEvents,
                "totalSignups", totalSignups,
                "citiesCount", citiesCount,
                "activeEvents", activeEvents
        );
    }

    @GetMapping("/modules/stats")
    public Map<String, Object> getModulesStats() {
        long totalModules = homeConfigRepository.countByConfigGroup("module");
        long activeModules = homeConfigRepository.countByConfigGroupAndEnabledTrue("module");
        return Map.of(
                "totalModules", totalModules,
                "activeModules", activeModules
        );
    }

    @GetMapping("/home/config")
    public Map<String, Object> getHomeConfig() {
        return homeConfigService.getPublicHomeConfig();
    }

    @GetMapping("/admin/reserved")
    public Map<String, Object> adminReserved() {
        return Map.of(
                "announcements", "/api/admin/announcements",
                "articles", "/api/admin/articles",
                "events", "/api/admin/events",
                "users", "/api/admin/users",
                "verifications", "/api/admin/verifications",
                "reports", "/api/admin/reports"
        );
    }

    private void tryRecordViewAction(String authHeader, String bizId) {
        if (authHeader == null || authHeader.isBlank()) {
            return;
        }
        try {
            User user = adminAuthService.requireUser(authHeader);
            growthService.recordAction(user.getUserid(), "VIEW_CONTENT", bizId);
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> buildSignupRow(PlatformEvent event, EventSignup signup, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("eventId", event.getId());
        row.put("title", event.getTitle());
        row.put("summary", event.getSummary() == null ? "" : event.getSummary());
        row.put("location", event.getLocation() == null ? "" : event.getLocation());
        row.put("eventTime", event.getEventTime());
        row.put("endTime", event.getEndTime());
        row.put("effectiveEndTime", PlatformEventLifecycle.resolveEnd(event));
        row.put("signupAt", signup.getCreatedAt());
        row.put("checkedIn", Boolean.TRUE.equals(signup.getCheckedIn()));
        row.put("checkedInAt", signup.getCheckedInAt());
        row.put("checkinEnabled", event.getCheckinCode() != null && !event.getCheckinCode().isBlank());
        boolean checkedIn = Boolean.TRUE.equals(signup.getCheckedIn());
        boolean eventEnded = PlatformEventLifecycle.isEnded(event, now);
        boolean eventStarted = PlatformEventLifecycle.hasStarted(event, now);
        Map<String, Object> reviewSummary = eventEngagementService.buildReviewSummary(
                userId, event.getId(), checkedIn, eventEnded);
        row.put("canReview", Boolean.TRUE.equals(reviewSummary.get("canReview")));
        row.put("pendingReviewCount", reviewSummary.get("pendingReviewCount"));
        row.put("reviewCompleted", reviewSummary.get("reviewCompleted"));
        row.put("eventEnded", eventEnded);
        row.put("eventStarted", eventStarted);
        row.put("eventOngoing", PlatformEventLifecycle.isOngoing(event, now));
        boolean reviewCompleted = Boolean.TRUE.equals(reviewSummary.get("reviewCompleted"));
        row.put("status", PlatformEventLifecycle.resolveParticipationStatus(
                checkedIn, event, now, reviewCompleted));
        datingEventService.enrichSignupRow(row, event, userId);
        return row;
    }

    private String resolveArticleTags(Map<String, Object> payload, String fallback) {
        java.util.LinkedHashSet<String> tags = new java.util.LinkedHashSet<>();
        Object tagsObj = payload.get("tags");
        if (tagsObj instanceof java.util.Collection<?> collection) {
            for (Object item : collection) {
                addNormalizedTag(tags, item == null ? "" : String.valueOf(item));
            }
        }
        if (tags.isEmpty()) {
            Object rawTag = payload.get("tag");
            if (rawTag != null && !"null".equalsIgnoreCase(String.valueOf(rawTag))) {
                for (String part : String.valueOf(rawTag).split("[,，]")) {
                    addNormalizedTag(tags, part);
                }
            }
        }
        if (tags.isEmpty()) {
            return fallback;
        }
        return String.join(",", tags);
    }

    private void addNormalizedTag(java.util.Set<String> tags, String raw) {
        if (tags.size() >= 5) {
            return;
        }
        String tag = raw == null ? "" : raw.trim();
        if (tag.isEmpty() || "null".equalsIgnoreCase(tag)) {
            return;
        }
        if (tag.length() > 12) {
            tag = tag.substring(0, 12);
        }
        tags.add(tag);
    }

    private Long parseLong(Object value) {
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
