<template>
  <ContentWrap>
    <div class="edhr-batch-record-test-page" data-edhr-batch-record-test-page>
      <section class="edhr-batch-record-test-page__header">
        <div>
          <div class="edhr-batch-record-test-page__title">批记录测试</div>
          <div class="edhr-batch-record-test-page__subtitle">
            通过受控 Codex Runner 对生产组长职责执行只读代码分析
          </div>
        </div>
        <el-tag type="success" effect="plain">独立测试页签</el-tag>
      </section>
      <el-tabs v-model="activeInnerTab" class="edhr-batch-record-test-page__inner-tabs">
        <el-tab-pane label="生产组长" name="productionLeader">
          <UnifiedListTemplate
            class="edhr-batch-record-test-page__list-template"
            data-edhr-batch-record-test-production-leader-list
            table-key="mes.pro.edhrBatchRecordTest.productionLeader"
            :query-model="queryParams"
            :filter-definitions="productionLeaderQuickFilterDefinitions"
            :quick-filter-state="productionLeaderQuickFilter.state"
            :selected-filter-definition="productionLeaderQuickFilter.selectedDefinition.value"
            :operator-options="productionLeaderQuickFilter.operatorOptions.value"
            :columns="productionLeaderColumns"
            :column-saving="productionLeaderColumnSaving"
            :show-column-reset="false"
            :total="filteredProductionLeaderRows.length"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @update:quick-filter-state="productionLeaderQuickFilter.updateState"
            @quick-filter-query="productionLeaderQuickFilter.applyQuickFilter"
            @column-change="saveProductionLeaderColumnConfig"
            @pagination="handleProductionLeaderPagination"
          >
            <template #actions>
              <el-form-item class="edhr-batch-record-test-page__tenant-filter" label="测试租户">
                <el-select v-model="selectedTenantId" class="!w-240px" placeholder="请选择测试租户">
                  <el-option
                    v-for="tenant in tenantOptions"
                    :key="tenant.id"
                    :label="tenant.name"
                    :value="tenant.id"
                  />
                </el-select>
              </el-form-item>
              <el-tag :type="runnerStatusTagType" effect="plain">
                Runner：{{ runnerStatusLabel }}
              </el-tag>
              <span class="edhr-batch-record-test-page__runner-message">{{ runnerStatusMessage }}</span>
              <el-button :loading="runnerStatusLoading" link type="primary" @click="refreshRunnerStatus">
                刷新状态
              </el-button>
            </template>

            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-alert
                v-if="loadError"
                class="edhr-batch-record-test-page__alert"
                :title="loadError"
                type="error"
                :closable="false"
                show-icon
              />
              <el-table
                data-user-table-column-explicit
                data-user-table-key="mes.pro.edhrBatchRecordTest.productionLeader"
                :data="pagedProductionLeaderRows"
                border
                row-key="id"
                :show-overflow-tooltip="true"
                stripe
                @header-dragend="handleProductionLeaderHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('sort')"
                  label="序号"
                  prop="sort"
                  :width="getProductionLeaderColumnWidthString('sort', 80)"
                  v-bind="sortColumnAttrs('sort')"
                />
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('title')"
                  label="职责"
                  prop="title"
                  :min-width="getProductionLeaderColumnMinWidthString('title', 220)"
                />
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('description')"
                  label="描述"
                  prop="description"
                  :min-width="getProductionLeaderColumnMinWidthString('description', 520)"
                />
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('caseName')"
                  label="测试项名称"
                  prop="caseName"
                  :min-width="getProductionLeaderColumnMinWidthString('caseName', 260)"
                />
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('actions')"
                  fixed="right"
                  label="操作"
                  prop="actions"
                  :width="getProductionLeaderColumnWidthString('actions', 110)"
                >
                  <template #default="{ row }">
                    <el-button
                      v-hasPermi="['system:codex-test:execute']"
                      :disabled="!selectedTenantId || testingRowId === row.id"
                      :loading="testingRowId === row.id"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-tab-pane>
      </el-tabs>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition,
  type UserTableColumnState
} from '@/hooks/web/useUserTableColumns'
import * as CodexTestApi from '@/api/system/codexTestManagement'
import * as TenantApi from '@/api/system/tenant'

defineOptions({ name: 'MesProEdhrBatchRecordTest' })

type ProductionLeaderTestRow = {
  id: number
  sort: number
  title: string
  description: string
  caseName: string
}

type PaginationPayload = {
  page?: number
  limit?: number
}

