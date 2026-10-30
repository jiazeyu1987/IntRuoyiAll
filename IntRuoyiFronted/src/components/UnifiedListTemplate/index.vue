<template>
  <div
    class="unified-list-template"
    :class="{ 'unified-list-template--single-line-toolbar': singleLineToolbar }"
    :data-table-key="tableKey"
  >
    <div
      v-if="showQueryForm !== false && $slots.filters"
      class="unified-list-template__query-form unified-list-template__custom-query"
    >
      <div class="unified-list-template__filter-panel">
        <div class="unified-list-template__filters"><slot name="filters"></slot></div>
        <TableMultiFilter
          v-if="columnFilteringEnabled"
          :table-key="tableKey"
          :filter-definitions="resolvedStandardFilterDefinitions"
          :state="columnFilter.state.value"
          :show-actions="false"
          @update:state="handleStandardFilterStateUpdate"
          @query="handleStandardFilterQuery"
          @reset="handleStandardFilterReset"
          @remove="handleStandardFilterRemove"
        />
        <ListFilterActions
          :loading="queryLoading || columnFilter.loading.value"
          :query-button-attrs="queryButtonAttrs"
          @query="handleCustomFilterQuery"
          @reset="handleCustomFilterReset"
        />
      </div>
      <div class="unified-list-template__toolbar">
        <slot name="actions"></slot>
        <UserTableColumnSettings
          v-if="showColumnSettings !== false && columns.length"
          :columns="columns"
          :saving="columnSaving"
          :show-reset="showColumnReset"
          @change="$emit('column-change', $event)"
          @reset="$emit('column-reset')"
        />
      </div>
    </div>
    <el-form
      v-else-if="
        showQueryForm !== false &&
        ($slots.actions ||
          $slots['extra-filters'] ||
          shouldRenderStandardConditionFilter ||
          (showColumnSettings !== false && columns.length))
      "
      class="unified-list-template__query-form"
      :model="queryModel"
      :inline="true"
      :label-width="labelWidth"
      :data-testid="queryFormTestId"
    >
      <div class="unified-list-template__filter-panel">
        <el-form-item
          v-if="shouldRenderStandardConditionFilter"
          class="unified-list-template__multi-filter"
        >
          <TableMultiFilter
            :table-key="tableKey"
            :filter-definitions="resolvedStandardFilterDefinitions"
            :state="resolvedStandardFilterState"
            :show-operators="showMultiFilterOperators"
            :show-actions="false"
            @update:state="handleStandardFilterStateUpdate"
            @query="handleStandardFilterQuery"
            @reset="handleStandardFilterReset"
            @remove="handleStandardFilterRemove"
          />
        </el-form-item>

        <div v-if="$slots['extra-filters']" class="unified-list-template__extra-filters">
          <slot name="extra-filters"></slot>
        </div>

        <ListFilterActions
          v-if="
            shouldRenderStandardConditionFilter &&
            (resolvedStandardFilterState.conditions.length > 0 || $slots['extra-filters'])
          "
          :loading="queryLoading || columnFilter.loading.value"
          :query-button-attrs="queryButtonAttrs"
          @query="handleStandardFilterQuery"
          @reset="handleStandardFilterReset"
        />
      </div>

      <el-form-item class="unified-list-template__toolbar-actions">
        <div class="unified-list-template__toolbar">
          <slot name="actions"></slot>
          <UserTableColumnSettings
            v-if="showColumnSettings !== false && columns.length"
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

    <el-alert
      v-if="columnFilter.error.value"
      :title="columnFilter.error.value"
      type="error"
      :closable="false"
    />
    <div class="unified-list-template__table-shell">
      <slot
        name="table"
        :rows="pageRows"
        :resolve-rows="resolveRows"
        :sort-state="normalizedSortState"
        :sortable-columns="standardSortableColumns"
        :sortable-column-map="standardSortableColumnMap"
        :sort-column-attrs="getStandardSortColumnAttrs"
        :handle-sort-change="handleStandardSortChange"
      ></slot>
    </div>

    <Pagination
      :total="resolvedTotal"
      :page="resolvedPage"
      :limit="resolvedLimit"
      :storage-key="tableKey"
      :layout="paginationLayout"
      @update:page="updatePage"
      @update:limit="updateLimit"
      @pagination="handlePagination"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, toRaw, watch } from 'vue'
