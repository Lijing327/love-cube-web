package com.lovecube.backend.controllers;

import com.lovecube.backend.entity.PlatGroup;
import com.lovecube.backend.entity.PlatGroupActivity;
import com.lovecube.backend.entity.PlatGroupActivitySignup;
import com.lovecube.backend.entity.PlatGroupCheckin;
import com.lovecube.backend.entity.PlatGroupMember;
import com.lovecube.backend.entity.PlatGroupNotice;
import com.lovecube.backend.entity.PlatGroupPost;
import com.lovecube.backend.entity.PlatGroupPostComment;
import com.lovecube.backend.entity.PlatGroupPostLike;
import com.lovecube.backend.entity.PlatGroupTaskProgress;
import com.lovecube.backend.entity.PlatformEvent;
import com.lovecube.backend.entity.UserGrowth;
import com.lovecube.backend.models.User;
import com.lovecube.backend.notification.NotificationCatalog;
import com.lovecube.backend.repository.PlatGroupActivityRepository;
import com.lovecube.backend.repository.PlatGroupActivitySignupRepository;
import com.lovecube.backend.repository.PlatGroupCheckinRepository;
import com.lovecube.backend.repository.PlatGroupMemberRepository;
import com.lovecube.backend.repository.PlatGroupNoticeRepository;
import com.lovecube.backend.repository.PlatGroupPostCommentRepository;
import com.lovecube.backend.repository.PlatGroupPostLikeRepository;
import com.lovecube.backend.repository.PlatGroupPostRepository;
import com.lovecube.backend.repository.PlatGroupRepository;
import com.lovecube.backend.repository.PlatCheckinCommentRepository;
import com.lovecube.backend.repository.PlatCheckinLikeRepository;
import com.lovecube.backend.repository.PlatGroupTaskProgressRepository;
import com.lovecube.backend.repository.PlatformEventRepository;
import com.lovecube.backend.repository.UserGrowthRepository;
import com.lovecube.backend.repository.UserRepository;
import com.lovecube.backend.services.AdminAuthService;
import com.lovecube.backend.services.GroupAdminRoleConstants;
import com.lovecube.backend.services.GroupActivityEngagementService;
import com.lovecube.backend.services.GroupWeeklyDigestService;
import com.lovecube.backend.services.GrowthService;
import com.lovecube.backend.services.GroupMemberRealNameSupport;
import com.lovecube.backend.services.GroupSeasonService;
import com.lovecube.backend.services.NotificationService;
import com.lovecube.backend.services.PlatformGroupSupport;
import com.lovecube.backend.services.PlatformGroupInviteSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/platform/groups")
public class PlatformGroupController {

    private final PlatGroupRepository groupRepository;
    private final PlatGroupMemberRepository memberRepository;
    private final PlatGroupPostRepository postRepository;
    private final PlatGroupPostLikeRepository postLikeRepository;
    private final PlatGroupPostCommentRepository commentRepository;
    private final PlatGroupNoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final AdminAuthService adminAuthService;
    private final NotificationService notificationService;
    private final PlatGroupCheckinRepository checkinRepository;
    private final PlatGroupTaskProgressRepository taskProgressRepository;
    private final PlatGroupActivityRepository activityRepository;
    private final PlatGroupActivitySignupRepository activitySignupRepository;
    private final GrowthService growthService;
    private final PlatCheckinLikeRepository checkinLikeRepository;
    private final PlatCheckinCommentRepository checkinCommentRepository;
    private final UserGrowthRepository userGrowthRepository;
    private final GroupSeasonService groupSeasonService;
    private final GroupActivityEngagementService activityEngagementService;
    private final GroupWeeklyDigestService weeklyDigestService;
    private final PlatformEventRepository platformEventRepository;

    // 团体任务定义（code → 名称/EXP奖励/对应成长action）
    private static final Map<String, String> TASK_NAMES = Map.of(
            "CHECKIN", "今日打卡",
            "POST", "发布一条团体动态",
            "COMMENT", "评论一次团体动态",
            "LIKE", "点赞一次团体动态"
    );
    private static final Map<String, Integer> TASK_REWARDS = Map.of(
            "CHECKIN", 2, "POST", 5, "COMMENT", 3, "LIKE", 1
    );
    private static final Map<String, String> TASK_ACTION_TYPES = Map.of(
            "CHECKIN", "GROUP_CHECKIN",
            "POST", "GROUP_POST_TASK",
            "COMMENT", "GROUP_COMMENT_TASK",
            "LIKE", "GROUP_LIKE_TASK"
    );
    private static final List<String> TASK_ORDER = List.of("CHECKIN", "POST", "COMMENT", "LIKE");

    public PlatformGroupController(
            PlatGroupRepository groupRepository,
            PlatGroupMemberRepository memberRepository,
            PlatGroupPostRepository postRepository,
            PlatGroupPostLikeRepository postLikeRepository,
            PlatGroupPostCommentRepository commentRepository,
            PlatGroupNoticeRepository noticeRepository,
            UserRepository userRepository,
            AdminAuthService adminAuthService,
            NotificationService notificationService,
            PlatGroupCheckinRepository checkinRepository,
            PlatGroupTaskProgressRepository taskProgressRepository,
            PlatGroupActivityRepository activityRepository,
            PlatGroupActivitySignupRepository activitySignupRepository,
            GrowthService growthService,
            PlatCheckinLikeRepository checkinLikeRepository,
            PlatCheckinCommentRepository checkinCommentRepository,
            UserGrowthRepository userGrowthRepository,
            GroupSeasonService groupSeasonService,
            GroupActivityEngagementService activityEngagementService,
            GroupWeeklyDigestService weeklyDigestService,
            PlatformEventRepository platformEventRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.adminAuthService = adminAuthService;
        this.notificationService = notificationService;
        this.checkinRepository = checkinRepository;
        this.taskProgressRepository = taskProgressRepository;
        this.activityRepository = activityRepository;
        this.activitySignupRepository = activitySignupRepository;
        this.growthService = growthService;
        this.checkinLikeRepository = checkinLikeRepository;
        this.checkinCommentRepository = checkinCommentRepository;
        this.userGrowthRepository = userGrowthRepository;
        this.groupSeasonService = groupSeasonService;
        this.activityEngagementService = activityEngagementService;
        this.weeklyDigestService = weeklyDigestService;
        this.platformEventRepository = platformEventRepository;
    }


