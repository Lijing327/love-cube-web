export default {
  path: '/pc',
  component: () => import('@/layouts/PcLayout.vue'),
  children: [
    {
      path: '',
      redirect: '/pc/platform'
    },
    {
      path: 'platform',
      component: () => import('@/pages/pc/platform/HomePage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/me',
      component: () => import('@/pages/pc/platform/MePage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/me/favorites',
      component: () => import('@/pages/platform/MyFavoritesPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/growth',
      component: () => import('@/pages/pc/platform/GrowthRecordPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/play',
      component: () => import('@/pages/pc/platform/PlayHubPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/tasks',
      component: () => import('@/pages/pc/platform/MyTasksPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/checkin',
      component: () => import('@/pages/pc/platform/CheckinPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/content',
      component: () => import('@/pages/platform/PlatformContentPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/positive-share',
      component: () => import('@/pages/platform/PositiveSharePage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/help/create',
      component: () => import('@/pages/platform/HelpCreatePage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/help/my',
      component: () => import('@/pages/platform/HelpMyPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/help/:id',
      component: () => import('@/pages/platform/HelpDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/help',
      component: () => import('@/pages/platform/HelpSquarePage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/local',
      component: () => import('@/pages/pc/platform/LocalServicesPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/topics',
      component: () => import('@/pages/pc/platform/TopicsPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/messages',
      component: () => import('@/pages/pc/platform/MessagesPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/modules',
      component: () => import('@/pages/platform/ModulesPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/articles',
      component: () => import('@/pages/pc/platform/ArticlesPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/announcements',
      component: () => import('@/pages/platform/AnnouncementsPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/join',
      component: () => import('@/pages/platform/GroupJoinByInvitePage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/create',
      component: () => import('@/pages/platform/GroupCreatePage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/my-groups',
      component: () => import('@/pages/platform/MyGroupsPage.vue'),
      meta: { module: 'platform', requiresAuth: true }
    },
    {
      path: 'platform/me/groups',
      redirect: '/pc/platform/my-groups'
    },
    {
      path: 'platform/groups/:id/feed',
      redirect: to => `/pc/platform/groups/${to.params.id}/posts`
    },
    {
      path: 'platform/groups/:id/members',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/posts',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/notices',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/articles',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/profile',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/checkin',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/tasks',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/activities',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id/polls',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/:id',
      component: () => import('@/pages/pc/platform/GroupDetailPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups/season',
      component: () => import('@/pages/platform/GroupSeasonRankingsPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'platform/groups',
      component: () => import('@/pages/pc/platform/GroupsPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'events',
      component: () => import('@/pages/pc/platform/EventsPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'events/:id/onsite',
      component: () => import('@/pages/platform/OnsiteActivityEntryPage.vue'),
      meta: { module: 'platform' }
    },
    {
      path: 'events/:id',
      component: () => import('@/pages/platform/EventDetailPage.vue'),
      meta: { module: 'platform' }
    }
  ]
}

