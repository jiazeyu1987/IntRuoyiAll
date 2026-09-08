<template>
  <section data-testid="dcc-detail-publication-followup" aria-labelledby="dcc-followup-title">
    <header class="followup-heading">
      <div>
        <h2 id="dcc-followup-title">发布后续</h2>
        <p v-if="followup">{{ followup.fileNumber }} / {{ followup.versionNo }}</p>
      </div>
      <el-tag v-if="followup" :type="batchTagType">{{ batchStatusText }}</el-tag>
    </header>

    <el-alert
      v-if="followupLoadError"
      role="alert"
      type="error"
      :closable="false"
      show-icon
      title="发布后续加载失败"
      :description="followupLoadError"
    />
    <div v-loading="followupLoading" class="followup-body">
      <el-empty v-if="!followupLoading && !followupLoadError && !followup" description="暂无发布后续记录" />
      <template v-else-if="followup">
        <section class="followup-section" aria-labelledby="dcc-visibility-title">
          <h3 id="dcc-visibility-title">业务可见范围</h3>
          <el-table :data="followup.visibilityRules" empty-text="暂无可见范围快照" size="small">
            <el-table-column label="来源" min-width="180" prop="sourceSummary" />
            <el-table-column label="类型" width="190">
              <template #default="{ row }">{{ visibilitySourceLabel(row.sourceType) }}</template>
            </el-table-column>
            <el-table-column label="解析用户" min-width="260">
              <template #default="{ row }">
                <span v-if="!row.users.length">无解析用户</span>
                <span v-else>{{ row.users.map(userText).join('；') }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="180">
              <template #default="{ row }">{{ visibilityResolutionStatusLabel(row.resolutionStatus) }}</template>
            </el-table-column>
          </el-table>
        </section>

        <section class="followup-section" aria-labelledby="dcc-notification-title">
          <h3 id="dcc-notification-title">通知发送</h3>
          <el-table :data="followup.notificationDeliveries" empty-text="无通知收件人" size="small">
            <el-table-column label="收件人" min-width="150">
              <template #default="{ row }">{{ row.userName || row.userId }}</template>
            </el-table-column>
            <el-table-column label="部门" min-width="130" prop="deptName" />
            <el-table-column label="收件原因" min-width="240">
              <template #default="{ row }">{{ row.reasonSummaries.join('；') }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">{{ notificationStatusLabel(row.status) }}</template>
            </el-table-column>
            <el-table-column label="尝试次数" width="100" prop="attemptCount" />
          </el-table>
        </section>

        <section class="followup-section" aria-labelledby="dcc-impact-title">
          <h3 id="dcc-impact-title">影响评估</h3>
          <el-table :data="followup.impactTasks" empty-text="无需评估关联文件" size="small">
            <el-table-column label="相关文件" min-width="220">
              <template #default="{ row }">
                {{ row.relatedFileName || '-' }}（{{ row.relatedFileNumber || '-' }}）
              </template>
            </el-table-column>
            <el-table-column label="关系方向" min-width="150">
              <template #default="{ row }">{{ row.relationDirections.map(relationDirectionLabel).join(' / ') }}</template>
            </el-table-column>
            <el-table-column label="负责人" min-width="130" prop="assigneeUserName" />
            <el-table-column label="任务状态" width="120">
              <template #default="{ row }">{{ impactTaskStatusLabel(row.taskStatus) }}</template>
            </el-table-column>
            <el-table-column label="评估结论" min-width="150">
              <template #default="{ row }">{{ row.decision ? impactDecisionLabel(row.decision) : '尚未提交' }}</template>
            </el-table-column>
            <el-table-column label="升版跟踪" width="140">
              <template #default="{ row }">{{ revisionTrackingStatusLabel(row.revisionTrackingStatus) }}</template>
            </el-table-column>
          </el-table>
        </section>
        <section
          class="followup-section"
          data-testid="dcc-detail-publication-timeline"
          aria-labelledby="dcc-publication-timeline-title"
        >
          <h3 id="dcc-publication-timeline-title">完整时间线</h3>
          <PublicationFollowupTimeline :timeline="followup.timeline" />
        </section>
      </template>
    </div>
  </section>
</template>

<script lang="ts" setup>
import {
  getPublicationFollowupByFile,
  type DccPublicationFollowupVO,
  type DccPublicationVisibilityUserVO
} from '@/api/dcc/controlledFile/publicationFollowup'
import {
  batchStatusLabel,
  impactDecisionLabel,
  impactTaskStatusLabel,
  notificationStatusLabel,
  relationDirectionLabel,
  revisionTrackingStatusLabel,
  visibilityResolutionStatusLabel,
  visibilitySourceLabel
} from '../shared/publicationFollowupPresentation'
import PublicationFollowupTimeline from '../shared/PublicationFollowupTimeline.vue'

const props = defineProps<{ controlledFileId: string }>()
const followupLoading = ref(false)
const followupLoadError = ref('')
const followup = ref<DccPublicationFollowupVO | null>(null)

const batchStatusText = computed(() => batchStatusLabel(followup.value?.status))
const batchTagType = computed(() => followup.value?.status === 'COMPLETED' ? 'success'
  : followup.value?.status === 'PARTIAL_FAILED' ? 'danger' : 'warning')

const resolveError = (error: unknown) => {
  const message = (error as any)?.response?.data?.msg || (error as any)?.message
  return typeof message === 'string' && message.trim() ? message : '无法读取发布后续，请稍后重试'
}

const userText = (user: DccPublicationVisibilityUserVO) =>
  `${user.userName || user.userId}${user.deptName ? `（${user.deptName}）` : ''}`

const loadFollowup = async () => {
  if (!/^\d+$/.test(props.controlledFileId)) {
    followupLoadError.value = '受控文件 ID 无效'
    return
  }
  followupLoading.value = true
  followupLoadError.value = ''
  try {
    followup.value = await getPublicationFollowupByFile(props.controlledFileId)
  } catch (error) {
    followup.value = null
    followupLoadError.value = resolveError(error)
  } finally {
    followupLoading.value = false
  }
}

watch(() => props.controlledFileId, loadFollowup, { immediate: true })
</script>

<style scoped>
.followup-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.followup-heading h2, .followup-section h3 { margin: 0; color: #172033; }
.followup-heading h2 { font-size: 18px; line-height: 26px; }
.followup-heading p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
.followup-body { min-height: 90px; }
.followup-section { margin-top: 16px; }
.followup-section h3 { margin-bottom: 8px; font-size: 14px; line-height: 22px; }
@media (max-width: 768px) {
  .followup-heading { flex-direction: column; }
  .followup-section { overflow-x: auto; }
}
</style>
