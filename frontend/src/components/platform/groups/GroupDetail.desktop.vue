<template>
  <section class="group-detail-page">
    <div v-if="loading.detail" class="loading-state">加载团体详情中...</div>
    <div v-else-if="errors.detail" class="error-state">
      <h2>团体详情加载失败</h2>
      <p>{{ errors.detail }}</p>
      <button type="button" @click="loadDetail">重试</button>
      <router-link :to="groupsPath()">返回团体列表</router-link>
    </div>

    <template v-else-if="group">
      <section class="hero-card group-detail-hero operation-hero">
        <div class="hero-main">
          <p class="section-kicker">Space Overview</p>
          <div class="title-row">
            <img class="group-avatar" :src="heroImage" :alt="group.name">
            <div>
              <h1>{{ group.name }}</h1>
              <div class="hero-badges">
                <span class="status-badge neutral">{{ group.category }}</span>
                <span class="status-badge info">{{ activeStateLabel }}</span>
                <span class="status-badge" :class="myRoleBadge.tone">{{ myRoleBadge.label }}</span>
                <span class="status-badge neutral">{{ group.joinLabel }}</span>
              </div>
            </div>
          </div>
          <p class="meta-line">{{ group.region }} · {{ group.memberCount }} 人<template v-if="group.postCount != null"> · {{ group.postCount }} 条动态</template></p>
          <p class="group-desc">{{ group.description }}</p>
          <div class="hero-actions">
            <button type="button" class="platform-btn platform-btn-primary join-btn" :disabled="joinDisabled || joining" @click="applyJoin">
              {{ joining ? '处理中...' : joinButtonText }}
            </button>
            <router-link
              v-if="showSpaceManageLink"
              class="platform-btn platform-btn-ghost manage-link"
              :to="spaceManageEntry"
            >进入运营台</router-link>
          </div>
        </div>
        <div class="metric-grid compact detail-hero-metrics">
          <article v-for="item in detailStats" :key="item.label" class="metric-card">
            <span class="metric-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.hint }}</p>
          </article>
        </div>
      </section>

      <GroupCapabilityBanner :group-id="group.id" />

      <SpaceCampaignMemberPanel :group-id="group.id" :is-member="group.isMember" />

      <div v-if="group.managed && pendingMemberCount > 0" class="pending-audit-banner">
        <p>有 <strong>{{ pendingMemberCount }}</strong> 条入团申请待审核</p>
        <router-link class="text-link-btn" :to="spaceManageMembersEntry">去审核</router-link>
      </div>

      <GroupInvitePanel
        v-if="group.managed && joinModeKey === 'invite'"
        :group-id="group.id"
        :join-mode-key="joinModeKey"
        :managed="group.managed"
      />

      <div v-if="group.managed && weeklyDigest" class="weekly-digest-banner">
        <p>
          本周打卡率 <strong>{{ weeklyDigest.checkinRatePercent }}%</strong>，
          活跃成员 <strong>{{ weeklyDigest.activeMembers }}</strong> 人，
          待领取任务 <strong>{{ weeklyDigest.unclaimedTasks }}</strong> 项
        </p>
        <button type="button" class="text-link-btn" :disabled="sendingDigest" @click="sendWeeklyDigestNow">
          {{ sendingDigest ? '发送中…' : '发送周报到消息中心' }}
        </button>
      </div>

      <p v-if="joinModeKey === 'invite' && inviteCodeFromQuery && !group.isMember" class="invite-join-hint">
        你正在通过邀请加入，点击「验证邀请并加入」即可完成入团
      </p>

      <nav class="tabs" aria-label="团体内容">
        <router-link v-for="tab in tabs" :key="tab.key" :to="tab.to" :class="{ active: activeTab === tab.key }">
          {{ tab.label }}
        </router-link>
      </nav>

      <section class="detail-section-strip section-card" aria-label="Space 内容概览">
        <router-link v-for="item in detailSections" :key="item.key" :to="item.to" class="detail-section-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </router-link>
      </section>

      <section
        v-if="userStore.isLoggedIn && group.isMember"
        class="member-display-name-panel"
        aria-label="本团展示姓名"
      >
        <template v-if="!group.myMemberRealName">
          <p class="member-display-name-hint">{{ MEMBER_DISPLAY_PANEL_HINT }}</p>
          <button
            type="button"
            class="member-display-name-btn"
            :disabled="patchingMemberDisplayName"
            @click="openMemberDisplayNamePrompt(false)"
          >
            {{ patchingMemberDisplayName ? '保存中…' : '补填姓名' }}
          </button>
        </template>
        <div v-else class="member-display-name-row">
          <span class="member-display-name-label">
            本团展示姓名：<strong>{{ group.myMemberRealName }}</strong>
          </span>
          <button
            type="button"
            class="member-display-name-link"
            :disabled="patchingMemberDisplayName"
            @click="openMemberDisplayNamePrompt(true)"
          >
            {{ patchingMemberDisplayName ? '保存中…' : '修改' }}
          </button>
        </div>
      </section>

      <p v-if="message" class="page-message" :class="{ error: messageType === 'error' }">{{ message }}</p>

      <section class="content-grid">
        <main class="main-panel">
          <template v-if="activeTab === 'home'">
            <div class="home-two-col">
              <!-- 左列：打卡 + 动态 -->
              <div class="home-col">
                <!-- 今日打卡 -->
                <section class="panel-card">
                  <div class="section-head">
                    <h2>今日打卡</h2>
                    <div class="section-head-links">
                      <button type="button" class="text-link-btn" @click="goToTab('checkin')">查看全部</button>
                      <button v-if="userStore.isLoggedIn && group.isMember" type="button" class="text-link-btn" @click="goToTab('checkin')">排行榜</button>
                    </div>
                  </div>
                  <div v-if="loading.checkin" class="empty-inline">加载中...</div>
                  <template v-else>
                    <p class="home-guide-text">
                      <template v-if="userStore.isLoggedIn && group.isMember && checkinSummary.checkedInToday">
                        今日已打卡，连续 {{ checkinSummary.myStreakDays }} 天，继续保持 💪
                      </template>
                      <template v-else-if="userStore.isLoggedIn && group.isMember">
                        今日已有 {{ checkinSummary.todayCount }} 人打卡，来记录一下今天吧
                      </template>
                      <template v-else>加入团体后可参与每日打卡</template>
                    </p>
                    <div class="home-checkin-stats">
                      <div class="home-stat">
                        <span class="home-stat-val">{{ checkinSummary.todayCount }}</span>
                        <span class="home-stat-label">今日打卡人数</span>
                      </div>
                      <template v-if="userStore.isLoggedIn && group.isMember">
                        <div class="home-stat">
                          <span class="home-stat-val">{{ checkinSummary.myStreakDays }}</span>
                          <span class="home-stat-label">连续打卡天数</span>
                        </div>
                        <div class="home-stat">
                          <span class="home-stat-val" :class="{ 'stat-green': checkinSummary.checkedInToday }">
                            {{ checkinSummary.checkedInToday ? '已打卡' : '未打卡' }}
                          </span>
                          <span class="home-stat-label">今日状态</span>
                        </div>
                      </template>
                    </div>
                    <div class="home-card-foot">
                      <span v-if="checkinSummary.checkedInToday && userStore.isLoggedIn" class="checkin-done-hint">✓ 今日已完成打卡</span>
                      <span v-else-if="!group.isMember" class="home-muted-hint">加入团体后参与每日打卡</span>
                      <button v-else type="button" class="primary-btn small" @click="goToTab('checkin')">立即打卡</button>
                    </div>
                  </template>
                </section>

                <!-- 最新动态 -->
                <section class="panel-card">
                  <div class="section-head">
                    <h2>最新动态</h2>
                    <button type="button" class="text-link-btn" @click="goToTab('posts')">查看全部</button>
                  </div>
                  <div v-if="loading.posts" class="empty-inline">加载中...</div>
                  <div v-else-if="errors.posts" class="inline-error">{{ errors.posts }}</div>
                  <template v-else>
                    <p class="home-guide-text">
                      {{ posts.length ? '团体最近有新讨论，来看看大家在聊什么' : '这个团体还没有动态，来发布第一条吧' }}
                    </p>
                    <form v-if="group.isMember" class="quick-post-form" @submit.prevent="submitQuickPost">
                      <textarea
                        v-model="quickPostContent"
                        rows="2"
                        maxlength="2000"
                        placeholder="说点什么，和团体成员分享一下..."
                      ></textarea>
                      <div class="quick-post-foot">
                        <button
                          type="submit"
                          class="primary-btn small"
                          :disabled="quickPosting || contentCheck.state.checking || !quickPostContent.trim()"
                        >{{ quickPosting ? '发布中...' : '发布' }}</button>
                      </div>
                    </form>
                    <template v-if="posts.slice(0, 3).length">
                      <article v-for="post in posts.slice(0, 3)" :key="post.id" class="home-post-item">
                        <img :src="post.avatar" :alt="post.author" class="home-post-avatar">
                        <div class="home-post-body">
                          <div class="home-post-meta">
                            <strong>{{ post.author }}</strong>
                            <span>{{ post.time }}</span>
                          </div>
                          <p class="home-post-excerpt">{{ post.content }}</p>
                          <div class="home-post-stats">
                            <span>赞 {{ post.likes }}</span>
                            <span>评论 {{ post.comments }}</span>
                          </div>
                        </div>
                      </article>
                    </template>
                    <div v-else class="empty-inline">还没有人发布动态，来说第一句话吧</div>
                  </template>
                </section>
              </div>

              <!-- 右列：任务 + 活动 + 公告 -->
              <div class="home-col">
                <!-- 今日任务（仅登录成员） -->
                <section v-if="userStore.isLoggedIn && group.isMember" class="panel-card">
                  <div class="section-head">
                    <h2>今日任务</h2>
                    <button type="button" class="text-link-btn" @click="goToTab('tasks')">查看全部</button>
                  </div>
                  <div v-if="loading.tasks" class="empty-inline">加载中...</div>
                  <template v-else>
                    <p class="home-guide-text">
                      <template v-if="claimableCount > 0">你有 {{ claimableCount }} 个奖励待领取</template>
                      <template v-else-if="incompleteTasks.length > 0">再完成 {{ incompleteTasks.length }} 个任务可领取奖励</template>
                      <template v-else>今日任务已全部完成 🎉</template>
                    </p>
                    <div class="home-task-summary">
                      <span class="task-progress-text">
                        已完成 {{ todayTasks.filter((t) => t.completed).length }} / {{ todayTasks.length }} 项
                      </span>
                      <span v-if="claimableCount > 0" class="claimable-badge">{{ claimableCount }} 个奖励待领取</span>
                    </div>
                    <div v-if="incompleteTasks.length" class="home-task-list">
                      <div v-for="task in incompleteTasks.slice(0, 2)" :key="task.taskCode" class="home-task-item">
                        <span class="task-status-dot"></span>
                        <span>{{ task.name }}</span>
                        <span class="task-reward">+{{ task.rewardExp }} EXP</span>
                      </div>
                    </div>
                    <div v-else class="home-task-done">所有任务已完成 🎉</div>
                    <div class="home-card-foot">
                      <button type="button" class="primary-btn small" @click="goToTab('tasks')">
                        {{ claimableCount > 0 ? '领取奖励' : '去完成任务' }}
                      </button>
                    </div>
                  </template>
                </section>

                <section v-if="seasonRank" class="panel-card season-home-card">
                  <GroupSeasonPanel
                    :season-rank="seasonRank"
                    :season-top="seasonTop.slice(0, 5)"
                    title="季度赛季榜"
                  />
                </section>

                <!-- 最近活动 -->
                <section class="panel-card">
                  <div class="section-head">
                    <h2>最近活动</h2>
                    <button type="button" class="text-link-btn" @click="goToTab('activities')">查看全部</button>
                  </div>
                  <p class="home-guide-text">
                    {{ upcomingActivities.length ? `近期有 ${upcomingActivities.length} 个活动可参与` : '暂无近期活动，期待下次相聚' }}
                  </p>
                  <div v-if="upcomingActivities.length" class="home-activity-list">
                    <article v-for="act in upcomingActivities" :key="act.id" class="home-activity-item">
                      <div class="home-activity-head">
                        <strong>{{ act.title }}</strong>
                        <span :class="['activity-status', activityStatusClass(act)]">{{ activityStatusLabel(act) }}</span>
                      </div>
                      <div class="home-activity-meta">
                        <span>🕐 {{ formatActivityTime(act.startTime) }}</span>
                        <span v-if="act.location">📍 {{ act.location }}</span>
                        <span>👥 {{ act.participantCount }}{{ act.maxParticipants > 0 ? ' / ' + act.maxParticipants : '' }} 人</span>
                      </div>
                    </article>
                  </div>
                </section>

                <!-- 团体文章 -->
                <section class="panel-card">
                  <div class="section-head">
                    <h2>团体文章</h2>
                    <button type="button" class="text-link-btn" @click="goToTab('articles')">查看全部</button>
                  </div>
                  <p class="home-guide-text">
                    {{ articles.length ? '成员分享的长文都在这里' : '还没有文章，加入后可以写第一篇' }}
                  </p>
                  <div v-if="errors.articles" class="inline-error">{{ errors.articles }}</div>
                  <div v-else-if="articles.slice(0, 2).length" class="home-notice-list">
                    <article v-for="article in articles.slice(0, 2)" :key="article.id" class="notice-card compact">
                      <strong>{{ article.title }}</strong>
                      <p>{{ article.summary || article.content }}</p>
                      <time>{{ article.author }} · {{ article.date }}</time>
                    </article>
                  </div>
                </section>

                <!-- 团体公告 -->
                <section class="panel-card">
                  <div class="section-head">
                    <h2>团体公告</h2>
                    <button type="button" class="text-link-btn" @click="goToTab('notices')">查看全部</button>
                  </div>
                  <p class="home-guide-text">
                    {{ notices.length ? '请留意团体最新通知' : '暂无公告，请保持关注' }}
                  </p>
                  <div v-if="errors.notices" class="inline-error">{{ errors.notices }}</div>
                  <div v-else-if="notices.slice(0, 2).length" class="home-notice-list">
                    <article v-for="notice in notices.slice(0, 2)" :key="notice.id" class="notice-card compact">
                      <strong>{{ notice.title }}</strong>
                      <p>{{ notice.content }}</p>
                      <time>{{ notice.date }}</time>
                    </article>
                  </div>
                </section>
              </div>
            </div>
          </template>

          <section v-else-if="activeTab === 'posts'" class="panel-card">
            <div class="section-head">
              <h2>团体动态</h2>
            </div>
            <template v-if="group.isMember">
              <form class="post-form" @submit.prevent="submitPost">
                <textarea v-model.trim="postForm.content" rows="4" maxlength="2000" placeholder="分享团体动态..."></textarea>
                <input v-model.trim="postForm.imageUrls" type="text" placeholder="图片 URL，可用英文逗号分隔（可选）">
                <div class="post-form-foot">
                  <span>至少填写文字或图片链接之一</span>
                  <button type="submit" class="primary-btn" :disabled="posting || contentCheck.state.checking">{{ posting ? '发布中...' : '发布' }}</button>
                </div>
              </form>
            </template>
            <p v-else class="join-hint">加入团体后可以参与动态互动。</p>
            <p v-if="errors.posts" class="inline-error">{{ errors.posts }}</p>
            <template v-else-if="!posts.length">
              <div class="empty-inline">{{ emptyPostsHint }}</div>
            </template>
            <div v-else class="post-list post-list-feed">
              <article v-for="post in posts" :key="post.id" class="post-card">
                <div class="post-head">
                  <img :src="post.avatar" alt="">
                  <div>
                    <strong>{{ post.author }}</strong>
                    <span>{{ post.time }}</span>
                  </div>
                  <em>{{ post.role }}</em>
                  <button
                    v-if="canDeletePost(post)"
                    type="button"
                    class="post-delete-btn"
                    :disabled="deletingPostId === post.id"
                    @click="deletePost(post)"
                  >{{ deletingPostId === post.id ? '…' : '删除' }}</button>
                </div>
                <p>{{ post.content }}</p>
                <div v-if="post.images.length" class="post-images">
                  <img v-for="img in post.images" :key="img" :src="img" alt="">
                </div>
                <div class="post-actions">
                  <button
                    type="button"
                    :class="{ active: post.likedByMe }"
                    :disabled="!userStore.isLoggedIn || likingPostId === post.id"
                    @click="togglePostLike(post)"
                  >{{ post.likedByMe ? '已赞' : '赞' }} {{ post.likes }}</button>
                  <button type="button" @click="toggleCommentPanel(post)">
                    评论 {{ post.comments }}
                  </button>
                  <button
                    v-if="userStore.isLoggedIn"
                    type="button"
                    class="report-post-btn"
                    @click="openPostReport(post)"
                  >举报</button>
                </div>
                <div v-if="expandedPostId === post.id" class="comment-panel">
                  <p v-if="commentsLoading[post.id]" class="comment-status">评论加载中...</p>
                  <ul v-else class="comment-list">
                    <li v-for="c in (feedComments[post.id] || [])" :key="c.id">
                      <img :src="c.avatar" alt="">
                      <div class="comment-body">
                        <strong>{{ c.author }}</strong>
                        <span>{{ c.time }}</span>
                        <p>{{ c.content }}</p>
                      </div>
                      <button
                        v-if="canDeleteComment(c)"
                        type="button"
                        class="comment-del"
                        :disabled="deletingCommentKey === commentKey(post.id, c.id)"
                        @click="deleteComment(post, c)"
                      >删</button>
                    </li>
                    <li v-if="!(feedComments[post.id] || []).length" class="comment-empty">暂无评论</li>
                  </ul>
                  <form v-if="userStore.isLoggedIn" class="comment-form" @submit.prevent="submitComment(post)">
                    <input v-model.trim="commentDraft[post.id]" type="text" maxlength="500" placeholder="写一条评论…">
                    <button type="submit" class="primary-btn small" :disabled="commentPosting[post.id] || contentCheck.state.checking">
                      {{ commentPosting[post.id] ? '发送中' : '发送' }}
                    </button>
                  </form>
                  <p v-else class="comment-login-hint">登录后即可评论</p>
                </div>
              </article>
            </div>
          </section>

          <section v-else-if="activeTab === 'members'" class="panel-card">
            <div class="section-head member-tools">
              <h2>成员 {{ filteredMembers.length }}</h2>
              <div>
                <input v-model.trim="memberKeyword" type="search" placeholder="搜索成员">
                <select v-model="roleFilter" aria-label="角色筛选">
                  <option value="">全部角色</option>
                  <option value="owner">团长</option>
                  <option value="admin">管理员</option>
                  <option value="member">成员</option>
                </select>
              </div>
            </div>
            <div v-if="errors.members" class="inline-error">{{ errors.members }}</div>
            <div v-else-if="filteredMembers.length" class="member-list">
              <article v-for="member in filteredMembers" :key="member.id || member.userId" class="member-item">
                <img :src="member.avatar" :alt="member.name">
                <div class="member-main">
                  <div class="member-name-row">
                    <strong>{{ member.name }}</strong>
                    <span class="status-badge neutral">{{ member.roleLabel }}</span>
                  </div>
                  <span>{{ member.joinedAt || '未记录加入时间' }}</span>
                  <p v-if="member.status === 'pending' && member.applyReason">申请说明：{{ member.applyReason }}</p>
                </div>
                <span class="status-badge" :class="memberStatusBadgeClass(member)">{{ member.statusLabel }}</span>
                <div class="member-actions-wrap">
                  <div v-if="canAuditMember(member)" class="member-actions">
                    <button type="button" :disabled="moderatingMemberId === member.id" @click="approvePendingMember(member)">通过</button>
                    <button type="button" :disabled="moderatingMemberId === member.id" @click="rejectPendingMember(member)">拒绝</button>
                  </div>
                  <div v-if="canOwnerToggleAdmin(member)" class="member-actions owner-admin-actions">
                    <button
                      v-if="member.role === 'member'"
                      type="button"
                      class="role-admin-btn"
                      :disabled="roleChangingMemberId === member.id"
                      @click="setMemberAsAdmin(member)"
                    >设为管理员</button>
                    <button
                      v-else-if="member.role === 'admin'"
                      type="button"
                      class="role-admin-btn muted"
                      :disabled="roleChangingMemberId === member.id"
                      @click="unsetMemberAdmin(member)"
                    >取消管理员</button>
                  </div>
                  <button
                    v-if="canRemoveMember(member)"
                    type="button"
                    class="remove-member-btn"
                    :disabled="moderatingMemberId === member.id"
                    @click="removeMember(member)"
                  >移除</button>
                </div>
              </article>
            </div>
            <div v-else class="empty-inline">暂无成员</div>
          </section>

          <section v-else-if="activeTab === 'articles'" class="panel-card">
            <div class="section-head">
              <h2>团体文章</h2>
            </div>
            <template v-if="group.isMember">
              <form class="post-form" @submit.prevent="submitArticle">
                <input v-model.trim="articleForm.title" type="text" maxlength="200" placeholder="文章标题" required>
                <textarea v-model.trim="articleForm.content" rows="8" maxlength="20000" placeholder="写下正文，适合分享经验、故事或共读笔记"></textarea>
                <input v-model.trim="articleForm.coverUrl" type="text" placeholder="封面图 URL（可选）">
                <div class="post-form-foot">
                  <span>加入成员可发布，作者或管理员可删除</span>
                  <button type="submit" class="primary-btn" :disabled="publishingArticle || contentCheck.state.checking">
                    {{ publishingArticle ? '发布中...' : '发布文章' }}
                  </button>
                </div>
              </form>
            </template>
            <p v-else class="join-hint">加入团体后可以发布文章。</p>
            <p v-if="errors.articles" class="inline-error">{{ errors.articles }}</p>
            <div v-else-if="articles.length" class="notice-list">
              <article v-for="article in articles" :key="article.id" class="notice-card">
                <button type="button" @click="toggleArticle(article.id)">
                  <span>{{ article.title }}</span>
                  <time>{{ article.author }} · {{ article.date }}</time>
                </button>
                <p v-if="expandedArticleId !== article.id">{{ article.summary || article.content }}</p>
                <div v-else class="article-full">
                  <img v-if="article.coverUrl" :src="article.coverUrl" alt="" class="article-cover">
                  <p class="article-body">{{ article.content }}</p>
                  <button
                    v-if="canDeleteArticle(article)"
                    type="button"
                    class="text-link-btn"
                    :disabled="deletingArticleId === article.id"
                    @click="removeArticle(article)"
                  >删除</button>
                </div>
              </article>
            </div>
            <div v-else class="empty-inline">暂无文章</div>
          </section>

          <section v-else-if="activeTab === 'notices'" class="panel-card">
            <div class="section-head">
              <h2>团体公告</h2>
            </div>
            <div v-if="errors.notices" class="inline-error">{{ errors.notices }}</div>
            <div v-else-if="notices.length" class="notice-list">
              <article v-for="notice in notices" :key="notice.id" class="notice-card">
                <button type="button" @click="toggleNotice(notice.id)">
                  <span>{{ notice.title }}</span>
                  <time>{{ notice.date }}</time>
                </button>
                <p v-if="expandedNoticeId === notice.id">{{ notice.content }}</p>
              </article>
            </div>
            <div v-else class="empty-inline">暂无公告</div>
          </section>

          <!-- 打卡 tab -->
          <section v-else-if="activeTab === 'checkin'" class="panel-card">
            <div class="section-head">
              <h2>团体打卡</h2>
            </div>
            <div v-if="loading.checkin" class="empty-inline">加载中...</div>
            <template v-else>
              <p class="checkin-play-hint" role="note">
                今日打卡、点赞互动、进入排行榜，连续参与可提升称号。
              </p>
              <div class="checkin-summary">
                <div class="checkin-stat">
                  <span class="stat-val">{{ checkinSummary.todayCount }}</span>
                  <span class="stat-label">今日打卡人数</span>
                </div>
                <template v-if="userStore.isLoggedIn">
                  <div class="checkin-stat">
                    <span class="stat-val">{{ checkinSummary.myStreakDays }}</span>
                    <span class="stat-label">我的连续天数</span>
                  </div>
                  <div class="checkin-stat">
                    <span class="stat-val" :class="{ 'checked': checkinSummary.checkedInToday }">
                      {{ checkinSummary.checkedInToday ? '✓ 已打卡' : '未打卡' }}
                    </span>
                    <span class="stat-label">今日状态</span>
                  </div>
                </template>
              </div>

              <section v-if="userStore.isLoggedIn && group.isMember" class="rankings-card" aria-label="打卡排行榜">
                <div class="sub-section-head"><span>排行榜</span></div>
                <div class="ranking-tabs" role="tablist">
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="rankingTab === 'daily'"
                    :class="{ active: rankingTab === 'daily' }"
                    @click="setRankingTab('daily')"
                  >今日打卡榜</button>
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="rankingTab === 'streak'"
                    :class="{ active: rankingTab === 'streak' }"
                    @click="setRankingTab('streak')"
                  >连续打卡榜</button>
                </div>
                <div v-if="loadingRankings" class="empty-inline">加载中...</div>
                <p v-else-if="rankingEmptyHint" class="ranking-empty-hint">{{ rankingEmptyHint }}</p>
                <ol v-else class="ranking-list">
                  <li
                    v-for="(row, idx) in rankingDisplayRows"
                    :key="row.userId + '-' + idx"
                    :class="rankingRowClasses(row, idx)"
                  >
                    <span class="rank-medal" aria-hidden="true">{{ rankSlotDisplay(row, idx) }}</span>
                    <img class="rank-avatar" :src="row.avatarUrl || defaultAvatar" alt="">
                    <div class="rank-main">
                      <div class="rank-name-line">
                        <strong class="rank-nickname">{{ row.nickname || '成员' }}</strong>
                        <span v-if="row.title" class="user-title-pill rank-title-pill">{{ row.title }}</span>
                        <span v-if="row.__appendedMe" class="rank-append-tag">我的排名</span>
                      </div>
                      <div class="rank-meta">
                        <span>第 {{ row.rank }} 名</span>
                        <span v-if="rankingTab === 'daily'">今日打卡 {{ row.checkinCount }} 次</span>
                        <span>连续 {{ row.streakDays }} 天</span>
                      </div>
                    </div>
                  </li>
                </ol>
              </section>

              <template v-if="group.isMember && !checkinSummary.checkedInToday">
                <form class="checkin-form" @submit.prevent="submitCheckin">
                  <div class="checkin-types">
                    <button
                      v-for="t in checkinTypes"
                      :key="t.value"
                      type="button"
                      :class="{ active: checkinForm.type === t.value }"
                      @click="checkinForm.type = t.value"
                    >{{ t.label }}</button>
                  </div>
                  <textarea v-model.trim="checkinForm.content" rows="2" maxlength="200"
                    placeholder="打卡一句话（可选）"></textarea>
                  <div class="post-form-foot">
                    <span>{{ checkinForm.type ? checkinTypeLabels[checkinForm.type] : '选择打卡类型' }}</span>
                    <button type="submit" class="primary-btn" :disabled="checkingIn || !checkinForm.type">
                      {{ checkingIn ? '打卡中...' : '立即打卡' }}
                    </button>
                  </div>
                </form>
              </template>
              <div v-else-if="!group.isMember" class="join-hint">加入团体后可参与每日打卡。</div>
              <div v-else class="join-hint checkin-done">✓ 今日已打卡，明天再来吧！</div>

              <div class="sub-section-head">
                <span>最近打卡</span>
              </div>
              <div v-if="checkinSummary.recentCheckins?.length" class="checkin-list">
                <div v-for="c in checkinSummary.recentCheckins" :key="c.id" class="checkin-item">
                  <img :src="c.avatar || defaultAvatar" :alt="c.username">
                  <div class="checkin-body">
                    <div class="checkin-userline">
                      <strong class="checkin-username">{{ c.username }}</strong>
                      <span v-if="c.title" class="user-title-pill checkin-title-pill">{{ c.title }}</span>
                    </div>
                    <p v-if="c.content" class="checkin-text">{{ c.content }}</p>
                    <p class="checkin-submeta">
                      {{ checkinTypeLabels[c.checkinType] || c.checkinType }} · 连续 {{ c.streakDays }} 天
                    </p>
                    <time class="checkin-time">{{ formatDateTime(c.createdAt) }}</time>
                    <div v-if="userStore.isLoggedIn && group.isMember" class="checkin-social-row">
                      <button
                        type="button"
                        class="checkin-social-btn"
                        :class="{ active: c.likedByCurrentUser }"
                        :disabled="checkinLikeBusyId === c.id"
                        @click="toggleLikeCheckin(c)"
                      >赞 {{ c.likeCount ?? 0 }}</button>
                      <button
                        type="button"
                        class="checkin-social-btn"
                        :class="{ 'is-open': expandedCheckinId === c.id }"
                        @click="toggleCommentsBlock(c)"
                      >评论 {{ c.commentCount ?? 0 }}</button>
                    </div>
                    <div v-if="expandedCheckinId === c.id" class="checkin-comments-panel">
                      <p v-if="checkinCommentsLoading[c.id]" class="empty-inline">加载评论...</p>
                      <template v-else>
                        <ul class="checkin-comment-list">
                          <li v-for="cm in visibleCommentsFor(c.id)" :key="cm.id" class="checkin-comment-li">
                            <span class="cc-author">{{ cm.nickname || '用户' }}</span>
                            <span class="cc-text">{{ cm.content }}</span>
                            <time class="cc-time">{{ formatDateTime(cm.createdAt) }}</time>
                            <button
                              v-if="Number(cm.userId) === currentUserIdNum"
                              type="button"
                              class="cc-del"
                              @click="deleteMyCheckinComment(c.id, cm.id)"
                            >删除</button>
                          </li>
                          <li v-if="!(checkinCommentsById[c.id] || []).length" class="checkin-comment-empty">
                            还没有评论，来说一句吧
                          </li>
                        </ul>
                        <button
                          v-if="hasMoreComments(c.id)"
                          type="button"
                          class="checkin-comments-more"
                          @click="showAllCommentsFor(c.id)"
                        >查看更多评论</button>
                      </template>
                      <div class="checkin-comment-form">
                        <input
                          v-model="checkinCommentDraft[c.id]"
                          type="text"
                          maxlength="200"
                          placeholder="写一条评论..."
                          class="checkin-comment-input"
                        >
                        <button type="button" class="primary-btn small" :disabled="checkinCommentPosting" @click="submitCheckinComment(c)">
                          发送
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="empty-inline">暂无打卡记录</div>
            </template>
          </section>

          <!-- 任务 tab -->
          <section v-else-if="activeTab === 'tasks'" class="panel-card">
            <div class="section-head">
              <h2>今日任务</h2>
              <span class="task-date">{{ todayStr }}</span>
            </div>
            <div v-if="!group.isMember" class="join-hint">加入团体后可参与每日任务。</div>
            <div v-else-if="loading.tasks" class="empty-inline">加载中...</div>
            <div v-else class="task-list">
              <div v-for="task in todayTasks" :key="task.taskCode" class="task-item">
                <div class="task-info">
                  <span :class="['task-status-dot', task.completed ? 'done' : '']"></span>
                  <div>
                    <strong>{{ task.name }}</strong>
                    <span class="task-reward">+{{ task.rewardExp }} EXP</span>
                  </div>
                </div>
                <div class="task-action">
                  <span v-if="task.claimed" class="task-claimed">已领取</span>
                  <button
                    v-else-if="task.completed"
                    type="button"
                    class="primary-btn small"
                    :disabled="claimingTask === task.taskCode"
                    @click="claimTask(task)"
                  >{{ claimingTask === task.taskCode ? '领取中...' : '领取奖励' }}</button>
                  <span v-else class="task-pending">未完成</span>
                </div>
              </div>
              <div v-if="!todayTasks.length" class="empty-inline">暂无任务数据</div>
            </div>
          </section>

          <!-- 活动 tab -->
          <section v-else-if="activeTab === 'activities'" class="panel-card">
            <div class="section-head">
              <h2>团体活动</h2>
              <div class="section-head-actions">
                <button v-if="group.managed" type="button" class="primary-btn small" @click="showCreateActivity = !showCreateActivity">
                  {{ showCreateActivity ? '取消' : '发布活动' }}
                </button>
                <template v-else-if="userStore.isLoggedIn && group.isMember">
                  <router-link class="text-link-btn" :to="`/platform/groups/${group.id}/my-activity-proposals`">我的投稿</router-link>
                  <button type="button" class="primary-btn small" @click="handleProposeActivity">发起活动</button>
                </template>
              </div>
            </div>

            <form v-if="showCreateActivity" class="activity-form" @submit.prevent="submitActivity">
              <input v-model.trim="activityForm.title" type="text" maxlength="200" placeholder="活动标题 *" required>
              <textarea v-model.trim="activityForm.description" rows="3" maxlength="2000" placeholder="活动简介"></textarea>
              <div class="activity-form-row">
                <div>
                  <label>开始时间 *</label>
                  <input v-model="activityForm.startTime" type="datetime-local" required>
                </div>
                <div>
                  <label>结束时间 *</label>
                  <input v-model="activityForm.endTime" type="datetime-local" required>
                </div>
              </div>
              <div class="activity-form-row">
                <div>
                  <label>地点</label>
                  <input v-model.trim="activityForm.location" type="text" maxlength="200" placeholder="活动地点">
                </div>
                <div>
                  <label>人数上限（0=不限）</label>
                  <input v-model.number="activityForm.maxParticipants" type="number" min="0" max="9999">
                </div>
                <div>
                  <label>关联平台活动（可选）</label>
                  <select v-model="activityForm.platformEventId" class="activity-event-select">
                    <option value="">不关联</option>
                    <option v-for="ev in platformEvents" :key="ev.id" :value="ev.id">{{ ev.title }}</option>
                  </select>
                </div>
              </div>
              <p v-if="activityForm.platformEventId" class="activity-event-hint">
                关联后可从
                <router-link :to="`/events/${activityForm.platformEventId}`" target="_blank">活动中心</router-link>
                查看详情
              </p>
              <div class="post-form-foot">
                <span></span>
                <button type="submit" class="primary-btn" :disabled="creatingActivity">
                  {{ creatingActivity ? '发布中...' : '确认发布' }}
                </button>
              </div>
            </form>

            <template v-if="loading.activities">
              <div class="empty-inline">加载中...</div>
            </template>
            <template v-else>
              <div v-if="!activities.length" class="empty-inline">暂无活动</div>
              <div v-else class="activity-list">
              <article v-for="act in activities" :key="act.id" class="activity-card">
                <div class="activity-head">
                  <strong>{{ act.title }}</strong>
                  <span :class="['activity-status', activityStatusClass(act)]">
                    {{ activityStatusLabel(act) }}
                  </span>
                </div>
                <p v-if="act.description" class="activity-desc">{{ act.description }}</p>
                <div class="activity-meta">
                  <span>🕐 {{ formatActivityTime(act.startTime) }} — {{ formatActivityTime(act.endTime) }}</span>
                  <span v-if="act.location">📍 {{ act.location }}</span>
                  <span>👥 {{ act.participantCount }}{{ act.maxParticipants > 0 ? ' / ' + act.maxParticipants : '' }} 人</span>
                </div>
                <div v-if="act.platformEventPath" class="activity-platform-link">
                  <router-link :to="act.platformEventPath" target="_blank">查看平台活动详情 →</router-link>
                </div>
                <div class="activity-actions">
                  <template v-if="!act.isEnded && act.status === 'published'">
                    <button
                      v-if="!act.signedUpByMe"
                      type="button"
                      class="primary-btn small"
                      :disabled="signingActivityId === act.id"
                      @click="signUp(act)"
                    >{{ signingActivityId === act.id ? '报名中...' : '立即报名' }}</button>
                    <button
                      v-else
                      type="button"
                      class="cancel-signup-btn"
                      :disabled="signingActivityId === act.id"
                      @click="cancelSignUp(act)"
                    >{{ signingActivityId === act.id ? '处理中...' : '取消报名' }}</button>
                  </template>
                  <span v-if="act.signedUpByMe && act.checkedInByMe" class="signed-badge">已签到</span>
                  <span v-else-if="act.signedUpByMe && !act.isEnded" class="signed-badge">已报名</span>
                  <button
                    v-if="act.signedUpByMe && act.hasCheckinCode && !act.checkedInByMe && !act.isEnded"
                    type="button"
                    class="primary-btn small outline"
                    @click="openActivityCheckin(act)"
                  >现场签到</button>
                  <button
                    v-if="group.managed && act.status === 'published' && !act.isEnded"
                    type="button"
                    class="text-link-btn"
                    :disabled="checkinCodeBusyId === act.id"
                    @click="generateActivityCode(act)"
                  >{{ checkinCodeBusyId === act.id ? '生成中…' : (act.hasCheckinCode ? '刷新签到码' : '生成签到码') }}</button>
                  <button
                    v-if="act.canReview && act.pendingReviewCount > 0"
                    type="button"
                    class="primary-btn small"
                    @click="openActivityReview(act)"
                  >互评（{{ act.pendingReviewCount }}）</button>
                  <button
                    v-if="group.managed && act.status === 'published'"
                    type="button"
                    class="cancel-activity-btn"
                    :disabled="cancellingActivityId === act.id"
                    @click="cancelActivity(act)"
                  >取消活动</button>
                </div>
              </article>
            </div>
            </template>
          </section>

          <section v-else-if="activeTab === 'polls'" class="panel-card">
            <div class="section-head">
              <h2>团体投票</h2>
              <button
                v-if="group.managed && isPollsSupported"
                type="button"
                class="primary-btn small"
                @click="showPollCreate = !showPollCreate"
              >
                {{ showPollCreate ? '取消' : '发起投票' }}
              </button>
            </div>
            <p v-if="!isPollsSupported" class="poll-legacy-hint">当前团体类型暂不支持线上投票。</p>
            <template v-else>
              <form v-if="showPollCreate" class="poll-form" @submit.prevent="submitPollCreate">
                <input v-model.trim="pollForm.title" type="text" maxlength="200" placeholder="投票标题 *" required>
                <textarea v-model.trim="pollForm.description" rows="2" maxlength="2000" placeholder="说明（可选）"></textarea>
                <div class="poll-form-row">
                  <label class="poll-field">
                    <span>类型</span>
                    <select v-model="pollForm.selectionMode">
                      <option value="single">单选</option>
                      <option value="multiple">多选</option>
                    </select>
                  </label>
                  <label v-if="pollForm.selectionMode === 'multiple'" class="poll-field">
                    <span>每人最多可选几项 *</span>
                    <input v-model.number="pollForm.maxChoices" type="number" min="2" max="30" required>
                  </label>
                </div>
                <div class="activity-form-row">
                  <div>
                    <label>开始时间 *</label>
                    <input v-model="pollForm.startTime" type="datetime-local" required>
                  </div>
                  <div>
                    <label>结束时间 *</label>
                    <input v-model="pollForm.endTime" type="datetime-local" required>
                  </div>
                </div>
                <label class="poll-options-label">选项（每行一个，至少 2 行）*</label>
                <textarea v-model="pollForm.optionsText" rows="5" maxlength="8000" required placeholder="选项 A&#10;选项 B"></textarea>
                <div class="post-form-foot">
                  <span></span>
                  <button type="submit" class="primary-btn" :disabled="creatingPoll">
                    {{ creatingPoll ? '发布中...' : '发布投票' }}
                  </button>
                </div>
              </form>

              <div v-if="loading.polls" class="empty-inline">加载中...</div>
              <div v-else-if="!polls.length" class="empty-inline">暂无投票</div>
              <div v-else class="poll-list">
                <article v-for="p in polls" :key="p.id" class="poll-card">
                  <div class="poll-card-head">
                    <strong>{{ p.title }}</strong>
                    <span class="poll-mode-pill">{{ p.selectionMode === 'multiple' ? '多选' : '单选' }}</span>
                    <span v-if="p.isEnded" class="poll-status ended">已结束</span>
                    <span v-else-if="p.isNotYetOpen" class="poll-status wait">未开始</span>
                    <span v-else class="poll-status live">进行中</span>
                  </div>
                  <p class="poll-time">🕐 {{ formatActivityTime(p.startTime) }} — {{ formatActivityTime(p.endTime) }}</p>
                  <p v-if="p.hasVoted" class="poll-voted-tag">你已提交选择</p>
                  <p v-if="!p.countsVisible" class="poll-count-hint">
                    各选项票数在管理者「公开结果」前不对普通成员展示；管理者可随时查看票数。
                  </p>
                  <button type="button" class="text-link-btn" @click="togglePollExpand(p.id)">
                    {{ expandedPollId === p.id ? '收起' : '查看并参与' }}
                  </button>

                  <div v-if="expandedPollId === p.id && pollDetailCache[p.id]" class="poll-detail">
                    <p v-if="pollDetailCache[p.id].description" class="poll-detail-desc">{{ pollDetailCache[p.id].description }}</p>
                    <p v-if="pollDetailCache[p.id].countsVisible && pollDetailCache[p.id].voterCount != null" class="poll-voter-total">
                      参与人数：{{ pollDetailCache[p.id].voterCount }}
                    </p>
                    <ul class="poll-option-list">
                      <li v-for="opt in pollDetailCache[p.id].options" :key="opt.id" class="poll-option-row">
                        <label class="poll-option-label">
                          <input
                            v-if="pollDetailCache[p.id].selectionMode === 'single'"
                            type="radio"
                            :name="'poll-sel-' + p.id"
                            :checked="(pollPick[p.id] || []).includes(opt.id)"
                            :disabled="!group.isMember || pollDetailCache[p.id].isEnded || pollDetailCache[p.id].isNotYetOpen"
                            @change="setPollSinglePick(p.id, opt.id)"
                          >
                          <input
                            v-else
                            type="checkbox"
                            :checked="(pollPick[p.id] || []).includes(opt.id)"
                            :disabled="!group.isMember || pollDetailCache[p.id].isEnded || pollDetailCache[p.id].isNotYetOpen"
                            @change="togglePollMultiPick(p.id, opt.id, pollDetailCache[p.id], $event.target.checked)"
                          >
                          <span class="poll-option-text">{{ opt.label }}</span>
                          <span v-if="pollDetailCache[p.id].countsVisible && opt.voteCount != null" class="poll-option-count">
                            得票 {{ opt.voteCount }}
                          </span>
                        </label>
                      </li>
                    </ul>
                    <div class="poll-detail-actions">
                      <button
                        v-if="group.isMember && !pollDetailCache[p.id].isEnded && !pollDetailCache[p.id].isNotYetOpen"
                        type="button"
                        class="primary-btn small"
                        :disabled="votingPollId === p.id || !(pollPick[p.id] || []).length"
                        @click="submitPollVote(p.id)"
                      >
                        {{ votingPollId === p.id ? '提交中...' : '提交选择' }}
                      </button>
                      <button
                        v-if="group.managed && !pollDetailCache[p.id].resultsPublic"
                        type="button"
                        class="poll-reveal-btn"
                        :disabled="revealingPollId === p.id"
                        @click="revealPollResults(p.id)"
                      >
                        {{ revealingPollId === p.id ? '处理中...' : '公开结果（向成员展示票数）' }}
                      </button>
                    </div>
                  </div>
                </article>
              </div>
            </template>
          </section>

          <section v-else-if="activeTab === 'profile'" class="panel-card info-card">
            <h2>团体资料</h2>
            <InfoList :group="group" />
          </section>
        </main>

        <aside class="side-panel">
          <section v-if="activeTab !== 'profile'" class="side-card">
            <h2>团体资料</h2>
            <InfoList :group="group" compact />
          </section>
          <section v-if="activeTab === 'home'" class="side-card">
            <h2>成员概况</h2>
            <p>{{ group.memberCount }} 位成员 · {{ admins.length }} 位管理员</p>
            <router-link :to="groupsPath(String(group.id), 'members')">查看成员列表</router-link>
          </section>
          <section v-if="activeTab === 'home' && seasonRank" class="side-card season-card">
            <GroupSeasonPanel
              :season-rank="seasonRank"
              :season-top="seasonTop.slice(0, 5)"
              compact
              :show-breakdown="false"
            />
          </section>
        </aside>
      </section>
    </template>

    <ContentCheckDialog
      v-if="contentCheck.state.show"
      :suggestion="contentCheck.state.suggestion"
      :hit-words="contentCheck.state.hitWords"
      @use-suggestion="contentCheck.applySuggestion"
      @continue="contentCheck.continueAnyway"
    />

    <div v-if="activityCheckinOpen" class="activity-modal-overlay" @click.self="activityCheckinOpen = false">
      <div class="activity-modal">
        <h3>现场签到</h3>
        <p>请输入管理员提供的 6 位签到码</p>
        <input v-model.trim="activityCheckinCode" type="text" maxlength="8" placeholder="签到码">
        <div class="activity-modal-actions">
          <button type="button" class="text-link-btn" @click="activityCheckinOpen = false">取消</button>
          <button type="button" class="primary-btn small" :disabled="activityCheckinSubmitting" @click="submitActivityCheckin">
            {{ activityCheckinSubmitting ? '提交中…' : '确认签到' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="activityReviewOpen" class="activity-modal-overlay" @click.self="closeActivityReview">
      <div class="activity-modal wide">
        <h3>活动互评 · {{ activityReviewTarget?.title }}</h3>
        <p v-if="activityReviewLoading" class="empty-inline">加载中…</p>
        <ul v-else class="review-candidate-list">
          <li v-for="c in activityReviewCandidates" :key="c.userId">
            <img :src="c.avatarUrl || defaultAvatar" alt="">
            <span>{{ c.nickname }}</span>
            <template v-if="c.reviewed">
              <em>已评价</em>
            </template>
            <template v-else>
              <select v-model="activityReviewRatings[c.userId]">
                <option v-for="n in 5" :key="n" :value="n">{{ n }} 分</option>
              </select>
              <button type="button" class="primary-btn small" @click="submitOneReview(c)">提交</button>
            </template>
          </li>
        </ul>
        <button type="button" class="text-link-btn" @click="closeActivityReview">关闭</button>
      </div>
    </div>

    <GroupPostReportDialog
      :open="reportDialogOpen"
      :post-id="reportPostId"
      :target-user-id="reportTargetUserId"
      @close="reportDialogOpen = false"
      @submitted="flashMessage('举报已提交')"
    />

    <ActivityProposalDialog
      :open="showProposalDialog"
      :group-id="group?.id"
      @close="showProposalDialog = false"
      @submitted="onProposalSubmitted"
    />
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  approveMember,
  approveAdminGroupRequest,
  createGroupPost,
  createGroupPostComment,
  deletePlatformGroupPost,
  deletePlatformGroupPostComment,
  fetchGroupDetail,
  fetchGroupMembers,
  fetchGroupArticles,
  createGroupArticle,
  deleteGroupArticle,
  fetchGroupNotices,
  fetchGroupPostComments,
  fetchGroupPosts,
  getAdminGroupJoinRequests,
  isLegacyPlatformGroupId,
  joinGroup,
  patchMyGroupMemberRealName,
  unwrapGroupPostsList,
  rejectMember,
  rejectAdminGroupRequest,
  removeGroupMember,
  patchPlatformGroupMemberRole,
  toggleGroupPostLike,
  fetchCheckinSummary,
  createCheckin,
  fetchCheckinRankings,
  likePlatformCheckin,
  unlikePlatformCheckin,
  fetchPlatformCheckinComments,
  createPlatformCheckinComment,
  deletePlatformCheckinComment,
  fetchTodayTasks,
  claimGroupTask,
  fetchGroupActivities,
  createGroupActivity,
  signUpGroupActivity,
  cancelGroupActivitySignup,
  updateGroupActivity,
  fetchGroupPolls,
  createGroupPoll,
  fetchGroupPoll,
  submitGroupPollVotes,
  revealGroupPollResults,
  fetchGroupSeasonRank,
  fetchGroupSeasonRankings,
  generateGroupActivityCheckinCode,
  checkinGroupActivity,
  fetchGroupActivityReviewCandidates,
  submitGroupActivityReview,
  fetchGroupWeeklyDigest,
  sendGroupWeeklyDigest,
  resolveSpaceManageEntry
} from '@/api/groups.js'
import { fetchEvents } from '@/api/platformContent.js'
import {
  AUDIT_JOIN_MESSAGE_PROMPT,
  ERR_EMPTY_AUDIT_JOIN_MESSAGE,
  ERR_EMPTY_DISPLAY_NAME_PATCH,
  ERR_EMPTY_MEMBER_REAL_NAME,
  JOIN_MEMBER_REAL_NAME_PROMPT,
  MEMBER_DISPLAY_PANEL_HINT,
  supplementMemberDisplayTitle
} from '@/utils/groupMemberDisplayName.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { useUserStore } from '@/stores/user.js'
import { useContentCheck } from '@/composables/useContentCheck.js'
import ContentCheckDialog from '@/components/common/ContentCheckDialog.vue'
import GroupCapabilityBanner from '@/components/platform/groups/GroupCapabilityBanner.vue'
import SpaceCampaignMemberPanel from '@/components/platform/spaces/SpaceCampaignMemberPanel.vue'
import GroupInvitePanel from '@/components/platform/groups/GroupInvitePanel.vue'
import GroupSeasonPanel from '@/components/platform/groups/GroupSeasonPanel.vue'
import GroupPostReportDialog from '@/components/platform/groups/GroupPostReportDialog.vue'
import ActivityProposalDialog from '@/components/platform/groups/ActivityProposalDialog.vue'

const DEFAULT_COVER = 'https://images.unsplash.com/photo-1507692049790-de58290a4334?auto=format&fit=crop&w=1400&q=80'
const DEFAULT_AVATAR = 'https://api.dicebear.com/7.x/initials/svg?seed=LC&backgroundColor=eff6ff,fdf2f8,eef2ff'
const defaultAvatar = DEFAULT_AVATAR

const checkinTypes = [
  { value: 'thanks', label: '感谢' },
  { value: 'prayer', label: '心愿' },
  { value: 'study', label: '学习' },
  { value: 'exercise', label: '运动' },
  { value: 'share', label: '分享' },
  { value: 'other', label: '其他' }
]
const checkinTypeLabels = {
  thanks: '感谢', prayer: '心愿', study: '学习',
  exercise: '运动', share: '分享', other: '其他'
}

const todayStr = new Date().toISOString().slice(0, 10)

const route = useRoute()
const router = useRouter()
const { groupsPath } = usePlatformPath()
const userStore = useUserStore()
const contentCheck = useContentCheck()
const currentUserIdNum = computed(() => Number(userStore.userInfo?.id || userStore.userId || 0))
const group = ref(null)

function groupTabPath(segment) {
  const id = String(group.value?.id || route.params.id || '')
  return segment && segment !== 'home' ? groupsPath(id, segment) : groupsPath(id)
}

const showSpaceManageLink = computed(() => {
  const g = group.value
  if (!g || !userStore.isLoggedIn) return false
  if (g.managed || g.canReviewJoins) return true
  return userStore.hasPermission('group.manage.all')
})

const spaceManageEntry = computed(() => {
  const id = group.value?.id
  if (!id) return groupsPath()
  return resolveSpaceManageEntry(String(id))
})

const spaceManageMembersEntry = computed(() => {
  const id = group.value?.id
  if (!id) return groupsPath()
  return resolveSpaceManageEntry(String(id), { tab: 'members' })
})
const rawMembers = ref([])
const rawPosts = ref([])
const rawNotices = ref([])
const rawArticles = ref([])
const expandedArticleId = ref(null)
const articleForm = reactive({ title: '', content: '', coverUrl: '' })
const publishingArticle = ref(false)
const deletingArticleId = ref(null)
const memberKeyword = ref('')
const roleFilter = ref('')
const expandedNoticeId = ref(null)
const joining = ref(false)
const patchingMemberDisplayName = ref(false)
const posting = ref(false)
const moderatingMemberId = ref(null)
const roleChangingMemberId = ref(null)
const message = ref('')
const messageType = ref('success')

const loading = reactive({ detail: false, members: false, posts: false, notices: false, articles: false, checkin: false, tasks: false, activities: false, polls: false })
const errors = reactive({ detail: '', members: '', posts: '', notices: '', articles: '' })

// 打卡
const checkinSummary = reactive({ checkedInToday: false, todayCount: 0, myStreakDays: 0, recentCheckins: [] })
const checkinForm = reactive({ type: '', content: '' })
const checkingIn = ref(false)
const rankingTab = ref('daily')
const rankingsPayload = reactive({ items: [], currentUser: null })
const loadingRankings = ref(false)
const expandedCheckinId = ref(null)
const checkinCommentsById = reactive({})
const checkinCommentsLoading = reactive({})
const checkinCommentDraft = reactive({})
const checkinCommentPosting = ref(false)
const checkinLikeBusyId = ref(null)
/** 某条打卡下是否展开全部评论（否则只展示前 3 条） */
const checkinCommentsShowAll = reactive({})

// 任务
const todayTasks = ref([])
const claimingTask = ref('')

// 活动
const activities = ref([])
const upcomingActivities = ref([])
const showCreateActivity = ref(false)
const showProposalDialog = ref(false)
const activityForm = reactive({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  location: '',
  maxParticipants: 0,
  platformEventId: ''
})
const platformEvents = ref([])
const creatingActivity = ref(false)
const signingActivityId = ref(null)
const cancellingActivityId = ref(null)
const checkinCodeBusyId = ref(null)
const activityCheckinOpen = ref(false)
const activityCheckinCode = ref('')
const activityCheckinTarget = ref(null)
const activityCheckinSubmitting = ref(false)
const activityReviewOpen = ref(false)
const activityReviewTarget = ref(null)
const activityReviewCandidates = ref([])
const activityReviewLoading = ref(false)
const activityReviewRatings = reactive({})
const weeklyDigest = ref(null)
const sendingDigest = ref(false)
const reportDialogOpen = ref(false)
const reportPostId = ref(null)
const reportTargetUserId = ref(null)
const polls = ref([])
const seasonRank = ref(null)
const seasonTop = ref([])
const showPollCreate = ref(false)
const creatingPoll = ref(false)
const pollForm = reactive({
  title: '',
  description: '',
  selectionMode: 'single',
  maxChoices: 2,
  startTime: '',
  endTime: '',
  optionsText: ''
})
const expandedPollId = ref(null)
const pollDetailCache = reactive({})
const pollPick = reactive({})
const votingPollId = ref(null)
const revealingPollId = ref(null)
const postForm = reactive({ content: '', imageUrls: '' })
const quickPostContent = ref('')
const quickPosting = ref(false)
const expandedPostId = ref(null)
const feedComments = reactive({})
const commentsLoading = reactive({})
const commentDraft = reactive({})
const commentPosting = reactive({})
const likingPostId = ref(null)
const deletingPostId = ref(null)
const deletingCommentKey = ref('')

const activeTab = computed(() => {
  if (route.path.endsWith('/posts')) return 'posts'
  if (route.path.endsWith('/members')) return 'members'
  if (route.path.endsWith('/notices')) return 'notices'
  if (route.path.endsWith('/articles')) return 'articles'
  if (route.path.endsWith('/profile')) return 'profile'
  if (route.path.endsWith('/checkin')) return 'checkin'
  if (route.path.endsWith('/tasks')) return 'tasks'
  if (route.path.endsWith('/activities')) return 'activities'
  if (route.path.endsWith('/polls')) return 'polls'
  return 'home'
})

const tabs = computed(() => {
  const id = group.value?.id || route.params.id
  const rows = [
    { key: 'home', label: '首页', to: groupTabPath('home') },
    { key: 'posts', label: '动态', to: groupTabPath('posts') },
    { key: 'checkin', label: '打卡', to: groupTabPath('checkin') },
    { key: 'tasks', label: '任务', to: groupTabPath('tasks') },
    { key: 'activities', label: '活动', to: groupTabPath('activities') }
  ]
  if (!isLegacyPlatformGroupId(id)) {
    rows.push({ key: 'polls', label: '投票', to: groupTabPath('polls') })
  }
  rows.push(
    { key: 'members', label: '成员', to: groupTabPath('members') },
    { key: 'articles', label: '文章', to: groupTabPath('articles') },
    { key: 'notices', label: '公告', to: groupTabPath('notices') },
    { key: 'profile', label: '资料', to: groupTabPath('profile') }
  )
  return rows
})

const isPollsSupported = computed(() => Boolean(group.value?.id && !isLegacyPlatformGroupId(group.value.id)))

const heroImage = computed(() => group.value?.coverUrl || DEFAULT_COVER)
const latestNotice = computed(() => notices.value[0] || group.value?.latestNotice || null)
const joinModeKey = computed(() => {
  const g = group.value
  if (!g) return 'audit'
  return g.joinModeKey || (g.joinMode === 'free' ? 'open' : g.joinMode === 'invite' ? 'invite' : 'audit')
})

const inviteCodeFromQuery = computed(() => String(route.query.invite || '').trim().toUpperCase())

const pendingMemberCount = computed(() => Number(group.value?.pendingMemberCount || 0))
const myRoleBadge = computed(() => {
  const g = group.value
  if (!userStore.isLoggedIn) return { label: '未登录', tone: 'neutral' }
  if (g?.isOwner) return { label: 'Owner', tone: 'info' }
  if (g?.managed || g?.canReviewJoins) return { label: '管理员', tone: 'info' }
  if (g?.isMember) return { label: '已加入', tone: 'success' }
  if (g?.hasPendingRequest) return { label: '待审核', tone: 'warning' }
  return { label: '可申请', tone: 'info' }
})
const activeStateLabel = computed(() => {
  if (posts.value.length || Number(group.value?.postCount || 0) > 0) return '近期活跃'
  if (notices.value.length || activities.value.length) return '运营中'
  return '等待更新'
})
const detailStats = computed(() => [
  { label: '成员数', value: group.value?.memberCount ?? members.value.length, hint: 'Space 成员规模' },
  { label: '内容流', value: group.value?.postCount ?? posts.value.length, hint: '动态与讨论' },
  { label: '活动', value: activities.value.length, hint: '近期活动安排' },
  { label: '待处理', value: pendingMemberCount.value + claimableCount.value, hint: '审核与任务提醒' }
])
const detailSections = computed(() => [
  { key: 'notices', label: '公告', value: notices.value.length, hint: latestNotice.value ? '有最新公告' : '暂无公告', to: groupTabPath('notices') },
  { key: 'activities', label: '活动', value: activities.value.length, hint: upcomingActivities.value.length ? '有进行中活动' : '查看活动', to: groupTabPath('activities') },
  { key: 'members', label: '成员动态', value: members.value.length, hint: pendingMemberCount.value ? '有待审核成员' : '成员列表', to: groupTabPath('members') },
  { key: 'tasks', label: '打卡/任务', value: todayTasks.value.length, hint: claimableCount.value ? '有奖励待领取' : '今日任务', to: groupTabPath('tasks') },
  { key: 'posts', label: '内容流', value: posts.value.length, hint: posts.value.length ? '最近讨论' : '暂无动态', to: groupTabPath('posts') }
])

const joinDisabled = computed(() => {
  if (!group.value) return true
  if (group.value.isMember || group.value.hasPendingRequest) return true
  if (joinModeKey.value === 'invite') return !inviteCodeFromQuery.value
  return false
})

const joinButtonText = computed(() => {
  if (!userStore.isLoggedIn) return '登录后加入'
  if (group.value?.isMember) return '已加入'
  if (group.value?.hasPendingRequest) return '审核中'
  if (joinModeKey.value === 'invite') {
    return inviteCodeFromQuery.value ? '验证邀请并加入' : '需要邀请码'
  }
  if (joinModeKey.value === 'open') return '加入团体'
  return '申请加入'
})

const admins = computed(() => normalizeAdmins(group.value?.admins || []))
const posts = computed(() => rawPosts.value.map(normalizePost))
const notices = computed(() => rawNotices.value.map(normalizeNotice))
const articles = computed(() => rawArticles.value.map(normalizeArticle))
const members = computed(() => rawMembers.value.map(normalizeMember))

const filteredMembers = computed(() => {
  const keyword = memberKeyword.value.toLowerCase()
  return members.value.filter((member) => {
    const byKeyword = !keyword || member.name.toLowerCase().includes(keyword)
    const byRole = !roleFilter.value || member.role === roleFilter.value
    return byKeyword && byRole
  })
})

const emptyPostsHint = computed(() => {
  if (group.value?.isMember) return '这个团体还没有动态，来发布第一条吧。'
  return '这个团体还没有动态。'
})

const claimableCount = computed(() => todayTasks.value.filter((t) => t.completed && !t.claimed).length)
const incompleteTasks = computed(() => todayTasks.value.filter((t) => !t.completed))

const rankingEmptyHint = computed(() => {
  if (rankingTab.value !== 'daily') return ''
  if (Number(checkinSummary.todayCount || 0) > 0) return ''
  return '今天还没有人打卡，来成为第一个吧'
})

const rankingDisplayRows = computed(() => {
  const items = Array.isArray(rankingsPayload.items) ? rankingsPayload.items : []
  const me = rankingsPayload.currentUser
  if (!me || me.rank == null) return items
  const inTop = items.some((r) => r.isCurrentUser)
  if (inTop) return items
  return [...items, { ...me, __appendedMe: true }]
})

async function loadDetail() {
  loading.detail = true
  errors.detail = ''
  try {
    const data = await fetchGroupDetail(route.params.id)
    group.value = normalizeGroup(data)
    if (userStore.token && (group.value?.managed || group.value?.canReviewJoins)) {
      await userStore.loadAdminContext().catch(() => null)
    }
  } catch (error) {
    group.value = null
    errors.detail = error.message || '无法连接 /api/platform/groups/{id}'
  } finally {
    loading.detail = false
  }
}

function mapJoinRequestToMemberRow(r) {
  const real = r.memberRealName && String(r.memberRealName).trim()
  return {
    joinRequestId: r.id,
    id: `jr-${r.id}`,
    userId: r.userId,
    name: real || r.username || '申请者',
    avatar: r.avatarUrl || DEFAULT_AVATAR,
    role: 'member',
    roleLabel: '待入团',
    status: 'pending',
    statusLabel: '申请中',
    joinedAt: formatDate(r.requestedAt),
    applyReason: r.message || '',
    isJoinRequest: true
  }
}

async function loadMembers() {
  loading.members = true
  errors.members = ''
  try {
    const gid = group.value.id
    const legacy = isLegacyPlatformGroupId(gid)
    const canSeePending = Boolean(group.value?.managed || group.value?.canReviewJoins)
    const status = legacy && canSeePending ? 'all' : 'approved'
    const members = unwrapList(await fetchGroupMembers(gid, { status }))
    let pendingRows = []
    if (!legacy && canSeePending && joinModeKey.value === 'audit') {
      try {
        const reqs = await getAdminGroupJoinRequests(gid, 'pending')
        const list = Array.isArray(reqs) ? reqs : unwrapList(reqs)
        pendingRows = list.map(mapJoinRequestToMemberRow)
      } catch {
        pendingRows = []
      }
    }
    const approvedIds = new Set(
      members.filter((m) => (m.status || 'approved') === 'approved').map((m) => Number(m.userId))
    )
    pendingRows = pendingRows.filter((p) => !approvedIds.has(Number(p.userId)))
    rawMembers.value = [...pendingRows, ...members]
  } catch (error) {
    rawMembers.value = []
    errors.members = error.message || '成员接口加载失败'
  } finally {
    loading.members = false
  }
}

async function loadPosts() {
  loading.posts = true
  errors.posts = ''
  try {
    const res = await fetchGroupPosts(group.value.id, { page: 1, size: 80 })
    rawPosts.value = unwrapGroupPostsList(res)
  } catch (error) {
    rawPosts.value = []
    errors.posts = error.message || '动态接口加载失败'
  } finally {
    loading.posts = false
  }
}

async function loadNotices() {
  loading.notices = true
  errors.notices = ''
  try {
    rawNotices.value = unwrapList(await fetchGroupNotices(group.value.id))
  } catch (error) {
    rawNotices.value = []
    errors.notices = error.message || '公告接口加载失败'
  } finally {
    loading.notices = false
  }
}

async function loadArticles() {
  loading.articles = true
  errors.articles = ''
  try {
    rawArticles.value = unwrapList(await fetchGroupArticles(group.value.id))
  } catch (error) {
    rawArticles.value = []
    errors.articles = error.message || '文章接口加载失败'
  } finally {
    loading.articles = false
  }
}

async function submitArticle() {
  const title = articleForm.title.trim()
  const content = articleForm.content.trim()
  if (!title || !content) {
    flashMessage('请填写文章标题和正文', 'error')
    return
  }
  const checkResult = await contentCheck.check(`${title}\n${content}`, 'group-article')
  if (!checkResult.ok) return
  publishingArticle.value = true
  try {
    await createGroupArticle(group.value.id, {
      title,
      content,
      coverUrl: articleForm.coverUrl.trim() || undefined
    })
    articleForm.title = ''
    articleForm.content = ''
    articleForm.coverUrl = ''
    await loadArticles()
    flashMessage('文章已发布')
  } catch (error) {
    flashMessage(error.message || '文章发布失败', 'error')
  } finally {
    publishingArticle.value = false
  }
}

function canDeleteArticle(item) {
  if (!item) return false
  if (group.value?.managed || group.value?.isOwner) return true
  return Number(item.authorUserId) === currentUserIdNum.value
}

async function removeArticle(item) {
  if (!canDeleteArticle(item)) return
  if (!window.confirm(`确定删除文章「${item.title}」？`)) return
  deletingArticleId.value = item.id
  try {
    await deleteGroupArticle(group.value.id, item.id)
    await loadArticles()
    flashMessage('文章已删除')
  } catch (error) {
    flashMessage(error.message || '删除失败', 'error')
  } finally {
    deletingArticleId.value = null
  }
}

function toggleArticle(id) {
  expandedArticleId.value = expandedArticleId.value === id ? null : id
}

async function loadRelatedData() {
  if (!group.value?.id) return
  await Promise.all([
    loadMembers(),
    loadPosts(),
    loadNotices(),
    loadArticles(),
    loadActivitiesForHome(),
    loadCheckinSummary(),
    loadTodayTasks(),
    loadSeasonInfo(),
    loadWeeklyDigest()
  ])
}

async function loadSeasonInfo() {
  if (!group.value?.id || !isLegacyPlatformGroupId(group.value.id)) return
  try {
    const [rankRes, topRes] = await Promise.all([
      fetchGroupSeasonRank(group.value.id),
      fetchGroupSeasonRankings({ page: 1, size: 5 })
    ])
    seasonRank.value = rankRes
      ? { ...rankRes, scoringRules: rankRes.scoringRules || topRes?.scoringRules }
      : null
    seasonTop.value = Array.isArray(topRes?.items) ? topRes.items : []
  } catch {
    seasonRank.value = null
    seasonTop.value = []
  }
}

async function loadCheckinSummary() {
  if (!group.value?.id) return
  loading.checkin = true
  try {
    const data = await fetchCheckinSummary(group.value.id)
    Object.assign(checkinSummary, {
      checkedInToday: Boolean(data?.checkedInToday),
      todayCount: Number(data?.todayCount ?? 0),
      myStreakDays: Number(data?.myStreakDays ?? 0),
      recentCheckins: Array.isArray(data?.recentCheckins) ? data.recentCheckins : []
    })
  } catch {
    /* silent */
  } finally {
    loading.checkin = false
  }
}

async function loadRankings() {
  if (!group.value?.id || !userStore.isLoggedIn || !group.value.isMember) return
  loadingRankings.value = true
  try {
    const res = await fetchCheckinRankings(group.value.id, rankingTab.value)
    rankingsPayload.items = Array.isArray(res?.items) ? res.items : []
    rankingsPayload.currentUser = res?.currentUser && Object.keys(res.currentUser).length ? res.currentUser : null
  } catch {
    rankingsPayload.items = []
    rankingsPayload.currentUser = null
  } finally {
    loadingRankings.value = false
  }
}

function setRankingTab(tab) {
  if (rankingTab.value === tab) return
  rankingTab.value = tab
  loadRankings()
}

function rankMedal(index) {
  if (index === 0) return '🥇'
  if (index === 1) return '🥈'
  if (index === 2) return '🥉'
  return String(index + 1)
}

function rankSlotDisplay(row, idx) {
  if (!row.__appendedMe && idx < 3) return rankMedal(idx)
  return String(row.rank ?? idx + 1)
}

function rankingRowClasses(row, idx) {
  const parts = ['ranking-row']
  if (row.isCurrentUser || row.__appendedMe) parts.push('ranking-row--me')
  if (idx < 3 && !row.__appendedMe) parts.push('ranking-row--top3')
  else if (!row.isCurrentUser && !row.__appendedMe) parts.push('ranking-row--plain')
  if (row.__appendedMe) parts.push('ranking-row--appended')
  return parts.join(' ')
}

function visibleCommentsFor(checkinId) {
  const all = checkinCommentsById[checkinId] || []
  if (checkinCommentsShowAll[checkinId]) return all
  return all.slice(0, 3)
}

function hasMoreComments(checkinId) {
  const all = checkinCommentsById[checkinId] || []
  return all.length > 3 && !checkinCommentsShowAll[checkinId]
}

function showAllCommentsFor(checkinId) {
  checkinCommentsShowAll[checkinId] = true
}

async function toggleLikeCheckin(c) {
  if (!userStore.isLoggedIn || !group.value?.isMember || checkinLikeBusyId.value) return
  const id = c.id
  checkinLikeBusyId.value = id
  try {
    if (c.likedByCurrentUser) {
      const res = await unlikePlatformCheckin(id)
      c.likedByCurrentUser = false
      c.likeCount = Number(res?.likeCount ?? 0)
    } else {
      const res = await likePlatformCheckin(id)
      c.likedByCurrentUser = true
      c.likeCount = Number(res?.likeCount ?? 0)
    }
  } catch (err) {
    flashMessage(err.message || '操作失败', 'error')
  } finally {
    checkinLikeBusyId.value = null
  }
}

function toggleCommentsBlock(c) {
  const id = c.id
  if (expandedCheckinId.value === id) {
    expandedCheckinId.value = null
    return
  }
  expandedCheckinId.value = id
  checkinCommentsShowAll[id] = false
  if (!checkinCommentsById[id]) loadCheckinCommentsList(id)
}

async function loadCheckinCommentsList(checkinId) {
  checkinCommentsLoading[checkinId] = true
  try {
    const res = await fetchPlatformCheckinComments(checkinId, { page: 1, size: 50 })
    checkinCommentsById[checkinId] = Array.isArray(res?.items) ? res.items : []
  } catch {
    checkinCommentsById[checkinId] = []
  } finally {
    checkinCommentsLoading[checkinId] = false
  }
}

async function submitCheckinComment(c) {
  const id = c.id
  const text = String(checkinCommentDraft[id] || '').trim()
  if (!text || checkinCommentPosting.value) return
  checkinCommentPosting.value = true
  try {
    await createPlatformCheckinComment(id, { content: text })
    checkinCommentDraft[id] = ''
    c.commentCount = Number(c.commentCount || 0) + 1
    const wasAll = Boolean(checkinCommentsShowAll[id])
    await loadCheckinCommentsList(id)
    if (wasAll) checkinCommentsShowAll[id] = true
  } catch (err) {
    flashMessage(err.message || '评论失败', 'error')
  } finally {
    checkinCommentPosting.value = false
  }
}

async function deleteMyCheckinComment(checkinId, commentId) {
  try {
    await deletePlatformCheckinComment(commentId)
    const list = checkinCommentsById[checkinId] || []
    const idx = list.findIndex((x) => x.id === commentId)
    if (idx >= 0) list.splice(idx, 1)
    const c = checkinSummary.recentCheckins?.find((x) => x.id === checkinId)
    if (c) c.commentCount = Math.max(0, Number(c.commentCount || 0) - 1)
  } catch (err) {
    flashMessage(err.message || '删除失败', 'error')
  }
}

async function loadTodayTasks() {
  if (!group.value?.id || !userStore.isLoggedIn) return
  loading.tasks = true
  try {
    const data = await fetchTodayTasks(group.value.id)
    todayTasks.value = Array.isArray(data) ? data : []
  } catch {
    todayTasks.value = []
  } finally {
    loading.tasks = false
  }
}

async function loadActivities() {
  if (!group.value?.id) return
  loading.activities = true
  try {
    const res = await fetchGroupActivities(group.value.id, { page: 1, size: 50 })
    activities.value = Array.isArray(res?.items) ? res.items : (Array.isArray(res) ? res : [])
  } catch {
    activities.value = []
  } finally {
    loading.activities = false
  }
}

async function loadActivitiesForHome() {
  if (!group.value?.id) return
  try {
    const res = await fetchGroupActivities(group.value.id, { filter: 'upcoming', page: 1, size: 3 })
    upcomingActivities.value = Array.isArray(res?.items) ? res.items : (Array.isArray(res) ? res : [])
  } catch {
    upcomingActivities.value = []
  }
}

async function submitCheckin() {
  if (!checkinForm.type || checkingIn.value) return
  checkingIn.value = true
  try {
    await createCheckin(group.value.id, { checkinType: checkinForm.type, content: checkinForm.content })
    checkinForm.content = ''
    checkinForm.type = ''
    await loadCheckinSummary()
    await loadRankings()
    await loadTodayTasks()
    flashMessage('打卡成功！')
  } catch (err) {
    flashMessage(err.message || '打卡失败', 'error')
  } finally {
    checkingIn.value = false
  }
}

async function claimTask(task) {
  if (claimingTask.value) return
  claimingTask.value = task.taskCode
  try {
    const res = await claimGroupTask(group.value.id, task.taskCode)
    await loadTodayTasks()
    flashMessage(res?.message || `已领取 +${task.rewardExp} EXP`)
  } catch (err) {
    flashMessage(err.message || '领取失败', 'error')
  } finally {
    claimingTask.value = ''
  }
}

async function loadPlatformEvents() {
  try {
    const res = await fetchEvents({ status: 'published' })
    platformEvents.value = Array.isArray(res) ? res.slice(0, 30) : []
  } catch {
    platformEvents.value = []
  }
}

async function loadWeeklyDigest() {
  if (!group.value?.managed || !isLegacyPlatformGroupId(group.value?.id)) {
    weeklyDigest.value = null
    return
  }
  try {
    weeklyDigest.value = await fetchGroupWeeklyDigest(group.value.id)
  } catch {
    weeklyDigest.value = null
  }
}

async function sendWeeklyDigestNow() {
  if (sendingDigest.value || !group.value?.id) return
  sendingDigest.value = true
  try {
    await sendGroupWeeklyDigest(group.value.id)
    flashMessage('周报已发送至消息中心')
  } catch (err) {
    flashMessage(err.message || '发送失败', 'error')
  } finally {
    sendingDigest.value = false
  }
}

function openPostReport(post) {
  reportPostId.value = post.id
  reportTargetUserId.value = post.userId || null
  reportDialogOpen.value = true
}

function openActivityCheckin(act) {
  activityCheckinTarget.value = act
  activityCheckinCode.value = ''
  activityCheckinOpen.value = true
}

async function submitActivityCheckin() {
  if (!activityCheckinTarget.value || activityCheckinSubmitting.value) return
  activityCheckinSubmitting.value = true
  try {
    await checkinGroupActivity(group.value.id, activityCheckinTarget.value.id, activityCheckinCode.value)
    activityCheckinOpen.value = false
    await loadActivities()
    flashMessage('签到成功')
  } catch (err) {
    flashMessage(err.message || '签到失败', 'error')
  } finally {
    activityCheckinSubmitting.value = false
  }
}

async function generateActivityCode(act) {
  if (checkinCodeBusyId.value) return
  checkinCodeBusyId.value = act.id
  try {
    const res = await generateGroupActivityCheckinCode(group.value.id, act.id)
    flashMessage(`签到码：${res.checkinCode || '已生成'}（请告知到场成员）`)
    await loadActivities()
  } catch (err) {
    flashMessage(err.message || '生成失败', 'error')
  } finally {
    checkinCodeBusyId.value = null
  }
}

async function openActivityReview(act) {
  activityReviewTarget.value = act
  activityReviewOpen.value = true
  activityReviewLoading.value = true
  try {
    activityReviewCandidates.value = await fetchGroupActivityReviewCandidates(group.value.id, act.id)
  } catch (err) {
    activityReviewCandidates.value = []
    flashMessage(err.message || '加载失败', 'error')
  } finally {
    activityReviewLoading.value = false
  }
}

function closeActivityReview() {
  activityReviewOpen.value = false
  activityReviewTarget.value = null
  activityReviewCandidates.value = []
}

async function submitOneReview(candidate) {
  const rating = activityReviewRatings[candidate.userId] || 5
  try {
    await submitGroupActivityReview(group.value.id, activityReviewTarget.value.id, {
      targetUserId: candidate.userId,
      rating,
      comment: ''
    })
    candidate.reviewed = true
    await loadActivities()
    flashMessage('评价已提交')
  } catch (err) {
    flashMessage(err.message || '提交失败', 'error')
  }
}

async function submitActivity() {
  if (creatingActivity.value) return
  creatingActivity.value = true
  try {
    const payload = {
      title: activityForm.title,
      description: activityForm.description,
      startTime: activityForm.startTime ? activityForm.startTime + ':00' : '',
      endTime: activityForm.endTime ? activityForm.endTime + ':00' : '',
      location: activityForm.location,
      maxParticipants: activityForm.maxParticipants
    }
    if (activityForm.platformEventId) {
      payload.platformEventId = activityForm.platformEventId
    }
    await createGroupActivity(group.value.id, payload)
    showCreateActivity.value = false
    Object.assign(activityForm, {
      title: '',
      description: '',
      startTime: '',
      endTime: '',
      location: '',
      maxParticipants: 0,
      platformEventId: ''
    })
    await loadActivities()
    await loadActivitiesForHome()
    flashMessage('活动已发布')
  } catch (err) {
    flashMessage(err.message || '发布失败', 'error')
  } finally {
    creatingActivity.value = false
  }
}

async function loadPolls() {
  if (!group.value?.id) return
  if (!isPollsSupported.value) {
    polls.value = []
    return
  }
  loading.polls = true
  try {
    const res = await fetchGroupPolls(group.value.id, { page: 1, size: 50 })
    polls.value = Array.isArray(res?.items) ? res.items : []
  } catch {
    polls.value = []
  } finally {
    loading.polls = false
  }
}

async function refreshPollDetail(id) {
  if (!group.value?.id || isLegacyPlatformGroupId(group.value.id)) return
  const d = await fetchGroupPoll(group.value.id, id)
  pollDetailCache[id] = d
  pollPick[id] = Array.isArray(d.mySelections) ? [...d.mySelections] : []
}

async function togglePollExpand(id) {
  if (expandedPollId.value === id) {
    expandedPollId.value = null
    return
  }
  expandedPollId.value = id
  try {
    await refreshPollDetail(id)
  } catch (e) {
    flashMessage(e.message || '加载失败', 'error')
    expandedPollId.value = null
  }
}

function setPollSinglePick(pollId, optId) {
  pollPick[pollId] = [optId]
}

function togglePollMultiPick(pollId, optId, detail, checked) {
  const max = Number(detail.maxChoices || 2)
  let cur = [...(pollPick[pollId] || [])]
  if (checked) {
    if (!cur.includes(optId) && cur.length < max) {
      cur.push(optId)
    }
  } else {
    cur = cur.filter((x) => x !== optId)
  }
  pollPick[pollId] = cur
}

async function submitPollCreate() {
  if (!group.value?.id || creatingPoll.value) return
  creatingPoll.value = true
  try {
    const lines = pollForm.optionsText.split(/\r?\n/).map((s) => s.trim()).filter(Boolean)
    if (lines.length < 2) {
      flashMessage('请至少填写两个选项', 'error')
      return
    }
    const payload = {
      title: pollForm.title,
      description: pollForm.description,
      selectionMode: pollForm.selectionMode,
      startTime: pollForm.startTime ? `${pollForm.startTime}:00` : '',
      endTime: pollForm.endTime ? `${pollForm.endTime}:00` : '',
      options: lines
    }
    if (pollForm.selectionMode === 'multiple') {
      payload.maxChoices = pollForm.maxChoices
    }
    await createGroupPoll(group.value.id, payload)
    showPollCreate.value = false
    Object.assign(pollForm, {
      title: '',
      description: '',
      selectionMode: 'single',
      maxChoices: 2,
      startTime: '',
      endTime: '',
      optionsText: ''
    })
    expandedPollId.value = null
    for (const k of Object.keys(pollDetailCache)) {
      delete pollDetailCache[k]
    }
    await loadPolls()
    flashMessage('投票已发布')
  } catch (err) {
    flashMessage(err.message || '发布失败', 'error')
  } finally {
    creatingPoll.value = false
  }
}

async function submitPollVote(pollId) {
  const pick = pollPick[pollId] || []
  if (!pick.length) {
    flashMessage('请选择选项', 'error')
    return
  }
  if (votingPollId.value) return
  votingPollId.value = pollId
  try {
    await submitGroupPollVotes(group.value.id, pollId, { optionIds: pick })
    await refreshPollDetail(pollId)
    await loadPolls()
    flashMessage('投票已提交')
  } catch (e) {
    flashMessage(e.message || '提交失败', 'error')
  } finally {
    votingPollId.value = null
  }
}

async function revealPollResults(pollId) {
  if (revealingPollId.value) return
  revealingPollId.value = pollId
  try {
    await revealGroupPollResults(group.value.id, pollId)
    await refreshPollDetail(pollId)
    await loadPolls()
    flashMessage('已向全体成员展示票数')
  } catch (e) {
    flashMessage(e.message || '操作失败', 'error')
  } finally {
    revealingPollId.value = null
  }
}

async function signUp(act) {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  signingActivityId.value = act.id
  try {
    await signUpGroupActivity(group.value.id, act.id)
    await loadActivities()
    flashMessage('报名成功')
  } catch (err) {
    flashMessage(err.message || '报名失败', 'error')
  } finally {
    signingActivityId.value = null
  }
}

async function cancelSignUp(act) {
  signingActivityId.value = act.id
  try {
    await cancelGroupActivitySignup(group.value.id, act.id)
    await loadActivities()
    flashMessage('已取消报名')
  } catch (err) {
    flashMessage(err.message || '取消失败', 'error')
  } finally {
    signingActivityId.value = null
  }
}

async function cancelActivity(act) {
  if (!window.confirm(`确定取消活动「${act.title}」？取消后无法恢复。`)) return
  cancellingActivityId.value = act.id
  try {
    await updateGroupActivity(group.value.id, act.id, { status: 'cancelled' })
    await loadActivities()
    flashMessage('活动已取消')
  } catch (err) {
    flashMessage(err.message || '操作失败', 'error')
  } finally {
    cancellingActivityId.value = null
  }
}

function formatActivityTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function activityStatusLabel(act) {
  if (act.status === 'cancelled') return '已取消'
  if (act.isEnded) return '已结束'
  const now = Date.now()
  const start = act.startTime ? new Date(act.startTime).getTime() : 0
  if (start > now) return '即将开始'
  return '进行中'
}

function activityStatusClass(act) {
  if (act.status === 'cancelled') return 'cancelled'
  if (act.isEnded) return 'ended'
  return 'published'
}

async function applyJoin() {
  if (joinDisabled.value || joining.value) return
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(route.fullPath.replace(/^#/, '') || groupsPath(String(route.params.id)))
    router.push('/login')
    return
  }
  if (joinModeKey.value === 'invite' && !inviteCodeFromQuery.value) return
  const nameInput = window.prompt(JOIN_MEMBER_REAL_NAME_PROMPT, '')
  if (nameInput === null) return
  const memberRealName = String(nameInput).trim()
  if (!memberRealName) {
    flashMessage(ERR_EMPTY_MEMBER_REAL_NAME, 'error')
    return
  }
  let applyMessage = ''
  if (joinModeKey.value === 'audit') {
    const input = window.prompt(AUDIT_JOIN_MESSAGE_PROMPT, '')
    if (input === null) return
    applyMessage = input.trim()
    if (!applyMessage) {
      flashMessage(ERR_EMPTY_AUDIT_JOIN_MESSAGE, 'error')
      return
    }
  }
  joining.value = true
  try {
    const res = await joinGroup(group.value.id, {
      message: applyMessage,
      memberRealName,
      inviteCode: inviteCodeFromQuery.value
    })
    group.value.isMember = Boolean(res?.joined)
    group.value.hasPendingRequest = Boolean(res?.pending)
    await loadDetail()
    await loadRelatedData()
    flashMessage(res?.message || '申请已提交')
  } catch (error) {
    flashMessage(error.message || '申请加入失败', 'error')
  } finally {
    joining.value = false
  }
}

async function openMemberDisplayNamePrompt(isUpdate) {
  if (!group.value?.isMember || !userStore.isLoggedIn || patchingMemberDisplayName.value) return
  const title = supplementMemberDisplayTitle(isUpdate)
  const def = group.value.myMemberRealName || ''
  const input = window.prompt(title, def)
  if (input === null) return
  const memberRealName = String(input).trim()
  if (!memberRealName) {
    flashMessage(ERR_EMPTY_DISPLAY_NAME_PATCH, 'error')
    return
  }
  patchingMemberDisplayName.value = true
  try {
    const res = await patchMyGroupMemberRealName(group.value.id, memberRealName)
    await loadDetail()
    await loadRelatedData()
    flashMessage(res?.message || '已保存')
  } catch (error) {
    flashMessage(error.message || '保存失败', 'error')
  } finally {
    patchingMemberDisplayName.value = false
  }
}

async function submitPost() {
  const text = postForm.content.trim()
  const imgs = postForm.imageUrls.trim()
  if (!text && !imgs) {
    flashMessage('请填写文字或图片链接', 'error')
    return
  }
  if (text) {
    const checkResult = await contentCheck.check(text, 'group-post')
    if (!checkResult.ok) return
    if (checkResult.suggestion) postForm.content = checkResult.suggestion
  }
  posting.value = true
  try {
    await createGroupPost(group.value.id, {
      content: postForm.content.trim(),
      imageUrls: postForm.imageUrls.trim() || undefined
    })
    postForm.content = ''
    postForm.imageUrls = ''
    await loadPosts()
    await loadDetail()
    flashMessage('动态发布成功')
  } catch (error) {
    flashMessage(error.message || '动态发布失败', 'error')
  } finally {
    posting.value = false
  }
}

async function submitQuickPost() {
  const text = quickPostContent.value.trim()
  if (!text || quickPosting.value) return
  const checkResult = await contentCheck.check(text, 'group-post')
  if (!checkResult.ok) return
  const finalText = checkResult.suggestion || text
  quickPosting.value = true
  try {
    await createGroupPost(group.value.id, { content: finalText })
    quickPostContent.value = ''
    await loadPosts()
    await loadDetail()
    flashMessage('动态发布成功')
  } catch (error) {
    flashMessage(error.message || '动态发布失败', 'error')
  } finally {
    quickPosting.value = false
  }
}

function unwrapCommentItems(res) {
  if (Array.isArray(res?.items)) return res.items
  if (Array.isArray(res?.data?.items)) return res.data.items
  if (Array.isArray(res)) return res
  return []
}

function commentKey(postId, commentId) {
  return `${postId}-${commentId}`
}

function canDeletePost(post) {
  if (!group.value) return false
  if (group.value.managed) return true
  return String(post.userId) === String(userStore.userId)
}

function canDeleteComment(c) {
  if (!group.value) return false
  if (group.value.managed) return true
  return String(c.userId) === String(userStore.userId)
}

async function togglePostLike(post) {
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(route.fullPath.replace(/^#/, '') || groupsPath(String(route.params.id)))
    router.push('/login')
    return
  }
  const row = rawPosts.value.find((p) => p.id === post.id)
  if (!row) return
  const prevLiked = Boolean(row.likedByMe)
  const prevCount = Number(row.likeCount ?? 0)
  row.likedByMe = !prevLiked
  row.likeCount = prevLiked ? Math.max(0, prevCount - 1) : prevCount + 1
  likingPostId.value = post.id
  try {
    const res = await toggleGroupPostLike(group.value.id, post.id)
    row.likedByMe = Boolean(res?.likedByMe)
    row.likeCount = Number(res?.likeCount ?? row.likeCount)
  } catch (error) {
    row.likedByMe = prevLiked
    row.likeCount = prevCount
    flashMessage(error.message || '点赞失败', 'error')
  } finally {
    likingPostId.value = null
  }
}

async function toggleCommentPanel(post) {
  if (expandedPostId.value === post.id) {
    expandedPostId.value = null
    return
  }
  expandedPostId.value = post.id
  if (!feedComments[post.id]) {
    await loadCommentsForPost(post.id)
  }
}

async function loadCommentsForPost(postId) {
  commentsLoading[postId] = true
  try {
    const res = await fetchGroupPostComments(group.value.id, postId, { page: 1, size: 50 })
    const items = unwrapCommentItems(res)
    feedComments[postId] = items.map(normalizeComment)
  } catch (error) {
    feedComments[postId] = []
    flashMessage(error.message || '评论加载失败', 'error')
  } finally {
    commentsLoading[postId] = false
  }
}

function normalizeComment(item) {
  return {
    id: item.id,
    userId: item.userId,
    author: item.authorName || '成员',
    avatar: item.authorAvatarUrl || item.authorAvatar || DEFAULT_AVATAR,
    content: item.content || '',
    time: formatDateTime(item.createdAt)
  }
}

async function submitComment(post) {
  const key = post.id
  const text = (commentDraft[key] || '').trim()
  if (!text) return
  const checkResult = await contentCheck.check(text, 'comment')
  if (!checkResult.ok) return
  const finalText = checkResult.suggestion || text
  commentPosting[key] = true
  try {
    await createGroupPostComment(group.value.id, post.id, { content: finalText })
    commentDraft[key] = ''
    await loadCommentsForPost(post.id)
    await loadPosts()
    flashMessage('评论已发布')
  } catch (error) {
    flashMessage(error.message || '评论失败', 'error')
  } finally {
    commentPosting[key] = false
  }
}

async function deletePost(post) {
  if (!canDeletePost(post) || deletingPostId.value) return
  if (!window.confirm('确定删除这条动态？')) return
  deletingPostId.value = post.id
  try {
    await deletePlatformGroupPost(group.value.id, post.id)
    expandedPostId.value = null
    delete feedComments[post.id]
    await loadPosts()
    await loadDetail()
    flashMessage('已删除动态')
  } catch (error) {
    flashMessage(error.message || '删除失败', 'error')
  } finally {
    deletingPostId.value = null
  }
}

async function deleteComment(post, c) {
  if (!canDeleteComment(c)) return
  const ck = commentKey(post.id, c.id)
  if (deletingCommentKey.value) return
  if (!window.confirm('删除这条评论？')) return
  deletingCommentKey.value = ck
  try {
    await deletePlatformGroupPostComment(group.value.id, post.id, c.id)
    await loadCommentsForPost(post.id)
    await loadPosts()
  } catch (error) {
    flashMessage(error.message || '删除失败', 'error')
  } finally {
    deletingCommentKey.value = ''
  }
}

function canAuditMember(member) {
  if (member.status !== 'pending') return false
  if (member.isJoinRequest) return Boolean(group.value?.canReviewJoins)
  return Boolean(group.value?.managed)
}

function canRemoveMember(member) {
  return (
    group.value?.managed &&
    member.status === 'approved' &&
    member.role !== 'owner'
  )
}

function memberRoleSlug(role) {
  return String(role ?? '').trim().toLowerCase()
}

function memberStatusSlug(status) {
  return String(status ?? '').trim().toLowerCase()
}

function memberStatusBadgeClass(member) {
  const status = memberStatusSlug(member?.status)
  if (status === 'approved') return 'success'
  if (status === 'pending') return 'warning'
  if (status === 'left') return 'neutral'
  return 'info'
}

/** 仅「团体成员表中 role=owner」且与后端 owner_user_id（缺省则 created_by）一致时可调角色 PATCH，对齐接口权限。 */
function isCurrentUserGroupOwnerAligned() {
  if (!group.value || !userStore.isLoggedIn) return false
  const uid = currentUserIdNum.value
  if (!uid) return false
  const myRow = rawMembers.value.find((m) => Number(m.userId) === uid)
  if (!myRow || memberRoleSlug(myRow.role) !== 'owner') return false

  const g = group.value
  const declared =
    g.ownerUserId !== null && g.ownerUserId !== undefined && `${g.ownerUserId}`.length > 0
      ? Number(g.ownerUserId)
      : null
  const fallback =
    g.createdBy !== null && g.createdBy !== undefined && `${g.createdBy}`.length > 0
      ? Number(g.createdBy)
      : null

  if (declared != null && Number.isFinite(declared) && declared !== uid) return false
  if (
    declared == null &&
    fallback != null &&
    Number.isFinite(fallback) &&
    fallback !== uid
  ) {
    return false
  }
  return true
}

function canOwnerToggleAdmin(member) {
  const mr = memberRoleSlug(member.role)
  const ms = memberStatusSlug(member.status)
  if (ms !== '' && ms !== 'approved') return false
  if (mr !== 'member' && mr !== 'admin') return false
  return isCurrentUserGroupOwnerAligned()
}

async function removeMember(member) {
  if (!canRemoveMember(member) || moderatingMemberId.value) return
  if (!window.confirm(`确定将「${member.name}」移出团体？`)) return
  moderatingMemberId.value = member.id
  try {
    await removeGroupMember(group.value.id, member.id)
    await loadDetail()
    await loadMembers()
    flashMessage('已移除成员')
  } catch (error) {
    flashMessage(error.message || '移除失败', 'error')
  } finally {
    moderatingMemberId.value = null
  }
}

async function approvePendingMember(member) {
  if (!canAuditMember(member) || moderatingMemberId.value) return
  moderatingMemberId.value = member.id
  try {
    const res = member.isJoinRequest
      ? await approveAdminGroupRequest(group.value.id, member.joinRequestId)
      : await approveMember(group.value.id, member.id)
    await loadDetail()
    await loadMembers()
    flashMessage(res?.message || '已通过申请')
  } catch (error) {
    flashMessage(error.message || '审核通过失败', 'error')
  } finally {
    moderatingMemberId.value = null
  }
}

async function rejectPendingMember(member) {
  if (!canAuditMember(member) || moderatingMemberId.value) return
  moderatingMemberId.value = member.id
  try {
    const res = member.isJoinRequest
      ? await rejectAdminGroupRequest(group.value.id, member.joinRequestId)
      : await rejectMember(group.value.id, member.id)
    await loadDetail()
    await loadMembers()
    flashMessage(res?.message || '已拒绝申请')
  } catch (error) {
    flashMessage(error.message || '拒绝申请失败', 'error')
  } finally {
    moderatingMemberId.value = null
  }
}

async function setMemberAsAdmin(member) {
  if (!canOwnerToggleAdmin(member) || member.role !== 'member' || roleChangingMemberId.value) return
  roleChangingMemberId.value = member.id
  try {
    await patchPlatformGroupMemberRole(group.value.id, member.id, { role: 'admin' })
    await loadDetail()
    await loadMembers()
    flashMessage('已设为管理员')
  } catch (error) {
    flashMessage(error.message || '设置失败', 'error')
  } finally {
    roleChangingMemberId.value = null
  }
}

async function unsetMemberAdmin(member) {
  if (!canOwnerToggleAdmin(member) || member.role !== 'admin' || roleChangingMemberId.value) return
  roleChangingMemberId.value = member.id
  try {
    await patchPlatformGroupMemberRole(group.value.id, member.id, { role: 'member' })
    await loadDetail()
    await loadMembers()
    flashMessage('已取消管理员')
  } catch (error) {
    flashMessage(error.message || '操作失败', 'error')
  } finally {
    roleChangingMemberId.value = null
  }
}

function toggleNotice(id) {
  expandedNoticeId.value = expandedNoticeId.value === id ? null : id
}

function goToTab(key) {
  router.push(groupTabPath(key))
}

function normalizeGroup(item) {
  const jm = item.joinMode
  const key = item.joinModeKey || (jm === 'free' ? 'open' : jm === 'invite' ? 'invite' : 'audit')
  const joinLabel =
    key === 'open' ? '公开加入' : key === 'invite' ? '仅限邀请' : '审核加入'
  return {
    id: item.id || route.params.id,
    name: item.name || '未命名团体',
    description: item.description || '暂无团体简介',
    coverUrl: item.coverUrl || '',
    category: item.category || item.typeName || item.type || '团体',
    region: item.location || item.region || '未设置地区',
    memberCount: Number(item.memberCount || 0),
    joinMode: jm || (key === 'open' ? 'free' : 'audit'),
    joinModeKey: key,
    joinLabel,
    ownerUserId: item.ownerUserId ?? null,
    createdBy: item.createdBy ?? null,
    isMember: Boolean(item.isMember),
    managed: Boolean(item.managed),
    canReviewJoins: Boolean(item.canReviewJoins),
    isOwner: Boolean(item.isOwner),
    hasPendingRequest: Boolean(item.hasPendingRequest),
    pendingMemberCount: Number(item.pendingMemberCount || 0),
    inviteJoinRequired: Boolean(item.inviteJoinRequired),
    createdDate: formatDate(item.createdAt),
    latestNotice: item.latestNotice ? normalizeNotice(item.latestNotice) : null,
    admins: item.admins || [],
    postCount: item.postCount != null ? Number(item.postCount) : null,
    myMemberRealName:
      item.myMemberRealName != null && String(item.myMemberRealName).trim() !== ''
        ? String(item.myMemberRealName).trim()
        : ''
  }
}

function normalizePost(item) {
  return {
    id: item.id,
    userId: item.userId,
    author: item.authorName || '团体成员',
    role: item.type === 'announcement' ? '公告' : '动态',
    time: formatDateTime(item.createdAt),
    content: item.content || '',
    likes: Number(item.likeCount || 0),
    comments: Number(item.commentCount || 0),
    likedByMe: Boolean(item.likedByMe),
    avatar: item.authorAvatarUrl || item.authorAvatar || DEFAULT_AVATAR,
    images: parseImages(item.imageUrls)
  }
}

function normalizeNotice(item) {
  return {
    id: item.id,
    title: item.title || '未命名公告',
    content: item.content || '',
    date: formatDate(item.createdAt)
  }
}

function normalizeArticle(item) {
  return {
    id: item.id,
    title: item.title || '未命名文章',
    summary: item.summary || '',
    content: item.content || '',
    coverUrl: item.coverUrl || '',
    authorUserId: item.authorUserId,
    author: item.authorName || '成员',
    date: formatDate(item.createdAt)
  }
}

function normalizeMember(item) {
  const role = item.role || 'member'
  const status = item.status || 'approved'
  const displayName =
    (item.memberRealName && String(item.memberRealName).trim()) ||
    item.username ||
    '未命名成员'
  return {
    id: item.id,
    userId: item.userId,
    name: displayName,
    avatar: item.avatarUrl || DEFAULT_AVATAR,
    role,
    roleLabel: role === 'owner' ? '团长' : role === 'admin' ? '管理员' : '成员',
    status,
    statusLabel: status === 'approved' ? '已加入' : status === 'pending' ? '申请中' : status === 'left' ? '已退出' : status,
    joinedAt: formatDate(item.joinedAt),
    applyReason: item.applyReason || ''
  }
}

function normalizeAdmins(items) {
  return items.map((item, index) => ({
    userId: item.userId || index,
    name: item.name || '管理员',
    avatar: item.avatar || DEFAULT_AVATAR,
    roleLabel: item.role === 'owner' ? '团长' : '管理员'
  }))
}

function parseImages(value) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  if (!value) return []
  const s = String(value).trim()
  if (s.startsWith('[')) {
    try {
      const parsed = JSON.parse(s)
      if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean)
    } catch {
      /* ignore */
    }
  }
  return s.split(',').map((item) => item.trim()).filter(Boolean)
}

function unwrapList(res) {
  if (Array.isArray(res)) return res
  if (Array.isArray(res?.data)) return res.data
  return []
}

function handleProposeActivity() {
  if (group.value?.managed) {
    flashMessage('你是运营者，可前往运营台直接发布活动。', 'error')
    return
  }
  if (!group.value?.isMember || !userStore.isLoggedIn) return
  showProposalDialog.value = true
}

function onProposalSubmitted() {
  flashMessage('活动投稿已提交，等待审核')
}

function flashMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
  window.setTimeout(() => {
    message.value = ''
  }, 2200)
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toISOString().slice(0, 10)
}

