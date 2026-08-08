<template>
  <UnifiedListTemplate
    class="schedule-order-pool__schedule-template"
    table-key="mes.pro.scheduleOrder.main"
    :query-model="queryModel"
    label-width="88px"
    :filter-definitions="filterDefinitions"
    :show-quick-filter="!showMultiFilter"
    :show-quick-filter-label="false"
    :quick-filter-state="quickFilterState"
    :selected-filter-definition="selectedFilterDefinition"
    :operator-options="operatorOptions"
    :show-multi-filter="showMultiFilter"
    :multi-filter-definitions="multiFilterDefinitions"
    :multi-filter-state="multiFilterState"
    :show-multi-filter-operators="showMultiFilterOperators"
    :columns="columns"
    :column-saving="columnSaving"
    :sort-state="sortState"
    :show-column-settings="false"
    :show-column-reset="false"
    :total="total"
    :page="getPage(queryModel)"
    :limit="getLimit(queryModel)"
    @update:page="emit('update:page', $event)"
    @update:limit="emit('update:limit', $event)"
    @update:quick-filter-state="emit('update:quickFilterState', $event)"
    @quick-filter-query="emit('quickFilterQuery')"
    @update:multi-filter-state="emit('update:multiFilterState', $event)"
    @multi-filter-query="emit('multiFilterQuery')"
    @multi-filter-reset="emit('multiFilterReset')"
    @multi-filter-remove="emit('multiFilterRemove', $event)"
    @column-change="emit('columnChange', $event)"
    @column-reset="emit('columnReset')"
    @update:sort-state="emit('update:sortState', $event)"
    @sort-change="emit('sortChange', $event)"
    @pagination="emit('pagination', $event)"
    >
      <template #actions>
      <slot name="actions"></slot>
    </template>
    <template
      #table="{
        sortState,
        sortableColumns,
        sortableColumnMap,
        sortColumnAttrs,
        handleSortChange: handleTemplateSortChange
      }"
    >
      <slot
        name="table"
        :sort-state="sortState"
        :sortable-columns="sortableColumns"
        :sortable-column-map="sortableColumnMap"
        :sort-column-attrs="sortColumnAttrs"
        :handle-sort-change="handleTemplateSortChange"
      ></slot>
    </template>
  </UnifiedListTemplate>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import type {
  TableQuickFilterDefinition,
  TableQuickFilterOperator
} from '@/hooks/web/useTableQuickFilter'
import type {
  ListMultiFilterDefinition,
  ListMultiFilterState
} from '@/hooks/web/useTableMultiFilter'
import type { UserTableColumnState } from '@/hooks/web/useUserTableColumns'

defineOptions({ name: 'ScheduleOrderMainList' })

type ScheduleOrderSortOrder = 'ascending' | 'descending' | null

type ScheduleOrderSortState = {
  key?: string
  prop?: string
  order?: ScheduleOrderSortOrder
}

type ScheduleOrderSortableColumn = {
  key: string
  prop: string
  sortable?: boolean | 'custom'
  sortOrders?: ScheduleOrderSortOrder[]
}

type ScheduleOrderSortColumnAttrs = (columnKeyOrConfig: string | {
  key: string
  prop?: string
  sortable?: boolean | 'custom'
  sortOrders?: ScheduleOrderSortOrder[]
}) => {
  sortable: boolean | 'custom'
  sortOrders: ScheduleOrderSortOrder[]
}

type ScheduleOrderSortChangeHandler = (payload?: {
  prop?: string
  order?: string | null
  column?: any
}) => void

defineSlots<{
  actions?: () => any
  table?: (props: {
    sortState: ScheduleOrderSortState
    sortableColumns: ScheduleOrderSortableColumn[]
    sortableColumnMap: Map<string, ScheduleOrderSortableColumn>
    sortColumnAttrs: ScheduleOrderSortColumnAttrs
    handleSortChange: ScheduleOrderSortChangeHandler
  }) => any
}>()

defineProps({
  queryModel: { type: Object as PropType<Record<string, unknown>>, required: true },
  filterDefinitions: { type: Array as PropType<TableQuickFilterDefinition[]>, required: true },
  quickFilterState: { type: Object as PropType<Record<string, unknown>>, required: true },
  selectedFilterDefinition: {
    type: Object as PropType<TableQuickFilterDefinition | undefined>,
    default: undefined
  },
  operatorOptions: { type: Array as PropType<TableQuickFilterOperator[]>, required: true },
  showMultiFilter: { type: Boolean, default: false },
  multiFilterDefinitions: { type: Array as PropType<ListMultiFilterDefinition[]>, default: () => [] },
  multiFilterState: {
    type: Object as PropType<ListMultiFilterState>,
    default: () => ({ conditions: [] })
  },
  showMultiFilterOperators: { type: Boolean, default: true },
  columns: { type: Array as PropType<UserTableColumnState[]>, required: true },
  columnSaving: { type: Boolean, required: true },
  sortState: { type: Object as PropType<ScheduleOrderSortState>, default: () => ({}) },
  total: { type: Number, required: true }
})

const emit = defineEmits([
  'update:page',
  'update:limit',
  'update:quickFilterState',
  'quickFilterQuery',
  'update:multiFilterState',
  'multiFilterQuery',
  'multiFilterReset',
  'multiFilterRemove',
  'columnChange',
  'columnReset',
  'update:sortState',
  'sortChange',
  'pagination'
])

const getPage = (queryModel: Record<string, unknown>) => Number(queryModel.pageNo || 1)
const getLimit = (queryModel: Record<string, unknown>) => Number(queryModel.pageSize || 10)
</script>
