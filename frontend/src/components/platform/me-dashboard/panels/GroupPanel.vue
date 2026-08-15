<template>
  <section class="card" aria-label="我的团体">
    <div class="top-row">
      <div class="info-side">
        <div class="badge" aria-hidden="true">团</div>
        <div class="info">
          <div class="info-top">
            <strong class="gname">{{ groupInfo.name }}</strong>
            <span class="role">{{ groupInfo.role }}</span>
          </div>
          <div class="meta">
            <span>成员 {{ groupInfo.members }} 人</span>
            <span class="sep" aria-hidden="true"></span>
            <span>活跃度：{{ groupInfo.activity }}</span>
            <span class="sep" aria-hidden="true"></span>
            <span>本周贡献 {{ groupInfo.weekExp }} 经验</span>
          </div>
        </div>
      </div>

      <div class="action-side">
        <router-link class="btn btn-enter" :to="enterGroupTo">进入团体</router-link>
        <router-link class="btn btn-post" :to="noticeTo">发布公告</router-link>
        <router-link class="btn btn-member" :to="membersTo">成员管理</router-link>
        <router-link class="more-link" :to="myGroupsPath()">全部团体 ></router-link>
      </div>
    </div>

    <div class="rank-section">
      <div class="rank-head">
        <h3>团体活跃榜（本周）</h3>
        <router-link class="rank-more" :to="groupsPath()">查看完整榜单 ></router-link>
      </div>

      <ol class="rank-grid">
        <li v-for="item in groupRanking" :key="item.name" class="rank-card">
          <span class="medal" :class="`medal-${item.rank}`">{{ item.rank }}</span>
          <div class="rank-info">
            <strong>{{ item.name }}</strong>
            <small>活跃度 {{ item.activity }}</small>
          </div>
        </li>
      </ol>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { resolveSpaceManageEntry } from '@/api/groups.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'

const props = defineProps({
  groupInfo: { type: Object, required: true },
  groupRanking: { type: Array, required: true },
})

const { groupsPath, myGroupsPath } = usePlatformPath()

const groupId = computed(() => String(props.groupInfo?.id || '').trim())
const canManage = computed(() => Boolean(props.groupInfo?.canManage && groupId.value))

const enterGroupTo = computed(() => (groupId.value ? groupsPath(groupId.value) : myGroupsPath()))
const noticeTo = computed(() => (
  canManage.value
    ? resolveSpaceManageEntry(groupId.value, { tab: 'notices' })
    : myGroupsPath()
))
const membersTo = computed(() => (
  canManage.value
    ? resolveSpaceManageEntry(groupId.value, { tab: 'members' })
    : myGroupsPath()
))
</script>

<style scoped>
.card {
  background: var(--lc-surface);
  border: 1px solid rgba(79, 70, 229, 0.12);
  border-radius: 12px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.055);
  padding: 22px 24px;
}

.top-row {
  display: grid;
  grid-template-columns: 390px minmax(0, 1fr);
  gap: 30px;
  align-items: center;
}

.info-side {
  display: flex;
  align-items: center;
  gap: 16px;
}

.badge {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--lc-indigo), var(--lc-violet));
  color: var(--lc-surface);
  font-weight: 900;
  font-size: 20px;
  flex: 0 0 auto;
  box-shadow: 0 10px 20px rgba(79, 70, 229, 0.22);
}

.info {
  min-width: 0;
}

.info-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.gname {
  font-size: 18px;
  font-weight: 900;
  color: var(--lc-text);
}

.role {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(79, 70, 229, 0.10);
  color: var(--lc-indigo);
  font-size: 12px;
  font-weight: 800;
}

.meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: var(--lc-muted-light);
  font-size: 13px;
}

.sep {
  width: 1px;
  height: 12px;
  background: rgba(203, 213, 225, 0.9);
  flex: 0 0 auto;
}

.action-side {
  display: grid;
  grid-template-columns: repeat(4, minmax(110px, 1fr));
  align-items: center;
  gap: 10px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  transition: opacity 0.15s;
}

.btn:hover {
  opacity: 0.82;
}

.btn-enter {
  border: 0;
  background: linear-gradient(135deg, var(--lc-indigo), var(--lc-violet));
  color: var(--lc-surface);
}

.btn-post,
.btn-member {
  border: 1px solid var(--lc-soft-alt);
  background: var(--lc-surface);
  color: var(--lc-text);
}

.more-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  border: 1px solid var(--lc-soft-alt);
  border-radius: 10px;
  color: var(--lc-muted);
  font-size: 12px;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.rank-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--lc-soft-alt);
}

.rank-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.rank-head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 900;
  color: var(--lc-text);
}

.rank-more {
  color: var(--lc-muted);
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.rank-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.rank-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border: 1px solid var(--lc-soft-alt);
  border-radius: 14px;
  background: linear-gradient(180deg, var(--lc-bg), var(--lc-surface));
  min-height: 62px;
  transition: background 0.12s;
}

.rank-card:hover {
  background: var(--lc-bg);
}

.medal {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 900;
  font-size: 13px;
  flex: 0 0 auto;
  color: #9a6700;
  background: #fff1c2;
}

.medal-2 { color: var(--lc-muted-light); background: var(--lc-soft-alt); }
.medal-3 { color: var(--lc-orange); background: var(--lc-orange-light); }

.rank-info {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.rank-info strong {
  font-size: 13px;
  font-weight: 800;
  color: var(--lc-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-info small {
  font-size: 12px;
  color: var(--lc-muted-light);
}

@media (max-width: 1180px) {
  .top-row,
  .action-side {
    grid-template-columns: 1fr;
  }
}
</style>