function formatDateTime(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const InfoList = defineComponent({
  props: { group: { type: Object, required: true }, compact: Boolean },
  setup(props) {
    return () => h('dl', { class: props.compact ? 'info-list compact' : 'info-list' }, [
      ['团体类型', props.group.category],
      ['所在地区', props.group.region],
      ['成员数量', `${props.group.memberCount} 人`],
      ['加入方式', props.group.joinLabel],
      ['创建时间', props.group.createdDate || '未记录']
    ].map(([key, value]) => h('div', [h('dt', key), h('dd', value)])))
  }
})

const PostList = defineComponent({
  props: { posts: { type: Array, required: true }, error: String, emptyText: String },
  setup(props) {
    return () => {
      if (props.error) return h('div', { class: 'inline-error' }, props.error)
      if (!props.posts.length) return h('div', { class: 'empty-inline' }, props.emptyText)
      return h('div', { class: 'post-list' }, props.posts.map(post => h('article', { class: 'post-card', key: post.id }, [
        h('div', { class: 'post-head' }, [
          h('img', { src: post.avatar, alt: '' }),
          h('div', [h('strong', post.author), h('span', post.time)]),
          h('em', post.role)
        ]),
        h('p', post.content),
        post.images.length ? h('div', { class: 'post-images' }, post.images.map(image => h('img', { src: image, alt: '', key: image }))) : null,
        h('div', { class: 'post-actions' }, [h('button', '赞 ' + post.likes), h('button', '评论 ' + post.comments)])
      ])))
    }
  }
})

watch(() => route.params.id, async () => {
  await loadDetail()
  await loadRelatedData()
})

watch(showCreateActivity, (open) => {
  if (open && !platformEvents.value.length) loadPlatformEvents()
})

watch(activeTab, async (tab) => {
  if (!group.value?.id) return
  if (tab === 'checkin') {
    await loadCheckinSummary()
    await loadRankings()
  } else if (tab === 'tasks') await loadTodayTasks()
  else if (tab === 'activities') await loadActivities()
  else if (tab === 'polls') await loadPolls()
})

onMounted(async () => {
  await loadDetail()
  await loadRelatedData()
  // 若初始直接落在新 tab
  if (activeTab.value === 'checkin') {
    await loadCheckinSummary()
    await loadRankings()
  } else if (activeTab.value === 'tasks') await loadTodayTasks()
  else if (activeTab.value === 'activities') await loadActivities()
  else if (activeTab.value === 'polls') await loadPolls()
})
</script>

<style scoped>
.group-detail-page {
  width: calc(100% - var(--lc-space-8));
  margin: var(--lc-space-4) auto 0;
  padding-bottom: var(--lc-space-12);
}

.loading-state,
.error-state {
  min-height: 420px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: var(--lc-space-3);
  color: var(--lc-muted);
}

.error-state h2 {
  margin: 0;
  color: var(--lc-text);
}

.error-state button,
.error-state a {
  height: 36px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  padding: 0 var(--lc-space-4);
  color: var(--lc-blue);
  background: var(--lc-surface);
  text-decoration: none;
  font-weight: 900;
}

.hero-card,
.tabs,
.panel-card,
.side-card {
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  box-shadow: var(--lc-shadow-sm);
}

.hero-card {
  overflow: hidden;
  border-radius: var(--lc-radius);
}

.group-detail-hero {
  overflow: visible;
  align-items: stretch;
}

.hero-cover {
  height: 220px;
  background-size: cover;
  background-position: center;
}

.hero-body {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr) auto;
  gap: var(--lc-space-5);
  padding: 0 var(--lc-space-6) var(--lc-space-6);
}

