<template>
  <ContentWrap v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <SignoffAssignmentPanel
      v-if="context"
      :key="contextKey"
      :file-id="context.controlledFileId"
      :process-instance-id="context.processInstanceId"
      :task-id="context.taskId"
      :department-name="context.departmentName || `部门 #${context.departmentId}（名称未记录）`"
      :users="context.assigneeOptions"
      :assigned="context.assigned"
      :arrangements-ready="ready && context.canAssign && !supplementLoading && !supplementVisible"
      :save="saveAssignment"
      @saved="onSaved"
    >
      <template #relation-arrangements="{ disabled }">
        <div v-if="canSupplement">
          <el-button :disabled="disabled || supplementLoading" :loading="supplementLoading" @click="openSupplement">
            补充关联
          </el-button>
          <el-alert v-if="supplementError" :title="supplementError" type="error" :closable="false" />
          <p>本次后补 {{ supplementalFiles.length }} 个；仅确认的名单随本次指派签名保存，原上传关联保持不变。</p>
          <el-tag v-for="file in supplementalFiles" :key="file.masterId">
            {{ file.fileName }} · {{ file.versionNo }}
          </el-tag>
        </div>
        <DetailRelationArrangements
          v-if="context.processDefinitionKey === 'dcc-controlled-file-revision'"
          ref="arrangementsRef"
          :context-key="contextKey"
          :relations="relations"
          :users="users"
          :saved="savedArrangements"
          :disabled="disabled || !ready || !context.canAssign"
        />
      </template>
    </SignoffAssignmentPanel>
    <!-- 候选目录由既有选择器按正式来源项目自行读取。 -->
    <DccFileSelector
      v-if="supplementSource && canSupplement"
      v-model="supplementVisible"
      :source="supplementSource"
      purpose="relations"
      :directories="[]"
      :selected="supplementalFiles"
      :load-page="loadSupplementPage"
      :persist="confirmSupplement"
      :open-preview="previewSupplement"
    />
  </ContentWrap>
</template>
<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import {
  getSignoffAssignmentContext,
  getControlledFileRelationPermissions,
  type SignoffAssignmentContext
} from '@/api/dcc/controlledFile/applicationRead'
import { assignWorkflowSignoff } from '@/api/dcc/controlledFile/workflowLifecycle'
import {
  listHistoricalRelations,
  listRelationArrangements,
  loadSelectorBrowserPage
} from '@/api/dcc/controlledFile/relations'
import { getSimpleUserList } from '@/api/system/user'
import SignoffAssignmentPanel from '../workflow/SignoffAssignmentPanel.vue'
import DetailRelationArrangements from './DetailRelationArrangements.vue'
import DccFileSelector from '../relations/DccFileSelector.vue'
import type { SelectorSource } from '../relations/DccFileSelector.vue'
import type { FileCandidate, SelectorQuery } from '../relations/selector-state'
import {
  isWorkflowId,
  type SignoffAssignment,
  type ArrangementCommand
} from '../workflow/workflow-actions'
const props = defineProps<{ fileId: number | string; processInstanceId: string; taskId: string }>()
const emit = defineEmits<{
  saved: []
  state: [value: { fileId: string; taskId: string; assigned: boolean }]
}>()
const context = ref<SignoffAssignmentContext>(),
  error = ref(''),
  loading = ref(false),
  ready = ref(false)
const users = ref<Array<{ id: string; name: string }>>([])
const relations = ref<Array<{ masterId: string; fileName: string; versionNo: string }>>([])
const savedArrangements = ref<ArrangementCommand[]>([])
const arrangementsRef = ref<InstanceType<typeof DetailRelationArrangements>>()
const supplementSource = ref<SelectorSource>()
const supplementalFiles = ref<FileCandidate[]>([])
const supplementVisible = ref(false), supplementLoading = ref(false), supplementError = ref('')
const canSupplement = computed(() => ready.value && context.value?.canAssign && !context.value.assigned
  && context.value.controlledFileId === String(props.fileId)
  && context.value.processInstanceId === props.processInstanceId && context.value.taskId === props.taskId
  && ['dcc-controlled-file-upload', 'dcc-controlled-file-revision'].includes(context.value.processDefinitionKey))
