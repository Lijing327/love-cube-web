package com.lovecube.backend.repository;

import com.lovecube.backend.entity.PlatGroupArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatGroupArticleRepository extends JpaRepository<PlatGroupArticle, Long> {

    List<PlatGroupArticle> findByGroupIdAndStatusOrderByCreatedAtDesc(String groupId, String status);

    Optional<PlatGroupArticle> findByIdAndGroupId(Long id, String groupId);
}
