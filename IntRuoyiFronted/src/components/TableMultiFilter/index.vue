<template>
  <div class="table-multi-filter" :data-table-key="tableKey">
    <div class="table-multi-filter__tabs-row">
      <el-button
        class="table-multi-filter__tab-action"
        circle
        :disabled="conditionTabs.length === 0"
        aria-label="删除当前筛选条件"
        @click="removeActiveConditionTab"
      >
        <Icon icon="ep:minus" />
      </el-button>

      <el-tabs
        v-if="conditionTabs.length > 0"
        :model-value="activeConditionId"
        class="table-multi-filter__tabs"
        type="card"
        @tab-change="setActiveConditionId"
      >
        <el-tab-pane
          v-for="(condition, index) in conditionTabs"
          :key="getConditionId(condition, index)"
          :name="getConditionId(condition, index)"
          :label="getTabLabel(condition, index)"
        />
      </el-tabs>
      <div v-else class="table-multi-filter__tabs-empty">暂无筛选条件</div>

      <el-button
        class="table-multi-filter__tab-action"
        circle
        aria-label="新增筛选条件"
        @click="addConditionTab"
      >
        <Icon icon="ep:plus" />
      </el-button>

      <el-tag
        v-if="hasUnappliedChanges"
        class="table-multi-filter__pending-status"
        type="warning"
        effect="plain"
      >
        筛选条件待应用
      </el-tag>
    </div>

    <div v-if="activeCondition && activeDefinition" class="table-multi-filter__condition-row">
      <el-select
        :model-value="activeCondition.key"
        class="table-multi-filter__field-select"
        placeholder="请选择筛选字段"
        @update:model-value="updateActiveDefinition"
      >
        <el-option
          v-for="definition in availableDefinitionsForActiveTab"
          :key="definition.key"
          :label="definition.label"
          :value="definition.key"
        />
      </el-select>

      <TableMultiFilterField
        :definition="activeDefinition"
        :condition="activeCondition"
        :show-label="false"
        :show-operator="showOperators"
        @update="updateCondition"
        @query="onQuery"
      />

      <el-button type="primary" @click="onQuery">
        <Icon icon="ep:search" class="mr-5px" />
        查询
      </el-button>
      <el-button @click="clearAllConditions">
        <Icon icon="ep:refresh-left" class="mr-5px" />
        重置
      </el-button>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import TableMultiFilterField from './MultiFilterField.vue'
import {
  getDefaultMultiFilterOperator,
  hasMultiFilterDraftChanges,
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
  remove: [conditionId: string]
}>()

const conditionTabs = computed(() => props.state.conditions || [])
const hasUnappliedChanges = computed(() =>
  hasMultiFilterDraftChanges(props.filterDefinitions, props.state)
)

const definitionMap = computed(() => {
  const map = new Map<string, ListMultiFilterDefinition>()
  props.filterDefinitions.forEach((definition) => map.set(definition.key, definition))
  return map
})

const getConditionId = (condition: Partial<ListMultiFilterCondition>, index = 0) =>
  condition.id || condition.key || `condition-${index + 1}`

const activeConditionId = computed(() =>
  props.state.activeConditionId ||
  (conditionTabs.value[0] ? getConditionId(conditionTabs.value[0], 0) : '')
)

const activeCondition = computed(() =>
  conditionTabs.value.find(
    (condition, index) => getConditionId(condition, index) === activeConditionId.value
  )
)

const activeDefinition = computed(() =>
  activeCondition.value ? definitionMap.value.get(activeCondition.value.key) : undefined
)

const availableDefinitionsForActiveTab = computed(() => {
  const activeId = activeConditionId.value
  const usedKeys = new Set(
    conditionTabs.value
      .filter((condition, index) => getConditionId(condition, index) !== activeId)
      .map((condition) => condition.key)
  )
  return props.filterDefinitions.filter(
    (definition) => definition.key === activeCondition.value?.key || !usedKeys.has(definition.key)
  )
})

