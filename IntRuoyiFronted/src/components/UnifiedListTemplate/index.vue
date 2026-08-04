<template>
  <div class="unified-list-template" :data-table-key="tableKey">
    <el-form
      v-if="showQueryForm !== false"
      class="unified-list-template__query-form"
      :model="queryModel"
      :inline="true"
      :label-width="labelWidth"
      :data-testid="queryFormTestId"
    >
      <el-form-item v-if="showQuickFilter !== false" class="unified-list-template__quick-filter">
        <TableQuickFilter
          :table-key="tableKey"
          :filter-definitions="filterDefinitions"
          :show-label="showQuickFilterLabel"
          :state="quickFilterState"
          :selected-definition="selectedFilterDefinition"
          :operator-options="operatorOptions"
          @update:state="handleQuickFilterStateUpdate"
          @query="$emit('quick-filter-query')"
        />
      </el-form-item>

      <el-form-item v-if="showMultiFilter === true" class="unified-list-template__multi-filter">
        <TableMultiFilter
          :table-key="tableKey"
          :filter-definitions="multiFilterDefinitions"
          :state="resolvedMultiFilterState"
          :show-operators="showMultiFilterOperators"
          @update:state="$emit('update:multiFilterState', $event)"
          @query="$emit('multi-filter-query')"
          @reset="$emit('multi-filter-reset')"
          @remove="$emit('multi-filter-remove', $event)"
        />
      </el-form-item>

      <slot name="extra-filters"></slot>

      <el-form-item class="unified-list-template__toolbar-actions">
        <div class="unified-list-template__toolbar">
          <slot name="actions"></slot>
          <UserTableColumnSettings
            v-if="showColumnSettings !== false"
            class="unified-list-template__column-settings"
            :columns="columns"
            :saving="columnSaving"
            :show-reset="showColumnReset"
            @change="$emit('column-change', $event)"
            @reset="$emit('column-reset')"
          />
        </div>
      </el-form-item>
    </el-form>

    <div class="unified-list-template__table-shell">
      <slot
        name="table"
        :sort-state="normalizedSortState"
        :sortable-columns="standardSortableColumns"
        :sortable-column-map="standardSortableColumnMap"
        :sort-column-attrs="getStandardSortColumnAttrs"
        :handle-sort-change="handleStandardSortChange"
      ></slot>
    </div>

    <Pagination
      :total="total"
      :page="page"
      :limit="limit"
      :storage-key="tableKey"
      @update:page="$emit('update:page', $event)"
      @update:limit="$emit('update:limit', $event)"
      @pagination="$emit('pagination', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick } from 'vue'
import TableQuickFilter from '@/components/TableQuickFilter/index.vue'
import TableMultiFilter from '@/components/TableMultiFilter/index.vue'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import type {
  ListMultiFilterDefinition,
  ListMultiFilterState
} from '@/hooks/web/useTableMultiFilter'
import type {
  TableQuickFilterDefinition,
  TableQuickFilterOperator
} from '@/hooks/web/useTableQuickFilter'
import type { UserTableColumnState } from '@/hooks/web/useUserTableColumns'

defineOptions({ name: 'UnifiedListTemplate' })

type QuickFilterState = {
  fieldKey?: string
  operator?: TableQuickFilterOperator
  value?: string | number | boolean | Array<string | number>
}

const EMPTY_MULTI_FILTER_STATE: ListMultiFilterState = {
  conditions: []
}

type UnifiedListSortOrder = 'ascending' | 'descending' | null

type UnifiedListSortState = {
  key?: string
  prop?: string
  order?: UnifiedListSortOrder
}

type UnifiedListSortChange = UnifiedListSortState & {
  column?: any
}

type UnifiedListPaginationPayload = {
  page?: number
  limit?: number
}

type UnifiedListSortableColumnInput = string | {
  key: string
  prop?: string
  sortable?: boolean | 'custom'
  sortOrders?: UnifiedListSortOrder[]
}