.group-avatar {
  width: 116px;
  height: 116px;
  margin-top: -42px;
  border: 4px solid var(--lc-surface);
  border-radius: var(--lc-radius-xs);
  object-fit: cover;
  box-shadow: var(--lc-shadow);
}

.group-detail-hero .group-avatar {
  width: 72px;
  height: 72px;
  margin-top: 0;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  box-shadow: none;
}

.hero-main {
  padding-top: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
  min-width: 0;
}

.title-row h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: 30px;
  line-height: 1.15;
}

.hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  margin-top: var(--lc-space-2);
}

.detail-hero-metrics {
  margin-top: 0;
}

.detail-hero-metrics .metric-card {
  min-height: 104px;
}

.post-head em,
.member-item em,
.member-item b {
  border-radius: 999px;
  padding: 3px var(--lc-space-2);
  color: var(--lc-blue);
  background: var(--lc-blue-light);
  font-size: var(--lc-text-xs);
  font-style: normal;
  font-weight: 900;
}

.meta-line,
.group-desc {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-base);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: var(--lc-space-2);
  padding-top: var(--lc-space-4);
  flex-wrap: wrap;
}

.join-btn,
.primary-btn,
.icon-btn {
  height: 36px;
  border-radius: var(--lc-radius-xs);
  font-weight: 900;
  cursor: pointer;
}

