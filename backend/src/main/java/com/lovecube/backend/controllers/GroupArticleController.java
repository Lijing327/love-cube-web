package com.lovecube.backend.controllers;

import com.lovecube.backend.models.User;
import com.lovecube.backend.services.AdminAuthService;
import com.lovecube.backend.services.GroupArticleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups/{groupId}/articles")
public class GroupArticleController {

    private final GroupArticleService groupArticleService;
    private final AdminAuthService adminAuthService;

    public GroupArticleController(GroupArticleService groupArticleService, AdminAuthService adminAuthService) {
        this.groupArticleService = groupArticleService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable String groupId) {
        return groupArticleService.list(groupId);
    }

    @PostMapping
    public Map<String, Object> create(
            @PathVariable String groupId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload
    ) {
        User user = adminAuthService.requireUser(authHeader);
        return groupArticleService.create(groupId, user, payload);
    }

    @DeleteMapping("/{articleId}")
    public Map<String, Object> delete(
            @PathVariable String groupId,
            @PathVariable Long articleId,
            @RequestHeader("Authorization") String authHeader
    ) {
        User user = adminAuthService.requireUser(authHeader);
        return groupArticleService.delete(groupId, articleId, user);
    }
}
