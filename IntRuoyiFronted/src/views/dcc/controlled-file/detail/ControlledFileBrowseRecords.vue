<template>
  <section class="browse-records" data-testid="dcc-detail-business-records">
    <el-alert v-if="file.status === 'CONTROLLED_PENDING_EFFECTIVE'" type="warning" :closable="false"
      :title="`已受控、待生效：${formatControlledFileDate(file.effectiveDate)}；生效前不得作为正式执行文件使用。${file.currentActiveVersionNo ? `当前执行版本 ${getDccVersionDisplayText(file.currentActiveVersionNo)} 继续执行。` : '当前没有执行版本。'}`" />
    <div class="controlled-detail-grid">
      <section class="controlled-detail-card">
        <h3>文件附件</h3>
        <el-alert v-if="attachmentError" :title="attachmentError" type="error" :closable="false" />
        <div v-else ref="attachmentHost" class="overview-attachments">
          <el-alert v-if="attachmentActionError" :title="attachmentActionError" type="error" :closable="false" />
          <p v-if="!file.attachments?.length" class="controlled-detail-empty">暂无文件附件</p>
          <el-table v-else data-user-table-column-explicit :data="file.attachments || []" empty-text="暂无文件附件" :row-class-name="attachmentRowClass">
            <el-table-column prop="fileName" label="附件名称" min-width="180" />
            <el-table-column label="用途" width="110"><template #default="{ row }">{{ attachmentPurposeText(row) }}</template></el-table-column>
            <el-table-column label="大小" width="95"><template #default="{ row }">{{ formatAttachmentSize(row.fileSize) }}</template></el-table-column>
            <el-table-column label="操作" min-width="180"><template #default="{ row }">
              <el-button v-if="!['OFFICE', 'DOWNLOAD_ONLY'].includes(row.previewKind || '')" link type="primary" @click="previewAttachment(row)">在线查看</el-button>
              <span v-else>不支持在线预览</span>
              <el-button v-if="row.canDownload === true" link type="primary" :disabled="!!attachmentDownloading" :loading="attachmentDownloading === String(row.attachmentId)" @click="downloadAttachment(row)">下载</el-button>
            </template></el-table-column>
          </el-table>
        </div>
        <DetailRelationsPanel v-if="file.masterId && file.dccProjectCodeId" :key="`relations-${file.id}`"
          :file="file" :body-read-context="readSelectionState.selection?.bodyReadContext" :application-selection="relationApplicationSelection" :read-authority="readBrowseAuthority" :allow-edit="false" />
        <p v-else>关联文件：缺少正式项目或文件身份，无法读取关联记录</p>
        <el-alert v-if="relationApplicationSelectionError" :title="relationApplicationSelectionError" type="error" :closable="false" />
      </section>
      <section class="controlled-detail-card">
        <h3>文件归属</h3>
        <dl class="controlled-detail-fields">
          <div><dt>所属项目</dt><dd>{{ metadata?.selected.projectName || file.projectName || '未记录' }}
            <el-button v-if="file.dccProjectCodeId" link type="primary" @click="emit('open-project', file.dccProjectCodeId)">关联文档入口</el-button>
          </dd></div>
          <div><dt>文件分类</dt><dd>{{ categoryName }}</dd></div>
          <div><dt>产品名称</dt><dd>{{ file.productName || '未记录' }}</dd></div>
          <div><dt>目录</dt><dd>{{ metadata?.selected.projectFolderName || file.projectFolderName || directoryName || '未记录' }}</dd></div>
          <div v-if="metadata"><dt>被引用项目</dt><dd>
            <span v-if="metadata.referenceProjectCount === 0">0 个</span>
            <el-button v-else link type="primary" @click="usageVisible = true">{{ metadata.referenceProjectCount }} 个 · 查看引用项目和所用版本</el-button>
          </dd></div>
        </dl>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <el-button v-if="error" link :loading="loading" @click="load">重新读取归属与引用信息</el-button>
      </section>
    </div>
    <section class="controlled-detail-card">
      <h3>文件与申请资料</h3>
      <dl v-if="!hasRelationApplicationContext" class="controlled-detail-fields controlled-detail-fields--columns">
        <div><dt>文件编制人</dt><dd>{{ file.fileOwnerNicknameSnapshot || file.fileOwnerUsernameSnapshot || '未记录' }}</dd></div>
        <div v-if="file.submittedTime"><dt>申请时间</dt><dd>{{ formatControlledFileDateTime(file.submittedTime) }}</dd></div>
        <div v-if="file.effectiveMode"><dt>申请生效方式</dt><dd>{{ effectiveness.modeText }}</dd></div>
      </dl>
      <DetailApplicationHistory v-if="hasRelationApplicationContext" :key="relationApplicationContextKey"
        display-mode="controlled-overview"
        :file-id="file.id" :expected-version-no="file.versionNo" :read-authority="readBrowseAuthority" :body-read-context="readSelectionState.selection?.bodyReadContext"
        :primary-application-type="readSelectionState.selection?.applicationType" :primary-bpm-round="selectedBpmRound"
        :lock-primary-round="hasRequestedBpmRound" :context-key="contextKey" :context-error="readSelectionState.error"
        @selection-change="onApplicationSelectionChange">
        <template #file-fields="{ selectedRound }">
          <div><dt>文件编制人</dt><dd>{{ file.fileOwnerNicknameSnapshot || file.fileOwnerUsernameSnapshot || '未记录' }}</dd></div>
          <div v-if="selectedRound || file.submittedTime"><dt>申请时间</dt><dd>{{ formatControlledFileDateTime(selectedRound ? selectedRound.applicationTime : file.submittedTime) }}</dd></div>
          <div v-if="file.effectiveMode"><dt>申请生效方式</dt><dd>{{ effectiveness.modeText }}</dd></div>
        </template>
      </DetailApplicationHistory>
    </section>
    <section v-if="file.revisionChangeType && file.revisionChangeType !== 'INITIAL' || file.changeDescription" class="controlled-detail-card">
      <h3>本次变更</h3>
      <dl class="controlled-detail-fields">
        <div><dt>变更类型</dt><dd>{{ revisionChangeLabel(file.revisionChangeType) }}</dd></div>
        <div v-if="file.revisionSourceVersionNo"><dt>来源受控版本</dt><dd>{{ getDccVersionDisplayText(file.revisionSourceVersionNo) }}</dd></div>
        <div v-if="file.changeDescription"><dt>变更说明</dt><dd>{{ file.changeDescription }}</dd></div>
      </dl>
    </section>
    <el-dialog v-model="attachmentPreviewVisible" :title="attachmentSelected?.fileName || '附件预览'" width="90%" destroy-on-close append-to-body>
      <ProtectedPdfViewer v-if="attachmentPreviewSource && attachmentPreviewVisible" :preview-source="attachmentPreviewSource" :title="attachmentSelected?.fileName || '附件预览'" />
    </el-dialog>
    <DccReferenceUsageDialog v-if="metadata" v-model="usageVisible" :source="usageSource!" :context-key="contextKey" :return-to="route.fullPath" />
  </section>