import TableMultiFilter from '@/components/TableMultiFilter/index.vue'
import ListFilterActions from '@/components/ListFilterActions/index.vue'
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
import {
  buildListColumnFilters,
  useListColumnFilter,
  type ListColumnFilterDefinition
} from '@/hooks/web/useListColumnFilter'

defineOptions({ name: 'UnifiedListTemplate' })

type QuickFilterState = {
  fieldKey?: string
  operator?: TableQuickFilterOperator
  value?: string | number | boolean | Array<string | number>
  conditions?: ListMultiFilterCondition[]
  appliedConditions?: ListMultiFilterCondition[]
  activeConditionId?: string
}

const EMPTY_MULTI_FILTER_STATE: ListMultiFilterState = {
  conditions: [],
  appliedConditions: []
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

type UnifiedListSortableColumnInput =
  | string
  | {
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

const props = withDefaults(
  defineProps<{
    tableKey: string
    queryModel?: Record<string, any>
    labelWidth?: string
    queryFormTestId?: string
    filterDefinitions?: TableQuickFilterDefinition[]
    showQuickFilter?: boolean
    showQuickFilterLabel?: boolean
    quickFilterState?: QuickFilterState
    selectedFilterDefinition?: TableQuickFilterDefinition
    operatorOptions?: TableQuickFilterOperator[]
    showMultiFilter?: boolean
    multiFilterDefinitions?: ListMultiFilterDefinition[]
    multiFilterState?: ListMultiFilterState
    queryLoading?: boolean
    queryButtonAttrs?: Record<string, unknown>
    resetFilters?: () => void | Promise<void>
    showMultiFilterOperators?: boolean
    columns?: UserTableColumnState[]
    columnSaving?: boolean
    showColumnSettings?: boolean
    showColumnReset?: boolean
    showQueryForm?: boolean
    singleLineToolbar?: boolean
    sortableColumns?: UnifiedListSortableColumnInput[]
    sortState?: UnifiedListSortState
    rows?: any[]
    filterRows?: any[]
    loadFilterRows?: () => Promise<any[]>
    listRows?: any[]
    columnFilterDefinitions?: ListColumnFilterDefinition[]
    paginationLayout?: string
    total?: number
    page?: number
    limit?: number
  }>(),
  {
    queryModel: () => ({}),
    filterDefinitions: () => [],
    quickFilterState: () => ({ conditions: [], appliedConditions: [] }),
    operatorOptions: () => [],
    columns: () => [],
    total: 0,
    page: 1,
    limit: 20,
    showQueryForm: true,
    showQuickFilter: true,
    showQuickFilterLabel: true,
    showMultiFilter: false,
    multiFilterDefinitions: () => [],
    showMultiFilterOperators: true,
    showColumnSettings: true,
    showColumnReset: false,
    singleLineToolbar: true
  }
)

const emit = defineEmits<{
  query: []
  reset: []
  'update:quickFilterState': [state: QuickFilterState]
  'quick-filter-query': []
  'update:multiFilterState': [state: ListMultiFilterState]
  'multi-filter-query': []
  'multi-filter-reset': []
  'multi-filter-remove': [key: string]
  'column-change': [columns: UserTableColumnState[]]
  'column-reset': []
  'filtered-rows': [rows: any[]]
  'column-filter-reset': []
  'update:page': [page: number]
  'update:limit': [limit: number]
  'update:sortState': [state: UnifiedListSortState]
  'sort-change': [state: UnifiedListSortChange]
  pagination: [payload: UnifiedListPaginationPayload]
}>()

// Local arrays are paged here; server lists keep their existing page/limit contract.
const localPage = ref(1)
const localLimit = ref(20)
const columnFilteringEnabled = computed(
  () => props.filterRows !== undefined || !!props.loadFilterRows || props.rows !== undefined
)
const columnDefinitions = computed(() =>
  buildListColumnFilters(props.columns, [
    ...(props.filterDefinitions as ListColumnFilterDefinition[]),
    ...props.multiFilterDefinitions,
    ...(props.columnFilterDefinitions || [])
  ])
)
const columnFilter = useListColumnFilter(
  columnDefinitions,
  () => props.filterRows ?? props.rows,
  () => props.loadFilterRows
)
const resolvedTotal = computed(() =>
  columnFilter.active.value
    ? columnFilter.rows.value.length
    : props.rows === undefined
      ? props.total
      : props.rows.length
)
const resolvedPage = computed(() =>
  columnFilter.active.value || props.rows !== undefined ? localPage.value : props.page
)
const resolvedLimit = computed(() =>
  columnFilter.active.value || props.rows !== undefined ? localLimit.value : props.limit
)
const pageRows = computed(() =>
  (columnFilter.active.value ? columnFilter.rows.value : props.rows)?.slice(
    (resolvedPage.value - 1) * resolvedLimit.value,
    resolvedPage.value * resolvedLimit.value
  )
)
let publishedRows: any[] | undefined
watch(pageRows, (rows) => {
  if (columnFilter.active.value && props.loadFilterRows && rows) {
    if (
      publishedRows?.length === rows.length &&
      rows.every((row, index) => toRaw(row) === toRaw(publishedRows![index]))
    )
      return
    publishedRows = rows
    emit('filtered-rows', rows)
  }
})
watch(
  () => props.listRows,
  (rows) => {
    if (
      rows &&
      toRaw(rows) !== toRaw(publishedRows) &&
      (columnFilter.active.value || columnFilter.loading.value)
    ) {
      columnFilter.invalidate()
      void columnFilter.apply()
    }
  }
)
const resolveRows = <T,>(original: T[]): T[] =>
  columnFilter.active.value ? (pageRows.value as T[]) : original
const updatePage = (page: number) => {
  if (props.rows !== undefined || columnFilter.active.value) localPage.value = page
  emit('update:page', page)
}
const updateLimit = (limit: number) => {
  if (props.rows !== undefined || columnFilter.active.value) {
    localLimit.value = limit
    localPage.value = 1
  }
  emit('update:limit', limit)
}
const handlePagination = (payload: UnifiedListPaginationPayload) => {
  if (!columnFilter.active.value) emit('pagination', payload)
}
watch(resolvedTotal, (total) => {
  if (props.rows !== undefined || columnFilter.active.value)
    localPage.value = Math.min(localPage.value, Math.max(1, Math.ceil(total / localLimit.value)))
})
watch(
  () => props.rows,
  (rows) => {
    if (rows !== undefined) localPage.value = 1
  }
)

const DEFAULT_COLUMN_SORTABLE = false
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
  if (columnFilteringEnabled.value) return columnDefinitions.value.length > 0
  if (props.showMultiFilter === true) return true
  return (
    props.showQuickFilter !== false && quickDefinitionsAsMultiFilterDefinitions.value.length > 0
  )
})