.primary-btn {
  border: 0;
  padding: 0 var(--lc-space-4);
  color: var(--lc-surface);
  background: linear-gradient(135deg, var(--lc-pink), var(--lc-blue));
  box-shadow: var(--lc-shadow-blue);
}

.primary-btn.small {
  height: 32px;
  font-size: var(--lc-text-sm);
}

.icon-btn {
  width: 36px;
  border: 1px solid var(--lc-border);
  color: var(--lc-muted);
  background: var(--lc-surface);
}

.join-btn {
  min-width: 96px;
  padding: 0 var(--lc-space-4);
}

.join-btn:disabled {
  color: var(--lc-blue);
  background: var(--lc-blue-light);
  cursor: default;
}

.manage-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}

.manage-link-admin {
  border-color: var(--lc-pink);
  color: var(--lc-pink);
  background: var(--lc-surface);
}

.remove-member-btn {
  height: 28px;
  padding: 0 var(--lc-space-2);
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-border);
  color: var(--lc-red);
  background: var(--lc-surface);
  font-size: var(--lc-text-xs);
  font-weight: 900;
  cursor: pointer;
}

.tabs {
  display: flex;
  gap: var(--lc-space-8);
  overflow-x: auto;
  border-top: 0;
  padding: 0 var(--lc-space-6);
  border-radius: 0 0 var(--lc-radius) var(--lc-radius);
  scrollbar-width: none;
}