const contextKey = computed(() =>
  JSON.stringify([String(props.fileId), props.processInstanceId, props.taskId])
)
let generation = 0
onBeforeUnmount(() => {
  generation++
})
watch(
  contextKey,
  async () => {
    const token = ++generation
    context.value = undefined
    error.value = ''
    loading.value = true
    ready.value = false
    relations.value = []
    users.value = []
    savedArrangements.value = []
    supplementSource.value = undefined
    supplementalFiles.value = []
    supplementVisible.value = false
    supplementLoading.value = false
    supplementError.value = ''
    emit('state', { fileId: String(props.fileId), taskId: props.taskId, assigned: false })
    try {
      const result = await getSignoffAssignmentContext(props.fileId, props.taskId)
      if (token !== generation) return
      if (result.controlledFileId !== String(props.fileId) || result.taskId !== props.taskId)
        throw new Error('会签响应不属于当前文件和任务')
      if (result.processInstanceId !== props.processInstanceId)
        throw new Error('会签任务不属于当前申请流程')
      if (!['dcc-controlled-file-upload', 'dcc-controlled-file-revision', 'dcc-controlled-file-obsolete'].includes(result.processDefinitionKey))
        throw new Error('会签任务缺少准确的当前流程类型')
      context.value = result
      emit('state', {
        fileId: result.controlledFileId,
        taskId: result.taskId,
        assigned: result.assigned
      })
      if (!result.assigned && !result.canAssign) throw new Error('当前任务不允许本账号指派')
      if (result.processDefinitionKey !== 'dcc-controlled-file-revision') {
        ready.value = true
        return
      }
      const [history, people, saved] = await Promise.all([
        listHistoricalRelations(result.controlledFileId),
        getSimpleUserList(),
        listRelationArrangements(result.controlledFileId, result.processInstanceId)
      ])
      if (token !== generation) return
      if (
        !Array.isArray(history) ||
        history.some((row) => !isWorkflowId(row.masterId) || !row.fileName || !row.versionNo)
      )
        throw new Error('本次审阅关联版本快照不完整')
      relations.value = history.map((row) => ({
        masterId: String(row.masterId),
        fileName: row.fileName,
        versionNo: row.versionNo
      }))
      users.value = people.map((person) => {
        if (!isWorkflowId(person.id) || !person.nickname) throw new Error('整改负责人目录身份缺失')
        return { id: String(person.id), name: person.nickname }
      })
      savedArrangements.value = saved
      ready.value = true
    } catch (cause) {
      if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause)
    } finally {
      if (token === generation) loading.value = false
    }
  },
  { immediate: true, flush: 'sync' }
)
const openSupplement = async () => {
  const current = context.value
  if (!canSupplement.value || supplementLoading.value || !current) return
  const token = generation, key = contextKey.value, fileId = current.controlledFileId
  supplementLoading.value = true; supplementError.value = ''
  try {
    const file = await getControlledFileRelationPermissions(fileId)
    if (token !== generation || key !== contextKey.value || !canSupplement.value) return
    if (file.controlledFileId !== fileId) throw new Error('后补关联来源不属于当前文件和任务')
    supplementSource.value = {
      contextKey: key, controlledFileId: file.controlledFileId, tenantId: file.tenantId,
      masterId: file.masterId, projectId: file.projectId, projectName: file.projectName,
      folderId: file.projectFolderId ?? undefined, folderName: file.projectFolderName ?? '未记录',
      fileName: file.fileName, fileNumber: file.fileNumber, versionNo: file.versionNo
    }
    supplementVisible.value = true
  } catch (cause) {
    if (token === generation) supplementError.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (token === generation) supplementLoading.value = false }
}
const currentSupplementSource = () => {
  const source = supplementSource.value
  if (!canSupplement.value || !supplementVisible.value || !source || source.contextKey !== contextKey.value)
    throw new Error('后补关联上下文已变化，请重新打开当前任务选择器')
  return source
}
const loadSupplementPage = (query: SelectorQuery) => {
  const source = currentSupplementSource()
  return loadSelectorBrowserPage(source.tenantId, { ...query, projectId: source.projectId })
}
const confirmSupplement = async (rows: FileCandidate[]) => {
  const source = currentSupplementSource(), masters = new Set<string>()
  const selected = rows.map(row => {
    if (row.tenantId !== source.tenantId || row.projectId !== source.projectId
      || typeof row.controlledFileId !== 'string' || !isWorkflowId(row.controlledFileId)
      || typeof row.masterId !== 'string' || !isWorkflowId(row.masterId)
      || row.masterId === source.masterId || masters.has(row.masterId)
      || !row.controlled || row.status !== 'ACTIVE' || row.pendingEffect || !row.executable || !row.canPreview)
      throw new Error('只能后补当前项目中有正文查看权限的其他已生效固定版本，且不能重复')
    masters.add(row.masterId)
    return { ...row }
  })
  supplementalFiles.value = selected
}
const previewSupplement = async (row: FileCandidate) => {
  const source = currentSupplementSource()
  if (row.tenantId !== source.tenantId || row.projectId !== source.projectId
    || !isWorkflowId(row.controlledFileId) || !row.canPreview)
    throw new Error('没有当前项目所选固定版本的正文查看权限')
  const opened = window.open(`/dcc/controlled-file/detail/${row.controlledFileId}?viewer=1&from=relation-selector`, '_blank')
  if (!opened) throw new Error('预览窗口未能打开，请允许此站点打开新窗口')
  opened.opener = null
}
const saveAssignment = async (request: SignoffAssignment) => {
  const current = context.value
  if (
    !current ||
    !ready.value ||
    supplementLoading.value || supplementVisible.value ||
    !current.canAssign ||
    (current.processDefinitionKey === 'dcc-controlled-file-revision' && !arrangementsRef.value) ||
    request.taskId !== current.taskId ||
    current.taskId !== props.taskId ||
    current.controlledFileId !== String(props.fileId) ||
    current.processInstanceId !== props.processInstanceId
  )
    throw new Error('指派上下文已变化，请重新读取')
  if (!current.assigneeOptions.some(user => String(user.id) === String(request.assigneeUserId)))
    throw new Error('请从当前本部门正式候选目录选择会签人')
  const payload = { ...request }
  if (supplementalFiles.value.length)
    payload.supplementalRelatedControlledFileIds = supplementalFiles.value.map(file => file.controlledFileId)
  else delete payload.supplementalRelatedControlledFileIds
  if (current.processDefinitionKey === 'dcc-controlled-file-revision') {
    const relationArrangements = arrangementsRef.value!.validate()
    return assignWorkflowSignoff(current.controlledFileId, { ...payload, relationArrangements })
  }
  delete payload.relationArrangements
  return assignWorkflowSignoff(current.controlledFileId, payload)
}
const onSaved = (value: { fileId: number | string; processInstanceId: string; taskId: string }) => {
  if (
    String(value.fileId) === String(props.fileId) &&
    value.processInstanceId === props.processInstanceId &&
    value.taskId === props.taskId
  ) {
    if (context.value) context.value.assigned = true
    emit('state', { fileId: String(value.fileId), taskId: value.taskId, assigned: true })
    emit('saved')
  }
}
</script>
