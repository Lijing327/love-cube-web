package com.lovecube.backend.controllers;

import com.lovecube.backend.entity.GroupJoinRequest;
import com.lovecube.backend.entity.GroupMember;
import com.lovecube.backend.entity.GroupPost;
import com.lovecube.backend.entity.PlatGroup;
import com.lovecube.backend.entity.PlatGroupMember;
import com.lovecube.backend.entity.PlatformGroup;
import com.lovecube.backend.entity.PlatformGroupAdmin;
import com.lovecube.backend.models.User;
import com.lovecube.backend.repository.GroupJoinRequestRepository;
import com.lovecube.backend.repository.GroupMemberRepository;
import com.lovecube.backend.repository.GroupPostRepository;
import com.lovecube.backend.repository.PlatGroupMemberRepository;
import com.lovecube.backend.repository.PlatGroupRepository;
import com.lovecube.backend.repository.PlatformGroupAdminRepository;
import com.lovecube.backend.repository.PlatformGroupRepository;
import com.lovecube.backend.repository.UserRepository;
import com.lovecube.backend.services.AdminAuthService;
import com.lovecube.backend.services.GroupAdminRoleConstants;
import com.lovecube.backend.services.GroupMemberRealNameSupport;
import com.lovecube.backend.services.PermissionConstants;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/groups")
public class AdminGroupController {

    private final PlatformGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupPostRepository postRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final PlatformGroupAdminRepository groupAdminRepository;
    private final PlatGroupRepository platGroupRepository;
    private final PlatGroupMemberRepository platGroupMemberRepository;
    private final UserRepository userRepository;
    private final AdminAuthService adminAuthService;

    public AdminGroupController(
            PlatformGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            GroupPostRepository postRepository,
            GroupJoinRequestRepository joinRequestRepository,
            PlatformGroupAdminRepository groupAdminRepository,
            PlatGroupRepository platGroupRepository,
            PlatGroupMemberRepository platGroupMemberRepository,
            UserRepository userRepository,
            AdminAuthService adminAuthService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.groupAdminRepository = groupAdminRepository;
        this.platGroupRepository = platGroupRepository;
        this.platGroupMemberRepository = platGroupMemberRepository;
        this.userRepository = userRepository;
        this.adminAuthService = adminAuthService;
    }

    // ── 团体列表 ───────────────────────────────────────────────────────────────

    /** SUPER_ADMIN 看全部；GROUP_OWNER/ADMIN/REVIEWER 只看自己管理的，并携带角色信息 */
    @GetMapping
    public List<Map<String, Object>> listGroups(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = adminAuthService.requireUser(authHeader);
        Set<String> perms = adminAuthService.getUserPermissions(user);
        boolean manageAll = perms.contains(PermissionConstants.GROUP_MANAGE_ALL);
        boolean manageOwn = perms.contains(PermissionConstants.GROUP_MANAGE_OWN);

        if (!manageAll && !manageOwn) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无团体管理权限");
        }

        List<PlatformGroup> groups;
        if (manageAll) {
            groups = groupRepository.findAllOrderByPinnedDescCreatedAtDesc();
        } else {
            List<String> ids = adminAuthService.getManagedGroupIds(user);
            groups = ids.isEmpty() ? Collections.emptyList() : groupRepository.findAllById(ids);
        }

        List<Map<String, Object>> modernItems = groups.stream().map(g -> {
            Map<String, Object> item = buildGroupDetail(g);
            item.put("pendingRequestCount",
                    joinRequestRepository.findByGroupIdAndStatusOrderByRequestedAtDesc(g.getId(), "pending").size());
            String tableRole = adminAuthService.getGroupRole(user, g.getId());
            if (tableRole == null && manageAll) {
                item.put("userRole", null);
                item.put("userRoleName", "平台监管");
                item.put("userPermissions", buildGroupPermissions(GroupAdminRoleConstants.OWNER));
                item.put("regulatingAsPlatformAdmin", true);
            } else {
                String norm = tableRole != null ? tableRole : GroupAdminRoleConstants.OWNER;
                item.put("userRole", norm);
                item.put("userRoleName", GroupAdminRoleConstants.displayName(norm));
                item.put("userPermissions", buildGroupPermissions(norm));
                item.put("regulatingAsPlatformAdmin", false);
            }
            return item;
        }).collect(Collectors.toList());