</template>
<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import { getControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
import { getProjectReferenceUsage } from '@/api/dcc/controlledFile/relations'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import DetailApplicationHistory from './DetailApplicationHistory.vue'
import ProtectedPdfViewer from '../view/index.vue'
import { buildDccControlledFileAttachmentPreviewSource } from '@/api/common/filePreview'
import { downloadControlledFileAttachmentWithName } from '@/api/dcc/controlledFile/workflow'
import { downloadByData } from '@/utils/filt'
import { ElMessageBox } from 'element-plus'
import { assertAttachmentBelongsToVersion, attachmentPurposeText } from './attachment-facts'
import type { ControlledFileAttachmentVO } from '@/api/dcc/controlledFile/workflow'
import DetailRelationsPanel from './DetailRelationsPanel.vue'
import DccReferenceUsageDialog from '../relations/DccReferenceUsageDialog.vue'
import { revisionChangeLabel } from '../revision/revision-model'
import { getDccVersionDisplayText } from '../shared/version-display'
import { effectiveFacts } from '../workflow/effective-facts'
import { formatControlledFileDate, formatControlledFileDateTime } from './presentation'
import { loadBrowseMetadata, type BrowseMetadata } from './browse-records'
import { assertRelationApplicationSelection, type DetailRelationApplicationSelection } from './relation-contract'
import { resolveDetailReadSelection } from './detail-read-context'
const props = defineProps<{
  file: ControlledFileVO
  categoryName: string
  userNames: Map<number, string>
  departmentNames: Map<number, string>
  directoryName: string
  readAuthority?: () => string
}>()
const emit = defineEmits<{ refresh: []; 'open-project': [id: number] }>()
const attachmentHost = ref<HTMLElement>()
const attachmentSelected = ref<ControlledFileAttachmentVO>()
const attachmentPreviewVisible = ref(false)
const attachmentActionError = ref('')
const attachmentDownloading = ref('')
const highlightedAttachment = ref('')
let trainingHighlightTimer: ReturnType<typeof setTimeout> | undefined
const clearTrainingHighlight = () => {
  if (trainingHighlightTimer !== undefined) clearTimeout(trainingHighlightTimer)
  trainingHighlightTimer = undefined
  highlightedAttachment.value = ''
}
const attachmentRowClass = ({ row }: { row: ControlledFileAttachmentVO }) => String(row.attachmentId) === highlightedAttachment.value ? 'training-attachment-highlight' : ''
const formatAttachmentSize = (size?: number | null) => size == null ? '未记录' : size >= 1048576 ? `${(size / 1048576).toFixed(2)} MB` : size >= 1024 ? `${(size / 1024).toFixed(2)} KB` : `${size} B`
const attachmentPreviewSource = computed(() => attachmentSelected.value
  ? buildDccControlledFileAttachmentPreviewSource(props.file.id, attachmentSelected.value.attachmentId, readSelectionState.value.selection?.bodyReadContext) : undefined)
const previewAttachment = (row: ControlledFileAttachmentVO) => {
  try {
    if (attachmentError.value || readSelectionState.value.error) throw new Error(attachmentError.value || readSelectionState.value.error)
    assertAttachmentBelongsToVersion(row, props.file.id)
    attachmentSelected.value = row
    attachmentPreviewVisible.value = true
    attachmentActionError.value = ''
  } catch (cause) { attachmentActionError.value = cause instanceof Error ? cause.message : String(cause) }
}
const downloadAttachment = async (row: ControlledFileAttachmentVO) => {
  if (attachmentDownloading.value) return
  const file = props.file, authority = readBrowseAuthority(), context = contextKey.value, token = generation
  const fingerprint = JSON.stringify(file.attachments)
  const isContextCurrent = () => token === generation && props.file === file && authority === readBrowseAuthority() && context === contextKey.value
  const isCurrent = () => isContextCurrent() && fingerprint === JSON.stringify(props.file.attachments)
  try {
    if (attachmentError.value || row.canDownload !== true) throw new Error(attachmentError.value || '当前版本附件不可下载')
    assertAttachmentBelongsToVersion(row, file.id)
    attachmentDownloading.value = String(row.attachmentId)
    await ElMessageBox.confirm('确认下载该附件？下载后的文件为非受控文件，系统将记录本次下载留痕。', '附件下载确认', { type: 'warning' })
    if (!isCurrent() || row.canDownload !== true) throw new Error('文件、附件、身份或下载权限已变化，请重新确认')
    const result = await downloadControlledFileAttachmentWithName(file.id, row.attachmentId, { nonControlledWarningConfirmed: true })
    if (!isCurrent()) throw new Error('下载期间文件、附件、身份或权限已变化，请重新进入对应版本')
    attachmentActionError.value = ''
    downloadByData(result.blob, result.fileName, result.blob.type)
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close' && isContextCurrent()) attachmentActionError.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (isContextCurrent()) attachmentDownloading.value = '' }
}
const locateTrainingAttachment = async (id: number | string) => {
  const index = props.file.attachments?.findIndex(row => String(row.attachmentId) === String(id) && row.attachmentPurpose === 'TRAINING_RECORD') ?? -1
  if (index < 0) throw new Error('当前查看版本中未找到对应培训记录单')
  assertAttachmentBelongsToVersion(props.file.attachments![index], props.file.id)
  await nextTick()
  const row = attachmentHost.value?.querySelectorAll('.el-table__body-wrapper tbody tr')[index]
  if (!row) throw new Error('培训记录单所在附件行尚未显示，请刷新详情')
  row.scrollIntoView({ behavior: 'smooth', block: 'center' })
  clearTrainingHighlight()
  highlightedAttachment.value = String(id)
  trainingHighlightTimer = setTimeout(clearTrainingHighlight, 3000)
}
defineExpose({ locateTrainingAttachment })
const attachmentError = computed(() => {
  try {
    if (!Array.isArray(props.file.attachments)) throw new Error('文件附件清单未返回，请刷新详情')
    props.file.attachments.forEach(row => assertAttachmentBelongsToVersion(row, props.file.id))
    return ''
  } catch (cause) { return cause instanceof Error ? cause.message : String(cause) }
})
const effectiveness = computed(() => effectiveFacts(props.file))
const route = useRoute()
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
  attachmentSelected.value = undefined
  attachmentPreviewVisible.value = false
  attachmentDownloading.value = ''
  attachmentActionError.value = ''
  clearTrainingHighlight()
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
watch([contextKey, () => props.file], load, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => {
  clearTrainingHighlight()
  generation++
  metadata.value = undefined
  stopRelationApplicationContext()
  selectedRelationApplication.value = null
})
</script>
<style scoped>
.browse-records { display: grid; gap: 16px; min-width: 0; }
.overview-attachments :deep(.training-attachment-highlight td) { background: var(--el-color-primary-light-9); }
.browse-records :deep([data-testid="dcc-detail-relations"]),
.browse-records :deep([data-testid="dcc-detail-application-history"]) { border: 0; margin: 12px 0 0; box-shadow: none; }
.browse-records :deep([data-testid="dcc-detail-relations"] > .el-card__body),
.browse-records :deep([data-testid="dcc-detail-application-history"] > .el-card__body) { padding: 0 !important; }
.controlled-detail-empty { margin: 0; }
.browse-records :deep([data-testid="dcc-detail-relations"]) { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--el-border-color-lighter); }
.browse-records :deep([data-testid="dcc-detail-relations"] > .el-card__body > p.font-600) { margin: 0 0 12px; font-size: 18px; line-height: 26px; font-weight: 600; }
.browse-records :deep([data-testid="dcc-detail-application-history"]) { margin-top: 0; }
</style>