.tabs::-webkit-scrollbar {
  display: none;
}

.tabs a {
  position: relative;
  flex: 0 0 auto;
  padding: var(--lc-space-4) 0;
  color: var(--lc-muted);
  text-decoration: none;
  font-size: var(--lc-text-base);
  font-weight: 900;
  white-space: nowrap;
}

.tabs a.active {
  color: var(--lc-blue);
}

.tabs a.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  border-radius: 999px;
  background: var(--lc-blue);
}

.detail-section-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--lc-space-3);
  margin-top: var(--lc-space-4);
}

.detail-section-item {
  display: grid;
  gap: var(--lc-space-1);
  min-width: 0;
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-muted);
  background: var(--lc-soft);
  text-decoration: none;
  font-weight: 900;
}

.detail-section-item strong {
  color: var(--lc-text);
  font-size: 24px;
  line-height: 1;
}

.detail-section-item em {
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
  font-style: normal;
}

.member-display-name-panel {
  margin: var(--lc-space-4) var(--lc-space-6) 0;
  padding: var(--lc-space-4) var(--lc-space-5);
  border-radius: var(--lc-radius);
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
}

.member-display-name-hint {
  margin: 0 0 var(--lc-space-3);
  font-size: var(--lc-text-sm);
  line-height: 1.5;
  color: var(--lc-muted);
}

