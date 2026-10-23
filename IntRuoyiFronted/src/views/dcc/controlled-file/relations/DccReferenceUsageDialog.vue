<template>
  <el-dialog
    :model-value="modelValue"
    title="引用使用明细"
    width="1050px"
    append-to-body
    destroy-on-close
    @close="close"
  >
    <p
      >源文件：{{ source.fileName }} · {{ source.fileNumber }} ·
      {{ getDccVersionDisplayText(source.versionNo) }}</p
    >
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-if="page">
      <p
        >全部引用项目：{{ page.referenceProjectCount }} · 可查看引用项目：{{
          page.visibleReferenceProjectCount
        }}
        · 可查看目录引用：{{ page.total }}</p
      >
      <el-alert
        v-if="page.detailsRestricted"
        title="明细仅展示你的权限范围内的项目；全部引用项目数包含其他项目。"
        type="info"
        :closable="false"
      />
      <el-table :data="page.list" v-loading="loading" row-key="referenceId">
        <el-table-column prop="projectName" label="引用目的项目" min-width="150" />
        <el-table-column prop="folderName" label="引用目的文件夹" min-width="150" />
        <el-table-column prop="fileName" label="所用文件" min-width="160" />
        <el-table-column prop="fileNumber" label="编号" min-width="100" />
        <el-table-column label="固定所用版本" width="110"
          ><template #default="{ row }">{{
            getDccVersionDisplayText(row.versionNo)
          }}</template></el-table-column
        >
        <el-table-column label="状态" min-width="120"
          ><template #default="{ row }">{{ statusLabel(row.status) }}</template></el-table-column
        >
        <el-table-column label="正文" width="100"
          ><template #default="{ row }"
            ><el-button
              link
              data-testid="dcc-reference-usage-preview"
              :disabled="loading || Boolean(previewing) || !row.canPreview"
              @click="preview(row)"
              >查看</el-button
            ></template
          ></el-table-column
        >
      </el-table>
      <el-pagination
        v-model:current-page="pageNo"
        :page-size="pageSize"
        :total="page.total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </template>
    <div v-else v-loading="loading" class="min-h-60px"></div>
    <template #footer
      ><el-button :disabled="loading" @click="load">刷新</el-button
      ><el-button @click="close">关闭</el-button></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { getProjectReferenceUsagePage } from '@/api/dcc/controlledFile/relations'
import { getControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
import { referenceIdentity } from './project-reference-contract'
import type { ReferenceUsagePage, ReferenceUsageRow } from './reference-usage'
import type { FileCandidate } from './selector-state'
import { buildControlledFileViewerPath } from '../view/presentation'
import { serializeDccBodyReadContext } from '@/api/dcc/controlledFile/bodyReadContext'
import { getDccControlledFileStatusLabel, type DccControlledFileStatus } from '../shared/lifecycle'
import { getDccVersionDisplayText } from '../shared/version-display'
export type ReferenceUsageSource = Pick<
  FileCandidate,
  'tenantId' | 'controlledFileId' | 'masterId' | 'fileName' | 'fileNumber' | 'versionNo'
>
const props = defineProps<{
  modelValue: boolean
  source: ReferenceUsageSource
  contextKey: string
  returnTo: string
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const userStore = useUserStore()
const readReferenceAuthority = () => JSON.stringify([
  String(userStore.getUser.id), String(getTenantId() ?? ''), String(getVisitTenantId() ?? '')
])
const page = ref<ReferenceUsagePage>(),
  loading = ref(false),
  error = ref(''),
  pageNo = ref(1),
  pageSize = 20
const previewing = ref('')
const loadedAuthority = ref('')
let disposed = false
let closed = false
let sequence = 0,
  contextGeneration = 0,
  previewSequence = 0
const statusLabel = (status: string) =>
  getDccControlledFileStatusLabel(status as DccControlledFileStatus)
const close = () => {
  closed = true
  loadedAuthority.value = ''
  error.value = ''
  sequence++
  contextGeneration++
  previewSequence++
  page.value = undefined
  loading.value = false
  previewing.value = ''
  emit('update:modelValue', false)
}
const load = async () => {
  if (disposed || closed || !props.modelValue) return
  const token = ++sequence,
    generation = contextGeneration,
    authority = readReferenceAuthority()
  const isCurrent = () => token === sequence && generation === contextGeneration
    && !disposed && !closed && props.modelValue && authority === readReferenceAuthority()
  const context = {
    tenantId: props.source.tenantId,
    sourceControlledFileId: props.source.controlledFileId,
    masterId: props.source.masterId
  }
  page.value = undefined
  loadedAuthority.value = ''
  loading.value = true
  error.value = ''
  previewSequence++
  previewing.value = ''
  try {
    const result = await getProjectReferenceUsagePage(context, pageNo.value, pageSize)
    if (isCurrent()) {
      page.value = result
      loadedAuthority.value = authority
    }
  } catch (cause) {
    if (isCurrent())
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (isCurrent()) loading.value = false
  }
}
const preview = async (row: ReferenceUsageRow) => {
  if (disposed || closed || !props.modelValue || !page.value) return
  if (loadedAuthority.value !== readReferenceAuthority()) {
    sequence++
    contextGeneration++
    previewSequence++
    page.value = undefined
    loadedAuthority.value = ''
    loading.value = false
    previewing.value = ''
    error.value = '引用权限上下文已变化，请刷新当前详情后重试'
    return
  }
  const saved = page.value?.list.find((entry) => entry.referenceId === row.referenceId)
  if (!saved || !saved.canPreview || loading.value || previewing.value) return
  const generation = contextGeneration,
    token = ++previewSequence,
    authority = readReferenceAuthority(),
    expected = { ...saved }
  const isCurrent = () => generation === contextGeneration && token === previewSequence
    && !disposed && !closed && props.modelValue && authority === readReferenceAuthority()
  previewing.value = row.referenceId
  error.value = ''
  try {
    if (expected.selectedControlledFileId !== props.source.controlledFileId)
      throw new Error('引用明细不属于所选固定版本')
    const metadata = await getControlledFileRelationPermissions(
      referenceIdentity(expected.selectedControlledFileId)
    )
    if (!isCurrent()) return
    if (
      metadata.tenantId !== props.source.tenantId ||
      metadata.masterId !== props.source.masterId ||
      metadata.controlledFileId !== expected.selectedControlledFileId ||
      metadata.versionNo !== expected.versionNo ||
      metadata.fileNumber !== expected.fileNumber ||
      metadata.fileName !== expected.fileName
    )
      throw new Error('引用明细正文权限与固定所用版本不一致，请刷新明细')
    if (
      metadata.pendingEffect ||
      !['ACTIVE', 'SUPERSEDED', 'OBSOLETE'].includes(metadata.status) ||
      metadata.canPreview !== true
    )
      throw new Error('当前没有该固定引用版本的正式正文查看权限或状态')
    const opened = window.open(
      buildControlledFileViewerPath(
        metadata.controlledFileId,
        'project-browser',
        props.returnTo,
        serializeDccBodyReadContext(expected.selectedControlledFileId, {
          intent: 'VERSION_HISTORY',
          controlledFileId: expected.selectedControlledFileId,
          expectedVersionNo: expected.versionNo
        })
      ),
      '_blank'
    )
    if (!opened) throw new Error('正文窗口未打开，请允许此站点打开新窗口')
    opened.opener = null
  } catch (cause) {
    if (isCurrent())
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (isCurrent()) previewing.value = ''
  }
}
watch(
  () => [props.modelValue, props.contextKey, props.source],
  () => {
    closed = !props.modelValue
    loadedAuthority.value = ''
    sequence++
    contextGeneration++
    previewSequence++
    page.value = undefined
    loading.value = false
    error.value = ''
    previewing.value = ''
    pageNo.value = 1
    if (props.modelValue) void load()
  },
  { immediate: true, flush: 'sync' }
)
onBeforeUnmount(() => {
  disposed = true
  loadedAuthority.value = ''
  error.value = ''
  sequence++
  contextGeneration++
  previewSequence++
  page.value = undefined
})
</script>
