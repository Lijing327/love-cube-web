package com.lovecube.backend.services;

import com.lovecube.backend.entity.InviteRecord;
import com.lovecube.backend.entity.UserInviteCode;
import com.lovecube.backend.entity.UserInviteRelation;
import com.lovecube.backend.models.User;
import com.lovecube.backend.repository.InviteRecordRepository;
import com.lovecube.backend.repository.UserInviteCodeRepository;
import com.lovecube.backend.repository.UserInviteRelationRepository;
import com.lovecube.backend.repository.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FellowshipInviteService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RANDOM_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final InviteRecordRepository inviteRecordRepository;
    private final UserInviteCodeRepository userInviteCodeRepository;
    private final UserInviteRelationRepository userInviteRelationRepository;
    private final UserRepository userRepository;

    public FellowshipInviteService(
            InviteRecordRepository inviteRecordRepository,
            UserInviteCodeRepository userInviteCodeRepository,
            UserInviteRelationRepository userInviteRelationRepository,
            UserRepository userRepository
    ) {
        this.inviteRecordRepository = inviteRecordRepository;
        this.userInviteCodeRepository = userInviteCodeRepository;
        this.userInviteRelationRepository = userInviteRelationRepository;
        this.userRepository = userRepository;
    }

    public User validateInviteCodeForRegistration(String inviteCode) {
        String normalizedCode = normalizeInviteCode(inviteCode);
        if (normalizedCode.isBlank()) {
            throw new IllegalArgumentException("Invite code cannot be empty");
        }
        UserInviteCode codeRecord = userInviteCodeRepository.findByInviteCode(normalizedCode)
                .orElseGet(() -> migrateLegacyInviteCode(normalizedCode));
        if (codeRecord == null || "DISABLED".equalsIgnoreCase(codeRecord.getStatus())) {
            throw new IllegalArgumentException("Invalid invite code");
        }
        User inviter = userRepository.findById(codeRecord.getUserId()).orElse(null);
        if (inviter == null) {
            throw new IllegalArgumentException("Invalid invite code");
        }
        if ("DISABLED".equalsIgnoreCase(inviter.getUserStatus())
                || "DISABLED".equalsIgnoreCase(inviter.getInviteCodeStatus())) {
            throw new IllegalArgumentException("Invalid invite code");
        }
        return inviter;
    }

    public String generateUniqueInviteCode(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be empty");
        }
        String prefix = "LC" + userId;
        for (int i = 0; i < 20; i++) {
            String candidate = prefix + randomSuffix(4);
            if (!userInviteCodeRepository.existsByInviteCode(candidate) && !userRepository.existsByInviteCode(candidate)) {
                return candidate;
            }
        }
        String fallback = prefix + randomSuffix(6);
        if (!userInviteCodeRepository.existsByInviteCode(fallback) && !userRepository.existsByInviteCode(fallback)) {
            return fallback;
        }
        throw new IllegalStateException("Invite code generation failed");
    }

    @Transactional
    public String ensureUserInviteCode(User user) {
        Optional<UserInviteCode> existing = userInviteCodeRepository.findByUserId(user.getUserid());
        if (existing.isPresent()) {
            String code = existing.get().getInviteCode();
            syncLegacyInviteCode(user, code, existing.get().getStatus());
            return code;
        }

        String code = user.getInviteCode() != null && !user.getInviteCode().isBlank()
                ? normalizeInviteCode(user.getInviteCode())
                : generateUniqueInviteCode(user.getUserid());
        if (userInviteCodeRepository.existsByInviteCode(code)) {
            code = generateUniqueInviteCode(user.getUserid());
        }

        UserInviteCode record = new UserInviteCode();
        record.setUserId(user.getUserid());
        record.setInviteCode(code);
        record.setStatus(user.getInviteCodeStatus() == null || user.getInviteCodeStatus().isBlank()
                ? "ENABLED"
                : user.getInviteCodeStatus().trim().toUpperCase());
        userInviteCodeRepository.save(record);
        syncLegacyInviteCode(user, code, record.getStatus());
        return code;
    }

    @Transactional
    public void createSuccessRecord(String inviteCode, User inviter, User invitee, String registerIp, String userAgent) {
        if (inviter.getUserid().equals(invitee.getUserid())) {
            throw new IllegalArgumentException("Cannot use your own invite code");
        }
        if (userInviteRelationRepository.existsByInvitedUserId(invitee.getUserid())) {
            return;
        }

        String normalizedCode = normalizeInviteCode(inviteCode);
        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviteCode(normalizedCode);
        relation.setInviterUserId(inviter.getUserid());
        relation.setInvitedUserId(invitee.getUserid());
        relation.setRegisterIp(trim(registerIp, 64));
        relation.setRegisterUserAgent(trim(userAgent, 500));
        relation.setStatus("SUCCESS");
        userInviteRelationRepository.save(relation);

        InviteRecord record = new InviteRecord();
        record.setInviteCode(normalizedCode);
        record.setInviterUserId(inviter.getUserid());
        record.setInviteeUserId(invitee.getUserid());
        record.setInviteeUsername(resolveDisplayName(invitee));
        record.setRegisterIp(trim(registerIp, 64));
        record.setRegisterUserAgent(trim(userAgent, 500));
        record.setStatus("SUCCESS");
        inviteRecordRepository.save(record);
    }

    public long countEffectiveInvites(Long userId) {
        return userInviteRelationRepository.countByInviterUserIdAndStatus(userId, "EFFECTIVE");
    }

    public long countPendingInvites(Long userId) {
        return userInviteRelationRepository.countByInviterUserIdAndStatus(userId, "SUCCESS");
    }

    /** 累计邀请注册人数（SUCCESS + EFFECTIVE，注册即计入）。 */
    public long countRegisteredInvites(Long userId) {
        return countEffectiveInvites(userId) + countPendingInvites(userId);
    }

    public Map<String, Object> getMyCodeSummary(User user) {
        String code = ensureUserInviteCode(user);
        long effectiveCount = countEffectiveInvites(user.getUserid());
        long pendingCount = countPendingInvites(user.getUserid());
        long registeredCount = effectiveCount + pendingCount;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inviteCode", code);
        result.put("inviteCount", registeredCount);
        result.put("effectiveCount", effectiveCount);
        result.put("pendingCount", pendingCount);
        result.put("totalRegistered", registeredCount);
        return result;
    }

    public Map<String, Object> getInviteInfo(User user, String origin) {
        Map<String, Object> result = getMyCodeSummary(user);
        String normalizedOrigin = origin == null || origin.isBlank() ? "" : origin.trim().replaceAll("/+$", "");
        String inviteLink = normalizedOrigin.isBlank()
                ? "/register?inviteCode=" + result.get("inviteCode")
                : normalizedOrigin + "/register?inviteCode=" + result.get("inviteCode");
        result.put("inviteLink", inviteLink);
        return result;
    }

    public List<Map<String, Object>> getMyInvitees(User user) {
        List<UserInviteRelation> records = userInviteRelationRepository
                .findByInviterUserIdAndStatusOrderByCreatedAtDesc(user.getUserid(), "SUCCESS");
        List<UserInviteRelation> effectiveRecords = userInviteRelationRepository
                .findByInviterUserIdAndStatusOrderByCreatedAtDesc(user.getUserid(), "EFFECTIVE");
        List<UserInviteRelation> all = new ArrayList<>(records);
        all.addAll(effectiveRecords);
        all.sort(Comparator.comparing(UserInviteRelation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        Set<Long> inviteeIds = all.stream()
                .map(UserInviteRelation::getInvitedUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findAllById(inviteeIds).stream()
                .collect(Collectors.toMap(User::getUserid, item -> item));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserInviteRelation record : all) {
            User invitee = userMap.get(record.getInvitedUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", record.getInvitedUserId());
            item.put("nickname", invitee != null ? resolveDisplayName(invitee) : "");
            item.put("username", invitee != null ? safe(invitee.getUsername()) : "");
            item.put("registeredAt", record.getCreatedAt());
            item.put("status", invitee != null ? normalizeStatus(invitee.getUserStatus()) : "NORMAL");
            item.put("inviteStatus", "EFFECTIVE".equals(record.getStatus()) ? "EFFECTIVE" : "PENDING");
            item.put("effectiveAt", record.getEffectiveAt());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> searchInvites(
            Long inviterUserId,
            Long inviteeUserId,
            String inviteCode,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status
    ) {
        Specification<InviteRecord> specification = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (inviterUserId != null) {
                predicates.add(cb.equal(root.get("inviterUserId"), inviterUserId));
            }
            if (inviteeUserId != null) {
                predicates.add(cb.equal(root.get("inviteeUserId"), inviteeUserId));
            }
            if (inviteCode != null && !inviteCode.isBlank()) {
                predicates.add(cb.equal(root.get("inviteCode"), normalizeInviteCode(inviteCode)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<InviteRecord> records = inviteRecordRepository.findAll(specification);
        Set<Long> userIds = new HashSet<>();
        records.forEach(record -> {
            if (record.getInviterUserId() != null) {
                userIds.add(record.getInviterUserId());
            }
            if (record.getInviteeUserId() != null) {
                userIds.add(record.getInviteeUserId());
            }
        });
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserid, item -> item));

        List<Map<String, Object>> result = new ArrayList<>();
        for (InviteRecord record : records) {
            User inviter = userMap.get(record.getInviterUserId());
            User invitee = userMap.get(record.getInviteeUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", record.getId());
            item.put("inviteCode", record.getInviteCode());
            item.put("inviterUserId", record.getInviterUserId());
            item.put("inviterName", inviter == null ? "" : resolveDisplayName(inviter));
            item.put("inviteeUserId", record.getInviteeUserId());
            item.put("inviteeName", invitee == null ? safe(record.getInviteeUsername()) : resolveDisplayName(invitee));
            item.put("registerIp", safe(record.getRegisterIp()));
            item.put("registerUserAgent", safe(record.getRegisterUserAgent()));
            item.put("status", record.getStatus());
            item.put("createdAt", record.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    private String randomSuffix(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        }
        return builder.toString();
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode == null ? "" : inviteCode.trim().toUpperCase();
    }

    private UserInviteCode migrateLegacyInviteCode(String inviteCode) {
        User legacyUser = userRepository.findByInviteCode(inviteCode);
        if (legacyUser == null) {
            return null;
        }
        return userInviteCodeRepository.findByUserId(legacyUser.getUserid()).orElseGet(() -> {
            UserInviteCode record = new UserInviteCode();
            record.setUserId(legacyUser.getUserid());
            record.setInviteCode(inviteCode);
            record.setStatus(legacyUser.getInviteCodeStatus() == null || legacyUser.getInviteCodeStatus().isBlank()
                    ? "ENABLED"
                    : legacyUser.getInviteCodeStatus().trim().toUpperCase());
            return userInviteCodeRepository.save(record);
        });
    }

    private void syncLegacyInviteCode(User user, String code, String status) {
        boolean changed = false;
        if (!code.equals(user.getInviteCode())) {
            user.setInviteCode(code);
            changed = true;
        }
        if (user.getInviteCodeStatus() == null || user.getInviteCodeStatus().isBlank()) {
            user.setInviteCodeStatus(status == null || status.isBlank() ? "ENABLED" : status);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "";
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            return user.getPhoneNumber();
        }
        return "User" + user.getUserid();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "NORMAL";
        }
        return status.toUpperCase();
    }

    private String trim(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String val = text.trim();
        if (val.length() <= maxLength) {
            return val;
        }
        return val.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
