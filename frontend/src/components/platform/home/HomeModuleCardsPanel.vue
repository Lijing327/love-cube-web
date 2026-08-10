<template>
  <div class="content-panel">
    <div class="panel-head">
      <h2>模块推荐</h2>
      <router-link to="/modules">查看更多 →</router-link>
    </div>
    <div class="people-grid">
      <article v-for="module in moduleCards" :key="module.title" class="person-card">
        <div class="person-photo" :class="module.tone">
          <span>{{ module.icon }}</span>
          <i></i>
        </div>
        <strong>{{ module.title }}</strong>
        <small>{{ module.meta }}</small>
        <p>{{ module.desc }}</p>
        <router-link :to="module.to">{{ module.actionText }}</router-link>
      </article>
    </div>
  </div>
</template>

<script setup>
defineProps({
  moduleCards: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.content-panel {
  min-width: 0;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0;
  color: var(--lc-text);
  font-size: 22px;
}

.panel-head a {
  color: var(--lc-blue);
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.people-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.person-card {
  display: grid;
  justify-items: center;
  gap: 7px;
  min-height: 206px;
  padding: 14px 12px;
  text-align: center;
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  background: var(--lc-surface);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.045);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.person-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.1);
}

.person-photo {
  position: relative;
  display: grid;
  place-items: center;
  width: 90px;
  height: 100px;
  border-radius: 8px;
}

.person-photo span {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  color: var(--lc-surface);
  background: linear-gradient(135deg, var(--tone), var(--lc-subtle));
  font-size: 24px;
  font-weight: 900;
}

.person-photo i {
  position: absolute;
  right: 12px;
  bottom: 17px;
  width: 9px;
  height: 9px;
  border: 2px solid var(--lc-surface);
  border-radius: 50%;
  background: #22c55e;
}

.person-card strong {
  color: var(--lc-text);
  font-size: 15px;
}

.person-card small,
.person-card p {
  color: var(--lc-muted-light);
  font-size: 12px;
  line-height: 1.45;
}

.person-card p {
  margin: 0;
  min-height: 34px;
}

.person-card a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  height: 30px;
  border: 1px solid currentColor;
  border-radius: 8px;
  color: var(--tone);
  font-size: 12px;
  font-weight: 800;
  text-decoration: none;
}

.tone-pink { --tone: var(--lc-pink); --tone-bg: var(--lc-pink-light); color: var(--lc-pink); background: linear-gradient(180deg, #fff7fb, var(--lc-surface)); }
.tone-violet { --tone: var(--lc-indigo); --tone-bg: var(--lc-indigo-light); color: var(--lc-indigo); background: linear-gradient(180deg, var(--lc-indigo-soft), var(--lc-surface)); }
.tone-blue { --tone: var(--lc-blue); --tone-bg: var(--lc-blue-light); color: var(--lc-blue-mid); background: linear-gradient(180deg, var(--lc-blue-light), var(--lc-surface)); }
.tone-orange { --tone: var(--lc-orange); --tone-bg: var(--lc-orange-light); color: #c2410c; background: linear-gradient(180deg, var(--lc-orange-light), var(--lc-surface)); }
.tone-green { --tone: var(--lc-green); --tone-bg: var(--lc-emerald-light); color: #15803d; background: linear-gradient(180deg, var(--lc-emerald-light), var(--lc-surface)); }
.tone-purple { --tone: var(--lc-purple); --tone-bg: var(--lc-indigo-soft); color: #6d28d9; background: linear-gradient(180deg, var(--lc-indigo-soft), var(--lc-surface)); }

@media (max-width: 520px) {
  .people-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .person-card {
    min-height: 0;
    padding: 10px 8px;
    gap: 4px;
  }

  .person-photo {
    width: 64px;
    height: 72px;
  }

  .person-photo span {
    width: 48px;
    height: 48px;
    font-size: 18px;
  }

  .person-card p {
    min-height: 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
</style>