const resolvedQuickFilterStateAsMultiFilter = computed<ListMultiFilterState>(() => ({
  conditions: props.quickFilterState.conditions || [],
  appliedConditions: props.quickFilterState.appliedConditions || [],
  activeConditionId: props.quickFilterState.activeConditionId
}))

const resolvedStandardFilterDefinitions = computed(() =>
  columnFilteringEnabled.value
    ? columnDefinitions.value
    : props.showMultiFilter === true
      ? props.multiFilterDefinitions
      : quickDefinitionsAsMultiFilterDefinitions.value
)

const resolvedStandardFilterState = computed(() =>
  columnFilteringEnabled.value
    ? columnFilter.state.value
    : props.showMultiFilter === true
      ? resolvedMultiFilterState.value
      : resolvedQuickFilterStateAsMultiFilter.value
)

const normalizeSortOrder = (order: unknown): UnifiedListSortOrder =>
  order === 'ascending' || order === 'descending' ? order : null

const normalizeSortableColumn = (
  column: UnifiedListSortableColumnInput
): UnifiedListSortableColumn => {
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
    sortable: column.sortable ?? DEFAULT_COLUMN_SORTABLE,
    sortOrders: column.sortOrders
  }
}

const isDefaultSortableColumn = (column: UserTableColumnState) =>
  column.sortable !== false && column.business !== false && column.hideable !== false

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
  const lookupKey =
    typeof columnKeyOrConfig === 'string' ? columnKeyOrConfig : columnKeyOrConfig.key
  const sortableColumn = inlineColumn || standardSortableColumnMap.value.get(lookupKey)
  if (!sortableColumn) {
    return {
      sortable: DEFAULT_COLUMN_SORTABLE,
      sortOrders: STANDARD_SORT_ORDERS
    }
  }
  return {
    sortable: sortableColumn.sortable ?? DEFAULT_COLUMN_SORTABLE,
    sortOrders: sortableColumn.sortOrders || STANDARD_SORT_ORDERS
  }
}