    @GetMapping
    public Map<String, Object> listGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String joinMode,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long currentUserId = resolveOptionalUserId(authHeader);
        List<PlatGroup> groups = "all".equals(status)
                ? groupRepository.findAllByOrderByMemberCountDescCreatedAtDesc()
                : groupRepository.findByStatusOrderByMemberCountDescCreatedAtDesc(
                        (status != null && !status.isBlank()) ? status : "published");

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            groups = groups.stream()
                    .filter(g -> g.getName().toLowerCase().contains(kw)
                            || (g.getDescription() != null && g.getDescription().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }
        if (region != null && !region.isBlank()) {
            groups = groups.stream().filter(g -> region.equals(g.getRegion())).collect(Collectors.toList());
        }
        String typeFilter = (type != null && !type.isBlank()) ? type : category;
        if (typeFilter != null && !typeFilter.isBlank()) {
            groups = groups.stream().filter(g -> typeFilter.equals(g.getType())).collect(Collectors.toList());
        }
        if (joinMode != null && !joinMode.isBlank()) {
            groups = groups.stream()
                    .filter(g -> PlatformGroupSupport.matchesJoinModeFilter(g.getJoinMode(), joinMode))
                    .collect(Collectors.toList());
        }
        if ("newest".equals(sort)) {
            groups = groups.stream()
                    .sorted(Comparator.comparing(PlatGroup::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        int total = groups.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<PlatGroup> pageGroups = from < total ? groups.subList(from, to) : Collections.emptyList();

        Map<Long, PlatGroupMember> memberMap = loadUserMemberMap(currentUserId);
        Map<Long, User> ownerUsers = loadOwnersForGroups(pageGroups);
        List<Map<String, Object>> items = pageGroups.stream()
                .map(g -> PlatformGroupSupport.buildGroupSummary(g, memberMap, ownerUsers))
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("total", total);
        body.put("page", safePage);
        body.put("pageSize", safeSize);
        return body;
    }


    @GetMapping("/hot")
    public List<Map<String, Object>> hotGroups(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long currentUserId = resolveOptionalUserId(authHeader);
        List<PlatGroup> groups = groupRepository.findTop5ByStatusOrderByMemberCountDesc("published");
        Map<Long, PlatGroupMember> memberMap = loadUserMemberMap(currentUserId);
        Map<Long, User> ownerUsers = loadOwnersForGroups(groups);
        return groups.stream().map(g -> PlatformGroupSupport.buildGroupSummary(g, memberMap, ownerUsers)).collect(Collectors.toList());
    }


    @GetMapping("/feed")
    public List<Map<String, Object>> feed() {
        List<PlatGroupPost> posts = postRepository.findTop20ByStatusOrderByCreatedAtDesc("published");

        Set<Long> groupIds = posts.stream().map(PlatGroupPost::getGroupId).collect(Collectors.toSet());
        Map<Long, PlatGroup> groupMap = groupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(PlatGroup::getId, g -> g));

        Set<Long> userIds = posts.stream().map(PlatGroupPost::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));

        return posts.stream().map(p -> {
            PlatGroup g = groupMap.get(p.getGroupId());
            User u = userMap.get(p.getUserId());
            String text = p.getContent().length() > 50
                    ? p.getContent().substring(0, 50) + "..."
                    : p.getContent();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("groupId", p.getGroupId());
            item.put("groupName", g != null ? g.getName() : "");
            item.put("coverUrl", g != null ? g.getCoverUrl() : "");
            item.put("text", text);
            item.put("time", formatRelativeTime(p.getCreatedAt()));
            item.put("authorName", u != null ? u.getUsername() : "");
            item.put("content", p.getContent());
            item.put("createdAt", p.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
    }


    @GetMapping("/my")
    public List<Map<String, Object>> myGroups(
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        List<PlatGroupMember> memberships = memberRepository.findByUserId(user.getUserid());

        List<PlatGroupMember> relevant = memberships.stream()
                .filter(m -> "approved".equals(m.getStatus()) || "pending".equals(m.getStatus()))
                .collect(Collectors.toList());

        if (relevant.isEmpty()) return Collections.emptyList();

        Set<Long> groupIds = relevant.stream().map(PlatGroupMember::getGroupId).collect(Collectors.toSet());
        Map<Long, PlatGroup> groupMap = groupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(PlatGroup::getId, g -> g));
        Map<Long, PlatGroupMember> memberMap = relevant.stream()
                .collect(Collectors.toMap(PlatGroupMember::getGroupId, m -> m, (a, b) -> a));

        Map<Long, User> ownerUsers = loadOwnersForGroups(
                groupMap.values().stream()
                        .filter(g -> "published".equals(g.getStatus()))
                        .collect(Collectors.toList()));

        return relevant.stream()
                .map(m -> {
                    PlatGroup group = groupMap.get(m.getGroupId());
                    if (group == null || !"published".equals(group.getStatus())) return null;
                    return PlatformGroupSupport.buildGroupSummary(group, memberMap, ownerUsers);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    /** 通过邀请码预览团体（公开） */
    @GetMapping("/by-invite-code/{code}")
    public Map<String, Object> previewGroupByInviteCode(@PathVariable String code) {
        String normalized = PlatformGroupInviteSupport.normalizeInviteCode(code);
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邀请码无效");
        }
        PlatGroup group = groupRepository.findByInviteCode(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀请码无效或已失效"));
        if (!"published".equals(group.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该团体暂不可加入");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", group.getId());
        out.put("name", group.getName());
        out.put("coverUrl", group.getCoverUrl());
        out.put("region", group.getRegion());
        out.put("description", group.getDescription());
        out.put("memberCount", group.getMemberCount() == null ? 0 : group.getMemberCount());
        out.put("joinMode", group.getJoinMode());
        out.put("joinModeKey", PlatformGroupSupport.joinModeToApiKey(group.getJoinMode()));
        return out;
    }

    @GetMapping("/{idOrSlug}")
    public Map<String, Object> getGroup(
            @PathVariable String idOrSlug,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        PlatGroup group = resolveGroup(idOrSlug);
        Long currentUserId = resolveOptionalUserId(authHeader);
        Map<Long, PlatGroupMember> memberMap = loadUserMemberMap(currentUserId);
        Map<Long, User> ownerUsers = loadOwnersForGroups(Collections.singletonList(group));
        Map<String, Object> result = PlatformGroupSupport.buildGroupSummary(group, memberMap, ownerUsers);
        User viewerUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        boolean revealInnerAdmins = platViewerSeesInnerMemberNames(group.getId(), currentUserId, viewerUser);

        // Include latest notice
        List<PlatGroupNotice> notices = noticeRepository
                .findByGroupIdAndStatusOrderByCreatedAtDesc(group.getId(), "published");
        if (!notices.isEmpty()) {
            PlatGroupNotice n = notices.get(0);
            Map<String, Object> notice = new LinkedHashMap<>();
            notice.put("id", n.getId());
            notice.put("title", n.getTitle());
            notice.put("content", n.getContent());
            notice.put("createdAt", n.getCreatedAt());
            result.put("latestNotice", notice);
        }

        // Include admins (owner + admin roles)
        List<PlatGroupMember> admins = memberRepository
                .findByGroupIdAndStatusOrderByJoinedAtAsc(group.getId(), "approved")
                .stream()
                .filter(m -> "owner".equals(m.getRole()) || "admin".equals(m.getRole()))
                .collect(Collectors.toList());
        Set<Long> adminUserIds = admins.stream().map(PlatGroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> adminUsers = userRepository.findAllById(adminUserIds).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));
        result.put("admins", admins.stream().map(m -> {
            User u = adminUsers.get(m.getUserId());
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("userId", m.getUserId());
            a.put("role", m.getRole());
            a.put("name", GroupMemberRealNameSupport.authorDisplay(revealInnerAdmins, u, m.getMemberRealName()));
            a.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
            return a;
        }).collect(Collectors.toList()));

        if (currentUserId != null) {
            memberRepository.findByGroupIdAndUserId(group.getId(), currentUserId)
                    .filter(m -> "approved".equals(m.getStatus()))
                    .ifPresent(m -> result.put("myMemberRealName", m.getMemberRealName()));
        }

        result.put("inviteJoinRequired", "invite".equals(group.getJoinMode()));
        if (Boolean.TRUE.equals(result.get("managed"))) {
            long pendingCount = memberRepository.countByGroupIdAndStatus(group.getId(), "pending");
            result.put("pendingMemberCount", pendingCount);
            if ("invite".equals(group.getJoinMode()) && group.getInviteCode() != null) {
                result.put("inviteCode", group.getInviteCode());
            }
        }

        return result;
    }

    /** 团长/管理员获取邀请码与分享信息 */
    @GetMapping("/{id}/invite-info")
    public Map<String, Object> getGroupInviteInfo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        requireManagerRole(id, authHeader);
        PlatGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!"invite".equals(group.getJoinMode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该团体未开启邀请制");
        }
        String code = PlatformGroupInviteSupport.ensureInviteCode(group, groupRepository);
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("inviteCode", code);
        out.put("groupId", id);
        out.put("groupName", group.getName());
        out.put("joinPath", "/platform/groups/" + id + "?invite=" + code);
        out.put("joinByCodePath", "/platform/groups/join?code=" + code);
        return out;
    }

    /** 重新生成邀请码 */
    @PostMapping("/{id}/invite-code/refresh")
    @Transactional
    public Map<String, Object> refreshGroupInviteCode(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        requireManagerRole(id, authHeader);
        PlatGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!"invite".equals(group.getJoinMode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该团体未开启邀请制");
        }
        group.setInviteCode(PlatformGroupInviteSupport.generateUniqueCode(groupRepository));
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
        return Map.of(
                "inviteCode", group.getInviteCode(),
                "message", "邀请码已更新，旧链接将失效");
    }

    /** 已通过成员自助补全或修改在本团的展示姓名 */
    @PatchMapping("/{id}/me/member-real-name")
    @Transactional
    public Map<String, Object> patchMyPlatMemberRealName(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        User user = adminAuthService.requireUser(authHeader);
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        PlatGroupMember m = memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(mm -> "approved".equals(mm.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "仅已通过审核的成员可设置本团展示姓名"));
        String name = GroupMemberRealNameSupport.requireMemberRealName(body);
        m.setMemberRealName(name);
        m.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(m);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("myMemberRealName", name);
        out.put("message", "已更新你在本团的展示姓名");
        return out;
    }



    @PostMapping
    @Transactional
    public Map<String, Object> createGroup(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {

        User user = adminAuthService.requireUser(authHeader);

        String name = String.valueOf(payload.getOrDefault("name", "")).trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "团体名称不能为空");
        }

        PlatGroup group = new PlatGroup();
        group.setName(name);
        group.setType(String.valueOf(payload.getOrDefault("type", "region")));
        group.setRegion(String.valueOf(payload.getOrDefault("region", "")));
        group.setDescription(String.valueOf(payload.getOrDefault("description", "")));
        group.setCoverUrl((String) payload.get("coverUrl"));
        group.setJoinMode(PlatformGroupSupport.normalizeJoinModeForStore(payload.get("joinMode")));
        if (payload.containsKey("tags")) {
            String t = String.valueOf(payload.get("tags")).trim();
            group.setTags(t.isBlank() ? null : t);
        }
        group.setOwnerUserId(user.getUserid());
        group.setMemberCount(1);
        group.setStatus("published");
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        String slug = (String) payload.get("slug");
        group.setSlug((slug != null && !slug.isBlank()) ? slug : "group-" + System.currentTimeMillis());

        PlatGroup saved = groupRepository.save(group);

        String creatorRealName = GroupMemberRealNameSupport.requireMemberRealName(payload);
        PlatGroupMember owner = new PlatGroupMember();
        owner.setGroupId(saved.getId());
        owner.setUserId(user.getUserid());
        owner.setRole("owner");
        owner.setStatus("approved");
        owner.setJoinedAt(LocalDateTime.now());
        owner.setMemberRealName(creatorRealName);
        owner.setCreatedAt(LocalDateTime.now());
        owner.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(owner);
        adminAuthService.upsertPlatformGroupAdmin(
                String.valueOf(saved.getId()),
                user.getUserid(),
                GroupAdminRoleConstants.OWNER);

        if ("invite".equals(saved.getJoinMode())) {
            PlatformGroupInviteSupport.ensureInviteCode(saved, groupRepository);
            saved = groupRepository.save(saved);
        }

        return Map.of("id", saved.getId(), "slug", saved.getSlug(), "message", "Created successfully");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> deleteGroup(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        User user = adminAuthService.requireUser(authHeader);
        PlatGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        boolean owner = user.getUserid().equals(group.getOwnerUserId()) || adminAuthService.isAdmin(user);
        if (!owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission");
        }
        memberRepository.findByGroupIdOrderByJoinedAtAsc(id).forEach(memberRepository::delete);
        groupRepository.delete(group);
        return Map.of("id", id, "message", "团体已删除");
    }


    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> updateGroup(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {

        User user = adminAuthService.requireUser(authHeader);
        PlatGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        PlatGroupMember self = memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .orElse(null);

        boolean siteAdmin = adminAuthService.isAdmin(user);
        boolean ownerLike = siteAdmin || (self != null && "owner".equals(self.getRole()));
        boolean groupAdmin = self != null && "admin".equals(self.getRole());
        boolean canEdit = siteAdmin || ownerLike || groupAdmin;

        if (!canEdit) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission");
        }

        if (payload.containsKey("name") && ownerLike) {
            String name = String.valueOf(payload.get("name")).trim();
            if (!name.isBlank()) group.setName(name);
        }
        String type = strOf(payload, "type");
        if (type == null) type = strOf(payload, "category");
        if (type != null && !type.isBlank() && ownerLike) group.setType(type);

        if (payload.containsKey("region") && (ownerLike || groupAdmin)) {
            group.setRegion(String.valueOf(payload.get("region")));
        }
        if (payload.containsKey("description") && (ownerLike || groupAdmin)) {
            group.setDescription(String.valueOf(payload.get("description")));
        }
        if (payload.containsKey("coverUrl") && (ownerLike || groupAdmin)) {
            group.setCoverUrl(String.valueOf(payload.get("coverUrl")));
        }
        if (payload.containsKey("status") && ownerLike) {
            group.setStatus(String.valueOf(payload.get("status")));
        }
        if (payload.containsKey("tags") && ownerLike) {
            String t = String.valueOf(payload.get("tags")).trim();
            group.setTags(t.isBlank() ? null : t);
        }

        if ((payload.containsKey("joinMode") || payload.containsKey("joinType")) && ownerLike) {
            if (payload.containsKey("joinMode")) {
                group.setJoinMode(PlatformGroupSupport.normalizeJoinModeForStore(payload.get("joinMode")));
            } else {
                group.setJoinMode("open".equals(String.valueOf(payload.get("joinType"))) ? "free" : "audit");
            }
            if ("invite".equals(group.getJoinMode())) {
                PlatformGroupInviteSupport.ensureInviteCode(group, groupRepository);
            } else {
                group.setInviteCode(null);
            }
        }

        group.setUpdatedAt(LocalDateTime.now());
        PlatGroup saved = groupRepository.save(group);

        Map<Long, PlatGroupMember> selfMap = loadUserMemberMap(user.getUserid());
        Map<Long, User> owners = loadOwnersForGroups(Collections.singletonList(saved));
        return PlatformGroupSupport.buildGroupSummary(saved, selfMap, owners);
    }


    @PostMapping("/{id}/join")
    @Transactional
    public Map<String, Object> joinGroup(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, Object> payload) {

        User user = adminAuthService.requireUser(authHeader);
        PlatGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (!"published".equals(group.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is not joinable");
        }

        if ("invite".equals(group.getJoinMode())) {
            String inviteCode = payload != null ? String.valueOf(payload.getOrDefault("inviteCode", "")) : "";
            if (!PlatformGroupInviteSupport.matchesInviteCode(group, inviteCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邀请码不正确，请向团长索取有效邀请");
            }
        }

        String reason = payload != null ? String.valueOf(payload.getOrDefault("message", "")).trim() : "";
        boolean needApplyNote = "audit".equals(group.getJoinMode());
        if (needApplyNote && reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申请验证信息不能为空");
        }
        if (reason.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申请验证信息不能超过255个字符");
        }

        String memberRealName = GroupMemberRealNameSupport.requireMemberRealName(payload);

        Optional<PlatGroupMember> existing = memberRepository.findByGroupIdAndUserId(id, user.getUserid());
        if (existing.isPresent()) {
            PlatGroupMember m = existing.get();
            if ("approved".equals(m.getStatus())) {
                return Map.of("joined", true, "pending", false, "message", "Joined successfully");
            }
            if ("pending".equals(m.getStatus())) {
                return Map.of("joined", false, "pending", true, "message", "Request pending");
            }
            if ("free".equals(group.getJoinMode()) || "invite".equals(group.getJoinMode())) {
                m.setStatus("approved");
                m.setJoinedAt(LocalDateTime.now());
                m.setUpdatedAt(LocalDateTime.now());
                m.setMemberRealName(memberRealName);
                memberRepository.save(m);
                group.setMemberCount((group.getMemberCount() == null ? 0 : group.getMemberCount()) + 1);
                groupRepository.save(group);
                return Map.of("joined", true, "pending", false, "message", "Joined successfully");
            }
            m.setStatus("pending");
            m.setApplyReason(reason);
            m.setMemberRealName(memberRealName);
            m.setUpdatedAt(LocalDateTime.now());
            memberRepository.save(m);
            notifyGroupManagersJoinRequest(id, group, user);
            return Map.of("joined", false, "pending", true, "message", "Request submitted, waiting for approval");
        }

        PlatGroupMember member = new PlatGroupMember();
        member.setGroupId(id);
        member.setUserId(user.getUserid());
        member.setRole("member");
        member.setMemberRealName(memberRealName);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());

        if ("free".equals(group.getJoinMode()) || "invite".equals(group.getJoinMode())) {
            member.setStatus("approved");
            member.setJoinedAt(LocalDateTime.now());
            memberRepository.save(member);
            group.setMemberCount((group.getMemberCount() == null ? 0 : group.getMemberCount()) + 1);
            groupRepository.save(group);
            return Map.of("joined", true, "pending", false, "message", "Joined successfully");
        }

        member.setStatus("pending");
        member.setApplyReason(reason);
        memberRepository.save(member);
        notifyGroupManagersJoinRequest(id, group, user);
        return Map.of("joined", false, "pending", true, "message", "Request submitted, waiting for approval");
    }

    private void notifyGroupManagersJoinRequest(Long groupId, PlatGroup group, User applicant) {
        String groupName = group.getName() != null ? group.getName() : "团体";
        String applicantName = applicant.getUsername() != null ? applicant.getUsername() : "有用户";
        memberRepository.findByGroupIdAndStatusOrderByJoinedAtAsc(groupId, "approved").stream()
                .filter(m -> {
                    String r = m.getRole() == null ? "" : m.getRole().toLowerCase(Locale.ROOT);
                    return "owner".equals(r) || "admin".equals(r) || "reviewer".equals(r);
                })
                .filter(m -> !m.getUserId().equals(applicant.getUserid()))
                .forEach(m -> {
                    try {
                        notificationService.createNotification(
                                m.getUserId(),
                                NotificationCatalog.TYPE_GROUP_JOIN_REQUEST,
                                applicantName + " 申请加入「" + groupName + "」",
                                applicantName + " 申请加入你管理的团体「" + groupName + "」，请及时审核。",
                                platformGroupMembersPath(groupId),
                                "platform_group",
                                String.valueOf(groupId));
                    } catch (Exception ignored) {
                    }
                });
    }


    @PostMapping("/{id}/leave")
    @Transactional
    public Map<String, Object> leaveGroup(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        PlatGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        PlatGroupMember member = memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not a group member"));

        if ("owner".equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot leave group directly");
        }

        if ("approved".equals(member.getStatus())) {
            group.setMemberCount(Math.max(0, (group.getMemberCount() == null ? 0 : group.getMemberCount()) - 1));
            groupRepository.save(group);
        }

        member.setStatus("left");
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);

        return Map.of("left", true, "message", "Left group successfully");
    }


    @GetMapping("/{id}/members")
    public List<Map<String, Object>> getMembers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "approved") String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        List<PlatGroupMember> members = "all".equals(status)
                ? memberRepository.findByGroupIdOrderByJoinedAtAsc(id)
                : memberRepository.findByGroupIdAndStatusOrderByJoinedAtAsc(id, status);

        Set<Long> userIds = members.stream().map(PlatGroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));

        Long viewerId = resolveOptionalUserId(authHeader);
        User viewerUser = viewerId != null ? userRepository.findById(viewerId).orElse(null) : null;
        boolean revealInner = platViewerSeesInnerMemberNames(id, viewerId, viewerUser);

        return members.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("userId", m.getUserId());
            item.put("role", m.getRole());
            item.put("status", m.getStatus());
            item.put("joinedAt", m.getJoinedAt());
            item.put("applyReason", m.getApplyReason());
            item.put("requestedAt", m.getCreatedAt());
            item.put("username", u != null ? u.getUsername() : "");
            item.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
            if (revealInner && m.getMemberRealName() != null && !m.getMemberRealName().isBlank()) {
                item.put("memberRealName", m.getMemberRealName());
            }
            return item;
        }).collect(Collectors.toList());
    }

