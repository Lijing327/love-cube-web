package com.lovecube.backend.controllers;

import com.lovecube.backend.entity.UserNotification;
import com.lovecube.backend.models.User;
import com.lovecube.backend.services.GrowthCampaignService;
import com.lovecube.backend.services.NotificationService;
import com.lovecube.backend.services.NotificationSettingService;
import com.lovecube.backend.services.UnifiedProfileService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSettingService notificationSettingService;
    private final UnifiedProfileService unifiedProfileService;
    private final GrowthCampaignService growthCampaignService;

    public NotificationController(NotificationService notificationService,
                                  NotificationSettingService notificationSettingService,
                                  UnifiedProfileService unifiedProfileService,
                                  GrowthCampaignService growthCampaignService) {
        this.notificationService = notificationService;
        this.notificationSettingService = notificationSettingService;
        this.unifiedProfileService = unifiedProfileService;
        this.growthCampaignService = growthCampaignService;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit) {
        try {
            User user = unifiedProfileService.requireCurrentUser(authHeader);
            int safePage = page;
            int safeSize = size;
            if (limit != null && limit > 0 && (type != null && !type.isBlank())) {
                safePage = 0;
                safeSize = Math.min(200, Math.max(1, limit));
            }
            if (safePage == 0) {
                notificationSettingService.ensureDefaultRows(user.getUserid());
            }
            Page<UserNotification> pg = notificationService.getUserNotifications(
                    user.getUserid(), safePage, safeSize, read, tab, type);
            List<Map<String, Object>> items = pg.getContent().stream().map(this::toApiRow).collect(Collectors.toList());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("items", items);
            body.put("content", items);
            body.put("total", pg.getTotalElements());
            body.put("page", pg.getNumber());
            body.put("pageSize", pg.getSize());
            body.put("totalPages", pg.getTotalPages());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = unifiedProfileService.requireCurrentUser(authHeader);
            notificationSettingService.ensureDefaultRows(user.getUserid());
            long count = notificationService.getUnreadCount(user.getUserid());
            return ResponseEntity.ok(Map.of("count", count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> markReadPut(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        try {
            User user = unifiedProfileService.requireCurrentUser(authHeader);
            notificationService.markAsRead(user.getUserid(), id);
            return ResponseEntity.ok(Map.of("message", "已标记已读"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> readAll(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = unifiedProfileService.requireCurrentUser(authHeader);
            notificationService.markAllAsRead(user.getUserid());
            return ResponseEntity.ok(Map.of("message", "全部已读"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOne(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        try {
            User user = unifiedProfileService.requireCurrentUser(authHeader);
            boolean ok = notificationService.deleteForUser(user.getUserid(), id);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "消息不存在"));
            }
            return ResponseEntity.ok(Map.of("message", "已删除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/by-type")
    public ResponseEntity<?> deleteByType(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String type) {
        try {
            User user = unifiedProfileService.requireCurrentUser(authHeader);
            if (type == null || type.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "缺少通知类型"));
            }
            int removed = notificationService.deleteByTypeForUser(user.getUserid(), type);
            return ResponseEntity.ok(Map.of("message", "已删除", "removed", removed));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    private Map<String, Object> toApiRow(UserNotification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("userId", n.getUserId());
        m.put("type", n.getType());
        m.put("level", n.getLevel());
        m.put("title", n.getTitle());
        m.put("content", n.getContent());
        m.put("linkUrl", n.getLinkUrl());
        m.put("relatedType", n.getRelatedType());
        m.put("relatedId", n.getRelatedId());
        m.put("targetType", n.getRelatedType());
        m.put("targetId", n.getRelatedId());
        m.put("isRead", Boolean.TRUE.equals(n.getIsRead()));
        m.put("pushChannel", n.getPushChannel());
        m.put("pushStatus", n.getPushStatus());
        m.put("createdAt", n.getCreatedAt());
        m.put("readAt", n.getReadAt());
        growthCampaignService.enrichGrowthNotification(n, m);
        return m;
    }
}
