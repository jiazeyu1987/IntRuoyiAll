<template>
  <div class="candidate-picker">
    <div class="candidate-picker__head">
      <span>{{ props.mode === 'rollback' ? '版本候选' : '恢复集候选' }}</span>
      <el-tag :type="availableCandidates.length ? 'success' : 'danger'">
        可用 {{ availableCandidates.length }} / {{ candidates.length }}
      </el-tag>
    </div>
    <el-radio-group
      class="candidate-picker__list"
      :model-value="props.modelValue"
      @update:model-value="emit('update:modelValue', String($event))"
    >
      <div
        v-for="candidate in candidates"
        :key="candidate.candidateId"
        class="candidate-row"
        :class="{ 'candidate-row--blocked': candidate.status === 'BLOCKED' }"
      >
        <el-radio :label="candidate.candidateId" :disabled="candidate.status !== 'AVAILABLE'">
          <div class="candidate-row__main">
            <div class="candidate-row__title">
              <span>{{ candidate.candidateId }}</span>
              <el-tag :type="opsTagType(candidate.status)">{{ opsStatusText(candidate.status) }}</el-tag>
            </div>
            <div class="candidate-row__meta">
              <span>{{ sourceLabel(candidate) }}</span>
              <span>版本 {{ candidate.imageTag || '-' }}</span>
            </div>
            <template v-if="props.mode === 'rollback'">
              <div
                v-if="'compatibilityStatus' in candidate && candidate.compatibilityStatus"
                class="candidate-row__path"
              >
                兼容性状态：{{ candidate.compatibilityStatus }}
              </div>
              <div
                v-if="'compatibilityEvidencePath' in candidate && candidate.compatibilityEvidencePath"
                class="candidate-row__path"
              >
                兼容性证据：{{ candidate.compatibilityEvidencePath }}
              </div>
              <div
                v-if="'compatibilityCheckedAt' in candidate && candidate.compatibilityCheckedAt"
                class="candidate-row__path"
              >
                检查时间：{{ candidate.compatibilityCheckedAt }}
              </div>
              <div
                v-if="'compatibilitySummary' in candidate && candidate.compatibilitySummary"
                class="candidate-row__path"
              >
                摘要：{{ candidate.compatibilitySummary }}
              </div>
            </template>
            <div v-if="candidate.manifestPath" class="candidate-row__path">
              {{ candidate.manifestPath }}
            </div>
            <div
              v-if="props.mode === 'rollback' && 'prodHistoryPath' in candidate && candidate.prodHistoryPath"
              class="candidate-row__path"
            >
              正式服发布历史：{{ candidate.prodHistoryPath }}
            </div>
            <template v-if="props.mode === 'restore'">
              <div v-if="'recoverySetId' in candidate && candidate.recoverySetId" class="candidate-row__path">
                恢复集：{{ candidate.recoverySetId }} / {{ candidate.recoverySetStatus || '-' }}
              </div>
              <div v-if="'programVersion' in candidate && candidate.programVersion" class="candidate-row__path">
                程序版本：{{ candidate.programVersion }}
              </div>
              <div v-if="'redisPolicy' in candidate && candidate.redisPolicy" class="candidate-row__path">
                Redis 策略：{{ candidate.redisPolicy }}
              </div>
              <div
                v-if="'configurationManifestPath' in candidate && candidate.configurationManifestPath"
                class="candidate-row__path"
              >
                配置清单：{{ candidate.configurationManifestPath }}
              </div>
              <div
                v-if="'recoverySetManifestHash' in candidate && candidate.recoverySetManifestHash"
                class="candidate-row__path"
              >
                manifest hash：{{ candidate.recoverySetManifestHash }}
              </div>
              <div
                v-if="'componentSummary' in candidate && candidate.componentSummary"
                class="candidate-row__path"
              >
                组件摘要：{{ componentSummaryText(candidate.componentSummary) }}
              </div>
              <div
                v-if="'dccBackupMode' in candidate && (candidate.dccBackupMode || candidate.dccChainStatus)"
                class="candidate-row__path"
              >
                DCC：{{ dccSummaryText(candidate) }}
              </div>
              <div
                v-if="'dccChangeSummary' in candidate && candidate.dccChangeSummary"
                class="candidate-row__path"
              >
                DCC 变更：{{ dccChangeSummaryText(candidate.dccChangeSummary) }}
              </div>
              <div v-if="'checksumPath' in candidate && candidate.checksumPath" class="candidate-row__path">
                checksum：{{ candidate.checksumPath }}
              </div>
              <div
                v-if="'rehearsalReportPath' in candidate && candidate.rehearsalReportPath"
                class="candidate-row__path"
              >
                演练报告：{{ candidate.rehearsalReportPath }}
              </div>
              <div v-if="'snapshotPath' in candidate && candidate.snapshotPath" class="candidate-row__path">
                现场快照：{{ candidate.snapshotPath }}
              </div>
            </template>
            <div v-if="candidate.blockedReasons?.length" class="candidate-row__reason">
              {{ joinReasons(candidate.blockedReasons) }}
            </div>
          </div>
        </el-radio>
      </div>
    </el-radio-group>
    <div v-if="!candidates.length" class="candidate-empty">服务端未返回候选，提交已阻断</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  RuntimeControlRollbackCandidateVO,
  RuntimeControlRestoreCandidateVO
} from '@/api/infra/runtimeControl'
import { joinReasons, opsStatusText, opsTagType } from './shared'

