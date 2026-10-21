<template>
  <section class="browse-records" data-testid="dcc-detail-business-records">
    <h3>文件信息与记录（只读）</h3>
    <el-tag :type="getDetailStatusTagType(file.status)">{{
      getDetailStatusLabel(file.status)
    }}</el-tag>
    <el-alert
      v-if="file.status === 'CONTROLLED_PENDING_EFFECTIVE'"
      type="warning"
      :closable="false"
      :title="`已受控、待生效：${formatControlledFileDate(file.effectiveDate)}；生效前不得作为正式执行文件使用。${file.currentActiveVersionNo ? `当前执行版本 ${getDccVersionDisplayText(file.currentActiveVersionNo)} 继续执行。` : '当前没有执行版本。'}`"
    />
    <el-descriptions :column="2" border>
      <el-descriptions-item label="文件名称">{{
        file.fileName
      }}</el-descriptions-item>
      <el-descriptions-item label="文件编号">{{
        file.fileNumber || '未记录'
      }}</el-descriptions-item>
      <el-descriptions-item label="查看版本">{{ getDccVersionDisplayText(file.versionNo) }}</el-descriptions-item>
      <el-descriptions-item label="文件负责人">{{
        file.fileOwnerNicknameSnapshot || file.fileOwnerUsernameSnapshot || '未记录'
      }}</el-descriptions-item>
      <el-descriptions-item label="项目">{{
        metadata?.selected.projectName || file.projectName || '未记录'
      }}</el-descriptions-item>
      <el-descriptions-item label="存储文件夹">{{
        metadata?.selected.projectFolderName || (file.projectFolderId ? '尚未读取' : '未记录')
      }}</el-descriptions-item>
      <el-descriptions-item label="文件类型">{{ categoryName }}</el-descriptions-item>
      <el-descriptions-item label="受控日期">{{
        formatControlledFileDateTime(file.controlledTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="生效方式">{{ effectiveness.modeText }}</el-descriptions-item>
      <el-descriptions-item v-if="file.effectiveMode === 'DELAYED'" label="延迟生效时间">{{ effectiveness.delayedEffectiveText }}</el-descriptions-item>
      <el-descriptions-item v-if="file.effectiveMode === 'DELAYED'" label="延迟确认签名时间">{{ effectiveness.delayedConfirmedText }}</el-descriptions-item>
      <el-descriptions-item v-if="file.effectiveMode === 'DELAYED'" label="延迟确认签名">{{ file.delayedEffectiveSignatureId ? `#${file.delayedEffectiveSignatureId}` : '未记录' }}</el-descriptions-item>
      <el-descriptions-item v-if="file.effectiveMode === 'DELAYED'" label="延迟确认轮次">{{ file.delayedEffectiveProcessInstanceId || '未记录' }}</el-descriptions-item>
      <el-descriptions-item v-if="effectiveness.presetDateVisible" label="预设生效日期">{{
        formatControlledFileDate(file.effectiveDate)
      }}</el-descriptions-item>
      <el-descriptions-item label="实际生效时间">{{ effectiveness.effectiveText }}</el-descriptions-item>
      <el-descriptions-item label="日常启用时间">{{ effectiveness.activatedText }}</el-descriptions-item>
      <el-descriptions-item label="下发日期">{{
        formatControlledFileDateTime(file.distributedTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="检出人">{{
        file.checkedOutBy ? file.checkedOutByName || `账号 #${file.checkedOutBy}` : '未检出'
      }}</el-descriptions-item>
      <el-descriptions-item label="检出时间">{{
        file.checkedOutBy
          ? file.checkedOutTime == null
            ? '未记录'
            : formatControlledFileDateTime(file.checkedOutTime)
          : '未检出'
      }}</el-descriptions-item>
      <el-descriptions-item label="检出原因">{{
        file.checkedOutBy ? file.checkedOutReason || '未记录' : '未检出'
      }}</el-descriptions-item>
      <el-descriptions-item label="实际变更类型">{{
        revisionChangeLabel(file.revisionChangeType)
      }}</el-descriptions-item>
      <el-descriptions-item label="来源受控版本">{{
        getDccVersionDisplayText(file.revisionSourceVersionNo)
      }}</el-descriptions-item>
      <el-descriptions-item label="选用正文小版本">{{
        getDccVersionDisplayText(file.selectedIterationVersionNo)
      }}</el-descriptions-item>
      <el-descriptions-item label="变更说明">{{
        file.changeDescription || '未记录'
      }}</el-descriptions-item>
    </el-descriptions>
    <section data-testid="dcc-detail-current-version-reference">
      <h4>当前版本信息（独立参考）</h4>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="最新受控版本">
          <span v-if="metadata?.latest">{{ getDccVersionDisplayText(metadata.latest.versionNo) }} · {{ getDetailStatusLabel(metadata.latest.status) }}</span>
          <span v-else>{{ metadata ? '无可用受控版本' : '尚未读取' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="当前执行受控版本">{{ file.currentActiveVersionNo ? getDccVersionDisplayText(file.currentActiveVersionNo) : '无当前执行版本' }}</el-descriptions-item>
      </el-descriptions>
    </section>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-button :loading="loading" @click="emit('refresh')">刷新版本与引用信息</el-button>
    <p v-if="metadata" data-testid="dcc-detail-selected-version-references">
      所选版本的引用项目：{{ metadata.referenceProjectCount }}
      <el-button link @click="usageVisible = true">查看引用项目和所用版本</el-button>
    </p>
    <DetailApplicationHistory
      v-if="hasRelationApplicationContext"
      :key="relationApplicationContextKey"
      :file-id="file.id"
      :expected-version-no="file.versionNo"
      :read-authority="readBrowseAuthority"
      :primary-application-type="readSelectionState.selection?.applicationType"
      :primary-bpm-round="selectedBpmRound"
      :lock-primary-round="hasRequestedBpmRound"
      :context-key="contextKey" show-node-records
      :context-error="readSelectionState.error"
      @selection-change="onApplicationSelectionChange"
    />
    <el-alert
      v-if="relationApplicationSelectionError" :title="relationApplicationSelectionError"
      type="error" :closable="false" />
    <DetailRelationsPanel
      v-if="file.masterId && file.dccProjectCodeId"
      :key="`relations-${file.id}`"
      :file="file"
      :application-selection="relationApplicationSelection"
      :read-authority="readBrowseAuthority"
      :allow-edit="false"
    />
    <ControlledFileAttachments :file="file" />
    <h4>版本历史</h4>
    <el-table :data="file.versionHistory || []" empty-text="暂无获授权的版本历史">
      <el-table-column label="版本"><template #default="{ row }">{{ getDccVersionDisplayText(row.versionNo) }}</template></el-table-column>
      <el-table-column label="状态"
        ><template #default="{ row }">{{
          getDetailStatusLabel(row.status)
        }}</template></el-table-column
      >
      <el-table-column label="实际变更类型"
        ><template #default="{ row }">{{
          revisionChangeLabel(row.revisionChangeType)
        }}</template></el-table-column
      >
      <el-table-column label="来源受控版本"><template #default="{ row }">{{ getDccVersionDisplayText(row.revisionSourceVersionNo) }}</template></el-table-column>
      <el-table-column label="选用草稿"><template #default="{ row }">{{ getDccVersionDisplayText(row.selectedIterationVersionNo) }}</template></el-table-column>
      <el-table-column prop="changeDescription" label="变更说明" />
      <el-table-column label="查看"
        ><template #default="{ row }"
          ><el-button link @click="openVersion(row.id)">只读记录</el-button></template
        ></el-table-column
      >
    </el-table>
    <h4>会签、批准及文控审核签名</h4>
    <el-table :data="selectedSignatures" empty-text="所选版本或申请暂无签名记录">
      <el-table-column label="签名人"
        ><template #default="{ row }">{{
          row.actorNicknameSnapshot || row.actorUsernameSnapshot || '未记录'
        }}</template></el-table-column
      >
      <el-table-column prop="actorDeptNameSnapshot" label="签名部门" />
      <el-table-column label="动作"
        ><template #default="{ row }">{{
          getDccSignatureTaskActionLabel(row.actionType)
        }}</template></el-table-column
      >
      <el-table-column label="签名时间"
        ><template #default="{ row }">{{
          formatControlledFileDateTime(row.signedAt)
        }}</template></el-table-column
      >
      <el-table-column label="意见"
        ><template #default="{ row }">{{
          getDccSignatureOpinionLabel(row.comment)
        }}</template></el-table-column
      >
    </el-table>
    <h4>培训记录</h4>
    <p>{{
      file.needTraining
        ? file.trainingRecordAvailable
          ? `已上传线下培训文件：${file.trainingRecordFileName || '文件名称未记录'}`
          : '需要培训，尚无已上传记录'
        : '本版本无需培训'
    }}</p>
    <h4>下发及回执记录</h4>
    <p
      >{{ file.distributionCompleted ? '文控已完成下发' : '文控尚未完成下发' }} · 下发日期：{{
        formatControlledFileDateTime(file.distributedTime)
      }}</p
    >
    <el-table :data="file.distributionStatuses || []" empty-text="暂无逐部门回执记录">
      <el-table-column label="接收部门"
        ><template #default="{ row }">{{
          departmentNames.get(row.departmentId) || `部门 #${row.departmentId}`
        }}</template></el-table-column
      >
      <el-table-column label="下发方式"
        ><template #default="{ row }">{{
          getDistributionMediumLabel(row.distributionMedium)
        }}</template></el-table-column
      >
      <el-table-column label="状态"
        ><template #default="{ row }">{{
          getDistributionStatusLabel(row.status, file)
        }}</template></el-table-column
      >
      <el-table-column label="签收时间"
        ><template #default="{ row }">{{
          formatControlledFileDateTime(row.acknowledgedAt)
        }}</template></el-table-column
      >
      <el-table-column label="回收时间"
        ><template #default="{ row }">{{
          formatControlledFileDateTime(row.recoveredAt)
        }}</template></el-table-column
      >
      <el-table-column label="接收人及回执"
        ><template #default="{ row }"
          ><p v-for="recipient in row.recipients || []" :key="recipient.id"
            >{{ userNames.get(recipient.userId) || `账号 #${recipient.userId}` }} · 阅读：{{
              formatControlledFileDateTime(recipient.readAt)
            }}
            · 签收：{{ formatControlledFileDateTime(recipient.acknowledgedAt) }}</p
          ></template
        ></el-table-column
      >
    </el-table>
    <DccReferenceUsageDialog
      v-if="metadata"
      v-model="usageVisible"
      :source="usageSource!"
      :context-key="contextKey"
      :return-to="route.fullPath"
    />
  </section>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import { getControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
import { getProjectReferenceUsage } from '@/api/dcc/controlledFile/relations'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import DetailApplicationHistory from './DetailApplicationHistory.vue'
import ControlledFileAttachments from './ControlledFileAttachments.vue'
import DetailRelationsPanel from './DetailRelationsPanel.vue'
import DccReferenceUsageDialog from '../relations/DccReferenceUsageDialog.vue'
import { buildControlledFileTraceabilityPath } from '../view/presentation'
import { revisionChangeLabel } from '../revision/revision-model'
import { getDccVersionDisplayText } from '../shared/version-display'
import { effectiveFacts } from '../workflow/effective-facts'
import {
  getDccSignatureTaskActionLabel,
  getDccSignatureOpinionLabel
} from '../shared/signature-evidence'
import {
  formatControlledFileDate,
  formatControlledFileDateTime,
  getDetailStatusLabel,
  getDetailStatusTagType,
  getDistributionMediumLabel,
  getDistributionStatusLabel
} from './presentation'
import { loadBrowseMetadata, type BrowseMetadata } from './browse-records'
import { assertRelationApplicationSelection, type DetailRelationApplicationSelection } from './relation-contract'
import { resolveDetailReadSelection } from './detail-read-context'
const props = defineProps<{
  file: ControlledFileVO
  categoryName: string
  userNames: Map<number, string>
  departmentNames: Map<number, string>
  readAuthority?: () => string
}>()
const emit = defineEmits<{ refresh: [] }>()
const effectiveness = computed(() => effectiveFacts(props.file))
const route = useRoute(),
  router = useRouter()
const metadata = ref<BrowseMetadata>(),
  error = ref(''),
  loading = ref(false),
  usageVisible = ref(false)
const tenant = () => String(getVisitTenantId() ?? getTenantId())
const readBrowseAuthority = () => JSON.stringify([tenant(), props.readAuthority?.()])
const contextKey = computed(() => JSON.stringify([readBrowseAuthority(), props.file.id, route.fullPath]))
const readSelectionState = computed(() => {
  try { return { selection: resolveDetailReadSelection(props.file.id, route.query), error: '' } }
  catch (error) { return { selection: undefined, error: error instanceof Error ? error.message : String(error) } }
})
const hasRequestedBpmRound = computed(() => readSelectionState.value.selection?.lockRound === true)
const selectedBpmRound = computed(() => hasRequestedBpmRound.value
  ? readSelectionState.value.selection?.bpmRound : readSelectionState.value.error ? undefined : props.file.processInstanceId)
const selectedSignatures = computed(() => readSelectionState.value.error ? [] : (props.file.signatureSummaries || [])
  .filter(row => !hasRequestedBpmRound.value || row.processInstanceId === selectedBpmRound.value))
const selectedRelationApplication = ref<DetailRelationApplicationSelection | null>(null)
const relationApplicationSelectionError = ref('')
const relationApplicationGeneration = ref(0)
const hasRelationApplicationContext = computed(() => Boolean(selectedBpmRound.value || readSelectionState.value.error))
const relationApplicationContextKey = computed(() => JSON.stringify([contextKey.value, relationApplicationGeneration.value]))
const relationApplicationSelection = computed(() => hasRelationApplicationContext.value
  ? selectedRelationApplication.value : undefined)
const onApplicationSelectionChange = computed(() => {
  const context = relationApplicationContextKey.value, file = props.file
  return (selection: DetailRelationApplicationSelection | null) => {
    if (context !== relationApplicationContextKey.value || file !== props.file || !hasRelationApplicationContext.value) return
    selectedRelationApplication.value = null
    relationApplicationSelectionError.value = ''
    if (!selection) return
    try {
      assertRelationApplicationSelection(file, selection)
      if (readSelectionState.value.error || (hasRequestedBpmRound.value && (selection.bpmRound !== selectedBpmRound.value
        || readSelectionState.value.selection?.applicationType && selection.applicationType !== readSelectionState.value.selection.applicationType)))
        throw new Error('所选关联申请不属于当前查看的正式流程轮次')
      selectedRelationApplication.value = { ...selection }
    } catch (error) {
      relationApplicationSelectionError.value = error instanceof Error ? error.message : String(error)
    }
  }
})
const stopRelationApplicationContext = watch([contextKey, () => props.file], () => {
  relationApplicationGeneration.value++
  selectedRelationApplication.value = null
  relationApplicationSelectionError.value = ''
}, { flush: 'sync' })
const usageSource = computed(() =>
  metadata.value
    ? {
        tenantId: metadata.value.selected.tenantId,
        controlledFileId: metadata.value.selected.controlledFileId,
        masterId: metadata.value.selected.masterId,
        fileName: metadata.value.selected.fileName,
        fileNumber: metadata.value.selected.fileNumber,
        versionNo: metadata.value.selected.versionNo
      }
    : undefined
)
let generation = 0
const load = async () => {
  const token = ++generation,
    context = contextKey.value,
    authority = readBrowseAuthority(),
    file = props.file
  const isCurrent = () => token === generation && context === contextKey.value && authority === readBrowseAuthority()
  metadata.value = undefined
  usageVisible.value = false
  error.value = ''
  loading.value = true
  try {
    if (!file.masterId || !file.dccProjectCodeId)
      throw new Error('此历史文件没有正式项目或稳定文件身份，无法读取最新受控版与引用信息')
    const result = await loadBrowseMetadata(file, tenant(), {
      getPermissions: getControlledFileRelationPermissions,
      getUsage: getProjectReferenceUsage
    })
    if (isCurrent()) metadata.value = result
  } catch (cause) {
    if (isCurrent())
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (isCurrent()) loading.value = false
  }
}
const openVersion = (id: number | string) =>
  router.push(buildControlledFileTraceabilityPath(id, 'version-history', route.fullPath, 'trace'))
watch([contextKey, () => props.file], load, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => {
  generation++
  metadata.value = undefined
  stopRelationApplicationContext()
  selectedRelationApplication.value = null
})
</script>
<style scoped>
.browse-records {
  margin-top: 20px;
  display: grid;
  gap: 12px;
  min-width: 0;
}
.browse-records h3,
.browse-records h4,
.browse-records p {
  margin: 0;
}
</style>
