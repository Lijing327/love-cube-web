package com.lovecube.backend.services;

import com.lovecube.backend.dating.DatingParticipant;
import com.lovecube.backend.entity.PlatformEvent;
import com.lovecube.backend.notification.NotificationCatalog;
import com.lovecube.backend.repository.PlatformEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DatingMutualMatchNotifyService {

    private static final Logger log = LoggerFactory.getLogger(DatingMutualMatchNotifyService.class);

    private final PlatformEventRepository eventRepository;
    private final NotificationService notificationService;

    public DatingMutualMatchNotifyService(
            PlatformEventRepository eventRepository,
            NotificationService notificationService
    ) {
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public int notifyNewMatches(String eventId, List<Map<String, Object>> createdMatches) {
        if (createdMatches == null || createdMatches.isEmpty()) {
            return 0;
        }
        PlatformEvent event = eventRepository.findById(eventId).orElse(null);
        String eventTitle = event != null && event.getTitle() != null ? event.getTitle() : "联谊专场活动";
        String linkUrl = "/fellowship/events/" + eventId + "/dating/matches";
        String content = "你在「" + eventTitle + "」中有新的互选结果，快去查看吧。";

        int sent = 0;
        for (Map<String, Object> match : createdMatches) {
            Long matchId = toLong(match.get("matchId"));
            if (matchId == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> participantA = (Map<String, Object>) match.get("participantA");
            @SuppressWarnings("unchecked")
            Map<String, Object> participantB = (Map<String, Object>) match.get("participantB");
            sent += notifyParticipant(eventId, matchId, participantA, linkUrl, content);
            sent += notifyParticipant(eventId, matchId, participantB, linkUrl, content);
        }
        if (sent > 0) {
            log.info("联谊互选通知已发送 eventId={} count={}", eventId, sent);
        }
        return sent;
    }

    private int notifyParticipant(
            String eventId,
            Long matchId,
            Map<String, Object> participant,
            String linkUrl,
            String content
    ) {
        if (participant == null) {
            return 0;
        }
        String type = String.valueOf(participant.get("type"));
        Long id = toLong(participant.get("id"));
        if (!DatingParticipant.TYPE_REGISTERED.equalsIgnoreCase(type) || id == null) {
            return 0;
        }
        String relatedId = matchId + ":" + id;
        if (notificationService.hasNotification(
                id,
                NotificationCatalog.TYPE_DATING_MUTUAL_MATCHED,
                NotificationCatalog.RELATED_DATING_MUTUAL_MATCH,
                relatedId
        )) {
            return 0;
        }
        try {
            notificationService.createNotification(
                    id,
                    NotificationCatalog.TYPE_DATING_MUTUAL_MATCHED,
                    "互选成功",
                    content,
                    linkUrl,
                    NotificationCatalog.RELATED_DATING_MUTUAL_MATCH,
                    relatedId
            );
            return 1;
        } catch (Exception e) {
            log.warn("联谊互选通知发送失败 eventId={} userId={} matchId={}: {}",
                    eventId, id, matchId, e.getMessage());
            return 0;
        }
    }

    private Long toLong(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
