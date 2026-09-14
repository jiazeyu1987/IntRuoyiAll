<template>
  <ContentWrap>
    <div class="edhr-flow-intervention">
      <el-form :inline="true" :model="queryParams" class="edhr-flow-intervention__toolbar" @submit.prevent>
        <div class="edhr-flow-intervention__title">流程干预</div>
        <el-form-item label="对象类型">
          <el-input v-model="queryParams.businessObjectType" clearable class="!w-150px" />
        </el-form-item>
        <el-form-item label="对象ID">
          <el-input v-model="queryParams.businessObjectId" clearable class="!w-160px" />
        </el-form-item>
        <el-form-item label="对象编号">
          <el-input v-model="queryParams.businessObjectCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="流程实例">
          <el-input v-model="queryParams.flowInstanceId" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="queryParams.interventionAction" clearable class="!w-140px">
            <el-option label="退回" value="RETURN" />
            <el-option label="撤回" value="WITHDRAW" />
            <el-option label="转办" value="TRANSFER" />
            <el-option label="加签" value="ADD_SIGN" />
            <el-option label="管理员干预" value="ADMIN_INTERVENE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" show-icon />
      <el-alert v-if="interventionError" :title="interventionError" type="error" :closable="false" show-icon />

      <div class="edhr-flow-intervention__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无流程干预记录"
        >
          <el-table-column label="干预编号" min-width="210">
            <template #default="{ row }">
              <div class="edhr-flow-intervention__strong">{{ row.interventionCode || '--' }}</div>
              <div class="edhr-flow-intervention__muted">{{ row.businessObjectType }} / {{ row.businessObjectId }}</div>
            </template>
          </el-table-column>
          <el-table-column label="业务对象" min-width="220">
            <template #default="{ row }">
              <div class="edhr-flow-intervention__strong">{{ row.businessObjectCode || '--' }}</div>
              <div class="edhr-flow-intervention__muted">流程实例：{{ row.flowInstanceId || '--' }}</div>
              <div class="edhr-flow-intervention__muted">节点：{{ row.nodeKey || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="动作" width="130">
            <template #default="{ row }">
              <el-tag :type="resolveActionTag(row.interventionAction)">
                {{ resolveActionLabel(row.interventionAction) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态变化" width="180">
            <template #default="{ row }">
              <div>{{ row.fromStatus || '--' }}</div>
              <div class="edhr-flow-intervention__muted">到 {{ row.toStatus || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="原因" min-width="260">
            <template #default="{ row }">
              <div>{{ row.reasonCategory || '--' }}</div>
              <div class="edhr-flow-intervention__muted">{{ row.reason || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="签核证据" min-width="220" prop="signoffEvidenceHash" />
          <el-table-column label="完整性复检" width="140">
            <template #default="{ row }">
              <el-tag :type="resolveIntegrityTag(row.integrityCheckResult)">
                {{ resolveIntegrityLabel(row.integrityCheckResult) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="幂等键" min-width="220" prop="idempotencyKey" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <div class="edhr-flow-intervention__actions">
                <el-button
                  v-hasPermi="['mes:pro-edhr-flow-intervention:return']"
                  link
                  type="primary"
                  @click="openReturnDialog(row)"
                >
                  退回
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-flow-intervention:withdraw']"
                  link
                  type="warning"
                  @click="openWithdrawDialog(row)"
                >
                  撤回
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-flow-intervention:transfer']"
                  link
                  type="primary"
                  @click="openTransferDialog(row)"
                >
                  转办
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-flow-intervention:add-sign']"
                  link
                  type="primary"
                  @click="openAddSignDialog(row)"
                >
                  加签
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-flow-intervention:admin-intervene']"
                  link
                  type="danger"
                  @click="openAdminInterveneDialog(row)"
                >
                  管理员干预
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-flow-intervention:event-query']"
                  link
                  type="primary"
                  @click="openEventDrawer(row)"
                >
                  流程日志
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>

      <el-drawer v-model="eventDrawerVisible" title="流程日志" size="82%" class="edhr-flow-intervention__drawer">
        <el-table
          v-loading="eventLoading"
          :data="flowEventList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无流程日志"
        >
          <el-table-column label="事件" width="150" prop="eventType" />
          <el-table-column label="状态变化" width="180">
            <template #default="{ row }">
              {{ row.fromStatus || '--' }} -> {{ row.toStatus || '--' }}
            </template>
          </el-table-column>
          <el-table-column label="权限" min-width="250">
            <template #default="{ row }">
              <div>{{ row.permissionCode || '--' }}</div>
              <div class="edhr-flow-intervention__muted">{{ row.permissionDecision || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="目标处理人" width="130" prop="targetUserId" />
          <el-table-column label="原因" min-width="240" prop="reason" />
          <el-table-column label="签核证据" min-width="230" prop="signoffEvidenceHash" />
          <el-table-column label="完整性复检" width="140">
            <template #default="{ row }">
              <el-tag :type="resolveIntegrityTag(row.integrityCheckResult)">
                {{ resolveIntegrityLabel(row.integrityCheckResult) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="证据摘要" min-width="230" prop="evidenceHash" />
          <el-table-column label="发生时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="eventTotal"
          v-model:page="eventQuery.pageNo"
          v-model:limit="eventQuery.pageSize"
          @pagination="loadFlowEventList"
        />
      </el-drawer>

      <el-dialog v-model="interventionDialogVisible" :title="interventionDialogTitle" width="620px">
        <el-alert
          v-if="selectedRow"
          :title="`${selectedRow.businessObjectType || '--'} / ${selectedRow.businessObjectId || '--'}`"
          type="info"
          :closable="false"
          show-icon
          class="edhr-flow-intervention__dialog-alert"
        />
        <el-form label-width="110px" class="edhr-flow-intervention__dialog-form">
          <el-form-item label="对象类型">
            <el-input v-model="interventionForm.businessObjectType" />
          </el-form-item>
          <el-form-item label="对象ID">
            <el-input v-model="interventionForm.businessObjectId" />
          </el-form-item>
          <el-form-item label="对象编号">
            <el-input v-model="interventionForm.businessObjectCode" />
          </el-form-item>
          <el-form-item label="流程实例">
            <el-input v-model="interventionForm.flowInstanceId" />
          </el-form-item>
          <el-form-item label="任务ID">
            <el-input v-model="interventionForm.taskId" />
          </el-form-item>
          <el-form-item label="节点">
            <el-input v-model="interventionForm.nodeKey" />
          </el-form-item>
          <el-form-item label="状态变化">
            <div class="edhr-flow-intervention__status-row">
              <el-input v-model="interventionForm.fromStatus" />
              <span>到</span>
              <el-input v-model="interventionForm.toStatus" />
            </div>
          </el-form-item>
          <el-form-item v-if="requiresTargetUser" label="目标处理人">
            <el-select
              v-model="interventionForm.targetUserId"
              class="!w-100%"
              filterable
              clearable
              :loading="targetUserLoading"
              placeholder="选择目标处理人"
            >
              <el-option
                v-for="user in targetUserOptions"
                :key="user.id"
                :label="resolveTargetUserLabel(user)"
                :value="user.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="原因分类">
            <el-input v-model="interventionForm.reasonCategory" />
          </el-form-item>
          <el-form-item label="原因">
            <el-input v-model="interventionForm.reason" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item v-if="interventionMode === 'ADMIN_INTERVENE'" label="授权依据">
            <el-input v-model="interventionForm.authorizationBasis" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="签核证据">
            <el-input v-model="interventionForm.signoffEvidenceHash" />
          </el-form-item>
          <el-form-item label="幂等键">
            <el-input v-model="interventionForm.idempotencyKey" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="interventionDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="interventionSubmitting" @click="handleInterventionConfirm">
            确认
          </el-button>
        </template>
      </el-dialog>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  getEdhrFlowEventPage,
  getEdhrFlowInterventionPage,
  submitAddSignIntervention,
  submitAdminIntervention,
  submitReturnIntervention,
  submitTransferIntervention,
  submitWithdrawIntervention,
  type EdhrFlowEventPageReqVO,
  type EdhrFlowEventRespVO,
  type EdhrFlowInterventionAction,
  type EdhrFlowInterventionBaseReqVO,
  type EdhrFlowInterventionPageReqVO,
  type EdhrFlowInterventionRespVO
} from '@/api/mes/pro/edhr/flowIntervention'
import * as UserApi from '@/api/system/user'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrFlowIntervention' })

const message = useMessage()

const loading = ref(false)
const eventLoading = ref(false)
const interventionSubmitting = ref(false)
const targetUserLoading = ref(false)
const loadError = ref('')
const actionError = ref('')
const interventionError = ref('')
const list = ref<EdhrFlowInterventionRespVO[]>([])
const flowEventList = ref<EdhrFlowEventRespVO[]>([])
const targetUserOptions = ref<UserApi.UserVO[]>([])
const total = ref(0)
const eventTotal = ref(0)
const eventDrawerVisible = ref(false)
const interventionDialogVisible = ref(false)
const interventionMode = ref<EdhrFlowInterventionAction>('RETURN')
const selectedRow = ref<EdhrFlowInterventionRespVO>()

const queryParams = reactive<EdhrFlowInterventionPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  businessObjectType: '',
  businessObjectId: '',
  businessObjectCode: '',
  flowInstanceId: '',
  interventionAction: '',
  interventionStatus: ''
})

const eventQuery = reactive<EdhrFlowEventPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  businessObjectType: '',
  businessObjectId: '',
  flowInstanceId: '',
  eventType: ''
})

const interventionForm = reactive<EdhrFlowInterventionBaseReqVO & { authorizationBasis?: string }>({
  businessObjectType: '',
  businessObjectId: '',
  businessObjectCode: '',
  flowInstanceId: '',
  taskId: '',
  nodeKey: '',
  fromStatus: '',
  toStatus: '',
  targetTaskId: '',
  targetUserId: undefined,
  reasonCategory: '',
  reason: '',
  signoffEvidenceHash: '',
  idempotencyKey: '',
  authorizationBasis: ''
})

const interventionDialogTitle = computed(() => resolveActionLabel(interventionMode.value))
const requiresTargetUser = computed(() => interventionMode.value === 'TRANSFER' || interventionMode.value === 'ADD_SIGN')

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return fallback
}

const buildQuery = () => ({
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  businessObjectType: queryParams.businessObjectType?.trim() || undefined,
  businessObjectId: queryParams.businessObjectId?.trim() || undefined,
  businessObjectCode: queryParams.businessObjectCode?.trim() || undefined,
  flowInstanceId: queryParams.flowInstanceId?.trim() || undefined,
  interventionAction: queryParams.interventionAction || undefined,
  interventionStatus: queryParams.interventionStatus || undefined
})

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrFlowInterventionPage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR流程干预列表加载失败，请查看后端错误信息。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.businessObjectType = ''
  queryParams.businessObjectId = ''
  queryParams.businessObjectCode = ''
  queryParams.flowInstanceId = ''
  queryParams.interventionAction = ''
  queryParams.interventionStatus = ''
  getList()
}

const resetInterventionForm = (mode: EdhrFlowInterventionAction, row?: EdhrFlowInterventionRespVO) => {
  interventionForm.businessObjectType = row?.businessObjectType || queryParams.businessObjectType || ''
  interventionForm.businessObjectId = row?.businessObjectId || queryParams.businessObjectId || ''
  interventionForm.businessObjectCode = row?.businessObjectCode || queryParams.businessObjectCode || ''
  interventionForm.flowInstanceId = row?.flowInstanceId || queryParams.flowInstanceId || ''
  interventionForm.taskId = row?.sourceTaskId || ''
  interventionForm.nodeKey = row?.nodeKey || ''
  interventionForm.fromStatus = row?.toStatus || row?.fromStatus || ''
  interventionForm.toStatus = ''
  interventionForm.targetTaskId = row?.targetTaskId || ''
  interventionForm.targetUserId = undefined
  interventionForm.reasonCategory = ''
  interventionForm.reason = ''
  interventionForm.signoffEvidenceHash = ''
  interventionForm.idempotencyKey = buildIdempotencyKey(mode, row)
  interventionForm.authorizationBasis = ''
}

const openInterventionDialog = (mode: EdhrFlowInterventionAction, row?: EdhrFlowInterventionRespVO) => {
  interventionMode.value = mode
  selectedRow.value = row
  actionError.value = ''
  interventionError.value = ''
  resetInterventionForm(mode, row)
  interventionDialogVisible.value = true
  if ((mode === 'TRANSFER' || mode === 'ADD_SIGN') && !targetUserOptions.value.length) {
    void loadTargetUserOptions()
  }
}

const openReturnDialog = (row?: EdhrFlowInterventionRespVO) => openInterventionDialog('RETURN', row)
const openWithdrawDialog = (row?: EdhrFlowInterventionRespVO) => openInterventionDialog('WITHDRAW', row)
const openTransferDialog = (row?: EdhrFlowInterventionRespVO) => openInterventionDialog('TRANSFER', row)
const openAddSignDialog = (row?: EdhrFlowInterventionRespVO) => openInterventionDialog('ADD_SIGN', row)
const openAdminInterveneDialog = (row?: EdhrFlowInterventionRespVO) => openInterventionDialog('ADMIN_INTERVENE', row)

const buildIdempotencyKey = (mode: EdhrFlowInterventionAction, row?: EdhrFlowInterventionRespVO) => {
  const objectId = row?.businessObjectId || queryParams.businessObjectId || 'object'
  return `EDHR-FLOW-${mode}-${objectId}-${Date.now()}`
}

const buildInterventionPayload = () => {
  const payload = {
    businessObjectType: interventionForm.businessObjectType.trim(),
    businessObjectId: interventionForm.businessObjectId.trim(),
    businessObjectCode: interventionForm.businessObjectCode?.trim() || undefined,
    flowInstanceId: interventionForm.flowInstanceId?.trim() || undefined,
    taskId: interventionForm.taskId?.trim() || undefined,
    nodeKey: interventionForm.nodeKey?.trim() || undefined,
    fromStatus: interventionForm.fromStatus.trim(),
    toStatus: interventionForm.toStatus.trim(),
    targetTaskId: interventionForm.targetTaskId?.trim() || undefined,
    targetUserId: interventionForm.targetUserId,
    reasonCategory: interventionForm.reasonCategory?.trim() || undefined,
    reason: interventionForm.reason.trim(),
    signoffEvidenceHash: interventionForm.signoffEvidenceHash.trim(),
    idempotencyKey: interventionForm.idempotencyKey.trim(),
    authorizationBasis: interventionForm.authorizationBasis?.trim() || undefined
  }
  return payload
}

const validateInterventionForm = () => {
  const payload = buildInterventionPayload()
  if (!payload.businessObjectType || !payload.businessObjectId || !payload.fromStatus || !payload.toStatus) {
    interventionError.value = '对象、原状态和目标状态不能为空。'
    return undefined
  }
  if (!payload.reason) {
    interventionError.value = '原因不能为空。'
    return undefined
  }
  if (!payload.signoffEvidenceHash) {
    interventionError.value = '签核证据不能为空。'
    return undefined
  }
  if (!payload.idempotencyKey) {
    interventionError.value = '幂等键不能为空。'
    return undefined
  }
  if (requiresTargetUser.value && !payload.targetUserId) {
    interventionError.value = '目标处理人不能为空。'
    return undefined
  }
  if (interventionMode.value === 'ADMIN_INTERVENE' && !payload.authorizationBasis) {
    interventionError.value = '授权依据不能为空。'
    return undefined
  }
  return payload
}

const loadTargetUserOptions = async () => {
  targetUserLoading.value = true
  try {
    targetUserOptions.value = await UserApi.getSimpleUserList()
  } catch (error) {
    targetUserOptions.value = []
    interventionError.value = resolveErrorMessage(error, '目标处理人列表加载失败。')
    message.error(interventionError.value)
  } finally {
    targetUserLoading.value = false
  }
}

const submitReturnInterventionAction = async () => {
  const payload = validateInterventionForm()
  if (!payload) return
  await runInterventionAction(async () => {
    await submitReturnIntervention(payload)
    message.success('退回已记录')
  })
}

const submitWithdrawInterventionAction = async () => {
  const payload = validateInterventionForm()
  if (!payload) return
  await runInterventionAction(async () => {
    await submitWithdrawIntervention(payload)
    message.success('撤回已记录')
  })
}

const submitTransferInterventionAction = async () => {
  const payload = validateInterventionForm()
  if (!payload) return
  const targetUserId = payload.targetUserId
  if (targetUserId === undefined) return
  await runInterventionAction(async () => {
    await submitTransferIntervention({ ...payload, targetUserId })
    message.success('转办已生效')
  })
}

const submitAddSignInterventionAction = async () => {
  const payload = validateInterventionForm()
  if (!payload) return
  const targetUserId = payload.targetUserId
  if (targetUserId === undefined) return
  await runInterventionAction(async () => {
    await submitAddSignIntervention({ ...payload, targetUserId })
    message.success('加签待办已创建')
  })
}

const submitAdminInterventionAction = async () => {
  const payload = validateInterventionForm()
  if (!payload) return
  const authorizationBasis = payload.authorizationBasis
  if (authorizationBasis === undefined) return
  await runInterventionAction(async () => {
    await submitAdminIntervention({ ...payload, authorizationBasis })
    message.success('管理员干预已记录')
  })
}

const runInterventionAction = async (action: () => Promise<void>) => {
  interventionSubmitting.value = true
  interventionError.value = ''
  try {
    await action()
    interventionDialogVisible.value = false
    await getList()
    if (eventDrawerVisible.value) {
      await loadFlowEventList()
    }
  } catch (error) {
    interventionError.value = resolveErrorMessage(error, 'eDHR流程干预提交失败，请查看后端错误信息。')
    message.error(resolveErrorMessage(error, interventionError.value))
  } finally {
    interventionSubmitting.value = false
  }
}

const handleInterventionConfirm = async () => {
  if (interventionMode.value === 'WITHDRAW') {
    await submitWithdrawInterventionAction()
    return
  }
  if (interventionMode.value === 'TRANSFER') {
    await submitTransferInterventionAction()
    return
  }
  if (interventionMode.value === 'ADD_SIGN') {
    await submitAddSignInterventionAction()
    return
  }
  if (interventionMode.value === 'ADMIN_INTERVENE') {
    await submitAdminInterventionAction()
    return
  }
  await submitReturnInterventionAction()
}

const openEventDrawer = async (row: EdhrFlowInterventionRespVO) => {
  if (!row.businessObjectType || !row.businessObjectId) {
    actionError.value = '业务对象缺失，无法查询流程日志。'
    return
  }
  eventQuery.pageNo = 1
  eventQuery.businessObjectType = row.businessObjectType
  eventQuery.businessObjectId = row.businessObjectId
  eventQuery.flowInstanceId = row.flowInstanceId || ''
  eventDrawerVisible.value = true
  await loadFlowEventList()
}

const loadFlowEventList = async () => {
  if (!eventQuery.businessObjectType || !eventQuery.businessObjectId) {
    flowEventList.value = []
    eventTotal.value = 0
    return
  }
  eventLoading.value = true
  actionError.value = ''
  try {
    const data = await getEdhrFlowEventPage(eventQuery)
    flowEventList.value = data.list || []
    eventTotal.value = data.total || 0
  } catch (error) {
    flowEventList.value = []
    eventTotal.value = 0
    actionError.value = resolveErrorMessage(error, 'eDHR流程日志加载失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  } finally {
    eventLoading.value = false
  }
}

const resolveActionLabel = (action?: string) => {
  if (action === 'WITHDRAW') return '撤回'
  if (action === 'TRANSFER') return '转办'
  if (action === 'ADD_SIGN') return '加签'
  if (action === 'ADMIN_INTERVENE') return '管理员干预'
  return '退回'
}

const resolveActionTag = (action?: string) => {
  if (action === 'WITHDRAW') return 'warning'
  if (action === 'ADMIN_INTERVENE') return 'danger'
  if (action === 'TRANSFER' || action === 'ADD_SIGN') return 'primary'
  return 'info'
}

const resolveIntegrityLabel = (result?: string) => {
  if (result === 'PASS') return '通过'
  if (result === 'FAIL') return '失败'
  if (result === 'RECHECK_REQUIRED') return '待复检'
  return result || '--'
}

const resolveIntegrityTag = (result?: string) => {
  if (result === 'PASS') return 'success'
  if (result === 'FAIL') return 'danger'
  return 'warning'
}

const resolveTargetUserLabel = (user: UserApi.UserVO) => {
  return `${user.nickname || user.username || user.id} / ${user.username || '--'} / ${user.id}`
}

const formatDateTime = (value?: string | number) => {
  return formatEdhrDateTime(value)
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.edhr-flow-intervention {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.edhr-flow-intervention__toolbar,
.edhr-flow-intervention__table {
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-flow-intervention__toolbar {
  padding: 16px 16px 0;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.edhr-flow-intervention__title {
  width: 100%;
  margin-bottom: 12px;
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.edhr-flow-intervention__table {
  padding: 16px;
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.edhr-flow-intervention__strong {
  color: #172033;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-flow-intervention__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-flow-intervention__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.edhr-flow-intervention__drawer :deep(.el-drawer__body) {
  padding-top: 8px;
}

.edhr-flow-intervention__dialog-alert {
  margin-bottom: 12px;
}

.edhr-flow-intervention__dialog-form {
  padding-top: 4px;
}

.edhr-flow-intervention__status-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  width: 100%;
}
</style>
