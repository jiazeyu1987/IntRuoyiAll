<template>
  <ContentWrap>
    <div class="edhr-unified-change">
      <el-form :inline="true" :model="queryParams" class="edhr-unified-change__toolbar" @submit.prevent>
        <div class="edhr-unified-change__title">统一变更</div>
        <el-form-item label="对象类型">
          <el-select v-model="queryParams.controlledObjectType" clearable class="!w-170px">
            <el-option label="表单模板" value="FORM_TEMPLATE" />
            <el-option label="DHR模板" value="DHR_TEMPLATE" />
            <el-option label="记录本模板" value="RECORDBOOK_TEMPLATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象ID">
          <el-input v-model="queryParams.controlledObjectId" clearable class="!w-160px" />
        </el-form-item>
        <el-form-item label="对象编号">
          <el-input v-model="queryParams.controlledObjectCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.changeStatus" clearable class="!w-150px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已审批" value="APPROVED" />
            <el-option label="生效阻断" value="EFFECT_BLOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="queryParams.riskLevel" clearable class="!w-140px">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="关键" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button
            v-hasPermi="['mes:pro-edhr-change:unified-create']"
            type="primary"
            plain
            @click="openCreateDialog"
          >
            新建
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" show-icon />
      <el-alert v-if="changeError" :title="changeError" type="error" :closable="false" show-icon />

      <div class="edhr-unified-change__table">
        <el-alert
          title="历史版本不可覆盖；生效申请只记录阻断状态，待正式版本生效适配器接入后处理。"
          type="warning"
          :closable="false"
          show-icon
          class="edhr-unified-change__warning"
        />
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无统一变更记录"
        >
          <el-table-column label="变更编号" min-width="220">
            <template #default="{ row }">
              <div class="edhr-unified-change__strong">{{ row.changeCode || '--' }}</div>
              <div class="edhr-unified-change__muted">
                {{ resolveObjectTypeLabel(row.controlledObjectType) }} / {{ row.controlledObjectId }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="业务对象" min-width="220">
            <template #default="{ row }">
              <div class="edhr-unified-change__strong">{{ row.controlledObjectCode || '--' }}</div>
              <div class="edhr-unified-change__muted">原版本：{{ row.currentVersion || '--' }}</div>
              <div class="edhr-unified-change__muted">目标版本：{{ row.targetVersion || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="resolveStatusTag(row.changeStatus)">
                {{ resolveStatusLabel(row.changeStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="120">
            <template #default="{ row }">
              <el-tag :type="resolveRiskTag(row.riskLevel)">
                {{ resolveRiskLabel(row.riskLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原因" min-width="260">
            <template #default="{ row }">
              <div>{{ row.reasonCategory || '--' }}</div>
              <div class="edhr-unified-change__muted">{{ row.reason || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="影响范围" min-width="260">
            <template #default="{ row }">
              <div class="edhr-unified-change__json">{{ row.impactSummaryJson || '--' }}</div>
              <div class="edhr-unified-change__muted">复算：{{ formatDateTime(row.impactRecalculatedAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="复算摘要" min-width="220" prop="impactRecalculationHash" />
          <el-table-column label="幂等键" min-width="220" prop="idempotencyKey" />
          <el-table-column label="操作" width="330" fixed="right">
            <template #default="{ row }">
              <div class="edhr-unified-change__actions">
                <el-button
                  v-hasPermi="['mes:pro-edhr-change:impact-query']"
                  link
                  type="primary"
                  @click="openImpactDrawer(row)"
                >
                  影响范围
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-change:event-query']"
                  link
                  type="primary"
                  @click="openEventDrawer(row)"
                >
                  事件
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-change:unified-submit']"
                  link
                  type="primary"
                  @click="openSubmitDialog(row)"
                >
                  提交
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-change:unified-submit']"
                  link
                  type="warning"
                  @click="handleRecalculateImpact(row)"
                >
                  复算
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-change:unified-approve']"
                  link
                  type="success"
                  @click="openApproveDialog(row)"
                >
                  审批
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-change:unified-effect']"
                  link
                  type="danger"
                  @click="openEffectDialog(row)"
                >
                  生效申请
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

      <el-drawer v-model="impactDrawerVisible" title="影响范围" size="82%" class="edhr-unified-change__drawer">
        <el-table
          v-loading="impactLoading"
          :data="impactList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无影响范围"
        >
          <el-table-column label="影响类型" width="140" prop="impactType" />
          <el-table-column label="影响对象" min-width="240">
            <template #default="{ row }">
              <div>{{ row.impactObjectType }} / {{ row.impactObjectId }}</div>
              <div class="edhr-unified-change__muted">{{ row.impactObjectCode || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="120">
            <template #default="{ row }">{{ resolveRiskLabel(row.riskLevel) }}</template>
          </el-table-column>
          <el-table-column label="责任模块" width="140" prop="responsibilityModule" />
          <el-table-column label="再培训" width="100">
            <template #default="{ row }">{{ row.requiresTraining ? '需要' : '不需要' }}</template>
          </el-table-column>
          <el-table-column label="再验证" width="100">
            <template #default="{ row }">{{ row.requiresRevalidation ? '需要' : '不需要' }}</template>
          </el-table-column>
          <el-table-column label="放行复检" width="110">
            <template #default="{ row }">{{ row.requiresReleaseRecheck ? '需要' : '不需要' }}</template>
          </el-table-column>
          <el-table-column label="影响说明" min-width="260" prop="impactDetail" />
          <el-table-column label="后续动作" min-width="240" prop="nextAction" />
        </el-table>
        <Pagination
          :total="impactTotal"
          v-model:page="impactQuery.pageNo"
          v-model:limit="impactQuery.pageSize"
          @pagination="loadImpactList"
        />
      </el-drawer>

      <el-drawer v-model="eventDrawerVisible" title="统一变更事件" size="82%" class="edhr-unified-change__drawer">
        <el-table
          v-loading="eventLoading"
          :data="eventList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无统一变更事件"
        >
          <el-table-column label="事件" width="170" prop="eventType" />
          <el-table-column label="状态变化" width="190">
            <template #default="{ row }">{{ row.fromStatus || '--' }} -> {{ row.toStatus || '--' }}</template>
          </el-table-column>
          <el-table-column label="原因" min-width="260" prop="reason" />
          <el-table-column label="签核证据" min-width="220" prop="signoffEvidenceHash" />
          <el-table-column label="事件摘要" min-width="230" prop="evidenceHash" />
          <el-table-column label="发生时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="eventTotal"
          v-model:page="eventQuery.pageNo"
          v-model:limit="eventQuery.pageSize"
          @pagination="loadEventList"
        />
      </el-drawer>

      <el-dialog v-model="changeDialogVisible" :title="changeDialogTitle" width="720px">
        <el-alert
          v-if="selectedRow"
          :title="`${selectedRow.changeCode || '--'} / ${selectedRow.controlledObjectCode || '--'}`"
          type="info"
          :closable="false"
          show-icon
          class="edhr-unified-change__dialog-alert"
        />
        <el-form label-width="110px" class="edhr-unified-change__dialog-form">
          <template v-if="changeMode === 'CREATE'">
            <el-form-item label="对象类型">
              <el-select v-model="changeForm.controlledObjectType" class="!w-100%">
                <el-option label="表单模板" value="FORM_TEMPLATE" />
                <el-option label="DHR模板" value="DHR_TEMPLATE" />
                <el-option label="记录本模板" value="RECORDBOOK_TEMPLATE" />
              </el-select>
            </el-form-item>
            <el-form-item label="对象ID">
              <el-input v-model="changeForm.controlledObjectId" />
            </el-form-item>
            <el-form-item label="对象编号">
              <el-input v-model="changeForm.controlledObjectCode" />
            </el-form-item>
            <el-form-item label="原版本">
              <el-input v-model="changeForm.currentVersion" />
            </el-form-item>
            <el-form-item label="目标版本">
              <el-input v-model="changeForm.targetVersion" />
            </el-form-item>
            <el-form-item label="变更类型">
              <el-input v-model="changeForm.changeType" />
            </el-form-item>
            <el-form-item label="风险等级">
              <el-select v-model="changeForm.riskLevel" class="!w-100%">
                <el-option label="低" value="LOW" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="高" value="HIGH" />
                <el-option label="关键" value="CRITICAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="原因分类">
              <el-input v-model="changeForm.reasonCategory" />
            </el-form-item>
            <el-form-item label="原因">
              <el-input v-model="changeForm.reason" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="差异快照">
              <el-input v-model="changeForm.diffSnapshotJson" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item label="影响范围">
              <el-input v-model="changeForm.impactSummaryJson" type="textarea" :rows="4" />
            </el-form-item>
          </template>
          <template v-else>
            <el-form-item label="申请ID">
              <el-input-number v-model="changeForm.changeRequestId" :min="1" :controls="false" class="!w-100%" />
            </el-form-item>
            <el-form-item v-if="changeMode === 'SUBMIT' || changeMode === 'EFFECT'" label="原因">
              <el-input v-model="changeForm.reason" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item v-if="changeMode === 'APPROVE'" label="审批">
              <el-input v-model="changeForm.approvalOpinion" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="签核证据">
              <el-input v-model="changeForm.signoffEvidenceHash" />
            </el-form-item>
          </template>
          <el-form-item label="幂等键">
            <el-input v-model="changeForm.idempotencyKey" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="changeDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="changeSubmitting" @click="handleChangeConfirm">
            确认
          </el-button>
        </template>
      </el-dialog>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  approveEdhrUnifiedChange,
  createEdhrUnifiedChange,
  getEdhrUnifiedChangeEventPage,
  getEdhrUnifiedChangeImpactPage,
  getEdhrUnifiedChangePage,
  recalculateEdhrUnifiedChangeImpact,
  requestEdhrUnifiedChangeEffect,
  submitEdhrUnifiedChange,
  type EdhrUnifiedChangeEventPageReqVO,
  type EdhrUnifiedChangeEventRespVO,
  type EdhrUnifiedChangeImpactPageReqVO,
  type EdhrUnifiedChangeImpactRespVO,
  type EdhrUnifiedChangeObjectType,
  type EdhrUnifiedChangePageReqVO,
  type EdhrUnifiedChangeRespVO,
  type EdhrUnifiedChangeRiskLevel,
  type EdhrUnifiedChangeStatus
} from '@/api/mes/pro/edhr/unifiedChange'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrUnifiedChange' })

type ChangeMode = 'CREATE' | 'SUBMIT' | 'APPROVE' | 'EFFECT'

interface ChangeForm {
  controlledObjectType: EdhrUnifiedChangeObjectType
  controlledObjectId: string
  controlledObjectCode: string
  currentVersion: string
  targetVersion: string
  changeType: string
  riskLevel: EdhrUnifiedChangeRiskLevel
  reasonCategory: string
  reason: string
  diffSnapshotJson: string
  impactSummaryJson: string
  changeRequestId?: number
  approvalOpinion: string
  signoffEvidenceHash: string
  idempotencyKey: string
}

const message = useMessage()

const loading = ref(false)
const impactLoading = ref(false)
const eventLoading = ref(false)
const changeSubmitting = ref(false)
const loadError = ref('')
const actionError = ref('')
const changeError = ref('')
const list = ref<EdhrUnifiedChangeRespVO[]>([])
const impactList = ref<EdhrUnifiedChangeImpactRespVO[]>([])
const eventList = ref<EdhrUnifiedChangeEventRespVO[]>([])
const total = ref(0)
const impactTotal = ref(0)
const eventTotal = ref(0)
const impactDrawerVisible = ref(false)
const eventDrawerVisible = ref(false)
const changeDialogVisible = ref(false)
const changeMode = ref<ChangeMode>('CREATE')
const selectedRow = ref<EdhrUnifiedChangeRespVO>()

const queryParams = reactive<EdhrUnifiedChangePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  controlledObjectType: '',
  controlledObjectId: '',
  controlledObjectCode: '',
  changeType: '',
  changeStatus: '',
  riskLevel: ''
})

const impactQuery = reactive<EdhrUnifiedChangeImpactPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  changeRequestId: 0,
  impactType: '',
  impactObjectType: '',
  riskLevel: ''
})

const eventQuery = reactive<EdhrUnifiedChangeEventPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  changeRequestId: 0,
  eventType: ''
})

const changeForm = reactive<ChangeForm>({
  controlledObjectType: 'FORM_TEMPLATE',
  controlledObjectId: '',
  controlledObjectCode: '',
  currentVersion: '',
  targetVersion: '',
  changeType: '',
  riskLevel: 'MEDIUM',
  reasonCategory: '',
  reason: '',
  diffSnapshotJson: '',
  impactSummaryJson: '',
  changeRequestId: undefined,
  approvalOpinion: '',
  signoffEvidenceHash: '',
  idempotencyKey: ''
})

const changeDialogTitle = computed(() => {
  if (changeMode.value === 'SUBMIT') return '提交统一变更'
  if (changeMode.value === 'APPROVE') return '审批统一变更'
  if (changeMode.value === 'EFFECT') return '生效申请'
  return '新建统一变更'
})

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
  controlledObjectType: queryParams.controlledObjectType || undefined,
  controlledObjectId: queryParams.controlledObjectId?.trim() || undefined,
  controlledObjectCode: queryParams.controlledObjectCode?.trim() || undefined,
  changeType: queryParams.changeType?.trim() || undefined,
  changeStatus: queryParams.changeStatus || undefined,
  riskLevel: queryParams.riskLevel || undefined
})

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrUnifiedChangePage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR统一变更列表加载失败，请查看后端错误信息。')
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
  queryParams.controlledObjectType = ''
  queryParams.controlledObjectId = ''
  queryParams.controlledObjectCode = ''
  queryParams.changeType = ''
  queryParams.changeStatus = ''
  queryParams.riskLevel = ''
  getList()
}

const resetChangeForm = (mode: ChangeMode, row?: EdhrUnifiedChangeRespVO) => {
  changeMode.value = mode
  selectedRow.value = row
  changeForm.controlledObjectType = row?.controlledObjectType || queryParams.controlledObjectType || 'FORM_TEMPLATE'
  changeForm.controlledObjectId = row?.controlledObjectId || queryParams.controlledObjectId || ''
  changeForm.controlledObjectCode = row?.controlledObjectCode || queryParams.controlledObjectCode || ''
  changeForm.currentVersion = row?.currentVersion || ''
  changeForm.targetVersion = row?.targetVersion || ''
  changeForm.changeType = row?.changeType || ''
  changeForm.riskLevel = row?.riskLevel || 'MEDIUM'
  changeForm.reasonCategory = row?.reasonCategory || ''
  changeForm.reason = ''
  changeForm.diffSnapshotJson = row?.diffSnapshotJson || ''
  changeForm.impactSummaryJson = row?.impactSummaryJson || ''
  changeForm.changeRequestId = row?.id
  changeForm.approvalOpinion = ''
  changeForm.signoffEvidenceHash = ''
  changeForm.idempotencyKey = buildIdempotencyKey(mode, row)
  changeError.value = ''
  actionError.value = ''
}

const openCreateDialog = () => {
  resetChangeForm('CREATE')
  changeDialogVisible.value = true
}

const openSubmitDialog = (row: EdhrUnifiedChangeRespVO) => {
  resetChangeForm('SUBMIT', row)
  changeDialogVisible.value = true
}

const openApproveDialog = (row: EdhrUnifiedChangeRespVO) => {
  resetChangeForm('APPROVE', row)
  changeDialogVisible.value = true
}

const openEffectDialog = (row: EdhrUnifiedChangeRespVO) => {
  resetChangeForm('EFFECT', row)
  changeDialogVisible.value = true
}

const buildIdempotencyKey = (mode: ChangeMode, row?: EdhrUnifiedChangeRespVO) => {
  const objectId = row?.controlledObjectId || queryParams.controlledObjectId || 'object'
  return `EDHR-CHANGE-${mode}-${objectId}-${Date.now()}`
}

const validateCreateForm = () => {
  if (
    !changeForm.controlledObjectType ||
    !changeForm.controlledObjectId.trim() ||
    !changeForm.controlledObjectCode.trim() ||
    !changeForm.currentVersion.trim() ||
    !changeForm.targetVersion.trim()
  ) {
    changeError.value = '对象、对象编号、原版本和目标版本不能为空。'
    return undefined
  }
  if (changeForm.currentVersion.trim() === changeForm.targetVersion.trim()) {
    changeError.value = '历史版本不可覆盖，目标版本必须区别于原版本。'
    return undefined
  }
  if (!changeForm.changeType.trim() || !changeForm.reason.trim()) {
    changeError.value = '变更类型和原因不能为空。'
    return undefined
  }
  if (!changeForm.diffSnapshotJson.trim() || !changeForm.impactSummaryJson.trim()) {
    changeError.value = '差异快照和影响范围不能为空。'
    return undefined
  }
  if (!changeForm.idempotencyKey.trim()) {
    changeError.value = '幂等键不能为空。'
    return undefined
  }
  return {
    controlledObjectType: changeForm.controlledObjectType,
    controlledObjectId: changeForm.controlledObjectId.trim(),
    controlledObjectCode: changeForm.controlledObjectCode.trim(),
    currentVersion: changeForm.currentVersion.trim(),
    targetVersion: changeForm.targetVersion.trim(),
    changeType: changeForm.changeType.trim(),
    riskLevel: changeForm.riskLevel,
    reasonCategory: changeForm.reasonCategory.trim() || undefined,
    reason: changeForm.reason.trim(),
    diffSnapshotJson: changeForm.diffSnapshotJson.trim(),
    impactSummaryJson: changeForm.impactSummaryJson.trim(),
    idempotencyKey: changeForm.idempotencyKey.trim()
  }
}

const validateActionForm = () => {
  if (!changeForm.changeRequestId) {
    changeError.value = '变更申请ID不能为空。'
    return undefined
  }
  if (changeMode.value === 'SUBMIT' && !changeForm.reason.trim()) {
    changeError.value = '提交原因不能为空。'
    return undefined
  }
  if (changeMode.value === 'APPROVE' && !changeForm.approvalOpinion.trim()) {
    changeError.value = '审批意见不能为空。'
    return undefined
  }
  if (!changeForm.signoffEvidenceHash.trim()) {
    changeError.value = '签核证据不能为空。'
    return undefined
  }
  if (!changeForm.idempotencyKey.trim()) {
    changeError.value = '幂等键不能为空。'
    return undefined
  }
  return {
    changeRequestId: changeForm.changeRequestId,
    reason: changeForm.reason.trim(),
    approvalOpinion: changeForm.approvalOpinion.trim(),
    signoffEvidenceHash: changeForm.signoffEvidenceHash.trim(),
    idempotencyKey: changeForm.idempotencyKey.trim()
  }
}

const runChangeAction = async (action: () => Promise<void>) => {
  changeSubmitting.value = true
  changeError.value = ''
  try {
    await action()
    changeDialogVisible.value = false
    await getList()
    if (impactDrawerVisible.value) {
      await loadImpactList()
    }
    if (eventDrawerVisible.value) {
      await loadEventList()
    }
  } catch (error) {
    changeError.value = resolveErrorMessage(error, 'eDHR统一变更提交失败，请查看后端错误信息。')
    message.error(resolveErrorMessage(error, changeError.value))
  } finally {
    changeSubmitting.value = false
  }
}

const handleChangeConfirm = async () => {
  if (changeMode.value === 'CREATE') {
    const payload = validateCreateForm()
    if (!payload) return
    await runChangeAction(async () => {
      await createEdhrUnifiedChange(payload)
      message.success('统一变更已创建')
    })
    return
  }
  const payload = validateActionForm()
  if (!payload) return
  if (changeMode.value === 'APPROVE') {
    await runChangeAction(async () => {
      await approveEdhrUnifiedChange({
        changeRequestId: payload.changeRequestId,
        approvalOpinion: payload.approvalOpinion,
        signoffEvidenceHash: payload.signoffEvidenceHash,
        idempotencyKey: payload.idempotencyKey
      })
      message.success('统一变更已审批')
    })
    return
  }
  if (changeMode.value === 'EFFECT') {
    await runChangeAction(async () => {
      await requestEdhrUnifiedChangeEffect({
        changeRequestId: payload.changeRequestId,
        reason: payload.reason || undefined,
        signoffEvidenceHash: payload.signoffEvidenceHash,
        idempotencyKey: payload.idempotencyKey
      })
      message.success('生效申请已记录为阻断状态')
    })
    return
  }
  await runChangeAction(async () => {
    await submitEdhrUnifiedChange({
      changeRequestId: payload.changeRequestId,
      reason: payload.reason,
      signoffEvidenceHash: payload.signoffEvidenceHash,
      idempotencyKey: payload.idempotencyKey
    })
    message.success('统一变更已提交')
  })
}

const handleRecalculateImpact = async (row: EdhrUnifiedChangeRespVO) => {
  actionError.value = ''
  if (!row.id || !row.impactSummaryJson) {
    actionError.value = '变更申请或影响范围缺失，无法复算。'
    return
  }
  try {
    await recalculateEdhrUnifiedChangeImpact({
      changeRequestId: row.id,
      impactSummaryJson: row.impactSummaryJson,
      idempotencyKey: buildIdempotencyKey('SUBMIT', row)
    })
    message.success('影响范围已复算')
    await getList()
  } catch (error) {
    actionError.value = resolveErrorMessage(error, 'eDHR统一变更影响范围复算失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  }
}

const openImpactDrawer = async (row: EdhrUnifiedChangeRespVO) => {
  if (!row.id) {
    actionError.value = '变更申请缺失，无法查询影响范围。'
    return
  }
  impactQuery.pageNo = 1
  impactQuery.changeRequestId = row.id
  impactDrawerVisible.value = true
  await loadImpactList()
}

const loadImpactList = async () => {
  if (!impactQuery.changeRequestId) {
    impactList.value = []
    impactTotal.value = 0
    return
  }
  impactLoading.value = true
  actionError.value = ''
  try {
    const data = await getEdhrUnifiedChangeImpactPage(impactQuery)
    impactList.value = data.list || []
    impactTotal.value = data.total || 0
  } catch (error) {
    impactList.value = []
    impactTotal.value = 0
    actionError.value = resolveErrorMessage(error, 'eDHR统一变更影响范围加载失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  } finally {
    impactLoading.value = false
  }
}

const openEventDrawer = async (row: EdhrUnifiedChangeRespVO) => {
  if (!row.id) {
    actionError.value = '变更申请缺失，无法查询事件。'
    return
  }
  eventQuery.pageNo = 1
  eventQuery.changeRequestId = row.id
  eventDrawerVisible.value = true
  await loadEventList()
}

const loadEventList = async () => {
  if (!eventQuery.changeRequestId) {
    eventList.value = []
    eventTotal.value = 0
    return
  }
  eventLoading.value = true
  actionError.value = ''
  try {
    const data = await getEdhrUnifiedChangeEventPage(eventQuery)
    eventList.value = data.list || []
    eventTotal.value = data.total || 0
  } catch (error) {
    eventList.value = []
    eventTotal.value = 0
    actionError.value = resolveErrorMessage(error, 'eDHR统一变更事件加载失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  } finally {
    eventLoading.value = false
  }
}

const resolveObjectTypeLabel = (type?: string) => {
  if (type === 'DHR_TEMPLATE') return 'DHR模板'
  if (type === 'RECORDBOOK_TEMPLATE') return '记录本模板'
  if (type === 'FORM_TEMPLATE') return '表单模板'
  return type || '--'
}

const resolveStatusLabel = (status?: EdhrUnifiedChangeStatus) => {
  if (status === 'SUBMITTED') return '已提交'
  if (status === 'APPROVED') return '已审批'
  if (status === 'EFFECT_BLOCKED') return '生效阻断'
  if (status === 'DRAFT') return '草稿'
  return status || '--'
}

const resolveStatusTag = (status?: EdhrUnifiedChangeStatus) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'SUBMITTED') return 'warning'
  if (status === 'EFFECT_BLOCKED') return 'danger'
  return 'info'
}

const resolveRiskLabel = (risk?: string) => {
  if (risk === 'LOW') return '低'
  if (risk === 'MEDIUM') return '中'
  if (risk === 'HIGH') return '高'
  if (risk === 'CRITICAL') return '关键'
  return risk || '--'
}

const resolveRiskTag = (risk?: string) => {
  if (risk === 'CRITICAL') return 'danger'
  if (risk === 'HIGH') return 'warning'
  if (risk === 'MEDIUM') return 'primary'
  return 'info'
}

const formatDateTime = (value?: string | number) => {
  return formatEdhrDateTime(value)
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.edhr-unified-change {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.edhr-unified-change__toolbar,
.edhr-unified-change__table {
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-unified-change__toolbar {
  padding: 16px 16px 0;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.edhr-unified-change__title {
  width: 100%;
  margin-bottom: 12px;
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.edhr-unified-change__table {
  padding: 16px;
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.edhr-unified-change__warning {
  margin-bottom: 12px;
}

.edhr-unified-change__strong {
  color: #172033;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-unified-change__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-unified-change__json {
  max-height: 48px;
  overflow: hidden;
  color: #172033;
  line-height: 1.5;
  word-break: break-all;
}

.edhr-unified-change__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.edhr-unified-change__drawer :deep(.el-drawer__body) {
  padding-top: 8px;
}

.edhr-unified-change__dialog-alert {
  margin-bottom: 12px;
}

.edhr-unified-change__dialog-form {
  padding-top: 4px;
}
</style>
