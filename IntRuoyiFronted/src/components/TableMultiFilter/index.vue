<template>
  <div class="table-multi-filter" :data-table-key="tableKey">
    <div class="table-multi-filter__bar">
      <TableMultiFilterField
        v-for="definition in visibleDefinitions"
        :key="definition.key"
        :definition="definition"
        :condition="getConditionForDefinition(definition.key)"
        :show-operator="showOperators"
        @update="updateCondition"
        @query="onQuery"
      />

      <el-popover
        v-if="hiddenDefinitions.length > 0"
        placement="bottom-start"
        width="640"
        trigger="click"
        popper-class="table-multi-filter__popover"
      >
        <template #reference>
          <el-button class="table-multi-filter__more-button">
            更多筛选
          </el-button>
        </template>
        <div class="table-multi-filter__more-panel">
          <TableMultiFilterField
            v-for="definition in hiddenDefinitions"
            :key="definition.key"
            :definition="definition"
            :condition="getConditionForDefinition(definition.key)"
            :show-operator="showOperators"
            @update="updateCondition"
            @query="onQuery"
          />
        </div>
      </el-popover>

      <el-button type="primary" @click="onQuery">
        <Icon icon="ep:search" class="mr-5px" />
        查询
      </el-button>
      <el-button @click="clearAllConditions">
        <Icon icon="ep:refresh-left" class="mr-5px" />
        重置
      </el-button>
    </div>

    <div v-if="activeChips.length > 0" class="table-multi-filter__chips">
      <el-tag
        v-for="chip in activeChips"
        :key="chip.key"
        class="table-multi-filter__chip"
        closable
        @close="removeCondition(chip.key)"
      >
        {{ chip.label }}
      </el-tag>
      <el-button link type="primary" @click="clearAllConditions">清空筛选</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import TableMultiFilterField from './MultiFilterField.vue'
import {
  normalizeMultiFilterCondition,
  type ListMultiFilterCondition,
  type ListMultiFilterDefinition,
  type ListMultiFilterScalar,
  type ListMultiFilterState
} from '@/hooks/web/useTableMultiFilter'

defineOptions({ name: 'TableMultiFilter' })

const props = withDefaults(defineProps<{
  tableKey: string
  filterDefinitions: ListMultiFilterDefinition[]
  state: ListMultiFilterState
  showOperators?: boolean
  maxInlineFilters?: number
}>(), {
  showOperators: true,
  maxInlineFilters: 4
})

const emit = defineEmits<{
  'update:state': [state: ListMultiFilterState]
  query: []
  reset: []
  remove: [key: string]
}>()

const visibleDefinitions = computed(() => {
  const maxInlineFilters = Math.max(1, props.maxInlineFilters)
  return props.filterDefinitions.filter(
    (definition, index) =>
      definition.defaultVisible === true ||
      (definition.defaultVisible !== false && index < maxInlineFilters)
  )
})

const hiddenDefinitions = computed(() => {
  const visibleKeys = new Set(visibleDefinitions.value.map((definition) => definition.key))
  return props.filterDefinitions.filter((definition) => !visibleKeys.has(definition.key))
})

const getConditionForDefinition = (key: string) =>
  props.state.conditions.find((condition) => condition.key === key)

const sortConditionsByDefinition = (conditions: ListMultiFilterCondition[]) => {
  const definitionOrder = new Map<string, number>()
  props.filterDefinitions.forEach((definition, index) => definitionOrder.set(definition.key, index))
  return [...conditions].sort(
    (left, right) =>
      (definitionOrder.get(left.key) ?? Number.MAX_SAFE_INTEGER) -
      (definitionOrder.get(right.key) ?? Number.MAX_SAFE_INTEGER)
  )
}

const updateCondition = (condition: ListMultiFilterCondition) => {
  const nextConditions = sortConditionsByDefinition([
    ...props.state.conditions.filter((currentCondition) => currentCondition.key !== condition.key),
    condition
  ])
  emit('update:state', { conditions: nextConditions })
}

const removeCondition = (key: string) => {
  emit('update:state', {
    conditions: props.state.conditions.filter((condition) => condition.key !== key)
  })
  emit('remove', key)
}

const clearAllConditions = () => {
  emit('update:state', { conditions: [] })
  emit('reset')
}

const onQuery = () => {
  if (!props.tableKey) {
    ElMessage.error('多维度筛选表格标识缺失，请联系管理员。')
    return
  }
  emit('query')
}

const formatOptionValue = (definition: ListMultiFilterDefinition, value: ListMultiFilterScalar) => {
  const option = definition.options?.find((item) => item.value === value)
  return option?.label || String(value)
}

const formatConditionValue = (
  definition: ListMultiFilterDefinition,
  condition: ListMultiFilterCondition
) => {
  if (Array.isArray(condition.value)) {
    return condition.value.map((value) => formatOptionValue(definition, value)).join('、')
  }
  if (condition.valueEnd !== undefined) {
    return `${formatOptionValue(definition, condition.value as ListMultiFilterScalar)} 至 ${formatOptionValue(
      definition,
      condition.valueEnd
    )}`
  }
  return formatOptionValue(definition, condition.value as ListMultiFilterScalar)
}

const activeChips = computed(() => {
  const chips: Array<{ key: string; label: string }> = []
  for (const condition of props.state.conditions) {
    const definition = props.filterDefinitions.find((item) => item.key === condition.key)
    if (!definition) continue
    const normalizedCondition = normalizeMultiFilterCondition(definition, condition)
    if (!normalizedCondition) continue
    chips.push({
      key: condition.key,
      label: `${definition.label}: ${formatConditionValue(definition, normalizedCondition)}`
    })
  }
  return chips
})
</script>

<style scoped>
.table-multi-filter {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
}

.table-multi-filter__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.table-multi-filter__more-button {
  white-space: nowrap;
}

.table-multi-filter__more-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.table-multi-filter__chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.table-multi-filter__chip {
  max-width: 360px;
}

@media (max-width: 1360px) {
  .table-multi-filter__more-panel {
    grid-template-columns: 1fr;
  }
}
</style>
