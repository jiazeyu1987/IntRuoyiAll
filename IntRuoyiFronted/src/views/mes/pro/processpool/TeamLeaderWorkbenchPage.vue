<template>
  <ContentWrap>
    <div class="team-leader-workbench__header">
      <div>
        <div class="team-leader-workbench__title">工序池班组长工作台</div>
        <div class="team-leader-workbench__subtitle">
          负责员工提交复核、生产工单异常上报、班组基础维护
        </div>
      </div>
    </div>

    <el-tabs
      v-model="activeLeaderTab"
      data-team-leader-type-tabs
      @tab-change="handleLeaderTypeChange"
    >
      <el-tab-pane label="生产组长" name="PRODUCTION" />
      <el-tab-pane label="PQC 组长" name="PQC" />
    </el-tabs>
  </ContentWrap>

  <template>
    <ContentWrap v-if="loadError">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
    </ContentWrap>

    <ContentWrap>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="提交看板" name="submission" />
        <el-tab-pane v-if="isProductionLeader" label="异常上报" name="abnormal" />
        <el-tab-pane v-if="isProductionLeader" label="班组维护" name="maintenance" />
      </el-tabs>
    </ContentWrap>

    <ContentWrap v-if="activeTab === 'submission'">
      <el-form
        ref="queryFormRef"
        class="team-leader-workbench__query"
        :model="queryParams"
        :inline="true"
        label-width="88px"
      >
        <el-form-item label="提交日期" prop="submitDate">
          <el-date-picker
            v-model="queryParams.submitDate"
            value-format="YYYY-MM-DD"
            type="date"
            placeholder="请选择提交日期"
            class="!w-180px"
          />
        </el-form-item>
        <el-form-item :label="employeeFilterLabel" prop="employeeUserId">
          <el-input-number
            v-model="queryParams.employeeUserId"
            :min="1"
            :controls="false"
            placeholder="员工编号"
            class="!w-180px"
          />
        </el-form-item>
        <el-form-item label="工序" prop="processId">
          <el-input-number
            v-model="queryParams.processId"
            :min="1"
            :controls="false"
            placeholder="工序编号"
            class="!w-180px"
          />
        </el-form-item>
        <el-form-item label="模板类型" prop="templateType">
          <el-select
            v-model="queryParams.templateType"
            clearable
            filterable
            placeholder="请选择模板"
            class="!w-190px"
          >
            <el-option label="生产简化模板" value="PRODUCTION_SIMPLIFIED" />
            <el-option label="PQC 简化模板" value="PQC_SIMPLIFIED" />
          </el-select>
        </el-form-item>
        <el-form-item label="生产工单" prop="workOrderCode">
          <el-input
            v-model="queryParams.workOrderCode"
            clearable
            placeholder="工单编码"
            class="!w-220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" />
            搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" />
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="submissionList" border stripe>
        <el-table-column label="提交时间" prop="submittedAt" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column :label="employeeColumnLabel" min-width="140">
          <template #default="{ row }">
            {{ row.actualEmployeeUserName || row.actualEmployeeUserId || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="工序" min-width="150">
          <template #default="{ row }">{{ row.processName || row.processCode || '--' }}</template>
        </el-table-column>
        <el-table-column label="生产工单" min-width="160">
          <template #default="{ row }">{{ row.workOrderCode || '--' }}</template>
        </el-table-column>
        <el-table-column label="PQC" min-width="130">
          <template #default="{ row }">
            <el-tag :type="resolvePqcTagType(row.pqcResult)" effect="plain">
              {{ row.pqcSummary || row.pqcResult || '--' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交内容" min-width="220">
          <template #default="{ row }">
            {{ row.submittedSummary || row.pqcSummary || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="审核副本" min-width="130">
          <template #default="{ row }">{{ row.auditCopyStatus || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="success" @click="openReview(row)">复核</el-button>
            <el-button v-if="isProductionLeader" link type="warning" @click="prefillAbnormal(row)">
              标记异常
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        :total="submissionTotal"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getSubmissionList"
      />
    </ContentWrap>

    <ContentWrap v-if="activeTab === 'abnormal'">
      <el-form
        ref="abnormalFormRef"
        :model="abnormalForm"
        :rules="abnormalRules"
        label-width="120px"
        class="team-leader-workbench__form"
      >
        <el-form-item label="生产工单ID" prop="workOrderId">
          <el-input-number v-model="abnormalForm.workOrderId" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="工序ID" prop="processId">
          <el-input-number v-model="abnormalForm.processId" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="来源提交ID" prop="sourceEventId">
          <el-input-number v-model="abnormalForm.sourceEventId" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="异常原因" prop="abnormalReasonCode">
          <el-input v-model="abnormalForm.abnormalReasonCode" placeholder="请输入异常原因编码" />
        </el-form-item>
        <el-form-item label="异常说明" prop="abnormalDescription">
          <el-input
            v-model="abnormalForm.abnormalDescription"
            type="textarea"
            :rows="4"
            placeholder="请输入异常说明"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="abnormalSubmitting" @click="submitAbnormal">
            <Icon icon="ep:warning-filled" class="mr-5px" />
            标记并上报
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap v-if="activeTab === 'maintenance'">
      <div class="team-leader-workbench__maintenance-grid">
        <el-card shadow="never">
          <template #header>员工工序绑定</template>
          <el-form :model="employeeBindingForm" label-width="98px">
            <el-form-item label="工序ID">
              <el-input-number v-model="employeeBindingForm.processId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="员工ID">
              <el-input-number
                v-model="employeeBindingForm.employeeUserId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="maintenanceSubmitting"
                @click="submitEmployeeBinding"
              >
                添加员工
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="employeeDisableForm" label-width="98px">
            <el-form-item label="绑定ID">
              <el-input-number v-model="employeeDisableForm.bindingId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="danger"
                plain
                :loading="maintenanceSubmitting"
                @click="submitDisableBinding"
              >
                禁用员工
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>不良原因列表</template>
          <el-form :model="defectReasonForm" label-width="98px">
            <el-form-item label="工序ID">
              <el-input-number v-model="defectReasonForm.processId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="原因类型">
              <el-select v-model="defectReasonForm.reasonType">
                <el-option label="损耗" value="LOSS" />
                <el-option label="不合格" value="UNQUALIFIED" />
                <el-option label="PQC 失败" value="PQC_FAILURE" />
              </el-select>
            </el-form-item>
            <el-form-item label="原因编码">
              <el-input v-model="defectReasonForm.reasonCode" />
            </el-form-item>
            <el-form-item label="原因名称">
              <el-input v-model="defectReasonForm.reasonName" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="maintenanceSubmitting"
                @click="submitDefectReason"
              >
                新增原因
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>设备参数上下限</template>
          <el-form :model="deviceRuleForm" label-width="98px">
            <el-form-item label="工序ID">
              <el-input-number v-model="deviceRuleForm.processId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="设备ID">
              <el-input-number v-model="deviceRuleForm.deviceId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="参数编码">
              <el-input v-model="deviceRuleForm.parameterCode" />
            </el-form-item>
            <el-form-item label="参数名称">
              <el-input v-model="deviceRuleForm.parameterName" />
            </el-form-item>
            <el-form-item label="下限">
              <el-input-number v-model="deviceRuleForm.lowerLimit" :controls="false" />
            </el-form-item>
            <el-form-item label="上限">
              <el-input-number v-model="deviceRuleForm.upperLimit" :controls="false" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitDeviceRule">
                保存参数
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </ContentWrap>

    <el-drawer v-model="detailVisible" :title="detailDrawerTitle" size="620px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border>
          <el-descriptions-item label="服务端提交时间">
            {{ formatDateTime(detail.submittedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="employeeDetailLabel">
            {{ detail.actualEmployeeUserName || detail.actualEmployeeUserId || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="工序">
            {{ detail.processName || detail.processCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="生产工单">
            {{ detail.workOrderCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交摘要">
            {{ detail.submittedSummary || '--' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.pqcResult || detail.pqcSummary" label="PQC检验内容">
            <el-tag :type="resolvePqcTagType(detail.pqcResult)" effect="plain">
              {{ detail.pqcSummary || detail.pqcResult }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="原始 payload">
            <pre class="team-leader-workbench__payload">{{
              detail.originalPayloadJson || '--'
            }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewVisible" title="复核员工提交" width="520px">
      <el-form :model="reviewForm" label-width="92px">
        <el-form-item label="复核结果">
          <el-select v-model="reviewForm.reviewStatus">
            <el-option label="通过" value="APPROVED" />
            <el-option label="退回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="复核说明">
          <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview"
          >提交复核</el-button
        >
      </template>
    </el-dialog>
  </template>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  addTeamEmployeeBinding,
  createTeamDefectReason,
  disableTeamEmployeeBinding,
  getTeamLeaderSubmissionDetail,
  getTeamLeaderSubmissionPage,
  markAndReportWorkOrderAbnormal,
  reviewTeamLeaderSubmission,
  saveTeamDeviceParameterRule,
  type TeamLeaderSubmissionPageReqVO,
  type TeamLeaderType
} from '@/api/mes/pro/processpool/teamLeader'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO
} from '@/api/mes/pro/processpool'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'MesProProcessPoolTeamLeaderWorkbench' })

const queryFormRef = ref()
const abnormalFormRef = ref()
const activeLeaderTab = ref<TeamLeaderType>('PRODUCTION')
const activeTab = ref<'submission' | 'abnormal' | 'maintenance'>('submission')
const loading = ref(false)
const detailLoading = ref(false)
const reviewSubmitting = ref(false)
const abnormalSubmitting = ref(false)
const maintenanceSubmitting = ref(false)
const detailVisible = ref(false)
const reviewVisible = ref(false)
const loadError = ref('')
const submissionTotal = ref(0)
const submissionList = ref<ProcessPoolTimelineEventVO[]>([])
const detail = ref<ProcessPoolTimelineDetailVO>()
const reviewEvent = ref<ProcessPoolTimelineEventVO>()

const isProductionLeader = computed(() => activeLeaderTab.value === 'PRODUCTION')
const employeeFilterLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '员工'
)
const employeeColumnLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '员工'
)
const employeeDetailLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '实际员工'
)
const detailDrawerTitle = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员提交详情' : '员工提交详情'
)

const queryParams = reactive<TeamLeaderSubmissionPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  leaderType: 'PRODUCTION',
  submitDate: new Date().toISOString().slice(0, 10),
  employeeUserId: undefined,
  processId: undefined,
  deviceId: undefined,
  templateType: undefined,
  workOrderId: undefined,
  workOrderCode: undefined
})

const reviewForm = reactive({
  reviewStatus: 'APPROVED' as 'APPROVED' | 'REJECTED',
  reviewRemark: ''
})

const abnormalForm = reactive({
  workOrderId: undefined as number | undefined,
  routeProcessId: undefined as number | undefined,
  processId: undefined as number | undefined,
  sourceEventId: undefined as number | undefined,
  abnormalReasonCode: '',
  abnormalDescription: ''
})

const employeeBindingForm = reactive({
  processId: undefined as number | undefined,
  employeeUserId: undefined as number | undefined
})

const employeeDisableForm = reactive({
  bindingId: undefined as number | undefined
})

const defectReasonForm = reactive({
  processId: undefined as number | undefined,
  reasonType: 'LOSS',
  reasonCode: '',
  reasonName: ''
})

const deviceRuleForm = reactive({
  processId: undefined as number | undefined,
  deviceId: undefined as number | undefined,
  parameterCode: '',
  parameterName: '',
  lowerLimit: undefined as number | undefined,
  upperLimit: undefined as number | undefined,
  valueType: 'DECIMAL'
})

const abnormalRules = {
  workOrderId: [{ required: true, message: '生产工单ID不能为空', trigger: 'blur' }],
  abnormalReasonCode: [{ required: true, message: '异常原因不能为空', trigger: 'blur' }],
  abnormalDescription: [{ required: true, message: '异常说明不能为空', trigger: 'blur' }]
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const normalizePositiveNumber = (value?: number) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const requirePositiveNumber = (value: unknown, message: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(message)
  }
  return parsed
}

const buildSubmissionParams = (): TeamLeaderSubmissionPageReqVO => {
  if (!queryParams.submitDate) {
    throw new Error('提交日期不能为空')
  }
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    leaderType: queryParams.leaderType,
    submitDate: queryParams.submitDate,
    employeeUserId: normalizePositiveNumber(queryParams.employeeUserId),
    processId: normalizePositiveNumber(queryParams.processId),
    deviceId: normalizePositiveNumber(queryParams.deviceId),
    templateType: queryParams.templateType || undefined,
    workOrderId: normalizePositiveNumber(queryParams.workOrderId),
    workOrderCode: queryParams.workOrderCode?.trim() || undefined
  }
}

const getSubmissionList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getTeamLeaderSubmissionPage(buildSubmissionParams())
    submissionList.value = data.list || []
    submissionTotal.value = data.total || 0
  } catch (error) {
    submissionList.value = []
    submissionTotal.value = 0
    loadError.value = resolveErrorMessage(error, '班组长提交看板加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getSubmissionList()
}

const handleLeaderTypeChange = (value: string | number) => {
  const leaderType = String(value) as TeamLeaderType
  queryParams.leaderType = leaderType
  if (leaderType === 'PQC') {
    activeTab.value = 'submission'
    queryParams.templateType = 'PQC_SIMPLIFIED'
  } else if (queryParams.templateType === 'PQC_SIMPLIFIED') {
    queryParams.templateType = undefined
  }
  handleQuery()
}

const resetQuery = () => {
  const leaderType = activeLeaderTab.value
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.leaderType = leaderType
  queryParams.submitDate = new Date().toISOString().slice(0, 10)
  queryParams.templateType = leaderType === 'PQC' ? 'PQC_SIMPLIFIED' : undefined
  getSubmissionList()
}

const openDetail = async (event: ProcessPoolTimelineEventVO) => {
  const eventId = requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  detailVisible.value = true
  detailLoading.value = true
  detail.value = undefined
  try {
    detail.value = await getTeamLeaderSubmissionDetail(
      eventId,
      queryParams.leaderType as TeamLeaderType
    )
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '员工提交详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

const openReview = (event: ProcessPoolTimelineEventVO) => {
  requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  reviewEvent.value = event
  reviewForm.reviewStatus = 'APPROVED'
  reviewForm.reviewRemark = ''
  reviewVisible.value = true
}

const submitReview = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  reviewSubmitting.value = true
  try {
    await reviewTeamLeaderSubmission({
      eventId,
      leaderType: queryParams.leaderType as TeamLeaderType,
      reviewStatus: reviewForm.reviewStatus,
      reviewRemark: reviewForm.reviewRemark.trim() || undefined
    })
    ElMessage.success('复核已提交')
    reviewVisible.value = false
    await getSubmissionList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '复核提交失败'))
  } finally {
    reviewSubmitting.value = false
  }
}

const prefillAbnormal = (event: ProcessPoolTimelineEventVO) => {
  activeTab.value = 'abnormal'
  abnormalForm.workOrderId = normalizePositiveNumber(event.workOrderId)
  abnormalForm.routeProcessId = normalizePositiveNumber(event.routeProcessId)
  abnormalForm.processId = normalizePositiveNumber(event.processId)
  abnormalForm.sourceEventId = normalizePositiveNumber(event.id)
}

const submitAbnormal = async () => {
  const valid = await abnormalFormRef.value?.validate?.()
  if (valid === false) return
  abnormalSubmitting.value = true
  try {
    await markAndReportWorkOrderAbnormal({
      workOrderId: requirePositiveNumber(abnormalForm.workOrderId, '生产工单ID不能为空'),
      routeProcessId: normalizePositiveNumber(abnormalForm.routeProcessId),
      processId: normalizePositiveNumber(abnormalForm.processId),
      sourceEventId: normalizePositiveNumber(abnormalForm.sourceEventId),
      abnormalReasonCode: abnormalForm.abnormalReasonCode.trim(),
      abnormalDescription: abnormalForm.abnormalDescription.trim()
    })
    ElMessage.success('异常已上报')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '异常上报失败'))
  } finally {
    abnormalSubmitting.value = false
  }
}

const submitEmployeeBinding = async () => {
  maintenanceSubmitting.value = true
  try {
    await addTeamEmployeeBinding({
      processId: requirePositiveNumber(employeeBindingForm.processId, '工序ID不能为空'),
      employeeUserId: requirePositiveNumber(employeeBindingForm.employeeUserId, '员工ID不能为空')
    })
    ElMessage.success('员工已添加')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '员工添加失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitDisableBinding = async () => {
  maintenanceSubmitting.value = true
  try {
    await disableTeamEmployeeBinding({
      bindingId: requirePositiveNumber(employeeDisableForm.bindingId, '绑定ID不能为空')
    })
    ElMessage.success('员工已禁用')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '员工禁用失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitDefectReason = async () => {
  maintenanceSubmitting.value = true
  try {
    await createTeamDefectReason({
      processId: normalizePositiveNumber(defectReasonForm.processId),
      reasonType: defectReasonForm.reasonType,
      reasonCode: defectReasonForm.reasonCode.trim(),
      reasonName: defectReasonForm.reasonName.trim()
    })
    ElMessage.success('不良原因已新增')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '不良原因新增失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitDeviceRule = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamDeviceParameterRule({
      processId: requirePositiveNumber(deviceRuleForm.processId, '工序ID不能为空'),
      deviceId: requirePositiveNumber(deviceRuleForm.deviceId, '设备ID不能为空'),
      parameterCode: deviceRuleForm.parameterCode.trim(),
      parameterName: deviceRuleForm.parameterName.trim() || undefined,
      lowerLimit: requirePositiveNumber(deviceRuleForm.lowerLimit, '参数下限不能为空'),
      upperLimit: requirePositiveNumber(deviceRuleForm.upperLimit, '参数上限不能为空'),
      valueType: deviceRuleForm.valueType
    })
    ElMessage.success('设备参数已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备参数保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const formatDateTime = (value?: string | number | Date) => formatDateTimeValue(value, '--')

const resolvePqcTagType = (pqcResult?: string) => {
  if (pqcResult === 'SUCCESS' || pqcResult === 'PASS') return 'success'
  if (pqcResult === 'FAILURE' || pqcResult === 'FAIL') return 'danger'
  return 'info'
}

onMounted(() => getSubmissionList())
</script>

<style scoped>
.team-leader-workbench__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.team-leader-workbench__title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.team-leader-workbench__subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.team-leader-workbench__query {
  margin-bottom: -15px;
}

.team-leader-workbench__form {
  max-width: 760px;
}

.team-leader-workbench__maintenance-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.team-leader-workbench__payload {
  max-height: 260px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1180px) {
  .team-leader-workbench__maintenance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
