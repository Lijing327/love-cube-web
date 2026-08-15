// ─────────────────────────────────────────────────────────────────────────────
// Mobile 独立路由（UI 分离架构）
// 移动端优先入口：玩法 hub + 联谊 Tab 页 + 平台任务/签到/心声/互助
// 旧路由（/fellowship/*、/platform/*）保留不动，仅新增 /m/* 灰度入口
// 参考：docs/ui-architecture.md
// ─────────────────────────────────────────────────────────────────────────────

const authFellowship = { module: 'fellowship', requiresAuth: true }
const authPlatform = { module: 'platform', requiresAuth: true }

export default {
  path: '/m',
  component: () => import('@/layouts/MobileLayout.vue'),
  children: [
    { path: '', redirect: '/m/fellowship/discover' },

    // ── 平台玩法（纯 CSS，max-width 480px）──
    {
      path: 'platform',
      component: () => import('@/pages/mobile/platform/HubPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/tasks',
      component: () => import('@/pages/mobile/platform/MyTasksPage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/checkin',
      component: () => import('@/pages/mobile/platform/CheckinPage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/content',
      component: () => import('@/pages/platform/PlatformContentPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/positive-share',
      component: () => import('@/pages/mobile/platform/PositiveSharePage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/help/create',
      component: () => import('@/pages/platform/HelpCreatePage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/help/my',
      component: () => import('@/pages/platform/HelpMyPage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/help/:id',
      component: () => import('@/pages/platform/HelpDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/help',
      component: () => import('@/pages/mobile/platform/HelpSquarePage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/local',
      component: () => import('@/pages/mobile/platform/LocalServicesPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/topics',
      component: () => import('@/pages/mobile/platform/TopicsPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/messages',
      component: () => import('@/pages/mobile/platform/MessagesPage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/member',
      component: () => import('@/pages/mobile/platform/MemberPage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/groups/join',
      component: () => import('@/pages/platform/GroupJoinByInvitePage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/groups/create',
      component: () => import('@/pages/mobile/platform/GroupCreatePage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/my-groups',
      component: () => import('@/pages/mobile/platform/MyGroupsPage.vue'),
      meta: authPlatform
    },
    {
      path: 'platform/groups/:id',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/feed',
      redirect: to => `/m/platform/groups/${to.params.id}/posts`
    },
    {
      path: 'platform/groups/:id/members',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/posts',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/notices',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/articles',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/profile',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/checkin',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/tasks',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/activities',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/polls',
      component: () => import('@/pages/mobile/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/season',
      component: () => import('@/pages/platform/GroupSeasonRankingsPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups',
      component: () => import('@/pages/mobile/platform/GroupsPage.vue'),
      meta: { module: 'platform' }
    },

    // ── 联谊 Tab 页（Vant，复用现有页面组件）──
    {
      path: 'fellowship/discover',
      component: () => import('@/pages/fellowship/DiscoverPage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/match',
      component: () => import('@/pages/fellowship/MatchPage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/dynamic',
      component: () => import('@/pages/dynamic/index.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/dynamic/publish',
      component: () => import('@/pages/fellowship/DynamicPublishPage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/messages',
      component: () => import('@/pages/fellowship/MessagePage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/me',
      component: () => import('@/pages/mobile/fellowship/MePage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/vip',
      component: () => import('@/pages/fellowship/VipPage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/liked-me',
      component: () => import('@/pages/personal/LikedMePage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/my-likes',
      component: () => import('@/pages/personal/MyLikesPage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/event-signups',
      component: () => import('@/pages/fellowship/EventSignupsPage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/invite',
      component: () => import('@/pages/fellowship/InvitePage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/ai-tools',
      component: () => import('@/pages/mobile/fellowship/AiToolsPage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/match/history',
      component: () => import('@/pages/match/history.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/match/all',
      component: () => import('@/pages/match/all.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/user-profile/:id',
      component: () => import('@/pages/fellowship/UserProfilePage.vue'),
      meta: authFellowship
    },
    {
      path: 'fellowship/chat/:receiverId',
      component: () => import('@/pages/fellowship/ChatPage.vue'),
      meta: authFellowship
    },

    // 旧 /m/platform/* 通配重定向 → 新 hub（避免 404）
    {
      path: 'platform/:pathMatch(.*)*',
      redirect: to => {
        const tail = String(to.params.pathMatch || '')
        const map = {
          tasks: '/m/platform/tasks',
          checkin: '/m/platform/checkin',
          'positive-share': '/m/platform/positive-share',
          help: '/m/platform/help',
          local: '/m/platform/local',
          topics: '/m/platform/topics',
          groups: '/m/platform/groups',
          'my-groups': '/m/platform/my-groups',
          content: '/m/platform/content',
          messages: '/m/platform/messages',
          member: '/m/platform/member'
        }
        return map[tail] || '/m/platform'
      }
    }
  ]
}
