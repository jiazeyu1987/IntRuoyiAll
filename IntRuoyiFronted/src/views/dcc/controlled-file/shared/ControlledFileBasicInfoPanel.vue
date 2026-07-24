<template>
  <div
    class="controlled-file-basic-info-panel"
    :class="{ 'controlled-file-basic-info-panel--compact': compact }"
  >
    <div v-if="title || showInfoActions || showEdit" class="controlled-file-basic-info-panel__header">
      <div class="controlled-file-basic-info-panel__header-main">
        <div v-if="showInfoActions" class="controlled-file-basic-info-panel__actions">
          <el-button
            size="small"
            type="primary"
            plain
            data-testid="dcc-controlled-preview-approval-button"
            @click="emit('openApprovalInfo')"
          >
            <Icon icon="ep:finished" class="mr-4px" />
            审批
          </el-button>
          <el-button
            size="small"
            type="primary"
            plain
            data-testid="dcc-controlled-preview-distribution-button"
            @click="emit('openDistributionInfo')"
          >
            <Icon icon="ep:share" class="mr-4px" />
            分发
          </el-button>
          <el-button
            size="small"
            type="primary"
            plain
            data-testid="dcc-controlled-preview-version-button"
            @click="emit('openVersionInfo')"
          >
            <Icon icon="ep:document-copy" class="mr-4px" />
            版本
          </el-button>
        </div>
        <div v-else class="controlled-file-basic-info-panel__title">{{ title }}</div>
      </div>
      <el-button
        v-if="showEdit"
        type="primary"
        plain
        :data-testid="editTestId"
        @click="emit('edit')"
      >
        <Icon icon="ep:edit" class="mr-5px" />
        {{ editButtonText }}
      </el-button>
    </div>

    <el-descriptions
      :column="column"
      border
      class="controlled-file-basic-info-panel__descriptions"
    >
      <el-descriptions-item label="文件类别">
        {{ categoryName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="受控目录">
        {{ directoryName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="文件名称">
        {{ file?.fileName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="产品编号">
        {{ file?.productCode || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="产品名称">
        <span class="project-code-recognition-row">
          <span class="project-code-recognition-row__value">
            {{ file?.productName || '-' }}
          </span>
          <el-button
            v-if="showProductRecognition"
            size="small"
            type="primary"
            plain
            :loading="projectCodeRecognitionLoading"
            @click="emit('recognizeProjectCode')"
          >
            <Icon icon="ep:magic-stick" class="mr-4px" />
            识别基础信息
          </el-button>
        </span>
      </el-descriptions-item>
      <el-descriptions-item label="DCC基础条目">
        <el-link
          v-if="file?.dccProjectCodeId"
          type="primary"
          @click="emit('openDccProjectCode', file.dccProjectCodeId)"
        >
          {{ formatDccProjectCodeLink(file) }}
        </el-link>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="文件类别 I">
        {{ file?.fileTypeLevel1 || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="文件类别 II">
        {{ file?.fileTypeLevel2 || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="文件类别 III">
        {{ file?.fileTypeLevel3 || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="文件类别 IV">
        {{ file?.fileTypeLevel4 || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="文件类别 V">
        {{ file?.fileTypeLevel5 || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="培训要求">
        {{ file?.needTraining ? '需要培训' : '无需培训' }}
      </el-descriptions-item>
      <el-descriptions-item label="现行版本">
        {{ file?.currentActiveVersionNo || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="流程实例">
        {{ file?.processInstanceId || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="提交人">
        {{ requesterName || file?.requesterId || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="提交时间">
        {{ formatControlledFileDateTime(file?.submittedTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="发布时间">
        {{ formatControlledFileDateTime(file?.publishedTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="流程定义">
        {{ file?.processDefinitionKey || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="提交备注" :span="column">
        {{ file?.remark || '-' }}
      </el-descriptions-item>
      <el-descriptions-item v-if="file?.rejectReason" label="驳回原因" :span="column">
        {{ file.rejectReason }}
      </el-descriptions-item>
      <el-descriptions-item v-if="file?.finalizationError" label="发布失败原因" :span="column">
        {{ file.finalizationError }}
      </el-descriptions-item>
      <el-descriptions-item v-if="file?.obsoleteReason" label="作废原因" :span="column">
        {{ file.obsoleteReason }}
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script lang="ts" setup>
import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import { formatControlledFileDateTime } from '../detail/presentation'

defineOptions({ name: 'ControlledFileBasicInfoPanel' })

withDefaults(
  defineProps<{
    file?: ControlledFileVO
    categoryName?: string
    directoryName?: string
    requesterName?: string
    column?: number
    title?: string
    showEdit?: boolean
    editButtonText?: string
    editTestId?: string
    showProductRecognition?: boolean
    projectCodeRecognitionLoading?: boolean
    showInfoActions?: boolean
    compact?: boolean
  }>(),
  {
    categoryName: '-',
    directoryName: '-',
    requesterName: '-',
    column: 2,
    title: '',
    showEdit: false,
    editButtonText: '修改',
    editTestId: '',
    showProductRecognition: false,
    projectCodeRecognitionLoading: false,
    showInfoActions: false,
    compact: false
  }
)

const emit = defineEmits<{
  (event: 'edit'): void
  (event: 'recognizeProjectCode'): void
  (event: 'openDccProjectCode', id: number): void
  (event: 'openApprovalInfo'): void
  (event: 'openDistributionInfo'): void
  (event: 'openVersionInfo'): void
}>()

const formatDccProjectCodeLink = (file: ControlledFileVO) => {
  return [file.productName, file.productCode].filter(Boolean).join(' / ') || `基础条目 ${file.dccProjectCodeId}`
}
</script>

<style scoped>
.controlled-file-basic-info-panel {
  min-width: 0;
}

.controlled-file-basic-info-panel--compact {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.controlled-file-basic-info-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.controlled-file-basic-info-panel__header-main {
  flex: 1;
  min-width: 0;
}

.controlled-file-basic-info-panel--compact .controlled-file-basic-info-panel__header {
  align-items: flex-start;
  margin-bottom: 10px;
}

.controlled-file-basic-info-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.controlled-file-basic-info-panel__actions :deep(.el-button) {
  margin-left: 0;
}

.controlled-file-basic-info-panel__title {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.controlled-file-basic-info-panel--compact .controlled-file-basic-info-panel__title {
  font-size: 16px;
}

.controlled-file-basic-info-panel__descriptions :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
  min-width: 96px;
  white-space: nowrap;
  word-break: keep-all;
  writing-mode: horizontal-tb;
}

.controlled-file-basic-info-panel__descriptions :deep(.el-descriptions__content.el-descriptions__cell.is-bordered-content) {
  min-width: 0;
  line-height: 20px;
  overflow-wrap: anywhere;
}

.controlled-file-basic-info-panel--compact
  .controlled-file-basic-info-panel__descriptions
  :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
  width: 108px;
  min-width: 108px;
  padding: 10px 12px;
  background: #f7f9fc;
  color: #526074;
  font-weight: 600;
}

.controlled-file-basic-info-panel--compact
  .controlled-file-basic-info-panel__descriptions
  :deep(.el-descriptions__content.el-descriptions__cell.is-bordered-content) {
  padding: 10px 12px;
  background: #fff;
  color: #172033;
}

.project-code-recognition-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-width: 0;
}

.project-code-recognition-row__value {
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere;
}

@media (max-width: 640px) {
  .controlled-file-basic-info-panel--compact
    .controlled-file-basic-info-panel__descriptions
    :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
    width: 92px;
    min-width: 92px;
  }
}
</style>
