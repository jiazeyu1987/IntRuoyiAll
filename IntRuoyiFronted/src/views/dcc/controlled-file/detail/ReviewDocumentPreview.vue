<template>
  <ProtectedPdfViewer
    v-if="source && file?.canPreview"
    ref="bodyViewer" :controlled-file-id="controlledFileId" :preview-source="source"
    title="主文档" compact hide-reading-details review-toolbar
  >
    <template #document-entry>
      <el-tag effect="plain">主文档</el-tag>
      <ControlledFileAttachments :file="file" :body-read-context="context" :context-error="contextError" compact toolbar review
        @preview-open="bodyViewer?.pauseReading()" @preview-close="bodyViewer?.resumeReading()" />
    </template>
  </ProtectedPdfViewer>
  <template v-else>
    <el-alert v-if="file?.previewUnavailableReason" :title="file.previewUnavailableReason" type="info" :closable="false" />
    <ControlledFileAttachments :file="file" :body-read-context="context" :context-error="contextError" compact toolbar review />
  </template>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import type { DccBodyReadContext } from '@/api/dcc/controlledFile/bodyReadContext'
import type { OnlineFilePreviewSource } from '@/api/common/filePreview'
import ProtectedPdfViewer from '../view/index.vue'
import ControlledFileAttachments from './ControlledFileAttachments.vue'
defineProps<{ controlledFileId: number | string; source: OnlineFilePreviewSource | null | undefined; file?: ControlledFileVO; context?: DccBodyReadContext; contextError?: string }>()
const bodyViewer = ref<InstanceType<typeof ProtectedPdfViewer>>()
defineExpose({
  pauseReading: () => bodyViewer.value?.pauseReading(),
  resumeReading: () => bodyViewer.value?.resumeReading()
})
</script>