type UnifiedListSortableColumn = {
  key: string
  prop: string
  sortable?: boolean | 'custom'
  sortOrders?: UnifiedListSortOrder[]
}

const props = withDefaults(defineProps<{
  tableKey: string
  queryModel: Record<string, any>
  labelWidth?: string
  queryFormTestId?: string
  filterDefinitions: TableQuickFilterDefinition[]
  showQuickFilter?: boolean
  showQuickFilterLabel?: boolean
  quickFilterState: QuickFilterState
  selectedFilterDefinition?: TableQuickFilterDefinition
  operatorOptions: TableQuickFilterOperator[]
  showMultiFilter?: boolean
  multiFilterDefinitions?: ListMultiFilterDefinition[]
  multiFilterState?: ListMultiFilterState
  showMultiFilterOperators?: boolean
  columns: UserTableColumnState[]
  columnSaving?: boolean
  showColumnSettings?: boolean
  showColumnReset?: boolean
  showQueryForm?: boolean
  sortableColumns?: UnifiedListSortableColumnInput[]
  sortState?: UnifiedListSortState
  total: number
  page: number
  limit: number
}>(), {
  showQueryForm: true,
  showQuickFilter: true,
  showQuickFilterLabel: true,
  showMultiFilter: false,
  multiFilterDefinitions: () => [],
  showMultiFilterOperators: true,
  showColumnSettings: true,
  showColumnReset: false
})

const emit = defineEmits<{
  'update:quickFilterState': [state: QuickFilterState]
  'quick-filter-query': []
  'update:multiFilterState': [state: ListMultiFilterState]
  'multi-filter-query': []
  'multi-filter-reset': []
  'multi-filter-remove': [key: string]
  'column-change': [columns: UserTableColumnState[]]
  'column-reset': []
  'update:page': [page: number]
  'update:limit': [limit: number]
  'update:sortState': [state: UnifiedListSortState]
  'sort-change': [state: UnifiedListSortChange]
  pagination: [payload: UnifiedListPaginationPayload]
}>()

const DEFAULT_COLUMN_SORTABLE = true
const STANDARD_SORT_ORDERS: UnifiedListSortOrder[] = ['ascending', 'descending', null]

const resolvedMultiFilterState = computed(() => props.multiFilterState || EMPTY_MULTI_FILTER_STATE)

const normalizeSortOrder = (order: unknown): UnifiedListSortOrder =>
  order === 'ascending' || order === 'descending' ? order : null

const normalizeSortableColumn = (column: UnifiedListSortableColumnInput): UnifiedListSortableColumn => {
  if (typeof column === 'string') {
    return {
      key: column,
      prop: column,
      sortable: DEFAULT_COLUMN_SORTABLE
    }
  }
  return {
    key: column.key,
    prop: column.prop || column.key,
    sortable: column.sortable || DEFAULT_COLUMN_SORTABLE,
    sortOrders: column.sortOrders
  }
}

const isDefaultSortableColumn = (column: UserTableColumnState) =>
  column.sortable !== false &&
  column.business !== false &&
  column.hideable !== false

const standardSortableColumns = computed<UnifiedListSortableColumn[]>(() => {
  const sortableByKey = new Map<string, UnifiedListSortableColumn>()
  for (const column of props.columns) {
    if (!isDefaultSortableColumn(column)) continue
    const sortableColumn = normalizeSortableColumn({
      key: column.key,
      prop: column.sortProp || column.key,
      sortable: column.sortable,
      sortOrders: column.sortOrders
    })
    sortableByKey.set(sortableColumn.key, sortableColumn)
  }
  for (const column of props.sortableColumns || []) {
    const sortableColumn = normalizeSortableColumn(column)
    sortableByKey.set(sortableColumn.key, sortableColumn)
  }
  return Array.from(sortableByKey.values())
})

const standardSortableColumnMap = computed(() => {
  const sortableMap = new Map<string, UnifiedListSortableColumn>()
  for (const column of standardSortableColumns.value) {
    sortableMap.set(column.key, column)
    sortableMap.set(column.prop, column)
  }
  return sortableMap
})

