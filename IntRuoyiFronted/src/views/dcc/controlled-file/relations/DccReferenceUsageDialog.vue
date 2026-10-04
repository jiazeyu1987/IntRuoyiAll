<template>
  <el-dialog :model-value="modelValue" title="引用使用明细" width="1050px" append-to-body destroy-on-close @close="close">
    <p>源文件：{{ source.fileName }} · {{ source.fileNumber }} · {{ source.versionNo }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-if="page">
      <p>全部引用项目：{{ page.referenceProjectCount }} · 可查看引用项目：{{ page.visibleReferenceProjectCount }} · 可查看目录引用：{{ page.total }}</p>
      <el-alert v-if="page.detailsRestricted" title="明细仅展示你的权限范围内的项目；全部引用项目数包含其他项目。" type="info" :closable="false" />
      <el-table :data="page.list" v-loading="loading" row-key="referenceId">
        <el-table-column prop="projectName" label="引用目的项目" min-width="150" />
        <el-table-column prop="folderName" label="引用目的文件夹" min-width="150" />
        <el-table-column prop="fileName" label="所用文件" min-width="160" />
        <el-table-column prop="fileNumber" label="编号" min-width="100" />
        <el-table-column prop="versionNo" label="固定所用版本" width="110" />
        <el-table-column label="状态" min-width="120"><template #default="{ row }">{{ statusLabel(row.status) }}</template></el-table-column>
        <el-table-column label="正文" width="100"><template #default="{ row }"><el-button link data-testid="dcc-reference-usage-preview" :disabled="loading || Boolean(previewing) || !row.canPreview" @click="preview(row)">查看</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNo" :page-size="pageSize" :total="page.total" layout="total, prev, pager, next" @current-change="load" />
    </template>
    <div v-else v-loading="loading" class="min-h-60px"></div>
    <template #footer><el-button :disabled="loading" @click="load">刷新</el-button><el-button @click="close">关闭</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { getProjectReferenceUsagePage } from '@/api/dcc/controlledFile/relations'
import { getControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
import { referenceIdentity } from './project-reference-contract'
import type { ReferenceUsagePage, ReferenceUsageRow } from './reference-usage'
import type { FileCandidate } from './selector-state'
import { buildControlledFileViewerPath } from '../view/presentation'
import { getDccControlledFileStatusLabel, type DccControlledFileStatus } from '../shared/lifecycle'
export type ReferenceUsageSource = Pick<FileCandidate, 'tenantId' | 'controlledFileId' | 'masterId' | 'fileName' | 'fileNumber' | 'versionNo'>
const props = defineProps<{ modelValue: boolean; source: ReferenceUsageSource; contextKey: string; returnTo: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const page = ref<ReferenceUsagePage>(), loading = ref(false), error = ref(''), pageNo = ref(1), pageSize = 20
const previewing = ref('')
let sequence = 0, contextGeneration = 0, previewSequence = 0
const statusLabel = (status: string) => getDccControlledFileStatusLabel(status as DccControlledFileStatus)
const close = () => { sequence++; contextGeneration++; previewSequence++; page.value = undefined; loading.value = false; previewing.value = ''; emit('update:modelValue', false) }
const load = async () => {
  if (!props.modelValue) return
  const token = ++sequence, generation = contextGeneration
  const context = { tenantId: props.source.tenantId, sourceControlledFileId: props.source.controlledFileId, masterId: props.source.masterId }
  page.value = undefined; loading.value = true; error.value = ''; previewSequence++; previewing.value = ''
  try {
    const result = await getProjectReferenceUsagePage(context, pageNo.value, pageSize)
    if (token === sequence && generation === contextGeneration) page.value = result
  } catch (cause) { if (token === sequence && generation === contextGeneration) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === sequence && generation === contextGeneration) loading.value = false }
}
const preview = async (row: ReferenceUsageRow) => {
  if (!row.canPreview || loading.value || previewing.value || !page.value?.list.some(entry => entry.referenceId === row.referenceId)) return
  const generation = contextGeneration, token = ++previewSequence, expected = { ...row }
  previewing.value = row.referenceId; error.value = ''
  try {
    const metadata = await getControlledFileRelationPermissions(referenceIdentity(expected.selectedControlledFileId))
    if (generation !== contextGeneration || token !== previewSequence || !props.modelValue) return
    if (metadata.tenantId !== props.source.tenantId || metadata.masterId !== props.source.masterId
      || metadata.controlledFileId !== expected.selectedControlledFileId || metadata.versionNo !== expected.versionNo
      || metadata.fileNumber !== expected.fileNumber || metadata.fileName !== expected.fileName)
      throw new Error('引用明细正文权限与固定所用版本不一致，请刷新明细')
    if (!metadata.canPreview) throw new Error('当前没有该固定引用版本的正文查看权限')
    const opened = window.open(buildControlledFileViewerPath(metadata.controlledFileId, 'project-browser', props.returnTo), '_blank')
    if (!opened) throw new Error('正文窗口未打开，请允许此站点打开新窗口')
    opened.opener = null
  } catch (cause) { if (generation === contextGeneration && token === previewSequence) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (generation === contextGeneration && token === previewSequence) previewing.value = '' }
}
watch(() => [props.modelValue, props.contextKey, props.source], () => {
  sequence++; contextGeneration++; previewSequence++; page.value = undefined; loading.value = false; error.value = ''; previewing.value = ''; pageNo.value = 1
  if (props.modelValue) void load()
}, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { sequence++; contextGeneration++; previewSequence++; page.value = undefined })
</script>
