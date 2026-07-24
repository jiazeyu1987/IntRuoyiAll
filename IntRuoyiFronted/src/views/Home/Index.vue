<template>
  <div class="home-welcome-page">
    <section class="welcome-shell">
      <div class="welcome-header">
        <p class="welcome-kicker">首页</p>
        <h1>欢迎使用 IntRuoyi 管理后台</h1>
        <p class="welcome-subtitle">开始使用前，请从左侧菜单进入对应业务模块。</p>
        <p class="welcome-user">当前登录：{{ username }}</p>
      </div>

      <div class="welcome-meta">
        <div class="meta-item">
          <span class="meta-label">当前日期</span>
          <strong>{{ currentDateLabel }}</strong>
        </div>
        <div class="meta-item">
          <span class="meta-label">系统状态</span>
          <strong>系统已就绪</strong>
        </div>
        <div class="meta-item">
          <span class="meta-label">操作方式</span>
          <strong>通过左侧菜单进入业务页面</strong>
        </div>
      </div>

      <div class="showroom-entry">
        <div class="content-block showroom-entry__card">
          <p class="showroom-entry__kicker">数字展厅</p>
          <h2>展厅后台入口</h2>
          <p>从主页直接进入展厅后台管理页，无需手动输入路径。</p>
          <div class="showroom-entry__actions">
            <el-button type="primary" @click="openShowroomAdmin">进入展厅后台</el-button>
          </div>
        </div>
      </div>

      <div class="welcome-content">
        <div class="content-block">
          <h2>开始使用</h2>
          <p>建议先确认当前用户权限、所属部门和业务入口是否正确，再进入具体模块处理数据。</p>
        </div>

        <div class="content-block">
          <h2>使用提示</h2>
          <ul>
            <li>MES、ERP、DCC 等模块请按实际业务流程逐项操作。</li>
            <li>遇到报错时请保留页面提示和操作路径，便于快速排查。</li>
            <li>需要新增权限或菜单时，请联系系统管理员处理。</li>
          </ul>
        </div>
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'Index' })

const userStore = useUserStore()
const router = useRouter()

const username = computed(() => userStore.getUser.nickname || '管理员')
const currentDateLabel = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const openShowroomAdmin = () => {
  router.push('/showroom/company')
}
</script>

<style lang="scss" scoped>
.home-welcome-page {
  min-height: calc(100vh - 120px);
  padding: 16px;
  background: #f7f9fc;
}

.welcome-shell {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 100%;
  padding: 24px;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.welcome-header {
  padding-bottom: 20px;
  border-bottom: 1px solid #edf1f6;
}

.welcome-kicker {
  margin: 0 0 8px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.welcome-header h1 {
  margin: 0;
  color: #172033;
  font-size: 28px;
  line-height: 1.25;
}

.welcome-subtitle {
  margin: 12px 0 0;
  color: #263247;
  font-size: 14px;
  line-height: 1.7;
}

.welcome-user {
  margin: 8px 0 0;
  color: #4b5563;
  font-size: 13px;
}

.welcome-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  background: #fafcff;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.meta-label {
  color: #4b5563;
  font-size: 12px;
}

.meta-item strong {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
}

.welcome-content {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.showroom-entry {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
}

.showroom-entry__card {
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.showroom-entry__kicker {
  margin: 0 0 8px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.showroom-entry__card p {
  margin-bottom: 0;
}

.showroom-entry__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.content-block {
  padding: 20px;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

.content-block h2 {
  margin: 0 0 10px;
  color: #172033;
  font-size: 16px;
  font-weight: 600;
}

.content-block p,
.content-block li {
  color: #4b5563;
  font-size: 14px;
  line-height: 1.75;
}

.content-block p {
  margin: 0;
}

.content-block ul {
  margin: 0;
  padding-left: 20px;
}

.content-block li + li {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .welcome-meta,
  .welcome-content {
    grid-template-columns: 1fr;
  }

  .welcome-shell {
    padding: 18px;
  }

  .welcome-header h1 {
    font-size: 24px;
  }
}
</style>
