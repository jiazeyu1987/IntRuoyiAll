<template>
  <div
    class="unified-list-template"
    :class="{ 'unified-list-template--single-line-toolbar': singleLineToolbar }"
    :data-table-key="tableKey"
  >
    <el-form
      v-if="showQueryForm !== false"
      class="unified-list-template__query-form"
      :model="queryModel"
      :inline="true"
      :label-width="labelWidth"
      :data-testid="queryFormTestId"
    >
      <el-form-item
        v-if="shouldRenderStandardConditionFilter"
        class="unified-list-template__multi-filter"
      >
        <TableMultiFilter
          :table-key="tableKey"
          :filter-definitions="resolvedStandardFilterDefinitions"
          :state="resolvedStandardFilterState"
          :show-operators="showMultiFilterOperators"
          @update:state="handleStandardFilterStateUpdate"
          @query="handleStandardFilterQuery"
          @reset="handleStandardFilterReset"
          @remove="handleStandardFilterRemove"
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
import TableMultiFilter from '@/components/TableMultiFilter/index.vue'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import type {
  ListMultiFilterCondition,
  ListMultiFilterDefinition,
  ListMultiFilterOperator,
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
  conditions?: ListMultiFilterCondition[]
  activeConditionId?: string
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
  singleLineToolbar?: boolean
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
  showColumnReset: false,
  singleLineToolbar: false
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

const quickDefinitionsAsMultiFilterDefinitions = computed<ListMultiFilterDefinition[]>(() =>
  props.filterDefinitions.map((definition) => ({
    ...definition,
    type: definition.type,
    operators: definition.operators as ListMultiFilterOperator[] | undefined
  }))
)

const shouldRenderStandardConditionFilter = computed(() => {
  if (props.showMultiFilter === true) return true
  return props.showQuickFilter !== false && quickDefinitionsAsMultiFilterDefinitions.value.length > 0
})

const resolvedQuickFilterStateAsMultiFilter = computed<ListMultiFilterState>(() => ({
  conditions: props.quickFilterState.conditions || [],
  activeConditionId: props.quickFilterState.activeConditionId
}))

const resolvedStandardFilterDefinitions = computed(() =>
  props.showMultiFilter === true
    ? props.multiFilterDefinitions
    : quickDefinitionsAsMultiFilterDefinitions.value
)

const resolvedStandardFilterState = computed(() =>
  props.showMultiFilter === true
    ? resolvedMultiFilterState.value
    : resolvedQuickFilterStateAsMultiFilter.value
)

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

const getConditionId = (condition: Partial<ListMultiFilterCondition>, index = 0) =>
  condition.id || condition.key || `condition-${index + 1}`

const getActiveConditionFromState = (state: ListMultiFilterState) => {
  const activeId = state.activeConditionId || (state.conditions[0] ? getConditionId(state.conditions[0], 0) : '')
  return state.conditions.find((condition, index) => getConditionId(condition, index) === activeId)
}

const toQuickFilterState = (state: ListMultiFilterState): QuickFilterState => {
  const activeCondition = getActiveConditionFromState(state)
  return {
    ...props.quickFilterState,
    fieldKey: activeCondition?.key,
    operator: activeCondition?.operator as TableQuickFilterOperator | undefined,
    value:
      activeCondition?.valueEnd !== undefined
        ? [activeCondition.value as string | number, activeCondition.valueEnd as string | number]
        : activeCondition?.value as string | number | boolean | Array<string | number> | undefined,
    conditions: [...(state.conditions || [])],
    activeConditionId: state.activeConditionId
  }
}

const handleStandardFilterStateUpdate = (state: ListMultiFilterState) => {
  if (props.showMultiFilter === true) {
    emit('update:multiFilterState', state)
    return
  }
  emit('update:quickFilterState', toQuickFilterState(state))
}

const handleStandardFilterQuery = () => {
  if (props.showMultiFilter === true) {
    emit('multi-filter-query')
    return
  }
  emit('quick-filter-query')
}

const handleStandardFilterReset = async () => {
  if (props.showMultiFilter === true) {
    emit('multi-filter-reset')
    return
  }
  emit('update:quickFilterState', {
    ...props.quickFilterState,
    fieldKey: undefined,
    operator: undefined,
    value: undefined,
    conditions: [],
    activeConditionId: undefined
  })
  await nextTick()
  emit('quick-filter-query')
}

const handleStandardFilterRemove = (conditionId: string) => {
  if (props.showMultiFilter === true) {
    emit('multi-filter-remove', conditionId)
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

@media (min-width: 1181px) {
  .unified-list-template--single-line-toolbar .unified-list-template__query-form {
    display: grid;
    grid-template-columns: minmax(720px, 1fr) auto;
    align-items: start;
  }

  .unified-list-template--single-line-toolbar .unified-list-template__multi-filter {
    grid-column: 1;
    grid-row: 1;
    min-width: 720px;
  }

  .unified-list-template--single-line-toolbar .unified-list-template__toolbar-actions {
    grid-column: 2;
    grid-row: 1;
    align-self: start;
    margin-left: 0;
  }

  .unified-list-template--single-line-toolbar .unified-list-template__toolbar {
    flex-wrap: nowrap;
  }
}
</style>