const message = useMessage()
const activeInnerTab = ref<'productionLeader'>('productionLeader')
const tenantOptions = ref<TenantApi.TenantVO[]>([])
const selectedTenantId = ref<number>()
const runnerStatus = ref<CodexTestApi.CodexTestRunnerStatusVO>()
const runnerStatusError = ref('')
const runnerStatusLoading = ref(false)
const testingRowId = ref<number>()
const loadError = ref('')

const productionLeaderRows: ProductionLeaderTestRow[] = [
  {
    id: 1,
    sort: 1,
    title: '工艺路线生产组长配置',
    description: '在工艺路线中配置生产组长，并关联到对应工序或“工序开始”节点。',
    caseName: '批记录测试-生产组长-01-工艺路线生产组长配置'
  },
  {
    id: 2,
    sort: 2,
    title: '批记录解析与工序配置',
    description: '从 QA 给的批记录文件解析批记录表单、工序、设备、参数、上下限，并为工序分配不良原因。',
    caseName: '批记录测试-生产组长-02-批记录解析与工序配置'
  },
  {
    id: 3,
    sort: 3,
    title: '生产人员管理',
    description: '维护正式员工和临时工，可新增临时工、设置/修改临时工密码、启用/禁用员工。',
    caseName: '批记录测试-生产组长-03-生产人员管理'
  },
  {
    id: 4,
    sort: 4,
    title: '报工分配与生产进度',
    description: '查看一线报工数据，将报工数量分配给一个或多个活跃订单；某订单某工序累计分配达到订单数量后更新生产进度。',
    caseName: '批记录测试-生产组长-04-报工分配与生产进度'
  },
  {
    id: 5,
    sort: 5,
    title: '活跃订单与检验进度',
    description: '将生产工单加入活跃订单列表；一线 PQC 提交活跃订单工序检验结果，PQC 组长确认后更新检验进度。',
    caseName: '批记录测试-生产组长-05-活跃订单与检验进度'
  }
]

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

const productionLeaderDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sort', label: '序号', width: 80 },
  { key: 'title', label: '职责', minWidth: 220 },
  { key: 'description', label: '描述', minWidth: 520, sortable: false },
  { key: 'caseName', label: '测试项名称', minWidth: 260, sortable: false },
  { key: 'actions', label: '操作', width: 110, hideable: false, business: false, sortable: false }
]

const productionLeaderColumnControl = useUserTableColumns(
  'mes.pro.edhrBatchRecordTest.productionLeader',
  productionLeaderDefaultColumns
)
const productionLeaderColumns = computed(() => productionLeaderColumnControl.columns.value)
const productionLeaderColumnSaving = computed(() => productionLeaderColumnControl.saving.value)
const isProductionLeaderColumnVisible = (key: string) => productionLeaderColumnControl.isColumnVisible(key)
const getProductionLeaderColumnWidthString = (key: string, fallback?: number) =>
  productionLeaderColumnControl.getColumnWidthString(key, fallback)
const getProductionLeaderColumnMinWidthString = (key: string, fallback?: number) =>
  productionLeaderColumnControl.getColumnMinWidthString(key, fallback)
const handleProductionLeaderHeaderDragend = async (newWidth: number, oldWidth: number, column: any) => {
  await productionLeaderColumnControl.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveProductionLeaderColumnConfig = async (columns: UserTableColumnState[]) => {
  await productionLeaderColumnControl.saveConfig(columns)
}

const productionLeaderQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'keyword',
    label: '职责/描述',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '输入职责或描述关键字'
  }
])

const productionLeaderQuickFilter = useTableQuickFilter(
  'mes.pro.edhrBatchRecordTest.productionLeader',
  productionLeaderQuickFilterDefinitions,
  queryParams,
  applyProductionLeaderListFilters
)

const filteredProductionLeaderRows = computed(() => {
  const keyword = queryParams.keyword.trim()
  if (!keyword) return productionLeaderRows
  return productionLeaderRows.filter((row) =>
    [row.title, row.description, row.caseName].some((text) => text.includes(keyword))
  )
})

const pagedProductionLeaderRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredProductionLeaderRows.value.slice(start, start + queryParams.pageSize)
})

const runnerStatusLabel = computed(() => {
  if (runnerStatus.value?.online) return '可用'
  if (runnerStatus.value?.status === 'CAPABILITY_MISSING') return '配置异常'
  if (runnerStatusError.value) return '诊断失败'
  return '按需启动'
})

const runnerStatusMessage = computed(() => {
  if (runnerStatus.value?.online) return 'Runner 可领取代码分析测试任务'
  if (runnerStatus.value?.message) return runnerStatus.value.message
  if (runnerStatusError.value) return runnerStatusError.value
  return '点击测试时后端会按需启动受控 Runner'
})