const props = withDefaults(
  defineProps<{
    mode: 'rollback' | 'restore'
    modelValue?: string
    rollbackCandidates?: RuntimeControlRollbackCandidateVO[]
    restoreCandidates?: RuntimeControlRestoreCandidateVO[]
  }>(),
  {
    modelValue: '',
    rollbackCandidates: () => [],
    restoreCandidates: () => []
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const candidates = computed(() =>
  props.mode === 'rollback' ? props.rollbackCandidates : props.restoreCandidates
)
const availableCandidates = computed(() =>
  candidates.value.filter((candidate) => candidate.status === 'AVAILABLE')
)

const sourceLabel = (candidate: RuntimeControlRollbackCandidateVO | RuntimeControlRestoreCandidateVO) => {
  if (props.mode === 'rollback') {
    return `发布包 ${candidate.releaseTag || candidate.backupId || '-'}`
  }
  return `恢复集 ${'recoverySetId' in candidate && candidate.recoverySetId ? candidate.recoverySetId : candidate.backupId || '-'}`
}

const componentSummaryText = (summary: Record<string, string>) =>
  Object.entries(summary)
    .map(([key, value]) => `${key}=${value}`)
    .join('；')

const dccSummaryText = (candidate: RuntimeControlRestoreCandidateVO) =>
  `${candidate.dccBackupMode || '-'} / ${candidate.dccChainStatus || '-'}`

const DCC_CHANGE_LABELS: Record<string, string> = {
  addedRecords: '新增记录',
  changedRecords: '修改记录',
  deletedRecords: '删除记录',
  invalidatedRecords: '作废记录',
  addedObjects: '新增对象',
  changedObjects: '修改对象',
  reusedObjects: '复用对象',
  tombstoneObjects: '删除标记'
}

const dccChangeSummaryText = (summary: Record<string, string>) =>
  Object.entries(summary)
    .map(([key, value]) => `${DCC_CHANGE_LABELS[key] || key}=${value}`)
    .join('；')
</script>

<style scoped>
.candidate-picker {
  display: grid;
  gap: 8px;
  width: 100%;
}

.candidate-picker__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #263247;
  font-size: 13px;
  font-weight: 700;
}

.candidate-picker__list {
  display: grid;
  gap: 8px;
  width: 100%;
  max-height: 280px;
  overflow: auto;
}

.candidate-row {
  width: 100%;
  padding: 8px;
  background: #fafcff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

.candidate-row--blocked {
  background: #fff7f7;
}

.candidate-row :deep(.el-radio) {
  width: 100%;
  height: auto;
  margin-right: 0;
}

.candidate-row :deep(.el-radio__label) {
  width: 100%;
  min-width: 0;
}

.candidate-row__main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.candidate-row__title,
.candidate-row__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.candidate-row__title span:first-child,
.candidate-row__path {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.candidate-row__title span:first-child {
  color: #172033;
  font-weight: 700;
}

.candidate-row__meta,
.candidate-row__path {
  color: #4b5563;
  font-size: 12px;
}

.candidate-row__reason,
.candidate-empty {
  color: #b42318;
  font-size: 12px;
  line-height: 18px;
}
</style>
