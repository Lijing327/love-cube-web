package com.lovecube.backend.services;

import com.lovecube.backend.entity.AdminUserRole;
import com.lovecube.backend.entity.PlatformGroupAdmin;
import com.lovecube.backend.models.User;
import com.lovecube.backend.repository.AdminRolePermissionRepository;
import com.lovecube.backend.repository.AdminUserRoleRepository;
import com.lovecube.backend.repository.PlatGroupRepository;
import com.lovecube.backend.repository.PlatformGroupAdminRepository;
import com.lovecube.backend.repository.PlatformGroupRepository;
import com.lovecube.backend.repository.UserRepository;
import com.lovecube.backend.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminAuthService {
    /**
     * Hidden super admin whitelist — never expose via any API/UI.
     */
    private static final Set<String> HIDDEN_SUPER_ADMIN_PHONES = Set.of(
            "15030251407"
    );
    private static final Set<Long> HIDDEN_SUPER_ADMIN_USER_IDS = Set.of();

    private static final Set<String> LEGACY_ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "ROOT");

    private final UserRepository userRepository;
    private final AdminUserRoleRepository adminUserRoleRepository;
    private final AdminRolePermissionRepository adminRolePermissionRepository;
    private final PlatformGroupAdminRepository platformGroupAdminRepository;
    private final PlatformGroupRepository platformGroupRepository;
    private final PlatGroupRepository platGroupRepository;

    public AdminAuthService(
            UserRepository userRepository,
            AdminUserRoleRepository adminUserRoleRepository,
            AdminRolePermissionRepository adminRolePermissionRepository,
            PlatformGroupAdminRepository platformGroupAdminRepository,
            PlatformGroupRepository platformGroupRepository,
            PlatGroupRepository platGroupRepository
    ) {
        this.userRepository = userRepository;
        this.adminUserRoleRepository = adminUserRoleRepository;
        this.adminRolePermissionRepository = adminRolePermissionRepository;
        this.platformGroupAdminRepository = platformGroupAdminRepository;
        this.platformGroupRepository = platformGroupRepository;
        this.platGroupRepository = platGroupRepository;
    }

    public User requireUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少登录令牌");
        }
        String token = authHeader.substring(7);
        String openid = JwtUtil.getOpenIdFromToken(token);
        if (openid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token 已失效");
        }
        User user = userRepository.findByOpenid(openid);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        return user;
    }

    /** 要求是任意管理员（有任意后台权限） */
    public User requireAdmin(String authHeader) {
        User user = requireUser(authHeader);
        if (!isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无管理员权限");
        }
        return user;
    }

    /** 要求拥有指定权限，返回当前用户 */
    public User requirePermission(String authHeader, String permission) {
        User user = requireUser(authHeader);
        Set<String> permissions = getUserPermissions(user);
        if (!permissions.contains(permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无操作权限: " + permission);
        }
        return user;
    }

    /** 要求是隐藏超级管理员 */
    public void requireHiddenSuperAdmin(String authHeader) {
        User user = requireUser(authHeader);
        if (!isHiddenSuperAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }
    }

    // ── 团体角色方法 ────────────────────────────────────────────────────────────

    /** 全站「监管全部团体」权限 */
    public boolean hasGroupManageAll(User user) {
        return getUserPermissions(user).contains(PermissionConstants.GROUP_MANAGE_ALL);
    }

    /**
     * 团体内角色，仅来自 platform_group_admin。
     * 超级管理员若无记录则返回 null（不代表该团体的 OWNER）。
     */
    public String getGroupRoleFromTable(User user, String groupId) {
        return platformGroupAdminRepository.findByGroupIdAndUserId(groupId, user.getUserid())
                .map(a -> GroupAdminRoleConstants.normalize(a.getRole()))
                .orElse(null);
    }

    /** 与 {@link #getGroupRoleFromTable} 相同，供外部语义使用 */
    public String getGroupRole(User user, String groupId) {
        return findGroupRoleAcrossAliases(user, groupId);
    }

    /**
     * 旧平台团体迁移后，platform_groups.id 多为 {@code legacy-{数字}}，而 platform_group_admin.group_id
     * 可能仍为纯数字。返回路径 id 与其 legacy 别名（双向）供权限与数据解析共用。
     */
    /**
     * 供 Controller 按多种历史 id 形态查询同一条 platform_groups 记录（如 20 与 legacy-20）。
     */
    public List<String> expandGroupIdAliasesForLegacy(String groupId) {
        return expandLegacyGroupIdAliases(groupId);
    }

    private List<String> expandLegacyGroupIdAliases(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(groupId);
        if (groupId.startsWith("legacy-")) {
            String rest = groupId.substring("legacy-".length());
            if (!rest.isEmpty()) {
                out.add(rest);
            }
        } else if (groupId.chars().allMatch(Character::isDigit)) {
            out.add("legacy-" + groupId);
        }
        return new ArrayList<>(out);
    }

    private String findGroupRoleAcrossAliases(User user, String groupId) {
        for (String gid : expandLegacyGroupIdAliases(groupId)) {
            String r = getGroupRoleFromTable(user, gid);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private boolean isGroupOwnerOrCreatorAcrossAliases(Long userId, String groupId) {
        for (String gid : expandLegacyGroupIdAliases(groupId)) {
            if (isGroupOwnerOrCreator(userId, gid)) {
                return true;
            }
        }
        return false;
    }

    /** 路径上的团体 id 与入团申请记录的 group_id 是否为同一团体（含 legacy 别名） */
    public boolean isSameLegacyGroup(String pathGroupId, String requestGroupId) {
        if (pathGroupId == null || requestGroupId == null) {
            return false;
        }
        if (pathGroupId.equals(requestGroupId)) {
            return true;
        }
        Set<String> a = new HashSet<>(expandLegacyGroupIdAliases(pathGroupId));
        for (String x : expandLegacyGroupIdAliases(requestGroupId)) {
            if (a.contains(x)) {
                return true;
            }
        }
        return false;
    }

    /** 解析 platform_groups 中实际存在的主键（legacy / 数字择一） */
    public String resolvePlatformGroupIdOrThrow(String groupId) {
        for (String gid : expandLegacyGroupIdAliases(groupId)) {
            if (platformGroupRepository.existsById(gid)) {
                return gid;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在");
    }

    /**
     * 兜底检查：platform_group_admin 中没有记录时，
     * 若用户就是该团体的 owner_user_id 或 created_by，则视同 OWNER。
     * 用于修复 V39 迁移遗漏 platform_group_admin 记录的 legacy 团体。
     */
    private boolean isGroupOwnerOrCreator(Long userId, String groupId) {
        if (platformGroupRepository.findById(groupId)
                .map(g -> userId.equals(g.getOwnerUserId()) || userId.equals(g.getCreatedBy()))
                .orElse(false)) {
            return true;
        }
        return parseSpaceGroupId(groupId)
                .flatMap(platGroupRepository::findById)
                .map(g -> userId.equals(g.getOwnerUserId()))
                .orElse(false);
    }

    private Optional<Long> parseSpaceGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return Optional.empty();
        }
        String raw = groupId.startsWith("legacy-") ? groupId.substring("legacy-".length()) : groupId;
        if (raw.isEmpty() || !raw.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * 要求用户在 platform_group_admin 中拥有指定角色之一。
     * 拥有 group.manage.all 的用户可监管任意团体（不等同于团体内 OWNER）。
     */
    public User requireGroupRole(String authHeader, String groupId, String... allowedRoles) {
        User user = requireUser(authHeader);
        if (hasGroupManageAll(user)) {
            return user;
        }
        String tableRole = findGroupRoleAcrossAliases(user, groupId);
        if (tableRole == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无该团体的访问权限");
        }
        Set<String> allowed = Set.of(allowedRoles);
        if (!allowed.contains(tableRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足，当前角色：" + GroupAdminRoleConstants.displayName(tableRole));
        }
        return user;
    }

    /** 本团体管理员（platform_group_admin 任一角）或全站团体监管员 */
    public User requireGroupAdmin(String authHeader, String groupId) {
        User user = requireUser(authHeader);
        if (findGroupRoleAcrossAliases(user, groupId) != null) return user;
        if (hasGroupManageAll(user)) return user;
        if (isGroupOwnerOrCreatorAcrossAliases(user.getUserid(), groupId)) return user;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无该团体的管理权限");
    }

    /** 团体 OWNER（表中角色）或全站团体监管员 */
    public User requireGroupOwner(String authHeader, String groupId) {
        User user = requireUser(authHeader);
        if (hasGroupManageAll(user)) return user;
        String tableRole = findGroupRoleAcrossAliases(user, groupId);
        if (GroupAdminRoleConstants.isOwner(tableRole)) return user;
        if (isGroupOwnerOrCreatorAcrossAliases(user.getUserid(), groupId)) return user;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要团体拥有者权限");
    }

    /** OWNER / ADMIN（表中）或全站监管 */
    public User requireGroupManagePermission(String authHeader, String groupId) {
        User user = requireUser(authHeader);
        if (hasGroupManageAll(user)) return user;
        String r = findGroupRoleAcrossAliases(user, groupId);
        if (r != null && (GroupAdminRoleConstants.OWNER.equals(r) || GroupAdminRoleConstants.ADMIN.equals(r))) return user;
        if (isGroupOwnerOrCreatorAcrossAliases(user.getUserid(), groupId)) return user;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无编辑该团体的权限");
    }

    /** 可审核入团：OWNER / ADMIN / REVIEWER 或全站监管 */
    public User requireGroupReviewPermission(String authHeader, String groupId) {
        User user = requireUser(authHeader);
        if (hasGroupManageAll(user)) return user;
        String r = findGroupRoleAcrossAliases(user, groupId);
        if (GroupAdminRoleConstants.canReview(r)) return user;
        if (isGroupOwnerOrCreatorAcrossAliases(user.getUserid(), groupId)) return user;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无成员审核权限");
    }

    /** 表中 OWNER 或全站监管（删除团体） */
    public User requireGroupOwnerOrSuperAdmin(String authHeader, String groupId) {
        User user = requireUser(authHeader);
        if (hasGroupManageAll(user)) return user;
        String tableRole = findGroupRoleAcrossAliases(user, groupId);
        if (GroupAdminRoleConstants.isOwner(tableRole)) return user;
        if (isGroupOwnerOrCreatorAcrossAliases(user.getUserid(), groupId)) return user;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有团体拥有者或超级管理员可删除团体");
    }

    /** 与 {@link #requireGroupManagePermission} 一致（不抛异常），用于前台「团体管理」能力。 */
    public boolean hasPlatformGroupManageAccess(User user, String groupId) {
        if (user == null || groupId == null || groupId.isBlank()) return false;
        if (hasGroupManageAll(user)) return true;
        String r = findGroupRoleAcrossAliases(user, groupId);
        if (r != null && (GroupAdminRoleConstants.OWNER.equals(r) || GroupAdminRoleConstants.ADMIN.equals(r))) {
            return true;
        }
        return isGroupOwnerOrCreatorAcrossAliases(user.getUserid(), groupId);
    }

    /** 与 {@link #requireGroupReviewPermission} 一致（不抛异常），用于入团申请审核入口。 */
    public boolean hasPlatformGroupJoinReviewAccess(User user, String groupId) {
        if (user == null || groupId == null || groupId.isBlank()) return false;
        if (hasGroupManageAll(user)) return true;
        String r = findGroupRoleAcrossAliases(user, groupId);
        if (GroupAdminRoleConstants.canReview(r)) return true;
        return isGroupOwnerOrCreatorAcrossAliases(user.getUserid(), groupId);
    }

    /**
     * 写入或更新 platform_group_admin；重复 group_id + user_id 时更新角色。
     * 并确保用户具备后台 GROUP_OWNER 入口角色码。
     */
    @Transactional
    public void upsertPlatformGroupAdmin(String groupId, Long userId, String role) {
        String norm = GroupAdminRoleConstants.normalize(role);
        Optional<PlatformGroupAdmin> existing = platformGroupAdminRepository.findByGroupIdAndUserId(groupId, userId);
        PlatformGroupAdmin ga = existing.orElseGet(() -> {
            PlatformGroupAdmin n = new PlatformGroupAdmin();
            n.setGroupId(groupId);
            n.setUserId(userId);
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });
        ga.setRole(norm);
        platformGroupAdminRepository.save(ga);
        ensureGroupOwnerRole(userId);
    }

    /**
     * 仅有团体负责人后台入口角色（{@code GROUP_OWNER}），且无遗留管理员身份、非隐藏超管。
     * 此类账号只应使用「我的团体」相关功能，不得进入全站运营工作台或拉取全站聚合统计。
     */
    public boolean isGroupStewardOnly(User user) {
        if (user == null || !isAdmin(user)) return false;
        if (isHiddenSuperAdmin(user)) return false;
        String legacyRole = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        if (LEGACY_ADMIN_ROLES.contains(legacyRole)) return false;
        String legacyStatus = user.getUserStatus() == null ? "" : user.getUserStatus().trim().toUpperCase(Locale.ROOT);
        if (LEGACY_ADMIN_ROLES.contains(legacyStatus)) return false;
        List<AdminUserRole> rows = adminUserRoleRepository.findByUserId(user.getUserid());
        if (rows == null || rows.isEmpty()) return false;
        if (rows.size() != 1) return false;
        String code = rows.get(0).getRoleCode();
        return code != null && "GROUP_OWNER".equalsIgnoreCase(code.trim());
    }

    public boolean isAdmin(User user) {
        if (user == null) return false;
        if (isHiddenSuperAdmin(user)) return true;

        // Legacy role check
        String role = user.getRole();
        if (role != null && LEGACY_ADMIN_ROLES.contains(role.trim().toUpperCase(Locale.ROOT))) return true;
        String userStatus = user.getUserStatus();
        if (userStatus != null && LEGACY_ADMIN_ROLES.contains(userStatus.trim().toUpperCase(Locale.ROOT))) return true;

        // Fine-grained role check — any entry in admin_user_role counts as admin
        return !adminUserRoleRepository.findByUserId(user.getUserid()).isEmpty();
    }

    public boolean isHiddenSuperAdmin(User user) {
        if (user == null) return false;
        if (user.getUserid() != null && HIDDEN_SUPER_ADMIN_USER_IDS.contains(user.getUserid())) return true;
        String phone = user.getPhoneNumber();
        return phone != null && HIDDEN_SUPER_ADMIN_PHONES.contains(phone);
    }

    /** 返回用户持有的所有角色码（合并遗留角色与细粒度角色） */
    public Set<String> getUserRoles(User user) {
        Set<String> roles = new LinkedHashSet<>();
        if (isHiddenSuperAdmin(user)) {
            roles.add("SUPER_ADMIN");
            return roles;
        }
        String legacyRole = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        if (LEGACY_ADMIN_ROLES.contains(legacyRole)) {
            // Legacy ADMIN/SUPER_ADMIN/ROOT all map to SUPER_ADMIN permissions
            roles.add("SUPER_ADMIN");
        }
        // Legacy userStatus fallback
        String legacyStatus = user.getUserStatus() == null ? "" : user.getUserStatus().trim().toUpperCase(Locale.ROOT);
        if (LEGACY_ADMIN_ROLES.contains(legacyStatus)) {
            roles.add("SUPER_ADMIN");
        }
        // Fine-grained roles from DB
        adminUserRoleRepository.findByUserId(user.getUserid())
                .forEach(r -> roles.add(r.getRoleCode()));
        return roles;
    }

    /** 返回用户持有的所有权限码 */
    public Set<String> getUserPermissions(User user) {
        Set<String> roles = getUserRoles(user);
        Set<String> permissions = new LinkedHashSet<>();
        for (String role : roles) {
            adminRolePermissionRepository.findByRoleCode(role)
                    .forEach(p -> permissions.add(p.getPermissionCode()));
        }
        return permissions;
    }

    /** 返回 GROUP_OWNER 有权管理的团体 ID 列表 */
    public List<String> getManagedGroupIds(User user) {
        return platformGroupAdminRepository.findByUserId(user.getUserid())
                .stream()
                .map(a -> a.getGroupId())
                .collect(Collectors.toList());
    }

    /** 确保用户持有 GROUP_OWNER 角色码，不存在则添加 */
    public void ensureGroupOwnerRole(Long userId) {
        if (!adminUserRoleRepository.existsByUserIdAndRoleCode(userId, "GROUP_OWNER")) {
            com.lovecube.backend.entity.AdminUserRole ur = new com.lovecube.backend.entity.AdminUserRole();
            ur.setUserId(userId);
            ur.setRoleCode("GROUP_OWNER");
            adminUserRoleRepository.save(ur);
        }
    }

    /** 如果用户已无任何团体管理权，移除 GROUP_OWNER 角色码 */
    public void cleanGroupOwnerRoleIfUnused(Long userId) {
        if (platformGroupAdminRepository.findByUserId(userId).isEmpty()) {
            adminUserRoleRepository.deleteByUserIdAndRoleCode(userId, "GROUP_OWNER");
        }
    }
}