const handleStandardSortChange = (payload?: {
  prop?: string
  order?: string | null
  column?: any
}) => {
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
  const activeId =
    state.activeConditionId || (state.conditions[0] ? getConditionId(state.conditions[0], 0) : '')
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
        : (activeCondition?.value as
            | string
            | number
            | boolean
            | Array<string | number>
            | undefined),
    conditions: [...(state.conditions || [])],
    appliedConditions: [...state.appliedConditions],
    activeConditionId: state.activeConditionId
  }
}

const handleStandardFilterStateUpdate = (state: ListMultiFilterState) => {
  if (columnFilteringEnabled.value) {
    columnFilter.state.value = state
    return
  }
  if (props.showMultiFilter === true) {
    emit('update:multiFilterState', state)
    return
  }
  emit('update:quickFilterState', toQuickFilterState(state))
}

const handleStandardFilterQuery = async () => {
  if (columnFilteringEnabled.value) {
    await columnFilter.apply()
    localPage.value = 1
    localLimit.value = props.limit
    emit('update:page', 1)
    if (!columnFilter.active.value && props.loadFilterRows) emit('column-filter-reset')
    return
  }
  if (props.showMultiFilter === true) {
    emit('multi-filter-query')
    return
  }
  emit('quick-filter-query')
}

const handleCustomFilterQuery = async () => {
  if (columnFilteringEnabled.value && columnFilter.state.value.conditions.length) {
    updatePage(1)
    emit('query')
    await nextTick()
    await handleStandardFilterQuery()
    return
  }
  updatePage(1)
  await nextTick()
  emit('query')
}
const handleCustomFilterReset = () => {
  columnFilter.reset()
  publishedRows = undefined
  updatePage(1)
  emit('reset')
}

const handleStandardFilterReset = async () => {
  if (columnFilteringEnabled.value) {
    columnFilter.reset()
    publishedRows = undefined
    localPage.value = 1
    emit('update:page', 1)
    if (props.resetFilters) await props.resetFilters()
    else if (props.loadFilterRows) emit('column-filter-reset')
    return
  }
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
    appliedConditions: props.quickFilterState.appliedConditions || [],
    activeConditionId: undefined
  })
  await nextTick()
  if (props.resetFilters) {
    await props.resetFilters()
    return
  }
  emit('quick-filter-query')
}

const handleStandardFilterRemove = (conditionId: string) => {
  if (columnFilteringEnabled.value) {
    void handleStandardFilterQuery()
    return
  }
  if (props.showMultiFilter === true) {
    emit('multi-filter-remove', conditionId)
  }
}

