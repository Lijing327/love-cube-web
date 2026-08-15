package com.lovecube.backend.services;

import com.lovecube.backend.entity.GroupMember;
import com.lovecube.backend.entity.PlatGroupArticle;
import com.lovecube.backend.entity.PlatGroupMember;
import com.lovecube.backend.models.User;
import com.lovecube.backend.repository.GroupMemberRepository;
import com.lovecube.backend.repository.PlatGroupArticleRepository;
import com.lovecube.backend.repository.PlatGroupMemberRepository;
import com.lovecube.backend.repository.PlatGroupRepository;
import com.lovecube.backend.repository.PlatformGroupRepository;
import com.lovecube.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupArticleService {

    private static final int TITLE_MAX = 200;
    private static final int SUMMARY_MAX = 500;
    private static final int CONTENT_MAX = 20000;

    private final PlatGroupArticleRepository articleRepository;
    private final PlatGroupRepository platGroupRepository;
    private final PlatformGroupRepository platformGroupRepository;
    private final PlatGroupMemberRepository platMemberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final AdminAuthService adminAuthService;

    public GroupArticleService(
            PlatGroupArticleRepository articleRepository,
            PlatGroupRepository platGroupRepository,
            PlatformGroupRepository platformGroupRepository,
            PlatGroupMemberRepository platMemberRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository,
            AdminAuthService adminAuthService
    ) {
        this.articleRepository = articleRepository;
        this.platGroupRepository = platGroupRepository;
        this.platformGroupRepository = platformGroupRepository;
        this.platMemberRepository = platMemberRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.adminAuthService = adminAuthService;
    }

    public List<Map<String, Object>> list(String groupId) {
        String gid = requireExistingGroupId(groupId);
        List<PlatGroupArticle> rows = articleRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(gid, "published");
        Set<Long> authorIds = rows.stream().map(PlatGroupArticle::getAuthorUserId).collect(Collectors.toSet());
        Map<Long, User> users = authorIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u));
        return rows.stream().map(a -> toItem(a, users.get(a.getAuthorUserId()))).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(String groupId, User user, Map<String, Object> payload) {
        String gid = requireExistingGroupId(groupId);
        requireApprovedMember(gid, user);
        String title = textOf(payload, "title");
        String content = textOf(payload, "content");
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写文章标题");
        }
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写文章正文");
        }
        if (title.length() > TITLE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能超过" + TITLE_MAX + "字");
        }
        if (content.length() > CONTENT_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正文不能超过" + CONTENT_MAX + "字");
        }
        String summary = textOf(payload, "summary");
        if (summary.isBlank()) {
            summary = content.length() > 120 ? content.substring(0, 120) : content;
        }
        if (summary.length() > SUMMARY_MAX) {
            summary = summary.substring(0, SUMMARY_MAX);
        }
        String coverUrl = textOf(payload, "coverUrl");
        LocalDateTime now = LocalDateTime.now();
        PlatGroupArticle article = new PlatGroupArticle();
        article.setGroupId(gid);
        article.setTitle(title);
        article.setSummary(summary);
        article.setContent(content);
        article.setCoverUrl(coverUrl.isBlank() ? null : coverUrl);
        article.setAuthorUserId(user.getUserid());
        article.setStatus("published");
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        return toItem(articleRepository.save(article), user);
    }

    @Transactional
    public Map<String, Object> delete(String groupId, Long articleId, User user) {
        String gid = requireExistingGroupId(groupId);
        PlatGroupArticle article = articleRepository.findByIdAndGroupId(articleId, gid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));
        boolean author = user.getUserid().equals(article.getAuthorUserId());
        if (!author && !isManager(gid, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "没有删除这篇文章的权限");
        }
        article.setStatus("deleted");
        article.setUpdatedAt(LocalDateTime.now());
        articleRepository.save(article);
        return Map.of("id", article.getId(), "message", "文章已删除");
    }

    private String requireExistingGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "团体 ID 无效");
        }
        String gid = groupId.trim();
        if (gid.chars().allMatch(Character::isDigit)) {
            long numericId = Long.parseLong(gid);
            if (platGroupRepository.existsById(numericId) || platformGroupRepository.existsById(gid)) {
                return gid;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在");
        }
        if (platformGroupRepository.existsById(gid)) {
            return gid;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "团体不存在");
    }

    private void requireApprovedMember(String groupId, User user) {
        if (isApprovedMember(groupId, user) || adminAuthService.isAdmin(user)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "加入团体后才能发布文章");
    }

    private boolean isApprovedMember(String groupId, User user) {
        if (user == null) return false;
        if (groupId.chars().allMatch(Character::isDigit)) {
            boolean spaceMember = platMemberRepository
                    .findByGroupIdAndUserId(Long.parseLong(groupId), user.getUserid())
                    .filter(m -> "approved".equals(m.getStatus()))
                    .isPresent();
            if (spaceMember) return true;
        }
        return groupMemberRepository.findByGroupIdAndUserId(groupId, user.getUserid()).isPresent();
    }

    private boolean isManager(String groupId, User user) {
        if (user == null) return false;
        if (adminAuthService.isAdmin(user) || adminAuthService.hasGroupManageAll(user)) {
            return true;
        }
        if (groupId.chars().allMatch(Character::isDigit)) {
            boolean spaceManager = platMemberRepository
                    .findByGroupIdAndUserId(Long.parseLong(groupId), user.getUserid())
                    .filter(m -> "approved".equals(m.getStatus()))
                    .map(PlatGroupMember::getRole)
                    .filter(role -> "owner".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role))
                    .isPresent();
            if (spaceManager) return true;
        }
        return groupMemberRepository.findByGroupIdAndUserId(groupId, user.getUserid())
                .map(GroupMember::getRole)
                .filter(role -> "owner".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role))
                .isPresent()
                || adminAuthService.hasPlatformGroupManageAccess(user, groupId);
    }

    private Map<String, Object> toItem(PlatGroupArticle article, User author) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", article.getId());
        item.put("groupId", article.getGroupId());
        item.put("title", article.getTitle());
        item.put("summary", article.getSummary());
        item.put("content", article.getContent());
        item.put("coverUrl", article.getCoverUrl());
        item.put("authorUserId", article.getAuthorUserId());
        item.put("authorName", author != null && author.getUsername() != null ? author.getUsername() : "成员");
        item.put("authorAvatarUrl", author != null ? author.getProfilePhoto() : "");
        item.put("status", article.getStatus());
        item.put("createdAt", article.getCreatedAt());
        item.put("updatedAt", article.getUpdatedAt());
        return item;
    }

    private static String textOf(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key) || payload.get(key) == null) {
            return "";
        }
        return String.valueOf(payload.get(key)).trim();
    }
}