.member-display-name-btn {
  padding: var(--lc-space-2) var(--lc-space-5);
  border: none;
  border-radius: var(--lc-radius-xs);
  font-size: var(--lc-text-sm);
  font-weight: 800;
  color: var(--lc-surface);
  background: var(--lc-blue);
  cursor: pointer;
}

.member-display-name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--lc-space-3);
  font-size: var(--lc-text-sm);
}

.member-display-name-label {
  color: var(--lc-text);
}

.member-display-name-link {
  padding: 0;
  border: none;
  background: none;
  color: var(--lc-blue);
  font-weight: 800;
  font-size: inherit;
  cursor: pointer;
  text-decoration: underline;
}

.member-display-name-link:disabled,
.member-display-name-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.page-message {
  margin-top: var(--lc-space-3);
  padding: var(--lc-space-2) var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-green);
  background: var(--lc-green-light);
  font-size: var(--lc-text-sm);
  font-weight: 900;
}

.page-message.error,
.inline-error {
  color: var(--lc-red);
  background: var(--lc-red-light);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--lc-space-5);
  margin-top: var(--lc-space-5);
}

.main-panel,
.side-panel,
.post-list,
.notice-list,
.member-list,
.home-grid {
  display: grid;
  gap: var(--lc-space-4);
  align-content: start;
}