defineExpose({
  resetColumnFilters: () => {
    columnFilter.reset()
    publishedRows = undefined
    localPage.value = 1
    localLimit.value = props.limit
  }
})
</script>

<style scoped>
.unified-list-template {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.unified-list-template > .unified-list-template__query-form.unified-list-template__custom-query {
  display: grid;
  grid-template-columns: minmax(0, 1fr) fit-content(55%);
  align-items: start;
}

.unified-list-template__filters,
.unified-list-template__custom-query > .unified-list-template__toolbar {
  min-width: 0;
  overflow-x: auto;
}

.unified-list-template__filter-panel {
  display: flex;
  flex: 1 1 100%;
  flex-direction: column;
  min-width: 0;
  gap: 8px;
}

.unified-list-template__extra-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.unified-list-template__filters :deep(.el-form),
.unified-list-template__filters > :deep(div) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.unified-list-template__filters :deep(.el-form-item) {
  flex: 1 1 200px;
  min-width: 180px;
  margin: 0;
}

.unified-list-template__filters :deep(.el-form-item__content) {
  min-width: 0;
}
.unified-list-template__filters :deep(.el-input),
.unified-list-template__filters :deep(.el-select),
.unified-list-template__filters :deep(.el-date-editor) {
  width: 100% !important;
  min-width: 110px;
}
.unified-list-template__custom-query > .unified-list-template__toolbar {
  flex-wrap: nowrap;
  justify-content: flex-start;
}
.unified-list-template__custom-query > .unified-list-template__toolbar > :deep(*) {
  flex-shrink: 0;
}
.unified-list-template__custom-query
  > .unified-list-template__toolbar
  > :deep(.el-button + .el-button) {
  margin-left: 0;
}
.unified-list-template__filters :deep(.el-form-item__label) {
  white-space: nowrap;
  height: 32px;
  line-height: 32px;
  margin-bottom: 0;
}
.unified-list-template__table-shell > :deep(.el-table) {
  --el-table-border-color: #dbe3ef;
}

.unified-list-template--contained {
  height: var(--unified-list-height, 360px);
  min-height: 0;
}
.unified-list-template--contained > .unified-list-template__table-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.unified-list-template--contained > .unified-list-template__table-shell > :deep(.el-table) {
  height: 100%;
  flex: 1;
  min-height: 0;
}
.unified-list-template--contained > .unified-list-template__query-form,
.unified-list-template--contained > :deep(.el-pagination) {
  flex-shrink: 0;
}
.unified-list-template--contained :deep(.el-table__inner-wrapper) {
  height: 100%;
}
.unified-list-template--contained :deep(.el-table__body-wrapper) {
  flex: 1;
  min-height: 0;
}
.unified-list-template--contained :deep(.el-table__body-wrapper > .el-scrollbar),
.unified-list-template--contained :deep(.el-scrollbar__wrap) {
  height: 100%;
  max-height: none !important;
}
:global(.standard-list-page) {
  height: calc(
    100dvh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding)
  );
  margin-bottom: 0;
}
:global(.standard-list-page > .el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
:global(.standard-list-page > .el-card__body > .unified-list-template--contained) {
  flex: 1;
  height: auto;
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
  font-size: 14px;
}

.unified-list-template__table-shell :deep(.el-table th.el-table__cell) {
  height: 46px;
  background: #f7f9fc;
  color: #172033;
  font-weight: 600;
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
    grid-template-columns: minmax(0, 1fr) minmax(0, auto);
    align-items: start;
  }

  .unified-list-template--single-line-toolbar .unified-list-template__multi-filter {
    grid-column: 1;
    grid-row: 1;
    min-width: 0;
  }

  .unified-list-template--single-line-toolbar .unified-list-template__filter-panel {
    grid-column: 1;
    grid-row: 1;
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
