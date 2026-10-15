<template>
  <ContentWrap>
    <section data-testid="dcc-distribution-read">
      <p>本次下发文件：{{ fileName || '正在核对文件' }}；版本：{{ identity?.versionNo || '未记录' }}</p>
      <p>查阅和电子签名均限定本次下发版本。</p>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <template v-if="identity && authorized">
        <ProtectedPdfViewer :preview-source="previewSource" :title="fileName" @opened="confirmOfficeOpened" />
        <el-alert v-if="openedError" :title="openedError" type="error" :closable="false" show-icon />
        <p v-if="openedRecorded">本次正文打开记录已保存。</p>
        <DistributionReceiptConfirm
          :controlled-file-id="identity.controlledFileId"
          :distribution-id="identity.distributionId" :recipient-id="identity.recipientId"
          :version-no="identity.versionNo" :context-key="contextKey" :can-acknowledge="authorized"
          :save="saveReceipt" />
      </template>
    </section>
  </ContentWrap>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { readDccDistributionRoute, type DccDistributionReadIdentity } from '@/utils/dccDistributionNavigation'
import { getControlledFilePreviewMetadata, acknowledgeElectronicDistribution, confirmDistributionOfficeOpened,
  type ControlledFilePreviewMetadataVO } from '@/api/dcc/controlledFile/workflow'
import type { OnlineFilePreviewSource } from '@/api/common/filePreview'
import ProtectedPdfViewer from '../view/index.vue'
import DistributionReceiptConfirm from '../workflow/DistributionReceiptConfirm.vue'
import type { DistributionReceiptCommand } from '../workflow/distribution-facts'

const props = defineProps<{ fileId: string; query: Record<string, unknown>; contextKey: string }>()
const identity = ref<DccDistributionReadIdentity>(), error = ref(''), fileName = ref(''), authorized = ref(false)
const openedError = ref(''), openedRecorded = ref(false), opening = ref(false)
let generation = 0
const previewSource = computed<OnlineFilePreviewSource | null>(() => identity.value && authorized.value
  ? { type: 'DCC_CONTROLLED_FILE', controlledFileId: identity.value.controlledFileId, distribution: identity.value } : null)
watch(() => props.contextKey, async () => {
  const attempt = ++generation
  identity.value = undefined; error.value = ''; fileName.value = ''; authorized.value = false
  openedError.value = ''; openedRecorded.value = false; opening.value = false
  try {
    const task = readDccDistributionRoute(props.fileId, props.query)
    identity.value = task
    const metadata = await getControlledFilePreviewMetadata(task.controlledFileId, task)
    if (attempt !== generation) return
    if (metadata.previewUnavailableReason) throw new Error(metadata.previewUnavailableReason)
    fileName.value = metadata.fileName
    authorized.value = true
  } catch (cause) {
    if (attempt === generation) error.value = cause instanceof Error ? cause.message : '下发版本查阅失败'
  }
}, { immediate: true, flush: 'sync' })
const saveReceipt = (command: DistributionReceiptCommand): Promise<boolean> => {
  const task = identity.value
  if (!authorized.value || !task || String(command.controlledFileId) !== task.controlledFileId
    || String(command.distributionId) !== task.distributionId || String(command.recipientId) !== task.recipientId
    || command.versionNo !== task.versionNo) throw new Error('签收命令与当前精确下发上下文不一致')
  return acknowledgeElectronicDistribution(task.controlledFileId, task.distributionId, task.recipientId,
    { password: command.password, comment: command.comment })
}
const confirmOfficeOpened = async (event: { source: OnlineFilePreviewSource; metadata: ControlledFilePreviewMetadataVO }) => {
  const task = identity.value, attempt = generation
  if (opening.value || openedRecorded.value || !authorized.value || !task) return
  openedError.value = ''
  const source = event.source
  if (source.type !== 'DCC_CONTROLLED_FILE' || !source.distribution
    || String(source.controlledFileId) !== task.controlledFileId
    || source.distribution.controlledFileId !== task.controlledFileId
    || source.distribution.distributionId !== task.distributionId || source.distribution.recipientId !== task.recipientId
    || source.distribution.versionNo !== task.versionNo || source.distribution.approvalProcessInstanceId !== task.approvalProcessInstanceId) {
    openedError.value = '正文打开事件与当前精确下发上下文不一致'
    return
  }
  opening.value = true
  try {
    await confirmDistributionOfficeOpened(task, event.metadata)
    if (attempt === generation) openedRecorded.value = true
  } catch (cause) {
    if (attempt === generation) openedError.value = cause instanceof Error ? cause.message : '正文打开记录保存失败'
  } finally { if (attempt === generation) opening.value = false }
}
onBeforeUnmount(() => { generation++; authorized.value = false })
</script>
