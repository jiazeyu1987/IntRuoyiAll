<template>
  <UnifiedListTemplate
    table-key="mes.pro.schedulerWorkbench.processWip"
    :query-model="queryModel"
    label-width="88px"
    :filter-definitions="filterDefinitions"
    :show-quick-filter-label="false"
    :quick-filter-state="quickFilterState"
    :selected-filter-definition="selectedFilterDefinition"
    :operator-options="operatorOptions"
    :columns="columns"
    :column-saving="columnSaving"
    :sort-state="sortState"
    :total="total"
    :page="getPage(queryModel)"
    :limit="getLimit(queryModel)"
    @update:page="emit('update:page', $event)"
    @update:limit="emit('update:limit', $event)"
    @update:quick-filter-state="emit('update:quickFilterState', $event)"
    @quick-filter-query="emit('quickFilterQuery')"
    @column-change="emit('columnChange', $event)"
    @column-reset="emit('columnReset')"
    @update:sort-state="emit('update:sortState', $event)"
    @sort-change="emit('sortChange', $event)"
    @pagination="emit('pagination', $event)"
    >
      <template #actions>
      <slot name="actions"></slot>
    </template>
    <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
      <slot
        name="table"
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
import type { UserTableColumnState } from '@/hooks/web/useUserTableColumns'

defineOptions({ name: 'ProcessWipTable' })

defineProps({
  queryModel: { type: Object as PropType<Record<string, unknown>>, required: true },
  filterDefinitions: { type: Array as PropType<TableQuickFilterDefinition[]>, required: true },
  quickFilterState: { type: Object as PropType<Record<string, unknown>>, required: true },
  selectedFilterDefinition: {
    type: Object as PropType<TableQuickFilterDefinition | undefined>,
    default: undefined
  },
  operatorOptions: { type: Array as PropType<TableQuickFilterOperator[]>, required: true },
  columns: { type: Array as PropType<UserTableColumnState[]>, required: true },
  columnSaving: { type: Boolean, required: true },
  sortState: { type: Object as PropType<Record<string, unknown>>, default: () => ({}) },
  total: { type: Number, required: true }
})

const emit = defineEmits([
  'update:page',
  'update:limit',
  'update:quickFilterState',
  'quickFilterQuery',
  'columnChange',
  'columnReset',
  'update:sortState',
  'sortChange',
  'pagination'
])

const getPage = (queryModel: Record<string, unknown>) => Number(queryModel.pageNo || 1)
const getLimit = (queryModel: Record<string, unknown>) => Number(queryModel.pageSize || 10)
</script>
