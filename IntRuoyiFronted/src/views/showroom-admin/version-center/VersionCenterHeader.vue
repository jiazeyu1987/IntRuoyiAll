<template>
  <section class="showroom-version-center-header">
    <div class="showroom-version-center-header__lead">
      <el-button link type="primary" @click="emit('back')">返回</el-button>
      <div>
        <h2 class="showroom-version-center-header__title">
          {{ targetType === 'COMPANY' ? '公司版本中心' : '产品版本中心' }}
        </h2>
        <p class="showroom-version-center-header__subtitle">
          {{ title }}
          <template v-if="titleEn"> / {{ titleEn }}</template>
        </p>
      </div>
    </div>

    <div class="showroom-version-center-header__summary">
      <article class="showroom-version-center-header__summary-card">
        <span class="showroom-version-center-header__summary-label">当前内容版本</span>
        <strong>{{ currentContentVersion ? `V${currentContentVersion.revisionNo}` : '未解析' }}</strong>
        <span class="showroom-version-center-header__summary-meta">
          {{ currentContentVersion?.title || '当前内容版本暂不可读' }}
        </span>
      </article>
      <article class="showroom-version-center-header__summary-card">
        <span class="showroom-version-center-header__summary-label">当前线上版本</span>
        <strong>{{ currentPublicVersion ? `V${currentPublicVersion.revisionNo}` : '未上线' }}</strong>
        <span class="showroom-version-center-header__summary-meta">
          {{ currentPublicVersion?.title || '当前没有进入 live release 的版本' }}
        </span>
      </article>
      <article class="showroom-version-center-header__summary-card">
        <span class="showroom-version-center-header__summary-label">当前 release</span>
        <strong>{{ currentRelease ? currentRelease.releaseId : '未发布' }}</strong>
        <span class="showroom-version-center-header__summary-meta">
          {{
            currentRelease
              ? `${currentRelease.manifestHash} · ${currentRelease.publishedAt}`
              : '当前没有 active showroom release'
          }}
        </span>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import type {
  VersionCenterReleaseSummary,
  VersionCenterSnapshotVO,
  VersionCenterTargetType
} from './contracts'

defineOptions({ name: 'VersionCenterHeader' })

defineProps<{
  targetType: VersionCenterTargetType
  title: string
  titleEn: string | null
  currentContentVersion: VersionCenterSnapshotVO | null
  currentPublicVersion: VersionCenterSnapshotVO | null
  currentRelease: VersionCenterReleaseSummary | null
}>()

const emit = defineEmits<{
  back: []
}>()
</script>

<style scoped>
.showroom-version-center-header {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-version-center-header__lead {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.showroom-version-center-header__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-version-center-header__subtitle,
.showroom-version-center-header__summary-meta {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.88rem;
  line-height: 1.5;
}

.showroom-version-center-header__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.showroom-version-center-header__summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.showroom-version-center-header__summary-label {
  color: #4b5563;
  font-size: 0.82rem;
}

@media (max-width: 960px) {
  .showroom-version-center-header__summary {
    grid-template-columns: 1fr;
  }
}
</style>
