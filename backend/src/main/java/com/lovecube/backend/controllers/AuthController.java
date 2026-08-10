package com.lovecube.backend.controllers;

import com.lovecube.backend.models.User;
import com.lovecube.backend.growth.dto.GrowthEventCreateRequest;
import com.lovecube.backend.growth.enums.GrowthEventType;
import com.lovecube.backend.growth.enums.SourcePlatform;
import com.lovecube.backend.growth.service.GrowthEventService;
import com.lovecube.backend.repository.UserRepository;
import com.lovecube.backend.services.AdminAuthService;
import com.lovecube.backend.services.FellowshipInviteService;
import com.lovecube.backend.services.FellowshipProfileGrowthService;
import com.lovecube.backend.services.GrowthService;
import com.lovecube.backend.services.InviteEffectiveSettleService;
import com.lovecube.backend.services.LoginStreakService;
import com.lovecube.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int USERNAME_MAX_LENGTH = 20;
    private static final Pattern CHINA_MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FellowshipInviteService fellowshipInviteService;
    private final AdminAuthService adminAuthService;
    private final GrowthService growthService;
    private final GrowthEventService growthEventService;
    private final com.lovecube.backend.growth.service.GrowthRewardService growthRewardService;
    private final LoginStreakService loginStreakService;
    private final InviteEffectiveSettleService inviteEffectiveSettleService;
    private final FellowshipProfileGrowthService fellowshipProfileGrowthService;

    public AuthController(
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder,
            FellowshipInviteService fellowshipInviteService,
            AdminAuthService adminAuthService,
            GrowthService growthService,
            GrowthEventService growthEventService,
            com.lovecube.backend.growth.service.GrowthRewardService growthRewardService,
            LoginStreakService loginStreakService,
            InviteEffectiveSettleService inviteEffectiveSettleService,
            FellowshipProfileGrowthService fellowshipProfileGrowthService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fellowshipInviteService = fellowshipInviteService;
        this.adminAuthService = adminAuthService;
        this.growthService = growthService;
        this.growthEventService = growthEventService;
        this.growthRewardService = growthRewardService;
        this.loginStreakService = loginStreakService;
        this.inviteEffectiveSettleService = inviteEffectiveSettleService;
        this.fellowshipProfileGrowthService = fellowshipProfileGrowthService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String email = body.get("email");
        String password = body.get("password");

        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password cannot be empty"));
        }

        User user;
        if (phone != null && !phone.isEmpty()) {
            user = userRepository.findByPhoneNumber(phone);
        } else if (email != null && !email.isEmpty()) {
            user = userRepository.findByEmail(email);
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone or email is required"));
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User does not exist"));
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Incorrect password"));
        }

        if (user.getOpenid() == null || user.getOpenid().isEmpty()) {
            user.setOpenid("h5_" + user.getUserid());
            userRepository.save(user);
        }

        String token = JwtUtil.generateToken(user.getOpenid());
        growthService.recordAction(user.getUserid(), "LOGIN", "LOGIN_" + java.time.LocalDate.now());
        loginStreakService.recordLogin(user.getUserid(), LocalDate.now());
        try {
            inviteEffectiveSettleService.trySettleForInvitee(user.getUserid());
        } catch (Exception ignored) {
            // settle must not break login
        }
        publishGrowthEventSafely(
                GrowthEventType.USER_DAILY_ACTIVE,
                user.getUserid(),
                user.getUserid(),
                "auth_login",
                String.valueOf(user.getUserid()),
                "growth:user_daily_active:user:" + user.getUserid() + ":date:" + LocalDate.now(),
                SourcePlatform.API
        );
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserid());
        result.put("token", token);
        return ResponseEntity.ok(result);
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String phone = body.get("phone");
        String password = body.get("password");
        String username = body.get("username");
        String gender = body.get("gender");
        String inviteCodeRaw = body.get("inviteCode");
        String inviteCode = inviteCodeRaw == null ? "" : inviteCodeRaw.trim().toUpperCase();
        String normalizedUsername;
        Integer genderCode;

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone cannot be empty"));
        }
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters"));
        }
        try {
            normalizedUsername = normalizeUsername(username);
            genderCode = parseRegisterGender(gender);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
        if (userRepository.findByPhoneNumber(phone) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone number is already registered"));
        }

        if (inviteCode.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请填写邀请码"));
        }
        User inviter;
        try {
            inviter = fellowshipInviteService.validateInviteCodeForRegistration(inviteCode);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "邀请码无效，请检查后重试"));
        }

        try {
            User user = new User();
            user.setPhoneNumber(phone);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setUsername(normalizedUsername != null ? normalizedUsername : "User" + phone.substring(phone.length() - 4));
            user.setOpenid("h5_tmp_" + UUID.randomUUID().toString().replace("-", ""));
            user.setInvitedByUserId(inviter == null ? null : inviter.getUserid());
            user.setRegisterIp(resolveClientIp(request));
            user.setRegisterUserAgent(trim(request.getHeader("User-Agent"), 500));
            user.setUserStatus("NORMAL");
            user.setInviteCodeStatus("ENABLED");
            user.setGender(genderCode);
            user.setFellowshipEnabled(false);
            user.setFellowshipMatchVisible(false);

            User saved = userRepository.save(user);
            saved.setOpenid("h5_" + saved.getUserid());
            fellowshipInviteService.ensureUserInviteCode(saved);
            userRepository.save(saved);

            if (inviter != null) {
                fellowshipInviteService.createSuccessRecord(
                        inviteCode,
                        inviter,
                        saved,
                        saved.getRegisterIp(),
                        saved.getRegisterUserAgent()
                );
                try {
                    long registeredCount = fellowshipInviteService.countRegisteredInvites(inviter.getUserid());
                    growthRewardService.checkAndGrantInviteMilestoneRewards(inviter.getUserid(), (int) registeredCount);
                } catch (Exception ignored) {
                    // milestone reward must not break registration
                }
            }

            String token = JwtUtil.generateToken(saved.getOpenid());
            LocalDate today = LocalDate.now();
            growthService.recordAction(saved.getUserid(), "LOGIN", "LOGIN_" + today);
            loginStreakService.recordLogin(saved.getUserid(), today);
            try {
                inviteEffectiveSettleService.trySettleForInvitee(saved.getUserid());
            } catch (Exception ignored) {
                // settle must not break registration
            }
            publishGrowthEventSafely(
                    GrowthEventType.USER_REGISTERED,
                    saved.getUserid(),
                    saved.getUserid(),
                    "auth_register",
                    String.valueOf(saved.getUserid()),
                    "growth:user_registered:user:" + saved.getUserid(),
                    SourcePlatform.API
            );
            fellowshipProfileGrowthService.syncAfterRegistration(saved.getUserid());
            if (inviter != null) {
                publishGrowthEventSafely(
                        GrowthEventType.USER_INVITED_REGISTERED,
                        inviter.getUserid(),
                        saved.getUserid(),
                        "invite_relation",
                        String.valueOf(saved.getUserid()),
                        "growth:user_invited_registered:inviter:" + inviter.getUserid() + ":invitee:" + saved.getUserid(),
                        SourcePlatform.API
                );
                publishGrowthEventSafely(
                        GrowthEventType.USER_INVITED_EFFECTIVE,
                        inviter.getUserid(),
                        saved.getUserid(),
                        "invite_relation",
                        String.valueOf(saved.getUserid()),
                        "growth:user_invited_effective:inviter:" + inviter.getUserid() + ":invitee:" + saved.getUserid(),
                        SourcePlatform.API
                );
            }
            Map<String, Object> result = new HashMap<>();
            result.put("userId", saved.getUserid());
            result.put("token", token);
            return ResponseEntity.ok(result);
        } catch (DataIntegrityViolationException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            Throwable root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
            log.warn("Register failed: data integrity — phone={}, cause={}", phone, root.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message",
                    "Registration failed: data conflict. If this phone is new, try again later or contact support."
            ));
        } catch (IllegalStateException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.warn("Register failed: illegal state — phone={}, error={}", phone, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message",
                    "Invite code could not be allocated. Please try again in a moment."
            ));
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Register failed: unexpected — phone={}", phone, ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Registration failed, please try again"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body
    ) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (oldPassword == null || oldPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Old password cannot be empty"));
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password cannot be empty"));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 6 characters"));
        }
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));
        }
        if (newPassword.equals(oldPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password cannot match old password"));
        }

        User user = adminAuthService.requireUser(authHeader);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Old password is incorrect"));
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    @Transactional
    @PutMapping("/email")
    public ResponseEntity<?> changeEmail(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body
    ) {
        String password = body.get("password");
        String newEmail = normalizeEmail(body.get("newEmail"));
        String confirmEmail = normalizeEmail(body.get("confirmEmail"));

        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入登录密码以验证身份"));
        }
        if (newEmail == null || newEmail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入邮箱地址"));
        }
        if (!EMAIL_PATTERN.matcher(newEmail).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入有效的邮箱地址"));
        }
        if (confirmEmail == null || confirmEmail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请再次输入邮箱地址"));
        }
        if (!newEmail.equals(confirmEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "两次输入的邮箱不一致"));
        }

        User user = adminAuthService.requireUser(authHeader);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "登录密码不正确"));
        }

        String currentEmail = normalizeEmail(user.getEmail());
        if (currentEmail != null && currentEmail.equals(newEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "新邮箱不能与当前邮箱相同"));
        }

        User existing = userRepository.findByEmail(newEmail);
        if (existing != null && !Objects.equals(existing.getUserid(), user.getUserid())) {
            return ResponseEntity.badRequest().body(Map.of("message", "该邮箱已被其他账号使用"));
        }

        try {
            user.setEmail(newEmail);
            userRepository.save(user);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "邮箱已更新");
            result.put("email", newEmail);
            return ResponseEntity.ok(result);
        } catch (DataIntegrityViolationException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.warn("Change email failed: data integrity — userId={}, email={}", user.getUserid(), newEmail);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message",
                    "邮箱更新失败：该地址可能已被占用，请更换后重试"
            ));
        }
    }

    @Transactional
    @PutMapping("/phone")
    public ResponseEntity<?> changePhone(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body
    ) {
        String password = body.get("password");
        String newPhone = normalizePhoneNumber(body.get("newPhone"));
        String confirmPhone = normalizePhoneNumber(body.get("confirmPhone"));

        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入登录密码以验证身份"));
        }
        if (newPhone == null || newPhone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入新手机号"));
        }
        if (!CHINA_MOBILE_PATTERN.matcher(newPhone).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入有效的 11 位中国大陆手机号"));
        }
        if (confirmPhone == null || confirmPhone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请再次输入新手机号"));
        }
        if (!newPhone.equals(confirmPhone)) {
            return ResponseEntity.badRequest().body(Map.of("message", "两次输入的新手机号不一致"));
        }

        User user = adminAuthService.requireUser(authHeader);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "登录密码不正确"));
        }

        String currentPhone = normalizePhoneNumber(user.getPhoneNumber());
        if (currentPhone != null && currentPhone.equals(newPhone)) {
            return ResponseEntity.badRequest().body(Map.of("message", "新手机号不能与当前手机号相同"));
        }

        User existing = userRepository.findByPhoneNumber(newPhone);
        if (existing != null && !Objects.equals(existing.getUserid(), user.getUserid())) {
            return ResponseEntity.badRequest().body(Map.of("message", "该手机号已被其他账号使用"));
        }

        try {
            user.setPhoneNumber(newPhone);
            userRepository.save(user);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "手机号已更新");
            result.put("phone", newPhone);
            return ResponseEntity.ok(result);
        } catch (DataIntegrityViolationException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.warn("Change phone failed: data integrity — userId={}, phone={}", user.getUserid(), newPhone);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message",
                    "手机号更新失败：该号码可能已被占用，请更换后重试"
            ));
        }
    }

    private String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }
        String digits = rawPhone.replaceAll("\\s+", "");
        return digits.isEmpty() ? null : digits;
    }

    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            return null;
        }
        String email = rawEmail.trim().toLowerCase();
        return email.isEmpty() ? null : email;
    }

    private String normalizeUsername(String rawUsername) {
        if (rawUsername == null) {
            return null;
        }
        String username = rawUsername.trim();
        if (username.isEmpty()) {
            return null;
        }
        if (username.length() > USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Username cannot exceed 20 characters");
        }
        return username;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] headerCandidates = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };
        for (String header : headerCandidates) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                if (value.contains(",")) {
                    return value.split(",")[0].trim();
                }
                return trim(value, 64);
            }
        }
        return trim(request.getRemoteAddr(), 64);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private Integer parseRegisterGender(String rawGender) {
        String value = rawGender == null ? "" : rawGender.trim().toLowerCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be empty");
        }
        if (Set.of("male", "man", "m", "1").contains(value)) {
            return 1;
        }
        if (Set.of("female", "woman", "f", "2").contains(value)) {
            return 2;
        }
        throw new IllegalArgumentException("Invalid gender");
    }

    private void publishGrowthEventSafely(
            GrowthEventType eventType,
            Long actorUserId,
            Long targetUserId,
            String bizRefType,
            String bizRefId,
            String dedupeKey,
            SourcePlatform sourcePlatform
    ) {
        try {
            GrowthEventCreateRequest request = new GrowthEventCreateRequest();
            request.setEventType(eventType);
            request.setActorUserId(actorUserId);
            request.setTargetUserId(targetUserId);
            request.setBizRefType(bizRefType);
            request.setBizRefId(bizRefId);
            request.setDedupeKey(dedupeKey);
            request.setRuleVersion("v1");
            request.setSourcePlatform(sourcePlatform);
            request.setOccurredAt(LocalDateTime.now());
            growthEventService.publish(request);
        } catch (Exception ex) {
            log.warn("Skip growth event {} due to publish failure: {}", eventType, ex.getMessage());
        }
    }
}
