<template>
  <ContentWrap class="in-process-order-viewport">
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
    <UnifiedListTemplate
      table-key="mes.pro.in-process-order"
      single-line-toolbar
      :show-filter-reset="false"
      :query-model="queryParams"
      :filter-definitions="filterDefinitions"
      :quick-filter-state="quickFilter.state"
      :selected-filter-definition="quickFilter.selectedDefinition.value"
      :operator-options="quickFilter.operatorOptions.value"
      :columns="columns"
      :column-saving="columnSaving"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="updateQuickFilterState"
      @quick-filter-query="quickFilter.applyQuickFilter"
      @column-change="saveColumnConfig"
      @pagination="getList"
    >
      <template #table>
        <el-table
          v-loading="loading || columnLoading"
          :data="list"
          height="100%"
          border
          stripe
          row-key="id"
          show-overflow-tooltip
          data-user-table-column-explicit
          data-user-table-key="mes.pro.in-process-order"
          @header-dragend="handleHeaderDragend"
        >
          <el-table-column
            v-for="column in visibleColumns"
            :key="column.key"
            :column-key="column.key"
            :prop="column.key"
            :label="column.label"
            :width="getColumnWidthString(column.key, column.width)"
            :fixed="column.key === 'workOrderCode' ? 'left' : false"
            align="center"
          >
            <template #default="{ row }">
              <template
                v-if="
                  column.key === 'productionProgressPercent' ||
                  column.key === 'inspectionProgressPercent'
                "
              >
                <span v-if="row.readBlocked || row[column.key] == null" :title="row.readBlockReason"
                  >无法读取</span
                >
                <span v-else>{{ row[column.key] }}%</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <el-tag v-if="row.readBlocked" type="danger" :title="row.readBlockReason"
                  >数据异常</el-tag
                >
                <el-tag v-else-if="row.businessStatus === 'VERSION_UPGRADE_PENDING'" type="warning"
                  >升版审批中</el-tag
                >
                <el-tag v-else-if="row.abnormal" type="warning" :title="row.abnormalReason"
                  >异常处理中</el-tag
                >
                <el-tag v-else-if="row.businessStatus === 'COMPLETED'" type="info">已完工待放行</el-tag>
                <el-tag v-else type="success">在制</el-tag>
              </template>
              <span v-else-if="column.key === 'joinedAt'">{{ formatDate(row.joinedAt) }}</span>
              <span v-else>{{ row[column.key] }}</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :description="loadError ? '查询失败，请重试' : '暂无符合条件的在制工单'" />
          </template>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterState,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import { getInProcessOrderPage, type InProcessOrder } from '@/api/mes/pro/inProcessOrder'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'MesInProcessOrder' })
const queryParams = reactive({ pageNo: 1, pageSize: 20 })
const list = ref<InProcessOrder[]>([])
const total = ref(0)
const loading = ref(false)
const loadError = ref('')
let requestSequence = 0
const getList = async () => {
  const sequence = ++requestSequence
  loading.value = true
  loadError.value = ''
  try {
    const result = await getInProcessOrderPage({ ...queryParams })
    if (sequence !== requestSequence) return
    list.value = result.list
    total.value = result.total
  } catch (error) {
    if (sequence !== requestSequence) return
    list.value = []
    total.value = 0
    loadError.value = error instanceof Error ? error.message : '在制工单查询失败，请重试'
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}
const filterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'productName',
    label: '名称',
    placeholder: '产品名称或工单名称',
    type: 'text',
    queryParamKey: 'productName',
    operators: ['contains']
  },
  {
    key: 'equipment',
    label: '设备',
    placeholder: '已复核生产、检验设备名称或编码',
    type: 'text',
    queryParamKey: 'equipment',
    operators: ['contains']
  },
  {
    key: 'batchCode',
    label: '生产批号',
    type: 'text',
    queryParamKey: 'batchCode',
    operators: ['contains']
  },
  {
    key: 'workOrderCode',
    label: '生产订单号',
    type: 'text',
    queryParamKey: 'workOrderCode',
    operators: ['contains']
  }
]
const defaultColumns: UserTableColumnDefinition[] = [
  { key: 'workOrderCode', label: '生产订单号', width: 200 },
  { key: 'productName', label: '产品名称', width: 220 },
  { key: 'productCode', label: '产品编码', width: 170 },
  { key: 'batchCode', label: '生产批号', width: 180 },
  { key: 'leaderName', label: '生产组长', width: 120 },
  { key: 'routeName', label: '工艺路线', width: 180 },
  { key: 'quantity', label: '生产数量', width: 120 },
  { key: 'productionProgressPercent', label: '生产进度', width: 120 },
  { key: 'inspectionProgressPercent', label: '检验进度', width: 120 },
  { key: 'status', label: '状态', width: 120 },
  { key: 'joinedAt', label: '加入时间', width: 180 }
]
const {
  columns,
  loading: columnLoading,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig
} = useUserTableColumns('mes.pro.in-process-order', defaultColumns)
const visibleColumns = computed(() => columns.value.filter((column) => isColumnVisible(column.key)))
const quickFilter = useTableQuickFilter(
  'mes.pro.in-process-order',
  filterDefinitions,
  queryParams,
  getList
)
const updateQuickFilterState = async (next: Partial<TableQuickFilterState>) => {
  const previousCount = quickFilter.state.conditions?.length ?? 0
  quickFilter.updateState(next)
  if (next.conditions && next.conditions.length < previousCount) {
    await quickFilter.applyQuickFilter()
  }
}

let activated = false
onMounted(getList)
onActivated(() => {
  if (activated) getList()
  activated = true
})
</script>

<style scoped>
.in-process-order-viewport {
  height: calc(
    100dvh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding)
  );
  min-height: 0;
  margin-bottom: 0;
}
.in-process-order-viewport :deep(> .el-card__body) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}
.in-process-order-viewport :deep(.unified-list-template) {
  flex: 1;
  min-height: 0;
}
.in-process-order-viewport :deep(.unified-list-template__table-shell) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.in-process-order-viewport :deep(.el-pagination) {
  flex-shrink: 0;
  margin-bottom: 0;
}
</style>
