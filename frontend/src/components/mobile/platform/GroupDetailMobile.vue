<template>
  <section class="gd-mobile">
    <p v-if="loading.detail" class="gd-state">加载中…</p>
    <div v-else-if="errors.detail" class="gd-state err">
      <p>{{ errors.detail }}</p>
      <button type="button" @click="loadDetail">重试</button>
      <router-link :to="groupsPath()">返回列表</router-link>
    </div>

    <template v-else-if="group">
      <header class="gd-hero">
        <img v-if="group.coverUrl" class="gd-cover" :src="group.coverUrl" :alt="group.name">
        <div class="gd-hero-body">
          <h1>{{ group.name }}</h1>
          <p class="gd-meta">{{ group.region }} · {{ group.memberCount }} 人 · {{ group.joinLabel }}</p>
          <p v-if="group.description" class="gd-desc">{{ group.description }}</p>
          <div class="gd-hero-actions">
            <button
              type="button"
              class="gd-join"
              :disabled="joinDisabled || joining"
              @click="applyJoin"
            >
              {{ joining ? '处理中…' : joinButtonText }}
            </button>
            <router-link
              v-if="showSpaceManageLink"
              class="gd-manage"
              :to="spaceManageEntry"
            >进入运营台</router-link>
          </div>
        </div>
      </header>

      <GroupCapabilityBanner :group-id="group.id" class="gd-capability-banner" />

      <SpaceCampaignMemberPanel :group-id="group.id" :is-member="group.isMember" class="gd-camp-panel" />

      <div v-if="group.managed && pendingMemberCount > 0" class="gd-pending-banner">
        <span>有 {{ pendingMemberCount }} 条入团申请待审核</span>
        <router-link :to="spaceManageMembersEntry">去审核</router-link>
      </div>

      <GroupInvitePanel
        v-if="group.managed && joinModeKey === 'invite'"
        :group-id="group.id"
        :join-mode-key="joinModeKey"
        :managed="group.managed"
      />

      <div v-if="group.managed && weeklyDigest" class="gd-digest">
        <p>本周打卡率 {{ weeklyDigest.checkinRatePercent }}% · 活跃 {{ weeklyDigest.activeMembers }} 人</p>
        <button type="button" class="gd-link-btn" :disabled="sendingDigest" @click="sendWeeklyDigestNow">
          {{ sendingDigest ? '…' : '发周报' }}
        </button>
      </div>

      <p v-if="joinModeKey === 'invite' && inviteCodeFromQuery && !group.isMember" class="gd-invite-hint">
        已识别邀请码，点击下方按钮验证并加入
      </p>

      <nav class="gd-tabs gd-tabs-sticky" aria-label="团体内容">
        <router-link
          v-for="tab in tabs"
          :key="tab.key"
          :to="tab.to"
          :class="{ active: activeTab === tab.key }"
        >
          {{ tab.label }}
        </router-link>
      </nav>

      <p v-if="message" class="gd-flash" :class="{ err: messageType === 'error' }">{{ message }}</p>

      <!-- 首页 -->
      <div v-if="activeTab === 'home'" class="gd-panel">
        <article class="gd-card gd-card-row">
          <div>
            <h2>今日打卡</h2>
            <p class="gd-sub">
              今日 {{ checkinSummary.todayCount }} 人打卡
              <template v-if="group.isMember"> · 连续 {{ checkinSummary.myStreakDays }} 天</template>
            </p>
          </div>
          <router-link :to="groupTabPath('checkin')" class="gd-link">去打卡</router-link>
        </article>
        <article class="gd-card gd-card-row">
          <div>
            <h2>团体动态</h2>
            <p class="gd-sub">{{ posts.length ? `最近 ${posts.length} 条讨论` : '还没有动态' }}</p>
          </div>
          <router-link :to="groupTabPath('posts')" class="gd-link">查看</router-link>
        </article>
        <article class="gd-card gd-card-row">
          <div>
            <h2>团体活动</h2>
            <p class="gd-sub">
              {{ upcomingActivities.length ? `近期 ${upcomingActivities.length} 个活动可参与` : '暂无近期活动' }}
            </p>
          </div>
          <router-link :to="groupTabPath('activities')" class="gd-link">查看</router-link>
        </article>
        <article v-if="isPollsSupported" class="gd-card gd-card-row">
          <div>
            <h2>团体投票</h2>
            <p class="gd-sub">参与团体决策与意见征集</p>
          </div>
          <router-link :to="groupTabPath('polls')" class="gd-link">查看</router-link>
        </article>
        <article v-if="group.isMember" class="gd-card gd-card-row">
          <div>
            <h2>今日任务</h2>
            <p class="gd-sub">
              <template v-if="claimableCount">有 {{ claimableCount }} 个奖励待领取</template>
              <template v-else>完成团体任务领经验</template>
            </p>
          </div>
          <router-link :to="groupTabPath('tasks')" class="gd-link">查看</router-link>
        </article>
        <article v-if="articles.length" class="gd-card gd-card-row">
          <div>
            <h2>最新文章</h2>
            <p class="gd-notice-title">{{ articles[0].title }}</p>
          </div>
          <router-link :to="groupTabPath('articles')" class="gd-link">全部</router-link>
        </article>
        <article v-if="notices.length" class="gd-card gd-card-row">
          <div>
            <h2>最新公告</h2>
            <p class="gd-notice-title">{{ notices[0].title }}</p>
          </div>
          <router-link :to="groupTabPath('notices')" class="gd-link">全部</router-link>
        </article>
        <article v-if="seasonRank" class="gd-card gd-season">
          <GroupSeasonPanel
            :season-rank="seasonRank"
            :season-top="seasonTop"
            title="季度赛季榜"
          />
        </article>
      </div>

      <!-- 动态 -->
      <div v-else-if="activeTab === 'posts'" class="gd-panel">
        <form v-if="group.isMember" class="gd-compose" @submit.prevent="submitPost">
          <textarea v-model.trim="postDraft" rows="3" maxlength="2000" placeholder="分享团体动态…" />
          <button type="submit" class="gd-btn primary" :disabled="posting || !postDraft.trim()">
            {{ posting ? '发布中…' : '发布' }}
          </button>
        </form>
        <p v-else class="gd-hint">加入团体后可发布动态</p>
        <p v-if="errors.posts" class="gd-hint err">{{ errors.posts }}</p>
        <p v-if="loading.posts" class="gd-state">加载中…</p>
        <article v-for="post in posts" v-else :key="post.id" class="gd-post">
          <div class="gd-post-head">
            <img :src="post.avatar" alt="">
            <div>
              <strong>{{ post.author }}</strong>
              <span>{{ post.time }}</span>
            </div>
          </div>
          <p class="gd-post-text">{{ post.content }}</p>
          <div class="gd-post-actions">
            <button type="button" :class="{ on: post.likedByMe }" @click="likePost(post)">
              {{ post.likedByMe ? '已赞' : '赞' }} {{ post.likes }}
            </button>
            <button type="button" :class="{ on: expandedPostId === post.id }" @click="toggleCommentPanel(post)">
              评论 {{ post.comments }}
            </button>
            <button v-if="userStore.isLoggedIn" type="button" class="gd-link-btn" @click="openPostReport(post)">
              举报
            </button>
          </div>
          <div v-if="expandedPostId === post.id" class="gd-post-comments">
            <p v-if="commentsLoading[post.id]" class="gd-hint">评论加载中…</p>
            <ul v-else class="gd-comment-list">
              <li v-for="c in (feedComments[post.id] || [])" :key="c.id">
                <img :src="c.avatar" alt="">
                <div>
                  <strong>{{ c.author }}</strong>
                  <span>{{ c.time }}</span>
                  <p>{{ c.content }}</p>
                </div>
                <button
                  v-if="canDeleteComment(c)"
                  type="button"
                  class="gd-link-btn"
                  :disabled="deletingCommentKey === commentKey(post.id, c.id)"
                  @click="deleteComment(post, c)"
                >删</button>
              </li>
              <li v-if="!(feedComments[post.id] || []).length" class="gd-hint">暂无评论</li>
            </ul>
            <form v-if="userStore.isLoggedIn" class="gd-comment-form" @submit.prevent="submitComment(post)">
              <input v-model="commentDraft[post.id]" type="text" maxlength="500" placeholder="写一条评论…">
              <button type="submit" class="gd-btn sm primary" :disabled="commentPosting[post.id]">发送</button>
            </form>
            <p v-else class="gd-hint">登录后即可评论</p>
          </div>
        </article>
        <p v-if="!loading.posts && !posts.length" class="gd-state">暂无动态</p>
      </div>

      <!-- 打卡 -->
      <div v-else-if="activeTab === 'checkin'" class="gd-panel">
        <div class="gd-stats">
          <div><strong>{{ checkinSummary.todayCount }}</strong><span>今日人数</span></div>
          <div><strong>{{ checkinSummary.myStreakDays }}</strong><span>我的连续</span></div>
          <div><strong>{{ checkinSummary.checkedInToday ? '✓' : '—' }}</strong><span>今日状态</span></div>
        </div>
        <form v-if="group.isMember && !checkinSummary.checkedInToday" class="gd-compose" @submit.prevent="submitCheckin">
          <label class="gd-label">打卡类型</label>
          <select v-model="checkinForm.type">
            <option v-for="t in CHECKIN_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
          <textarea v-model.trim="checkinForm.content" rows="2" maxlength="500" placeholder="写一句今日记录（可选）" />
          <button type="submit" class="gd-btn primary" :disabled="checkingIn">
            {{ checkingIn ? '提交中…' : '立即打卡' }}
          </button>
        </form>
        <p v-else-if="checkinSummary.checkedInToday" class="gd-hint ok">今日已打卡</p>
        <p v-else class="gd-hint">加入团体后可打卡</p>

        <section v-if="userStore.isLoggedIn && group.isMember" class="gd-rankings">
          <h3 class="gd-section-title">排行榜</h3>
          <div class="gd-rank-tabs">
            <button type="button" :class="{ active: rankingTab === 'daily' }" @click="setRankingTab('daily')">今日榜</button>
            <button type="button" :class="{ active: rankingTab === 'streak' }" @click="setRankingTab('streak')">连续榜</button>
          </div>
          <p v-if="loadingRankings" class="gd-state sm">加载中…</p>
          <p v-else-if="rankingEmptyHint" class="gd-hint">{{ rankingEmptyHint }}</p>
          <ol v-else class="gd-rank-list">
            <li
              v-for="(row, idx) in rankingDisplayRows"
              :key="row.userId + '-' + idx"
              :class="{ me: row.isCurrentUser || row.__appendedMe, top3: idx < 3 && !row.__appendedMe }"
            >
              <span class="gd-rank-medal">{{ rankSlotDisplay(row, idx) }}</span>
              <img :src="row.avatarUrl || 'https://api.dicebear.com/7.x/initials/svg?seed=LC'" alt="">
              <div>
                <strong>{{ row.nickname || '成员' }}</strong>
                <span v-if="row.title" class="gd-title-pill">{{ row.title }}</span>
                <p>
                  第 {{ row.rank }} 名
                  <template v-if="rankingTab === 'daily'"> · 今日 {{ row.checkinCount }} 次</template>
                  · 连续 {{ row.streakDays }} 天
                </p>
              </div>
            </li>
          </ol>
        </section>

        <h3 class="gd-section-title">最近打卡</h3>
        <article v-for="c in checkinSummary.recentCheckins" :key="c.id" class="gd-checkin-item gd-checkin-rich">
          <strong>{{ c.username || c.userName || '成员' }}</strong>
          <span v-if="c.title" class="gd-title-pill">{{ c.title }}</span>
          <p v-if="c.content">{{ c.content }}</p>
          <p class="gd-checkin-meta">
            {{ CHECKIN_TYPE_LABELS[c.checkinType] || c.checkinType || '打卡' }} · 连续 {{ c.streakDays ?? 0 }} 天
          </p>
          <time>{{ formatDateTime(c.createdAt) }}</time>
          <div v-if="userStore.isLoggedIn && group.isMember" class="gd-checkin-social">
            <button
              type="button"
              :class="{ on: c.likedByCurrentUser }"
              :disabled="checkinLikeBusyId === c.id"
              @click="toggleLikeCheckin(c)"
            >赞 {{ c.likeCount ?? 0 }}</button>
            <button
              type="button"
              :class="{ on: expandedCheckinId === c.id }"
              @click="toggleCommentsBlock(c)"
            >评论 {{ c.commentCount ?? 0 }}</button>
          </div>
          <div v-if="expandedCheckinId === c.id" class="gd-checkin-comments">
            <p v-if="checkinCommentsLoading[c.id]" class="gd-hint">加载评论…</p>
            <template v-else>
              <ul>
                <li v-for="cm in visibleCommentsFor(c.id)" :key="cm.id">
                  <strong>{{ cm.nickname || '用户' }}</strong>
                  <span>{{ cm.content }}</span>
                  <button
                    v-if="Number(cm.userId) === currentUserIdNum"
                    type="button"
                    class="gd-link-btn"
                    @click="deleteMyCheckinComment(c.id, cm.id)"
                  >删除</button>
                </li>
                <li v-if="!(checkinCommentsById[c.id] || []).length" class="gd-hint">还没有评论</li>
              </ul>
              <button v-if="hasMoreComments(c.id)" type="button" class="gd-link-btn" @click="showAllCommentsFor(c.id)">
                查看更多
              </button>
            </template>
            <form class="gd-comment-form" @submit.prevent="submitCheckinComment(c)">
              <input v-model="checkinCommentDraft[c.id]" type="text" maxlength="200" placeholder="写一条评论…">
              <button type="submit" class="gd-btn sm primary" :disabled="checkinCommentPosting">发送</button>
            </form>
          </div>
        </article>
        <p v-if="!checkinSummary.recentCheckins?.length" class="gd-state">暂无打卡记录</p>
      </div>

      <!-- 任务 -->
      <div v-else-if="activeTab === 'tasks'" class="gd-panel">
        <p v-if="loading.tasks" class="gd-state">加载中…</p>
        <template v-else-if="group.isMember">
          <article v-for="task in todayTasks" :key="task.taskCode" class="gd-task">
            <div>
              <strong>{{ task.name }}</strong>
              <span>+{{ task.rewardExp }} 经验 · {{ task.progress }}/{{ task.targetCount }}</span>
            </div>
            <button
              v-if="task.completed && !task.claimed"
              type="button"
              class="gd-btn sm primary"
              :disabled="claimingTask === task.taskCode"
              @click="claimTask(task)"
            >
              {{ claimingTask === task.taskCode ? '…' : '领取' }}
            </button>
            <span v-else-if="task.claimed" class="gd-tag done">已领取</span>
            <span v-else class="gd-tag">进行中</span>
          </article>
          <p v-if="!todayTasks.length" class="gd-state">暂无团体任务</p>
        </template>
        <p v-else class="gd-hint">加入团体后参与任务</p>
      </div>

      <!-- 成员 -->
      <div v-else-if="activeTab === 'members'" class="gd-panel">
        <p v-if="loading.members" class="gd-state">加载中…</p>
        <p v-else-if="errors.members" class="gd-hint err">{{ errors.members }}</p>
        <article v-for="m in members" v-else :key="m.id" class="gd-member gd-member-row">
          <img :src="m.avatar" alt="">
          <div class="gd-member-main">
            <div class="gd-member-head">
              <strong>{{ m.name }}</strong>
              <span class="gd-pill" :class="{ pending: m.status === 'pending' }">{{ m.roleLabel }}</span>
            </div>
            <span class="gd-member-meta">{{ m.joinedAt || m.statusLabel }}</span>
            <p v-if="m.status === 'pending' && m.applyReason" class="gd-member-reason">申请说明：{{ m.applyReason }}</p>
          </div>
          <div v-if="canAuditMember(m) || canRemoveMember(m) || canOwnerToggleAdmin(m)" class="gd-member-actions">
            <template v-if="canAuditMember(m)">
              <button type="button" class="gd-btn sm primary" :disabled="moderatingMemberId === m.id" @click="approvePendingMember(m)">
                通过
              </button>
              <button type="button" class="gd-btn sm" :disabled="moderatingMemberId === m.id" @click="rejectPendingMember(m)">
                拒绝
              </button>
            </template>
            <template v-if="canOwnerToggleAdmin(m)">
              <button
                v-if="m.role === 'member'"
                type="button"
                class="gd-btn sm"
                :disabled="roleChangingMemberId === m.id"
                @click="setMemberAsAdmin(m)"
              >设为管理员</button>
              <button
                v-else-if="m.role === 'admin'"
                type="button"
                class="gd-btn sm"
                :disabled="roleChangingMemberId === m.id"
                @click="unsetMemberAdmin(m)"
              >取消管理员</button>
            </template>
            <button
              v-if="canRemoveMember(m)"
              type="button"
              class="gd-btn sm danger"
              :disabled="removingMemberId === m.id"
              @click="removeMember(m)"
            >移除</button>
          </div>
        </article>
        <p v-if="!loading.members && !members.length" class="gd-state">暂无成员</p>
      </div>

      <!-- 文章 -->
      <div v-else-if="activeTab === 'articles'" class="gd-panel">
        <form v-if="group.isMember" class="gd-compose" @submit.prevent="submitArticle">
          <input v-model.trim="articleForm.title" type="text" maxlength="200" placeholder="文章标题" required>
          <textarea v-model.trim="articleForm.content" rows="6" maxlength="20000" placeholder="写下正文…" />
          <button type="submit" class="gd-btn primary" :disabled="publishingArticle || !articleForm.title.trim() || !articleForm.content.trim()">
            {{ publishingArticle ? '发布中…' : '发布文章' }}
          </button>
        </form>
        <p v-else class="gd-state">加入团体后可以发布文章</p>
        <article v-for="a in articles" :key="a.id" class="gd-notice">
          <button type="button" class="gd-article-toggle" @click="toggleArticle(a.id)">
            <h3>{{ a.title }}</h3>
            <time>{{ a.author }} · {{ a.date }}</time>
          </button>
          <p>{{ expandedArticleId === a.id ? a.content : (a.summary || a.content) }}</p>
          <button
            v-if="expandedArticleId === a.id && canDeleteArticle(a)"
            type="button"
            class="gd-btn sm danger"
            :disabled="deletingArticleId === a.id"
            @click="removeArticle(a)"
          >删除</button>
        </article>
        <p v-if="!articles.length" class="gd-state">暂无文章</p>
      </div>

      <!-- 公告 -->
      <div v-else-if="activeTab === 'notices'" class="gd-panel">
        <article v-for="n in notices" :key="n.id" class="gd-notice">
          <h3>{{ n.title }}</h3>
          <p>{{ n.content }}</p>
          <time>{{ n.date }}</time>
        </article>
        <p v-if="!notices.length" class="gd-state">暂无公告</p>
      </div>

      <!-- 活动 -->
      <div v-else-if="activeTab === 'activities'" class="gd-panel">
        <div v-if="group.managed || (userStore.isLoggedIn && group.isMember)" class="gd-panel-head">
          <button
            v-if="group.managed"
            type="button"
            class="gd-btn sm"
            @click="showCreateActivity = !showCreateActivity"
          >
            {{ showCreateActivity ? '取消' : '发布活动' }}
          </button>
          <template v-else>
            <router-link class="gd-link" :to="`/platform/groups/${group.id}/my-activity-proposals`">我的投稿</router-link>
            <button type="button" class="gd-btn sm primary" @click="handleProposeActivity">发起活动</button>
          </template>
        </div>
        <form v-if="showCreateActivity && group.managed" class="gd-compose gd-form-block" @submit.prevent="submitActivity">
          <input v-model.trim="activityForm.title" type="text" maxlength="200" placeholder="活动标题 *" required>
          <textarea v-model.trim="activityForm.description" rows="2" maxlength="2000" placeholder="活动简介" />
          <label class="gd-label">开始时间 *</label>
          <input v-model="activityForm.startTime" type="datetime-local" required>
          <label class="gd-label">结束时间 *</label>
          <input v-model="activityForm.endTime" type="datetime-local" required>
          <input v-model.trim="activityForm.location" type="text" maxlength="200" placeholder="地点（可选）">
          <label class="gd-label">关联平台活动（可选）</label>
          <select v-model="activityForm.platformEventId">
            <option value="">不关联</option>
            <option v-for="ev in platformEvents" :key="ev.id" :value="ev.id">{{ ev.title }}</option>
          </select>
          <button type="submit" class="gd-btn primary" :disabled="creatingActivity">
            {{ creatingActivity ? '发布中…' : '确认发布' }}
          </button>
        </form>
        <p v-if="loading.activities" class="gd-state">加载中…</p>
        <article v-for="act in activities" v-else :key="act.id" class="gd-activity">
          <div class="gd-activity-head">
            <strong>{{ act.title }}</strong>
            <span :class="['gd-pill', activityStatusClass(act)]">{{ activityStatusLabel(act) }}</span>
          </div>
          <p v-if="act.description" class="gd-activity-desc">{{ act.description }}</p>
          <p class="gd-activity-meta">
            {{ formatActivityTime(act.startTime) }} — {{ formatActivityTime(act.endTime) }}
            <template v-if="act.location"> · {{ act.location }}</template>
          </p>
          <p class="gd-activity-meta">
            报名 {{ act.participantCount }}{{ act.maxParticipants > 0 ? ` / ${act.maxParticipants}` : '' }} 人
          </p>
          <div class="gd-activity-actions">
            <template v-if="!act.isEnded && act.status === 'published'">
              <button
                v-if="!act.signedUpByMe"
                type="button"
                class="gd-btn sm primary"
                :disabled="signingActivityId === act.id"
                @click="signUpActivity(act)"
              >
                {{ signingActivityId === act.id ? '…' : '立即报名' }}
              </button>
              <button
                v-else
                type="button"
                class="gd-btn sm"
                :disabled="signingActivityId === act.id"
                @click="cancelSignUpActivity(act)"
              >
                {{ signingActivityId === act.id ? '…' : '取消报名' }}
              </button>
            </template>
            <button
              v-if="act.signedUpByMe && act.hasCheckinCode && !act.checkedInByMe && !act.isEnded"
              type="button"
              class="gd-btn sm primary"
              @click="openActivityCheckin(act)"
            >签到</button>
            <button
              v-if="group.managed && act.status === 'published' && !act.isEnded"
              type="button"
              class="gd-btn sm"
              :disabled="checkinCodeBusyId === act.id"
              @click="generateActivityCode(act)"
            >{{ act.hasCheckinCode ? '刷新码' : '签到码' }}</button>
            <button
              v-if="act.canReview && act.pendingReviewCount > 0"
              type="button"
              class="gd-btn sm primary"
              @click="openActivityReview(act)"
            >互评 {{ act.pendingReviewCount }}</button>
            <router-link v-if="act.platformEventPath" :to="act.platformEventPath" class="gd-link-btn">平台活动</router-link>
            <button
              v-if="group.managed && act.status === 'published'"
              type="button"
              class="gd-btn sm danger"
              :disabled="cancellingActivityId === act.id"
              @click="cancelActivity(act)"
            >
              取消活动
            </button>
          </div>
        </article>
        <p v-if="!loading.activities && !activities.length" class="gd-state">暂无活动</p>
      </div>

      <!-- 投票 -->
      <div v-else-if="activeTab === 'polls'" class="gd-panel">
        <p v-if="!isPollsSupported" class="gd-hint">当前团体类型暂不支持线上投票</p>
        <template v-else>
          <div v-if="group.managed" class="gd-panel-head">
            <button type="button" class="gd-btn sm" @click="showPollCreate = !showPollCreate">
              {{ showPollCreate ? '取消' : '发起投票' }}
            </button>
          </div>
          <form v-if="showPollCreate && group.managed" class="gd-compose gd-form-block" @submit.prevent="submitPollCreate">
            <input v-model.trim="pollForm.title" type="text" maxlength="200" placeholder="投票标题 *" required>
            <textarea v-model.trim="pollForm.description" rows="2" placeholder="说明（可选）" />
            <label class="gd-label">类型</label>
            <select v-model="pollForm.selectionMode">
              <option value="single">单选</option>
              <option value="multiple">多选</option>
            </select>
            <label v-if="pollForm.selectionMode === 'multiple'" class="gd-label">最多可选几项</label>
            <input
              v-if="pollForm.selectionMode === 'multiple'"
              v-model.number="pollForm.maxChoices"
              type="number"
              min="2"
              max="30"
              required
            >
            <label class="gd-label">开始时间 *</label>
            <input v-model="pollForm.startTime" type="datetime-local" required>
            <label class="gd-label">结束时间 *</label>
            <input v-model="pollForm.endTime" type="datetime-local" required>
            <label class="gd-label">选项（每行一个，至少 2 行）*</label>
            <textarea v-model="pollForm.optionsText" rows="4" required placeholder="选项 A&#10;选项 B" />
            <button type="submit" class="gd-btn primary" :disabled="creatingPoll">
              {{ creatingPoll ? '发布中…' : '发布投票' }}
            </button>
          </form>
          <p v-if="loading.polls" class="gd-state">加载中…</p>
          <article v-for="p in polls" v-else :key="p.id" class="gd-poll">
            <div class="gd-poll-head">
              <strong>{{ p.title }}</strong>
              <span class="gd-pill">{{ p.selectionMode === 'multiple' ? '多选' : '单选' }}</span>
              <span v-if="p.isEnded" class="gd-pill ended">已结束</span>
              <span v-else-if="p.isNotYetOpen" class="gd-pill wait">未开始</span>
              <span v-else class="gd-pill live">进行中</span>
            </div>
            <p class="gd-activity-meta">
              {{ formatActivityTime(p.startTime) }} — {{ formatActivityTime(p.endTime) }}
            </p>
            <p v-if="p.hasVoted" class="gd-hint ok">你已提交选择</p>
            <button type="button" class="gd-link-btn" @click="togglePollExpand(p.id)">
              {{ expandedPollId === p.id ? '收起' : '查看并参与' }}
            </button>
            <div v-if="expandedPollId === p.id && pollDetailCache[p.id]" class="gd-poll-detail">
              <p v-if="pollDetailCache[p.id].description" class="gd-activity-desc">
                {{ pollDetailCache[p.id].description }}
              </p>
              <ul class="gd-poll-options">
                <li v-for="opt in pollDetailCache[p.id].options" :key="opt.id">
                  <label>
                    <input
                      v-if="pollDetailCache[p.id].selectionMode === 'single'"
                      type="radio"
                      :name="'poll-' + p.id"
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
                    <span>{{ opt.label }}</span>
                    <em v-if="pollDetailCache[p.id].countsVisible && opt.voteCount != null">
                      {{ opt.voteCount }} 票
                    </em>
                  </label>
                </li>
              </ul>
              <div class="gd-poll-actions">
                <button
                  v-if="group.isMember && !pollDetailCache[p.id].isEnded && !pollDetailCache[p.id].isNotYetOpen"
                  type="button"
                  class="gd-btn sm primary"
                  :disabled="votingPollId === p.id || !(pollPick[p.id] || []).length"
                  @click="submitPollVote(p.id)"
                >
                  {{ votingPollId === p.id ? '…' : '提交选择' }}
                </button>
                <button
                  v-if="group.managed && !pollDetailCache[p.id].resultsPublic"
                  type="button"
                  class="gd-btn sm"
                  :disabled="revealingPollId === p.id"
                  @click="revealPollResults(p.id)"
                >
                  公开结果
                </button>
              </div>
            </div>
          </article>
          <p v-if="!loading.polls && !polls.length" class="gd-state">暂无投票</p>
        </template>
      </div>

      <!-- 资料 -->
      <div v-else-if="activeTab === 'profile'" class="gd-panel">
        <dl class="gd-profile">
          <div><dt>名称</dt><dd>{{ group.name }}</dd></div>
          <div><dt>分类</dt><dd>{{ group.category }}</dd></div>
          <div><dt>地区</dt><dd>{{ group.region }}</dd></div>
          <div><dt>成员</dt><dd>{{ group.memberCount }} 人</dd></div>
          <div><dt>加入方式</dt><dd>{{ group.joinLabel }}</dd></div>
        </dl>
        <p class="gd-desc-block">{{ group.description }}</p>
      </div>
    </template>

    <div v-if="activityReviewOpen" class="gd-modal" @click.self="closeActivityReview">
      <div class="gd-modal-body gd-modal-tall">
        <h3>活动互评</h3>
        <p v-if="activityReviewLoading" class="gd-hint">加载中…</p>
        <ul v-else class="gd-review-list">
          <li v-for="c in activityReviewCandidates" :key="c.userId">
            <span>{{ c.nickname }}</span>
            <template v-if="c.reviewed"><em>已评</em></template>
            <template v-else>
              <select v-model="activityReviewRatings[c.userId]">
                <option v-for="n in 5" :key="n" :value="n">{{ n }}分</option>
              </select>
              <button type="button" class="gd-btn sm" @click="submitOneReview(c)">提交</button>
            </template>
          </li>
        </ul>
        <button type="button" class="gd-link-btn" @click="closeActivityReview">关闭</button>
      </div>
    </div>

    <div v-if="activityCheckinOpen" class="gd-modal" @click.self="activityCheckinOpen = false">
      <div class="gd-modal-body">
        <h3>现场签到</h3>
        <input v-model.trim="activityCheckinCode" type="text" maxlength="8" placeholder="输入签到码">
        <button type="button" class="gd-btn primary" :disabled="activityCheckinSubmitting" @click="submitActivityCheckin">
          {{ activityCheckinSubmitting ? '…' : '确认' }}
        </button>
      </div>
    </div>

    <GroupPostReportDialog
      :open="reportDialogOpen"
      :post-id="reportPostId"
      :target-user-id="reportTargetUserId"
      @close="reportDialogOpen = false"
      @submitted="message = '举报已提交'; messageType = 'success'"
    />

    <ActivityProposalDialog
      :open="showProposalDialog"
      :group-id="group?.id"
      @close="closeProposalDialog"
      @submitted="onProposalSubmitted"
    />
  </section>
