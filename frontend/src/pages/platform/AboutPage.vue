<template>
  <!--
    Deprecated page:
    /about has been redirected to /feedback in router/modules/platform.routes.js.
    Keep this file for compatibility/reference only, do not add new entry links here.
  -->
  <section class="platform-page module-page">
    <section class="platform-card module-hero">
      <p class="platform-kicker">About Platform</p>
      <h1 class="platform-title">关于 Love Cube 平台</h1>
      <p class="platform-subtitle">
        Love Cube 围绕内容、活动、社交和服务构建，联谊只是其中一个模块。
      </p>
    </section>

    <section class="module-grid module-grid-3">
      <article class="platform-card module-card">
        <h3 class="module-card-title">平台定位</h3>
        <p class="module-card-desc">“内容 + 活动 + 关系连接”的综合型平台，不局限于单一场景</p>
      </article>
      <article class="platform-card module-card">
        <h3 class="module-card-title">模块体系</h3>
        <p class="module-card-desc">包含平台动精选内容活动中心联谊功能平台个人中心和治理系统</p>
      </article>
      <article class="platform-card module-card">
        <h3 class="module-card-title">运营目标</h3>
        <p class="module-card-desc">通过可信信息、持续活动与高质量内容，提升用户活跃和长期留存</p>
      </article>
    </section>

    <section class="platform-card module-block">
      <h2 class="platform-section-title">平台模块导航</h2>
      <div class="module-chip-row">
        <router-link class="module-chip" to="/announcements">平台动</router-link>
        <router-link class="module-chip" to="/articles">精内</router-link>
        <router-link class="module-chip" to="/events">活动中心</router-link>
        <router-link class="module-chip" to="/fellowship-intro">联谊</router-link>
        <router-link class="module-chip" to="/me">平台个人中心</router-link>
      </div>
    </section>
  </section>
</template>