.home-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.panel-card,
.side-card {
  border-radius: var(--lc-radius-sm);
  padding: var(--lc-space-5);
}

.panel-card h2,
.side-card h2 {
  margin: 0 0 var(--lc-space-3);
  color: var(--lc-text);
  font-size: var(--lc-text-md);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-3);
  margin-bottom: var(--lc-space-3);
}

.section-head h2 {
  margin: 0;
}

.section-head a,
.side-card a {
  color: var(--lc-blue);
  text-decoration: none;
  font-size: var(--lc-text-sm);
  font-weight: 900;
}

.notice-card,
.post-card,
.member-item {
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
}

.notice-card {
  padding: var(--lc-space-4);
}

.notice-card.compact p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notice-card button {
  display: flex;
  justify-content: space-between;
  gap: var(--lc-space-4);
  width: 100%;
  border: 0;
  padding: 0;
  color: var(--lc-text);
  background: transparent;
  font: inherit;
  font-weight: 900;
  text-align: left;
  cursor: pointer;
}

.notice-card strong {
  color: var(--lc-text);
}

.notice-card p,
.side-card p,
.activity-placeholder p {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  line-height: 1.7;
}

.notice-card time {
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
}

.article-full {
  display: grid;
  gap: var(--lc-space-3);
  margin-top: var(--lc-space-3);
}

.article-cover {
  width: 100%;
  max-height: 240px;
  object-fit: cover;
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-border);
}

.article-body {
  white-space: pre-wrap;
  color: var(--lc-text);
}

.empty-inline,
.inline-error {
  padding: var(--lc-space-4);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-muted);
  background: var(--lc-bg);
  font-weight: 800;
}

.inline-error {
  color: var(--lc-red);
}

.post-form {
  display: grid;
  gap: var(--lc-space-3);
  margin-bottom: var(--lc-space-4);
}

.post-form textarea,
.post-form input,
.member-tools input,
.member-tools select {
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-text);
  background: var(--lc-surface);
  font: inherit;
  font-weight: 700;
}

.post-form textarea {
  min-height: 96px;
  padding: var(--lc-space-3);
  resize: vertical;
}

.post-form input,
.member-tools input,
.member-tools select {
  height: 38px;
  padding: 0 var(--lc-space-3);
}

.post-form-foot,
.member-tools > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-3);
}

.post-form-foot span {
  color: var(--lc-subtle);
  font-size: var(--lc-text-sm);
  font-weight: 800;
}

.post-card {
  padding: var(--lc-space-4);
}

.post-head {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto auto;
  gap: var(--lc-space-3);
  align-items: center;
}

:deep(.post-list) {
  display: grid;
  gap: var(--lc-space-4);
}

:deep(.post-card) {
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  padding: var(--lc-space-4);
  background: var(--lc-bg);
}

:deep(.post-head) {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: var(--lc-space-3);
  align-items: center;
}

:deep(.post-head img) {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

:deep(.post-head strong) {
  display: block;
  color: var(--lc-text);
  font-size: var(--lc-text-base);
}

:deep(.post-head span) {
  display: block;
  margin-top: 2px;
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
}

:deep(.post-head em) {
  border-radius: 999px;
  padding: 3px var(--lc-space-2);
  color: var(--lc-blue);
  background: var(--lc-blue-light);
  font-size: var(--lc-text-xs);
  font-style: normal;
  font-weight: 900;
}

:deep(.post-card p) {
  margin: var(--lc-space-3) 0 0;
  color: var(--lc-muted);
  line-height: 1.75;
}

:deep(.post-images) {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--lc-space-2);
  margin-top: var(--lc-space-3);
}

:deep(.post-images img) {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: var(--lc-radius-xs);
  object-fit: cover;
}

:deep(.post-actions) {
  display: flex;
  gap: var(--lc-space-3);
  margin-top: var(--lc-space-3);
}

:deep(.post-actions button) {
  height: 30px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-muted);
  background: var(--lc-surface);
  font-weight: 900;
}

:deep(.info-list) {
  display: grid;
  gap: var(--lc-space-3);
  margin: 0;
}

:deep(.info-list div) {
  display: flex;
  justify-content: space-between;
  gap: var(--lc-space-4);
}

:deep(.info-list dt) {
  color: var(--lc-subtle);
  font-size: var(--lc-text-sm);
}

:deep(.info-list dd) {
  margin: 0;
  color: var(--lc-text);
  font-size: var(--lc-text-sm);
  font-weight: 900;
  text-align: right;
}

.post-head img,
.member-item img,
.admin-item img {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.post-head strong,
.member-item strong,
.admin-item span,
.activity-placeholder strong {
  display: block;
  color: var(--lc-text);
  font-size: var(--lc-text-base);
}

.post-head span,
.member-item span {
  display: block;
  margin-top: 2px;
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
}

.post-card p {
  margin: var(--lc-space-3) 0 0;
  color: var(--lc-muted);
  line-height: 1.75;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--lc-space-2);
  margin-top: var(--lc-space-3);
}

.post-images img {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: var(--lc-radius-xs);
  object-fit: cover;
}

.post-actions {
  display: flex;
  gap: var(--lc-space-3);
  margin-top: var(--lc-space-3);
}

.post-actions button {
  height: 30px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-muted);
  background: var(--lc-surface);
  font-weight: 900;
}

.post-actions button.active {
  color: var(--lc-blue);
  border-color: var(--lc-blue-border);
  background: var(--lc-blue-light);
}

.join-hint {
  margin: 0 0 var(--lc-space-4);
  padding: var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-muted);
  background: var(--lc-bg);
  font-weight: 800;
}

.post-delete-btn {
  height: 28px;
  padding: 0 var(--lc-space-2);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-subtle);
  background: var(--lc-surface);
  font-size: var(--lc-text-xs);
  font-weight: 800;
  cursor: pointer;
}

.comment-panel {
  margin-top: var(--lc-space-3);
  padding: var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
}

.comment-status,
.comment-login-hint {
  margin: 0;
  font-size: var(--lc-text-sm);
  color: var(--lc-subtle);
}

.comment-list {
  list-style: none;
  margin: 0 0 var(--lc-space-3);
  padding: 0;
  display: grid;
  gap: var(--lc-space-2);
}

.comment-list li {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  gap: var(--lc-space-2);
  align-items: start;
}

.comment-list li img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-body strong {
  display: block;
  font-size: var(--lc-text-sm);
  color: var(--lc-text);
}

.comment-body span {
  font-size: var(--lc-text-xs);
  color: var(--lc-subtle);
}

.comment-body p {
  margin: 4px 0 0;
  font-size: var(--lc-text-sm);
  color: var(--lc-muted);
  line-height: 1.5;
}

.comment-empty {
  grid-column: 1 / -1;
  color: var(--lc-subtle);
  font-size: var(--lc-text-sm);
}

.comment-form {
  display: flex;
  gap: var(--lc-space-2);
  align-items: center;
}

.comment-form input {
  flex: 1;
  min-width: 0;
  height: 36px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  font: inherit;
}

.comment-del {
  height: 26px;
  padding: 0 6px;
  border: 0;
  background: none;
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
  cursor: pointer;
}

.member-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto auto;
  gap: var(--lc-space-3);
  align-items: center;
  padding: var(--lc-space-3);
}

.member-main {
  min-width: 0;
}

.member-name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--lc-space-2);
}

.role-pill {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 2px var(--lc-space-2);
  font-size: var(--lc-text-xs);
  font-weight: 800;
  color: var(--lc-blue);
  background: var(--lc-blue-light);
}

.member-actions-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--lc-space-2);
}

.owner-admin-actions button:last-child {
  color: var(--lc-blue);
}

.role-admin-btn {
  height: 30px;
  border: 1px solid var(--lc-blue-border);
  border-radius: var(--lc-radius-xs);
  padding: 0 var(--lc-space-3);
  color: var(--lc-blue);
  background: var(--lc-surface);
  font-size: var(--lc-text-xs);
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.role-admin-btn.muted {
  border-color: var(--lc-border);
  color: var(--lc-muted);
}

.role-admin-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.member-item b.pending {
  color: var(--lc-amber);
  background: var(--lc-amber-light);
}

.member-actions {
  display: flex;
  gap: var(--lc-space-2);
}

.member-actions button {
  height: 30px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  padding: 0 var(--lc-space-3);
  color: var(--lc-blue);
  background: var(--lc-surface);
  font-weight: 900;
  cursor: pointer;
}

.member-actions button:last-child {
  color: var(--lc-red);
}

.member-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.info-list {
  display: grid;
  gap: var(--lc-space-3);
  margin: 0;
}

.info-list div {
  display: flex;
  justify-content: space-between;
  gap: var(--lc-space-4);
}

.info-list dt {
  color: var(--lc-subtle);
  font-size: var(--lc-text-sm);
}

.info-list dd {
  margin: 0;
  color: var(--lc-text);
  font-size: var(--lc-text-sm);
  font-weight: 900;
  text-align: right;
}

.admin-list {
  display: grid;
  gap: var(--lc-space-3);
}

.admin-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: var(--lc-space-3);
  align-items: center;
}

.admin-item em {
  color: var(--lc-muted);
  font-size: var(--lc-text-xs);
  font-style: normal;
  font-weight: 900;
}

@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .home-two-col {
    grid-template-columns: minmax(0, 1fr) minmax(0, 280px);
  }

  .home-grid {
    grid-template-columns: 1fr;
  }
}

/* ── 近期活动预览 ── */
.activity-preview h2 { margin: 0; }
.activity-mini-list { display: grid; gap: var(--lc-space-3); margin-top: var(--lc-space-3); }
.activity-mini-item {
  display: grid;
  gap: var(--lc-space-1);
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
}
.activity-mini-item strong { color: var(--lc-text); font-size: var(--lc-text-sm); }
.activity-mini-item span { color: var(--lc-muted); font-size: var(--lc-text-xs); }
.act-location, .act-count { font-size: var(--lc-text-xs); color: var(--lc-subtle); }

/* ── 子标题（替代裸 h3） ── */
.sub-section-head {
  margin: var(--lc-space-5) 0 var(--lc-space-3);
  color: var(--lc-text);
  font-size: var(--lc-text-base);
  font-weight: 900;
}

/* ── 打卡 ── */
.checkin-summary {
  display: flex;
  gap: var(--lc-space-5);
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
  margin-bottom: var(--lc-space-4);
}
.checkin-stat { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; }
.stat-val { font-size: 22px; font-weight: 900; color: var(--lc-blue); }
.stat-val.checked { color: var(--lc-green); }
.stat-label { font-size: var(--lc-text-xs); color: var(--lc-subtle); }
.checkin-types { display: flex; flex-wrap: wrap; gap: var(--lc-space-2); margin-bottom: var(--lc-space-3); }
.checkin-types button {
  height: 32px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: 999px;
  color: var(--lc-muted);
  background: var(--lc-surface);
  font-size: var(--lc-text-sm);
  font-weight: 800;
  cursor: pointer;
}
.checkin-types button.active {
  border-color: var(--lc-blue);
  color: var(--lc-blue);
  background: var(--lc-blue-light);
}
.checkin-form { display: grid; gap: var(--lc-space-3); margin-bottom: var(--lc-space-5); }
.checkin-form textarea {
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  font: inherit;
  resize: vertical;
}
.checkin-done { color: var(--lc-green); background: var(--lc-green-light); }
.checkin-play-hint {
  margin: 0 0 var(--lc-space-4);
  padding: var(--lc-space-2) var(--lc-space-3);
  font-size: var(--lc-text-xs);
  color: var(--lc-muted);
  line-height: 1.5;
  background: var(--lc-bg);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
}
.checkin-list { display: grid; gap: var(--lc-space-3); margin-top: var(--lc-space-3); }
.checkin-item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: var(--lc-space-3);
  align-items: start;
}
.checkin-item img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.checkin-userline {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.checkin-username {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--lc-text-sm);
  color: var(--lc-text);
}
.checkin-title-pill {
  flex: 0 1 auto;
  max-width: 42%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.checkin-text { margin: 6px 0 0; font-size: var(--lc-text-sm); color: var(--lc-muted); }
.checkin-submeta {
  margin: 4px 0 0;
  font-size: 10px;
  color: var(--lc-subtle);
  line-height: 1.4;
}
.checkin-time { display: block; margin-top: 4px; font-size: var(--lc-text-xs); color: var(--lc-subtle); }

.section-head-links {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-3);
  align-items: center;
}

.user-title-pill {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 800;
  color: var(--lc-muted);
  background: var(--lc-soft-alt);
  line-height: 1.4;
}

.rankings-card {
  margin: var(--lc-space-4) 0 var(--lc-space-5);
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
}

.ranking-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-3);
}

.ranking-tabs button {
  height: 32px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: 999px;
  background: var(--lc-surface);
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
  font-weight: 800;
  cursor: pointer;
}

.ranking-tabs button.active {
  border-color: var(--lc-blue);
  color: var(--lc-blue);
  background: var(--lc-blue-light);
}

.ranking-empty-hint {
  margin: 0;
  padding: var(--lc-space-3);
  text-align: center;
  font-size: var(--lc-text-sm);
  color: var(--lc-muted);
}

.ranking-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--lc-space-2);
  max-width: 100%;
}

