<template>
  <el-radio-group v-model="activeDocument" class="mb-10px" aria-label="正文与附件切换">
    <el-radio-button value="body">主文档</el-radio-button>
    <el-radio-button value="attachments">附件</el-radio-button>
  </el-radio-group>
  <ProtectedPdfViewer
    v-if="activeDocument === 'body' && source"
    ref="bodyViewer" :controlled-file-id="controlledFileId" :preview-source="source"
    title="主文档" compact :hide-reading-details="hideReadingDetails" review-toolbar
  />
  <ControlledFileAttachments v-if="activeDocument === 'attachments'" :file="file" :body-read-context="context" :context-error="contextError" compact inline />
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import type { DccBodyReadContext } from '@/api/dcc/controlledFile/bodyReadContext'
import type { OnlineFilePreviewSource } from '@/api/common/filePreview'
import ProtectedPdfViewer from '../view/index.vue'
import ControlledFileAttachments from './ControlledFileAttachments.vue'
defineProps<{ controlledFileId: number | string; source: OnlineFilePreviewSource | null; file?: ControlledFileVO; context?: DccBodyReadContext; contextError?: string; hideReadingDetails?: boolean }>()
const activeDocument = ref('body')
const bodyViewer = ref<InstanceType<typeof ProtectedPdfViewer>>()
defineExpose({
  pauseReading: () => bodyViewer.value?.pauseReading(),
  resumeReading: () => bodyViewer.value?.resumeReading()
})
</script>
