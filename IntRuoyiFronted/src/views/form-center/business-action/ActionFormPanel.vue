<template>
  <div class="form-action-panel">
    <el-alert
      v-if="blockerCode"
      :title="blockerTitle"
      :closable="false"
      show-icon
      type="error"
    />

    <div class="form-action-panel__actions">
      <el-button :loading="loading" type="primary" @click="resolveAction">
        <Icon class="mr-5px" icon="ep:connection" />
        解析
      </el-button>
      <el-button
        :disabled="disabled || Boolean(instanceId) || !resolution?.requiresForm"
        :loading="loading"
        type="primary"
        @click="createInstance"
      >
        <Icon class="mr-5px" icon="ep:document-add" />
        创建
      </el-button>
      <el-button
        :disabled="disabled || !instanceId"
        :loading="loading"
        type="primary"
        plain
        @click="saveDraft"
      >
        <Icon class="mr-5px" icon="ep:document-checked" />
        保存草稿
      </el-button>
      <el-button
        :disabled="disabled || !instanceId"
        :loading="loading"
        type="success"
        @click="submitInstance"
      >
        <Icon class="mr-5px" icon="ep:promotion" />
        提交
      </el-button>
      <el-button
        :disabled="disabled || !instanceId"
        :loading="loading"
        type="warning"
        @click="reworkSubmit"
      >
        <Icon class="mr-5px" icon="ep:refresh-left" />
        重提
      </el-button>
      <el-button
        :disabled="disabled || !instanceId"
        :loading="loading"
        type="danger"
        @click="abandonInstance"
      >
        <Icon class="mr-5px" icon="ep:close" />
        放弃
      </el-button>
    </div>

    <el-descriptions v-if="instanceId" :column="3" border size="small">
      <el-descriptions-item label="实例编号">{{ instanceCode }}</el-descriptions-item>
      <el-descriptions-item label="实例ID">{{ instanceId }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType(instanceStatus)">{{ statusLabel(instanceStatus) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="bpmProcessInstanceId" label="BPM流程">
        {{ bpmProcessInstanceId }}
      </el-descriptions-item>
    </el-descriptions>

    <el-collapse v-if="snapshots.length" class="form-action-panel__snapshots">
      <el-collapse-item title="快照" name="snapshots">
        <el-table :data="snapshots" border size="small">
          <el-table-column label="版本" prop="snapshotVersion" width="90" />
          <el-table-column label="类型" prop="snapshotType" width="140">
            <template #default="{ row }">
              <el-tag>{{ snapshotTypeLabel(row.snapshotType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" prop="createdTime" width="180" :formatter="dateTimeValueFormatter" />
          <el-table-column label="表单数据" min-width="260">
            <template #default="{ row }">
              <span class="form-action-panel__json-preview">{{ stringifySnapshot(row.formData) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
    </el-collapse>

    <el-table v-if="resolution?.slots?.length" :data="resolution.slots" border size="small">
      <el-table-column label="槽位" prop="slotCode" width="160" />
      <el-table-column label="模板名称" min-width="180">
        <template #default="{ row }">
          {{ row.templateVersionRef.templateName }}
        </template>
      </el-table-column>
      <el-table-column label="版本" width="120">
        <template #default="{ row }">
          {{ row.templateVersionRef.versionNo }}
        </template>
      </el-table-column>
      <el-table-column label="必填" prop="required" width="100">
        <template #default="{ row }">
          <el-tag :type="row.required ? 'danger' : 'info'">{{ row.required ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import {
  resolveBusinessAction,
  type BusinessActionContextVO,
  type FormActionResolutionVO
} from '@/api/form-center/businessAction'
import { resolveProjectionErrorMessage } from '@/api/form-center/actionProjection'
import {
  abandonFormInstance,
  createFormInstance,
  getInstanceSnapshots,
  reworkSubmitFormInstance,
  saveFormDraft,
  submitFormInstance,
  type FormInstanceSnapshotVO,
  type FormInstanceStatus,
  type SubmitFormInstanceReqVO
} from '@/api/form-center/instance'
import { dateTimeValueFormatter } from '@/utils/formatTime'

defineOptions({ name: 'FormCenterBusinessActionPanel' })

const props = defineProps<{
  context: BusinessActionContextVO
  formData: Record<string, unknown>
  idempotencyKey: string
  disabled?: boolean
  initialInstanceId?: number
  initialInstanceCode?: string
  initialInstanceStatus?: FormInstanceStatus
  initialBpmProcessInstanceId?: string
}>()

const message = useMessage()
const loading = ref(false)
const resolution = ref<FormActionResolutionVO>()
const instanceId = ref<number>()
const instanceCode = ref('')
const instanceStatus = ref<FormInstanceStatus | ''>('')
const bpmProcessInstanceId = ref('')
const snapshots = ref<FormInstanceSnapshotVO[]>([])
const blockerCode = ref('')

watch(
  () => [
    props.initialInstanceId,
    props.initialInstanceCode,
    props.initialInstanceStatus,
    props.initialBpmProcessInstanceId
  ],
  async ([nextInstanceId, nextInstanceCode, nextInstanceStatus, nextBpmProcessInstanceId]) => {
    if (!nextInstanceId) return
    instanceId.value = Number(nextInstanceId)
    instanceCode.value = String(nextInstanceCode || nextInstanceId)
    instanceStatus.value = (nextInstanceStatus as FormInstanceStatus | undefined) || 'DRAFT'
    bpmProcessInstanceId.value = String(nextBpmProcessInstanceId || '')
    await loadSnapshots()
  },
  { immediate: true }
)

const blockerTitle = computed(() => {
  if (blockerCode.value === 'FORM_POLICY_NOT_FOUND') return '未找到业务审批策略'
  if (blockerCode.value === 'BPM_BINDING_MISSING') return '审批流程未配置'
  return blockerCode.value
})

const surfaceError = (error: unknown) => {
  const response = (error as any)?.response?.data
  const visibleMessage = resolveProjectionErrorMessage(error, '业务动作')
  blockerCode.value = response?.code || visibleMessage || 'FORM_ACTION_BLOCKED'
  message.error(visibleMessage)
}

const runVisibleAction = async (action: () => Promise<void>) => {
  loading.value = true
  blockerCode.value = ''
  try {
    await action()
  } catch (error) {
    surfaceError(error)
    throw error
  } finally {
    loading.value = false
  }
}

const buildSubmitPayload = (): SubmitFormInstanceReqVO => {
  const payload: SubmitFormInstanceReqVO = { formData: props.formData }
  const selectedAssignees = props.formData.startUserSelectAssignees
  if (selectedAssignees === undefined || selectedAssignees === null) {
    return payload
  }
  if (Array.isArray(selectedAssignees) || typeof selectedAssignees !== 'object') {
    throw new Error('startUserSelectAssignees 必须是对象')
  }
  const normalized: Record<string, number[]> = {}
  for (const [taskKey, assignees] of Object.entries(selectedAssignees)) {
    if (!taskKey || !Array.isArray(assignees)) {
      throw new Error('startUserSelectAssignees 的每个节点必须配置审批人数组')
    }
    const userIds = assignees.map((item) => Number(item)).filter((item) => Number.isInteger(item) && item > 0)
    if (userIds.length !== assignees.length) {
      throw new Error('startUserSelectAssignees 只能包含正整数用户ID')
    }
    normalized[taskKey] = userIds
  }
  payload.startUserSelectAssignees = normalized
  return payload
}

const statusLabel = (status: FormInstanceStatus | '') => {
  const labels: Record<FormInstanceStatus, string> = {
    DRAFT: '草稿',
    IN_APPROVAL: '审批中',
    REWORKING: '返工中',
    REJECTED: '已驳回',
    ABANDONED: '已放弃',
    PENDING_EFFECT: '待生效',
    EFFECTIVE: '已生效',
    EFFECT_FAILED_PENDING: '生效失败待处理'
  }
  return status ? labels[status] : '-'
}

const statusTagType = (status: FormInstanceStatus | '') => {
  if (status === 'EFFECTIVE') return 'success'
  if (status === 'EFFECT_FAILED_PENDING') return 'danger'
  if (status === 'PENDING_EFFECT' || status === 'IN_APPROVAL') return 'warning'
  if (status === 'REJECTED' || status === 'ABANDONED') return 'info'
  return 'primary'
}

const snapshotTypeLabel = (snapshotType: string) => {
  if (snapshotType === 'SUBMIT') return '提交快照'
  if (snapshotType === 'REWORK_SUBMIT') return '返工提交快照'
  return '草稿快照'
}

const stringifySnapshot = (formData: Record<string, unknown>) => {
  return JSON.stringify(formData || {})
}

async function loadSnapshots() {
  if (!instanceId.value) return
  snapshots.value = await getInstanceSnapshots(instanceId.value)
}

const resolveAction = async () => {
  await runVisibleAction(async () => {
    resolution.value = await resolveBusinessAction(props.context)
    if (resolution.value.policyType === 'NONE') {
      message.success('已匹配业务审批策略，无需补充表单')
    }
  })
}

const createInstance = async () => {
  await runVisibleAction(async () => {
    const created = await createFormInstance({
      context: props.context,
      idempotencyKey: props.idempotencyKey,
      formData: props.formData
    })
    instanceId.value = created.id
    instanceCode.value = created.instanceCode
    instanceStatus.value = created.status
    bpmProcessInstanceId.value = created.bpmProcessInstanceId || ''
    await loadSnapshots()
  })
}

const submitInstance = async () => {
  if (!instanceId.value) return
  await runVisibleAction(async () => {
    const submitted = await submitFormInstance(instanceId.value!, buildSubmitPayload())
    instanceStatus.value = submitted.status
    bpmProcessInstanceId.value = submitted.bpmProcessInstanceId || ''
    await loadSnapshots()
    message.success('已提交')
  })
}

const saveDraft = async () => {
  if (!instanceId.value) return
  await runVisibleAction(async () => {
    await saveFormDraft(instanceId.value!, { formData: props.formData })
    instanceStatus.value = 'DRAFT'
    await loadSnapshots()
    message.success('草稿已保存')
  })
}

const reworkSubmit = async () => {
  if (!instanceId.value) return
  await runVisibleAction(async () => {
    await reworkSubmitFormInstance(instanceId.value!, buildSubmitPayload())
    instanceStatus.value = 'IN_APPROVAL'
    await loadSnapshots()
    message.success('已重提')
  })
}

const abandonInstance = async () => {
  if (!instanceId.value) return
  await runVisibleAction(async () => {
    await abandonFormInstance(instanceId.value!)
    instanceStatus.value = 'ABANDONED'
    message.success('已放弃')
  })
}
</script>

<style scoped>
.form-action-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-action-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.form-action-panel__snapshots {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  padding: 0 12px;
}

.form-action-panel__json-preview {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #4b5563;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