.ranking-row {
  display: grid;
  grid-template-columns: 40px 36px minmax(0, 1fr);
  gap: var(--lc-space-3);
  align-items: center;
  padding: var(--lc-space-2) var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.ranking-row--plain {
  border-color: var(--lc-soft-alt);
  opacity: 0.98;
}

.ranking-row--top3 {
  border-color: rgba(245, 158, 11, 0.4);
  box-shadow: inset 3px 0 0 rgba(245, 158, 11, 0.35);
}

.ranking-row--me {
  border-color: rgba(59, 130, 246, 0.28);
  background: rgba(239, 246, 255, 0.65);
}

.ranking-row--top3.ranking-row--me {
  box-shadow:
    inset 3px 0 0 rgba(245, 158, 11, 0.35),
    inset 0 0 0 999px rgba(239, 246, 255, 0.4);
}

.ranking-row--appended {
  border-style: dashed;
}

.rank-medal {
  font-size: 17px;
  text-align: center;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.ranking-row--plain .rank-medal {
  font-size: 13px;
  font-weight: 800;
  color: var(--lc-subtle);
}

.rank-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.rank-main {
  min-width: 0;
}

.rank-name-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.rank-nickname {
  font-size: var(--lc-text-sm);
  color: var(--lc-text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-title-pill {
  flex: 0 1 auto;
  max-width: 38%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 1px 5px;
  font-size: 9px;
}

.rank-append-tag {
  font-size: var(--lc-text-xs);
  color: var(--lc-blue);
  font-weight: 800;
}

.rank-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  font-size: var(--lc-text-xs);
  color: var(--lc-subtle);
  margin-top: 2px;
}

.checkin-social-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: var(--lc-space-3);
}

.checkin-social-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid var(--lc-border);
  border-radius: 999px;
  background: var(--lc-surface);
  color: var(--lc-muted);
  font-size: var(--lc-text-xs);
  font-weight: 800;
  cursor: pointer;
}

.checkin-social-btn.active {
  border-color: var(--lc-pink);
  color: var(--lc-pink);
  background: var(--lc-pink-light);
}

.checkin-social-btn.is-open {
  border-color: rgba(59, 130, 246, 0.45);
  color: var(--lc-blue);
  background: rgba(239, 246, 255, 0.9);
}

.checkin-comments-panel {
  margin-top: var(--lc-space-2);
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
}

.checkin-comment-list {
  list-style: none;
  margin: 0 0 var(--lc-space-3);
  padding: 0;
  display: grid;
  gap: var(--lc-space-2);
}

.checkin-comment-li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  font-size: var(--lc-text-xs);
  color: var(--lc-muted);
  align-items: baseline;
}

.checkin-comment-empty {
  margin: 0;
  padding: var(--lc-space-2) 0;
  font-size: var(--lc-text-xs);
  color: var(--lc-subtle);
  text-align: center;
  list-style: none;
}

.checkin-comments-more {
  display: block;
  width: 100%;
  margin: 0 0 var(--lc-space-3);
  padding: 6px 0;
  border: none;
  background: none;
  color: var(--lc-blue);
  font-size: var(--lc-text-xs);
  font-weight: 800;
  cursor: pointer;
  text-align: center;
}

.cc-author {
  font-weight: 800;
  color: var(--lc-text);
}

.cc-text {
  grid-column: 1 / -1;
  overflow-wrap: break-word;
}

.cc-time {
  color: var(--lc-subtle);
  font-size: 10px;
}

.cc-del {
  grid-column: 2;
  grid-row: 1;
  border: none;
  background: none;
  color: var(--lc-red);
  font-size: var(--lc-text-xs);
  cursor: pointer;
  padding: 0;
}

.checkin-comment-form {
  display: flex;
  flex-wrap: nowrap;
  gap: var(--lc-space-2);
  align-items: center;
  min-width: 0;
}

.checkin-comment-input {
  flex: 1 1 auto;
  min-width: 0;
  width: 0;
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  font: inherit;
  box-sizing: border-box;
}

.checkin-comment-form .primary-btn.small {
  flex: 0 0 auto;
  white-space: nowrap;
}

@media (max-width: 560px) {
  .rank-name-line {
    flex-wrap: wrap;
  }

  .rank-title-pill {
    max-width: 100%;
  }

  .checkin-userline {
    flex-wrap: wrap;
  }

  .checkin-title-pill {
    max-width: 100%;
  }

  .checkin-comment-form {
    flex-wrap: wrap;
  }

  .checkin-comment-input {
    width: 100%;
    flex: 1 1 100%;
  }

  .checkin-comment-form .primary-btn.small {
    width: 100%;
  }
}

/* ── 任务 ── */
.task-list { display: grid; gap: var(--lc-space-3); }
.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--lc-space-4);
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
}
.task-info { display: flex; align-items: center; gap: var(--lc-space-3); }
.task-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--lc-border);
  flex: 0 0 10px;
}
.task-status-dot.done { background: var(--lc-green); }
.task-info strong { display: block; color: var(--lc-text); font-size: var(--lc-text-sm); }
.task-reward { font-size: var(--lc-text-xs); color: var(--lc-blue); font-weight: 800; }
.task-claimed { color: var(--lc-green); font-size: var(--lc-text-sm); font-weight: 800; }
.task-pending { color: var(--lc-subtle); font-size: var(--lc-text-sm); }
.task-date { color: var(--lc-subtle); font-size: var(--lc-text-sm); }

/* ── 活动 ── */
.activity-form {
  display: grid;
  gap: var(--lc-space-3);
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
  margin-bottom: var(--lc-space-5);
}
.activity-form input, .activity-form textarea, .activity-form select {
  width: 100%;
  padding: var(--lc-space-2) var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-text);
  background: var(--lc-surface);
  font: inherit;
}
.activity-form textarea { resize: vertical; }
.activity-form-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--lc-space-3); }
.activity-form-row label { display: block; margin-bottom: 4px; font-size: var(--lc-text-xs); color: var(--lc-subtle); font-weight: 800; }
.activity-list { display: grid; gap: var(--lc-space-4); margin-top: var(--lc-space-3); }
.activity-card {
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
}
.activity-head { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--lc-space-3); margin-bottom: var(--lc-space-2); }
.activity-head strong { color: var(--lc-text); font-size: var(--lc-text-base); }
.activity-status {
  flex: 0 0 auto;
  padding: 3px var(--lc-space-2);
  border-radius: 999px;
  font-size: var(--lc-text-xs);
  font-weight: 800;
  color: var(--lc-blue);
  background: var(--lc-blue-light);
}
.activity-status.ended, .activity-status.cancelled {
  color: var(--lc-muted);
  background: var(--lc-bg);
}
.activity-desc { margin: 0 0 var(--lc-space-2); color: var(--lc-muted); font-size: var(--lc-text-sm); line-height: 1.6; }
.activity-meta { display: flex; flex-wrap: wrap; gap: var(--lc-space-3); margin-bottom: var(--lc-space-3); }
.activity-meta span { font-size: var(--lc-text-xs); color: var(--lc-subtle); }
.activity-actions { display: flex; flex-wrap: wrap; gap: var(--lc-space-2); align-items: center; }
.signed-badge { font-size: var(--lc-text-xs); color: var(--lc-green); font-weight: 800; }
.cancel-signup-btn {
  height: 30px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-muted);
  background: var(--lc-surface);
  font-size: var(--lc-text-xs);
  font-weight: 800;
  cursor: pointer;
}
.cancel-activity-btn {
  height: 30px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-red);
  background: var(--lc-surface);
  font-size: var(--lc-text-xs);
  font-weight: 800;
  cursor: pointer;
}

/* ── 按钮形式的 tab 跳转链接 ── */
.text-link-btn {
  border: 0;
  padding: 0;
  color: var(--lc-blue);
  background: transparent;
  font-size: var(--lc-text-sm);
  font-weight: 900;
  cursor: pointer;
}

/* ── 首页打卡卡片 ── */
.home-checkin-stats {
  display: flex;
  gap: var(--lc-space-4);
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
  margin-bottom: var(--lc-space-3);
}
.home-stat { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; }
.home-stat-val { font-size: 22px; font-weight: 900; color: var(--lc-blue); }
.home-stat-val.stat-green { color: var(--lc-green); }
.home-stat-label { font-size: var(--lc-text-xs); color: var(--lc-subtle); }
.home-card-foot { display: flex; justify-content: flex-end; margin-top: var(--lc-space-2); }
.checkin-done-hint { font-size: var(--lc-text-sm); color: var(--lc-green); font-weight: 800; }
.home-muted-hint { font-size: var(--lc-text-sm); color: var(--lc-subtle); }

/* ── 首页任务卡片 ── */
.home-task-summary { display: flex; align-items: center; gap: var(--lc-space-3); margin-bottom: var(--lc-space-3); }
.task-progress-text { font-size: var(--lc-text-sm); color: var(--lc-muted); font-weight: 800; }
.claimable-badge {
  padding: 2px var(--lc-space-2);
  border-radius: 999px;
  font-size: var(--lc-text-xs);
  font-weight: 800;
  color: var(--lc-amber);
  background: var(--lc-amber-light);
}
.home-task-list { display: grid; gap: var(--lc-space-2); margin-bottom: var(--lc-space-2); }
.home-task-item { display: flex; align-items: center; gap: var(--lc-space-2); font-size: var(--lc-text-sm); color: var(--lc-muted); }
.home-task-done { margin-bottom: var(--lc-space-2); font-size: var(--lc-text-sm); color: var(--lc-green); font-weight: 800; }

/* ── 首页动态列表 ── */
.home-post-item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: var(--lc-space-3);
  align-items: start;
  padding: var(--lc-space-3) 0;
  border-bottom: 1px solid var(--lc-border);
}
.home-post-item:last-child { border-bottom: 0; }
.home-post-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.home-post-meta { display: flex; align-items: center; gap: var(--lc-space-2); margin-bottom: 4px; }
.home-post-meta strong { font-size: var(--lc-text-sm); color: var(--lc-text); }
.home-post-meta span { font-size: var(--lc-text-xs); color: var(--lc-subtle); }
.home-post-excerpt {
  margin: 0 0 4px;
  font-size: var(--lc-text-sm);
  color: var(--lc-muted);
  line-height: 1.6;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.home-post-stats { display: flex; gap: var(--lc-space-3); font-size: var(--lc-text-xs); color: var(--lc-subtle); }

/* ── 首页活动列表 ── */
.home-activity-list { display: grid; gap: var(--lc-space-3); }
.home-activity-item {
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
}
.home-activity-head { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--lc-space-2); margin-bottom: var(--lc-space-1); }
.home-activity-head strong { color: var(--lc-text); font-size: var(--lc-text-sm); }
.home-activity-meta { display: flex; flex-wrap: wrap; gap: var(--lc-space-2); font-size: var(--lc-text-xs); color: var(--lc-subtle); }

/* ── 首页公告列表 ── */
.home-notice-list { display: grid; gap: var(--lc-space-3); }

/* ── 首页两列布局 ── */
.home-two-col {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 320px);
  gap: var(--lc-space-4);
  align-items: start;
}

.home-col {
  display: grid;
  gap: var(--lc-space-4);
  align-content: start;
}

/* ── 行动引导文案 ── */
.home-guide-text {
  margin: 0 0 var(--lc-space-3);
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
  line-height: 1.6;
  font-weight: 800;
}

/* ── 首页轻量发动态表单 ── */
.quick-post-form {
  display: grid;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-4);
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
}

.quick-post-form textarea {
  width: 100%;
  padding: var(--lc-space-2) var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-text);
  background: var(--lc-surface);
  font: inherit;
  font-size: var(--lc-text-sm);
  resize: none;
  box-sizing: border-box;
}

.quick-post-form textarea:focus {
  outline: none;
  border-color: var(--lc-blue-border);
}

.quick-post-foot {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .group-detail-page {
    width: calc(100% - var(--lc-space-4));
  }

  .member-display-name-panel {
    margin-left: var(--lc-space-4);
    margin-right: var(--lc-space-4);
  }

  .detail-hero-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;
  }

  .detail-hero-metrics .metric-card {
    min-height: 0;
    padding: 8px 6px;
  }

  .detail-hero-metrics .metric-card p {
    display: none;
  }

  .hero-body {
    grid-template-columns: 1fr;
    padding: 0 var(--lc-space-4) var(--lc-space-4);
  }

  .hero-actions,
  .section-head,
  .post-form-foot,
  .member-tools > div {
    align-items: stretch;
    flex-direction: column;
  }

  .home-two-col {
    grid-template-columns: 1fr;
  }

  .home-two-col .section-head {
    flex-direction: row;
    align-items: center;
  }

  .tabs {
    padding: 0 var(--lc-space-4);
    gap: var(--lc-space-5);
  }

  .post-images {
    grid-template-columns: 1fr;
  }

  .member-item {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .member-actions-wrap {
    grid-column: 2;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: flex-start;
    align-items: center;
  }

  .member-actions {
    grid-column: 2;
  }
}

/* ── 团体投票 ── */
.poll-legacy-hint {
  margin: 0;
  padding: var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
}
.poll-form {
  display: grid;
  gap: var(--lc-space-3);
  margin-bottom: var(--lc-space-5);
  padding-bottom: var(--lc-space-4);
  border-bottom: 1px solid var(--lc-border);
}
.poll-form-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-4);
  align-items: flex-end;
}
.poll-field {
  display: grid;
  gap: var(--lc-space-1);
  font-size: var(--lc-text-sm);
  color: var(--lc-muted);
}
.poll-field select,
.poll-field input[type='number'] {
  min-width: 200px;
  height: 36px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  padding: 0 var(--lc-space-2);
  background: var(--lc-surface);
  color: var(--lc-text);
}
.poll-options-label {
  font-size: var(--lc-text-sm);
  color: var(--lc-muted);
  font-weight: 800;
}
.poll-list {
  display: grid;
  gap: var(--lc-space-4);
}
.poll-card {
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-bg);
}
.poll-card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-2);
}
.poll-card-head strong {
  color: var(--lc-text);
  font-size: var(--lc-text-md);
}
.poll-mode-pill {
  font-size: var(--lc-text-xs);
  font-weight: 800;
  padding: 2px var(--lc-space-2);
  border-radius: 999px;
  background: var(--lc-blue-light);
  color: var(--lc-blue);
}
.poll-status {
  font-size: var(--lc-text-xs);
  font-weight: 800;
  margin-left: auto;
}
.poll-status.live { color: var(--lc-green); }
.poll-status.wait { color: var(--lc-amber); }
.poll-status.ended { color: var(--lc-subtle); }
.poll-time {
  margin: 0 0 var(--lc-space-2);
  font-size: var(--lc-text-sm);
  color: var(--lc-muted);
}
.poll-voted-tag {
  margin: 0 0 var(--lc-space-2);
  font-size: var(--lc-text-sm);
  color: var(--lc-green);
  font-weight: 800;
}
.poll-count-hint {
  margin: 0 0 var(--lc-space-2);
  font-size: var(--lc-text-xs);
  color: var(--lc-subtle);
  line-height: 1.5;
}
.poll-detail {
  margin-top: var(--lc-space-4);
  padding-top: var(--lc-space-3);
  border-top: 1px dashed var(--lc-border);
}
.poll-detail-desc {
  margin: 0 0 var(--lc-space-3);
  font-size: var(--lc-text-sm);
  color: var(--lc-muted);
  line-height: 1.6;
}
.poll-voter-total {
  margin: 0 0 var(--lc-space-2);
  font-size: var(--lc-text-sm);
  color: var(--lc-text);
  font-weight: 800;
}
.poll-option-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--lc-space-2);
}
.poll-option-row {
  margin: 0;
}
.poll-option-label {
  display: flex;
  align-items: flex-start;
  gap: var(--lc-space-2);
  font-size: var(--lc-text-sm);
  color: var(--lc-text);
  cursor: pointer;
}
.poll-option-text {
  flex: 1;
  min-width: 0;
  line-height: 1.5;
}
.poll-option-count {
  font-size: var(--lc-text-xs);
  color: var(--lc-muted);
  white-space: nowrap;
}
.poll-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-3);
  margin-top: var(--lc-space-4);
}
.poll-reveal-btn {
  height: 32px;
  padding: 0 var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  color: var(--lc-text);
  font-size: var(--lc-text-xs);
  font-weight: 800;
  cursor: pointer;
}
.poll-reveal-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.season-card .season-title {
  margin: 0 0 var(--lc-space-2);
  font-size: var(--lc-text-sm);
  color: var(--lc-subtle);
}

.pending-audit-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 0 12px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  font-size: 14px;
  color: #9a3412;
}

.invite-join-hint {
  margin: 0 0 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  font-size: 13px;
  color: #065f46;
}

.season-top-list {
  list-style: none;
  margin: var(--lc-space-3) 0 0;
  padding: 0;
}

.season-top-list li {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
  padding: 6px 0;
  font-size: var(--lc-text-sm);
  border-bottom: 1px solid var(--lc-soft);
}

.season-top-list li strong {
  margin-left: auto;
  color: var(--lc-indigo);
}

.weekly-digest-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-3);
  padding: var(--lc-space-3);
  border-radius: var(--lc-radius-md);
  background: var(--lc-surface-muted);
  border: 1px solid var(--lc-border);
  font-size: var(--lc-text-sm);
}

.activity-event-select {
  width: 100%;
  padding: var(--lc-space-2);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  font: inherit;
}

.activity-event-hint,
.activity-platform-link {
  font-size: var(--lc-text-sm);
  color: var(--lc-text-muted);
  margin: 0 0 var(--lc-space-2);
}

.report-post-btn {
  border: none;
  background: transparent;
  color: var(--lc-text-muted);
  cursor: pointer;
  font: inherit;
  font-size: var(--lc-text-sm);
}

.activity-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: var(--lc-z-modal, 1000);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--lc-space-4);
  background: rgb(0 0 0 / 40%);
}

.activity-modal {
  width: min(400px, 100%);
  padding: var(--lc-space-4);
  border-radius: var(--lc-radius-lg);
  background: var(--lc-surface);
  box-shadow: var(--lc-shadow-lg);
}

.activity-modal.wide {
  width: min(520px, 100%);
}

.activity-modal input,
.activity-modal select {
  width: 100%;
  margin: var(--lc-space-2) 0;
  padding: var(--lc-space-2);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  font: inherit;
}

.activity-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--lc-space-2);
}

.review-candidate-list {
  list-style: none;
  margin: 0 0 var(--lc-space-3);
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--lc-space-2);
}

.review-candidate-list li {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
  font-size: var(--lc-text-sm);
}

.review-candidate-list img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.review-candidate-list em {
  margin-left: auto;
  color: var(--lc-text-muted);
  font-style: normal;
}

.primary-btn.small.outline {
  background: transparent;
  color: var(--lc-blue);
  border: 1px solid var(--lc-blue-border);
}
</style>
