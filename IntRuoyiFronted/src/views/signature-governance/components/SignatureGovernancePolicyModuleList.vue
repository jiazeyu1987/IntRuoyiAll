<template>
  <UnifiedListTemplate
    class="signature-governance-policy-module-list"
    :table-key="tableKey"
    :query-model="queryParams"
    :filter-definitions="policyQuickFilterDefinitions"
    :show-quick-filter-label="false"
    :quick-filter-state="policyQuickFilter.state"
    :selected-filter-definition="policyQuickFilter.selectedDefinition.value"
    :operator-options="policyQuickFilter.operatorOptions.value"
    :columns="policyColumns"
    :column-saving="policyColumnSaving"
    :total="filteredRows.length"
    v-model:page="queryParams.pageNo"
    v-model:limit="queryParams.pageSize"
    @update:quick-filter-state="policyQuickFilter.updateState"
    @quick-filter-query="policyQuickFilter.applyQuickFilter"
    @column-change="savePolicyColumnConfig"
    @column-reset="resetPolicyColumnConfig"
    @pagination="handlePagination"
  >
    <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
      <el-table
        v-loading="loading"
        data-user-table-column-explicit
        :data-user-table-key="tableKey"
        :data="pagedRows"
        :show-overflow-tooltip="true"
        :empty-text="emptyText"
        @header-dragend="handlePolicyHeaderDragend"
        @sort-change="handleTemplateSortChange"
      >
        <el-table-column
          v-if="isPolicyColumnVisible('moduleCode')"
          label="模块"
          prop="moduleCode"
          :width="getPolicyColumnWidthString('moduleCode', 160)"
          v-bind="sortColumnAttrs('moduleCode')"
        />
        <el-table-column
          v-if="isPolicyColumnVisible('status')"
          label="策略状态"
          prop="status"
          :min-width="getPolicyColumnMinWidthString('status', 180)"
          v-bind="sortColumnAttrs('status')"
        >
          <template #default>
            <el-tag :type="statusType" size="small">{{ statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isPolicyColumnVisible('blocker')"
          label="关键阻断"
          prop="blocker"
          :min-width="getPolicyColumnMinWidthString('blocker', 220)"
          v-bind="sortColumnAttrs('blocker')"
        >
          <template #default="{ row }">{{ resolveFirstBlocker(row) }}</template>
        </el-table-column>
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

defineOptions({ name: 'SignatureGovernancePolicyModuleList' })

type PolicyModuleRow = {
  moduleCode: string
  blockers?: SignatureGovernanceBlocker[]
}

const props = withDefaults(
  defineProps<{
    tableKey: string
    rows?: PolicyModuleRow[]
    loading?: boolean
    statusText: string
    statusType: string
    blockers?: SignatureGovernanceBlocker[]
    emptyText?: string
  }>(),
  {
    rows: () => [],
    blockers: () => [],
    emptyText: '暂无策略模块',
    loading: false
  }
)

const policyDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'moduleCode', label: '模块', width: 160 },
  { key: 'status', label: '策略状态', minWidth: 180 },
  { key: 'blocker', label: '关键阻断', minWidth: 220 }
]

const {
  columns: policyColumns,
  saving: policyColumnSaving,
  isColumnVisible: isPolicyColumnVisible,
  getColumnWidthString: getPolicyColumnWidthString,
  getColumnMinWidthString: getPolicyColumnMinWidthString,
  handleHeaderDragend: handlePolicyHeaderDragend,
  saveConfig: savePolicyColumnConfig,
  resetConfig: resetPolicyColumnConfig
} = useUserTableColumns(props.tableKey, policyDefaultColumns)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const policyQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'moduleCode', label: '模块', type: 'text', placeholder: '请输入模块' },
  { key: 'status', label: '策略状态', type: 'text', placeholder: '请输入策略状态' },
  { key: 'blocker', label: '关键阻断', type: 'text', placeholder: '请输入阻断码' }
]

const firstBlockerCode = (blockers?: SignatureGovernanceBlocker[]) =>
  blockers && blockers.length > 0 ? blockers[0].code : '-'

const resolveFirstBlocker = (row: PolicyModuleRow) =>
  firstBlockerCode(row.blockers && row.blockers.length > 0 ? row.blockers : props.blockers)

const normalizeFilterText = (value: unknown) => String(value ?? '').trim().toLowerCase()

const resolveFilterText = (row: PolicyModuleRow, fieldKey: string) => {
  if (fieldKey === 'status') return props.statusText
  if (fieldKey === 'blocker') return resolveFirstBlocker(row)
  return row.moduleCode
}

const isMatchedRow = (row: PolicyModuleRow, quickFilter?: TableQuickFilterValue) => {
  if (!quickFilter) return true
  const actual = normalizeFilterText(resolveFilterText(row, quickFilter.fieldKey))
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

const policyQuickFilter = useTableQuickFilter(
  props.tableKey,
  policyQuickFilterDefinitions,
  queryParams,
  handlePagination
)

watch(
  () => props.rows,
  () => handlePagination()
)
</script>
