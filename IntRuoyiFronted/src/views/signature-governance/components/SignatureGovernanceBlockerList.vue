<template>
  <UnifiedListTemplate
    class="signature-governance-blocker-list"
    :table-key="tableKey"
    :query-model="queryParams"
    :filter-definitions="blockerQuickFilterDefinitions"
    :show-quick-filter-label="false"
    :quick-filter-state="blockerQuickFilter.state"
    :selected-filter-definition="blockerQuickFilter.selectedDefinition.value"
    :operator-options="blockerQuickFilter.operatorOptions.value"
    :columns="blockerColumns"
    :column-saving="blockerColumnSaving"
    :total="filteredRows.length"
    v-model:page="queryParams.pageNo"
    v-model:limit="queryParams.pageSize"
    @update:quick-filter-state="blockerQuickFilter.updateState"
    @quick-filter-query="blockerQuickFilter.applyQuickFilter"
    @column-change="saveBlockerColumnConfig"
    @column-reset="resetBlockerColumnConfig"
    @pagination="handlePagination"
  >
    <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
      <el-table
        data-user-table-column-explicit
        :data-user-table-key="tableKey"
        :data="pagedRows"
        :empty-text="emptyText"
        :show-overflow-tooltip="true"
        @header-dragend="handleBlockerHeaderDragend"
        @sort-change="handleTemplateSortChange"
      >
        <el-table-column
          v-if="isBlockerColumnVisible('code')"
          label="阻断码"
          prop="code"
          :width="getBlockerColumnWidthString('code', 210)"
          v-bind="sortColumnAttrs('code')"
        />
        <el-table-column
          v-if="isBlockerColumnVisible('message')"
          label="原因"
          prop="message"
          :min-width="getBlockerColumnMinWidthString('message', 260)"
          v-bind="sortColumnAttrs('message')"
        />
        <el-table-column
          v-if="isBlockerColumnVisible('impact')"
          label="影响"
          prop="impact"
          :min-width="getBlockerColumnMinWidthString('impact', 340)"
          v-bind="sortColumnAttrs('impact')"
        />
      </el-table>
    </template>
  </UnifiedListTemplate>
</template>

<script lang="ts" setup>
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import type { SignatureGovernanceBlocker } from '@/api/signature-governance/shared'

defineOptions({ name: 'SignatureGovernanceBlockerList' })

const props = withDefaults(
  defineProps<{
    tableKey: string
    rows?: SignatureGovernanceBlocker[]
    emptyText?: string
  }>(),
  {
    rows: () => [],
    emptyText: '暂无阻断'
  }
)

const blockerDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '阻断码', width: 210 },
  { key: 'message', label: '原因', minWidth: 260 },
  { key: 'impact', label: '影响', minWidth: 340 }
]

const {
  columns: blockerColumns,
  saving: blockerColumnSaving,
  isColumnVisible: isBlockerColumnVisible,
  getColumnWidthString: getBlockerColumnWidthString,
  getColumnMinWidthString: getBlockerColumnMinWidthString,
  handleHeaderDragend: handleBlockerHeaderDragend,
  saveConfig: saveBlockerColumnConfig,
  resetConfig: resetBlockerColumnConfig
} = useUserTableColumns(props.tableKey, blockerDefaultColumns)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const blockerQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'code', label: '阻断码', type: 'text', placeholder: '请输入阻断码' },
  { key: 'message', label: '原因', type: 'text', placeholder: '请输入原因' },
  { key: 'impact', label: '影响', type: 'text', placeholder: '请输入影响' }
]

const normalizeFilterText = (value: unknown) => String(value ?? '').trim().toLowerCase()

const isMatchedRow = (row: SignatureGovernanceBlocker, quickFilter?: TableQuickFilterValue) => {
  if (!quickFilter) return true
  const actual = normalizeFilterText(row[quickFilter.fieldKey as keyof SignatureGovernanceBlocker])
  const expected = normalizeFilterText(quickFilter.value)
  return quickFilter.operator === 'eq' ? actual === expected : actual.includes(expected)
}

const filteredRows = computed(() =>
  (props.rows || []).filter((row) => isMatchedRow(row, queryParams.quickFilter))
)

const pagedRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredRows.value.slice(start, start + queryParams.pageSize)
})

const handlePagination = () => {
  if ((queryParams.pageNo - 1) * queryParams.pageSize >= filteredRows.value.length) {
    queryParams.pageNo = 1
  }
}

const blockerQuickFilter = useTableQuickFilter(
  props.tableKey,
  blockerQuickFilterDefinitions,
  queryParams,
  handlePagination
)

watch(
  () => props.rows,
  () => handlePagination()
)
</script>