        if (!manageAll) {
            return modernItems;
        }

        // 兼容旧平台团体表：超级管理员应可在“我的团体”中看见全量团体数据。
        List<Map<String, Object>> legacyItems = platGroupRepository
                .findByStatusOrderByMemberCountDescCreatedAtDesc("published")
                .stream()
                .map(g -> {
                    Map<String, Object> item = buildLegacyGroupDetail(g);
                    item.put("pendingRequestCount",
                            platGroupMemberRepository.findByGroupIdAndStatusOrderByJoinedAtAsc(g.getId(), "pending").size());
                    item.put("userRole", null);
                    item.put("userRoleName", "平台监管");
                    item.put("userPermissions", buildGroupPermissions(GroupAdminRoleConstants.OWNER));
                    item.put("regulatingAsPlatformAdmin", true);
                    return item;
                })
                .collect(Collectors.toList());

        if (legacyItems.isEmpty()) {
            return modernItems;
        }
        Set<String> modernIds = modernItems.stream()
                .map(item -> String.valueOf(item.getOrDefault("id", "")))
                .collect(Collectors.toSet());

        List<Map<String, Object>> merged = new ArrayList<>(modernItems.size() + legacyItems.size());
        merged.addAll(modernItems);
        for (Map<String, Object> legacyItem : legacyItems) {
            String legacyId = String.valueOf(legacyItem.getOrDefault("id", ""));
            // If modern table already contains migrated id (legacy-{id}) or same raw id, skip legacy duplicate.
            if (modernIds.contains(legacyId) || modernIds.contains("legacy-" + legacyId)) {
                continue;
            }
            merged.add(legacyItem);
        }
        return merged;
    }

    /** 团体详情（后台）：含当前用户在该团体内的角色与权限，供编辑页与 tab 控制 */
    @GetMapping("/{id}")
    public Map<String, Object> getGroupForAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id
    ) {
        User user = adminAuthService.requireUser(authHeader);
        Optional<PlatformGroup> modern = findModernGroup(id);
        Optional<PlatGroup> space = modern.isEmpty() ? findSpaceGroup(id) : Optional.empty();
        if (modern.isEmpty() && space.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在");
        }
        adminAuthService.requireGroupAdmin(authHeader, id);
        Map<String, Object> item = modern.isPresent()
                ? buildGroupDetail(modern.get())
                : buildLegacyGroupDetail(space.get());
        return attachAdminViewerMeta(item, user, id);
    }

    // ── 团体 CRUD ──────────────────────────────────────────────────────────────

    @PostMapping
    @Transactional
    public PlatformGroup createGroup(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload
    ) {
        User admin = adminAuthService.requirePermission(authHeader, PermissionConstants.GROUP_MANAGE_ALL);
        String name = String.valueOf(payload.getOrDefault("name", "")).trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "团体名称不能为空");
        }
        PlatformGroup group = new PlatformGroup();
        group.setId("group-" + UUID.randomUUID());
        group.setName(name);
        group.setDescription(String.valueOf(payload.getOrDefault("description", "")));
        group.setCategory(String.valueOf(payload.getOrDefault("category", "")));
        group.setCoverUrl(String.valueOf(payload.getOrDefault("coverUrl", "")));
        group.setStatus(String.valueOf(payload.getOrDefault("status", "active")));
        group.setJoinType(String.valueOf(payload.getOrDefault("joinType", "approval")));
        group.setMemberCount(1);
        group.setPinned(Boolean.parseBoolean(String.valueOf(payload.getOrDefault("pinned", "false"))));
        group.setCreatedBy(admin.getUserid());
        group.setOwnerUserId(admin.getUserid());
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        PlatformGroup saved = groupRepository.save(group);

        adminAuthService.upsertPlatformGroupAdmin(saved.getId(), admin.getUserid(), GroupAdminRoleConstants.OWNER);

        GroupMember creatorMember = new GroupMember();
        creatorMember.setGroupId(saved.getId());
        creatorMember.setUserId(admin.getUserid());
        creatorMember.setRole("owner");
        creatorMember.setJoinedAt(LocalDateTime.now());
        GroupMemberRealNameSupport.optionalMemberRealName(payload).ifPresent(creatorMember::setMemberRealName);
        memberRepository.save(creatorMember);

        return saved;
    }

    /** OWNER / ADMIN 可编辑团体资料 */
    @PutMapping("/{id}")
    public Map<String, Object> updateGroup(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @RequestBody Map<String, Object> payload
    ) {
        adminAuthService.requireGroupManagePermission(authHeader, id);
        Optional<PlatformGroup> modern = findModernGroup(id);
        if (modern.isPresent()) {
            PlatformGroup group = modern.get();
            if (payload.containsKey("name")) {
                String name = String.valueOf(payload.get("name")).trim();
                if (!name.isBlank()) group.setName(name);
            }
            if (payload.containsKey("description")) group.setDescription(String.valueOf(payload.get("description")));
            if (payload.containsKey("category"))    group.setCategory(String.valueOf(payload.get("category")));
            if (payload.containsKey("coverUrl"))    group.setCoverUrl(String.valueOf(payload.get("coverUrl")));
            if (payload.containsKey("status"))      group.setStatus(String.valueOf(payload.get("status")));
            if (payload.containsKey("joinType"))    group.setJoinType(String.valueOf(payload.get("joinType")));
            if (payload.containsKey("pinned"))      group.setPinned(Boolean.parseBoolean(String.valueOf(payload.get("pinned"))));
            group.setUpdatedAt(LocalDateTime.now());
            return buildGroupDetail(groupRepository.save(group));
        }
        PlatGroup space = findSpaceGroup(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在"));
        applySpaceGroupUpdates(space, payload);
        space.setUpdatedAt(LocalDateTime.now());
        return buildLegacyGroupDetail(platGroupRepository.save(space));
    }

    /** SUPER_ADMIN 或 OWNER 可删除 */
    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> deleteGroup(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id
    ) {
        adminAuthService.requireGroupOwnerOrSuperAdmin(authHeader, id);
        Optional<PlatformGroup> modern = findModernGroup(id);
        if (modern.isPresent()) {
            String canonicalId = modern.get().getId();
            memberRepository.findByGroupIdOrderByJoinedAtAsc(canonicalId).forEach(memberRepository::delete);
            postRepository.findByGroupIdOrderByCreatedAtDesc(canonicalId).forEach(postRepository::delete);
            joinRequestRepository.findByGroupIdOrderByRequestedAtDesc(canonicalId).forEach(joinRequestRepository::delete);
            groupAdminRepository.findByGroupId(canonicalId).forEach(groupAdminRepository::delete);
            groupRepository.deleteById(canonicalId);
            return Map.of("id", canonicalId, "message", "团体已删除");
        }
        PlatGroup space = findSpaceGroup(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在"));
        platGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(space.getId())
                .forEach(platGroupMemberRepository::delete);
        platGroupRepository.delete(space);
        return Map.of("id", String.valueOf(space.getId()), "message", "团体已删除");
    }

    // ── 入团申请（OWNER / ADMIN / REVIEWER） ──────────────────────────────────

    @GetMapping("/{id}/join-requests")
    public List<Map<String, Object>> listJoinRequests(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @RequestParam(defaultValue = "pending") String status
    ) {
        adminAuthService.requireGroupReviewPermission(authHeader, id);
        Optional<PlatformGroup> modern = findModernGroup(id);
        if (modern.isEmpty()) {
            PlatGroup space = findSpaceGroup(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在"));
            return buildSpaceJoinRequests(space, status);
        }
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<GroupJoinRequest> requests = new ArrayList<>();
        for (String gid : adminAuthService.expandGroupIdAliasesForLegacy(id)) {
            List<GroupJoinRequest> chunk = "all".equals(status)
                    ? joinRequestRepository.findByGroupIdOrderByRequestedAtDesc(gid)
                    : joinRequestRepository.findByGroupIdAndStatusOrderByRequestedAtDesc(gid, status);
            for (GroupJoinRequest r : chunk) {
                if (seen.add(r.getId())) {
                    requests.add(r);
                }
            }
        }
        requests.sort(Comparator.comparing(GroupJoinRequest::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        Set<Long> userIds = requests.stream().map(GroupJoinRequest::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));
        return requests.stream().map(r -> {
            User u = userMap.get(r.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("userId", r.getUserId());
            item.put("status", r.getStatus());
            item.put("message", r.getMessage());
            item.put("requestedAt", r.getRequestedAt());
            item.put("handledAt", r.getHandledAt());
            item.put("username", u != null ? u.getUsername() : "");
            item.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
            item.put("memberRealName", r.getMemberRealName());
            return item;
        }).collect(Collectors.toList());
    }

    @PatchMapping("/{id}/join-requests/{requestId}/approve")
    @Transactional
    public Map<String, Object> approveRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @PathVariable Long requestId
    ) {
        adminAuthService.requireGroupReviewPermission(authHeader, id);
        GroupJoinRequest req = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申请不存在"));
        if (!adminAuthService.isSameLegacyGroup(id, req.getGroupId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申请与团体不匹配");
        }
        if (!"pending".equals(req.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该申请已处理");
        String canonicalId = adminAuthService.resolvePlatformGroupIdOrThrow(id);
        PlatformGroup group = groupRepository.findById(canonicalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在"));
        if (!memberRepository.existsByGroupIdAndUserId(canonicalId, req.getUserId())) {
            GroupMember member = new GroupMember();
            member.setGroupId(canonicalId);
            member.setUserId(req.getUserId());
            member.setRole("member");
            member.setJoinedAt(LocalDateTime.now());
            member.setMemberRealName(req.getMemberRealName());
            memberRepository.save(member);
            group.setMemberCount((group.getMemberCount() == null ? 0 : group.getMemberCount()) + 1);
            groupRepository.save(group);
        }
        req.setStatus("approved");
        req.setHandledAt(LocalDateTime.now());
        joinRequestRepository.save(req);
        return Map.of("id", requestId, "status", "approved", "message", "已通过申请");
    }

    @PatchMapping("/{id}/join-requests/{requestId}/reject")
    @Transactional
    public Map<String, Object> rejectRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @PathVariable Long requestId
    ) {
        adminAuthService.requireGroupReviewPermission(authHeader, id);
        GroupJoinRequest req = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申请不存在"));
        if (!adminAuthService.isSameLegacyGroup(id, req.getGroupId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申请与团体不匹配");
        }
        if (!"pending".equals(req.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该申请已处理");
        req.setStatus("rejected");
        req.setHandledAt(LocalDateTime.now());
        joinRequestRepository.save(req);
        return Map.of("id", requestId, "status", "rejected", "message", "已拒绝申请");
    }

    // ── 成员管理（OWNER / ADMIN） ──────────────────────────────────────────────

    @GetMapping("/{id}/members")
    public List<Map<String, Object>> listMembers(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id
    ) {
        adminAuthService.requireGroupManagePermission(authHeader, id);
        Optional<PlatformGroup> modern = findModernGroup(id);
        if (modern.isEmpty()) {
            PlatGroup space = findSpaceGroup(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在"));
            return buildSpaceMembers(space);
        }
        String canonicalId = modern.get().getId();
        List<GroupMember> members = memberRepository.findByGroupIdOrderByJoinedAtAsc(canonicalId);
        Set<Long> uids = members.stream().map(GroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(uids).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));
        return members.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("userId", m.getUserId());
            item.put("role", m.getRole());
            item.put("joinedAt", m.getJoinedAt());
            item.put("username", u != null ? u.getUsername() : "");
            item.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
            item.put("memberRealName", m.getMemberRealName());
            return item;
        }).collect(Collectors.toList());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Transactional
    public Map<String, Object> removeMember(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @PathVariable Long userId
    ) {
        adminAuthService.requireGroupManagePermission(authHeader, id);
        PlatformGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在"));
        if (!memberRepository.existsByGroupIdAndUserId(id, userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该用户不是团体成员");
        }
        memberRepository.deleteByGroupIdAndUserId(id, userId);
        int count = group.getMemberCount() == null ? 0 : group.getMemberCount();
        group.setMemberCount(Math.max(0, count - 1));
        groupRepository.save(group);
        return Map.of("userId", userId, "message", "成员已移除");
    }

    // ── 团体动态（OWNER / ADMIN） ──────────────────────────────────────────────

    @PostMapping("/{id}/posts")
    public GroupPost createPost(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @RequestBody Map<String, Object> payload
    ) {
        User admin = adminAuthService.requireGroupManagePermission(authHeader, id);
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在"));
        String content = String.valueOf(payload.getOrDefault("content", "")).trim();
        if (content.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "内容不能为空");
        String type = String.valueOf(payload.getOrDefault("type", "post"));
        GroupPost post = new GroupPost();
        post.setId("gpost-" + UUID.randomUUID());
        post.setGroupId(id);
        post.setUserId(admin.getUserid());
        post.setType(type);
        post.setContent(content);
        post.setLikeCount(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    @DeleteMapping("/{id}/posts/{postId}")
    public Map<String, Object> deletePost(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @PathVariable String postId
    ) {
        adminAuthService.requireGroupManagePermission(authHeader, id);
        postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动态不存在"));
        postRepository.deleteById(postId);
        return Map.of("id", postId, "message", "动态已删除");
    }

    // ── 团体管理员管理（仅 OWNER） ────────────────────────────────────────────

    @GetMapping("/{id}/admins")
    public List<Map<String, Object>> listGroupAdmins(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id
    ) {
        adminAuthService.requireGroupOwner(authHeader, id);
        List<PlatformGroupAdmin> admins = groupAdminRepository.findByGroupId(id);
        Set<Long> uids = admins.stream().map(PlatformGroupAdmin::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(uids).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));
        return admins.stream().map(a -> {
            User u = userMap.get(a.getUserId());
            String normRole = GroupAdminRoleConstants.normalize(a.getRole());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("userId", a.getUserId());
            item.put("role", normRole);
            item.put("roleName", GroupAdminRoleConstants.displayName(normRole));
            item.put("createdAt", a.getCreatedAt());
            item.put("username", u != null ? u.getUsername() : "");
            item.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
            return item;
        }).collect(Collectors.toList());
    }

    @PostMapping("/{id}/admins")
    public Map<String, Object> addGroupAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @RequestBody Map<String, Object> payload
    ) {
        User operator = adminAuthService.requireGroupOwner(authHeader, id);
        Long targetUserId = Long.valueOf(String.valueOf(payload.get("userId")));
        String role = GroupAdminRoleConstants.normalize(String.valueOf(payload.getOrDefault("role", GroupAdminRoleConstants.ADMIN)));
        if (GroupAdminRoleConstants.OWNER.equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能直接添加 OWNER，请使用转让功能");
        }
        if (operator.getUserid().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能修改自己的角色");
        }
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在"));

        Optional<PlatformGroupAdmin> existing = groupAdminRepository.findByGroupIdAndUserId(id, targetUserId);
        PlatformGroupAdmin ga;
        if (existing.isPresent()) {
            ga = existing.get();
            ga.setRole(role);
        } else {
            ga = new PlatformGroupAdmin();
            ga.setGroupId(id);
            ga.setUserId(targetUserId);
            ga.setRole(role);
        }
        groupAdminRepository.save(ga);

        // 确保用户持有 GROUP_OWNER 角色码（用于后台访问权限）
        adminAuthService.ensureGroupOwnerRole(targetUserId);
        return Map.of("userId", targetUserId, "role", role,
                "roleName", GroupAdminRoleConstants.displayName(role), "message", "管理员已添加");
    }

    @DeleteMapping("/{id}/admins/{userId}")
    @Transactional
    public Map<String, Object> removeGroupAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @PathVariable Long userId
    ) {
        User operator = adminAuthService.requireGroupOwner(authHeader, id);
        if (operator.getUserid().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能移除自己（团体拥有者）");
        }
        // 不能移除其他 OWNER
        groupAdminRepository.findByGroupIdAndUserId(id, userId).ifPresent(ga -> {
            if (GroupAdminRoleConstants.isOwner(ga.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能移除团体拥有者");
            }
        });
        groupAdminRepository.deleteByGroupIdAndUserId(id, userId);
        adminAuthService.cleanGroupOwnerRoleIfUnused(userId);
        return Map.of("userId", userId, "message", "管理员已移除");
    }

    // ── 私有工具 ──────────────────────────────────────────────────────────────

    private Optional<PlatformGroup> findModernGroup(String id) {
        for (String gid : adminAuthService.expandGroupIdAliasesForLegacy(id)) {
            Optional<PlatformGroup> found = groupRepository.findById(gid);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<PlatGroup> findSpaceGroup(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String raw = id.startsWith("legacy-") ? id.substring("legacy-".length()) : id;
        if (raw.isEmpty() || !raw.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        try {
            return platGroupRepository.findById(Long.parseLong(raw));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Object> attachAdminViewerMeta(Map<String, Object> item, User user, String id) {
        String tableRole = adminAuthService.getGroupRole(user, id);
        if (tableRole == null && adminAuthService.hasGroupManageAll(user)) {
            item.put("userRole", null);
            item.put("userRoleName", "平台监管");
            item.put("userPermissions", buildGroupPermissions(GroupAdminRoleConstants.OWNER));
            item.put("regulatingAsPlatformAdmin", true);
        } else {
            String norm = tableRole != null ? tableRole : GroupAdminRoleConstants.OWNER;
            item.put("userRole", norm);
            item.put("userRoleName", GroupAdminRoleConstants.displayName(norm));
            item.put("userPermissions", buildGroupPermissions(norm));
            item.put("regulatingAsPlatformAdmin", false);
        }
        return item;
    }

    private void applySpaceGroupUpdates(PlatGroup group, Map<String, Object> payload) {
        if (payload.containsKey("name")) {
            String name = String.valueOf(payload.get("name")).trim();
            if (!name.isBlank()) group.setName(name);
        }
        if (payload.containsKey("description")) {
            group.setDescription(String.valueOf(payload.get("description")));
        }
        if (payload.containsKey("category")) {
            group.setType(String.valueOf(payload.get("category")));
        }
        if (payload.containsKey("coverUrl")) {
            group.setCoverUrl(String.valueOf(payload.get("coverUrl")));
        }
        if (payload.containsKey("region")) {
            group.setRegion(String.valueOf(payload.get("region")));
        }
        if (payload.containsKey("status")) {
            String status = String.valueOf(payload.get("status")).trim();
            if ("active".equals(status) || "published".equals(status)) {
                group.setStatus("published");
            } else if ("inactive".equals(status) || "disabled".equals(status)) {
                group.setStatus("disabled");
            } else if (!status.isBlank()) {
                group.setStatus(status);
            }
        }
        if (payload.containsKey("joinType")) {
            String joinType = String.valueOf(payload.get("joinType")).trim().toLowerCase();
            if ("open".equals(joinType) || "free".equals(joinType)) {
                group.setJoinMode("free");
            } else if ("invite".equals(joinType)) {
                group.setJoinMode("invite");
            } else {
                group.setJoinMode("audit");
            }
        }
    }

    private List<Map<String, Object>> buildSpaceMembers(PlatGroup group) {
        List<PlatGroupMember> members = platGroupMemberRepository
                .findByGroupIdAndStatusOrderByJoinedAtAsc(group.getId(), "approved");
        Set<Long> uids = members.stream().map(PlatGroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(uids).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));
        return members.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("userId", m.getUserId());
            item.put("role", m.getRole());
            item.put("status", m.getStatus());
            item.put("joinedAt", m.getJoinedAt());
            item.put("username", u != null ? u.getUsername() : "");
            item.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
            item.put("memberRealName", m.getMemberRealName());
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildSpaceJoinRequests(PlatGroup group, String status) {
        String queryStatus = (status == null || status.isBlank() || "all".equals(status)) ? null : status;
        List<PlatGroupMember> members = queryStatus == null
                ? platGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId())
                : platGroupMemberRepository.findByGroupIdAndStatusOrderByJoinedAtAsc(group.getId(), queryStatus);
        if (queryStatus == null) {
            members = members.stream()
                    .filter(m -> !"approved".equals(m.getStatus()))
                    .collect(Collectors.toList());
        }
        Set<Long> uids = members.stream().map(PlatGroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(uids).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));
        return members.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("userId", m.getUserId());
            item.put("status", m.getStatus());
            item.put("message", m.getApplyReason());
            item.put("requestedAt", m.getCreatedAt());
            item.put("handledAt", m.getUpdatedAt());
            item.put("username", u != null ? u.getUsername() : "");
            item.put("avatarUrl", u != null ? u.getProfilePhoto() : "");
            item.put("memberRealName", m.getMemberRealName());
            return item;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> buildGroupDetail(PlatformGroup g) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", g.getId());
        item.put("name", g.getName());
        item.put("description", g.getDescription());
        item.put("category", g.getCategory());
        item.put("coverUrl", g.getCoverUrl());
        item.put("status", g.getStatus());
        item.put("joinType", g.getJoinType());
        item.put("memberCount", g.getMemberCount() == null ? 0 : g.getMemberCount());
        item.put("pinned", Boolean.TRUE.equals(g.getPinned()));
        item.put("createdBy", g.getCreatedBy());
        item.put("ownerUserId", g.getOwnerUserId());
        item.put("createdAt", g.getCreatedAt());
        item.put("updatedAt", g.getUpdatedAt());
        return item;
    }

    private Map<String, Object> buildLegacyGroupDetail(PlatGroup g) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(g.getId()));
        item.put("name", g.getName());
        item.put("description", g.getDescription());
        item.put("category", g.getType());
        item.put("region", g.getRegion());
        item.put("coverUrl", g.getCoverUrl());
        item.put("status", "published".equals(g.getStatus()) ? "active" : g.getStatus());
        item.put("joinType", "free".equals(g.getJoinMode()) ? "open" : "approval");
        item.put("memberCount", g.getMemberCount() == null ? 0 : g.getMemberCount());
        item.put("pinned", false);
        item.put("createdBy", g.getOwnerUserId());
        item.put("ownerUserId", g.getOwnerUserId());
        item.put("createdAt", g.getCreatedAt());
        item.put("updatedAt", g.getUpdatedAt());
        return item;
    }

    private List<String> buildGroupPermissions(String role) {
        if (role == null) return Collections.emptyList();
        List<String> perms = new ArrayList<>();
        perms.add("group.review.member");
        if (GroupAdminRoleConstants.canManage(role)) {
            perms.add("group.edit.info");
            perms.add("group.manage.notice");
            perms.add("group.manage.post");
            perms.add("group.remove.member");
        }
        if (GroupAdminRoleConstants.isOwner(role)) {
            perms.add("group.manage.admins");
            perms.add("group.delete");
        }
        return perms;
    }
}
