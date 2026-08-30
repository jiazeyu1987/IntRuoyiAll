<template>
  <ContentWrap
    class="scheme-d-basic-data-page scheme-d-basic-data-page--dcc-registration-certificate-historical-import"
    data-testid="dcc-registration-certificate-historical-import-page"
  >
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">基础数据 / 注册证历史导入</span>
    </div>
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
    >
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.registrationCertificate.historicalImport"
          :data="list"
          border
          :empty-text="historicalImportEmptyText"
          :show-overflow-tooltip="true"
          row-key="id"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isHistoricalImportColumnVisible('sourceHash')"
            label="来源哈希"
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

defineOptions({ name: 'DccRegistrationCertificateHistoricalImport' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<DccRegistrationCertificateHistoricalImportRespVO[]>([])
const loadError = ref('')

type HistoricalImportPageQuery = DccRegistrationCertificateHistoricalImportPageReqVO & {
  pageNo: number
  pageSize: number
}

const queryParams = reactive<HistoricalImportPageQuery>({
  pageNo: 1,
  pageSize: 10,
  sourceHash: undefined
})

const historicalImportQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'sourceHash',
    label: '来源哈希',
    type: 'text',
    queryParamKey: 'sourceHash',
    placeholder: '输入来源哈希'
  }
]

const historicalImportDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sourceHash', label: '来源哈希', minWidth: 180, sortable: false },
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
  const record = error as {
    message?: string
    msg?: string
    response?: { data?: { msg?: string; message?: string } }
  }
  return (
    record?.response?.data?.msg ||
    record?.response?.data?.message ||
    record?.msg ||
    record?.message ||
    '注册证历史导入加载失败，请查看网络或后端错误后重试。'
  )
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await DccHistoricalImportApi.getHistoricalImportPage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch (error) {
    const errorMessage = resolvePageErrorMessage(error)
    loadError.value = errorMessage
    list.value = []
    total.value = 0
    message.error(errorMessage)
  } finally {
    loading.value = false
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
</script>

<style scoped>
.historical-import-load-error {
  margin: 12px;
  width: auto;
}
</style>