const runnerStatusTagType = computed(() => {
  if (runnerStatus.value?.online) return 'success'
  if (runnerStatus.value?.status === 'CAPABILITY_MISSING' || runnerStatusError.value) return 'danger'
  return 'warning'
})

async function applyProductionLeaderListFilters() {
  queryParams.pageNo = 1
}

async function handleProductionLeaderPagination(payload?: PaginationPayload) {
  if (typeof payload?.page === 'number') queryParams.pageNo = payload.page
  if (typeof payload?.limit === 'number') queryParams.pageSize = payload.limit
}

function showRequestError(error: unknown, defaultMessage: string) {
  const text = error instanceof Error ? error.message : typeof error === 'string' ? error : defaultMessage
  message.error(text || defaultMessage)
}

async function getTenantOptions() {
  try {
    loadError.value = ''
    tenantOptions.value = await TenantApi.getTenantList()
    selectedTenantId.value = tenantOptions.value[0]?.id
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '测试租户加载失败'
    showRequestError(error, '测试租户加载失败')
  }
}

async function refreshRunnerStatus() {
  runnerStatusLoading.value = true
  try {
    runnerStatusError.value = ''
    runnerStatus.value = await CodexTestApi.getCodexTestRunnerStatus()
  } catch (error) {
    runnerStatus.value = undefined
    runnerStatusError.value = error instanceof Error ? error.message : 'Runner 状态加载失败'
  } finally {
    runnerStatusLoading.value = false
  }
}

function buildCodeReadonlyCasePayload(definition: ProductionLeaderTestRow): CodexTestApi.CodexTestCaseVO {
  return {
    name: definition.caseName,
    project: '批记录',
    methodText: '只读扫描当前代码，分析是否已经完整支持生产组长职责：' + definition.description,
    testDataText: '职责口径：' + definition.title + '。' + definition.description,
    analysisMode: 'CODE_READONLY',
    defaultExecutionMode: 'SEQUENTIAL',
    parallelSafe: false,
    status: 'ENABLE',
    sort: definition.sort,
    checkpoints: [
      {
        sort: 1,
        name: definition.title,
        expectedText: '当前代码、路由、API、权限、数据模型和测试能够满足职责描述：' + definition.description,
        severity: 'MAJOR'
      }
    ]
  }
}

async function upsertCodeReadonlyCase(definition: ProductionLeaderTestRow) {
  const pageResult = await CodexTestApi.getCodexTestCasePage({
    pageNo: 1,
    pageSize: 10,
    project: '批记录',
    name: definition.caseName
  })
  const existingCase = pageResult.list.find(
    (item) => item.name === definition.caseName && item.project === '批记录'
  )
  const casePayload = buildCodeReadonlyCasePayload(definition)
  if (existingCase?.id) {
    await CodexTestApi.updateCodexTestCase({ id: existingCase.id, ...casePayload })
    return existingCase.id
  }
  return await CodexTestApi.createCodexTestCase(casePayload)
}

async function handleTestRow(row: ProductionLeaderTestRow) {
  if (!selectedTenantId.value) {
    message.error('请选择测试租户')
    return
  }
  testingRowId.value = row.id
  try {
    const caseId = await upsertCodeReadonlyCase(row)
    const executionId = await CodexTestApi.startCodexTestExecution({
      targetTenantId: selectedTenantId.value,
      executionMode: 'SEQUENTIAL',
      caseIds: [caseId]
    })
    message.success('已创建代码分析执行批次 ' + executionId)
    await refreshRunnerStatus()
  } catch (error) {
    showRequestError(error, '代码分析测试启动失败')
  } finally {
    testingRowId.value = undefined
  }
}

onMounted(async () => {
  await getTenantOptions()
  await refreshRunnerStatus()
})
</script>

<style scoped lang="scss">
.edhr-batch-record-test-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-record-test-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-batch-record-test-page__title {
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
  line-height: 28px;
}

.edhr-batch-record-test-page__subtitle {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
  line-height: 20px;
}

.edhr-batch-record-test-page__inner-tabs {
  :deep(.el-tabs__header) {
    margin: 0 0 12px;
  }
}

.edhr-batch-record-test-page__list-template {
  :deep(.unified-list-template__toolbar) {
    align-items: center;
  }
}

.edhr-batch-record-test-page__tenant-filter {
  margin-bottom: 0;
}

.edhr-batch-record-test-page__runner-message {
  color: #667085;
  font-size: 13px;
}

.edhr-batch-record-test-page__alert {
  margin-bottom: 12px;
}
</style>
