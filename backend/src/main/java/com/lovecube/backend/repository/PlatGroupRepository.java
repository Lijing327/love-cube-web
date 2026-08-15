package com.lovecube.backend.repository;

import com.lovecube.backend.entity.PlatGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatGroupRepository extends JpaRepository<PlatGroup, Long> {

    List<PlatGroup> findByStatusOrderByMemberCountDescCreatedAtDesc(String status);

    List<PlatGroup> findAllByOrderByMemberCountDescCreatedAtDesc();

    List<PlatGroup> findTop5ByStatusOrderByMemberCountDesc(String status);

    Optional<PlatGroup> findBySlug(String slug);

    Optional<PlatGroup> findByInviteCode(String inviteCode);

    List<PlatGroup> findByOwnerUserId(Long ownerUserId);

    boolean existsByInviteCode(String inviteCode);

    long countByStatus(String status);
}
