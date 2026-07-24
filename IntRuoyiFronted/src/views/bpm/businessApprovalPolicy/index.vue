<template>
  <doc-alert title="平台业务审批策略" url="https://doc.iocoder.cn/bpm/" />

  <ContentWrap v-hasPermi="['bpm:business-approval-policy:query']">
    <UnifiedListTemplate
      table-key="bpm.business-approval-policy.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="businessApprovalPolicyQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="businessApprovalPolicyQuickFilter.state"
      :selected-filter-definition="businessApprovalPolicyQuickFilter.selectedDefinition.value"
      :operator-options="businessApprovalPolicyQuickFilter.operatorOptions.value"
      :columns="businessApprovalPolicyColumns"
      :column-saving="businessApprovalPolicyColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="businessApprovalPolicyQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveBusinessApprovalPolicyColumnConfig"
      @column-reset="resetBusinessApprovalPolicyColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openPolicyForm()"
          v-hasPermi="['bpm:business-approval-policy:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增策略
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          data-user-table-column-explicit
          data-user-table-key="bpm.business-approval-policy.main"
          @header-dragend="handleBusinessApprovalPolicyHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('dataDomain')"
            label="数据域"
            align="center"
            prop="dataDomain"
            :width="getBusinessApprovalPolicyColumnWidthString('dataDomain')"
            :min-width="getBusinessApprovalPolicyColumnMinWidthString('dataDomain', 120)"
            v-bind="sortColumnAttrs('dataDomain')"
          >
            <template #default="{ row }">
              <span :title="row.dataDomain">{{ formatPolicyDataDomain(row.dataDomain) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('systemCode')"
            label="系统编码"
            align="center"
            prop="systemCode"
            :width="getBusinessApprovalPolicyColumnWidthString('systemCode')"
            :min-width="getBusinessApprovalPolicyColumnMinWidthString('systemCode', 120)"
            v-bind="sortColumnAttrs('systemCode')"
          >
            <template #default="{ row }">
              <span :title="row.systemCode">{{ formatPolicySystemCode(row.systemCode) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('objectType')"
            label="对象类型"
            align="center"
            prop="objectType"
            :width="getBusinessApprovalPolicyColumnWidthString('objectType')"
            :min-width="getBusinessApprovalPolicyColumnMinWidthString('objectType', 140)"
            v-bind="sortColumnAttrs('objectType')"
          >
            <template #default="{ row }">
              <span :title="row.objectType">{{ formatPolicyObjectType(row.objectType) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('actionCode')"
            label="动作编码"
            align="center"
            prop="actionCode"
            :width="getBusinessApprovalPolicyColumnWidthString('actionCode')"
            :min-width="getBusinessApprovalPolicyColumnMinWidthString('actionCode', 150)"
            v-bind="sortColumnAttrs('actionCode')"
          >
            <template #default="{ row }">
              <span :title="row.actionCode">{{ formatPolicyActionCode(row.actionCode) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('objectState')"
            label="对象状态"
            align="center"
            prop="objectState"
            :width="getBusinessApprovalPolicyColumnWidthString('objectState')"
            :min-width="getBusinessApprovalPolicyColumnMinWidthString('objectState', 130)"
            v-bind="sortColumnAttrs('objectState')"
          >
            <template #default="{ row }">
              <span :title="row.objectState">{{ formatPolicyObjectState(row.objectState) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('policyMode')"
            label="审批模式"
            align="center"
            prop="policyMode"
            :width="getBusinessApprovalPolicyColumnWidthString('policyMode', 120)"
            v-bind="sortColumnAttrs('policyMode')"
          >
            <template #default="{ row }">
              <el-tag :type="row.policyMode === 'BPM_REQUIRED' ? 'warning' : row.policyMode === 'SIGNATURE_REQUIRED' ? 'primary' : 'success'">
                {{ formatPolicyMode(row.policyMode) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('approvalSwitch')"
            label="审批开关"
            align="center"
            prop="approvalSwitch"
            :width="getBusinessApprovalPolicyColumnWidthString('approvalSwitch', 130)"
          >
            <template #default="{ row }">
              <el-switch
                v-if="row.status === 'PUBLISHED'"
                v-hasPermi="['bpm:business-approval-policy:publish']"
                :model-value="row.policyMode === 'BPM_REQUIRED'"
                :loading="switchingPolicyId === row.id"
                :disabled="switchingPolicyId !== undefined"
                active-text="开启"
                inactive-text="关闭"
                inline-prompt
                @change="(enabled) => handlePolicyModeSwitch(row, Boolean(enabled))"
              />
              <el-tag v-else type="info">{{ formatPolicySwitchUnavailable(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('effectExecutorCode')"
            label="领域执行器"
            align="center"
            prop="effectExecutorCode"
            :width="getBusinessApprovalPolicyColumnWidthString('effectExecutorCode')"
            :min-width="getBusinessApprovalPolicyColumnMinWidthString('effectExecutorCode', 220)"
            v-bind="sortColumnAttrs('effectExecutorCode')"
          >
            <template #default="{ row }">
              <span :title="row.effectExecutorCode">{{ formatPolicyEffectExecutorCode(row.effectExecutorCode) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getBusinessApprovalPolicyColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
                {{ formatPolicyStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('updatedTime')"
            label="更新时间"
            align="center"
            prop="updatedTime"
            :formatter="dateFormatter"
            :width="getBusinessApprovalPolicyColumnWidthString('updatedTime', 180)"
            v-bind="sortColumnAttrs('updatedTime')"
          />
          <el-table-column
            v-if="isBusinessApprovalPolicyColumnVisible('actions')"
            label="操作"
            align="center"
            prop="actions"
            fixed="right"
            :width="getBusinessApprovalPolicyColumnWidthString('actions', 170)"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openPolicyForm(row)"
                v-hasPermi="['bpm:business-approval-policy:create']"
              >
                复制新增
              </el-button>
              <el-button
                v-if="row.status === 'DRAFT'"
                link
                type="success"
                :loading="publishingPolicyId === row.id"
                @click="publishPolicy(row.id)"
                v-hasPermi="['bpm:business-approval-policy:publish']"
              >
                发布
              </el-button>
              <el-button
                v-if="row.status === 'PUBLISHED'"
                link
                type="warning"
                :loading="disablingPolicyId === row.id"
                @click="disablePolicy(row.id)"
                v-hasPermi="['bpm:business-approval-policy:disable']"
              >
                停用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <Dialog :title="policyFormTitle" v-model="policyFormVisible" width="720px">
    <el-form
      ref="policyFormRef"
      :model="policyFormData"
      :rules="policyFormRules"
      label-width="128px"
      v-loading="policyFormLoading"
    >
      <el-form-item label="数据域" prop="dataDomain">
        <el-input v-model="policyFormData.dataDomain" placeholder="请输入数据域，如 MES" />
      </el-form-item>
      <el-form-item label="系统编码" prop="systemCode">
        <el-input v-model="policyFormData.systemCode" placeholder="请输入系统编码，如 MES" />
      </el-form-item>
      <el-form-item label="对象类型" prop="objectType">
        <el-input v-model="policyFormData.objectType" placeholder="请输入对象类型" />
      </el-form-item>
      <el-form-item label="动作编码" prop="actionCode">
        <el-input v-model="policyFormData.actionCode" placeholder="请输入动作编码" />
      </el-form-item>
      <el-form-item label="对象状态" prop="objectState">
        <el-input v-model="policyFormData.objectState" placeholder="请输入对象状态" />
      </el-form-item>
      <el-form-item label="审批模式" prop="policyMode">
        <el-segmented v-model="policyFormData.policyMode" :options="policyModeSegmentOptions" />
      </el-form-item>
      <el-form-item label="领域执行器" prop="effectExecutorCode">
        <el-input v-model="policyFormData.effectExecutorCode" placeholder="请输入领域执行器编码" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="policyFormData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitPolicyForm" type="primary" :disabled="policyFormLoading">
        保存草稿
      </el-button>
      <el-button @click="policyFormVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { dateFormatter } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import {
  BusinessApprovalPolicyApi,
  type BusinessApprovalPolicyMode,
  type BusinessApprovalPolicySaveReqVO,
  type BusinessApprovalPolicyStatus,
  type BusinessApprovalPolicySwitchModeReqVO,
  type BusinessApprovalPolicyVO
} from '@/api/bpm/businessApprovalPolicy'

defineOptions({ name: 'BpmBusinessApprovalPolicy' })

const message = useMessage()
const { t } = useI18n()

const businessApprovalPolicyDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'dataDomain', label: '数据域', minWidth: 120 },
  { key: 'systemCode', label: '系统编码', minWidth: 120 },
  { key: 'objectType', label: '对象类型', minWidth: 140 },
  { key: 'actionCode', label: '动作编码', minWidth: 150 },
  { key: 'objectState', label: '对象状态', minWidth: 130 },
  { key: 'policyMode', label: '审批模式', width: 150 },
  { key: 'approvalSwitch', label: '审批开关', width: 130, hideable: false, business: false },
  { key: 'effectExecutorCode', label: '领域执行器', minWidth: 220 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'updatedTime', label: '更新时间', width: 180 },
  { key: 'actions', label: '操作', width: 170, hideable: false, business: false }
]

const {
  columns: businessApprovalPolicyColumns,
  saving: businessApprovalPolicyColumnSaving,
  isColumnVisible: isBusinessApprovalPolicyColumnVisible,
  getColumnWidthString: getBusinessApprovalPolicyColumnWidthString,
  getColumnMinWidthString: getBusinessApprovalPolicyColumnMinWidthString,
  handleHeaderDragend: handleBusinessApprovalPolicyHeaderDragend,
  saveConfig: saveBusinessApprovalPolicyColumnConfig,
  resetConfig: resetBusinessApprovalPolicyColumnConfig
} = useUserTableColumns(
  'bpm.business-approval-policy.main',
  businessApprovalPolicyDefaultColumns
)

const policyModeOptions: Array<{ label: string; value: BusinessApprovalPolicyMode }> = [
  { label: 'BPM审批', value: 'BPM_REQUIRED' },
  { label: '历史签名模式', value: 'SIGNATURE_REQUIRED' },
  { label: '关闭审批', value: 'DIRECT' }
]

const policyStatusOptions: Array<{ label: string; value: BusinessApprovalPolicyStatus }> = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已停用', value: 'DISABLED' }
]

const policySystemLabels: Record<string, string> = {
  MES: '生产执行',
  FORM_CENTER: '表单中心',
  DCC: '文控中心'
}

const policyObjectTypeLabels: Record<string, string> = {
  ROUTE_VERSION: '工艺路线版本',
  FORM_TEMPLATE: '表单模板',
  CONTROLLED_FILE: '受控文件',
  BATCH_RECORD_VERSION: '批记录版本',
  EDHR_BATCH_EXECUTION: '批次执行记录'
}

const policyActionCodeLabels: Record<string, string> = {
  PUBLISH: '发布',
  UPGRADE: '升版',
  OBSOLETE: '作废',
  VOID: '作废',
  SUBMIT_REVIEW: '提交审核'
}

const policyObjectStateLabels: Record<string, string> = {
  READY_TO_PUBLISH: '待发布',
  READY: '就绪',
  DRAFT: '草稿',
  ACTIVE: '已生效',
  PRECHECK_PASSED: '预检通过',
  RELEASED: '已放行',
  CLOSED: '已关闭',
  ALL: '所有',
  REJECTED: '已驳回',
  PUBLISHED: '已发布',
  DISABLED: '已禁用'
}

const policyEffectExecutorCodeLabels: Record<string, string> = {
  MES_ROUTE_VERSION_PUBLISH: '工艺路线版本发布',
  FORM_TEMPLATE_UPGRADE: '表单模板升版',
  FORM_TEMPLATE_OBSOLETE: '表单模板作废',
  DCC_PUBLISH: '受控文件发布',
  DCC_OBSOLETE: '受控文件作废',
  BATCH_RECORD_VERSION_PUBLISH: '批记录版本发布',
  MES_BATCH_RECORD_VERSION_PUBLISH: '批记录版本发布',
  EDHR_BATCH_EXECUTION_SUBMIT_REVIEW: '批次执行提交审核',
  EDHR_BATCH_VOID: '批次执行作废'
}

const formatPolicyCodeLabel = (value: string | undefined, labels: Record<string, string>) => {
  const rawValue = value?.trim()
  if (!rawValue) return '--'
  return labels[rawValue.toUpperCase()] || rawValue
}

const formatPolicyDataDomain = (dataDomain: string | undefined) =>
  formatPolicyCodeLabel(dataDomain, policySystemLabels)

const formatPolicySystemCode = (systemCode: string | undefined) =>
  formatPolicyCodeLabel(systemCode, policySystemLabels)

const formatPolicyObjectType = (objectType: string | undefined) =>
  formatPolicyCodeLabel(objectType, policyObjectTypeLabels)

const formatPolicyActionCode = (actionCode: string | undefined) =>
  formatPolicyCodeLabel(actionCode, policyActionCodeLabels)

const formatPolicyObjectState = (objectState: string | undefined) =>
  formatPolicyCodeLabel(objectState, policyObjectStateLabels)

const formatPolicyEffectExecutorCode = (effectExecutorCode: string | undefined) =>
  formatPolicyCodeLabel(effectExecutorCode, policyEffectExecutorCodeLabels)

const loading = ref(true)
const list = ref<BusinessApprovalPolicyVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  dataDomain: undefined as string | undefined,
  systemCode: undefined as string | undefined,
  objectType: undefined as string | undefined,
  actionCode: undefined as string | undefined,
  objectState: undefined as string | undefined,
  policyMode: undefined as BusinessApprovalPolicyMode | undefined,
  status: undefined as BusinessApprovalPolicyStatus | undefined
})

const businessApprovalPolicyQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'dataDomain', label: '数据域', type: 'text', queryParamKey: 'dataDomain', placeholder: '请输入数据域' },
  { key: 'systemCode', label: '系统编码', type: 'text', queryParamKey: 'systemCode', placeholder: '请输入系统编码' },
  { key: 'objectType', label: '对象类型', type: 'text', queryParamKey: 'objectType', placeholder: '请输入对象类型' },
  { key: 'actionCode', label: '动作编码', type: 'text', queryParamKey: 'actionCode', placeholder: '请输入动作编码' },
  { key: 'objectState', label: '对象状态', type: 'text', queryParamKey: 'objectState', placeholder: '请输入对象状态' },
  { key: 'policyMode', label: '审批模式', type: 'select', queryParamKey: 'policyMode', options: policyModeOptions },
  { key: 'status', label: '状态', type: 'select', queryParamKey: 'status', options: policyStatusOptions }
])

const resolveBusinessApprovalPolicyErrorMessage = (error: unknown) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (responseMessage) return responseMessage
  const errorMessage = (error as Error)?.message
  return errorMessage || '平台业务审批策略请求失败'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await BusinessApprovalPolicyApi.getPolicyPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveBusinessApprovalPolicyErrorMessage(error))
    throw error
  } finally {
    loading.value = false
  }
}

const businessApprovalPolicyQuickFilter = useTableQuickFilter(
  'bpm.business-approval-policy.main',
  businessApprovalPolicyQuickFilterDefinitions,
  queryParams,
  getList
)

const handleQuery = async () => {
  await businessApprovalPolicyQuickFilter.applyQuickFilter()
}

const resetQuery = async () => {
  await businessApprovalPolicyQuickFilter.resetQuickFilter()
}

const policyFormVisible = ref(false)
const policyFormTitle = ref('新增平台业务审批策略')
const policyFormLoading = ref(false)
const policyFormRef = ref()
const publishingPolicyId = ref<number>()
const disablingPolicyId = ref<number>()
const switchingPolicyId = ref<number>()
const policyFormData = ref<BusinessApprovalPolicySaveReqVO>({
  dataDomain: '',
  systemCode: '',
  objectType: '',
  actionCode: '',
  objectState: '',
  policyMode: 'DIRECT',
  processDefinitionKey: undefined,
  effectExecutorCode: '',
  remark: ''
})

const policyModeSegmentOptions = computed(() =>
  policyModeOptions.filter(
    (item) =>
      item.value !== 'SIGNATURE_REQUIRED' &&
      (item.value !== 'BPM_REQUIRED' || Boolean(policyFormData.value.processDefinitionKey))
  )
)

const policyFormRules = reactive({
  dataDomain: [{ required: true, message: '数据域不能为空', trigger: 'blur' }],
  systemCode: [{ required: true, message: '系统编码不能为空', trigger: 'blur' }],
  objectType: [{ required: true, message: '对象类型不能为空', trigger: 'blur' }],
  actionCode: [{ required: true, message: '动作编码不能为空', trigger: 'blur' }],
  objectState: [{ required: true, message: '对象状态不能为空', trigger: 'blur' }],
  policyMode: [{ required: true, message: '审批模式不能为空', trigger: 'change' }],
  effectExecutorCode: [{ required: true, message: '领域执行器编码不能为空', trigger: 'blur' }]
})

const formatPolicyMode = (mode: BusinessApprovalPolicyMode) =>
  policyModeOptions.find((item) => item.value === mode)?.label || mode

const formatPolicyStatus = (status: BusinessApprovalPolicyStatus) =>
  policyStatusOptions.find((item) => item.value === status)?.label || status

const formatPolicySwitchUnavailable = (row: BusinessApprovalPolicyVO) =>
  row.status === 'DISABLED' ? '已停用，请复制新增' : '草稿发布后可切换'

const resetPolicyForm = () => {
  policyFormData.value = {
    dataDomain: '',
    systemCode: '',
    objectType: '',
    actionCode: '',
    objectState: '',
    policyMode: 'DIRECT',
    processDefinitionKey: undefined,
    effectExecutorCode: '',
    remark: ''
  }
  policyFormRef.value?.resetFields()
}

const openPolicyForm = (source?: BusinessApprovalPolicyVO) => {
  policyFormVisible.value = true
  policyFormTitle.value = source ? '复制新增平台业务审批策略' : '新增平台业务审批策略'
  resetPolicyForm()
  if (!source) return
  policyFormData.value = {
    dataDomain: source.dataDomain,
    systemCode: source.systemCode,
    objectType: source.objectType,
    actionCode: source.actionCode,
    objectState: source.objectState,
    policyMode: source.policyMode === 'SIGNATURE_REQUIRED' ? 'DIRECT' : source.policyMode,
    processDefinitionKey: source.processDefinitionKey,
    effectExecutorCode: source.effectExecutorCode,
    remark: source.remark || ''
  }
}

const submitPolicyForm = async () => {
  const valid = await policyFormRef.value.validate().catch(() => false)
  if (!valid) return
  policyFormLoading.value = true
  try {
    const data = {
      ...policyFormData.value,
      remark: policyFormData.value.remark?.trim()
    }
    await BusinessApprovalPolicyApi.savePolicy(data)
    message.success(t('common.createSuccess'))
    policyFormVisible.value = false
    await getList()
  } catch (error) {
    message.error(resolveBusinessApprovalPolicyErrorMessage(error))
    throw error
  } finally {
    policyFormLoading.value = false
  }
}

const publishPolicy = async (id: number) => {
  try {
    await message.confirm('确认发布该平台业务审批策略？发布后同一业务动作会按该策略执行。')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
  publishingPolicyId.value = id
  try {
    await BusinessApprovalPolicyApi.publishPolicy(id)
    message.success('发布成功')
    await getList()
  } catch (error) {
    message.error(resolveBusinessApprovalPolicyErrorMessage(error))
    throw error
  } finally {
    publishingPolicyId.value = undefined
  }
}

const disablePolicy = async (id: number) => {
  try {
    await message.confirm('确认停用该平台业务审批策略？停用后同一业务动作必须重新发布显式策略。')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
  disablingPolicyId.value = id
  try {
    await BusinessApprovalPolicyApi.disablePolicy(id)
    message.success('停用成功')
    await getList()
  } catch (error) {
    message.error(resolveBusinessApprovalPolicyErrorMessage(error))
    throw error
  } finally {
    disablingPolicyId.value = undefined
  }
}

const handlePolicyModeSwitch = async (row: BusinessApprovalPolicyVO, enabled: boolean) => {
  const targetMode: BusinessApprovalPolicyMode = enabled ? 'BPM_REQUIRED' : 'DIRECT'
  if (row.policyMode === targetMode) return
  let signaturePassword = ''
  try {
    const promptResult = await message.prompt('请输入电子签名密码', '审批开关电子签名', {
      inputType: 'password',
      inputPlaceholder: '请输入电子签名密码',
      inputValidator: (value) => Boolean(String(value || '').trim()),
      inputErrorMessage: '电子签名密码不能为空'
    })
    signaturePassword = String(promptResult.value || '').trim()
    if (!signaturePassword) {
      message.warning('电子签名密码不能为空')
      await getList()
      return
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      await getList()
      return
    }
    throw error
  }

  switchingPolicyId.value = row.id
  try {
    const data: BusinessApprovalPolicySwitchModeReqVO = {
      policyMode: targetMode,
      signaturePassword
    }
    await BusinessApprovalPolicyApi.switchPolicyMode(row.id, data)
    message.success(targetMode === 'BPM_REQUIRED' ? '审批流程已开启' : '审批已关闭')
    await getList()
  } catch (error) {
    message.error(resolveBusinessApprovalPolicyErrorMessage(error))
    await getList()
    throw error
  } finally {
    switchingPolicyId.value = undefined
  }
}

onMounted(() => {
  getList()
})
</script>