const emitState = (conditions: ListMultiFilterCondition[], activeId?: string) => {
  const normalizedActiveId = activeId || conditions[0]?.id || conditions[0]?.key
  emit('update:state', {
    conditions,
    appliedConditions: props.state.appliedConditions,
    activeConditionId: normalizedActiveId
  })
}

const createConditionId = () => {
  const existingIds = new Set(conditionTabs.value.map((condition, index) => getConditionId(condition, index)))
  let index = conditionTabs.value.length + 1
  while (existingIds.has(`condition-${index}`)) {
    index += 1
  }
  return `condition-${index}`
}

const getNextAvailableDefinition = () => {
  const usedKeys = new Set(conditionTabs.value.map((condition) => condition.key))
  return props.filterDefinitions.find((definition) => !usedKeys.has(definition.key))
}

const addConditionTab = () => {
  const definition = getNextAvailableDefinition()
  if (!definition) {
    ElMessage.warning('已添加所有可用筛选字段。')
    return
  }
  const conditionId = createConditionId()
  emitState(
    [
      ...conditionTabs.value,
      {
        id: conditionId,
        key: definition.key,
        operator: getDefaultMultiFilterOperator(definition)
      }
    ],
    conditionId
  )
}

const setActiveConditionId = (conditionId: string | number) => {
  emitState([...conditionTabs.value], String(conditionId))
}

const removeActiveConditionTab = () => {
  if (!activeConditionId.value) return
  const removedConditionId = activeConditionId.value
  const nextConditions = conditionTabs.value.filter(
    (condition, index) => getConditionId(condition, index) !== removedConditionId
  )
  emitState(nextConditions, nextConditions[0]?.id || nextConditions[0]?.key)
  emit('remove', removedConditionId)
}

const updateActiveDefinition = (key: string | number) => {
  const definition = definitionMap.value.get(String(key))
  if (!definition || !activeCondition.value) return
  const conditionId = activeConditionId.value || createConditionId()
  const nextCondition: ListMultiFilterCondition = {
    id: conditionId,
    key: definition.key,
    operator: getDefaultMultiFilterOperator(definition)
  }
  const nextConditions = conditionTabs.value.map((condition, index) =>
    getConditionId(condition, index) === conditionId ? nextCondition : condition
  )
  emitState(nextConditions, conditionId)
}

const updateCondition = (condition: ListMultiFilterCondition) => {
  const conditionId = condition.id || activeConditionId.value
  if (!conditionId) return
  const nextCondition = { ...condition, id: conditionId }
  const nextConditions = conditionTabs.value.map((currentCondition, index) =>
    getConditionId(currentCondition, index) === conditionId ? nextCondition : currentCondition
  )
  emitState(nextConditions, conditionId)
}

const clearAllConditions = () => {
  emit('update:state', {
    conditions: [],
    appliedConditions: props.state.appliedConditions,
    activeConditionId: undefined
  })
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

const getTabLabel = (condition: ListMultiFilterCondition, index: number) => {
  const definition = definitionMap.value.get(condition.key)
  if (!definition) return `条件${index + 1}`
  const normalizedCondition = normalizeMultiFilterCondition(definition, condition)
  if (!normalizedCondition) return definition.label
  return `${definition.label}: ${formatConditionValue(definition, normalizedCondition)}`
}
</script>

<style scoped>
.table-multi-filter {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
}

.table-multi-filter__tabs-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.table-multi-filter__tab-action {
  flex: 0 0 auto;
}

.table-multi-filter__pending-status {
  flex: 0 0 auto;
}

.table-multi-filter__tabs {
  flex: 1 1 auto;
  min-width: 0;
}

.table-multi-filter__tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.table-multi-filter__tabs :deep(.el-tabs__nav-wrap) {
  min-width: 0;
}

.table-multi-filter__tabs :deep(.el-tabs__item) {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-multi-filter__tabs-empty {
  flex: 1 1 auto;
  min-width: 0;
  height: 36px;
  border: 1px dashed #d6deea;
  border-radius: 6px;
  color: #7b8794;
  line-height: 34px;
  padding: 0 12px;
}

.table-multi-filter__condition-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.table-multi-filter__field-select {
  flex: 0 0 160px;
  min-width: 160px;
  width: 160px;
}

</style>
