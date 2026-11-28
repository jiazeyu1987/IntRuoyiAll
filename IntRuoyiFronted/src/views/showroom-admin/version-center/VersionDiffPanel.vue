<template>
  <section class="showroom-version-diff-panel" data-version-center-diff>
    <section class="showroom-version-diff-panel__surface">
      <div class="showroom-version-diff-panel__section-head">
        <div>
          <h3 class="showroom-version-diff-panel__title">当前内容差异</h3>
          <p class="showroom-version-diff-panel__tip"> 比较所选版本与当前内容版本。 </p>
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
        <details v-if="fieldDiff.labelEn"
          ><summary>英文字段名称</summary>{{ fieldDiff.labelEn }}</details
        >
        <div class="showroom-version-diff-panel__diff-columns">
          <div>
            <span class="showroom-version-diff-panel__column-label">所选版本</span>
            <p>中文：{{ fieldDiff.selectedValueZh || '未填写' }}</p>
            <p>英文：{{ fieldDiff.selectedValueEn || '未填写' }}</p>
          </div>
          <div>
            <span class="showroom-version-diff-panel__column-label">当前内容</span>
            <p>中文：{{ fieldDiff.currentContentValueZh || '未填写' }}</p>
            <p>英文：{{ fieldDiff.currentContentValueEn || '未填写' }}</p>
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
        当前线上版本与当前内容版本不同。以下为线上版本摘要。
      </p>
      <p class="showroom-version-diff-panel__summary-text">
        {{ currentPublicVersion?.title || '当前线上标题未记录' }}
      </p>
      <p class="showroom-version-diff-panel__summary-text">
        {{ currentPublicVersion?.titleEn || '线上版本的英文标题未记录' }}
      </p>
    </section>

    <section class="showroom-version-diff-panel__surface">
      <div class="showroom-version-diff-panel__section-head">
        <h4>当前发布信息</h4>
        <el-tag :type="currentRelease ? 'info' : 'warning'">
          {{ currentRelease ? '已有发布记录' : '未发布' }}
        </el-tag>
      </div>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="发布时间">
          {{ formatDateTimeValue(currentRelease?.publishedAt, '未发布') }}
        </el-descriptions-item>
      </el-descriptions>
      <details v-if="currentRelease"
        ><summary>发布标识与校验信息</summary> <p>发布标识：{{ currentRelease.releaseId }}</p
        ><p>清单校验码：{{ currentRelease.manifestHash }}</p>
        <p>公司线上修订编号：{{ currentRelease.companyRevisionId }}</p>
        <p v-if="currentRelease.productCurrentReleaseRevisionId != null"
          >产品线上修订编号：{{ currentRelease.productCurrentReleaseRevisionId }}</p
        >
      </details>
    </section>

    <section class="showroom-version-diff-panel__surface">
      <div class="showroom-version-diff-panel__section-head">
        <div>
          <h4>发布为当前线上版本</h4>
          <p class="showroom-version-diff-panel__tip">
            发布后将更新当前线上版本，并重新构建整个展厅发布包。
          </p>
        </div>
      </div>

      <details v-if="permissions.republishDisabledReason"
        ><summary>不可发布原因详情</summary>{{ permissions.republishDisabledReason }}</details
      >
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
        title="存在发布阻塞项"
      >
        <template #default>
          <ul class="showroom-version-diff-panel__blockers">
            <li
              v-for="blocker in republishReadiness.blockers"
              :key="`${blocker.scope}-${blocker.blockerCode}`"
            >
              <p>{{ blockerMessage(blocker) }}</p>
              <details
                ><summary>阻塞项技术详情</summary
                ><p>{{ formatVersionCenterBlocker(blocker) }}</p></details
              >
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
  VersionCenterBlocker,
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

const showCurrentPublicSummary = computed(() =>
  Boolean(
    props.currentPublicVersion &&
      props.currentContentVersion &&
      props.currentPublicVersion.revisionId !== props.currentContentVersion.revisionId
  )
)

const technicalBlockerLabels: Record<string, string> = {
  SHOWROOM_TARGET_NOT_FOUND: '未找到所选公司、产品或版本记录，无法发布，请核对版本记录。',
  SHOWROOM_VERSION_BUNDLE_NOT_FOUND: '所选版本的完整内容记录缺失或与当前对象不一致，无法发布。',
  SHOWROOM_VERSION_COMPANY_SNAPSHOT_MISSING: '公司版本缺少名称或类型记录，无法发布。',
  PRODUCT_COVER_MISSING: '所选产品版本缺少封面图片，无法发布。',
  PRODUCT_NARRATION_AUDIO_MISSING: '所选版本的讲解语音缺失或不可读取，无法发布。',
  SHOWROOM_VERSION_CENTER_NOT_READY: '版本记录尚未就绪，请核对讲解语音是否存在重复发布记录。'
}

const blockerMessage = (blocker: VersionCenterBlocker) => {
  // 已有中文业务原因直接保留，技术错误按服务端明确的错误码说明。
  if (/[\u3400-\u9fff]/.test(blocker.message)) {
    return blocker.message
  }
  return technicalBlockerLabels[blocker.blockerCode] || blocker.message
}

const disabledReason = computed(() => {
  if (!props.permissions.canRepublish) {
    const reason = props.permissions.republishDisabledReason
    if (reason && /[\u3400-\u9fff]/.test(reason)) {
      return reason
    }
    const blocker = props.republishReadiness.blockers.find((item) =>
      Boolean(technicalBlockerLabels[item.blockerCode])
    )
    return blocker ? blockerMessage(blocker) : reason || '当前用户没有发布权限'
  }
  if (props.interactionsDisabled) {
    return '版本切换中，当前暂不可执行发布操作'
  }
  if (!props.republishReadiness.ready) {
    return '请先处理发布阻塞项，再发布此版本'
  }
  return ''
})
</script>

<style scoped>
.showroom-version-diff-panel details {
  overflow-wrap: anywhere;
}
.showroom-version-diff-panel summary {
  cursor: pointer;
  color: var(--el-text-color-secondary);
}
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