</template>

<script setup>
import ActivityProposalDialog from '@/components/platform/groups/ActivityProposalDialog.vue'
import GroupCapabilityBanner from '@/components/platform/groups/GroupCapabilityBanner.vue'
import SpaceCampaignMemberPanel from '@/components/platform/spaces/SpaceCampaignMemberPanel.vue'
import GroupInvitePanel from '@/components/platform/groups/GroupInvitePanel.vue'
import GroupSeasonPanel from '@/components/platform/groups/GroupSeasonPanel.vue'
import GroupPostReportDialog from '@/components/platform/groups/GroupPostReportDialog.vue'
import { useGroupDetailMobile } from '@/composables/useGroupDetailMobile.js'

const {
  CHECKIN_TYPES,
  CHECKIN_TYPE_LABELS,
  group,
  loading,
  errors,
  message,
  messageType,
  tabs,
  activeTab,
  groupTabPath,
  groupsPath,
  joinModeKey,
  inviteCodeFromQuery,
  pendingMemberCount,
  showSpaceManageLink,
  spaceManageEntry,
  spaceManageMembersEntry,
  joinDisabled,
  joinButtonText,
  joining,
  posting,
  checkingIn,
  claimingTask,
  postDraft,
  checkinForm,
  checkinSummary,
  todayTasks,
  claimableCount,
  posts,
  members,
  notices,
  articles,
  articleForm,
  publishingArticle,
  expandedArticleId,
  deletingArticleId,
  submitArticle,
  canDeleteArticle,
  removeArticle,
  toggleArticle,
  upcomingActivities,
  activities,
  polls,
  isPollsSupported,
  seasonRank,
  seasonTop,
  rankingTab,
  loadingRankings,
  rankingEmptyHint,
  rankingDisplayRows,
  expandedCheckinId,
  checkinCommentsById,
  checkinCommentsLoading,
  checkinCommentDraft,
  checkinCommentPosting,
  checkinLikeBusyId,
  currentUserIdNum,
  moderatingMemberId,
  removingMemberId,
  signingActivityId,
  cancellingActivityId,
  showCreateActivity,
  showProposalDialog,
  creatingActivity,
  activityForm,
  expandedPollId,
  pollDetailCache,
  pollPick,
  votingPollId,
  revealingPollId,
  showPollCreate,
  creatingPoll,
  pollForm,
  formatActivityTime,
  formatDateTime,
  activityStatusLabel,
  activityStatusClass,
  canAuditMember,
  canRemoveMember,
  canOwnerToggleAdmin,
  canDeleteComment,
  commentKey,
  expandedPostId,
  feedComments,
  commentsLoading,
  commentDraft,
  commentPosting,
  deletingCommentKey,
  roleChangingMemberId,
  userStore,
  applyJoin,
  submitPost,
  likePost,
  toggleCommentPanel,
  submitComment,
  deleteComment,
  submitCheckin,
  claimTask,
  signUpActivity,
  cancelSignUpActivity,
  cancelActivity,
  submitActivity,
  handleProposeActivity,
  closeProposalDialog,
  onProposalSubmitted,
  setRankingTab,
  rankSlotDisplay,
  toggleLikeCheckin,
  toggleCommentsBlock,
  visibleCommentsFor,
  hasMoreComments,
  showAllCommentsFor,
  submitCheckinComment,
  deleteMyCheckinComment,
  approvePendingMember,
  rejectPendingMember,
  removeMember,
  setMemberAsAdmin,
  unsetMemberAdmin,
  togglePollExpand,
  setPollSinglePick,
  togglePollMultiPick,
  submitPollVote,
  revealPollResults,
  submitPollCreate,
  loadDetail,
  platformEvents,
  weeklyDigest,
  sendingDigest,
  sendWeeklyDigestNow,
  reportDialogOpen,
  reportPostId,
  reportTargetUserId,
  openPostReport,
  activityCheckinOpen,
  activityCheckinCode,
  activityCheckinSubmitting,
  openActivityCheckin,
  submitActivityCheckin,
  checkinCodeBusyId,
  generateActivityCode,
  activityReviewOpen,
  activityReviewCandidates,
  activityReviewLoading,
  activityReviewRatings,
  openActivityReview,
  closeActivityReview,
  submitOneReview
} = useGroupDetailMobile()
</script>

