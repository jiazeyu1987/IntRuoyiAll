<template>
  <ContentWrap>
    <div class="team-leader-workbench__header">
      <div>
        <div class="team-leader-workbench__title">工序池班组长工作台</div>
        <div class="team-leader-workbench__subtitle">
          负责报工确认、活跃订单分配、异常上报和班组配置中心维护
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

  <ContentWrap v-if="loadError">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
    </ContentWrap>

    <ContentWrap data-team-leader-report-workbench>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">报工确认工作台</div>
          <div class="team-leader-workbench__hint">
            查看员工结构化报工，确认后按 FIFO 或手动分配到活跃订单。
          </div>
        </div>
      </div>
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

    <ContentWrap v-if="isProductionLeader" data-team-leader-abnormal-report>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">订单异常上报</div>
          <div class="team-leader-workbench__hint">
            异常订单来自活跃订单池，异常原因来自当前工序配置。
          </div>
        </div>
      </div>
      <el-form
        ref="abnormalFormRef"
        :model="abnormalForm"
        :rules="abnormalRules"
        label-width="120px"
        class="team-leader-workbench__form"
      >
        <el-form-item label="活跃订单" prop="activeOrderId" data-team-leader-active-order-select>
          <el-select
            v-model="abnormalForm.activeOrderId"
            filterable
            placeholder="请选择活跃订单"
            @change="handleAbnormalActiveOrderChange"
          >
            <el-option
              v-for="order in activeOrderOptions"
              :key="order.id"
              :label="formatActiveOrderOption(order)"
              :value="order.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工序ID" prop="processId">
          <el-input-number v-model="abnormalForm.processId" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item
          label="异常原因"
          prop="abnormalReasonCode"
          data-team-leader-defect-reason-select
        >
          <el-select
            v-model="abnormalForm.abnormalReasonCode"
            filterable
            allow-create
            placeholder="请选择当前工序允许的异常原因"
          >
            <el-option
              v-for="reason in configuredDefectReasonOptions"
              :key="reason.reasonCode"
              :label="reason.reasonName"
              :value="reason.reasonCode"
            />
          </el-select>
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

    <ContentWrap v-if="isProductionLeader" data-team-leader-config-center>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">班组配置中心</div>
          <div class="team-leader-workbench__hint">
            维护员工、设备、参数、活跃订单和工序关系，员工端填报从这里读取配置。
          </div>
        </div>
      </div>
      <div class="team-leader-workbench__maintenance-grid">
        <el-card shadow="never" data-team-leader-active-order-config>
          <template #header>活跃订单池</template>
          <el-form :model="activeOrderForm" label-width="98px">
            <el-form-item label="生产订单ID">
              <el-input-number v-model="activeOrderForm.workOrderId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitAddActiveOrder">
                加入活跃订单
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="activeOrderRemoveForm" label-width="98px">
            <el-form-item label="活跃记录ID">
              <el-input-number
                v-model="activeOrderRemoveForm.activeOrderId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="danger"
                plain
                :loading="maintenanceSubmitting"
                @click="submitRemoveActiveOrder"
              >
                移出活跃订单
              </el-button>
            </el-form-item>
          </el-form>
          <div class="team-leader-workbench__hint">
            当前活跃订单：{{ activeOrderOptions.length }} 个
          </div>
        </el-card>

        <el-card shadow="never" data-team-leader-employee-config>
          <template #header>员工档案与工序员工</template>
          <el-form :model="employeeProfileForm" label-width="108px">
            <el-form-item label="员工编号">
              <el-input v-model="employeeProfileForm.employeeCode" />
            </el-form-item>
            <el-form-item label="员工姓名">
              <el-input v-model="employeeProfileForm.employeeName" />
            </el-form-item>
            <el-form-item label="员工类型">
              <el-select v-model="employeeProfileForm.employeeType">
                <el-option label="正式员工" value="FORMAL" />
                <el-option label="临时工" value="TEMPORARY" />
              </el-select>
            </el-form-item>
            <el-form-item label="系统用户ID">
              <el-input-number v-model="employeeProfileForm.systemUserId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitEmployeeProfile">
                新增员工
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="processEmployeeBindingForm" label-width="108px">
            <el-form-item label="工序ID">
              <el-input-number
                v-model="processEmployeeBindingForm.processId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item label="员工档案ID">
              <el-input-number
                v-model="processEmployeeBindingForm.employeeProfileId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="maintenanceSubmitting"
                @click="submitProcessEmployeeBinding"
              >
                绑定工序员工
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" data-team-leader-device-config>
          <template #header>设备档案与状态</template>
          <el-form :model="teamDeviceForm" label-width="98px">
            <el-form-item label="设备编号">
              <el-input v-model="teamDeviceForm.deviceCode" />
            </el-form-item>
            <el-form-item label="设备名称">
              <el-input v-model="teamDeviceForm.deviceName" />
            </el-form-item>
            <el-form-item label="设备状态">
              <el-select v-model="teamDeviceForm.deviceStatus">
                <el-option label="启用" value="ENABLED" />
                <el-option label="报修" value="REPAIRING" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitTeamDevice">
                新增设备
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="teamDeviceStatusForm" label-width="98px">
            <el-form-item label="设备ID">
              <el-input-number v-model="teamDeviceStatusForm.deviceId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="teamDeviceStatusForm.deviceStatus">
                <el-option label="启用" value="ENABLED" />
                <el-option label="报修" value="REPAIRING" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="maintenanceSubmitting" @click="submitTeamDeviceStatus">
                更新状态
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" data-team-leader-process-relation-config>
          <template #header>工序设备与异常关系</template>
          <el-form :model="processDeviceBindingForm" label-width="108px">
            <el-form-item label="工序ID">
              <el-input-number
                v-model="processDeviceBindingForm.processId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item label="设备ID">
              <el-input-number
                v-model="processDeviceBindingForm.deviceId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitProcessDeviceBinding">
                绑定工序设备
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="defectReasonForm" label-width="108px">
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
                @click="submitProcessDefectReason"
              >
                保存工序异常原因
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" data-team-leader-parameter-config>
          <template #header>设备参数维护</template>
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
            <el-form-item label="单位">
              <el-input v-model="deviceRuleForm.unit" />
            </el-form-item>
            <el-form-item label="下限">
              <el-input-number v-model="deviceRuleForm.lowerLimit" :controls="false" />
            </el-form-item>
            <el-form-item label="上限">
              <el-input-number v-model="deviceRuleForm.upperLimit" :controls="false" />
            </el-form-item>
            <el-form-item label="默认值">
              <el-input-number v-model="deviceRuleForm.defaultValue" :controls="false" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitRuntimeDeviceRule">
                保存参数
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </ContentWrap>

    <el-drawer v-model="detailVisible" :title="detailDrawerTitle" size="620px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border data-team-leader-structured-detail>
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
          <el-descriptions-item label="结构化报工内容">
            <el-table
              :data="resolveStructuredPayloadItems(detail.originalPayloadJson)"
              border
              size="small"
              empty-text="暂无结构化字段"
            >
              <el-table-column label="字段" prop="field" min-width="160" />
              <el-table-column label="值" prop="value" min-width="220" />
            </el-table>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewVisible" title="复核员工提交" width="760px">
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
      <div
        v-if="isProductionLeader && reviewForm.reviewStatus === 'APPROVED'"
        class="team-leader-workbench__allocation"
      >
        <div class="team-leader-workbench__allocation-toolbar">
          <div>
            <div class="team-leader-workbench__section-title">活跃订单分配</div>
            <div class="team-leader-workbench__hint">
              可先按 FIFO 自动分配，再根据现场情况手动调整。
            </div>
          </div>
          <div>
            <el-button
              data-team-leader-fifo-allocation
              type="primary"
              plain
              :loading="allocationPreviewLoading"
              @click="previewFifoAllocation"
            >
              FIFO 自动分配
            </el-button>
            <el-button @click="addAllocationLine">新增分配行</el-button>
          </div>
        </div>
        <el-table
          data-team-leader-allocation-table
          :data="allocationRows"
          border
          size="small"
          empty-text="请点击 FIFO 自动分配或手动新增分配行"
        >
          <el-table-column label="活跃订单" min-width="220">
            <template #default="{ row }">
              <el-select
                v-model="row.activeOrderId"
                filterable
                placeholder="请选择活跃订单"
                @change="markManualAllocation"
              >
                <el-option
                  v-for="order in activeOrderOptions"
                  :key="order.id"
                  :label="formatActiveOrderOption(order)"
                  :value="order.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分配数量" width="180">
            <template #default="{ row }">
              <el-input-number
                v-model="row.allocatedQuantity"
                :min="0"
                :precision="3"
                :controls="false"
                class="!w-140px"
                @change="markManualAllocation"
              />
            </template>
          </el-table-column>
          <el-table-column label="FIFO 剩余" width="140">
            <template #default="{ row }">
              {{ row.remainingQuantityBeforeAllocation ?? '--' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeAllocationLine($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="team-leader-workbench__hint mt-8px">
          当前分配模式：{{ reviewForm.allocationMode }}
        </div>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview"
          >提交复核</el-button
        >
      </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  addTeamLeaderActiveOrder,
  confirmTeamLeaderReportAllocation,
  createTeamDevice,
  createTeamEmployeeProfile,
  getTeamLeaderActiveOrderList,
  getTeamLeaderSubmissionDetail,
  getTeamLeaderSubmissionPage,
  markAndReportWorkOrderAbnormal,
  previewTeamLeaderReportFifoAllocation,
  removeTeamLeaderActiveOrder,
  reviewTeamLeaderSubmission,
  saveTeamProcessDefectReason,
  saveTeamProcessDeviceBinding,
  saveTeamProcessEmployeeBinding,
  saveTeamRuntimeDeviceParameterRule,
  updateTeamDeviceStatus,
  type TeamLeaderActiveOrderRespVO,
  type TeamLeaderReportAllocationLine,
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
const loading = ref(false)
const detailLoading = ref(false)
const reviewSubmitting = ref(false)
const allocationPreviewLoading = ref(false)
const abnormalSubmitting = ref(false)
const maintenanceSubmitting = ref(false)
const detailVisible = ref(false)
const reviewVisible = ref(false)
const loadError = ref('')
const submissionTotal = ref(0)
const submissionList = ref<ProcessPoolTimelineEventVO[]>([])
const detail = ref<ProcessPoolTimelineDetailVO>()
const reviewEvent = ref<ProcessPoolTimelineEventVO>()
const activeOrderOptions = ref<TeamLeaderActiveOrderRespVO[]>([])
const allocationRows = ref<TeamLeaderReportAllocationLine[]>([])
const configuredDefectReasonOptions = ref<
  Array<{ reasonType: string; reasonCode: string; reasonName: string }>
>([])

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
  allocationMode: 'FIFO' as 'FIFO' | 'MANUAL',
  reviewRemark: ''
})

const abnormalForm = reactive({
  activeOrderId: undefined as number | undefined,
  workOrderId: undefined as number | undefined,
  routeProcessId: undefined as number | undefined,
  processId: undefined as number | undefined,
  sourceEventId: undefined as number | undefined,
  abnormalReasonCode: '',
  abnormalDescription: ''
})

const activeOrderForm = reactive({
  workOrderId: undefined as number | undefined
})

const activeOrderRemoveForm = reactive({
  activeOrderId: undefined as number | undefined
})

const employeeProfileForm = reactive({
  systemUserId: undefined as number | undefined,
  employeeCode: '',
  employeeName: '',
  employeeType: 'TEMPORARY'
})

const processEmployeeBindingForm = reactive({
  processId: undefined as number | undefined,
  employeeProfileId: undefined as number | undefined
})

const teamDeviceForm = reactive({
  deviceCode: '',
  deviceName: '',
  deviceStatus: 'ENABLED' as 'ENABLED' | 'REPAIRING' | 'DISABLED'
})

const teamDeviceStatusForm = reactive({
  deviceId: undefined as number | undefined,
  deviceStatus: 'REPAIRING' as 'ENABLED' | 'REPAIRING' | 'DISABLED'
})

const processDeviceBindingForm = reactive({
  processId: undefined as number | undefined,
  deviceId: undefined as number | undefined
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
  unit: '',
  lowerLimit: undefined as number | undefined,
  upperLimit: undefined as number | undefined,
  defaultValue: undefined as number | undefined,
  valueType: 'DECIMAL'
})

const abnormalRules = {
  activeOrderId: [{ required: true, message: '活跃订单不能为空', trigger: 'change' }],
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

const normalizeFiniteNumber = (value?: number) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const requireFiniteNumber = (value: unknown, message: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    throw new Error(message)
  }
  return parsed
}

const formatActiveOrderOption = (order: TeamLeaderActiveOrderRespVO) => {
  return `订单 ${order.workOrderId} / 活跃池 ${order.id}`
}

const resetReviewAllocation = () => {
  reviewForm.allocationMode = 'FIFO'
  allocationRows.value = []
}

const loadActiveOrders = async () => {
  activeOrderOptions.value = await getTeamLeaderActiveOrderList()
}

const markManualAllocation = () => {
  reviewForm.allocationMode = 'MANUAL'
}

const addAllocationLine = () => {
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value.push({
    activeOrderId: activeOrderOptions.value[0]?.id ?? 0,
    allocatedQuantity: 0
  })
}

const removeAllocationLine = (index: number) => {
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value.splice(index, 1)
}

const previewFifoAllocation = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  allocationPreviewLoading.value = true
  try {
    const preview = await previewTeamLeaderReportFifoAllocation({
      eventId,
      leaderType: queryParams.leaderType as TeamLeaderType
    })
    reviewForm.allocationMode = 'FIFO'
    allocationRows.value = (preview.lines || []).map((line) => ({
      activeOrderId: line.activeOrderId,
      workOrderId: line.workOrderId,
      workOrderCode: line.workOrderCode,
      allocatedQuantity: line.allocatedQuantity,
      remainingQuantityBeforeAllocation: line.remainingQuantityBeforeAllocation
    }))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'FIFO 自动分配失败'))
  } finally {
    allocationPreviewLoading.value = false
  }
}

