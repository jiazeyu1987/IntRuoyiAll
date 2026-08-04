<template>
  <div class="table-multi-filter-field" :data-filter-key="definition.key">
    <span class="table-multi-filter-field__label">{{ definition.label }}</span>
    <el-select
      v-if="showOperator && operatorOptions.length > 1"
      :model-value="currentOperator"
      class="table-multi-filter-field__operator"
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
      v-if="definition.type === 'dateRange'"
      :model-value="rangeValue"
      class="table-multi-filter-field__value table-multi-filter-field__value--date"
      type="daterange"
      value-format="YYYY-MM-DD"
      range-separator="至"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      @update:model-value="updateValue"
      @keyup.enter="$emit('query')"
    />
    <el-select
      v-else-if="definition.type === 'select'"
      :model-value="condition?.value"
      class="table-multi-filter-field__value"
      clearable
      filterable
      :placeholder="definition.placeholder || '请选择'"
      @update:model-value="updateValue"
      @keyup.enter="$emit('query')"
    >
      <el-option
        v-for="option in definition.options || []"
        :key="String(option.value)"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
    <el-select
      v-else-if="definition.type === 'multiSelect'"
      :model-value="multiSelectValue"
      class="table-multi-filter-field__value"
      multiple
      clearable
      filterable
      collapse-tags
      collapse-tags-tooltip
      :placeholder="definition.placeholder || '请选择'"
      @update:model-value="updateValue"
      @keyup.enter="$emit('query')"
    >
      <el-option
        v-for="option in definition.options || []"
        :key="String(option.value)"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
    <el-autocomplete
      v-else-if="definition.type === 'autocomplete'"
      :model-value="condition?.value"
      class="table-multi-filter-field__value"
      clearable
      :fetch-suggestions="definition.fetchSuggestions"
      :trigger-on-focus="definition.triggerOnFocus === true"
      :popper-class="definition.popperClass || 'table-multi-filter-autocomplete-popper'"
      :placeholder="definition.placeholder || '请输入关键字'"
      :title="getValueTitle(condition?.value)"
      @update:model-value="updateValue"
      @keyup.enter="$emit('query')"
      @select="handleAutocompleteSelect"
    />
    <div v-else-if="definition.type === 'numberRange'" class="table-multi-filter-field__range">
      <el-input-number
        :model-value="startNumberValue"
        class="table-multi-filter-field__number"
        controls-position="right"
        placeholder="最小值"
        @update:model-value="(value) => updateRangeValue(0, value)"
        @keyup.enter="$emit('query')"
      />
      <span class="table-multi-filter-field__range-separator">至</span>
      <el-input-number
        :model-value="endNumberValue"
        class="table-multi-filter-field__number"
        controls-position="right"
        placeholder="最大值"
        @update:model-value="(value) => updateRangeValue(1, value)"
        @keyup.enter="$emit('query')"
      />
    </div>
    <el-input
      v-else
      :model-value="condition?.value"
      class="table-multi-filter-field__value"
      clearable
      :placeholder="definition.placeholder || '请输入关键字'"
      :title="getValueTitle(condition?.value)"
      @update:model-value="updateValue"
      @keyup.enter="$emit('query')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  getDefaultMultiFilterOperator,
  getMultiFilterOperatorOptions,
  type ListMultiFilterCondition,
  type ListMultiFilterDefinition,
  type ListMultiFilterOperator,
  type ListMultiFilterScalar,
  type ListMultiFilterSuggestion,
  type ListMultiFilterValue
} from '@/hooks/web/useTableMultiFilter'

defineOptions({ name: 'TableMultiFilterField' })

const props = withDefaults(defineProps<{
  definition: ListMultiFilterDefinition
  condition?: Partial<ListMultiFilterCondition>
  showOperator?: boolean
}>(), {
  showOperator: true
})

const emit = defineEmits<{
  update: [condition: ListMultiFilterCondition]
  query: []
}>()

const OPERATOR_LABELS: Record<ListMultiFilterOperator, string> = {
  contains: '包含',
  eq: '等于',
  in: '属于',
  between: '介于',
  gte: '大于等于',
  lte: '小于等于'
}

const operatorOptions = computed(() => getMultiFilterOperatorOptions(props.definition))

const currentOperator = computed(() =>
  props.condition?.operator || getDefaultMultiFilterOperator(props.definition)
)

const rangeValue = computed(() => {
  if (Array.isArray(props.condition?.value)) {
    return props.condition?.value
  }
  return [props.condition?.value, props.condition?.valueEnd].filter(
    (value) => value !== undefined && value !== null
  )
})

const multiSelectValue = computed(() =>
  Array.isArray(props.condition?.value) ? props.condition?.value : []
)

const startNumberValue = computed(() => {
  const value = rangeValue.value[0]
  return typeof value === 'number' ? value : undefined
})

const endNumberValue = computed(() => {
  const value = rangeValue.value[1]
  return typeof value === 'number' ? value : undefined
})

const getOperatorLabel = (operator: ListMultiFilterOperator) => OPERATOR_LABELS[operator] || operator

const getValueTitle = (value: unknown) => {
  if (Array.isArray(value)) {
    return value.map((item) => String(item || '').trim()).filter(Boolean).join('、')
  }
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean'
    ? String(value)
    : undefined
}

const emitCondition = (patch: Partial<ListMultiFilterCondition>) => {
  emit('update', {
    key: props.definition.key,
    operator: currentOperator.value,
    ...props.condition,
    ...patch
  })
}

const updateOperator = (operator: ListMultiFilterOperator) => {
  emitCondition({ operator })
}

const updateValue = (value?: ListMultiFilterValue) => {
  emitCondition({ value })
}

const updateRangeValue = (index: 0 | 1, value?: ListMultiFilterScalar) => {
  const nextRange = [...rangeValue.value]
  nextRange[index] = value
  emitCondition({
    value: nextRange as ListMultiFilterScalar[],
    valueEnd: nextRange[1] as ListMultiFilterScalar | undefined
  })
}

const handleAutocompleteSelect = (item: ListMultiFilterSuggestion) => {
  emitCondition({ value: item.value })
  emit('query')
}
</script>

<style scoped>
.table-multi-filter-field {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
}

.table-multi-filter-field__label {
  color: #172033;
  font-weight: 600;
  white-space: nowrap;
}

.table-multi-filter-field__operator {
  flex: 0 0 92px;
  min-width: 92px;
  width: 92px;
}

.table-multi-filter-field__value {
  flex: 0 0 220px;
  min-width: 220px;
  width: 220px;
}

.table-multi-filter-field__value--date {
  flex-basis: 260px;
  min-width: 260px;
  width: 260px;
}

.table-multi-filter-field__range {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.table-multi-filter-field__number {
  width: 118px;
}

.table-multi-filter-field__range-separator {
  color: #5f6b7a;
  white-space: nowrap;
}
</style>

<style>
.table-multi-filter-autocomplete-popper {
  min-width: 320px;
  max-width: min(560px, calc(100vw - 32px));
}

.table-multi-filter-autocomplete-popper .el-autocomplete-suggestion__wrap {
  max-height: 320px;
}

.table-multi-filter-autocomplete-popper .el-autocomplete-suggestion__list li {
  height: auto;
  min-height: 34px;
  line-height: 20px;
  padding: 7px 12px;
  white-space: normal;
  overflow-wrap: anywhere;
}
</style>
