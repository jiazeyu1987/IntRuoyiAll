<template>
  <div class="table-quick-filter" :data-table-key="tableKey">
    <span v-if="showLabel" class="table-quick-filter__label">快速过滤</span>
    <el-select
      :model-value="state.fieldKey"
      class="table-quick-filter__field"
      filterable
      placeholder="字段"
      @update:model-value="updateFieldKey"
    >
      <el-option
        v-for="field in filterDefinitions"
        :key="field.key"
        :label="field.label"
        :value="field.key"
      />
    </el-select>
    <el-select
      :model-value="state.operator"
      class="table-quick-filter__operator"
      placeholder="条件"
      @update:model-value="updateOperator"
    >
      <el-option
        v-for="operator in operatorOptions"
        :key="operator"
        :label="getOperatorLabel(operator)"
        :value="operator"
      />
    </el-select>
    <el-date-picker
      v-if="selectedDefinition?.type === 'dateRange'"
      :model-value="state.value"
      class="table-quick-filter__value table-quick-filter__value--date"
      type="daterange"
      value-format="YYYY-MM-DD"
      range-separator="至"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      @update:model-value="updateValue"
      @keyup.enter="onQuery"
    />
    <el-select
      v-else-if="selectedDefinition?.type === 'select'"
      :model-value="state.value"
      class="table-quick-filter__value"
      clearable
      filterable
      placeholder="请选择值"
      @update:model-value="updateValue"
      @keyup.enter="onQuery"
    >
      <el-option
        v-for="option in selectedDefinition.options || []"
        :key="String(option.value)"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
    <el-autocomplete
      v-else-if="selectedDefinition?.type === 'autocomplete'"
      :model-value="state.value"
      class="table-quick-filter__value"
      clearable
      :fetch-suggestions="selectedDefinition.fetchSuggestions"
      :trigger-on-focus="false"
      :placeholder="selectedDefinition.placeholder || '请输入关键字'"
      @update:model-value="updateValue"
      @keyup.enter="onQuery"
      @select="handleAutocompleteSelect"
    />
    <el-input
      v-else
      :model-value="state.value"
      class="table-quick-filter__value"
      clearable
      :placeholder="selectedDefinition?.placeholder || '请输入过滤值'"
      @update:model-value="updateValue"
      @keyup.enter="onQuery"
    />
    <el-button type="primary" @click="onQuery">
      <Icon icon="ep:search" class="mr-5px" />
      查询
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  TableQuickFilterDefinition,
  TableQuickFilterOperator,
  TableQuickFilterSuggestion
} from '@/hooks/web/useTableQuickFilter'

const props = withDefaults(defineProps<{
  tableKey: string
  filterDefinitions: TableQuickFilterDefinition[]
  showLabel?: boolean
  state: {
    fieldKey?: string
    operator?: TableQuickFilterOperator
    value?: string | number | boolean | Array<string | number>
  }
  selectedDefinition?: TableQuickFilterDefinition
  operatorOptions: TableQuickFilterOperator[]
}>(), {
  showLabel: true
})

const showLabel = computed(() => props.showLabel !== false)

const emit = defineEmits<{
  'update:state': [
    state: {
      fieldKey?: string
      operator?: TableQuickFilterOperator
      value?: string | number | boolean | Array<string | number>
    }
  ]
  query: []
}>()

const OPERATOR_LABELS: Record<TableQuickFilterOperator, string> = {
  contains: '包含',
  eq: '等于',
  between: '介于'
}

const getOperatorLabel = (operator: TableQuickFilterOperator) => OPERATOR_LABELS[operator] || operator

const emitState = (patch: Partial<typeof props.state>) => {
  emit('update:state', {
    ...props.state,
    ...patch
  })
}

const updateFieldKey = (fieldKey?: string) => {
  emitState({ fieldKey })
}

const updateOperator = (operator?: TableQuickFilterOperator) => {
  emitState({ operator })
}

const updateValue = (value?: string | number | boolean | Array<string | number>) => {
  emitState({ value })
}

const handleAutocompleteSelect = (item: TableQuickFilterSuggestion) => {
  emitState({ value: item.value })
  emit('query')
}

const onQuery = () => {
  if (!props.tableKey) {
    ElMessage.error('快速过滤表格标识缺失，请联系管理员。')
    return
  }
  emit('query')
}
</script>

<style scoped>
.table-quick-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
}

.table-quick-filter__label {
  color: #172033;
  font-weight: 600;
  white-space: nowrap;
}

.table-quick-filter__field {
  width: 150px;
}

.table-quick-filter__operator {
  width: 96px;
}

.table-quick-filter__value {
  width: 220px;
}

.table-quick-filter__value--date {
  width: 260px;
}
</style>
