<template>
  <ContentWrap>
    <div class="edhr-release-page">
      <el-form :inline="true" :model="queryParams" class="edhr-release-page__toolbar">
        <div class="edhr-release-page__title">电子批记录放行追溯</div>
        <el-form-item label="批次执行">
          <el-input v-model="queryParams.batchExecutionCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="工单号">
          <el-input v-model="queryParams.workOrderCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="queryParams.batchCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="放行状态">
          <el-select v-model="queryParams.releaseStatus" clearable class="!w-170px">
            <el-option label="待预检" value="PRECHECK_REQUIRED" />
            <el-option label="预检失败" value="PRECHECK_FAILED" />
            <el-option label="预检通过" value="PRECHECK_PASSED" />
            <el-option label="待审批" value="PENDING_APPROVAL" />
            <el-option label="已放行" value="RELEASED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已撤回" value="WITHDRAWN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" show-icon />

      <div class="edhr-release-page__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无放行追溯记录"
        >
          <el-table-column label="放行对象" min-width="250" :show-overflow-tooltip="false">
            <template #default="{ row }">
              <div class="edhr-release-page__strong release-text-summary">{{ row.batchExecutionCode || '未记录批次执行号' }}</div>
              <div class="edhr-release-page__muted release-text-summary">工单：{{ row.workOrderCode || '未记录' }}</div>
              <details class="release-technical"><summary>完整放行对象</summary>
                <p>批次执行号：{{ row.batchExecutionCode || '未记录' }}</p>
                <p>工单号：{{ row.workOrderCode || '未记录' }}</p><p>批次号：{{ row.batchCode || '未记录' }}</p>
              </details>
            </template>
          </el-table-column>
          <el-table-column label="产品与路线" min-width="230" :show-overflow-tooltip="false">
            <template #default="{ row }">
              <div class="edhr-release-page__strong release-text-summary">{{ row.productName || '未记录产品名称' }}</div>
              <div class="edhr-release-page__muted release-text-summary">产品编码：{{ row.productCode || '未记录' }}</div>
              <details class="release-technical"><summary>完整产品与路线</summary>
                <p>产品名称：{{ row.productName || '未记录' }}</p><p>产品编码：{{ row.productCode || '未记录' }}</p>
                <p>路线名称：{{ row.routeName || '未记录' }}</p><p>路线编码：{{ row.routeCode || '未记录' }}</p>
              </details>
            </template>
          </el-table-column>
          <el-table-column label="放行状态" width="145">
            <template #default="{ row }">
              <el-tag :type="resolveReleaseTagType(row.releaseStatus)">
                {{ resolveReleaseStatusLabel(row.releaseStatus) }}
              </el-tag>
              <div class="edhr-release-page__muted">{{ row.precheckSummary || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="检查摘要" width="170">
            <template #default="{ row }">
              <div class="edhr-release-page__metric">
                阻塞 <strong>{{ row.blockingCheckCount || 0 }}</strong>
              </div>
              <div class="edhr-release-page__metric">
                失败 <strong>{{ row.failedCheckCount || 0 }}</strong>
              </div>
              <div class="edhr-release-page__muted">最后：{{ formatDateTime(row.lastPrecheckAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="质量门禁" min-width="260">
            <template #default="{ row }">
              <div class="edhr-release-page__gate-row">
                <el-tag :type="resolveReleaseCheckResultTagType(row.dhrStatus)">DHR {{ resolveReleaseCheckResultLabel(row.dhrStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.inspectionStatus)">检验 {{ resolveReleaseCheckResultLabel(row.inspectionStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.inventoryStatus)">库存 {{ resolveReleaseCheckResultLabel(row.inventoryStatus) }}</el-tag>
              </div>
              <div class="edhr-release-page__gate-row">
                <el-tag :type="resolveReleaseCheckResultTagType(row.deviationStatus)">偏差 {{ resolveReleaseCheckResultLabel(row.deviationStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.reworkStatus)">返工 {{ resolveReleaseCheckResultLabel(row.reworkStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.scrapStatus)">报废 {{ resolveReleaseCheckResultLabel(row.scrapStatus) }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="事务时间" min-width="180">
            <template #default="{ row }">
              <div class="edhr-release-page__muted">提交：{{ formatDateTime(row.submittedAt) }}</div>
              <div class="edhr-release-page__muted">批准：{{ formatDateTime(row.approvedAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="追溯" width="220" fixed="right">
            <template #default="{ row }">
              <div class="edhr-release-page__actions">
                <MarketReleaseNegativeDecision :release-transaction-id="row.releaseTransactionId" :pending="row.releaseStatus === 'PENDING_APPROVAL'" @changed="getList" />
                <el-button
                  v-hasPermi="['mes:pro-edhr-nonconformance-review:create']"
                  link
                  type="danger"
                  :disabled="!row.activeOrderId"
                  @click="openNonconformanceReviewEntry(row)"
                >
                  不合格审查
                </el-button>
                <el-button v-if="row.releaseStatus === 'RELEASED'" link type="primary" @click="openSignedDossier(row)">已签资料</el-button>
                <el-button link type="primary" :disabled="!row.releaseTransactionId" @click="openCheckItems(row)">
                  检查项
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
    </div>

    <el-drawer v-model="signedDossierVisible" title="上市放行时签署的资料" size="82%">
      <div v-loading="signedDossierLoading">
        <el-alert v-if="signedDossierError" :title="signedDossierError" type="error" :closable="false" />
        <template v-if="signedDossier">
          <p>资料修订：{{ signedDossier.revision }}</p>
          <details class="release-technical"><summary>清单校验信息</summary><p>清单校验码：{{ signedDossier.manifestHash }}</p></details>
          <el-table :data="signedDossier.files" empty-text="本次签署的资料清单为空">
            <el-table-column label="文件名" prop="fileName" />
            <el-table-column label="资料类别">
              <template #default="{ row }">{{ dossierCategoryLabels[row.categoryKey] || `未识别类别（${row.categoryKey}）` }}</template>
            </el-table-column>
            <el-table-column label="文件大小">
              <template #default="{ row }">{{ dossierFileSizeLabel(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="技术详情" min-width="170">
              <template #default="{ row }"><details class="release-technical"><summary>文件编号与校验码</summary>
                <p>文件编号：{{ row.fileId }}</p><p>文件大小（字节）：{{ row.fileSize }}</p><p>文件校验码：{{ row.sha256 }}</p>
              </details></template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>
    <el-drawer v-model="drawerVisible" title="电子批记录放行检查项" size="82%" class="edhr-release-page__drawer">
      <el-alert
        v-if="currentRow"
        :title="`${currentRow.batchExecutionCode} / ${currentRow.precheckSummary || '检查项'}`"
        type="info"
        :closable="false"
        show-icon
        class="edhr-release-page__drawer-alert"
      />
      <el-table
        v-loading="checkItemLoading"
        :data="checkItems"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无检查项"
      >
        <el-table-column label="检查项" min-width="210">
          <template #default="{ row }">
            <div class="edhr-release-page__strong">{{ resolveReleaseCheckCodeLabel(row.checkCode) }}</div>
            <div class="edhr-release-page__muted">
              分类：{{ resolveReleaseCheckCategoryLabel(row.checkCategory) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="115">
          <template #default="{ row }">
            <el-tag :type="resolveReleaseCheckResultTagType(row.checkResult)">
              {{ resolveReleaseCheckResultLabel(row.checkResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="责任模块" width="110" prop="responsibilityModule" />
        <el-table-column label="源对象" min-width="190">
          <template #default="{ row }">
            <div>{{ row.sourceObjectCode || '--' }}</div>
            <div class="edhr-release-page__muted">{{ resolveReleaseCheckSourceObjectTypeLabel(row.sourceObjectType) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="260" prop="failureReason" />
        <el-table-column label="下一步动作" min-width="260" prop="remediationSuggestion" />
      </el-table>
      <Pagination
        :total="checkItemTotal"
        v-model:page="checkItemQuery.pageNo"
        v-model:limit="checkItemQuery.pageSize"
        @pagination="getCheckItems"
      />
    </el-drawer>

    

  </ContentWrap>
</template>

<script setup lang="ts">
import MarketReleaseNegativeDecision from '../production-release/components/MarketReleaseNegativeDecision.vue'
import {
  getEdhrRelease,
  getEdhrReleaseCheckItemPage,
  
  getEdhrReleasePage,
  type EdhrReleaseCheckItemPageReqVO,
  type EdhrReleaseCheckItemVO,
  
  
  type EdhrReleaseRowVO,
  type EdhrReleaseStatus
} from '@/api/mes/pro/edhr/release'
import {
  resolveReleaseCheckCategoryLabel,
  resolveReleaseCheckCodeLabel,
  resolveReleaseCheckResultLabel,
  resolveReleaseCheckResultTagType,
  resolveReleaseCheckSourceObjectTypeLabel,
  
  resolveReleaseStatusLabel,
  resolveReleaseTagType
} from '@/views/mes/pro/edhr/shared/releaseCheckPresentation'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import { dossierFileSizeLabel } from './releaseDisplay'
import { parseExactIntegerJson } from '@/utils/exactIntegerJson'

defineOptions({ name: 'MesProEdhrReleasePage' })

const dossierCategoryLabels: Record<string, string> = {
  INCOMING_INSPECTION_FILE: '来料检文件', STERILIZATION_FILE: '灭菌文件',
  FINISHED_PRODUCT_FILE: '成品检文件', OTHER_FILE: '其他文件'
}
interface SignedDossier {
  revision: string
  manifestHash: string
  files: Array<{ fileName: string; categoryKey: string; fileId: string | number; fileSize: string | number; sha256: string }>
}
const signedDossier = ref<SignedDossier>()
const signedDossierVisible = ref(false)
const signedDossierLoading = ref(false)
const signedDossierError = ref('')
let signedDossierRequest = 0
const openSignedDossier = async (row: EdhrReleaseRowVO) => {
  const request = ++signedDossierRequest
  signedDossier.value = undefined
  signedDossierError.value = ''
  signedDossierVisible.value = true
  signedDossierLoading.value = true
  try {
    if (!row.releaseTransactionId) throw new Error('放行记录缺少正式放行事务编号')
    const detail = await getEdhrRelease(row.releaseTransactionId)
    if (request !== signedDossierRequest) return
    if (!detail.signedDossierManifestJson) throw new Error('该历史放行记录未签署独立资料清单')
    signedDossier.value = parseExactIntegerJson(detail.signedDossierManifestJson)
  } catch (error) {
    if (request === signedDossierRequest) signedDossierError.value = error instanceof Error ? error.message : '读取已签资料失败'
  } finally {
    if (request === signedDossierRequest) signedDossierLoading.value = false
  }
}

const message = useMessage()
const router = useRouter()

const loading = ref(false)
const checkItemLoading = ref(false)

const loadError = ref('')
const actionError = ref('')
const list = ref<EdhrReleaseRowVO[]>([])
const total = ref(0)
const drawerVisible = ref(false)

const currentRow = ref<EdhrReleaseRowVO>()
const checkItems = ref<EdhrReleaseCheckItemVO[]>([])

const checkItemTotal = ref(0)


const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  batchExecutionCode: '',
  workOrderCode: '',
  batchCode: '',
  productCode: '',
  releaseStatus: '' as EdhrReleaseStatus | ''
})

const checkItemQuery = reactive<EdhrReleaseCheckItemPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  releaseTransactionId: '',
  itemStatus: 'OPEN' as const,
  checkResult: ''
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
  batchExecutionCode: queryParams.batchExecutionCode.trim() || undefined,
  workOrderCode: queryParams.workOrderCode.trim() || undefined,
  batchCode: queryParams.batchCode.trim() || undefined,
  productCode: queryParams.productCode.trim() || undefined,
  releaseStatus: queryParams.releaseStatus || undefined
})

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrReleasePage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '电子批记录放行追溯列表加载失败，请联系管理员。')
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
  queryParams.batchExecutionCode = ''
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  queryParams.productCode = ''
  queryParams.releaseStatus = ''
  getList()
}

const openCheckItems = async (row: EdhrReleaseRowVO) => {
  if (!row.releaseTransactionId) {
    actionError.value = '该放行记录尚未生成放行事务，暂无检查项。'
    return
  }
  currentRow.value = row
  drawerVisible.value = true
  checkItemQuery.pageNo = 1
  checkItemQuery.releaseTransactionId = row.releaseTransactionId
  await getCheckItems()
}

const openNonconformanceReviewEntry = (row: EdhrReleaseRowVO) => {
  if (!row.activeOrderId) {
    actionError.value = '缺少正式活跃订单ID，无法发起不合格审查。'
    return
  }
  router.push({
    name: 'MesProFeedbackEdhrNonconformanceReview',
    query: {
      activeOrderId: String(row.activeOrderId),
      autoCreate: '1'
    }
  })
}

const getCheckItems = async () => {
  if (!checkItemQuery.releaseTransactionId) {
    checkItems.value = []
    checkItemTotal.value = 0
    return
  }
  checkItemLoading.value = true
  actionError.value = ''
  try {
    const data = await getEdhrReleaseCheckItemPage(checkItemQuery)
    checkItems.value = data.list || []
    checkItemTotal.value = data.total || 0
  } catch (error) {
    checkItems.value = []
    checkItemTotal.value = 0
    actionError.value = resolveErrorMessage(error, '电子批记录放行检查项加载失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  } finally {
    checkItemLoading.value = false
  }
}





const formatDateTime = (value?: string | number) => {
  return formatEdhrDateTime(value)
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.release-text-summary { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow-wrap: anywhere; white-space: normal; }
.release-technical { overflow-wrap: anywhere; white-space: normal; }
.release-technical summary { cursor: pointer; color: var(--el-text-color-secondary); }
.release-technical p { margin: 6px 0; }
.edhr-release-page {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.edhr-release-page__toolbar,
.edhr-release-page__table {
  background: #fff;
  border: 1px solid #dbe3ef;
}

.edhr-release-page__toolbar {
  padding: 16px 16px 0;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.edhr-release-page__title {
  width: 100%;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #172033;
}

.edhr-release-page__table {
  padding: 16px;
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.edhr-release-page__strong {
  font-weight: 600;
  line-height: 1.5;
  color: #172033;
}

.edhr-release-page__muted {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #4b5563;
}

.edhr-release-page__metric {
  font-size: 13px;
  line-height: 1.6;
  color: #263247;
}

.edhr-release-page__metric strong {
  color: #172033;
  font-variant-numeric: tabular-nums;
}

.edhr-release-page__gate-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.edhr-release-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.edhr-release-page__drawer :deep(.el-drawer__body) {
  padding-top: 8px;
}

.edhr-release-page__drawer-alert,
.edhr-release-page__dialog-alert {
  margin-bottom: 12px;
}

.edhr-release-page__dialog-form {
  padding-top: 4px;
}
</style>
