<template>
  <el-dialog :model-value="modelValue" title="文件关联" width="1000px" append-to-body destroy-on-close @close="close">
    <p>{{ source.fileName }} · {{ source.fileNumber }} · {{ source.versionNo }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-loading="loading">
      <DetailRelationsPanel v-if="file" :key="String(file.id)" :file="file" :allow-edit="true" :auto-open-editor="true" />
    </div>
    <template #footer><el-button @click="close">关闭</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { getControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
import DetailRelationsPanel from '../detail/DetailRelationsPanel.vue'
import type { DetailRelationFile } from '../detail/relation-contract'
import type { FileCandidate } from '../relations/selector-state'
const props = defineProps<{ modelValue: boolean; source: FileCandidate; contextKey: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const file = ref<DetailRelationFile>(), loading = ref(false), error = ref('')
let sequence = 0
const close = () => { sequence++; file.value = undefined; loading.value = false; emit('update:modelValue', false) }
watch(() => [props.modelValue, props.contextKey, props.source], async () => {
  const token = ++sequence, expected: FileCandidate = JSON.parse(JSON.stringify(props.source))
  file.value = undefined; error.value = ''; loading.value = false
  if (!props.modelValue) return
  loading.value = true
  try {
    const selected = await getControlledFileRelationPermissions(expected.controlledFileId)
    if (token !== sequence) return
    if (selected.tenantId !== expected.tenantId || selected.controlledFileId !== expected.controlledFileId ||
        selected.masterId !== expected.masterId || selected.projectId !== expected.projectId ||
        selected.versionNo !== expected.versionNo || selected.fileNumber !== expected.fileNumber ||
        selected.fileName !== expected.fileName)
      throw new Error('关联入口与所选文件、项目、租户或版本不一致，请刷新文件列表')
    file.value = { id: selected.controlledFileId, masterId: selected.masterId,
      dccProjectCodeId: selected.projectId, projectFolderId: selected.projectFolderId,
      versionNo: selected.versionNo, title: selected.fileName, fileName: selected.fileName, fileNumber: selected.fileNumber }
  } catch (cause) { if (token === sequence) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === sequence) loading.value = false }
}, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { sequence++; file.value = undefined })
</script>