const buildAllocationSubmitLines = (): TeamLeaderReportAllocationLine[] => {
  const lines = allocationRows.value.map((line) => ({
    activeOrderId: requirePositiveNumber(line.activeOrderId, '活跃订单不能为空'),
    allocatedQuantity: requirePositiveNumber(line.allocatedQuantity, '分配数量必须大于 0')
  }))
  if (lines.length === 0) {
    throw new Error('生产组长确认报工前必须分配到活跃订单')
  }
  return lines
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
    queryParams.templateType = 'PQC_SIMPLIFIED'
  } else if (queryParams.templateType === 'PQC_SIMPLIFIED') {
    queryParams.templateType = undefined
  }
  if (leaderType === 'PRODUCTION') {
    loadActiveOrders().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    })
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

const openReview = async (event: ProcessPoolTimelineEventVO) => {
  requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  reviewEvent.value = event
  reviewForm.reviewStatus = 'APPROVED'
  resetReviewAllocation()
  reviewForm.reviewRemark = ''
  reviewVisible.value = true
  if (isProductionLeader.value) {
    try {
      await loadActiveOrders()
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    }
  }
}

const submitReview = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  reviewSubmitting.value = true
  try {
    const leaderType = queryParams.leaderType as TeamLeaderType
    const reviewRemark = reviewForm.reviewRemark.trim() || undefined
    if (isProductionLeader.value && reviewForm.reviewStatus === 'APPROVED') {
      await confirmTeamLeaderReportAllocation({
        eventId,
        leaderType,
        allocationMode: reviewForm.allocationMode,
        reviewRemark,
        allocations: buildAllocationSubmitLines()
      })
    } else {
      await reviewTeamLeaderSubmission({
        leaderType,
        eventId,
        reviewStatus: reviewForm.reviewStatus,
        reviewRemark
      })
    }
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
  abnormalForm.workOrderId = normalizePositiveNumber(event.workOrderId)
  const matchedActiveOrder = activeOrderOptions.value.find(
    (order) => order.workOrderId === abnormalForm.workOrderId
  )
  abnormalForm.activeOrderId = matchedActiveOrder?.id
  abnormalForm.routeProcessId = normalizePositiveNumber(event.routeProcessId)
  abnormalForm.processId = normalizePositiveNumber(event.processId)
  abnormalForm.sourceEventId = normalizePositiveNumber(event.id)
}