<style scoped>
.gd-mobile {
  padding-bottom: 16px;
}

.gd-capability-banner {
  margin: 8px 12px 0;
}

.gd-capability-banner :deep(.group-capability-banner) {
  margin: 0;
  padding: 8px 10px;
}

.gd-capability-banner :deep(.group-capability-banner p) {
  font-size: 12px;
  line-height: 1.45;
}

.gd-state {
  text-align: center;
  padding: 32px 16px;
  color: var(--lc-subtle, #94a3b8);
  font-size: 14px;
}

.gd-state.err {
  color: #dc2626;
}

.gd-hero {
  background: var(--lc-surface, #fff);
  border-bottom: 1px solid var(--lc-soft, #e8ecf4);
}

.gd-pending-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 10px 12px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  font-size: 13px;
  color: #9a3412;
}

.gd-pending-banner a {
  color: #c2410c;
  font-weight: 600;
  text-decoration: none;
}

.gd-invite-hint {
  margin: 10px 12px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  font-size: 12px;
  color: #065f46;
}

.gd-cover {
  width: 100%;
  height: 88px;
  object-fit: cover;
  display: block;
}

.gd-hero-body {
  padding: 10px 14px 12px;
}

.gd-hero-body h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
  line-height: 1.3;
}

.gd-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--lc-subtle, #64748b);
}

