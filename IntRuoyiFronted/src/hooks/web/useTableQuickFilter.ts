import { computed, reactive, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { ElMessage } from 'element-plus'
import {
  normalizeMultiFilterCondition,
  type ListMultiFilterCondition,
  type ListMultiFilterDefinition,
  type ListMultiFilterOperator
} from '@/hooks/web/useTableMultiFilter'

export type TableQuickFilterFieldType = 'text' | 'select' | 'dateRange' | 'autocomplete'
export type TableQuickFilterOperator = 'contains' | 'eq' | 'between'

export interface TableQuickFilterOption {
  label: string
  value: string | number | boolean
}

export interface TableQuickFilterSuggestion {
  label?: string
  value: string
  [key: string]: unknown
}

export interface TableQuickFilterDefinition {
  key: string
  label: string
  type: TableQuickFilterFieldType
  queryParamKey?: string
  operators?: TableQuickFilterOperator[]
  options?: readonly TableQuickFilterOption[]
  placeholder?: string
  triggerOnFocus?: boolean
  popperClass?: string
  fetchSuggestions?: (
    queryString: string,
    callback: (items: TableQuickFilterSuggestion[]) => void
  ) => void | Promise<void>
}

export interface TableQuickFilterValue {
  fieldKey: string
  operator: TableQuickFilterOperator
  value?: string | number | boolean
  valueEnd?: string | number | boolean
}

export type TableQuickFilterQueryParams = Record<string, any> & {
  pageNo?: number
  quickFilter?: TableQuickFilterValue
}

export interface TableQuickFilterState {
  fieldKey?: string
  operator?: TableQuickFilterOperator
  value?: string | number | boolean | Array<string | number>
  conditions?: ListMultiFilterCondition[]
  activeConditionId?: string
}

const DEFAULT_OPERATORS: Record<TableQuickFilterFieldType, TableQuickFilterOperator[]> = {
  text: ['contains', 'eq'],
  autocomplete: ['contains', 'eq'],
  select: ['eq'],
  dateRange: ['between']
}

const DEFAULT_OPERATOR: Record<TableQuickFilterFieldType, TableQuickFilterOperator> = {
  text: 'contains',
  autocomplete: 'contains',
  select: 'eq',
  dateRange: 'between'
}

const isEmptyQuickFilterValue = (value: unknown) =>
  value === undefined || value === null || (typeof value === 'string' && value.trim() === '')

const normalizeScalarValue = (value: unknown) => {
  if (typeof value === 'string') {
    const trimmed = value.trim()
    return trimmed || undefined
  }
  return value as string | number | boolean | undefined
}

const toMultiFilterDefinition = (
  definition: TableQuickFilterDefinition
): ListMultiFilterDefinition => ({
  ...definition,
  type: definition.type,
  operators: definition.operators as ListMultiFilterOperator[] | undefined
})

const isConditionTabsState = (state: TableQuickFilterState) => Array.isArray(state.conditions)

const getRangeValues = (condition: Partial<ListMultiFilterCondition>) => {
  if (Array.isArray(condition.value)) {
    return [condition.value[0], condition.value[1]]
  }
  return [condition.value, condition.valueEnd]
}

const hasAnyRangeValue = (condition: Partial<ListMultiFilterCondition>) => {
  const [value, valueEnd] = getRangeValues(condition)
  return !isEmptyQuickFilterValue(value) || !isEmptyQuickFilterValue(valueEnd)
}

const hasCompleteRangeValue = (condition: Partial<ListMultiFilterCondition>) => {
  const [value, valueEnd] = getRangeValues(condition)
  return !isEmptyQuickFilterValue(value) && !isEmptyQuickFilterValue(valueEnd)
}

export const useTableQuickFilter = <T extends TableQuickFilterQueryParams>(
  tableKey: string,
  filterDefinitions: MaybeRefOrGetter<TableQuickFilterDefinition[]>,
  queryParams: T,
  reload: () => void | Promise<void>
) => {
  const definitions = computed(() => toValue(filterDefinitions))
  const state = reactive<TableQuickFilterState>({
    fieldKey: definitions.value[0]?.key,
    operator: definitions.value[0] ? DEFAULT_OPERATOR[definitions.value[0].type] : undefined,
    value: undefined
  })

  const selectedDefinition = computed(() =>
    definitions.value.find((definition) => definition.key === state.fieldKey)
  )

  const operatorOptions = computed(() => {
    const definition = selectedDefinition.value
    if (!definition) return []
    return definition.operators || DEFAULT_OPERATORS[definition.type]
  })

  const multiDefinitions = computed(() => definitions.value.map(toMultiFilterDefinition))

  const multiDefinitionMap = computed(() => {
    const map = new Map<string, ListMultiFilterDefinition>()
    multiDefinitions.value.forEach((definition) => map.set(definition.key, definition))
    return map
  })

  const activeConditionTabs = computed<ListMultiFilterCondition[]>(() => {
    const conditions: ListMultiFilterCondition[] = []
    for (const condition of state.conditions || []) {
      const definition = multiDefinitionMap.value.get(condition.key)
      if (!definition) continue
      const normalizedCondition = normalizeMultiFilterCondition(definition, condition)
      if (normalizedCondition) {
        conditions.push(normalizedCondition)
      }
    }
    return conditions
  })

  const quickFilter = computed<TableQuickFilterValue | undefined>(() => {
    const definition = selectedDefinition.value
    if (!definition || !state.fieldKey || !state.operator) return undefined
    if (definition.type === 'dateRange') {
      const range = Array.isArray(state.value) ? state.value : []
      const value = normalizeScalarValue(range[0])
      const valueEnd = normalizeScalarValue(range[1])
      if (isEmptyQuickFilterValue(value) || isEmptyQuickFilterValue(valueEnd)) return undefined
      return {
        fieldKey: state.fieldKey,
        operator: 'between',
        value,
        valueEnd
      }
    }
    const value = normalizeScalarValue(state.value)
    if (isEmptyQuickFilterValue(value)) return undefined
    return {
      fieldKey: state.fieldKey,
      operator: state.operator,
      value
    }
  })

  const resetValueForField = () => {
    const definition = selectedDefinition.value
    state.value = definition?.type === 'dateRange' ? [] : undefined
    state.operator = definition ? DEFAULT_OPERATOR[definition.type] : undefined
  }

  watch(
    () => state.fieldKey,
    () => resetValueForField()
  )

  watch(
    definitions,
    (currentDefinitions) => {
      if (!currentDefinitions.some((definition) => definition.key === state.fieldKey)) {
        state.fieldKey = currentDefinitions[0]?.key
      }
      resetValueForField()
    },
    { deep: true }
  )

  const validate = () => {
    if (!tableKey) {
      ElMessage.error('快速过滤表格标识缺失，请联系管理员。')
      return false
    }
    const definition = selectedDefinition.value
    if (!definition) {
      ElMessage.warning('请选择快速过滤字段。')
      return false
    }
    if (!state.operator || !operatorOptions.value.includes(state.operator)) {
      ElMessage.warning('请选择合法过滤条件。')
      return false
    }
    if (definition.type === 'dateRange') {
      const range = Array.isArray(state.value) ? state.value : []
      if (!range[0] || !range[1]) {
        ElMessage.warning('请选择完整日期范围。')
        return false
      }
      return true
    }
    if (isEmptyQuickFilterValue(state.value)) {
      ElMessage.warning('请输入快速过滤值。')
      return false
    }
    return true
  }

  const isQuickFilterInputEmpty = () => {
    if (!tableKey) return false
    const definition = selectedDefinition.value
    if (!definition) return false
    if (definition.type === 'dateRange') {
      const range = Array.isArray(state.value) ? state.value : []
      return isEmptyQuickFilterValue(range[0]) && isEmptyQuickFilterValue(range[1])
    }

    return isEmptyQuickFilterValue(state.value)
  }

  const clearQuickFilterParams = () => {
    const queryParamTarget = queryParams as TableQuickFilterQueryParams
    delete queryParamTarget.quickFilter
    delete queryParamTarget.multiFilters
    definitions.value.forEach((definition) => {
      if (definition.queryParamKey) {
        delete queryParamTarget[definition.queryParamKey]
      }
    })
  }

  const resetQuickFilter = async () => {
    state.fieldKey = definitions.value[0]?.key
    resetValueForField()
    clearQuickFilterParams()
    queryParams.pageNo = 1
    await reload()
  }

  const validateConditionTabs = () => {
    if (!tableKey) {
      ElMessage.error('标准列表筛选表格标识缺失，请联系管理员。')
      return false
    }

    const seenParamKeys = new Map<string, string>()
    let unmappedConditionCount = 0
    for (const condition of state.conditions || []) {
      const definition = multiDefinitionMap.value.get(condition.key)
      if (!definition) {
        ElMessage.warning('存在未注册的筛选字段，请刷新页面后重试。')
        return false
      }

      const operator = condition.operator || DEFAULT_OPERATOR[definition.type as TableQuickFilterFieldType]
      if (!((definition.operators as ListMultiFilterOperator[] | undefined) || DEFAULT_OPERATORS[definition.type as TableQuickFilterFieldType]).includes(operator)) {
        ElMessage.warning(`${definition.label} 的筛选条件不合法。`)
        return false
      }

      if (definition.type === 'dateRange' || operator === 'between') {
        if (hasAnyRangeValue(condition) && !hasCompleteRangeValue(condition)) {
          ElMessage.warning(`请完整填写${definition.label}的起止范围。`)
          return false
        }
      }

      const normalizedCondition = normalizeMultiFilterCondition(definition, condition)
      if (!normalizedCondition) continue
      if (!definition.queryParamKey) {
        unmappedConditionCount += 1
        continue
      }
      const previousLabel = seenParamKeys.get(definition.queryParamKey)
      if (previousLabel) {
        ElMessage.warning(`${definition.label} 已存在筛选条件，请先删除重复条件 Tab。`)
        return false
      }
      seenParamKeys.set(definition.queryParamKey, definition.label)
    }

    if (unmappedConditionCount > 1) {
      ElMessage.warning('当前列表存在多个未映射正式参数的筛选条件，请先为页面补齐正式 query 参数。')
      return false
    }

    return true
  }

  const toQuickFilterValue = (condition: ListMultiFilterCondition): TableQuickFilterValue => {
    if (condition.valueEnd !== undefined) {
      return {
        fieldKey: condition.key,
        operator: 'between',
        value: condition.value as string | number | boolean | undefined,
        valueEnd: condition.valueEnd as string | number | boolean | undefined
      }
    }
    return {
      fieldKey: condition.key,
      operator: condition.operator as TableQuickFilterOperator,
      value: condition.value as string | number | boolean | undefined
    }
  }

  const applyConditionTabsFilter = async () => {
    if (!validateConditionTabs()) return
    if (activeConditionTabs.value.length === 0) {
      await resetQuickFilter()
      return
    }

    clearQuickFilterParams()
    const queryParamTarget = queryParams as TableQuickFilterQueryParams
    const unmappedConditions: TableQuickFilterValue[] = []
    for (const condition of activeConditionTabs.value) {
      const definition = multiDefinitionMap.value.get(condition.key)
      if (!definition) continue
      if (definition.queryParamKey) {
        queryParamTarget[definition.queryParamKey] =
          condition.valueEnd !== undefined ? [condition.value, condition.valueEnd] : condition.value
      } else {
        unmappedConditions.push(toQuickFilterValue(condition))
      }
    }
    if (unmappedConditions.length === 1) {
      queryParams.quickFilter = unmappedConditions[0]
    }
    queryParams.pageNo = 1
    await reload()
  }

  const applyQuickFilter = async () => {
    if (isConditionTabsState(state)) {
      await applyConditionTabsFilter()
      return
    }
    if (isQuickFilterInputEmpty()) {
      await resetQuickFilter()
      return
    }
    if (!validate()) return
    const definition = selectedDefinition.value
    clearQuickFilterParams()
    if (definition?.queryParamKey) {
      const queryParamTarget = queryParams as TableQuickFilterQueryParams
      queryParamTarget[definition.queryParamKey] =
        definition.type === 'dateRange'
          ? [quickFilter.value?.value, quickFilter.value?.valueEnd]
          : quickFilter.value?.value
    } else {
      queryParams.quickFilter = quickFilter.value
    }
    queryParams.pageNo = 1
    await reload()
  }

  const updateState = (nextState: Partial<typeof state>) => {
    if ('fieldKey' in nextState) {
      state.fieldKey = nextState.fieldKey
    }
    if ('operator' in nextState) {
      state.operator = nextState.operator
    }
    if ('value' in nextState) {
      state.value = nextState.value
    }
    if ('conditions' in nextState) {
      state.conditions = nextState.conditions ? [...nextState.conditions] : undefined
    }
    if ('activeConditionId' in nextState) {
      state.activeConditionId = nextState.activeConditionId
    }
  }

  return {
    tableKey,
    filterDefinitions: definitions,
    state,
    selectedDefinition,
    operatorOptions,
    quickFilter,
    applyQuickFilter,
    resetQuickFilter,
    updateState
  }
}
