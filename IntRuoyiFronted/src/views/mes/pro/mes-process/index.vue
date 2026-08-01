<template>
  <doc-alert title="【生产】MES工序" url="https://doc.iocoder.cn/mes/pro/process-route/" />

  <!-- 数据源：压力泵工序.xlsx 的二代压力泵工作表，按 Excel 有效工序行原样展示。 -->
  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.pro.mesProcess.main"
      :query-model="queryParams"
      label-width="76px"
      :filter-definitions="mesProcessQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="mesProcessQuickFilter.state"
      :selected-filter-definition="mesProcessQuickFilter.selectedDefinition.value"
      :operator-options="mesProcessQuickFilter.operatorOptions.value"
      :columns="mesProcessColumns"
      :column-saving="mesProcessColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="mesProcessQuickFilter.updateState"
      @quick-filter-query="mesProcessQuickFilter.applyQuickFilter"
      @column-change="saveMesProcessColumnConfig"
      @column-reset="resetMesProcessColumnConfig"
      @pagination="getList"
    >
      <template #table>
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.mesProcess.main"
          :data="list"
          border
          :allow-drag-last-column="true"
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="rowKey"
          @header-dragend="handleMesProcessHeaderDragend"
        >
          <el-table-column
            v-if="isMesProcessColumnVisible('productName')"
            label="产品名称"
            align="center"
            prop="productName"
            fixed="left"
            :width="getMesProcessColumnWidthString('productName', 160)"
          >
            <template #default="{ row }">{{ formatSourceText(row.productName) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('sourceMachineryCodes')"
            label="设备编码"
            align="center"
            prop="sourceMachineryCodes"
            :width="getMesProcessColumnWidthString('sourceMachineryCodes', 150)"
          >
            <template #default="{ row }">{{ formatSourceText(row.sourceMachineryCodes) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('mesProcessName')"
            label="工序名称"
            align="center"
            prop="mesProcessName"
            :width="getMesProcessColumnWidthString('mesProcessName', 190)"
          >
            <template #default="{ row }">{{ formatSourceText(row.mesProcessName) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('sourceMachineryName')"
            label="设备名称"
            align="center"
            prop="sourceMachineryName"
            :width="getMesProcessColumnWidthString('sourceMachineryName', 180)"
          >
            <template #default="{ row }">{{ formatSourceText(row.sourceMachineryName) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('sourceMachineryQuantity')"
            label="设备数量"
            align="center"
            prop="sourceMachineryQuantity"
            :width="getMesProcessColumnWidthString('sourceMachineryQuantity', 96)"
          >
            <template #default="{ row }">{{ formatSourceText(row.sourceMachineryQuantity) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('dailyCapacity10_5')"
            label="10.5小时日产能"
            align="center"
            prop="dailyCapacity10_5"
            :width="getMesProcessColumnWidthString('dailyCapacity10_5', 140)"
          >
            <template #default="{ row }">{{ formatSourceText(row.dailyCapacity10_5) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('dailyWorkerQuantity')"
            label="日常工序人力"
            align="center"
            prop="dailyWorkerQuantity"
            :width="getMesProcessColumnWidthString('dailyWorkerQuantity', 120)"
          >
            <template #default="{ row }">{{ formatSourceText(row.dailyWorkerQuantity) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('mesProcessCode')"
            label="工序编码"
            align="center"
            prop="mesProcessCode"
            :width="getMesProcessColumnWidthString('mesProcessCode', 110)"
          >
            <template #default="{ row }">{{ formatSourceText(row.mesProcessCode) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('processPrice')"
            label="工序单价"
            align="center"
            prop="processPrice"
            :width="getMesProcessColumnWidthString('processPrice', 100)"
          >
            <template #default="{ row }">{{ formatSourceText(row.processPrice) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('feedbackFlag')"
            label="工序是否报工"
            align="center"
            prop="feedbackFlag"
            :width="getMesProcessColumnWidthString('feedbackFlag', 120)"
          >
            <template #default="{ row }">{{ formatSourceText(row.feedbackFlag) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('batchRecordFlag')"
            label="工序是否形成批记录"
            align="center"
            prop="batchRecordFlag"
            :width="getMesProcessColumnWidthString('batchRecordFlag', 160)"
          >
            <template #default="{ row }">{{ formatSourceText(row.batchRecordFlag) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isMesProcessColumnVisible('batchRecordProcessName')"
            label="批记录工序名称"
            align="center"
            prop="batchRecordProcessName"
            :width="getMesProcessColumnWidthString('batchRecordProcessName', 160)"
          >
            <template #default="{ row }">{{ formatSourceText(row.batchRecordProcessName) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>

<script setup lang="ts">
import { MesProcessApi, type MesProcessVO } from '@/api/mes/pro/mes-process'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesProMesProcess' })

const mesProcessDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'productName', label: '产品名称', width: 160 },
  { key: 'sourceMachineryCodes', label: '设备编码', width: 150 },
  { key: 'mesProcessName', label: '工序名称', width: 190 },
  { key: 'sourceMachineryName', label: '设备名称', width: 180 },
  { key: 'sourceMachineryQuantity', label: '设备数量', width: 96 },
  { key: 'dailyCapacity10_5', label: '10.5小时日产能', width: 140 },
  { key: 'dailyWorkerQuantity', label: '日常工序人力', width: 120 },
  { key: 'mesProcessCode', label: '工序编码', width: 110 },
  { key: 'processPrice', label: '工序单价', width: 100 },
  { key: 'feedbackFlag', label: '工序是否报工', width: 120 },
  { key: 'batchRecordFlag', label: '工序是否形成批记录', width: 160 },
  { key: 'batchRecordProcessName', label: '批记录工序名称', width: 160 }
]

const {
  columns: mesProcessColumns,
  saving: mesProcessColumnSaving,
  isColumnVisible: isMesProcessColumnVisible,
  getColumnWidthString: getMesProcessColumnWidthString,
  handleHeaderDragend: handleMesProcessHeaderDragend,
  saveConfig: saveMesProcessColumnConfig,
  resetConfig: resetMesProcessColumnConfig
} = useUserTableColumns('mes.pro.mesProcess.main', mesProcessDefaultColumns)

const loading = ref(true)
const list = ref<MesProcessVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 50,
  keyword: undefined as string | undefined
})

const mesProcessQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '产品 / 工序 / 设备编码 / 批记录工序'
  }
])

const getList = async () => {
  loading.value = true
  try {
    const data = await MesProcessApi.getMesProcessPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const mesProcessQuickFilter = useTableQuickFilter('mes.pro.mesProcess.main', mesProcessQuickFilterDefinitions, queryParams, getList)

const formatSourceText = (value?: string | number | null) => {
  if (value === undefined || value === null) return ''
  return String(value)
}

onMounted(() => {
  getList()
})
</script>