    @PostMapping("/{id}/members/{memberId}/approve")
    @Transactional
    public Map<String, Object> approveMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader("Authorization") String authHeader) {

        requireManagerRole(id, authHeader);

        PlatGroupMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member record not found"));
        if (!"pending".equals(target.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request status cannot be changed");
        }

        target.setStatus("approved");
        target.setJoinedAt(LocalDateTime.now());
        target.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(target);

        PlatGroup group = groupRepository.findById(id).orElseThrow();
        group.setMemberCount((group.getMemberCount() == null ? 0 : group.getMemberCount()) + 1);
        groupRepository.save(group);

        notificationService.createNotification(
                target.getUserId(),
                NotificationCatalog.TYPE_GROUP_APPLICATION_APPROVED,
                "你的团体加入申请已通过",
                "你加入「" + group.getName() + "」的申请已通过",
                platformGroupDetailPath(id),
                "platform_group",
                String.valueOf(id));

        return Map.of("approved", true, "message", "Request approved");
    }

    @PostMapping("/{id}/members/{memberId}/reject")
    @Transactional
    public Map<String, Object> rejectMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader("Authorization") String authHeader) {

        requireManagerRole(id, authHeader);

        PlatGroupMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member record not found"));
        if (!"pending".equals(target.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request status cannot be changed");
        }

        target.setStatus("rejected");
        target.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(target);

        PlatGroup group = groupRepository.findById(id).orElse(null);
        String groupName = group != null ? group.getName() : "团体";
        notificationService.createNotification(
                target.getUserId(),
                NotificationCatalog.TYPE_GROUP_APPLICATION_REJECTED,
                "你的团体加入申请未通过",
                "你加入「" + groupName + "」的申请未通过",
                platformGroupDetailPath(id),
                "platform_group",
                String.valueOf(id));

        return Map.of("rejected", true, "message", "Request rejected");
    }

    @PutMapping("/{id}/members/{memberId}/audit")
    @Transactional
    public Map<String, Object> auditMemberPut(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, Object> body) {
        String action = body != null ? String.valueOf(body.getOrDefault("action", "approve")).trim().toLowerCase() : "approve";
        if ("reject".equals(action) || "rejected".equals(action)) {
            return rejectMember(id, memberId, authHeader);
        }
        return approveMember(id, memberId, authHeader);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Transactional
    public Map<String, Object> removeGroupMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader("Authorization") String authHeader) {

        requireManagerRole(id, authHeader);

        PlatGroupMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member record not found"));
        if (!id.equals(target.getGroupId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid member");
        }
        if ("owner".equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove owner");
        }

        PlatGroup group = groupRepository.findById(id).orElseThrow();
        if ("approved".equals(target.getStatus())) {
            group.setMemberCount(Math.max(0, (group.getMemberCount() == null ? 0 : group.getMemberCount()) - 1));
            groupRepository.save(group);
        }

        target.setStatus("removed");
        target.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(target);

        return Map.of("removed", true, "message", "Member removed");
    }

    private static final int MAX_GROUP_ADMINS = 5;

    @PatchMapping("/{id}/members/{memberId}/role")
    @Transactional
    public Map<String, Object> patchMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        User user = adminAuthService.requireUser(authHeader);
        memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()) && "owner".equals(m.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "仅团长可设置管理员"));

        PlatGroupMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member record not found"));
        if (!id.equals(target.getGroupId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid member");
        }
        if ("owner".equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能修改团长角色");
        }
        if (!"approved".equals(target.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可对已通过成员调整角色");
        }

        String role = String.valueOf(body != null ? body.getOrDefault("role", "") : "").trim().toLowerCase();
        if (!"admin".equals(role) && !"member".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role 仅支持 admin 或 member");
        }

        if ("admin".equals(role) && !"admin".equals(target.getRole())) {
            long adminCount = memberRepository.countByGroupIdAndStatusAndRole(id, "approved", "admin");
            if (adminCount >= MAX_GROUP_ADMINS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员最多 " + MAX_GROUP_ADMINS + " 人");
            }
        }

        target.setRole(role);
        target.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(target);

        return Map.of("role", role, "message", "已更新角色");
    }


    @GetMapping("/{id}/posts")
    public Map<String, Object> getPosts(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        PlatGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        assertCanViewGroupFeed(group, authHeader);

        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Page<PlatGroupPost> pageResult = postRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(
                id, "published", PageRequest.of(safePage - 1, safeSize));
        List<PlatGroupPost> posts = pageResult.getContent();

        Set<Long> userIds = posts.stream().map(PlatGroupPost::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));

        Long currentUserId = resolveOptionalUserId(authHeader);
        Set<Long> likedPostIds = Collections.emptySet();
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(PlatGroupPost::getId).collect(Collectors.toList());
            likedPostIds = new HashSet<>(postLikeRepository.findLikedPostIds(currentUserId, postIds));
        }
        final Set<Long> likedSet = likedPostIds;

        User viewerUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        boolean revealAuthors = platViewerSeesInnerMemberNames(id, currentUserId, viewerUser);
        Map<Long, String> realNames = revealAuthors ? platMemberRealNamesByUserIds(id, userIds) : Collections.emptyMap();

        List<Map<String, Object>> items = posts.stream().map(p -> {
            User u = userMap.get(p.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("postId", p.getId());
            item.put("groupId", p.getGroupId());
            item.put("userId", p.getUserId());
            item.put("content", p.getContent());
            item.put("imageUrls", p.getImageUrls());
            item.put("type", "post");
            item.put("likeCount", p.getLikeCount() == null ? 0 : p.getLikeCount());
            item.put("commentCount", p.getCommentCount() == null ? 0 : p.getCommentCount());
            item.put("likedByMe", likedSet.contains(p.getId()));
            item.put("createdAt", p.getCreatedAt());
            item.put("authorName", GroupMemberRealNameSupport.authorDisplay(revealAuthors, u, realNames.get(p.getUserId())));
            item.put("authorAvatarUrl", u != null ? u.getProfilePhoto() : "");
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("total", pageResult.getTotalElements());
        body.put("page", safePage);
        body.put("pageSize", safeSize);
        return body;
    }

    @PostMapping("/{id}/posts")
    @Transactional
    public Map<String, Object> createPost(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {

        User user = adminAuthService.requireUser(authHeader);
        memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Join the group before posting"));

        String content = String.valueOf(payload.getOrDefault("content", "")).trim();
        String imageUrls = normalizeImageUrls(payload.get("imageUrls"));
        if (content.isEmpty() && (imageUrls == null || imageUrls.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "内容不能为空");
        }

        PlatGroupPost post = new PlatGroupPost();
        post.setGroupId(id);
        post.setUserId(user.getUserid());
        post.setContent(content);
        post.setImageUrls(imageUrls);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus("published");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);

        // 更新团体活跃时间和动态数
        groupRepository.findById(id).ifPresent(g -> {
            g.setPostCount((g.getPostCount() == null ? 0 : g.getPostCount()) + 1);
            g.setLastActiveAt(LocalDateTime.now());
            groupRepository.save(g);
        });

        // 发帖人本人：消息中心可查看发布回执
        final long posterId = user.getUserid();
        final long postId = post.getId();
        final String groupName = groupRepository.findById(id).map(PlatGroup::getName).orElse("团体");
        final String posterName = user.getUsername() != null ? user.getUsername() : "成员";
        notificationService.send(
                posterId,
                "GROUP_POST_CREATED",
                "你发布了一条团体动态",
                "你已在团体「" + groupName + "」发布新动态",
                "platform_group",
                String.valueOf(id)
        );
        // 其余已通过成员：管理员用「你管理的团体」文案，普通成员用统一动态提醒
        memberRepository.findByGroupIdAndStatusOrderByJoinedAtAsc(id, "approved").stream()
                .filter(m -> !m.getUserId().equals(posterId))
                .forEach(m -> {
                    boolean isManager = "owner".equals(m.getRole()) || "admin".equals(m.getRole());
                    String title = isManager
                            ? posterName + " 在你管理的团体发布了新动态"
                            : posterName + " 在团体「" + groupName + "」发布了新动态";
                    String body = posterName + " 在团体「" + groupName + "」发布了新动态";
                    notificationService.send(
                            m.getUserId(),
                            "GROUP_POST_CREATED",
                            title,
                            body,
                            "platform_group",
                            String.valueOf(id)
                    );
                });

        // 发帖完成团体"发布动态"任务
        completeGroupTask(id, user.getUserid(), "POST");

        return Map.of("id", postId, "message", "Post published successfully");
    }

    @DeleteMapping("/{groupId}/posts/{postId}")
    @Transactional
    public Map<String, Object> deletePost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        PlatGroupPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (!post.getGroupId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }

        boolean isManager = memberRepository.findByGroupIdAndUserId(groupId, user.getUserid())
                .map(m -> "owner".equals(m.getRole()) || "admin".equals(m.getRole()))
                .orElse(false) || adminAuthService.isAdmin(user);

        if (!post.getUserId().equals(user.getUserid()) && !isManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission");
        }

        post.setStatus("deleted");
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);

        groupRepository.findById(groupId).ifPresent(g -> {
            g.setPostCount(Math.max(0, (g.getPostCount() == null ? 0 : g.getPostCount()) - 1));
            groupRepository.save(g);
        });

        return Map.of("deleted", true, "message", "Post deleted");
    }

    @PostMapping("/posts/{postId}/like")
    @Transactional
    public Map<String, Object> toggleLike(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        PlatGroupPost post = postRepository.findById(postId)
                .filter(p -> "published".equals(p.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        PlatGroup postGroup = groupRepository.findById(post.getGroupId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        assertCanViewGroupFeed(postGroup, authHeader);

        Optional<PlatGroupPostLike> existing = postLikeRepository.findByPostIdAndUserId(postId, user.getUserid());
        boolean nowLiked;
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, (post.getLikeCount() == null ? 0 : post.getLikeCount()) - 1));
            nowLiked = false;
        } else {
            PlatGroupPostLike like = new PlatGroupPostLike();
            like.setPostId(postId);
            like.setUserId(user.getUserid());
            like.setCreatedAt(LocalDateTime.now());
            postLikeRepository.save(like);
            post.setLikeCount((post.getLikeCount() == null ? 0 : post.getLikeCount()) + 1);
            nowLiked = true;

            // 点赞完成团体"点赞"任务
            completeGroupTask(post.getGroupId(), user.getUserid(), "LIKE");

            // 通知动态作者（不通知自己）
            if (!post.getUserId().equals(user.getUserid())) {
                String actorName = user.getUsername() != null ? user.getUsername() : "有人";
                notificationService.send(
                        post.getUserId(),
                        "GROUP_POST_LIKED",
                        actorName + " 点赞了你的团体动态",
                        actorName + " 点赞了你的团体动态",
                        "platform_group",
                        String.valueOf(post.getGroupId())
                );
            }
        }
        postRepository.save(post);

        return Map.of("likedByMe", nowLiked, "likeCount", post.getLikeCount());
    }

    @GetMapping("/posts/{postId}/comments")
    public Map<String, Object> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        PlatGroupPost post = postRepository.findById(postId)
                .filter(p -> "published".equals(p.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        PlatGroup group = groupRepository.findById(post.getGroupId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        assertCanViewGroupFeed(group, authHeader);

        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        List<PlatGroupPostComment> comments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(postId, "published", PageRequest.of(safePage - 1, safeSize));

        Set<Long> userIds = comments.stream().map(PlatGroupPostComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));

        Long viewerId = resolveOptionalUserId(authHeader);
        User viewerUser = viewerId != null ? userRepository.findById(viewerId).orElse(null) : null;
        boolean revealAuthors = platViewerSeesInnerMemberNames(group.getId(), viewerId, viewerUser);
        Map<Long, String> realNames = revealAuthors ? platMemberRealNamesByUserIds(group.getId(), userIds) : Collections.emptyMap();

        List<Map<String, Object>> items = comments.stream().map(c -> {
            User u = userMap.get(c.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("userId", c.getUserId());
            item.put("content", c.getContent());
            item.put("createdAt", c.getCreatedAt());
            item.put("authorName", GroupMemberRealNameSupport.authorDisplay(revealAuthors, u, realNames.get(c.getUserId())));
            item.put("authorAvatarUrl", u != null ? u.getProfilePhoto() : "");
            return item;
        }).collect(Collectors.toList());

        long total = commentRepository.countByPostIdAndStatus(postId, "published");
        return Map.of("items", items, "total", total, "page", safePage, "pageSize", safeSize);
    }

    @PostMapping("/posts/{postId}/comments")
    @Transactional
    public Map<String, Object> addComment(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {

        User user = adminAuthService.requireUser(authHeader);
        PlatGroupPost post = postRepository.findById(postId)
                .filter(p -> "published".equals(p.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        PlatGroup postGroup = groupRepository.findById(post.getGroupId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        assertCanViewGroupFeed(postGroup, authHeader);

        String content = String.valueOf(payload.getOrDefault("content", "")).trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论最多 500 字");
        }

        PlatGroupPostComment comment = new PlatGroupPostComment();
        comment.setPostId(postId);
        comment.setGroupId(post.getGroupId());
        comment.setUserId(user.getUserid());
        comment.setContent(content);
        comment.setStatus("published");
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
        postRepository.save(post);

        // 评论完成团体"评论"任务
        completeGroupTask(post.getGroupId(), user.getUserid(), "COMMENT");

        // 通知动态作者（不通知自己）
        if (!post.getUserId().equals(user.getUserid())) {
            String actorName = user.getUsername() != null ? user.getUsername() : "有人";
            notificationService.send(
                    post.getUserId(),
                    "GROUP_POST_COMMENTED",
                    actorName + " 评论了你的团体动态",
                    actorName + "：" + (content.length() > 50 ? content.substring(0, 50) + "…" : content),
                    "platform_group",
                    String.valueOf(post.getGroupId())
            );
        }

        User u = user;
        String myReal = memberRepository.findByGroupIdAndUserId(post.getGroupId(), user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .map(PlatGroupMember::getMemberRealName)
                .orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", comment.getId());
        result.put("userId", u.getUserid());
        result.put("content", comment.getContent());
        result.put("createdAt", comment.getCreatedAt());
        result.put("authorName", GroupMemberRealNameSupport.authorDisplay(true, u, myReal));
        result.put("authorAvatarUrl", u.getProfilePhoto() != null ? u.getProfilePhoto() : "");
        return result;
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    @Transactional
    public Map<String, Object> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        PlatGroupPostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!comment.getPostId().equals(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        boolean isManager = memberRepository.findByGroupIdAndUserId(comment.getGroupId(), user.getUserid())
                .map(m -> "owner".equals(m.getRole()) || "admin".equals(m.getRole()))
                .orElse(false) || adminAuthService.isAdmin(user);

        if (!comment.getUserId().equals(user.getUserid()) && !isManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission");
        }

        comment.setStatus("deleted");
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        postRepository.findById(postId).ifPresent(p -> {
            p.setCommentCount(Math.max(0, (p.getCommentCount() == null ? 0 : p.getCommentCount()) - 1));
            postRepository.save(p);
        });

        return Map.of("deleted", true, "message", "Comment deleted");
    }


    @GetMapping("/{id}/notices")
    public List<Map<String, Object>> getNotices(@PathVariable Long id) {
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        return noticeRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(id, "published").stream()
                .map(n -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", n.getId());
                    item.put("title", n.getTitle());
                    item.put("content", n.getContent());
                    item.put("createdAt", n.getCreatedAt());
                    return item;
                }).collect(Collectors.toList());
    }

    @PostMapping("/{id}/notices")
    @Transactional
    public Map<String, Object> createNotice(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {

        User user = adminAuthService.requireUser(authHeader);
        requireManagerRole(id, authHeader);

        PlatGroupNotice notice = new PlatGroupNotice();
        notice.setGroupId(id);
        notice.setTitle(String.valueOf(payload.getOrDefault("title", "")));
        notice.setContent(String.valueOf(payload.getOrDefault("content", "")));
        notice.setCreatedBy(user.getUserid());
        notice.setStatus("published");
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        noticeRepository.save(notice);

        return Map.of("id", notice.getId(), "message", "Notice published successfully");
    }


    private Map<Long, User> loadOwnersForGroups(List<PlatGroup> groups) {
        if (groups == null || groups.isEmpty()) return Collections.emptyMap();
        Set<Long> ids = groups.stream().map(PlatGroup::getOwnerUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getUserid, u -> u));
    }

    private Map<Long, PlatGroupMember> loadUserMemberMap(Long userId) {
        if (userId == null) return Collections.emptyMap();
        return memberRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(PlatGroupMember::getGroupId, m -> m, (a, b) -> a));
    }

    private PlatGroup resolveGroup(String idOrSlug) {
        try {
            long id = Long.parseLong(idOrSlug);
            return groupRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        } catch (NumberFormatException e) {
            return groupRepository.findBySlug(idOrSlug)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        }
    }

    private String readString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String strOf(Map<String, Object> m, String key) {
        return m.containsKey(key) && m.get(key) != null ? String.valueOf(m.get(key)) : null;
    }

    private Long resolveOptionalUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            return adminAuthService.requireUser(authHeader).getUserid();
        } catch (Exception e) {
            return null;
        }
    }

    private void requireManagerRole(Long groupId, String authHeader) {
        User user = adminAuthService.requireUser(authHeader);
        if (adminAuthService.isAdmin(user)) {
            return;
        }
        memberRepository.findByGroupIdAndUserId(groupId, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus())
                        && ("owner".equals(m.getRole()) || "admin".equals(m.getRole())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission"));
    }

    private static String platformGroupDetailPath(Long groupId) {
        return "/platform/groups/" + groupId;
    }

    private static String platformGroupMembersPath(Long groupId) {
        return "/platform/groups/" + groupId + "/members";
    }

    private boolean platViewerSeesInnerMemberNames(Long groupId, Long viewerId, User viewerUser) {
        if (viewerId == null) {
            return false;
        }
        if (viewerUser != null && adminAuthService.isAdmin(viewerUser)) {
            return true;
        }
        return memberRepository.findByGroupIdAndUserId(groupId, viewerId)
                .filter(m -> "approved".equals(m.getStatus()))
                .isPresent();
    }

    private Map<Long, String> platMemberRealNamesByUserIds(Long groupId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PlatGroupMember> rows = memberRepository.findByGroupIdAndUserIdIn(groupId, userIds);
        Map<Long, String> out = new HashMap<>();
        for (PlatGroupMember m : rows) {
            if (m.getMemberRealName() != null && !m.getMemberRealName().isBlank()) {
                out.putIfAbsent(m.getUserId(), m.getMemberRealName());
            }
        }
        return out;
    }

    /**
     * 公开加入（free）的团体：任何人可读动态；审核/邀请加入的团体：仅已通过成员或站点管理员可读。
     */
    private void assertCanViewGroupFeed(PlatGroup group, String authHeader) {
        if (!"published".equals(group.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
        String jm = group.getJoinMode();
        if ("free".equals(jm)) {
            return;
        }
        User user = null;
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                user = adminAuthService.requireUser(authHeader);
            }
        } catch (Exception ignored) {
        }
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅团体成员可查看动态");
        }
        if (adminAuthService.isAdmin(user)) {
            return;
        }
        memberRepository.findByGroupIdAndUserId(group.getId(), user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "仅团体成员可查看动态"));
    }

    private static String normalizeImageUrls(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof List<?> list) {
            String joined = list.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
            return joined.isEmpty() ? null : joined;
        }
        String s = String.valueOf(raw).trim();
        return s.isEmpty() ? null : s;
    }

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private String formatRelativeTime(LocalDateTime dt) {
        if (dt == null) return "";
        long minutes = java.time.Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " minutes ago";
        if (minutes < 1440) return (minutes / 60) + " hours ago";
        return dt.format(DF);
    }

    // ── 打卡 ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/checkins/summary")
    public Map<String, Object> checkinSummary(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        LocalDate today = LocalDate.now();
        int todayCount = checkinRepository.countByGroupIdAndCheckinDate(id, today);

        boolean checkedInToday = false;
        int myStreakDays = 0;
        Long userId = resolveOptionalUserId(authHeader);
        if (userId != null) {
            checkedInToday = checkinRepository.findByGroupIdAndUserIdAndCheckinDate(id, userId, today).isPresent();
            myStreakDays = checkinRepository.findTopByGroupIdAndUserIdOrderByCheckinDateDesc(id, userId)
                    .map(c -> c.getStreakDays() != null ? c.getStreakDays() : 1)
                    .orElse(0);
        }

        List<PlatGroupCheckin> recent = checkinRepository.findByGroupIdOrderByCreatedAtDesc(id, PageRequest.of(0, 20));
        Set<Long> userIds = recent.stream().map(PlatGroupCheckin::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getUserid, u -> u));

        List<Map<String, Object>> recentList = recent.stream().map(c -> {
            User u = userMap.get(c.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("userId", c.getUserId());
            item.put("username", u != null ? u.getUsername() : "");
            item.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
            item.put("checkinType", c.getCheckinType());
            item.put("content", c.getContent() != null ? c.getContent() : "");
            item.put("streakDays", c.getStreakDays());
            item.put("createdAt", c.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
        if (!recentList.isEmpty()) {
            Set<Long> uids = recent.stream().map(PlatGroupCheckin::getUserId).collect(Collectors.toSet());
            Map<Long, Integer> lvl = userGrowthRepository.findByUserIdIn(uids).stream()
                    .collect(Collectors.toMap(
                            UserGrowth::getUserId,
                            g -> g.getLevel() != null ? g.getLevel() : 1,
                            (a, b) -> a));
            for (Map<String, Object> item : recentList) {
                long uid = ((Number) item.get("userId")).longValue();
                item.put("title", GrowthService.getUserTitle(lvl.getOrDefault(uid, 1)));
            }
        }
        enrichCheckinFeedItems(recentList, userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedInToday", checkedInToday);
        result.put("todayCount", todayCount);
        result.put("myStreakDays", myStreakDays);
        result.put("recentCheckins", recentList);
        return result;
    }

    /**
     * 团体打卡排行榜（仅已通过成员；需登录）。
     */
    @GetMapping("/{id}/checkins/rankings")
    public Map<String, Object> checkinRankings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "daily") String type,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "仅团体成员可查看排行榜"));

        String t = type != null ? type.trim().toLowerCase() : "daily";
        if (!"daily".equals(t) && !"streak".equals(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type 仅支持 daily 或 streak");
        }

        List<PlatGroupMember> members = memberRepository.findByGroupIdAndStatusOrderByJoinedAtAsc(id, "approved");
        if (members.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("type", t);
            empty.put("items", Collections.emptyList());
            empty.put("currentUser", null);
            return empty;
        }

        LocalDate today = LocalDate.now();
        Map<Long, PlatGroupCheckin> todayByUser = checkinRepository.findByGroupIdAndCheckinDateOrderByCreatedAtAsc(id, today)
                .stream()
                .collect(Collectors.toMap(PlatGroupCheckin::getUserId, c -> c, (a, b) -> a));

        Map<Long, Integer> streakByUser = new HashMap<>();
        for (Object[] row : checkinRepository.findLatestStreakByUserForGroup(id)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
            streakByUser.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }

        List<RankScratch> scratches = new ArrayList<>();
        for (PlatGroupMember m : members) {
            Long uid = m.getUserId();
            PlatGroupCheckin td = todayByUser.get(uid);
            boolean checked = td != null;
            scratches.add(new RankScratch(uid, checked, checked ? td.getCreatedAt() : null,
                    checked ? 1 : 0, streakByUser.getOrDefault(uid, 0)));
        }

        Comparator<RankScratch> dailyCmp = Comparator
                .comparing((RankScratch r) -> r.checkedToday).reversed()
                .thenComparing(RankScratch::getTodayCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((RankScratch r) -> r.streakDays).reversed()
                .thenComparing((RankScratch r) -> r.userId);
        Comparator<RankScratch> streakCmp = Comparator
                .comparing((RankScratch r) -> r.streakDays).reversed()
                .thenComparing((RankScratch r) -> r.checkedToday).reversed()
                .thenComparing(RankScratch::getTodayCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((RankScratch r) -> r.userId);

        scratches.sort("streak".equals(t) ? streakCmp : dailyCmp);
        for (int i = 0; i < scratches.size(); i++) {
            scratches.get(i).rank = i + 1;
        }

        Set<Long> allUserIds = scratches.stream().map(s -> s.userId).collect(Collectors.toSet());
        Map<Long, User> users = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));
        Map<Long, Integer> levelByUser = userGrowthRepository.findByUserIdIn(allUserIds).stream()
                .collect(Collectors.toMap(UserGrowth::getUserId, g -> g.getLevel() != null ? g.getLevel() : 1));

        List<Map<String, Object>> fullRows = scratches.stream()
                .map(s -> toRankingRow(s, users.get(s.userId), levelByUser.getOrDefault(s.userId, 1), user.getUserid()))
                .collect(Collectors.toList());

        List<Map<String, Object>> top20 = fullRows.stream().limit(20).collect(Collectors.toList());
        Map<String, Object> current = fullRows.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("isCurrentUser")))
                .findFirst()
                .orElse(Collections.emptyMap());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", t);
        body.put("items", top20);
        body.put("currentUser", current);
        return body;
    }

    private static final class RankScratch {
        final Long userId;
        final boolean checkedToday;
        final LocalDateTime todayCreatedAt;
        final int checkinCount;
        final int streakDays;
        int rank;

        RankScratch(Long userId, boolean checkedToday, LocalDateTime todayCreatedAt, int checkinCount, int streakDays) {
            this.userId = userId;
            this.checkedToday = checkedToday;
            this.todayCreatedAt = todayCreatedAt;
            this.checkinCount = checkinCount;
            this.streakDays = streakDays;
        }

        LocalDateTime getTodayCreatedAt() {
            return todayCreatedAt;
        }
    }

    private Map<String, Object> toRankingRow(RankScratch s, User u, int level, Long currentUserId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", s.userId);
        row.put("nickname", u != null ? u.getUsername() : "");
        row.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
        row.put("checkinCount", s.checkinCount);
        row.put("streakDays", s.streakDays);
        row.put("rank", s.rank);
        row.put("isCurrentUser", currentUserId != null && currentUserId.equals(s.userId));
        row.put("title", GrowthService.getUserTitle(level));
        return row;
    }

    private void enrichCheckinFeedItems(List<Map<String, Object>> items, Long currentUserId) {
        if (items == null || items.isEmpty()) return;
        List<Long> ids = items.stream()
                .map(m -> m.get("id"))
                .filter(Objects::nonNull)
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;

        Map<Long, Long> likeCountMap = toLongCountMap(checkinLikeRepository.countByCheckinIdInGrouped(ids));
        Map<Long, Long> commentCountMap = toLongCountMap(checkinCommentRepository.countByCheckinIdInGrouped(ids));
        Set<Long> likedSet = Collections.emptySet();
        if (currentUserId != null) {
            likedSet = new HashSet<>(checkinLikeRepository.findCheckinIdsLikedByUser(currentUserId, ids));
        }
        final Set<Long> likedFinal = likedSet;
        for (Map<String, Object> item : items) {
            long cid = ((Number) item.get("id")).longValue();
            item.put("likeCount", likeCountMap.getOrDefault(cid, 0L).intValue());
            item.put("commentCount", commentCountMap.getOrDefault(cid, 0L).intValue());
            item.put("likedByCurrentUser", likedFinal.contains(cid));
        }
    }

    private static Map<Long, Long> toLongCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyMap();
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }

    @PostMapping("/{id}/checkins")
    @Transactional
    public Map<String, Object> createCheckin(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {

        User user = adminAuthService.requireUser(authHeader);
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "仅团体成员可以打卡"));

        LocalDate today = LocalDate.now();
        if (checkinRepository.findByGroupIdAndUserIdAndCheckinDate(id, user.getUserid(), today).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "今天已经打卡过了");
        }

        String checkinType = String.valueOf(payload.getOrDefault("checkinType", "other")).trim();
        List<String> validTypes = List.of("thanks", "prayer", "study", "exercise", "share", "other");
        if (!validTypes.contains(checkinType)) checkinType = "other";

        String content = String.valueOf(payload.getOrDefault("content", "")).trim();
        if (content.length() > 500) content = content.substring(0, 500);

        // 计算连续打卡天数
        int streakDays = 1;
        Optional<PlatGroupCheckin> previous = checkinRepository.findTopByGroupIdAndUserIdOrderByCheckinDateDesc(id, user.getUserid());
        if (previous.isPresent() && today.minusDays(1).equals(previous.get().getCheckinDate())) {
            streakDays = (previous.get().getStreakDays() != null ? previous.get().getStreakDays() : 1) + 1;
        }

        PlatGroupCheckin checkin = new PlatGroupCheckin();
        checkin.setGroupId(id);
        checkin.setUserId(user.getUserid());
        checkin.setCheckinDate(today);
        checkin.setCheckinType(checkinType);
        checkin.setContent(content.isBlank() ? null : content);
        checkin.setStreakDays(streakDays);
        checkin.setCreatedAt(LocalDateTime.now());
        checkinRepository.save(checkin);
        try { groupSeasonService.recordCheckin(id); } catch (Exception ignored) { }

        // 打卡完成团体"打卡"任务（EXP 在用户领取任务奖励时才发放，此处只标记完成）
        completeGroupTask(id, user.getUserid(), "CHECKIN");

        // 通知
        notificationService.send(user.getUserid(), "GROUP_CHECKIN_CREATED",
                "打卡成功",
                "你已在团体完成今日打卡，连续打卡 " + streakDays + " 天",
                "platform_group", String.valueOf(id));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", checkin.getId());
        result.put("streakDays", streakDays);
        result.put("message", "打卡成功");
        return result;
    }

    // ── 团体任务 ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/tasks/today")
    public List<Map<String, Object>> todayTasks(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "仅团体成员可查看任务"));

        LocalDate today = LocalDate.now();
        Map<String, PlatGroupTaskProgress> progressMap = taskProgressRepository
                .findByGroupIdAndUserIdAndTaskDate(id, user.getUserid(), today)
                .stream()
                .collect(Collectors.toMap(PlatGroupTaskProgress::getTaskCode, p -> p));

        return TASK_ORDER.stream().map(code -> {
            PlatGroupTaskProgress p = progressMap.get(code);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskCode", code);
            item.put("name", TASK_NAMES.getOrDefault(code, code));
            item.put("rewardExp", TASK_REWARDS.getOrDefault(code, 0));
            item.put("completed", p != null && p.getCompleted() == 1);
            item.put("claimed", p != null && p.getClaimed() == 1);
            item.put("completedAt", p != null ? p.getCompletedAt() : null);
            item.put("claimedAt", p != null ? p.getClaimedAt() : null);
            return item;
        }).collect(Collectors.toList());
    }

    @PostMapping("/{id}/tasks/{taskCode}/claim")
    @Transactional
    public Map<String, Object> claimTask(
            @PathVariable Long id,
            @PathVariable String taskCode,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "仅团体成员可领取奖励"));

        if (!TASK_NAMES.containsKey(taskCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的任务代码");
        }

        LocalDate today = LocalDate.now();
        PlatGroupTaskProgress progress = taskProgressRepository
                .findByGroupIdAndUserIdAndTaskCodeAndTaskDate(id, user.getUserid(), taskCode, today)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务尚未完成"));

        if (progress.getCompleted() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务尚未完成");
        }
        if (progress.getClaimed() == 1) {
            return Map.of("claimed", false, "message", "奖励已领取");
        }

        progress.setClaimed(1);
        progress.setClaimedAt(LocalDateTime.now());
        taskProgressRepository.save(progress);

        String actionType = TASK_ACTION_TYPES.getOrDefault(taskCode, "GROUP_CHECKIN");
        String bizId = "GROUP_TASK_" + id + "_" + taskCode + "_" + today;
        growthService.recordAction(user.getUserid(), actionType, bizId);
        try { groupSeasonService.recordTaskClaim(id); } catch (Exception ignored) { }

        int rewardExp = TASK_REWARDS.getOrDefault(taskCode, 0);
        notificationService.send(user.getUserid(), "GROUP_TASK_REWARD_CLAIMED",
                "你领取了团体任务奖励：+" + rewardExp + " EXP",
                "任务「" + TASK_NAMES.getOrDefault(taskCode, taskCode) + "」奖励已到账",
                "platform_group", String.valueOf(id));

        return Map.of("claimed", true, "taskCode", taskCode, "rewardExp", rewardExp, "message", "奖励已领取");
    }

    // ── 团体活动 ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/activities")
    public Map<String, Object> listActivities(
            @PathVariable Long id,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        Long userId = resolveOptionalUserId(authHeader);
        List<PlatGroupActivity> all = activityRepository.findByGroupIdOrderByStartTimeDesc(id).stream()
                .filter(a -> "published".equals(a.getStatus()) || "ended".equals(a.getStatus())
                        || "cancelled".equals(a.getStatus()))
                .collect(Collectors.toList());

        // filter: upcoming(已发布且未结束) / ended / all，默认返回全部
        if ("upcoming".equals(filter)) {
            LocalDateTime now = LocalDateTime.now();
            all = all.stream()
                    .filter(a -> "published".equals(a.getStatus()) && a.getEndTime().isAfter(now))
                    .collect(Collectors.toList());
        } else if ("ended".equals(filter)) {
            LocalDateTime now = LocalDateTime.now();
            all = all.stream()
                    .filter(a -> "ended".equals(a.getStatus()) ||
                            ("published".equals(a.getStatus()) && !a.getEndTime().isAfter(now)))
                    .collect(Collectors.toList());
        }

        int total = all.size();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<PlatGroupActivity> pageItems = from < total ? all.subList(from, to) : Collections.emptyList();

        Set<Long> userIds = pageItems.stream().map(PlatGroupActivity::getCreatorUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getUserid, u -> u));

        Map<Long, PlatGroupActivitySignup> mySignupMap = Collections.emptyMap();
        if (userId != null && !pageItems.isEmpty()) {
            List<Long> activityIds = pageItems.stream().map(PlatGroupActivity::getId).collect(Collectors.toList());
            mySignupMap = activitySignupRepository
                    .findByActivityIdInAndUserIdAndStatus(activityIds, userId, "signed_up")
                    .stream()
                    .collect(Collectors.toMap(PlatGroupActivitySignup::getActivityId, s -> s, (a, b) -> a));
        }
        final Map<Long, PlatGroupActivitySignup> signupMap = mySignupMap;
        final Long currentUserId = userId;

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> items = pageItems.stream().map(a -> {
            User creator = userMap.get(a.getCreatorUserId());
            PlatGroupActivitySignup mySignup = signupMap.get(a.getId());
            return buildActivityItem(a, creator, mySignup, currentUserId, now);
        }).collect(Collectors.toList());

        return Map.of("items", items, "total", total, "page", safePage, "pageSize", safeSize);
    }

    @PostMapping("/{id}/activities")
    @Transactional
    public Map<String, Object> createActivity(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {

        requireManagerRole(id, authHeader);
        User user = adminAuthService.requireUser(authHeader);
        PlatGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        String title = String.valueOf(payload.getOrDefault("title", "")).trim();
        if (title.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "活动标题不能为空");
        if (title.length() > 200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题最多 200 字");

        String startStr = String.valueOf(payload.getOrDefault("startTime", "")).trim();
        String endStr = String.valueOf(payload.getOrDefault("endTime", "")).trim();
        if (startStr.isBlank() || endStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "活动时间不能为空");
        }

        LocalDateTime startTime, endTime;
        try {
            startTime = LocalDateTime.parse(startStr);
            endTime = LocalDateTime.parse(endStr);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "时间格式不正确，请使用 ISO 8601 格式");
        }
        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
        }

        PlatGroupActivity activity = new PlatGroupActivity();
        activity.setGroupId(id);
        activity.setCreatorUserId(user.getUserid());
        activity.setTitle(title);
        activity.setDescription(String.valueOf(payload.getOrDefault("description", "")).trim());
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        String location = String.valueOf(payload.getOrDefault("location", "")).trim();
        activity.setLocation(location.isBlank() ? null : location);
        Object maxP = payload.get("maxParticipants");
        activity.setMaxParticipants(maxP != null ? Math.max(0, Integer.parseInt(String.valueOf(maxP))) : 0);
        activity.setStatus("published");
        activity.setParticipantCount(0);
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());

        Object platformEventIdObj = payload.get("platformEventId");
        if (platformEventIdObj != null) {
            String eventId = String.valueOf(platformEventIdObj).trim();
            if (!eventId.isBlank()) {
                PlatformEvent pe = platformEventRepository.findById(eventId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "关联的平台活动不存在"));
                activity.setPlatformEventId(eventId);
                if (title.isBlank() && pe.getTitle() != null) {
                    activity.setTitle(pe.getTitle());
                }
                if ((activity.getDescription() == null || activity.getDescription().isBlank())
                        && pe.getSummary() != null) {
                    activity.setDescription(pe.getSummary());
                }
                if ((activity.getLocation() == null || activity.getLocation().isBlank())
                        && pe.getLocation() != null) {
                    activity.setLocation(pe.getLocation());
                }
                if (pe.getEventTime() != null) {
                    activity.setStartTime(pe.getEventTime());
                    activity.setEndTime(pe.getEventTime().plusHours(2));
                }
            }
        }

        activityRepository.save(activity);

        // 通知团体成员
        final String activityTitle = title;
        memberRepository.findByGroupIdAndStatusOrderByJoinedAtAsc(id, "approved").stream()
                .filter(m -> !m.getUserId().equals(user.getUserid()))
                .forEach(m -> notificationService.send(
                        m.getUserId(), "GROUP_ACTIVITY_PUBLISHED",
                        "团体发布了新活动：" + activityTitle,
                        "团体「" + group.getName() + "」发布了新活动，快去报名吧！",
                        "platform_group", String.valueOf(id)));

        return Map.of("id", activity.getId(), "message", "活动已发布");
    }

    @GetMapping("/{id}/activities/{activityId}")
    public Map<String, Object> getActivity(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        PlatGroupActivity activity = activityRepository.findByIdAndGroupId(activityId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        Long userId = resolveOptionalUserId(authHeader);
        PlatGroupActivitySignup mySignup = null;
        if (userId != null) {
            mySignup = activitySignupRepository.findByActivityIdAndUserId(activityId, userId)
                    .filter(s -> "signed_up".equals(s.getStatus()))
                    .orElse(null);
        }

        User creator = userRepository.findById(activity.getCreatorUserId()).orElse(null);
        return buildActivityItem(activity, creator, mySignup, userId, LocalDateTime.now());
    }

    @PostMapping("/{id}/activities/{activityId}/signup")
    @Transactional
    public Map<String, Object> signUpActivity(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        memberRepository.findByGroupIdAndUserId(id, user.getUserid())
                .filter(m -> "approved".equals(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "仅团体成员可报名活动"));

        PlatGroupActivity activity = activityRepository.findByIdAndGroupId(activityId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        if (!"published".equals(activity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "活动不可报名");
        }
        if (activity.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "活动已结束");
        }
        if (activity.getMaxParticipants() > 0
                && activity.getParticipantCount() >= activity.getMaxParticipants()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名人数已满");
        }

        Optional<PlatGroupActivitySignup> existing = activitySignupRepository.findByActivityIdAndUserId(activityId, user.getUserid());
        if (existing.isPresent()) {
            PlatGroupActivitySignup s = existing.get();
            if ("signed_up".equals(s.getStatus())) {
                return Map.of("signedUp", true, "message", "已报名");
            }
            s.setStatus("signed_up");
            s.setUpdatedAt(LocalDateTime.now());
            activitySignupRepository.save(s);
        } else {
            PlatGroupActivitySignup signup = new PlatGroupActivitySignup();
            signup.setActivityId(activityId);
            signup.setGroupId(id);
            signup.setUserId(user.getUserid());
            signup.setStatus("signed_up");
            signup.setCreatedAt(LocalDateTime.now());
            signup.setUpdatedAt(LocalDateTime.now());
            activitySignupRepository.save(signup);
        }

        activityRepository.incrementParticipantCount(activityId);
        try { groupSeasonService.recordActivitySignup(id); } catch (Exception ignored) { }

        // 通知活动创建者（不通知自己）
        if (!user.getUserid().equals(activity.getCreatorUserId())) {
            String actorName = user.getUsername() != null ? user.getUsername() : "有人";
            notificationService.send(activity.getCreatorUserId(), "GROUP_ACTIVITY_SIGNED_UP",
                    actorName + " 报名了你发布的活动",
                    actorName + " 报名了活动「" + activity.getTitle() + "」",
                    "platform_group", String.valueOf(id));
        }

        int updatedCount = activityRepository.findById(activityId).map(PlatGroupActivity::getParticipantCount).orElse(0);
        return Map.of("signedUp", true, "participantCount", updatedCount, "message", "报名成功");
    }

    @PostMapping("/{id}/activities/{activityId}/cancel-signup")
    @Transactional
    public Map<String, Object> cancelSignUp(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader) {

        User user = adminAuthService.requireUser(authHeader);
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        PlatGroupActivity activity = activityRepository.findByIdAndGroupId(activityId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        if (activity.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "活动已结束，无法取消报名");
        }

        PlatGroupActivitySignup signup = activitySignupRepository
                .findByActivityIdAndUserId(activityId, user.getUserid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "你未报名此活动"));
        if (!"signed_up".equals(signup.getStatus())) {
            return Map.of("cancelled", true, "message", "已取消报名");
        }

        signup.setStatus("cancelled");
        signup.setUpdatedAt(LocalDateTime.now());
        activitySignupRepository.save(signup);

        activityRepository.decrementParticipantCount(activityId);

        int updatedCount = activityRepository.findById(activityId).map(PlatGroupActivity::getParticipantCount).orElse(0);
        return Map.of("cancelled", true, "participantCount", updatedCount, "message", "已取消报名");
    }

    @PatchMapping("/{id}/activities/{activityId}")
    @Transactional
    public Map<String, Object> updateActivity(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {

        requireManagerRole(id, authHeader);
        PlatGroupActivity activity = activityRepository.findByIdAndGroupId(activityId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        if (payload.containsKey("title")) {
            String title = String.valueOf(payload.get("title")).trim();
            if (!title.isBlank()) activity.setTitle(title);
        }
        if (payload.containsKey("description")) {
            activity.setDescription(String.valueOf(payload.get("description")).trim());
        }
        if (payload.containsKey("location")) {
            String loc = String.valueOf(payload.get("location")).trim();
            activity.setLocation(loc.isBlank() ? null : loc);
        }
        if (payload.containsKey("startTime")) {
            try { activity.setStartTime(LocalDateTime.parse(String.valueOf(payload.get("startTime")))); } catch (Exception ignored) {}
        }
        if (payload.containsKey("endTime")) {
            try { activity.setEndTime(LocalDateTime.parse(String.valueOf(payload.get("endTime")))); } catch (Exception ignored) {}
        }
        if (!activity.getEndTime().isAfter(activity.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        if (payload.containsKey("maxParticipants")) {
            activity.setMaxParticipants(Math.max(0, Integer.parseInt(String.valueOf(payload.get("maxParticipants")))));
        }
        if (payload.containsKey("status")) {
            String newStatus = String.valueOf(payload.get("status")).trim();
            if (List.of("published", "cancelled", "ended").contains(newStatus)) {
                activity.setStatus(newStatus);
                // 取消活动时通知报名成员
                if ("cancelled".equals(newStatus)) {
                    List<PlatGroupActivitySignup> signups = activitySignupRepository.findByActivityId(activityId);
                    final String actTitle = activity.getTitle();
                    signups.stream().filter(s -> "signed_up".equals(s.getStatus())).forEach(s ->
                            notificationService.send(s.getUserId(), "GROUP_ACTIVITY_CANCELLED",
                                    "团体活动已取消：" + actTitle,
                                    "你报名的活动「" + actTitle + "」已被取消",
                                    "platform_group", String.valueOf(id)));
                }
            }
        }

        if (payload.containsKey("platformEventId")) {
            String eventId = String.valueOf(payload.get("platformEventId")).trim();
            activity.setPlatformEventId(eventId.isBlank() ? null : eventId);
        }

        activity.setUpdatedAt(LocalDateTime.now());
        activityRepository.save(activity);
        return Map.of("updated", true, "message", "活动已更新");
    }

    @PostMapping("/{id}/activities/{activityId}/checkin-code")
    @Transactional
    public Map<String, Object> generateActivityCheckinCode(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader) {
        requireManagerRole(id, authHeader);
        return activityEngagementService.generateCheckinCode(id, activityId);
    }

    @PostMapping("/{id}/activities/{activityId}/checkin")
    @Transactional
    public Map<String, Object> checkinActivity(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        User user = adminAuthService.requireUser(authHeader);
        String code = body.get("code") != null ? String.valueOf(body.get("code")) : null;
        return activityEngagementService.checkin(user, id, activityId, code);
    }

    @GetMapping("/{id}/activities/{activityId}/review-candidates")
    public List<Map<String, Object>> activityReviewCandidates(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader) {
        User user = adminAuthService.requireUser(authHeader);
        return activityEngagementService.listReviewCandidates(user, id, activityId);
    }

    @PostMapping("/{id}/activities/{activityId}/reviews")
    @Transactional
    public Map<String, Object> submitActivityReview(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        User user = adminAuthService.requireUser(authHeader);
        Long targetUserId = body.get("targetUserId") != null
                ? Long.parseLong(String.valueOf(body.get("targetUserId"))) : null;
        Integer rating = body.get("rating") != null
                ? Integer.parseInt(String.valueOf(body.get("rating"))) : null;
        String comment = body.get("comment") != null ? String.valueOf(body.get("comment")) : null;
        return activityEngagementService.submitReview(user, id, activityId, targetUserId, rating, comment);
    }

    @GetMapping("/{id}/weekly-digest")
    public Map<String, Object> previewWeeklyDigest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        requireManagerRole(id, authHeader);
        return weeklyDigestService.buildDigest(id);
    }

    @PostMapping("/{id}/weekly-digest/send")
    @Transactional
    public Map<String, Object> sendWeeklyDigest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        requireManagerRole(id, authHeader);
        return weeklyDigestService.sendDigestForGroup(id, true);
    }

    private Map<String, Object> buildActivityItem(
            PlatGroupActivity a,
            User creator,
            PlatGroupActivitySignup mySignup,
            Long userId,
            LocalDateTime now
    ) {
        boolean isEnded = !a.getEndTime().isAfter(now);
        boolean signedUp = mySignup != null;
        boolean checkedIn = mySignup != null && Boolean.TRUE.equals(mySignup.getCheckedIn());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", a.getId());
        item.put("groupId", a.getGroupId());
        item.put("title", a.getTitle());
        item.put("description", a.getDescription() != null ? a.getDescription() : "");
        item.put("startTime", a.getStartTime());
        item.put("endTime", a.getEndTime());
        item.put("location", a.getLocation() != null ? a.getLocation() : "");
        item.put("maxParticipants", a.getMaxParticipants());
        item.put("participantCount", a.getParticipantCount());
        item.put("status", a.getStatus());
        item.put("isEnded", isEnded);
        item.put("signedUpByMe", signedUp);
        item.put("checkedInByMe", checkedIn);
        item.put("hasCheckinCode", a.getCheckinCode() != null && !a.getCheckinCode().isBlank());
        item.put("creatorName", creator != null ? creator.getUsername() : "");
        item.put("createdAt", a.getCreatedAt());
        if (a.getPlatformEventId() != null && !a.getPlatformEventId().isBlank()) {
            item.put("platformEventId", a.getPlatformEventId());
            item.put("platformEventPath", "/events/" + a.getPlatformEventId());
            platformEventRepository.findById(a.getPlatformEventId()).ifPresent(pe -> {
                item.put("platformEventTitle", pe.getTitle());
            });
        }
        if (userId != null) {
            item.putAll(activityEngagementService.buildReviewSummary(userId, a.getId(), checkedIn, isEnded));
        }
        return item;
    }

    // ── 任务进度辅助（在现有发帖/评论/点赞逻辑中调用） ─────────────────────

    private void completeGroupTask(Long groupId, Long userId, String taskCode) {
        if (!TASK_NAMES.containsKey(taskCode)) return;
        LocalDate today = LocalDate.now();
        PlatGroupTaskProgress progress = taskProgressRepository
                .findByGroupIdAndUserIdAndTaskCodeAndTaskDate(groupId, userId, taskCode, today)
                .orElseGet(() -> {
                    PlatGroupTaskProgress p = new PlatGroupTaskProgress();
                    p.setGroupId(groupId);
                    p.setUserId(userId);
                    p.setTaskCode(taskCode);
                    p.setTaskDate(today);
                    p.setCompleted(0);
                    p.setClaimed(0);
                    return p;
                });
        if (progress.getCompleted() == 0) {
            progress.setCompleted(1);
            progress.setCompletedAt(LocalDateTime.now());
            taskProgressRepository.save(progress);
        }
    }
}