.gd-desc {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.45;
  color: var(--lc-text, #334155);
}

.gd-camp-panel {
  margin: 8px 12px 0;
}

.gd-hero-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.gd-join {
  flex: 1;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--lc-indigo, #4f46e5), var(--lc-violet, #7c3aed));
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.gd-join:disabled {
  opacity: 0.55;
}

.gd-manage {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--lc-indigo, #4f46e5);
  border-radius: 10px;
  color: var(--lc-indigo, #4f46e5);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  background: #fff;
}

.gd-tabs-sticky {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--lc-surface, #fff);
  box-shadow: 0 1px 0 var(--lc-border, #e5e7eb);
}

.gd-digest {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 0 12px 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--lc-surface-muted, #f8fafc);
  font-size: 13px;
}

.gd-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgb(0 0 0 / 40%);
}

.gd-modal-body {
  width: 100%;
  max-width: 480px;
  padding: 16px;
  border-radius: 16px 16px 0 0;
  background: #fff;
}

.gd-modal-body input,
.gd-modal-body select {
  width: 100%;
  margin: 12px 0;
  padding: 10px;
  border: 1px solid var(--lc-border, #e5e7eb);
  border-radius: 8px;
}

.gd-modal-tall {
  max-height: 70vh;
  overflow-y: auto;
}

.gd-review-list {
  list-style: none;
  margin: 0 0 12px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.gd-review-list li {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.gd-review-list select {
  width: auto;
  margin: 0;
}

.gd-review-list em {
  color: var(--lc-text-muted, #64748b);
  font-style: normal;
}

.gd-tabs {
  display: flex;
  gap: 4px;
  padding: 6px 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  background: var(--lc-surface, #fff);
  border-bottom: 1px solid var(--lc-soft, #e8ecf4);
  position: sticky;
  top: 0;
  z-index: 5;
}

.gd-tabs a {
  flex-shrink: 0;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  color: var(--lc-subtle, #64748b);
  border: 1px solid transparent;
}

.gd-tabs a.active {
  background: #eef2ff;
  color: #4338ca;
  border-color: #c7d2fe;
}

.gd-flash {
  margin: 8px 14px 0;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: #059669;
}

.gd-flash.err {
  color: #dc2626;
}

.gd-panel {
  padding: 8px 12px 12px;
}

.gd-card {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.gd-card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.gd-card-row > div {
  min-width: 0;
  flex: 1;
}

.gd-card h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.3;
}

.gd-sub {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--lc-subtle, #64748b);
}

.gd-card-row .gd-link {
  margin-top: 0;
  flex-shrink: 0;
  font-size: 12px;
}

.gd-link {
  display: inline-block;
  margin-top: 6px;
  font-size: 13px;
  font-weight: 700;
  color: var(--lc-indigo, #4f46e5);
  text-decoration: none;
}

.gd-compose {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.gd-compose input,
.gd-compose textarea,
.gd-compose select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--lc-soft, #e2e8f0);
  border-radius: 10px;
  padding: 10px;
  font-size: 14px;
}

.gd-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--lc-subtle, #64748b);
}

.gd-btn {
  height: 40px;
  border-radius: 10px;
  border: 1px solid var(--lc-soft, #cbd5e1);
  background: #fff;
  font-weight: 700;
}

.gd-btn.primary {
  border: none;
  background: var(--lc-indigo, #4f46e5);
  color: #fff;
}

.gd-btn.sm {
  height: 32px;
  padding: 0 12px;
  font-size: 12px;
}

.gd-hint {
  font-size: 13px;
  color: var(--lc-subtle, #64748b);
  margin: 0 0 12px;
}

.gd-hint.ok {
  color: #059669;
  font-weight: 700;
}

.gd-hint.err {
  color: #dc2626;
}

.gd-post {
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.gd-post-head {
  display: flex;
  gap: 10px;
  align-items: center;
}

.gd-post-head img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.gd-post-head strong {
  display: block;
  font-size: 14px;
}

.gd-post-head span {
  font-size: 11px;
  color: #94a3b8;
}

.gd-post-text {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.55;
}

.gd-post-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
}

.gd-post-actions button {
  border: none;
  background: transparent;
  color: var(--lc-subtle, #64748b);
  font-weight: 700;
}

.gd-post-actions button.on {
  color: var(--lc-indigo, #4f46e5);
}

.gd-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.gd-stats div {
  padding: 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
  text-align: center;
}

.gd-stats strong {
  display: block;
  font-size: 18px;
}

.gd-stats span {
  font-size: 10px;
  color: var(--lc-subtle, #94a3b8);
}

.gd-section-title {
  margin: 16px 0 8px;
  font-size: 14px;
}

.gd-checkin-item {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}

.gd-checkin-item p {
  margin: 4px 0 0;
  color: var(--lc-subtle, #64748b);
}

.gd-task {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.gd-task strong {
  display: block;
  font-size: 14px;
}

.gd-task span {
  font-size: 11px;
  color: var(--lc-subtle, #64748b);
}

.gd-tag {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #64748b;
}

.gd-tag.done {
  background: #ecfdf5;
  color: #059669;
}

.gd-member {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.gd-member img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.gd-member span {
  display: block;
  font-size: 11px;
  color: #94a3b8;
}

.gd-notice {
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.gd-notice h3 {
  margin: 0;
  font-size: 15px;
}

.gd-notice p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--lc-subtle, #64748b);
}

.gd-notice time {
  font-size: 11px;
  color: #94a3b8;
}

.gd-article-toggle {
  display: grid;
  gap: 4px;
  width: 100%;
  border: 0;
  padding: 0;
  background: transparent;
  text-align: left;
  color: inherit;
  font: inherit;
  cursor: pointer;
}

.gd-profile {
  margin: 0;
}

.gd-profile div {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}

.gd-profile dt {
  color: var(--lc-subtle, #64748b);
}

.gd-desc-block {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--lc-text, #334155);
}

.gd-notice-title {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--lc-subtle, #64748b);
}

.gd-panel-head {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.gd-form-block {
  padding: 12px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.gd-form-block input[type='text'],
.gd-form-block input[type='number'],
.gd-form-block input[type='datetime-local'] {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--lc-soft, #e2e8f0);
  border-radius: 8px;
  padding: 10px;
  font-size: 14px;
}

.gd-activity,
.gd-poll {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.gd-activity-head,
.gd-poll-head {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.gd-activity-head strong,
.gd-poll-head strong {
  flex: 1 1 100%;
  font-size: 15px;
}

.gd-activity-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--lc-text, #334155);
}

.gd-activity-meta {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--lc-subtle, #64748b);
}

.gd-activity-actions,
.gd-poll-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.gd-pill {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #64748b;
}

.gd-pill.live {
  background: #ecfdf5;
  color: #059669;
}

.gd-pill.wait {
  background: #fef3c7;
  color: #b45309;
}

.gd-pill.ended,
.gd-pill.cancelled {
  background: #f1f5f9;
  color: #94a3b8;
}

.gd-link-btn {
  margin-top: 8px;
  border: none;
  background: transparent;
  color: var(--lc-indigo, #4f46e5);
  font-size: 13px;
  font-weight: 700;
  padding: 0;
}

.gd-poll-detail {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--lc-soft, #e2e8f0);
}

.gd-poll-options {
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
}

.gd-poll-options li {
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
}

.gd-poll-options label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.gd-poll-options em {
  margin-left: auto;
  font-style: normal;
  font-size: 11px;
  color: var(--lc-subtle, #94a3b8);
}

.gd-btn.danger {
  color: #dc2626;
  border-color: #fecaca;
}

.gd-season-stats {
  display: flex;
  gap: 16px;
  margin-top: 10px;
}

.gd-season-stats div {
  flex: 1;
  text-align: center;
  padding: 10px;
  border-radius: 10px;
  background: #f8fafc;
}

.gd-season-stats strong {
  display: block;
  font-size: 18px;
  color: var(--lc-indigo, #4f46e5);
}

.gd-season-stats span {
  font-size: 11px;
  color: var(--lc-subtle, #94a3b8);
}

.gd-season-top {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.gd-season-top li {
  display: grid;
  grid-template-columns: 36px 1fr auto;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}

.gd-season-top strong {
  color: var(--lc-indigo, #4f46e5);
}

.gd-rankings {
  margin: 16px 0;
  padding: 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.gd-rank-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.gd-rank-tabs button {
  flex: 1;
  height: 34px;
  border: 1px solid var(--lc-soft, #e2e8f0);
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
  font-weight: 600;
}

.gd-rank-tabs button.active {
  background: var(--lc-indigo, #4f46e5);
  border-color: transparent;
  color: #fff;
}

.gd-rank-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.gd-rank-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.gd-rank-list li.me {
  background: #eef2ff;
  margin: 0 -8px;
  padding: 10px 8px;
  border-radius: 8px;
}

.gd-rank-list img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.gd-rank-medal {
  width: 28px;
  text-align: center;
  font-size: 14px;
  flex-shrink: 0;
}

.gd-rank-list p {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--lc-subtle, #64748b);
}

.gd-title-pill {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #fef3c7;
  color: #b45309;
  font-size: 10px;
  font-weight: 600;
}

.gd-checkin-rich {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.gd-checkin-meta {
  margin: 4px 0;
  font-size: 12px;
  color: var(--lc-subtle, #64748b);
}

.gd-checkin-rich time {
  font-size: 11px;
  color: #94a3b8;
}

.gd-checkin-social {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.gd-checkin-social button {
  border: none;
  background: transparent;
  font-size: 12px;
  color: var(--lc-subtle, #64748b);
  padding: 0;
}

.gd-checkin-social button.on {
  color: var(--lc-indigo, #4f46e5);
  font-weight: 700;
}

.gd-checkin-comments {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
}

.gd-checkin-comments ul {
  margin: 0 0 8px;
  padding: 0;
  list-style: none;
}

.gd-checkin-comments li {
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid #f8fafc;
}

.gd-checkin-comments li strong {
  margin-right: 6px;
}

.gd-comment-form {
  display: flex;
  gap: 8px;
}

.gd-comment-form input {
  flex: 1;
  border: 1px solid var(--lc-soft, #e2e8f0);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
}

.gd-member-row {
  flex-wrap: wrap;
  align-items: flex-start;
}

.gd-member-main {
  flex: 1;
  min-width: 0;
}

.gd-member-head {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.gd-member-meta {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--lc-subtle, #94a3b8);
}

.gd-member-reason {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--lc-text, #475569);
  line-height: 1.4;
}

.gd-post-comments {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
}

.gd-comment-list {
  margin: 0 0 10px;
  padding: 0;
  list-style: none;
}

.gd-comment-list li {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px solid #f8fafc;
}

.gd-comment-list img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.gd-comment-list li div {
  flex: 1;
  min-width: 0;
}

.gd-comment-list li strong {
  font-size: 12px;
  margin-right: 6px;
}

.gd-comment-list li span {
  font-size: 10px;
  color: #94a3b8;
}

.gd-comment-list li p {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.4;
}

.gd-member-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  width: 100%;
  margin-top: 8px;
  padding-left: 48px;
}

.gd-pill.pending {
  background: #fef3c7;
  color: #b45309;
}

.gd-state.sm {
  padding: 16px;
  font-size: 13px;
}
</style>
