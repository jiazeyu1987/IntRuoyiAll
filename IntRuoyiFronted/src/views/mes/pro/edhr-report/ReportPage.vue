<template>
  <ContentWrap>
    <div class="edhr-report">
      <section class="edhr-report__toolbar">
        <div class="edhr-report__title-row">
          <div>
            <h2>统一追溯报表</h2>
            <div class="edhr-report__subtitle">12 类标准报表</div>
          </div>
          <el-tag type="success" effect="plain">FIRST_SLICE_READY</el-tag>
        </div>

        <el-form :inline="true" :model="catalogQueryParams" class="edhr-report__form" @submit.prevent>
          <el-form-item label="报表名称">
            <el-input
              v-model="catalogQueryParams.reportName"
              clearable
              class="!w-180px"
              @keyup.enter="handleCatalogQuery"
            />
          </el-form-item>
          <el-form-item label="报表分类">
            <el-input
              v-model="catalogQueryParams.reportCategory"
              clearable
              class="!w-150px"
              @keyup.enter="handleCatalogQuery"
            />
          </el-form-item>
          <el-form-item label="目录状态">
            <el-select v-model="catalogQueryParams.status" clearable class="!w-140px">
              <el-option label="启用" value="ACTIVE" />
              <el-option label="停用" value="INACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="handleCatalogQuery"
              v-hasPermi="['mes:pro-edhr-report:query']"
            >
              <Icon icon="ep:search" class="mr-5px" />
              查询
            </el-button>
            <el-button @click="resetCatalogQuery">
              <Icon icon="ep:refresh" class="mr-5px" />
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </section>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <section class="edhr-report__catalog">
        <div class="edhr-report__section-title">
          <span>标准报表目录</span>
          <span class="edhr-report__muted">数据来源 / 权限 / 导出策略</span>
        </div>
        <el-table
          v-loading="catalogLoading"
          :data="catalogList"
          stripe
          highlight-current-row
          :show-overflow-tooltip="true"
          empty-text="暂无标准报表目录"
          @row-click="selectCatalog"
        >
          <el-table-column label="报表" min-width="220">
            <template #default="{ row }">
              <div class="edhr-report__strong">{{ row.reportName }}</div>
              <div class="edhr-report__muted">{{ row.reportCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="分类" prop="reportCategory" width="110" />
          <el-table-column label="业务目的" prop="businessPurpose" min-width="180" />
          <el-table-column label="数据来源" prop="dataSourceSummary" min-width="210" />
          <el-table-column label="权限策略" prop="permissionPolicy" min-width="170" />
          <el-table-column label="导出策略" prop="exportPolicy" min-width="170" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ row.status || '--' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="验收" prop="acceptanceStatus" min-width="160" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click.stop="selectCatalog(row)"
                v-hasPermi="['mes:pro-edhr-report:query']"
              >
                <Icon icon="ep:view" class="mr-4px" />
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="catalogTotal"
          v-model:page="catalogQueryParams.pageNo"
          v-model:limit="catalogQueryParams.pageSize"
          @pagination="getCatalogList"
        />
      </section>

      <section class="edhr-report__detail-grid">
        <div class="edhr-report__definition">
          <div class="edhr-report__section-title">
            <span>报表定义</span>
            <span class="edhr-report__muted">{{ selectedCatalog?.reportName || '未选择目录' }}</span>
          </div>
          <el-alert
            v-if="definitionError"
            :title="definitionError"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-form :inline="true" class="edhr-report__definition-picker">
            <el-form-item label="定义版本">
              <el-select
                v-model="selectedDefinitionId"
                class="!w-260px"
                placeholder="请选择已发布定义"
                @change="handleDefinitionChange"
              >
                <el-option
                  v-for="definition in definitionList"
                  :key="definition.id"
                  :label="`${definition.reportName} / ${definition.caliberVersion || '--'}`"
                  :value="definition.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :disabled="!selectedDefinition"
                @click="handleRunQuery"
                v-hasPermi="['mes:pro-edhr-report:query']"
              >
                <Icon icon="ep:operation" class="mr-5px" />
                只读查询
              </el-button>
              <el-button
                type="success"
                :disabled="!queryResult"
                @click="handleRecordExportAudit"
                v-hasPermi="['mes:pro-edhr-report:export']"
              >
                <Icon icon="ep:tickets" class="mr-5px" />
                导出审计
              </el-button>
            </el-form-item>
          </el-form>

          <el-descriptions v-if="selectedDefinition" :column="2" border>
            <el-descriptions-item label="报表编码">
              {{ selectedDefinition.reportCode }}
            </el-descriptions-item>
            <el-descriptions-item label="口径版本">
              {{ selectedDefinition.caliberVersion || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="数据来源">
              {{ selectedDefinition.datasetCode || '--' }} / {{ selectedDefinition.datasetVersion || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="数据源状态">
              <el-tag :type="selectedDefinition.dataSourceStatus === 'READY' ? 'success' : 'danger'">
                {{ selectedDefinition.dataSourceStatus || '--' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="权限摘要" :span="2">
              <pre>{{ selectedDefinition.permissionSummaryJson || '--' }}</pre>
            </el-descriptions-item>
            <el-descriptions-item label="字段口径" :span="2">
              <pre>{{ selectedDefinition.fieldCaliberJson || '--' }}</pre>
            </el-descriptions-item>
            <el-descriptions-item label="筛选模型" :span="2">
              <pre>{{ selectedDefinition.filterSchemaJson || '--' }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="edhr-report__query">
          <div class="edhr-report__section-title">
            <span>只读查询证据</span>
            <span class="edhr-report__muted">{{ queryResult?.reportName || '未执行查询' }}</span>
          </div>
            <el-descriptions v-if="queryResult" :column="2" border>
              <el-descriptions-item label="口径版本">{{ queryResult.caliberVersion || '--' }}</el-descriptions-item>
              <el-descriptions-item label="数据更新时间">
                {{ formatEdhrDateTime(queryResult.dataUpdatedAt) }}
              </el-descriptions-item>
            <el-descriptions-item label="数据来源" :span="2">
              {{ queryResult.dataSourceSummary || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="筛选快照" :span="2">
              <pre>{{ queryResult.filterSnapshotJson || '--' }}</pre>
            </el-descriptions-item>
            <el-descriptions-item label="权限摘要" :span="2">
              <pre>{{ queryResult.permissionSummaryJson || '--' }}</pre>
            </el-descriptions-item>
          </el-descriptions>
          <el-table
            v-loading="queryLoading"
            :data="queryRows"
            stripe
            :show-overflow-tooltip="true"
            empty-text="请选择已发布报表后执行查询"
          >
            <el-table-column
              v-for="columnKey in queryColumnKeys"
              :key="columnKey"
              :label="columnKey"
              :prop="columnKey"
              min-width="150"
            >
              <template #default="{ row }">{{ formatCell(row[columnKey]) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section class="edhr-report__audit">
        <div class="edhr-report__section-title">
          <span>导出审计</span>
          <span class="edhr-report__muted">{{ auditQueryParams.reportCode || '未选择报表' }}</span>
        </div>
        <el-table
          v-loading="auditLoading"
          :data="auditList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无导出审计记录"
        >
          <el-table-column label="审计ID" prop="id" width="100" />
          <el-table-column label="报表" min-width="220">
            <template #default="{ row }">
              <div class="edhr-report__strong">{{ row.reportName || row.reportCode }}</div>
              <div class="edhr-report__muted">{{ row.caliberVersion || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="数据范围" prop="dataRangeSummary" min-width="180" />
          <el-table-column label="结果" width="110">
            <template #default="{ row }">
              <el-tag :type="row.resultStatus === 'RECORDED' ? 'success' : 'danger'">
                {{ row.resultStatus || '--' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作人" min-width="140">
            <template #default="{ row }">
              {{ row.operatorUsername || row.operatorUserId || '--' }}
            </template>
          </el-table-column>
          <el-table-column label="发生时间" prop="occurredAt" width="180" :formatter="edhrDateTimeFormatter" />
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="edhr-report__audit-evidence">
                <div>
                  <div class="edhr-report__evidence-title">筛选快照</div>
                  <pre>{{ row.filterSnapshotJson || '--' }}</pre>
                </div>
                <div>
                  <div class="edhr-report__evidence-title">权限摘要</div>
                  <pre>{{ row.permissionSummaryJson || '--' }}</pre>
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="auditTotal"
          v-model:page="auditQueryParams.pageNo"
          v-model:limit="auditQueryParams.pageSize"
          @pagination="getAuditList"
        />
      </section>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  getEdhrReportCatalogPage,
  getEdhrReportDefinitionPage,
  getEdhrReportExportAuditPage,
  recordEdhrReportExportAudit,
  runEdhrReportQuery,
  type EdhrReportCatalogPageReqVO,
  type EdhrReportCatalogRespVO,
  type EdhrReportDefinitionRespVO,
  type EdhrReportExportAuditPageReqVO,
  type EdhrReportExportAuditRespVO,
  type EdhrReportQueryRespVO
} from '@/api/mes/pro/edhr/report'
import { edhrDateTimeFormatter, formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrReport' })

const catalogLoading = ref(false)
const definitionLoading = ref(false)
const queryLoading = ref(false)
const auditLoading = ref(false)
const loadError = ref('')
const definitionError = ref('')

const catalogList = ref<EdhrReportCatalogRespVO[]>([])
const catalogTotal = ref(0)
const definitionList = ref<EdhrReportDefinitionRespVO[]>([])
const selectedCatalog = ref<EdhrReportCatalogRespVO>()
const selectedDefinitionId = ref<number>()
const queryResult = ref<EdhrReportQueryRespVO>()
const auditList = ref<EdhrReportExportAuditRespVO[]>([])
const auditTotal = ref(0)

const catalogQueryParams = reactive<EdhrReportCatalogPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  reportName: '',
  reportCategory: '',
  status: 'ACTIVE'
})

const auditQueryParams = reactive<EdhrReportExportAuditPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  reportCode: undefined
})

const selectedDefinition = computed(() =>
  definitionList.value.find((definition) => definition.id === selectedDefinitionId.value)
)

const queryRows = computed(() => queryResult.value?.rows || [])
const queryColumnKeys = computed(() => {
  const firstRow = queryRows.value[0]
  return firstRow ? Object.keys(firstRow) : []
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

function assertPageResult<T>(data: unknown, label: string): PageResult<T[]> {
  const page = data as { list?: unknown; total?: unknown }
  if (!page || !Array.isArray(page.list) || typeof page.total !== 'number') {
    throw new Error(`${label}响应结构异常，缺少 list/total。`)
  }
  return page as PageResult<T[]>
}

const assertQueryResult = (data: unknown): EdhrReportQueryRespVO => {
  const result = data as EdhrReportQueryRespVO
  if (!result || !Array.isArray(result.rows)) {
    throw new Error('报表查询响应结构异常，缺少 rows。')
  }
  return result
}

const formatCell = (value: unknown) => {
  if (value === null || value === undefined || value === '') return '--'
  return String(value)
}

const buildCatalogQuery = (): EdhrReportCatalogPageReqVO => ({
  pageNo: catalogQueryParams.pageNo,
  pageSize: catalogQueryParams.pageSize,
  reportName: catalogQueryParams.reportName?.trim() || undefined,
  reportCategory: catalogQueryParams.reportCategory?.trim() || undefined,
  status: catalogQueryParams.status || undefined
})

const buildFilterSnapshotJson = () => {
  const definition = selectedDefinition.value
  if (!definition) throw new Error('请选择已发布报表后执行查询。')
  return JSON.stringify({
    reportCode: definition.reportCode,
    reportDefinitionId: definition.id,
    caliberVersion: definition.caliberVersion,
    catalogFilters: buildCatalogQuery(),
    requestedAt: new Date().toISOString()
  })
}

const getCatalogList = async () => {
  catalogLoading.value = true
  loadError.value = ''
  try {
    const page = assertPageResult<EdhrReportCatalogRespVO>(
      await getEdhrReportCatalogPage(buildCatalogQuery()),
      '报表目录'
    )
    catalogList.value = page.list
    catalogTotal.value = page.total
    if (!selectedCatalog.value && page.list.length > 0) {
      await selectCatalog(page.list[0])
    }
  } catch (error) {
    catalogList.value = []
    catalogTotal.value = 0
    loadError.value = resolveErrorMessage(error, '报表目录加载失败，请检查接口和权限。')
  } finally {
    catalogLoading.value = false
  }
}

const getDefinitionList = async (reportCode: string) => {
  definitionLoading.value = true
  definitionError.value = ''
  try {
    const page = assertPageResult<EdhrReportDefinitionRespVO>(
      await getEdhrReportDefinitionPage({
        pageNo: 1,
        pageSize: 20,
        reportCode,
        status: 'PUBLISHED'
      }),
      '报表定义'
    )
    definitionList.value = page.list
    selectedDefinitionId.value = page.list[0]?.id
    if (!selectedDefinitionId.value) {
      definitionError.value = '当前报表暂无已发布定义，不能执行只读查询。'
    }
  } catch (error) {
    definitionList.value = []
    selectedDefinitionId.value = undefined
    definitionError.value = resolveErrorMessage(error, '报表定义加载失败，请检查口径版本配置。')
  } finally {
    definitionLoading.value = false
  }
}

const selectCatalog = async (row: EdhrReportCatalogRespVO) => {
  selectedCatalog.value = row
  queryResult.value = undefined
  auditList.value = []
  auditTotal.value = 0
  auditQueryParams.pageNo = 1
  auditQueryParams.reportCode = row.reportCode
  await getDefinitionList(row.reportCode)
  await getAuditList()
}

const handleDefinitionChange = () => {
  queryResult.value = undefined
}

const handleCatalogQuery = () => {
  catalogQueryParams.pageNo = 1
  selectedCatalog.value = undefined
  selectedDefinitionId.value = undefined
  definitionList.value = []
  queryResult.value = undefined
  getCatalogList()
}

const resetCatalogQuery = () => {
  catalogQueryParams.pageNo = 1
  catalogQueryParams.pageSize = 10
  catalogQueryParams.reportName = ''
  catalogQueryParams.reportCategory = ''
  catalogQueryParams.status = 'ACTIVE'
  handleCatalogQuery()
}

const handleRunQuery = async () => {
  queryLoading.value = true
  loadError.value = ''
  try {
    const definition = selectedDefinition.value
    if (!definition) throw new Error('请选择已发布报表后执行查询。')
    queryResult.value = assertQueryResult(
      await runEdhrReportQuery({
        reportDefinitionId: definition.id,
        reportCode: definition.reportCode,
        filterSnapshotJson: buildFilterSnapshotJson()
      })
    )
  } catch (error) {
    queryResult.value = undefined
    loadError.value = resolveErrorMessage(error, '报表只读查询失败，请检查数据源、口径版本和权限。')
  } finally {
    queryLoading.value = false
  }
}

const handleRecordExportAudit = async () => {
  loadError.value = ''
  try {
    const result = queryResult.value
    if (!result) throw new Error('请先执行只读查询，再记录导出审计。')
    await recordEdhrReportExportAudit({
      reportDefinitionId: result.reportDefinitionId,
      reportCode: result.reportCode,
      filterSnapshotJson: result.filterSnapshotJson || buildFilterSnapshotJson(),
      permissionSummaryJson: result.permissionSummaryJson || '{}',
      dataRangeSummary: `只读查询结果 ${result.rows.length} 行`
    })
    ElMessage.success('导出审计已记录')
    auditQueryParams.reportCode = result.reportCode
    auditQueryParams.pageNo = 1
    await getAuditList()
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '导出审计记录失败，请检查报表权限和筛选快照。')
  }
}

const getAuditList = async () => {
  if (!auditQueryParams.reportCode) {
    auditList.value = []
    auditTotal.value = 0
    return
  }
  auditLoading.value = true
  loadError.value = ''
  try {
    const page = assertPageResult<EdhrReportExportAuditRespVO>(
      await getEdhrReportExportAuditPage(auditQueryParams),
      '导出审计'
    )
    auditList.value = page.list
    auditTotal.value = page.total
  } catch (error) {
    auditList.value = []
    auditTotal.value = 0
    loadError.value = resolveErrorMessage(error, '导出审计加载失败，请检查接口和权限。')
  } finally {
    auditLoading.value = false
  }
}

onMounted(() => {
  getCatalogList()
})
</script>

<style scoped>
.edhr-report {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-report__toolbar,
.edhr-report__catalog,
.edhr-report__definition,
.edhr-report__query,
.edhr-report__audit {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-report__toolbar {
  padding: 16px 16px 0;
}

.edhr-report__catalog,
.edhr-report__definition,
.edhr-report__query,
.edhr-report__audit {
  padding: 16px;
}

.edhr-report__title-row,
.edhr-report__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.edhr-report__title-row {
  margin-bottom: 12px;
}

.edhr-report__title-row h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.3;
}

.edhr-report__subtitle,
.edhr-report__muted {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-report__form {
  display: flex;
  flex-wrap: wrap;
}

.edhr-report__section-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.edhr-report__detail-grid {
  display: grid;
  grid-template-columns: minmax(420px, 0.92fr) minmax(460px, 1.08fr);
  gap: 16px;
}

.edhr-report__definition-picker {
  margin-bottom: 12px;
}

.edhr-report__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-report :deep(.el-table__header th) {
  height: 44px;
  background: #f7f9fc;
}

.edhr-report :deep(.el-table__row) {
  height: 52px;
}

.edhr-report pre {
  max-height: 170px;
  margin: 0;
  overflow: auto;
  color: #172033;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-family:
    ui-monospace,
    SFMono-Regular,
    Menlo,
    Monaco,
    Consolas,
    'Liberation Mono',
    'Courier New',
    monospace;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-report__audit-evidence {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 8px 16px 12px;
}

.edhr-report__evidence-title {
  margin-bottom: 8px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .edhr-report__detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .edhr-report__title-row,
  .edhr-report__section-title,
  .edhr-report__audit-evidence {
    align-items: flex-start;
    grid-template-columns: 1fr;
  }
}
</style>