const handleAbnormalActiveOrderChange = (activeOrderId?: number) => {
  const activeOrder = activeOrderOptions.value.find((order) => order.id === activeOrderId)
  abnormalForm.workOrderId = activeOrder?.workOrderId
}

const requireSelectedActiveOrderWorkOrderId = () => {
  const activeOrderId = requirePositiveNumber(abnormalForm.activeOrderId, '活跃订单不能为空')
  const activeOrder = activeOrderOptions.value.find((order) => order.id === activeOrderId)
  if (!activeOrder) {
    throw new Error('活跃订单不存在或已移出')
  }
  return activeOrder.workOrderId
}

const resolveStructuredPayloadItems = (rawPayload?: string) => {
  if (!rawPayload?.trim()) return []
  try {
    const parsed = JSON.parse(rawPayload)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return [{ field: 'payload', value: String(parsed) }]
    }
    return Object.entries(parsed).map(([field, value]) => ({
      field,
      value: typeof value === 'object' ? JSON.stringify(value) : String(value)
    }))
  } catch {
    return [{ field: 'payload', value: rawPayload }]
  }
}

const submitAbnormal = async () => {
  const valid = await abnormalFormRef.value?.validate?.()
  if (valid === false) return
  abnormalSubmitting.value = true
  try {
    await markAndReportWorkOrderAbnormal({
      workOrderId: requireSelectedActiveOrderWorkOrderId(),
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

const submitAddActiveOrder = async () => {
  maintenanceSubmitting.value = true
  try {
    await addTeamLeaderActiveOrder({
      workOrderId: requirePositiveNumber(activeOrderForm.workOrderId, '生产订单ID不能为空')
    })
    ElMessage.success('活跃订单已加入')
    await loadActiveOrders()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '活跃订单加入失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitRemoveActiveOrder = async () => {
  maintenanceSubmitting.value = true
  try {
    await removeTeamLeaderActiveOrder({
      activeOrderId: requirePositiveNumber(activeOrderRemoveForm.activeOrderId, '活跃订单记录ID不能为空')
    })
    ElMessage.success('活跃订单已移出')
    await loadActiveOrders()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '活跃订单移出失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitEmployeeProfile = async () => {
  maintenanceSubmitting.value = true
  try {
    await createTeamEmployeeProfile({
      systemUserId: normalizePositiveNumber(employeeProfileForm.systemUserId),
      employeeCode: employeeProfileForm.employeeCode.trim(),
      employeeName: employeeProfileForm.employeeName.trim(),
      employeeType: employeeProfileForm.employeeType
    })
    ElMessage.success('员工档案已新增')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '员工档案新增失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitProcessEmployeeBinding = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamProcessEmployeeBinding({
      processId: requirePositiveNumber(processEmployeeBindingForm.processId, '工序ID不能为空'),
      employeeProfileId: requirePositiveNumber(
        processEmployeeBindingForm.employeeProfileId,
        '员工档案ID不能为空'
      )
    })
    ElMessage.success('工序员工关系已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序员工关系保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitTeamDevice = async () => {
  maintenanceSubmitting.value = true
  try {
    await createTeamDevice({
      deviceCode: teamDeviceForm.deviceCode.trim(),
      deviceName: teamDeviceForm.deviceName.trim(),
      deviceStatus: teamDeviceForm.deviceStatus
    })
    ElMessage.success('设备已新增')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备新增失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitTeamDeviceStatus = async () => {
  maintenanceSubmitting.value = true
  try {
    await updateTeamDeviceStatus({
      deviceId: requirePositiveNumber(teamDeviceStatusForm.deviceId, '设备ID不能为空'),
      deviceStatus: teamDeviceStatusForm.deviceStatus
    })
    ElMessage.success('设备状态已更新')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备状态更新失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitProcessDeviceBinding = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamProcessDeviceBinding({
      processId: requirePositiveNumber(processDeviceBindingForm.processId, '工序ID不能为空'),
      deviceId: requirePositiveNumber(processDeviceBindingForm.deviceId, '设备ID不能为空')
    })
    ElMessage.success('工序设备关系已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序设备关系保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitProcessDefectReason = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamProcessDefectReason({
      processId: requirePositiveNumber(defectReasonForm.processId, '工序ID不能为空'),
      reasonType: defectReasonForm.reasonType,
      reasonCode: defectReasonForm.reasonCode.trim(),
      reasonName: defectReasonForm.reasonName.trim()
    })
    const nextReason = {
      reasonType: defectReasonForm.reasonType,
      reasonCode: defectReasonForm.reasonCode.trim(),
      reasonName: defectReasonForm.reasonName.trim()
    }
    configuredDefectReasonOptions.value = [
      ...configuredDefectReasonOptions.value.filter(
        (reason) => reason.reasonCode !== nextReason.reasonCode
      ),
      nextReason
    ]
    ElMessage.success('工序异常原因已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序异常原因保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitRuntimeDeviceRule = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamRuntimeDeviceParameterRule({
      processId: requirePositiveNumber(deviceRuleForm.processId, '工序ID不能为空'),
      deviceId: requirePositiveNumber(deviceRuleForm.deviceId, '设备ID不能为空'),
      parameterCode: deviceRuleForm.parameterCode.trim(),
      parameterName: deviceRuleForm.parameterName.trim() || undefined,
      unit: deviceRuleForm.unit.trim() || undefined,
      lowerLimit: requireFiniteNumber(deviceRuleForm.lowerLimit, '参数下限不能为空'),
      upperLimit: requireFiniteNumber(deviceRuleForm.upperLimit, '参数上限不能为空'),
      defaultValue: normalizeFiniteNumber(deviceRuleForm.defaultValue),
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
