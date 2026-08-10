package com.lovecube.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovecube.backend.entity.FellowshipProfile;
import com.lovecube.backend.entity.FellowshipProfileMain;
import com.lovecube.backend.entity.UserPhoto;
import com.lovecube.backend.entity.UserProfile;
import com.lovecube.backend.entity.UserVerification;
import com.lovecube.backend.entity.VerificationRequest;
import com.lovecube.backend.models.User;
import com.lovecube.backend.repository.FellowshipProfileMainRepository;
import com.lovecube.backend.repository.FellowshipProfileRepository;
import com.lovecube.backend.repository.UserPhotoRepository;
import com.lovecube.backend.repository.UserProfileRepository;
import com.lovecube.backend.repository.UserRepository;
import com.lovecube.backend.repository.UserGrowthRepository;
import com.lovecube.backend.repository.UserVerificationRepository;
import com.lovecube.backend.repository.VerificationRequestRepository;
import com.lovecube.backend.utils.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UnifiedProfileService {
    private static final int NICKNAME_MAX_LENGTH = 20;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FellowshipProfileRepository legacyFellowshipProfileRepository;
    private final FellowshipProfileMainRepository fellowshipProfileMainRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationService verificationService;
    private final UserGrowthRepository userGrowthRepository;
    private final FellowshipCardEnrichmentService fellowshipCardEnrichmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UnifiedProfileService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            FellowshipProfileRepository legacyFellowshipProfileRepository,
            FellowshipProfileMainRepository fellowshipProfileMainRepository,
            UserPhotoRepository userPhotoRepository,
            UserVerificationRepository userVerificationRepository,
            VerificationRequestRepository verificationRequestRepository,
            VerificationService verificationService,
            UserGrowthRepository userGrowthRepository,
            FellowshipCardEnrichmentService fellowshipCardEnrichmentService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.legacyFellowshipProfileRepository = legacyFellowshipProfileRepository;
        this.fellowshipProfileMainRepository = fellowshipProfileMainRepository;
        this.userPhotoRepository = userPhotoRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.verificationService = verificationService;
        this.userGrowthRepository = userGrowthRepository;
        this.fellowshipCardEnrichmentService = fellowshipCardEnrichmentService;
    }

    public User requireCurrentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未登录");
        }
        String token = authHeader.substring(7);
        String openid = JwtUtil.getOpenIdFromToken(token);
        if (openid == null || openid.isBlank()) {
            throw new IllegalArgumentException("登录凭证无效");
        }
        User user = userRepository.findByOpenid(openid);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    public Map<String, Object> buildPublicProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Map<String, Object> profile = buildUnifiedProfile(user);
        Map<String, Boolean> verify = Map.of(
                "photoVerified", Boolean.TRUE.equals(profile.get("photoVerified")),
                "realnameVerified", Boolean.TRUE.equals(profile.get("realnameVerified")));
        fellowshipCardEnrichmentService.enrichUserCard(
                profile, user, verify, computeFellowshipCompletionRateForUser(user));
        return profile;
    }

    public Map<String, Object> buildCurrentProfile(User user) {
        return buildUnifiedProfile(user);
    }

    public Map<String, Object> buildLegacyUserPayload(User user) {
        Map<String, Object> merged = buildUnifiedProfile(user);
        Map<String, Object> result = new LinkedHashMap<>();
        Long userId = user.getUserid();

        result.put("userId", userId);
        result.put("id", userId);
        result.put("username", merged.getOrDefault("nickname", ""));
        result.put("nickname", merged.getOrDefault("nickname", ""));
        result.put("phone", user.getPhoneNumber());
        result.put("phoneNumber", user.getPhoneNumber());
        result.put("email", user.getEmail() != null ? user.getEmail() : "");
        result.put("gender", toChineseGender((String) merged.get("gender")));
        result.put("location", merged.getOrDefault("location", ""));
        result.put("occupation", merged.getOrDefault("occupation", ""));
        result.put("height", merged.get("height"));
        result.put("signature", merged.getOrDefault("bio", ""));
        result.put("bio", merged.getOrDefault("bio", ""));
        String avatarUrl = merged.getOrDefault("avatarUrl", "").toString();
        result.put("avatarUrl", avatarUrl);
        // 过渡期别名：与 users.profile_photo 及旧客户端对齐，与 avatarUrl 同值
        result.put("profilePhoto", avatarUrl);
        result.put("photos", merged.getOrDefault("photos", List.of()));
        result.put("role", normalizeRole(user.getRole()));
        result.put("status", "DISABLED".equalsIgnoreCase(user.getUserStatus()) ? "disabled" : "active");
        result.put("fellowshipEnabled", Boolean.TRUE.equals(user.getFellowshipEnabled()));
        result.put("fellowshipMatchVisible", Boolean.TRUE.equals(user.getFellowshipMatchVisible()));
        result.put("verificationStatus", merged.getOrDefault("verificationStatus", "none"));
        result.put("verificationRejectReason", merged.getOrDefault("verificationRejectReason", ""));
        result.put("photoVerified", merged.getOrDefault("photoVerified", false));
        result.put("realnameVerified", merged.getOrDefault("realnameVerified", false));
        result.put("completionRate", merged.getOrDefault("completionRate", 0));
        result.put("age", merged.get("age"));
        result.put("maritalStatus", merged.getOrDefault("maritalStatus", ""));
        result.put("constellation", merged.getOrDefault("constellation", ""));
        result.put("birthday", merged.getOrDefault("birthday", ""));
        result.put("birthDate", merged.get("birthDate"));
        return result;
    }

    /**
     * 消息中心等列表场景：批量解析展示昵称与头像，避免对每条记录调用 {@link #buildLegacyUserPayload} 造成 N+1 查询。
     */
    public Map<Long, Map<String, String>> buildInboxSenderSummaries(Collection<Long> userIds) {
        Map<Long, Map<String, String>> out = new LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return out;
        }
        Set<Long> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return out;
        }

        Map<Long, User> usersById = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getUserid, u -> u, (a, b) -> a));

        Map<Long, UserProfile> profileByUid = userProfileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (a, b) -> a));

        Map<Long, FellowshipProfileMain> mainByUid = fellowshipProfileMainRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(FellowshipProfileMain::getUserId, m -> m, (a, b) -> a));

        Map<Long, FellowshipProfile> legacyByUid = legacyFellowshipProfileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(FellowshipProfile::getUserId, p -> p, (a, b) -> a));

        for (Long uid : ids) {
            User user = usersById.get(uid);
            if (user == null) {
                continue;
            }
            FellowshipProfile legacy = legacyByUid.get(uid);
            FellowshipProfileMain main = mainByUid.get(uid);
            UserProfile userProfile = profileByUid.get(uid);
            String nickname = firstNonBlank(
                    legacy == null ? null : legacy.getNickname(),
                    main == null ? null : main.getNickname(),
                    userProfile == null ? null : userProfile.getNickname(),
                    user.getUsername()
            );
            String avatar = firstNonBlank(
                    legacy == null ? null : legacy.getAvatarUrl(),
                    main == null ? null : main.getAvatar(),
                    userProfile == null ? null : userProfile.getAvatar(),
                    user.getProfilePhoto()
            );
            Map<String, String> row = new LinkedHashMap<>();
            row.put("nickname", defaultText(nickname, ""));
            row.put("avatarUrl", defaultText(avatar, ""));
            out.put(uid, row);
        }
        return out;
    }

    @Transactional
    public Map<String, Object> updateLegacyProfile(User user, Map<String, Object> payload) {
        if (payload.containsKey("nickname") || payload.containsKey("username")) {
            String nickname = firstText(payload.get("nickname"), payload.get("username"));
            if (nickname != null) {
                user.setUsername(normalizeNickname(nickname));
            }
        }
        if (payload.containsKey("avatarUrl") || payload.containsKey("avatar") || payload.containsKey("profilePhoto")) {
            String avatar = firstText(payload.get("avatarUrl"), payload.get("avatar"), payload.get("profilePhoto"));
            if (avatar != null) {
                user.setProfilePhoto(avatar);
            }
        }
        if (payload.containsKey("gender")) {
            user.setGender(toGenderCode(payload.get("gender")));
        }
        if (payload.containsKey("birthday")) {
            LocalDate birthday = parseDate(payload.get("birthday"));
            user.setBirthDate(birthday == null ? null : birthday.atStartOfDay());
            user.setAge(birthday == null ? user.getAge() : calcAge(birthday));
        }
        if (payload.containsKey("location")) {
            user.setLocation(toTrimmed(payload.get("location")));
        }
        if (payload.containsKey("occupation")) {
            user.setOccupation(toTrimmed(payload.get("occupation")));
        }
        if (payload.containsKey("height")) {
            user.setHeight(toInteger(payload.get("height")));
        }
        if (payload.containsKey("signature") || payload.containsKey("bio")) {
            user.setBio(firstText(payload.get("signature"), payload.get("bio")));
        }
        userRepository.save(user);

        if (payload.containsKey("photos") && payload.get("photos") instanceof List<?> list) {
            replaceUserPhotos(user.getUserid(), list.stream().map(String::valueOf).collect(Collectors.toList()));
        }
        syncMainProfileFromUser(user);
        syncUserProfileFromUser(user);
        syncLegacyFellowshipProfileFromUser(user);

        return buildLegacyUserPayload(user);
    }

    @Transactional
    public Map<String, Object> updateFellowshipProfile(User user, Map<String, Object> payload) {
        FellowshipProfileMain main = fellowshipProfileMainRepository.findByUserId(user.getUserid())
                .orElseGet(() -> {
                    FellowshipProfileMain created = new FellowshipProfileMain();
                    created.setUserId(user.getUserid());
                    created.setReportedCount(0);
                    return created;
                });
        FellowshipProfile legacy = legacyFellowshipProfileRepository.findByUserId(user.getUserid())
                .orElseGet(() -> {
                    FellowshipProfile created = new FellowshipProfile();
                    created.setUserId(user.getUserid());
                    created.setGuardianContactVisible(true);
                    return created;
                });

        String nickname = getText(payload, "nickname", main.getNickname(), user.getUsername());
        nickname = normalizeNickname(nickname);
        String gender = normalizeGender(
                getText(payload, "gender", fromGenderCode(user.getGender()), main.getGender()));
        Integer birthYear = getInteger(payload, "birthYear", legacy.getBirthYear(), user.getBirthDate() == null ? null : user.getBirthDate().getYear());
        Integer age = getInteger(payload, "age", legacy.getAge(), user.getAge());
        if (age == null && birthYear != null) {
            age = calcAge(birthYear);
        }
        String city = getText(payload, "city", main.getCity(), user.getLocation());
        String occupation = getText(payload, "occupation", main.getOccupation(), user.getOccupation());
        String education = getText(payload, "education", legacy.getEducation(), null);
        Integer height = getInteger(payload, "height", main.getHeight(), user.getHeight());
        String bio = getText(payload, "bio", main.getBio(), user.getBio());
        String intention = getText(payload, "intention", legacy.getIntention(), null);
        String maritalStatus = normalizeMaritalStatus(getText(payload, "maritalStatus", legacy.getMaritalStatus(), null));
        String avatarUrl = getText(payload, "avatarUrl", main.getAvatar(), user.getProfilePhoto());
        String tags = normalizeTags(payload.get("tags"), legacy.getTags());

        main.setNickname(nickname);
        main.setGender(gender);
        main.setBirthday(birthYear == null ? null : LocalDate.of(birthYear, 1, 1));
        main.setCity(city);
        main.setOccupation(occupation);
        main.setHeight(height);
        main.setBio(bio);
        main.setAvatar(avatarUrl);
        main.setCompletionRate(calculateCompletionRateForFellowship(nickname, gender, birthYear, city, occupation, education, height, bio, intention, avatarUrl));
        main.setReviewStatus(defaultText(main.getReviewStatus(), "pending"));
        main.setVerificationStatus(defaultText(main.getVerificationStatus(), "none"));
        main.setLastActiveAt(LocalDateTime.now());
        fellowshipProfileMainRepository.save(main);

        legacy.setNickname(nickname);
        legacy.setGender(gender);
        legacy.setBirthYear(birthYear);
        legacy.setAge(age);
        legacy.setCity(city);
        legacy.setOccupation(occupation);
        legacy.setEducation(education);
        legacy.setHeight(height);
        legacy.setBio(bio);
        legacy.setIntention(intention);
        legacy.setMaritalStatus(maritalStatus);
        legacy.setAvatarUrl(avatarUrl);
        legacy.setTags(tags);
        legacy.setIdentityRole(getText(payload, "identityRole", legacy.getIdentityRole(), "self"));
        legacy.setGuardianRole(getText(payload, "guardianRole", legacy.getGuardianRole(), null));
        legacy.setChildGender(getText(payload, "childGender", legacy.getChildGender(), null));
        legacy.setChildAge(getInteger(payload, "childAge", legacy.getChildAge(), null));
        legacy.setChildHeight(getInteger(payload, "childHeight", legacy.getChildHeight(), null));
        legacy.setChildEducation(getText(payload, "childEducation", legacy.getChildEducation(), null));
        legacy.setChildJob(getText(payload, "childJob", legacy.getChildJob(), null));
        legacy.setChildCity(getText(payload, "childCity", legacy.getChildCity(), null));
        legacy.setChildHouseCarStatus(getText(payload, "childHouseCarStatus", legacy.getChildHouseCarStatus(), null));
        legacy.setChildMarriageIntention(getText(payload, "childMarriageIntention", legacy.getChildMarriageIntention(), null));
        legacy.setChildPartnerRequirements(getText(payload, "childPartnerRequirements", legacy.getChildPartnerRequirements(), null));
        if (payload.containsKey("guardianContactVisible")) {
            legacy.setGuardianContactVisible(payload.get("guardianContactVisible") == null
                    ? null
                    : Boolean.parseBoolean(String.valueOf(payload.get("guardianContactVisible"))));
        }
        legacy.setProfileStatus(main.getCompletionRate() >= 100 ? "COMPLETE" : "INCOMPLETE");
        legacy.setReviewStatus(defaultText(legacy.getReviewStatus(), "PENDING"));
        legacyFellowshipProfileRepository.save(legacy);

        user.setUsername(nickname);
        user.setProfilePhoto(avatarUrl);
        user.setLocation(city);
        user.setOccupation(occupation);
        user.setBio(bio);
        user.setHeight(height);
        user.setAge(age);
        user.setGender(toGenderCode(gender));
        user.setBirthDate(birthYear == null ? null : LocalDate.of(birthYear, 1, 1).atStartOfDay());
        userRepository.save(user);
        syncUserProfileFromUser(user);

        return buildFellowshipPayload(user);
    }

    public Map<String, Object> buildFellowshipPayload(User user) {
        Map<String, Object> merged = buildUnifiedProfile(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getUserid());
        result.put("nickname", merged.getOrDefault("nickname", ""));
        result.put("gender", merged.getOrDefault("gender", ""));
        result.put("birthYear", merged.get("birthYear"));
        result.put("age", merged.get("age"));
        result.put("city", merged.getOrDefault("location", ""));
        result.put("occupation", merged.getOrDefault("occupation", ""));
        result.put("education", merged.getOrDefault("education", ""));
        result.put("height", merged.get("height"));
        result.put("bio", merged.getOrDefault("bio", ""));
        result.put("intention", merged.getOrDefault("intention", ""));
        result.put("maritalStatus", merged.getOrDefault("maritalStatus", ""));
        result.put("avatarUrl", merged.getOrDefault("avatarUrl", ""));
        result.put("tags", merged.getOrDefault("tags", ""));
        result.put("identityRole", merged.getOrDefault("identityRole", "self"));
        result.put("guardianRole", merged.getOrDefault("guardianRole", ""));
        result.put("childGender", merged.getOrDefault("childGender", ""));
        result.put("childAge", merged.get("childAge"));
        result.put("childHeight", merged.get("childHeight"));
        result.put("childEducation", merged.getOrDefault("childEducation", ""));
        result.put("childJob", merged.getOrDefault("childJob", ""));
        result.put("childCity", merged.getOrDefault("childCity", ""));
        result.put("childHouseCarStatus", merged.getOrDefault("childHouseCarStatus", ""));
        result.put("childMarriageIntention", merged.getOrDefault("childMarriageIntention", ""));
        result.put("childPartnerRequirements", merged.getOrDefault("childPartnerRequirements", ""));
        result.put("guardianContactVisible", merged.getOrDefault("guardianContactVisible", true));
        result.put("profileStatus", merged.getOrDefault("profileStatus", "INCOMPLETE"));
        result.put("reviewStatus", merged.getOrDefault("reviewStatus", "pending"));
        result.put("completionRate", merged.getOrDefault("completionRate", 0));
        result.put("fellowshipEnabled", Boolean.TRUE.equals(user.getFellowshipEnabled()));
        result.put("fellowshipMatchVisible", Boolean.TRUE.equals(user.getFellowshipMatchVisible()));
        result.put("verificationStatus", merged.getOrDefault("verificationStatus", "none"));
        result.put("verificationRejectReason", merged.getOrDefault("verificationRejectReason", ""));
        result.put("photoVerified", merged.getOrDefault("photoVerified", false));
        result.put("realnameVerified", merged.getOrDefault("realnameVerified", false));
        result.put("photoCount", getUserPhotos(user.getUserid()).size());
        return result;
    }

    public Map<String, Object> buildFellowshipCompletion(User user) {
        Map<String, Object> profile = buildFellowshipPayload(user);
        int growthLevel = resolveGrowthLevel(user.getUserid());
        return FellowshipProfileCompletion.build(user, profile, (Integer) profile.getOrDefault("photoCount", 0), growthLevel);
    }

    public int computeFellowshipCompletionRateForUser(User user) {
        if (user == null) {
            return 0;
        }
        Map<String, Object> profile = buildFellowshipPayload(user);
        int photoCount = profile.get("photoCount") instanceof Number n ? n.intValue() : getUserPhotos(user.getUserid()).size();
        Map<String, Object> completion = FellowshipProfileCompletion.build(user, profile, photoCount, resolveGrowthLevel(user.getUserid()));
        Object rate = completion.get("completionRate");
        return rate instanceof Number n ? n.intValue() : 0;
    }

    public int computeExposureBoostForUser(User user) {
        if (user == null) {
            return 0;
        }
        Map<String, Object> profile = buildFellowshipPayload(user);
        int photoCount = profile.get("photoCount") instanceof Number n ? n.intValue() : getUserPhotos(user.getUserid()).size();
        Map<String, Object> completion = FellowshipProfileCompletion.build(user, profile, photoCount, resolveGrowthLevel(user.getUserid()));
        Object boost = completion.get("exposureBoostPercent");
        return boost instanceof Number n ? n.intValue() : 0;
    }

    public double computeRecommendRankForUser(User user, Map<String, Boolean> verifyBadges) {
        if (user == null) {
            return 0;
        }
        int completionRate = computeFellowshipCompletionRateForUser(user);
        boolean verified = verifyBadges != null && (
                Boolean.TRUE.equals(verifyBadges.get("photoVerified"))
                        || Boolean.TRUE.equals(verifyBadges.get("realnameVerified")));
        return FellowshipProfileCompletion.computeRecommendRank(
                completionRate,
                verified,
                resolveGrowthLevel(user.getUserid()));
    }

    /**
     * 首页/列表场景：仅基于 User 实体 + 批量预载的认证/照片/等级，零额外 profile 查询。
     */
    public int computeFellowshipCompletionRateLightweight(
            User user,
            Map<String, Boolean> verifyBadges,
            int photoCount,
            int growthLevel
    ) {
        if (user == null) {
            return 0;
        }
        Map<String, Object> profile = buildCompletionProfileFromUser(user, verifyBadges);
        Map<String, Object> completion = FellowshipProfileCompletion.build(user, profile, photoCount, growthLevel);
        Object rate = completion.get("completionRate");
        return rate instanceof Number n ? n.intValue() : 0;
    }

    public double computeRecommendRankLightweight(
            User user,
            Map<String, Boolean> verifyBadges,
            int photoCount,
            int growthLevel
    ) {
        if (user == null) {
            return 0;
        }
        int completionRate = computeFellowshipCompletionRateLightweight(user, verifyBadges, photoCount, growthLevel);
        boolean verified = verifyBadges != null && (
                Boolean.TRUE.equals(verifyBadges.get("photoVerified"))
                        || Boolean.TRUE.equals(verifyBadges.get("realnameVerified")));
        return FellowshipProfileCompletion.computeRecommendRank(completionRate, verified, growthLevel);
    }

    public int computeExposureBoostLightweight(
            User user,
            Map<String, Boolean> verifyBadges,
            int photoCount,
            int growthLevel
    ) {
        if (user == null) {
            return 0;
        }
        Map<String, Object> profile = buildCompletionProfileFromUser(user, verifyBadges);
        Map<String, Object> completion = FellowshipProfileCompletion.build(user, profile, photoCount, growthLevel);
        Object boost = completion.get("exposureBoostPercent");
        return boost instanceof Number n ? n.intValue() : 0;
    }

    private Map<String, Object> buildCompletionProfileFromUser(User user, Map<String, Boolean> verifyBadges) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("avatarUrl", defaultText(user.getProfilePhoto(), ""));
        profile.put("city", defaultText(user.getLocation(), ""));
        profile.put("bio", defaultText(user.getBio(), ""));
        if (user.getBirthDate() != null) {
            profile.put("birthYear", user.getBirthDate().getYear());
        } else {
            Integer age = user.getAge();
            if (age != null && age > 0) {
                profile.put("age", age);
            }
        }
        profile.put("photoVerified", verifyBadges != null && Boolean.TRUE.equals(verifyBadges.get("photoVerified")));
        profile.put("realnameVerified", verifyBadges != null && Boolean.TRUE.equals(verifyBadges.get("realnameVerified")));
        return profile;
    }

    private int resolveGrowthLevel(Long userId) {
        if (userId == null) {
            return 1;
        }
        return userGrowthRepository.findByUserId(userId)
                .map(g -> g.getLevel() == null ? 1 : g.getLevel())
                .orElse(1);
    }

    /**
     * 匹配列表卡片精简构建器：仅读取 User 实体（已由分页查询加载），零额外 SQL。
     * 适用于 /matches/list 等列表场景，比 buildLegacyUserPayload 少 7 条查询/用户。
     */
    public Map<String, Object> buildMatchCardPayload(User user, Map<String, Boolean> verifyBadges) {
        return buildMatchCardPayload(user, verifyBadges, null);
    }

    /**
     * 匹配列表卡片：可选传入当前浏览者以生成推荐理由。
     */
    public Map<String, Object> buildMatchCardPayload(User user, Map<String, Boolean> verifyBadges, User viewer) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("userId",       user.getUserid());
        card.put("userid",       user.getUserid());
        card.put("id",           user.getUserid());
        card.put("nickname",     defaultText(user.getUsername(), ""));
        card.put("username",     defaultText(user.getUsername(), ""));
        String photo = defaultText(user.getProfilePhoto(), "");
        card.put("avatarUrl",    photo);
        card.put("profilePhoto", photo);
        card.put("age",          user.getAge());
        card.put("location",     defaultText(user.getLocation(), ""));
        card.put("occupation",   defaultText(user.getOccupation(), ""));
        card.put("height",       user.getHeight());
        card.put("bio",          defaultText(user.getBio(), ""));
        card.put("signature",    defaultText(user.getBio(), ""));
        card.put("gender",       fromGenderCode(user.getGender()));
        String bday = user.getBirthDate() == null ? "" : user.getBirthDate().toLocalDate().toString();
        card.put("birthday",     bday);
        card.put("birthDate",    user.getBirthDate());
        card.put("constellation", user.getBirthDate() == null ? "" : getConstellation(user.getBirthDate().toLocalDate()));
        if (!photo.isBlank()) {
            card.put("photos", List.of(photo));
        } else {
            card.put("photos", List.of());
        }
        card.put("completionRate", computeFellowshipCompletionRateForUser(user));
        card.put("exposureBoostPercent", computeExposureBoostForUser(user));
        card.put("identityRole", "self");
        card.put("guardianRole", "");
        card.put("photoVerified",    verifyBadges != null && Boolean.TRUE.equals(verifyBadges.get("photoVerified")));
        card.put("realnameVerified", verifyBadges != null && Boolean.TRUE.equals(verifyBadges.get("realnameVerified")));
        card.put("recommendReasons", viewer != null ? buildRecommendReasons(viewer, user, verifyBadges) : List.of());
        fellowshipCardEnrichmentService.enrichUserCard(
                card, user, verifyBadges, computeFellowshipCompletionRateForUser(user));
        return card;
    }

    /** 基于 User 字段估算资料完整度（0–100），用于非联谊场景兜底。 */
    public int estimateProfileCompletionForUser(User user) {
        return computeFellowshipCompletionRateForUser(user);
    }

    private int estimateProfileCompletion(User user) {
        return computeFellowshipCompletionRateForUser(user);
    }

    /**
     * 生成匹配推荐理由（仅使用 User + 认证信息，无额外 SQL）。
     */
    public List<String> buildRecommendReasons(User viewer, User candidate, Map<String, Boolean> verifyBadges) {
        List<String> reasons = new ArrayList<>();
        if (viewer == null || candidate == null) {
            return reasons;
        }
        String vLoc = defaultText(viewer.getLocation(), "").trim();
        String cLoc = defaultText(candidate.getLocation(), "").trim();
        if (!vLoc.isEmpty() && vLoc.equals(cLoc)) {
            reasons.add("同城");
        } else if (vLoc.length() >= 2 && cLoc.length() >= 2
                && vLoc.substring(0, 2).equals(cLoc.substring(0, 2))) {
            reasons.add("同省");
        }
        String vOcc = defaultText(viewer.getOccupation(), "").trim();
        String cOcc = defaultText(candidate.getOccupation(), "").trim();
        if (!vOcc.isEmpty() && !cOcc.isEmpty() && vOcc.equals(cOcc)) {
            reasons.add("职业相同");
        }
        Integer viewerAge = viewer.getAge();
        Integer candidateAge = candidate.getAge();
        if (viewerAge != null && candidateAge != null && viewerAge > 0 && candidateAge > 0) {
            int ageDiff = Math.abs(viewerAge - candidateAge);
            if (ageDiff <= 3) {
                reasons.add("年龄相近");
            }
        }
        if (verifyBadges != null && Boolean.TRUE.equals(verifyBadges.get("photoVerified"))) {
            reasons.add("真人认证");
        }
        if (verifyBadges != null && Boolean.TRUE.equals(verifyBadges.get("realnameVerified"))) {
            reasons.add("实名认证");
        }
        int completion = computeFellowshipCompletionRateForUser(candidate);
        if (completion >= 80) {
            reasons.add("资料完善");
        }
        if (reasons.isEmpty()) {
            reasons.add("今日推荐");
        }
        return reasons.stream().limit(3).collect(Collectors.toList());
    }

    public List<String> buildRecommendReasonsLightweight(
            User viewer,
            User candidate,
            Map<String, Boolean> verifyBadges,
            int completionRate
    ) {
        List<String> reasons = new ArrayList<>();
        if (viewer == null || candidate == null) {
            return reasons;
        }
        String vLoc = defaultText(viewer.getLocation(), "").trim();
        String cLoc = defaultText(candidate.getLocation(), "").trim();
        if (!vLoc.isEmpty() && vLoc.equals(cLoc)) {
            reasons.add("同城");
        } else if (vLoc.length() >= 2 && cLoc.length() >= 2
                && vLoc.substring(0, 2).equals(cLoc.substring(0, 2))) {
            reasons.add("同省");
        }
        String vOcc = defaultText(viewer.getOccupation(), "").trim();
        String cOcc = defaultText(candidate.getOccupation(), "").trim();
        if (!vOcc.isEmpty() && !cOcc.isEmpty() && vOcc.equals(cOcc)) {
            reasons.add("职业相同");
        }
        Integer viewerAge = viewer.getAge();
        Integer candidateAge = candidate.getAge();
        if (viewerAge != null && candidateAge != null && viewerAge > 0 && candidateAge > 0) {
            int ageDiff = Math.abs(viewerAge - candidateAge);
            if (ageDiff <= 3) {
                reasons.add("年龄相近");
            }
        }
        if (verifyBadges != null && Boolean.TRUE.equals(verifyBadges.get("photoVerified"))) {
            reasons.add("真人认证");
        }
        if (verifyBadges != null && Boolean.TRUE.equals(verifyBadges.get("realnameVerified"))) {
            reasons.add("实名认证");
        }
        if (completionRate >= 80) {
            reasons.add("资料完善");
        }
        if (reasons.isEmpty()) {
            reasons.add("今日推荐");
        }
        return reasons.stream().limit(3).collect(Collectors.toList());
    }

    /** 联谊资料完成度：权重清单、权益与曝光加成（迭代 1）。 */
    public Map<String, Object> extractCompletion(Map<String, Object> p) {
        if (p == null || p.isEmpty()) {
            return FellowshipProfileCompletion.build(null, Map.of(), 0, 1);
        }
        User user = null;
        Object userIdObj = p.get("userId");
        if (userIdObj instanceof Number n) {
            user = userRepository.findById(n.longValue()).orElse(null);
        }
        int photoCount = p.get("photoCount") instanceof Number n
                ? n.intValue()
                : (user != null ? getUserPhotos(user.getUserid()).size() : 0);
        int growthLevel = user != null ? resolveGrowthLevel(user.getUserid()) : 1;
        return FellowshipProfileCompletion.build(user, p, photoCount, growthLevel);
    }

    public List<String> getUserPhotos(Long userId) {
        return userPhotoRepository.findByUserIdOrderBySortOrderAscIdAsc(userId).stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .map(UserPhoto::getPhotoUrl)
                .filter(Objects::nonNull)
                .toList();
    }

    /** 批量取用户生活照封面：优先主图，否则按排序取第一张 ACTIVE 照片 */
    public Map<Long, String> getPrimaryPhotoUrlByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<UserPhoto>> grouped = userPhotoRepository
                .findByUserIdInAndStatusOrderBySortOrderAscIdAsc(ids, "ACTIVE")
                .stream()
                .filter(p -> p.getPhotoUrl() != null && !p.getPhotoUrl().isBlank())
                .collect(Collectors.groupingBy(UserPhoto::getUserId));
        Map<Long, String> result = new LinkedHashMap<>();
        for (Long userId : ids) {
            List<UserPhoto> photos = grouped.get(userId);
            if (photos == null || photos.isEmpty()) {
                continue;
            }
            String url = photos.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getPrimary()))
                    .map(UserPhoto::getPhotoUrl)
                    .findFirst()
                    .orElse(photos.get(0).getPhotoUrl());
            result.put(userId, url);
        }
        return result;
    }

    /** 联谊开通门槛：至少一张已落库 user_photos 的生活照（与资料页「生活照」一致） */
    public boolean hasFellowshipLifePhotos(Long userId) {
        if (userId == null) {
            return false;
        }
        return !getUserPhotos(userId).isEmpty();
    }

    /** 已开通联谊但无任何生活照：需引导上传，否则限制推荐/喜欢等能力 */
    public boolean isFellowshipActiveButMissingLifePhotos(User user) {
        if (user == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(user.getFellowshipEnabled())) {
            return false;
        }
        return !hasFellowshipLifePhotos(user.getUserid());
    }

    public Map<String, Object> buildFellowshipPhotosRequiredErrorBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "你已开通联谊，但尚未上传生活照。请尽快上传至少一张生活照，否则将无法继续使用推荐、喜欢等联谊功能");
        body.put("code", "FELLOWSHIP_REQUIRES_PHOTOS");
        return body;
    }

    @Transactional
    public void replaceUserPhotos(Long userId, List<String> photoUrls) {
        List<String> cleaned = (photoUrls == null ? List.<String>of() : photoUrls).stream()
                .map(this::toTrimmed)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        userPhotoRepository.deleteByUserId(userId);
        for (int i = 0; i < cleaned.size(); i++) {
            UserPhoto row = new UserPhoto();
            row.setUserId(userId);
            row.setPhotoUrl(cleaned.get(i));
            row.setSortOrder(i);
            row.setPrimary(i == 0);
            row.setStatus("ACTIVE");
            userPhotoRepository.save(row);
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setPhotos(writePhotosJson(cleaned));
            userRepository.save(user);
        }

        FellowshipProfileMain main = fellowshipProfileMainRepository.findByUserId(userId).orElse(null);
        if (main != null) {
            main.setPhotosJson(writePhotosJson(cleaned));
            fellowshipProfileMainRepository.save(main);
        }
    }

    private Map<String, Object> buildUnifiedProfile(User user) {
        UserProfile userProfile = userProfileRepository.findByUserId(user.getUserid()).orElse(null);
        FellowshipProfileMain main = fellowshipProfileMainRepository.findByUserId(user.getUserid()).orElse(null);
        FellowshipProfile legacy = legacyFellowshipProfileRepository.findByUserId(user.getUserid()).orElse(null);
        Optional<UserVerification> uv = userVerificationRepository.findTopByUserIdOrderBySubmittedAtDesc(user.getUserid());
        Optional<VerificationRequest> lv = verificationRequestRepository
                .findByUserIdOrderBySubmittedAtDesc(user.getUserid()).stream().findFirst();
        List<String> photos = getUserPhotos(user.getUserid());

        String nickname = firstNonBlank(
                legacy == null ? null : legacy.getNickname(),
                main == null ? null : main.getNickname(),
                userProfile == null ? null : userProfile.getNickname(),
                user.getUsername()
        );
        String avatar = firstNonBlank(
                legacy == null ? null : legacy.getAvatarUrl(),
                main == null ? null : main.getAvatar(),
                userProfile == null ? null : userProfile.getAvatar(),
                user.getProfilePhoto()
        );
        String gender = normalizeGender(firstNonBlank(
                fromGenderCode(user.getGender()),
                main == null ? null : main.getGender(),
                legacy == null ? null : legacy.getGender(),
                userProfile == null ? null : userProfile.getGender()
        ));
        Integer birthYear = firstNonNull(
                legacy == null ? null : legacy.getBirthYear(),
                main != null && main.getBirthday() != null ? main.getBirthday().getYear() : null,
                user.getBirthDate() == null ? null : user.getBirthDate().getYear()
        );
        Integer age = firstNonNull(
                legacy == null ? null : legacy.getAge(),
                user.getAge(),
                birthYear == null ? null : calcAge(birthYear)
        );
        String city = firstNonBlank(
                legacy == null ? null : legacy.getCity(),
                main == null ? null : main.getCity(),
                userProfile == null ? null : userProfile.getCity(),
                user.getLocation()
        );
        String occupation = firstNonBlank(
                legacy == null ? null : legacy.getOccupation(),
                main == null ? null : main.getOccupation(),
                user.getOccupation()
        );
        Integer height = firstNonNull(
                legacy == null ? null : legacy.getHeight(),
                main == null ? null : main.getHeight(),
                user.getHeight()
        );
        String bio = firstNonBlank(
                legacy == null ? null : legacy.getBio(),
                main == null ? null : main.getBio(),
                user.getBio()
        );
        String education = firstNonBlank(legacy == null ? null : legacy.getEducation(), userProfile == null ? null : userProfile.getEducation());
        String intention = firstNonBlank(legacy == null ? null : legacy.getIntention());
        String maritalStatus = normalizeMaritalStatus(firstNonBlank(legacy == null ? null : legacy.getMaritalStatus()));
        String tags = firstNonBlank(legacy == null ? null : legacy.getTags());

        String verificationStatus = uv.map(UserVerification::getStatus).orElseGet(() ->
                lv.map(VerificationRequest::getStatus).orElse("none"));
        String verificationRejectReason = uv.map(UserVerification::getRejectReason).orElseGet(() ->
                lv.map(VerificationRequest::getRejectReason).orElse(""));
        Map<String, Object> verifyBadges = verificationService.getStatusSummary(user.getUserid());
        boolean photoVerified   = Boolean.TRUE.equals(verifyBadges.get("photoVerified"));
        boolean realnameVerified = Boolean.TRUE.equals(verifyBadges.get("realnameVerified"));
        String reviewStatus = firstNonBlank(
                legacy == null ? null : legacy.getReviewStatus(),
                main == null ? null : main.getReviewStatus(),
                "pending"
        );
        String profileStatus = firstNonBlank(
                legacy == null ? null : legacy.getProfileStatus(),
                main != null && main.getCompletionRate() != null && main.getCompletionRate() >= 100 ? "COMPLETE" : "INCOMPLETE"
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", user.getUserid());
        out.put("nickname", defaultText(nickname, ""));
        out.put("avatarUrl", defaultText(avatar, ""));
        out.put("gender", defaultText(gender, ""));
        out.put("birthYear", birthYear);
        out.put("birthday", birthYear == null ? "" : birthYear + "-01-01");
        out.put("birthDate", user.getBirthDate());
        out.put("age", age);
        out.put("constellation", user.getBirthDate() == null ? "" : getConstellation(user.getBirthDate().toLocalDate()));
        out.put("location", defaultText(city, ""));
        out.put("occupation", defaultText(occupation, ""));
        out.put("height", height);
        out.put("bio", defaultText(bio, ""));
        out.put("education", defaultText(education, ""));
        out.put("intention", defaultText(intention, ""));
        out.put("maritalStatus", defaultText(maritalStatus, ""));
        out.put("tags", defaultText(tags, ""));
        out.put("photos", photos);
        out.put("completionRate", firstNonNull(main == null ? null : main.getCompletionRate(), 0));
        out.put("verificationStatus", defaultText(verificationStatus, "none"));
        out.put("verificationRejectReason", defaultText(verificationRejectReason, ""));
        out.put("photoVerified", photoVerified);
        out.put("realnameVerified", realnameVerified);
        out.put("reviewStatus", defaultText(reviewStatus, "pending"));
        out.put("profileStatus", defaultText(profileStatus, "INCOMPLETE"));

        out.put("identityRole", legacy == null ? "self" : defaultText(legacy.getIdentityRole(), "self"));
        out.put("guardianRole", legacy == null ? "" : defaultText(legacy.getGuardianRole(), ""));
        out.put("childGender", legacy == null ? "" : defaultText(legacy.getChildGender(), ""));
        out.put("childAge", legacy == null ? null : legacy.getChildAge());
        out.put("childHeight", legacy == null ? null : legacy.getChildHeight());
        out.put("childEducation", legacy == null ? "" : defaultText(legacy.getChildEducation(), ""));
        out.put("childJob", legacy == null ? "" : defaultText(legacy.getChildJob(), ""));
        out.put("childCity", legacy == null ? "" : defaultText(legacy.getChildCity(), ""));
        out.put("childHouseCarStatus", legacy == null ? "" : defaultText(legacy.getChildHouseCarStatus(), ""));
        out.put("childMarriageIntention", legacy == null ? "" : defaultText(legacy.getChildMarriageIntention(), ""));
        out.put("childPartnerRequirements", legacy == null ? "" : defaultText(legacy.getChildPartnerRequirements(), ""));
        out.put("guardianContactVisible", legacy == null ? true : legacy.getGuardianContactVisible() == null || legacy.getGuardianContactVisible());
        return out;
    }

    private void syncMainProfileFromUser(User user) {
        FellowshipProfileMain main = fellowshipProfileMainRepository.findByUserId(user.getUserid()).orElseGet(() -> {
            FellowshipProfileMain created = new FellowshipProfileMain();
            created.setUserId(user.getUserid());
            return created;
        });
        main.setNickname(user.getUsername());
        main.setAvatar(user.getProfilePhoto());
        main.setGender(fromGenderCode(user.getGender()));
        main.setBirthday(user.getBirthDate() == null ? null : user.getBirthDate().toLocalDate());
        main.setCity(user.getLocation());
        main.setOccupation(user.getOccupation());
        main.setHeight(user.getHeight());
        main.setBio(user.getBio());
        main.setPhotosJson(user.getPhotos());
        main.setLastActiveAt(LocalDateTime.now());
        fellowshipProfileMainRepository.save(main);
    }

    private void syncUserProfileFromUser(User user) {
        UserProfile p = userProfileRepository.findByUserId(user.getUserid()).orElseGet(() -> {
            UserProfile created = new UserProfile();
            created.setUserId(user.getUserid());
            return created;
        });
        if (user.getUsername() != null) p.setNickname(user.getUsername());
        if (user.getProfilePhoto() != null) p.setAvatar(user.getProfilePhoto());
        if (user.getAge() != null) p.setAge(user.getAge());
        if (user.getGender() != null) p.setGender(fromGenderCode(user.getGender()));
        p.setCity(user.getLocation());
        p.setEducation(p.getEducation());
        p.setLastActiveTime(LocalDateTime.now());
        userProfileRepository.save(p);
    }

    private void syncLegacyFellowshipProfileFromUser(User user) {
        FellowshipProfile p = legacyFellowshipProfileRepository.findByUserId(user.getUserid()).orElseGet(() -> {
            FellowshipProfile created = new FellowshipProfile();
            created.setUserId(user.getUserid());
            return created;
        });
        p.setNickname(user.getUsername());
        p.setAvatarUrl(user.getProfilePhoto());
        p.setGender(fromGenderCode(user.getGender()));
        p.setAge(user.getAge());
        p.setBirthYear(user.getBirthDate() == null ? null : user.getBirthDate().getYear());
        p.setCity(user.getLocation());
        p.setOccupation(user.getOccupation());
        p.setHeight(user.getHeight());
        p.setBio(user.getBio());
        p.setProfileStatus(defaultText(p.getProfileStatus(), "INCOMPLETE"));
        p.setReviewStatus(defaultText(p.getReviewStatus(), "PENDING"));
        legacyFellowshipProfileRepository.save(p);
    }

    private String toChineseGender(String normalized) {
        if ("male".equalsIgnoreCase(normalized)) return "男";
        if ("female".equalsIgnoreCase(normalized)) return "女";
        return "";
    }

    private String normalizeRole(String role) {
        String r = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        if ("admin".equals(r) || "super_admin".equals(r) || "root".equals(r)) return "admin";
        return "user";
    }

    private Integer toGenderCode(Object raw) {
        if (raw == null) return null;
        String v = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        if ("1".equals(v) || "male".equals(v) || "男".equals(v)) return 1;
        if ("2".equals(v) || "female".equals(v) || "女".equals(v)) return 2;
        return null;
    }

    private String fromGenderCode(Integer code) {
        if (code == null) return "";
        return code == 1 ? "male" : "female";
    }

    private String normalizeGender(String value) {
        if (value == null || value.isBlank()) return "";
        return "female".equalsIgnoreCase(value) || "女".equals(value) || "2".equals(value) ? "female" : "male";
    }

    private String normalizeMaritalStatus(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim();
        return switch (normalized) {
            case "单身", "已婚", "离异" -> normalized;
            default -> "";
        };
    }

    private LocalDate parseDate(Object raw) {
        String text = toTrimmed(raw);
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer calcAge(LocalDate birthday) {
        return Period.between(birthday, LocalDate.now()).getYears();
    }

    private Integer calcAge(int birthYear) {
        return LocalDate.now().getYear() - birthYear;
    }

    private String getConstellation(LocalDate birthDate) {
        if (birthDate == null) return "";
        int month = birthDate.getMonthValue();
        int day = birthDate.getDayOfMonth();
        if ((month == 1 && day >= 20) || (month == 2 && day <= 18)) return "水瓶座";
        if ((month == 2 && day >= 19) || (month == 3 && day <= 20)) return "双鱼座";
        if ((month == 3 && day >= 21) || (month == 4 && day <= 19)) return "白羊座";
        if ((month == 4 && day >= 20) || (month == 5 && day <= 20)) return "金牛座";
        if ((month == 5 && day >= 21) || (month == 6 && day <= 21)) return "双子座";
        if ((month == 6 && day >= 22) || (month == 7 && day <= 22)) return "巨蟹座";
        if ((month == 7 && day >= 23) || (month == 8 && day <= 22)) return "狮子座";
        if ((month == 8 && day >= 23) || (month == 9 && day <= 22)) return "处女座";
        if ((month == 9 && day >= 23) || (month == 10 && day <= 23)) return "天秤座";
        if ((month == 10 && day >= 24) || (month == 11 && day <= 22)) return "天蝎座";
        if ((month == 11 && day >= 23) || (month == 12 && day <= 21)) return "射手座";
        return "摩羯座";
    }

    private int calculateCompletionRateForFellowship(
            String nickname,
            String gender,
            Integer birthYear,
            String city,
            String occupation,
            String education,
            Integer height,
            String bio,
            String intention,
            String avatar
    ) {
        int total = 10;
        int done = 0;
        if (isNotBlank(nickname)) done++;
        if (isNotBlank(gender)) done++;
        if (birthYear != null) done++;
        if (isNotBlank(city)) done++;
        if (isNotBlank(occupation)) done++;
        if (isNotBlank(education)) done++;
        if (height != null) done++;
        if (isNotBlank(bio)) done++;
        if (isNotBlank(intention)) done++;
        if (isNotBlank(avatar)) done++;
        return (int) Math.round(done * 100.0 / total);
    }

    private Integer getInteger(Map<String, Object> payload, String key, Integer current, Integer fallback) {
        if (!payload.containsKey(key) || payload.get(key) == null) {
            return firstNonNull(current, fallback);
        }
        return toInteger(payload.get(key));
    }

    private Integer toInteger(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(raw).replace("cm", "").trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getText(Map<String, Object> payload, String key, String current, String fallback) {
        if (!payload.containsKey(key)) {
            return firstNonBlank(current, fallback);
        }
        String value = toTrimmed(payload.get(key));
        return value == null ? firstNonBlank(current, fallback) : value;
    }

    private String normalizeTags(Object incoming, String fallback) {
        if (incoming == null) return fallback;
        if (incoming instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim).filter(this::isNotBlank).collect(Collectors.joining(","));
        }
        String text = toTrimmed(incoming);
        return text == null ? fallback : text;
    }

    /**
     * 依次取第一个非空白的字符串；若全部空白则返回最后一项的 {@link #toTrimmed} 结果（可为 null）。
     */
    private String firstText(Object... parts) {
        if (parts == null || parts.length == 0) {
            return null;
        }
        for (int i = 0; i < parts.length - 1; i++) {
            String av = toTrimmed(parts[i]);
            if (isNotBlank(av)) {
                return av;
            }
        }
        return toTrimmed(parts[parts.length - 1]);
    }

    private String toTrimmed(Object raw) {
        if (raw == null) return null;
        String t = String.valueOf(raw).trim();
        return t.isBlank() ? null : t;
    }

    private boolean isNotBlank(String text) {
        return text != null && !text.isBlank();
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String defaultText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private String normalizeNickname(String rawNickname) {
        if (rawNickname == null) {
            return null;
        }
        String nickname = rawNickname.trim();
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("昵称最多 20 个字符");
        }
        return nickname;
    }

    private String writePhotosJson(List<String> urls) {
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void checkMissing(List<String> missing, String key, Object value) {
        if (value == null) {
            missing.add(key);
            return;
        }
        if (value instanceof String s && s.isBlank()) {
            missing.add(key);
        }
    }
}
