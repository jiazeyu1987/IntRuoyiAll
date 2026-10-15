<template>
  <section data-testid="dcc-detail-attachments" class="mb-16px">
    <div class="mb-12px flex items-center justify-between gap-12px">
      <div class="text-15px font-600">文件附件</div>
      <el-tag size="small" type="info">共 {{ validationError ? '—' : attachments.length }} 个</el-tag>
    </div>
    <el-alert v-if="validationError || actionError" :title="validationError || actionError" type="error" :closable="false" class="mb-12px" />
    <el-table v-if="!validationError && attachments.length" :data="attachments" border>
      <el-table-column prop="fileName" label="附件名称" min-width="220" show-overflow-tooltip />
      <el-table-column label="用途" width="120"><template #default="{ row }">{{ attachmentPurposeText(row) }}</template></el-table-column>
      <el-table-column label="上传人" min-width="160"><template #default="{ row }">{{ attachmentUploaderText(row) }}</template></el-table-column>
      <el-table-column label="上传时间" min-width="170"><template #default="{ row }">{{ attachmentUploadTimeText(row) }}</template></el-table-column>
      <el-table-column label="首次提交轮次" min-width="180" show-overflow-tooltip><template #default="{ row }">{{ attachmentRoundText(row) }}</template></el-table-column>
      <el-table-column label="类型" min-width="120"><template #default="{ row }">{{ row.contentType || row.previewKind || '未记录' }}</template></el-table-column>
      <el-table-column label="大小" width="110"><template #default="{ row }">{{ formatSize(row.fileSize) }}</template></el-table-column>
      <el-table-column label="操作" min-width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!['OFFICE', 'DOWNLOAD_ONLY'].includes(row.previewKind || '')" link type="primary" @click="preview(row)">在线查看</el-button>
          <span v-else class="text-12px text-[var(--el-text-color-secondary)]">不支持在线预览</span>
          <el-button v-if="file?.canDownload === true" link type="primary" :loading="downloading === String(row.attachmentId)" :disabled="Boolean(downloading)" @click="download(row)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else-if="!validationError && file" :image-size="72" description="暂无文件附件" />
    <el-dialog v-model="previewVisible" :title="selected?.fileName || '附件预览'" width="90%" destroy-on-close>
      <ProtectedPdfViewer v-if="previewSource && previewVisible" :key="String(selected?.attachmentId)" :preview-source="previewSource" :title="selected?.fileName || '附件预览'" />
    </el-dialog>
  </section>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import ProtectedPdfViewer from '../view/index.vue'
import { buildDccControlledFileAttachmentPreviewSource } from '@/api/common/filePreview'
import { downloadControlledFileAttachmentWithName, type ControlledFileVO, type ControlledFileAttachmentVO } from '@/api/dcc/controlledFile/workflow'
import { attachmentPurposeText, attachmentUploaderText, attachmentUploadTimeText, attachmentRoundText, assertAttachmentBelongsToVersion } from './attachment-facts'
import { downloadByData } from '@/utils/filt'

const props = defineProps<{ file?: ControlledFileVO }>()
const actionError = ref(''), downloading = ref(''), selected = ref<ControlledFileAttachmentVO>(), previewVisible = ref(false)
const validationError = computed(() => {
  if (!props.file) return ''
  try {
    if (!Array.isArray(props.file.attachments)) throw new Error('文件附件清单未返回，请刷新详情')
    props.file.attachments.forEach(row => assertAttachmentBelongsToVersion(row, props.file!.id))
    return ''
  } catch (error) { return error instanceof Error ? error.message : String(error) }
})
const attachments = computed(() => validationError.value ? [] : props.file?.attachments || [])
const previewSource = computed(() => selected.value && props.file
  ? buildDccControlledFileAttachmentPreviewSource(props.file.id, selected.value.attachmentId) : undefined)
const formatSize = (size?: number | null) => size == null ? '未记录' : size >= 1048576 ? `${(size / 1048576).toFixed(2)} MB` : size >= 1024 ? `${(size / 1024).toFixed(2)} KB` : `${size} B`
let generation = 0
watch(() => [props.file, props.file?.id, props.file?.attachments, props.file?.canDownload], () => {
  generation++; selected.value = undefined; previewVisible.value = false; actionError.value = ''; downloading.value = ''
}, { flush: 'sync' })
const preview = (row: ControlledFileAttachmentVO) => {
  try {
    if (!props.file) throw new Error('文件详情未读取')
    assertAttachmentBelongsToVersion(row, props.file.id)
    selected.value = row; previewVisible.value = true; actionError.value = ''
  } catch (error) { actionError.value = error instanceof Error ? error.message : String(error) }
}
const download = async (row: ControlledFileAttachmentVO) => {
  const file = props.file, token = generation
  try {
    if (!file || file.canDownload !== true || downloading.value) throw new Error('当前版本附件不可下载')
    assertAttachmentBelongsToVersion(row, file.id)
    await ElMessageBox.confirm('确认下载该附件？下载后的文件为非受控文件，系统将记录本次下载留痕。', '附件下载确认', { type: 'warning' })
    if (token !== generation || props.file?.id !== file.id || props.file.canDownload !== true) throw new Error('文件详情或下载权限已变化，请重新确认')
    downloading.value = String(row.attachmentId); actionError.value = ''
    const result = await downloadControlledFileAttachmentWithName(file.id, row.attachmentId, { nonControlledWarningConfirmed: true })
    if (token !== generation) throw new Error('下载期间查看版本已变化，请重新进入对应版本')
    downloadByData(result.blob, result.fileName, result.blob.type)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close' && token === generation) actionError.value = error instanceof Error ? error.message : String(error)
  } finally { if (token === generation) downloading.value = '' }
}
</script>
