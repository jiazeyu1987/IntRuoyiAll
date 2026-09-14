<template>
  <section class="showroom-version-diff-panel" data-version-center-diff>
    <section class="showroom-version-diff-panel__surface">
      <div class="showroom-version-diff-panel__section-head">
        <div>
          <h3 class="showroom-version-diff-panel__title">当前内容 Diff</h3>
          <p class="showroom-version-diff-panel__tip">
            固定比较“所选版本 vs 当前内容版本”，不混用当前线上版本。
          </p>
        </div>
        <el-tag v-if="currentContentVersion" type="primary">
          当前内容 V{{ currentContentVersion.revisionNo }}
        </el-tag>
      </div>

      <div v-if="fieldDiffs.length === 0" class="showroom-version-diff-panel__empty">
        当前没有可展示的字段差异。
      </div>

      <article
        v-for="fieldDiff in fieldDiffs"
        :key="fieldDiff.fieldCode"
        class="showroom-version-diff-panel__diff-card"
      >
        <div class="showroom-version-diff-panel__diff-head">
          <strong>{{ fieldDiff.label }}</strong>
          <el-tag :type="fieldDiff.changed ? 'warning' : 'success'">
            {{ fieldDiff.changed ? '已变更' : '一致' }}
          </el-tag>
        </div>
        <p class="showroom-version-diff-panel__diff-label">{{ fieldDiff.labelEn }}</p>
        <div class="showroom-version-diff-panel__diff-columns">
          <div>
            <span class="showroom-version-diff-panel__column-label">所选版本</span>
            <p>中文：{{ fieldDiff.selectedValueZh || '未填写' }}</p>
            <p>English：{{ fieldDiff.selectedValueEn || 'Not filled' }}</p>
          </div>
          <div>
            <span class="showroom-version-diff-panel__column-label">当前内容</span>
            <p>中文：{{ fieldDiff.currentContentValueZh || '未填写' }}</p>
            <p>English：{{ fieldDiff.currentContentValueEn || 'Not filled' }}</p>
          </div>
        </div>
      </article>
    </section>

    <section v-if="showCurrentPublicSummary" class="showroom-version-diff-panel__surface">
      <div class="showroom-version-diff-panel__section-head">
        <h4>当前线上摘要</h4>
        <el-tag type="success">V{{ currentPublicVersion?.revisionNo }}</el-tag>
      </div>
      <p class="showroom-version-diff-panel__summary-text">
        当前线上版本和当前内容版本不一致，因此这里单独保留 live 摘要，避免和右侧主 diff 混淆。
      </p>
      <p class="showroom-version-diff-panel__summary-text">
        {{ currentPublicVersion?.title || '当前线上标题未记录' }}
      </p>
      <p class="showroom-version-diff-panel__summary-text">
        {{ currentPublicVersion?.titleEn || 'Current public English title not recorded' }}
      </p>
    </section>

    <section class="showroom-version-diff-panel__surface">
      <div class="showroom-version-diff-panel__section-head">
        <h4>当前 release 摘要</h4>
        <el-tag :type="currentRelease ? 'info' : 'warning'">
          {{ currentRelease ? currentRelease.releaseId : '未发布' }}
        </el-tag>
      </div>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="Manifest Hash">
          {{ currentRelease?.manifestHash || '未记录' }}
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">
          {{ formatDateTimeValue(currentRelease?.publishedAt, '未发布') }}
        </el-descriptions-item>
        <el-descriptions-item label="公司线上 Revision">
          {{ currentRelease ? `#${currentRelease.companyRevisionId}` : '未记录' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRelease?.productCurrentReleaseRevisionId !== null" label="产品线上 Revision">
          {{
            currentRelease?.productCurrentReleaseRevisionId
              ? `#${currentRelease.productCurrentReleaseRevisionId}`
              : '未记录'
          }}
        </el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="showroom-version-diff-panel__surface">
      <div class="showroom-version-diff-panel__section-head">
        <div>
          <h4>发布为当前线上版本</h4>
          <p class="showroom-version-diff-panel__tip">
            这是一步到位发布，但最终会触发全局 showroom release 重建。
          </p>
        </div>
      </div>

      <el-alert
        v-if="disabledReason"
        :closable="false"
        show-icon
        type="warning"
        :title="disabledReason"
      />

      <el-alert
        v-if="republishReadiness.blockers.length > 0"
        :closable="false"
        show-icon
        type="error"
        title="当前存在 republish blocker"
      >
        <template #default>
          <ul class="showroom-version-diff-panel__blockers">
            <li v-for="blocker in republishReadiness.blockers" :key="`${blocker.scope}-${blocker.blockerCode}`">
              {{ formatVersionCenterBlocker(blocker) }}
            </li>
          </ul>
        </template>
      </el-alert>

      <el-button
        type="primary"
        :disabled="Boolean(disabledReason)"
        :loading="republishing"
        @click="emit('republish')"
      >
        发布为当前线上版本
      </el-button>
    </section>
  </section>
</template>

<script setup lang="ts">
import type {
  VersionCenterFieldDiffVO,
  VersionCenterPermissionVO,
  VersionCenterReleaseSummary,
  VersionCenterRepublishReadiness,
  VersionCenterSnapshotVO
} from './contracts'
import { formatVersionCenterBlocker } from './contracts'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'VersionDiffPanel' })

