package com.lovecube.backend.services;

import com.lovecube.backend.entity.PlatformGroupWeeklyDigestLog;
import com.lovecube.backend.notification.NotificationCatalog;
import com.lovecube.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class GroupWeeklyDigestService {

    private final PlatGroupRepository groupRepository;
    private final PlatGroupMemberRepository memberRepository;
    private final PlatGroupCheckinRepository checkinRepository;
    private final PlatGroupTaskProgressRepository taskProgressRepository;
    private final PlatGroupActivitySignupRepository activitySignupRepository;
    private final PlatformGroupWeeklyDigestLogRepository digestLogRepository;
    private final NotificationService notificationService;

    public GroupWeeklyDigestService(
            PlatGroupRepository groupRepository,
            PlatGroupMemberRepository memberRepository,
            PlatGroupCheckinRepository checkinRepository,
            PlatGroupTaskProgressRepository taskProgressRepository,
            PlatGroupActivitySignupRepository activitySignupRepository,
            PlatformGroupWeeklyDigestLogRepository digestLogRepository,
            NotificationService notificationService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.checkinRepository = checkinRepository;
        this.taskProgressRepository = taskProgressRepository;
        this.activitySignupRepository = activitySignupRepository;
        this.digestLogRepository = digestLogRepository;
        this.notificationService = notificationService;
    }

    public Map<String, Object> buildDigest(Long groupId) {
        var group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return Map.of();
        }
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        long memberCount = memberRepository.countByGroupIdAndStatus(groupId, "approved");
        long checkinUsers = checkinRepository.countDistinctUsersSince(groupId, weekStart);
        long activityUsers = activitySignupRepository.countDistinctActiveSignupsSince(
                groupId, weekStart.atStartOfDay());
        long activeMembers = Math.max(checkinUsers, activityUsers);
        long unclaimedTasks = taskProgressRepository.countUnclaimedCompletedSince(groupId, weekStart);
        long pendingMembers = memberRepository.countByGroupIdAndStatus(groupId, "pending");
        int checkinRatePercent = memberCount <= 0 ? 0 : (int) Math.round(100.0 * checkinUsers / memberCount);

        Map<String, Object> digest = new LinkedHashMap<>();
        digest.put("groupId", groupId);
        digest.put("groupName", group.getName());
        digest.put("weekStart", weekStart.toString());
        digest.put("weekEnd", weekEnd.toString());
        digest.put("memberCount", memberCount);
        digest.put("checkinUsers", checkinUsers);
        digest.put("checkinRatePercent", checkinRatePercent);
        digest.put("activeMembers", activeMembers);
        digest.put("unclaimedTasks", unclaimedTasks);
        digest.put("pendingJoinRequests", pendingMembers);
        return digest;
    }

    @Transactional
    public Map<String, Object> sendDigestForGroup(Long groupId, boolean force) {
        String weekKey = currentWeekKey();
        if (!force && digestLogRepository.findByGroupIdAndWeekKey(groupId, weekKey).isPresent()) {
            return Map.of("sent", false, "message", "本周周报已发送");
        }
        Map<String, Object> digest = buildDigest(groupId);
        if (digest.isEmpty()) {
            return Map.of("sent", false, "message", "团体不存在");
        }
        String groupName = String.valueOf(digest.get("groupName"));
        int rate = (Integer) digest.get("checkinRatePercent");
        long active = ((Number) digest.get("activeMembers")).longValue();
        long unclaimed = ((Number) digest.get("unclaimedTasks")).longValue();
        long pending = ((Number) digest.get("pendingJoinRequests")).longValue();
        long memberCount = ((Number) digest.get("memberCount")).longValue();
        if (!force && memberCount <= 0 && pending <= 0) {
            return Map.of("sent", false, "message", "本周无成员动态，已跳过周报");
        }

        String content = String.format(
                "这是发给「%s」团体管理员的每周经营摘要（%s 至 %s）。本周打卡率 %d%%，活跃成员 %d 人；待领取任务 %d 项，待审核入团 %d 条。点击可查看团体。",
                groupName,
                digest.get("weekStart"),
                digest.get("weekEnd"),
                rate, active, unclaimed, pending);
        String link = "/platform/groups/" + groupId;

        List<Long> managerIds = memberRepository.findByGroupIdAndStatusOrderByJoinedAtAsc(groupId, "approved")
                .stream()
                .filter(m -> "owner".equals(m.getRole()) || "admin".equals(m.getRole()))
                .map(m -> m.getUserId())
                .distinct()
                .toList();

        int sent = 0;
        for (Long uid : managerIds) {
            var n = notificationService.createNotification(
                    uid,
                    NotificationCatalog.TYPE_GROUP_WEEKLY_DIGEST,
                    "团体周报 · " + groupName,
                    content,
                    link,
                    "platform_group",
                    String.valueOf(groupId));
            if (n != null) {
                sent++;
            }
        }

        if (sent > 0 && !force) {
            PlatformGroupWeeklyDigestLog log = new PlatformGroupWeeklyDigestLog();
            log.setGroupId(groupId);
            log.setWeekKey(weekKey);
            log.setSentAt(LocalDateTime.now());
            digestLogRepository.save(log);
        }

        return Map.of("sent", sent > 0, "recipientCount", sent, "digest", digest);
    }

    @Transactional
    public int sendAllWeeklyDigests() {
        int total = 0;
        for (var group : groupRepository.findByStatusOrderByMemberCountDescCreatedAtDesc("published")) {
            Map<String, Object> result = sendDigestForGroup(group.getId(), false);
            if (Boolean.TRUE.equals(result.get("sent"))) {
                total++;
            }
        }
        return total;
    }

    private static String currentWeekKey() {
        WeekFields wf = WeekFields.ISO;
        LocalDate today = LocalDate.now();
        return today.getYear() + "-W" + String.format("%02d", today.get(wf.weekOfWeekBasedYear()));
    }
}
