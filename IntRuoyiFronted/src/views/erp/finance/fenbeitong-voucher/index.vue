<template>
  <ContentWrap>
    <el-alert
      title="当前支持固定 JSON 模拟、分贝通 OpenAPI 配置和本地准备记录；写入金蝶只保存草稿凭证，由财务人工审核。真实拉取任务默认暂停且 prepareOnly=true，待 access-token 与金蝶可保存示例齐备后再启用。"
      type="warning"
      :closable="false"
      show-icon
      class="mb-15px"
    />
    <el-form
      ref="configFormRef"
      :model="configForm"
      :rules="configRules"
      label-width="150px"
      class="-mb-15px"
    >
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="账簿编码" prop="accountBookNumber">
            <el-input v-model="configForm.accountBookNumber" placeholder="例如 011" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="凭证字编码" prop="voucherGroupNumber">
            <el-input v-model="configForm.voucherGroupNumber" placeholder="例如 PZZ8" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="凭证模板 FID" prop="templateErpFid">
            <el-input v-model="configForm.templateErpFid" placeholder="金蝶模板凭证 FID" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="部门维度字段" prop="departmentDetailField">
            <el-input v-model="configForm.departmentDetailField" placeholder="FDETAILID__FFLEX5" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="员工维度字段" prop="employeeDetailField">
            <el-input v-model="configForm.employeeDetailField" placeholder="可为空" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="贷方科目编码" prop="creditAccountNumber">
            <el-input v-model="configForm.creditAccountNumber" placeholder="例如 1002.01" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="汇率类型编码" prop="exchangeRateTypeNumber">
            <el-input v-model="configForm.exchangeRateTypeNumber" placeholder="HLTX01_SYS" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="汇率" prop="exchangeRate">
            <el-input-number
              v-model="configForm.exchangeRate"
              :min="0.000001"
              :precision="6"
              controls-position="right"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="拆分进项税" prop="splitDeductibleTax">
            <el-switch v-model="configForm.splitDeductibleTax" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="进项税科目" prop="taxAccountNumber">
            <el-input v-model="configForm.taxAccountNumber" placeholder="2221.01.01.05" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="模拟凭证日期" prop="mockVoucherDate">
            <el-date-picker
              v-model="configForm.mockVoucherDate"
              type="date"
              value-format="YYYY-MM-DD"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="年度" prop="mockYear">
            <el-input-number
              v-model="configForm.mockYear"
              :min="1"
              controls-position="right"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="期间" prop="mockPeriod">
            <el-input-number
              v-model="configForm.mockPeriod"
              :min="1"
              controls-position="right"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="分贝通 OpenAPI" prop="fenbeitongBaseUrl">
            <el-input
              v-model="configForm.fenbeitongBaseUrl"
              placeholder="https://openapi.fenbeitong.com"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="access-token" prop="fenbeitongAccessToken">
            <el-input
              v-model="configForm.fenbeitongAccessToken"
              type="password"
              show-password
              placeholder="待分贝通开通后填写"
            />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="报销单状态" prop="fenbeitongReimbursementApplyState">
            <el-input-number
              v-model="configForm.fenbeitongReimbursementApplyState"
              :min="0"
              controls-position="right"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="每页数量" prop="fenbeitongReimbursementPageSize">
            <el-input-number
              v-model="configForm.fenbeitongReimbursementPageSize"
              :min="1"
              :max="20"
              controls-position="right"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="币种映射 JSON" prop="currencyNumbersText">
            <el-input
              v-model="mappingText.currencyNumbers"
              type="textarea"
              :rows="3"
              placeholder='{"CNY":"PRE001"}'
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="费用科目映射 JSON" prop="categoryAccountNumbersText">
            <el-input
              v-model="mappingText.categoryAccountNumbers"
              type="textarea"
              :rows="3"
              placeholder='{"TRAVEL":"6601.09","OFFICE":"6601.15"}'
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="贷方维度 JSON" prop="creditDetailNumbersText">
            <el-input
              v-model="mappingText.creditDetailNumbers"
              type="textarea"
              :rows="3"
              placeholder='{"FDETAILID__FF100009":"31050179420000002440"}'
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="模拟固定 JSON" prop="mockFixedJson">
            <el-input
              v-model="configForm.mockFixedJson"
              type="textarea"
              :rows="3"
              placeholder="粘贴分贝通报销详情固定 JSON"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button
          type="primary"
          @click="handleSaveConfig"
          :loading="configSaving"
          v-hasPermi="['erp:fenbeitong-voucher:config']"
        >
          <Icon icon="ep:check" class="mr-5px" />保存配置
        </el-button>
        <el-button
          @click="handleLoadConfig"
          :loading="configLoading"
          v-hasPermi="['erp:fenbeitong-voucher:query']"
        >
          <Icon icon="ep:refresh" class="mr-5px" />重新加载
        </el-button>
        <el-button
          @click="handleLoadMockTemplate"
          :loading="mockTemplateLoading"
          v-hasPermi="['erp:fenbeitong-voucher:query']"
        >
          <Icon icon="ep:magic-stick" class="mr-5px" />{{ mockTemplateButtonText }}
        </el-button>
        <el-button
          type="success"
          plain
          @click="handlePreviewMock"
          :loading="previewLoading"
          v-hasPermi="['erp:fenbeitong-voucher:query']"
        >
          <Icon icon="ep:view" class="mr-5px" />预览模拟凭证
        </el-button>
        <el-button
          type="warning"
          plain
          @click="handlePrepareMock"
          :loading="prepareLoading"
          v-hasPermi="['erp:fenbeitong-voucher:save']"
        >
          <Icon icon="ep:document-checked" class="mr-5px" />生成本地准备记录
        </el-button>
        <el-button
          type="danger"
          plain
          @click="handleSaveMock"
          :loading="saveLoading"
          v-hasPermi="['erp:fenbeitong-voucher:save']"
        >
          <Icon icon="ep:upload" class="mr-5px" />保存到金蝶草稿
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="traceQuery" label-width="90px">
      <el-form-item label="来源 ID">
        <el-input
          v-model="traceQuery.sourceId"
          placeholder="分贝通 reimb_id"
          clearable
          class="!w-260px"
          @keyup.enter="handleQueryProcess"
        />
      </el-form-item>
      <el-form-item label="ERP FID">
        <el-input
          v-model="traceQuery.erpFid"
          placeholder="金蝶凭证 FID"
          clearable
          class="!w-220px"
          @keyup.enter="handleViewErp"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQueryProcess" :loading="processLoading">
          <Icon icon="ep:search" class="mr-5px" />查询处理记录
        </el-button>
        <el-button @click="handleViewErp" :loading="erpViewLoading">
          <Icon icon="ep:document" class="mr-5px" />只读查看 ERP
        </el-button>
        <el-button @click="handleBusinessInfo" :loading="businessInfoLoading">
          <Icon icon="ep:info-filled" class="mr-5px" />业务元数据
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="previewResult">
    <template #header>
      <span>凭证预览</span>
    </template>
    <el-descriptions :column="3" border>
      <el-descriptions-item label="来源 ID">{{ previewResult.sourceId }}</el-descriptions-item>
      <el-descriptions-item label="来源单号">{{ previewResult.sourceCode }}</el-descriptions-item>
      <el-descriptions-item label="金额">{{ previewResult.totalAmount }}</el-descriptions-item>
      <el-descriptions-item label="可抵扣税额">
        {{ previewResult.deductibleTaxAmount }}
      </el-descriptions-item>
      <el-descriptions-item label="幂等键" :span="2">
        {{ previewResult.idempotencyKey }}
      </el-descriptions-item>
      <el-descriptions-item label="标记" :span="3">{{ previewResult.marker }}</el-descriptions-item>
    </el-descriptions>
    <el-input
      class="mt-15px"
      :model-value="formatJson(previewResult.payload)"
      type="textarea"
      :rows="12"
      readonly
    />
  </ContentWrap>

  <ContentWrap v-if="saveResult || processResult || erpViewResult || businessInfoResult">
    <template #header>
      <span>联调结果</span>
    </template>
    <el-descriptions v-if="saveResult" :column="3" border class="mb-15px">
      <el-descriptions-item label="已保存">{{ saveResult.saved ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="ERP FID">{{ saveResult.erpFid }}</el-descriptions-item>
      <el-descriptions-item label="凭证号">{{ saveResult.erpNumber }}</el-descriptions-item>
    </el-descriptions>
    <el-descriptions v-if="processResult" :column="3" border class="mb-15px">
      <el-descriptions-item label="来源 ID">{{ processResult.sourceId }}</el-descriptions-item>
      <el-descriptions-item label="阶段">{{ processResult.processStage }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ processStatusText(processResult.processStatus) }}</el-descriptions-item>
      <el-descriptions-item label="ERP FID">{{ processResult.erpFid || '-' }}</el-descriptions-item>
      <el-descriptions-item label="凭证号">{{ processResult.erpNumber || '-' }}</el-descriptions-item>
      <el-descriptions-item label="单据状态">
        {{ processResult.erpDocumentStatus || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="错误" :span="3">
        {{ processResult.errorMessage || '-' }}
      </el-descriptions-item>
    </el-descriptions>
    <el-input
      v-if="processResult?.voucherPayload"
      class="mb-15px"
      :model-value="formatJson(parseJsonText(processResult.voucherPayload))"
      type="textarea"
      :rows="12"
      readonly
    />
    <el-input
      v-if="erpViewResult"
      class="mb-15px"
      :model-value="formatJson(erpViewResult)"
      type="textarea"
      :rows="10"
      readonly
    />
    <el-input
      v-if="businessInfoResult"
      :model-value="formatJson(businessInfoResult)"
      type="textarea"
      :rows="10"
      readonly
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import {
  FenbeitongVoucherApi,
  FenbeitongVoucherConfigVO,
  FenbeitongVoucherPreviewVO,
  FenbeitongVoucherProcessVO,
  FenbeitongVoucherSaveVO
} from '@/api/erp/finance/fenbeitong-voucher'

defineOptions({ name: 'ErpFenbeitongVoucher' })

const message = useMessage()

const mockTemplateButtonText = '\u586b\u5165\u6a21\u62df\u6a21\u677f'
const mockTemplateLoadedMessage = '\u6a21\u62df\u6a21\u677f\u5df2\u586b\u5165\uff0c\u8bf7\u786e\u8ba4\u540e\u4fdd\u5b58\u914d\u7f6e'

const configFormRef = ref()
const configLoading = ref(false)
const configSaving = ref(false)
const mockTemplateLoading = ref(false)
const previewLoading = ref(false)
const saveLoading = ref(false)
const prepareLoading = ref(false)
const processLoading = ref(false)
const erpViewLoading = ref(false)
const businessInfoLoading = ref(false)

const configForm = reactive<FenbeitongVoucherConfigVO>({
  accountBookNumber: '',
  voucherGroupNumber: '',
  voucherGroupNo: '',
  templateErpFid: '',
  currencyNumbers: {},
  categoryAccountNumbers: {},
  departmentDetailField: '',
  employeeDetailField: '',
  creditAccountNumber: '',
  creditDetailNumbers: {},
  exchangeRateTypeNumber: '',
  exchangeRate: 1,
  splitDeductibleTax: true,
  taxAccountNumber: '',
  mockFixedJson: '',
  mockVoucherDate: '',
  mockYear: undefined,
  mockPeriod: undefined,
  fenbeitongBaseUrl: '',
  fenbeitongAccessToken: '',
  fenbeitongReimbursementApplyState: undefined,
  fenbeitongReimbursementPageSize: 20
})

const mappingText = reactive({
  currencyNumbers: '{}',
  categoryAccountNumbers: '{}',
  creditDetailNumbers: '{}'
})

const traceQuery = reactive({
  sourceId: '',
  erpFid: ''
})

const previewResult = ref<FenbeitongVoucherPreviewVO>()
const saveResult = ref<FenbeitongVoucherSaveVO>()
const processResult = ref<FenbeitongVoucherProcessVO>()
const erpViewResult = ref<Record<string, any>>()
const businessInfoResult = ref<Record<string, any>>()

const configRules = reactive({
  accountBookNumber: [{ required: true, message: '账簿编码不能为空', trigger: 'blur' }],
  voucherGroupNumber: [{ required: true, message: '凭证字编码不能为空', trigger: 'blur' }],
  templateErpFid: [{ required: true, message: '凭证模板 FID 不能为空', trigger: 'blur' }],
  departmentDetailField: [{ required: true, message: '部门维度字段不能为空', trigger: 'blur' }],
  creditAccountNumber: [{ required: true, message: '贷方科目编码不能为空', trigger: 'blur' }],
  exchangeRateTypeNumber: [{ required: true, message: '汇率类型编码不能为空', trigger: 'blur' }],
  exchangeRate: [{ required: true, message: '汇率不能为空', trigger: 'change' }]
})

const parseObject = (value: string, label: string) => {
  const parsed = JSON.parse(value || '{}')
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error(`${label} 必须是 JSON 对象`)
  }
  return parsed
}

const syncMappingToForm = () => {
  configForm.currencyNumbers = parseObject(mappingText.currencyNumbers, '币种映射')
  configForm.categoryAccountNumbers = parseObject(mappingText.categoryAccountNumbers, '费用科目映射')
  configForm.creditDetailNumbers = parseObject(mappingText.creditDetailNumbers, '贷方维度')
}

const syncFormToMapping = () => {
  mappingText.currencyNumbers = formatJson(configForm.currencyNumbers || {})
  mappingText.categoryAccountNumbers = formatJson(configForm.categoryAccountNumbers || {})
  mappingText.creditDetailNumbers = formatJson(configForm.creditDetailNumbers || {})
}

const applyConfigToForm = (data: FenbeitongVoucherConfigVO) => {
  Object.assign(configForm, data, {
    fenbeitongAccessToken: data.fenbeitongAccessToken || ''
  })
  syncFormToMapping()
}

const buildConfiguredRequest = () => {
  if (!configForm.mockFixedJson || !configForm.mockVoucherDate || !configForm.mockYear || !configForm.mockPeriod) {
    throw new Error('模拟固定 JSON、凭证日期、年度和期间不能为空')
  }
  return {
    fixedJson: configForm.mockFixedJson,
    voucherDate: configForm.mockVoucherDate,
    year: configForm.mockYear,
    period: configForm.mockPeriod
  }
}

const handleLoadConfig = async () => {
  configLoading.value = true
  try {
    const data = await FenbeitongVoucherApi.getConfig()
    applyConfigToForm(data)
  } finally {
    configLoading.value = false
  }
}

const handleLoadMockTemplate = async () => {
  mockTemplateLoading.value = true
  try {
    const data = await FenbeitongVoucherApi.getMockTemplate()
    applyConfigToForm(data)
    message.success(mockTemplateLoadedMessage)
  } finally {
    mockTemplateLoading.value = false
  }
}

const handleSaveConfig = async () => {
  await configFormRef.value.validate()
  syncMappingToForm()
  configSaving.value = true
  try {
    await FenbeitongVoucherApi.saveConfig(configForm)
    message.success('保存成功')
  } finally {
    configSaving.value = false
  }
}

const handlePreviewMock = async () => {
  syncMappingToForm()
  previewLoading.value = true
  try {
    previewResult.value = await FenbeitongVoucherApi.previewConfiguredFixedJson(buildConfiguredRequest())
  } finally {
    previewLoading.value = false
  }
}

const handlePrepareMock = async () => {
  syncMappingToForm()
  prepareLoading.value = true
  try {
    processResult.value = await FenbeitongVoucherApi.prepareConfiguredFixedJson(buildConfiguredRequest())
    traceQuery.sourceId = processResult.value.sourceId
    message.success('本地准备记录已生成，未调用金蝶保存')
  } finally {
    prepareLoading.value = false
  }
}

const handleSaveMock = async () => {
  await ElMessageBox.confirm(
    '确认只保存草稿凭证到金蝶测试账套？后续审核由财务人工完成。',
    '保存确认',
    {
      confirmButtonText: '确认保存',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  syncMappingToForm()
  saveLoading.value = true
  try {
    saveResult.value = await FenbeitongVoucherApi.saveConfiguredFixedJson(buildConfiguredRequest())
    message.success('保存请求完成')
  } finally {
    saveLoading.value = false
  }
}

const handleQueryProcess = async () => {
  if (!traceQuery.sourceId) {
    message.warning('请输入来源 ID')
    return
  }
  processLoading.value = true
  try {
    processResult.value = await FenbeitongVoucherApi.getProcessBySourceId(traceQuery.sourceId)
  } finally {
    processLoading.value = false
  }
}

const handleViewErp = async () => {
  if (!traceQuery.erpFid) {
    message.warning('请输入 ERP FID')
    return
  }
  erpViewLoading.value = true
  try {
    erpViewResult.value = await FenbeitongVoucherApi.viewErpVoucher(traceQuery.erpFid)
  } finally {
    erpViewLoading.value = false
  }
}

const handleBusinessInfo = async () => {
  businessInfoLoading.value = true
  try {
    businessInfoResult.value = await FenbeitongVoucherApi.queryErpVoucherBusinessInfo()
  } finally {
    businessInfoLoading.value = false
  }
}

const formatJson = (value: any) => JSON.stringify(value ?? {}, null, 2)

const parseJsonText = (value: string) => {
  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

const processStatusText = (status?: number) => {
  if (status === 10) return '处理中'
  if (status === 15) return '已准备'
  if (status === 20) return '已保存'
  if (status === 30) return '失败'
  if (status === 40) return '结果未知'
  return status == null ? '-' : String(status)
}

onMounted(() => {
  handleLoadConfig()
})
</script>
