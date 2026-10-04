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
      :arrangements-ready="ready && context.canAssign"
      :save="saveAssignment"
      @saved="onSaved"
    >
      <template #relation-arrangements="{ disabled }">
        <DetailRelationArrangements
          ref="arrangementsRef"
          :context-key="contextKey"
          :relations="relations"
          :users="users"
          :saved="savedArrangements"
          :disabled="disabled || !ready || !context.canAssign"
        />
      </template>
    </SignoffAssignmentPanel>
  </ContentWrap>
</template>
<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import {
  getSignoffAssignmentContext,
  type SignoffAssignmentContext
} from '@/api/dcc/controlledFile/applicationRead'
import { assignWorkflowSignoff } from '@/api/dcc/controlledFile/workflowLifecycle'
import {
  listHistoricalRelations,
  listRelationArrangements
} from '@/api/dcc/controlledFile/relations'
import { getSimpleUserList } from '@/api/system/user'
import SignoffAssignmentPanel from '../workflow/SignoffAssignmentPanel.vue'
import DetailRelationArrangements from './DetailRelationArrangements.vue'
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
    emit('state', { fileId: String(props.fileId), taskId: props.taskId, assigned: false })
    try {
      const result = await getSignoffAssignmentContext(props.fileId, props.taskId)
      if (token !== generation) return
      if (result.processInstanceId !== props.processInstanceId)
        throw new Error('会签任务不属于当前申请流程')
      context.value = result
      emit('state', {
        fileId: result.controlledFileId,
        taskId: result.taskId,
        assigned: result.assigned
      })
      if (!result.assigned && !result.canAssign) throw new Error('当前任务不允许本账号指派')
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
  { immediate: true }
)
const saveAssignment = async (request: SignoffAssignment) => {
  const current = context.value
  if (
    !current ||
    !ready.value ||
    !current.canAssign ||
    !arrangementsRef.value ||
    current.taskId !== props.taskId ||
    current.controlledFileId !== String(props.fileId) ||
    current.processInstanceId !== props.processInstanceId
  )
    throw new Error('指派上下文已变化，请重新读取')
  const relationArrangements = arrangementsRef.value.validate()
  return assignWorkflowSignoff(current.controlledFileId, { ...request, relationArrangements })
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
