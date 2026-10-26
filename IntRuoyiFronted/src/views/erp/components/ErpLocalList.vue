<template>
  <UnifiedListTemplate
    :show-filter-reset="false"
    :table-key="tableKey"
    single-line-toolbar
    :query-model="query"
    :filter-definitions="filterDefinitions"
    :quick-filter-state="quickFilter.state"
    :selected-filter-definition="quickFilter.selectedDefinition.value"
    :operator-options="quickFilter.operatorOptions.value"
    :columns="columns"
    :column-saving="saving"
    :total="filteredRows.length"
    v-model:page="query.pageNo"
    v-model:limit="query.pageSize"
    @update:quick-filter-state="quickFilter.updateState"
    @quick-filter-query="quickFilter.applyQuickFilter"
    @column-change="saveConfig"
  >
    <template #actions><slot name="actions"></slot></template>
    <template #table>
      <slot
        :rows="pageRows"
        :is-column-visible="isColumnVisible"
        :get-column-width-string="getColumnWidthString"
        :handle-header-dragend="handleHeaderDragend"
        :column-loading="loading"
      ></slot>
    </template>
  </UnifiedListTemplate>
</template>
<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'

const props = defineProps<{
  tableKey: string
  rows: Record<string, any>[]
  defaultColumns: UserTableColumnDefinition[]
  filterDefinitions: TableQuickFilterDefinition[]
}>()
const query = reactive<Record<string, any> & { pageNo: number; pageSize: number }>({
  pageNo: 1,
  pageSize: 20
})
const {
  columns,
  loading,
  saving,
  isColumnVisible,
  getColumnWidthString,
  handleHeaderDragend,
  saveConfig
} = useUserTableColumns(props.tableKey, props.defaultColumns)
const quickFilter = useTableQuickFilter(
  props.tableKey,
  () => props.filterDefinitions,
  query,
  () => {
    query.pageNo = 1
  }
)
const filteredRows = computed(() =>
  props.rows.filter((row) =>
    props.filterDefinitions.every((field) => {
      const value = query[field.queryParamKey || field.key]
      if (value === undefined || value === '') return true
      return String(row[field.key]).toLocaleLowerCase().includes(String(value).toLocaleLowerCase())
    })
  )
)
const pageRows = computed(() =>
  filteredRows.value.slice((query.pageNo - 1) * query.pageSize, query.pageNo * query.pageSize)
)
watch(
  () => filteredRows.value.length,
  (total) => {
    const lastPage = Math.max(1, Math.ceil(total / query.pageSize))
    if (query.pageNo > lastPage) query.pageNo = lastPage
  }
)
</script>
