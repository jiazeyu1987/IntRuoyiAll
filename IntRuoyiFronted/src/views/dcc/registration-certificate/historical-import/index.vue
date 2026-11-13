<template>
  <ContentWrap
    class="scheme-d-basic-data-page scheme-d-basic-data-page--dcc-registration-certificate-historical-import"
    data-testid="dcc-registration-certificate-historical-import-page"
  >
    <el-alert
      v-if="loadError"
      class="historical-import-load-error"
      type="error"
      :closable="false"
      :title="loadError"
    />
    <UnifiedListTemplate
      table-key="dcc.registrationCertificate.historicalImport"
      :query-model="queryParams"
      label-width="86px"
      query-form-test-id="dcc-registration-certificate-historical-import-filter-form"
      :filter-definitions="historicalImportQuickFilterDefinitions"
      :quick-filter-state="historicalImportQuickFilter.state"
      :selected-filter-definition="historicalImportQuickFilter.selectedDefinition.value"
      :operator-options="historicalImportQuickFilter.operatorOptions.value"
      :columns="historicalImportColumns"
      :column-saving="historicalImportColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="historicalImportQuickFilter.updateState"
      @quick-filter-query="historicalImportQuickFilter.applyQuickFilter"
      @column-change="saveHistoricalImportColumnConfig"
      @pagination="getList"
      server-filter
            :server-filter-query="getList"
      :column-filter-definitions="listColumnFilterDefinitions0"
    >
      <template #table="{ resolveRows, sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.registrationCertificate.historicalImport"
          :data="resolveRows(list)"
          border
          :empty-text="historicalImportEmptyText"
          :show-overflow-tooltip="true"
          row-key="id"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isHistoricalImportColumnVisible('sourceHash')"
            label="来源校验值"
            prop="sourceHash"
            :min-width="getHistoricalImportColumnMinWidthString('sourceHash', 180)"
            v-bind="sortColumnAttrs('sourceHash')"
          />
          <el-table-column
            v-if="isHistoricalImportColumnVisible('sourceRow')"
            label="源行号"
            prop="sourceRow"
            align="center"
            :width="getHistoricalImportColumnWidthString('sourceRow', 90)"
            v-bind="sortColumnAttrs('sourceRow')"
          />
          <el-table-column
            v-if="isHistoricalImportColumnVisible('ownerCompanyName')"
            label="所属公司"
            prop="ownerCompanyName"
            :min-width="getHistoricalImportColumnMinWidthString('ownerCompanyName', 180)"
            v-bind="sortColumnAttrs('ownerCompanyName')"
          >
            <template #default="{ row }">
              {{ row.ownerCompanyName || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isHistoricalImportColumnVisible('ownerCompanyCode')"
            label="公司编码"
            prop="ownerCompanyCode"
            :min-width="getHistoricalImportColumnMinWidthString('ownerCompanyCode', 150)"
            v-bind="sortColumnAttrs('ownerCompanyCode')"
          >
            <template #default="{ row }">
              {{ row.ownerCompanyCode || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isHistoricalImportColumnVisible('certificateNo')"
            label="注册证编号"
            prop="certificateNo"
            :min-width="getHistoricalImportColumnMinWidthString('certificateNo', 160)"
            v-bind="sortColumnAttrs('certificateNo')"
          >
            <template #default="{ row }">
              {{ row.certificateNo || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isHistoricalImportColumnVisible('productName')"
            label="产品名称"
            prop="productName"
            :min-width="getHistoricalImportColumnMinWidthString('productName', 180)"
            v-bind="sortColumnAttrs('productName')"
          >
            <template #default="{ row }">
              {{ row.productName || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isHistoricalImportColumnVisible('versionNo')"
            label="版本"
            prop="versionNo"
            align="center"
            :width="getHistoricalImportColumnWidthString('versionNo', 90)"
            v-bind="sortColumnAttrs('versionNo')"
          >
            <template #default="{ row }">
              {{ row.versionNo == null ? '-' : row.versionNo }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isHistoricalImportColumnVisible('result')"
            label="结果"
            prop="result"
            align="center"
            :width="getHistoricalImportColumnWidthString('result', 120)"
            v-bind="sortColumnAttrs('result')"
          >
            <template #default="{ row }">
              <el-tag :type="getResultTagType(row.result)">{{ formatResult(row.result) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isHistoricalImportColumnVisible('restrictedReasons')"
            label="受限原因"
            prop="restrictedReasons"
            :min-width="getHistoricalImportColumnMinWidthString('restrictedReasons', 200)"
            v-bind="sortColumnAttrs('restrictedReasons')"
          >
            <template #default="{ row }">
              {{ formatRestrictedReasons(row.restrictedReasons) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isHistoricalImportColumnVisible('occurredAt')"
            label="发生时间"
            prop="occurredAt"
            :width="getHistoricalImportColumnWidthString('occurredAt', 180)"
            :formatter="dateFormatter2"
            v-bind="sortColumnAttrs('occurredAt')"
          />
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import { onMounted, reactive, ref } from 'vue'
import { dateFormatter2 } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import * as DccHistoricalImportApi from '@/api/dcc/registrationCertificate/historicalImport'
import type {
  DccRegistrationCertificateHistoricalImportPageReqVO,
  DccRegistrationCertificateHistoricalImportRespVO
} from '@/api/dcc/registrationCertificate/historicalImport'
import { resolveRegistrationCertificateUserMessage } from '../shared/state'

defineOptions({ name: 'DccRegistrationCertificateHistoricalImport' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<DccRegistrationCertificateHistoricalImportRespVO[]>([])
const loadError = ref('')

type HistoricalImportPageQuery = DccRegistrationCertificateHistoricalImportPageReqVO & {
  listFilters?: string
  pageNo: number
  pageSize: number
}

const queryParams = reactive<HistoricalImportPageQuery>({
  pageNo: 1,
  pageSize: 10,
  listFilters: undefined as string | undefined,
  sourceHash: undefined
})

const historicalImportQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'sourceHash',
    label: '来源校验值',
    type: 'text',
    queryParamKey: 'sourceHash',
    placeholder: '请输入来源校验值'
  }
]

const historicalImportDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sourceHash', label: '来源校验值', minWidth: 180, sortable: false },
  { key: 'sourceRow', label: '源行号', width: 90, sortable: false },
  { key: 'ownerCompanyName', label: '所属公司', minWidth: 180, sortable: false },
  { key: 'ownerCompanyCode', label: '公司编码', minWidth: 150, sortable: false },
  { key: 'certificateNo', label: '注册证编号', minWidth: 160, sortable: false },
  { key: 'productName', label: '产品名称', minWidth: 180, sortable: false },
  { key: 'versionNo', label: '版本', width: 90, sortable: false },
  { key: 'result', label: '结果', width: 120, sortable: false },
  { key: 'restrictedReasons', label: '受限原因', minWidth: 200, sortable: false },
  { key: 'occurredAt', label: '发生时间', width: 180, sortable: false }
]

const {
  columns: historicalImportColumns,
  saving: historicalImportColumnSaving,
  isColumnVisible: isHistoricalImportColumnVisible,
  getColumnWidthString: getHistoricalImportColumnWidthString,
  getColumnMinWidthString: getHistoricalImportColumnMinWidthString,
  saveConfig: saveHistoricalImportColumnConfig
} = useUserTableColumns(
  'dcc.registrationCertificate.historicalImport',
  historicalImportDefaultColumns
)

const formatResult = (result?: string | null) =>
  result === 'SUCCESS' ? '导入成功' : result === 'FAILURE' ? '导入失败' : result || '-'

const getResultTagType = (result?: string | null) => {
  if (result === 'SUCCESS') return 'success'
  if (result === 'FAILURE') return 'danger'
  return 'info'
}

const formatRestrictedReasons = (reasons?: string[] | null) =>
  reasons && reasons.length > 0 ? reasons.join('、') : '无'

const resolvePageErrorMessage = (error: unknown) => {
  return resolveRegistrationCertificateUserMessage(
    error,
    '注册证历史导入加载失败，请检查网络或后端服务后重试。'
  )
}

let listRequestGeneration = 0
const getList = async () => {
  const generation = ++listRequestGeneration
  loading.value = true
  loadError.value = ''
  try {
    const data = await DccHistoricalImportApi.getHistoricalImportPage({ ...queryParams })
    if (generation !== listRequestGeneration) return
    list.value = data.list
    total.value = data.total
  } catch (error) {
    if (generation !== listRequestGeneration) throw error
    const errorMessage = resolvePageErrorMessage(error)
    loadError.value = errorMessage
    list.value = []
    total.value = 0
    message.error(errorMessage)
    throw error
  } finally {
    if (generation === listRequestGeneration) loading.value = false
  }
}

const historicalImportEmptyText = '当前暂无注册证历史导入记录'

const historicalImportQuickFilter = useTableQuickFilter(
  'dcc.registrationCertificate.historicalImport',
  historicalImportQuickFilterDefinitions,
  queryParams,
  getList
)

onMounted(() => {
  void getList()
})


const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'versionNo',
    label: '版本',
    type: 'text',
    getValue: (row) => [row.versionNo == null ? '-' : row.versionNo].join(' ')
  },
  {
    key: 'result',
    label: '结果',
    type: 'text',
    getValue: (row) => [formatResult(row.result)].join(' ')
  },
  {
    key: 'restrictedReasons',
    label: '受限原因',
    type: 'text',
    getValue: (row) => [formatRestrictedReasons(row.restrictedReasons)].join(' ')
  }
])
</script>

<style scoped>
.scheme-d-basic-data-page--dcc-registration-certificate-historical-import {
  --el-color-primary: #008f86;
  --el-color-primary-light-3: #4db1aa;
  --el-color-primary-light-5: #80c7c2;
  --el-color-primary-light-7: #b3ddd9;
  --el-color-primary-light-8: #cce9e7;
  --el-color-primary-light-9: #e6f5f2;
  --el-color-primary-dark-2: #00726b;

  min-width: 0;
}

.scheme-d-basic-data-page--dcc-registration-certificate-historical-import :deep(.el-card__body) {
  padding: 12px !important;
}

.historical-import-load-error {
  margin: 0 0 8px;
  width: auto;
}

:deep(.unified-list-template__query-form) {
  align-items: flex-end;
  gap: 8px;
  padding-bottom: 8px;
}

:deep(.unified-list-template__multi-filter) {
  flex: 1 1 640px;
  min-width: 0;
}

:deep(.unified-list-template__toolbar-actions) {
  margin-left: auto;
}

:deep(.unified-list-template__table-shell) {
  min-width: 0;
  border-color: #dbe3e7;
  border-radius: 4px;
}

:deep(.el-table) {
  --el-table-header-bg-color: #f5f8f9;
  --el-table-border-color: #dbe3e7;

  width: 100%;
  border-radius: 4px;
  font-size: 13px;
}

:deep(.el-table th.el-table__cell) {
  padding: 8px 0;
  font-size: 12px;
  font-weight: 600;
}

:deep(.el-table td.el-table__cell) {
  padding: 6px 0;
}

:deep(.el-button) {
  height: 32px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 32px;
}

:deep(.el-pagination) {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin: 8px 0 0;
}

@media (max-width: 700px) {
  .scheme-d-basic-data-page--dcc-registration-certificate-historical-import
    :deep(.table-multi-filter__tabs-row) {
    flex-wrap: wrap;
  }

  .scheme-d-basic-data-page--dcc-registration-certificate-historical-import
    :deep(.table-multi-filter-field) {
    flex-wrap: wrap;
    width: 100%;
    min-width: 0;
    max-width: 100%;
  }

  .scheme-d-basic-data-page--dcc-registration-certificate-historical-import
    :deep(.table-multi-filter-field__value) {
    flex: 1 1 180px;
    min-width: 0;
    max-width: 100%;
  }

  .scheme-d-basic-data-page--dcc-registration-certificate-historical-import :deep(.el-pagination) {
    justify-content: flex-start;
  }
}
</style>
