package com.lovecube.backend.repository;

import com.lovecube.backend.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 用户消息表数据访问。
 * <p><strong>禁止业务 Service / Controller 直接注入本接口。</strong>仅允许
 * {@link com.lovecube.backend.services.NotificationService} 注入并调用，以降低绕过消息中心写入的风险。</p>
 *
 * @deprecated 对业务代码而言已废弃：请通过 {@link com.lovecube.backend.services.NotificationService} 创建与更新通知。
 */
@Deprecated(since = "notification-guard", forRemoval = false)
@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long>, JpaSpecificationExecutor<UserNotification> {

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.readAt = :now WHERE n.userId = :userId AND n.isRead = false")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserNotification n WHERE n.id = :id AND n.userId = :userId")
    int deleteForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserNotification n WHERE n.userId = :userId AND n.type = :type")
    int deleteByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    long countByIsReadFalse();

    long countByCreatedAtGreaterThanEqual(LocalDateTime start);

    boolean existsByUserIdAndTypeAndRelatedTypeAndRelatedId(
            Long userId, String type, String relatedType, String relatedId);
}