const normalizedSortState = computed<UnifiedListSortState>(() => {
  const activeProp = props.sortState?.prop || props.sortState?.key
  const sortableColumn = activeProp ? standardSortableColumnMap.value.get(activeProp) : undefined
  return {
    key: props.sortState?.key || sortableColumn?.key || activeProp,
    prop: props.sortState?.prop || sortableColumn?.prop || activeProp,
    order: normalizeSortOrder(props.sortState?.order)
  }
})

const getStandardSortColumnAttrs = (columnKeyOrConfig: string | UnifiedListSortableColumnInput) => {
  const inlineColumn =
    typeof columnKeyOrConfig === 'string' ? undefined : normalizeSortableColumn(columnKeyOrConfig)
  const lookupKey = typeof columnKeyOrConfig === 'string' ? columnKeyOrConfig : columnKeyOrConfig.key
  const sortableColumn = inlineColumn || standardSortableColumnMap.value.get(lookupKey)
  if (!sortableColumn) {
    return {
      sortable: DEFAULT_COLUMN_SORTABLE,
      sortOrders: STANDARD_SORT_ORDERS
    }
  }
  return {
    sortable: sortableColumn.sortable || DEFAULT_COLUMN_SORTABLE,
    sortOrders: sortableColumn.sortOrders || STANDARD_SORT_ORDERS
  }
}

const handleStandardSortChange = (payload?: { prop?: string; order?: string | null; column?: any }) => {
  const payloadProp = String(payload?.prop || payload?.column?.property || '')
  const sortableColumn = standardSortableColumnMap.value.get(payloadProp)
  const nextState: UnifiedListSortState = {
    key: sortableColumn?.key || payloadProp || undefined,
    prop: sortableColumn?.prop || payloadProp || undefined,
    order: normalizeSortOrder(payload?.order)
  }
  emit('update:sortState', nextState)
  emit('sort-change', { ...nextState, column: payload?.column })
}

const isQuickFilterSelectValueChange = (state: QuickFilterState) =>
  props.selectedFilterDefinition?.type === 'select' &&
  'value' in state &&
  state.fieldKey === props.quickFilterState.fieldKey &&
  state.operator === props.quickFilterState.operator &&
  state.value !== props.quickFilterState.value

const handleQuickFilterStateUpdate = async (state: QuickFilterState) => {
  const shouldAutoSearch = isQuickFilterSelectValueChange(state)
  emit('update:quickFilterState', state)
  if (shouldAutoSearch) {
    await nextTick()
    emit('quick-filter-query')
  }
}
</script>

<style scoped>
.unified-list-template {
  display: flex;
  flex-direction: column;
}

.unified-list-template__query-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 0;
  padding: 0 0 12px;
}

.unified-list-template__query-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.unified-list-template__query-form :deep(.el-form-item__content) {
  align-items: center;
}

.unified-list-template__quick-filter {
  flex: 0 0 auto;
}

.unified-list-template__multi-filter {
  flex: 1 1 100%;
  min-width: min(720px, 100%);
}

.unified-list-template__multi-filter :deep(.el-form-item__content) {
  width: 100%;
}

.unified-list-template__toolbar-actions {
  flex: 0 0 auto;
  margin-left: auto;
}

.unified-list-template__toolbar-actions :deep(.el-form-item__content) {
  width: auto;
}

.unified-list-template__toolbar {
  display: flex;
  width: auto;
  max-width: 100%;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.unified-list-template__column-settings {
  margin-left: 0;
  white-space: nowrap;
}

.unified-list-template__table-shell {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.unified-list-template__table-shell :deep(.el-table) {
  border-radius: 8px;
}

.unified-list-template__table-shell :deep(.el-table th.is-sortable) {
  cursor: pointer;
}

@media (max-width: 1360px) {
  .unified-list-template__toolbar {
    justify-content: flex-start;
  }

  .unified-list-template__toolbar-actions {
    margin-left: 0;
  }
}
</style>