const props = defineProps<{
  fieldDiffs: VersionCenterFieldDiffVO[]
  currentContentVersion: VersionCenterSnapshotVO | null
  currentPublicVersion: VersionCenterSnapshotVO | null
  currentRelease: VersionCenterReleaseSummary | null
  permissions: VersionCenterPermissionVO
  republishReadiness: VersionCenterRepublishReadiness
  republishing: boolean
  interactionsDisabled: boolean
}>()

const emit = defineEmits<{
  republish: []
}>()

const showCurrentPublicSummary = computed(
  () =>
    Boolean(
      props.currentPublicVersion &&
        props.currentContentVersion &&
        props.currentPublicVersion.revisionId !== props.currentContentVersion.revisionId
    )
)

const disabledReason = computed(() => {
  if (!props.permissions.canRepublish) {
    return props.permissions.republishDisabledReason || '当前用户没有 republish 权限'
  }
  if (props.interactionsDisabled) {
    return '版本切换中，当前暂不可执行发布操作'
  }
  if (!props.republishReadiness.ready) {
    return '当前存在 blocker，无法执行一步到位发布'
  }
  return ''
})
</script>

<style scoped>
.showroom-version-diff-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.showroom-version-diff-panel__surface {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-version-diff-panel__section-head,
.showroom-version-diff-panel__diff-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.showroom-version-diff-panel__title,
.showroom-version-diff-panel__section-head h4,
.showroom-version-diff-panel__diff-head strong {
  margin: 0;
  color: #172033;
}

.showroom-version-diff-panel__tip,
.showroom-version-diff-panel__summary-text,
.showroom-version-diff-panel__diff-label,
.showroom-version-diff-panel__column-label,
.showroom-version-diff-panel__blockers {
  margin: 0;
  color: #4b5563;
  font-size: 0.82rem;
  line-height: 1.5;
}

.showroom-version-diff-panel__diff-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.showroom-version-diff-panel__diff-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.showroom-version-diff-panel__diff-columns p {
  margin: 4px 0 0;
  color: #263247;
  white-space: pre-wrap;
}

.showroom-version-diff-panel__empty {
  color: #4b5563;
  font-size: 0.88rem;
}

.showroom-version-diff-panel__blockers {
  padding-left: 18px;
}

@media (max-width: 960px) {
  .showroom-version-diff-panel__diff-columns {
    grid-template-columns: 1fr;
  }
}
</style>
