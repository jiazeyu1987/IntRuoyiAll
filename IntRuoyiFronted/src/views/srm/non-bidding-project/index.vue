<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="78px">
      <el-form-item label="项目编号" prop="projectNo">
        <el-input v-model="queryParams.projectNo" clearable class="!w-210px" placeholder="请输入项目编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="项目标题" prop="projectTitle">
        <el-input v-model="queryParams.projectTitle" clearable class="!w-220px" placeholder="请输入项目标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="projectStatus">
        <el-select v-model="queryParams.projectStatus" clearable class="!w-150px" placeholder="全部">
          <el-option v-for="item in srmNonBiddingProjectStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="success" plain @click="openContractableDialog" v-hasPermi="['srm:non-bidding-project:contract']">
          <Icon icon="ep:document-checked" class="mr-5px" /> 可建合同来源
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="项目编号" prop="projectNo" width="170" />
      <el-table-column label="项目标题" prop="projectTitle" min-width="190" />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.projectStatus)">{{ row.projectStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源计划" prop="sourcePlanNo" width="170" />
      <el-table-column label="询价模式" prop="quoteModeLabel" width="110" align="center" />
      <el-table-column label="预计金额" prop="expectedAmount" width="120" align="right" />
      <el-table-column label="供应商范围" width="110" align="center">
        <template #default="{ row }">{{ row.supplierScopes?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="报价数" width="86" align="center">
        <template #default="{ row }">{{ row.quotes?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="成交供应商" prop="dealSupplierName" min-width="160" />
      <el-table-column label="成交金额" prop="dealAmount" width="120" align="right" />
      <el-table-column label="报价截止" prop="quoteEndTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" width="280" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" :disabled="row.projectStatus !== 'DRAFT'" @click="openPublishDialog(row)" v-hasPermi="['srm:non-bidding-project:publish']">发布</el-button>
          <el-button link type="warning" :disabled="row.projectStatus !== 'PUBLISHED'" @click="openQuoteDialog(row)" v-hasPermi="['srm:non-bidding-project:quote']">报价</el-button>
          <el-button link type="success" :disabled="!canDeal(row)" @click="openDealDialog(row)" v-hasPermi="['srm:non-bidding-project:deal']">成交</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="publishVisible" title="发布非招标项目" width="720px">
    <el-form ref="publishFormRef" :model="publishForm" :rules="publishRules" label-width="110px">
      <el-form-item label="询价模式" prop="quoteMode">
        <el-radio-group v-model="publishForm.quoteMode">
          <el-radio-button v-for="item in srmNonBiddingQuoteModeOptions" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="报价开始" prop="quoteStartTime">
        <el-date-picker v-model="publishForm.quoteStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="报价截止" prop="quoteEndTime">
        <el-date-picker v-model="publishForm.quoteEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="发布附件" prop="attachmentUrl">
        <el-input v-model="publishForm.attachmentUrl" placeholder="请输入真实附件 URL" />
      </el-form-item>
      <el-form-item label="供应商范围" prop="supplierIdsText">
        <el-input
          v-model="publishForm.supplierIdsText"
          :disabled="publishForm.quoteMode === 'PUBLIC'"
          :placeholder="publishForm.quoteMode === 'PUBLIC' ? '公开询价无需填写供应商范围' : '请输入供应商 ID，多个用逗号分隔'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="publishVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitPublish">发布</el-button>
    </template>
  </Dialog>

  <Dialog v-model="quoteVisible" title="提交供应商报价" width="860px">
    <el-form ref="quoteFormRef" :model="quoteForm" :rules="quoteRules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="供应商ID" prop="supplierId">
            <el-input-number v-model="quoteForm.supplierId" :min="1" class="!w-1/1" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="报价金额" prop="quoteAmount">
            <el-input-number v-model="quoteForm.quoteAmount" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="报价附件" prop="attachmentUrl">
            <el-input v-model="quoteForm.attachmentUrl" placeholder="请输入报价附件 URL" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-table :data="quoteForm.lines" border size="small">
        <el-table-column label="项目行" prop="projectLineId" width="100" />
        <el-table-column label="物料编码" prop="materialCode" width="130" />
        <el-table-column label="物料名称" prop="materialName" min-width="160" />
        <el-table-column label="数量" prop="quantity" width="100" align="right" />
        <el-table-column label="单价" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.unitPrice" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" @change="syncLineAmount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="行金额" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.lineAmount" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" />
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <template #footer>
      <el-button @click="quoteVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitQuote">提交报价</el-button>
    </template>
  </Dialog>

  <Dialog v-model="dealVisible" title="确认成交" width="640px">
    <el-form ref="dealFormRef" :model="dealForm" :rules="dealRules" label-width="90px">
      <el-form-item label="成交报价" prop="quoteId">
        <el-select v-model="dealForm.quoteId" class="!w-1/1" placeholder="请选择成交报价">
          <el-option v-for="quote in currentProject?.quotes || []" :key="quote.id" :label="`${quote.supplierName} / ${quote.quoteAmount}`" :value="quote.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="成交说明" prop="dealRemark">
        <el-input v-model="dealForm.dealRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dealVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitDeal">确认成交</el-button>
    </template>
  </Dialog>

  <Dialog v-model="detailVisible" title="非招标项目详情" width="980px">
    <el-descriptions v-if="currentProject" :column="3" border>
      <el-descriptions-item label="项目编号">{{ currentProject.projectNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ currentProject.projectStatusLabel }}</el-descriptions-item>
      <el-descriptions-item label="来源计划">{{ currentProject.sourcePlanNo }}</el-descriptions-item>
      <el-descriptions-item label="询价模式">{{ currentProject.quoteModeLabel || '-' }}</el-descriptions-item>
      <el-descriptions-item label="报价时间">{{ currentProject.quoteStartTime || '-' }} 至 {{ currentProject.quoteEndTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="成交供应商">{{ currentProject.dealSupplierName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="合同ID">{{ currentProject.contractId || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-table class="mt-16px" :data="currentProject?.supplierScopes || []" border size="small">
      <el-table-column label="供应商ID" prop="supplierId" width="120" />
      <el-table-column label="供应商名称" prop="supplierName" />
    </el-table>
    <el-table class="mt-16px" :data="currentProject?.quotes || []" border size="small">
      <el-table-column label="报价ID" prop="id" width="90" />
      <el-table-column label="供应商" prop="supplierName" min-width="160" />
      <el-table-column label="报价金额" prop="quoteAmount" width="120" align="right" />
      <el-table-column label="报价人" prop="quotedName" width="110" />
      <el-table-column label="报价时间" prop="quotedTime" width="180" :formatter="dateFormatter" />
    </el-table>
    <el-card v-if="currentProject?.comparisonSummary" class="mt-16px" shadow="never">
      <template #header>比价汇总</template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="报价供应商数">{{ currentProject.comparisonSummary.supplierQuoteCount }}</el-descriptions-item>
        <el-descriptions-item label="最低报价">{{ currentProject.comparisonSummary.lowestQuoteAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="最低报价供应商">{{ currentProject.comparisonSummary.lowestQuoteSupplierName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="平均报价">{{ currentProject.comparisonSummary.averageQuoteAmount ?? '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table class="mt-12px" :data="currentProject.comparisonSummary.quoteRankings || []" border size="small">
        <el-table-column label="排名" prop="rankNo" width="80" align="center" />
        <el-table-column label="供应商" prop="supplierName" min-width="160" />
        <el-table-column label="报价金额" prop="quoteAmount" width="120" align="right" />
        <el-table-column label="报价时间" prop="quotedTime" width="180" :formatter="dateFormatter" />
      </el-table>
    </el-card>
    <el-card v-if="currentProject?.priceTrends?.length" class="mt-16px" shadow="never">
      <template #header>价格趋势</template>
      <div v-for="trend in currentProject.priceTrends" :key="trend.materialId" class="mb-16px last:mb-0">
        <div class="mb-8px text-14px font-600">{{ trend.materialName }}（{{ trend.materialCode }}）</div>
        <el-table :data="trend.points" border size="small">
          <el-table-column label="项目编号" prop="projectNo" width="160" />
          <el-table-column label="供应商" prop="supplierName" min-width="140" />
          <el-table-column label="单价" prop="unitPrice" width="120" align="right" />
          <el-table-column label="行金额" prop="lineAmount" width="120" align="right" />
          <el-table-column label="报价时间" prop="quotedTime" width="180" :formatter="dateFormatter" />
        </el-table>
      </div>
    </el-card>
  </Dialog>

  <Dialog v-model="contractableVisible" title="可建合同非招标来源" width="900px">
    <el-table v-loading="contractableLoading" :data="contractableList" border size="small">
      <el-table-column label="项目编号" prop="projectNo" width="170" />
      <el-table-column label="项目标题" prop="projectTitle" min-width="180" />
      <el-table-column label="成交供应商" prop="dealSupplierName" min-width="160" />
      <el-table-column label="成交金额" prop="dealAmount" width="120" align="right" />
      <el-table-column label="成交时间" prop="dealTime" width="180" :formatter="dateFormatter" />
    </el-table>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import {
  SrmNonBiddingProjectApi,
  srmNonBiddingQuoteModeOptions,
  srmNonBiddingProjectStatusOptions,
  type SrmNonBiddingProjectVO
} from '@/api/srm/non-bidding-project'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'SrmNonBiddingProject' })

const message = useMessage()
const loading = ref(false)
const actionLoading = ref(false)
const list = ref<SrmNonBiddingProjectVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  projectNo: undefined as string | undefined,
  projectTitle: undefined as string | undefined,
  projectStatus: undefined as string | undefined
})

const publishVisible = ref(false)
const publishFormRef = ref<FormInstance>()
const publishForm = reactive({
  projectId: undefined as number | undefined,
  quoteMode: 'INVITE',
  quoteStartTime: '',
  quoteEndTime: '',
  attachmentUrl: '',
  supplierIdsText: ''
})
const publishRules = reactive<FormRules>({
  quoteMode: [{ required: true, message: '请选择询价模式', trigger: 'change' }],
  quoteStartTime: [{ required: true, message: '请选择报价开始时间', trigger: 'change' }],
  quoteEndTime: [{ required: true, message: '请选择报价截止时间', trigger: 'change' }],
  attachmentUrl: [{ required: true, message: '请输入发布附件 URL', trigger: 'blur' }],
  supplierIdsText: [{
    validator: (_rule, value, callback) => {
      if (publishForm.quoteMode === 'PUBLIC') {
        callback()
        return
      }
      if (String(value || '').trim()) {
        callback()
        return
      }
      callback(new Error('邀请询价必须填写供应商范围'))
    },
    trigger: 'blur'
  }]
})

const quoteVisible = ref(false)
const quoteFormRef = ref<FormInstance>()
const quoteForm = reactive({
  projectId: undefined as number | undefined,
  supplierId: undefined as number | undefined,
  quoteAmount: 0,
  attachmentUrl: '',
  lines: [] as Array<{
    projectLineId: number
    materialCode?: string
    materialName?: string
    quantity?: number
    unitPrice: number
    lineAmount: number
  }>
})
const quoteRules = reactive<FormRules>({
  supplierId: [{ required: true, message: '请输入供应商 ID', trigger: 'change' }],
  quoteAmount: [{ required: true, message: '请输入报价金额', trigger: 'change' }],
  attachmentUrl: [{ required: true, message: '请输入报价附件 URL', trigger: 'blur' }]
})

const dealVisible = ref(false)
const dealFormRef = ref<FormInstance>()
const dealForm = reactive({ projectId: undefined as number | undefined, quoteId: undefined as number | undefined, dealRemark: '' })
const dealRules = reactive<FormRules>({
  quoteId: [{ required: true, message: '请选择成交报价', trigger: 'change' }],
  dealRemark: [{ required: true, message: '请输入成交说明', trigger: 'blur' }]
})

const detailVisible = ref(false)
const currentProject = ref<SrmNonBiddingProjectVO>()
const contractableVisible = ref(false)
const contractableLoading = ref(false)
const contractableList = ref<SrmNonBiddingProjectVO[]>([])

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'DEAL_CONFIRMED' || status === 'CONTRACT_CREATED') return 'success'
  if (status === 'PUBLISHED') return 'warning'
  return 'info'
}

const canDeal = (row: SrmNonBiddingProjectVO) => row.projectStatus === 'PUBLISHED' && (row.quotes?.length || 0) > 0

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmNonBiddingProjectApi.getProjectPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '非招标项目列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
}

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery(true)
}

const openPublishDialog = (row: SrmNonBiddingProjectVO) => {
  publishForm.projectId = row.id
  publishForm.quoteMode = row.quoteMode || 'INVITE'
  publishForm.quoteStartTime = ''
  publishForm.quoteEndTime = ''
  publishForm.attachmentUrl = 'http://127.0.0.1:9000/yudao/srm/non-bidding/publish.pdf'
  publishForm.supplierIdsText = row.quoteMode === 'PUBLIC' ? '' : '103'
  publishVisible.value = true
}

const parseSupplierIds = (value: string) =>
  value
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)

const parseDateTimeMillis = (value: string, label: string) => {
  const millis = new Date(value.replace(' ', 'T')).getTime()
  if (!Number.isFinite(millis)) {
    throw new Error(`${label}格式无效，请重新选择。`)
  }
  return millis
}

const submitPublish = async () => {
  await publishFormRef.value?.validate()
  if (!publishForm.projectId) return
  actionLoading.value = true
  try {
    const quoteStartTime = parseDateTimeMillis(publishForm.quoteStartTime, '报价开始时间')
    const quoteEndTime = parseDateTimeMillis(publishForm.quoteEndTime, '报价截止时间')
    if (quoteEndTime <= quoteStartTime) {
      throw new Error('报价截止时间必须晚于报价开始时间。')
    }
    await SrmNonBiddingProjectApi.publishProject({
      projectId: publishForm.projectId,
      quoteMode: publishForm.quoteMode,
      quoteStartTime,
      quoteEndTime,
      attachmentUrl: publishForm.attachmentUrl,
      supplierIds: publishForm.quoteMode === 'PUBLIC' ? [] : parseSupplierIds(publishForm.supplierIdsText)
    })
    publishVisible.value = false
    message.success('非招标项目已发布')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '非招标项目发布失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openQuoteDialog = async (row: SrmNonBiddingProjectVO) => {
  currentProject.value = await SrmNonBiddingProjectApi.getProject(row.id)
  quoteForm.projectId = row.id
  quoteForm.supplierId =
    currentProject.value.quoteMode === 'PUBLIC'
      ? undefined
      : currentProject.value.supplierScopes?.[0]?.supplierId || undefined
  quoteForm.attachmentUrl = 'http://127.0.0.1:9000/yudao/srm/non-bidding/quote.pdf'
  quoteForm.lines = currentProject.value.lines.map((line) => ({
    projectLineId: line.id,
    materialCode: line.materialCode,
    materialName: line.materialName,
    quantity: line.quantity,
    unitPrice: 1,
    lineAmount: line.quantity || 1
  }))
  quoteForm.quoteAmount = quoteForm.lines.reduce((sum, line) => sum + Number(line.lineAmount || 0), 0)
  quoteVisible.value = true
}

const syncLineAmount = (row: { quantity?: number; unitPrice: number; lineAmount: number }) => {
  row.lineAmount = Number((Number(row.quantity || 1) * Number(row.unitPrice || 0)).toFixed(2))
  quoteForm.quoteAmount = quoteForm.lines.reduce((sum, line) => sum + Number(line.lineAmount || 0), 0)
}

const submitQuote = async () => {
  await quoteFormRef.value?.validate()
  if (!quoteForm.projectId || !quoteForm.supplierId) return
  actionLoading.value = true
  try {
    await SrmNonBiddingProjectApi.submitQuote({
      projectId: quoteForm.projectId,
      supplierId: quoteForm.supplierId,
      quoteAmount: quoteForm.quoteAmount,
      attachmentUrl: quoteForm.attachmentUrl,
      lines: quoteForm.lines.map((line) => ({
        projectLineId: line.projectLineId,
        unitPrice: line.unitPrice,
        lineAmount: line.lineAmount
      }))
    })
    quoteVisible.value = false
    message.success('供应商报价已提交')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商报价提交失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openDealDialog = async (row: SrmNonBiddingProjectVO) => {
  currentProject.value = await SrmNonBiddingProjectApi.getProject(row.id)
  dealForm.projectId = row.id
  dealForm.quoteId = currentProject.value.quotes?.[0]?.id
  dealForm.dealRemark = '确认成交'
  dealVisible.value = true
}

const submitDeal = async () => {
  await dealFormRef.value?.validate()
  if (!dealForm.projectId || !dealForm.quoteId) return
  actionLoading.value = true
  try {
    await SrmNonBiddingProjectApi.confirmDeal({
      projectId: dealForm.projectId,
      quoteId: dealForm.quoteId,
      dealRemark: dealForm.dealRemark
    })
    dealVisible.value = false
    message.success('非招标项目已确认成交')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '非招标成交确认失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openDetail = async (row: SrmNonBiddingProjectVO) => {
  try {
    currentProject.value = await SrmNonBiddingProjectApi.getProject(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '非招标项目详情加载失败。'))
    throw error
  }
}

const openContractableDialog = async () => {
  contractableVisible.value = true
  contractableLoading.value = true
  try {
    const data = await SrmNonBiddingProjectApi.getContractableProjectPage({ pageNo: 1, pageSize: 20 })
    contractableList.value = data.list || []
  } catch (error) {
    message.error(resolveErrorMessage(error, '可建合同来源加载失败。'))
    throw error
  } finally {
    contractableLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
