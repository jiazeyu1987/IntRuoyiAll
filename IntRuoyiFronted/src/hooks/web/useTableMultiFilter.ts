import { computed, reactive, toValue, type MaybeRefOrGetter } from 'vue'
import { ElMessage } from 'element-plus'

export type ListMultiFilterScalar = string | number | boolean
export type ListMultiFilterValue = ListMultiFilterScalar | ListMultiFilterScalar[]
export type ListMultiFilterFieldType = 'text' | 'select' | 'multiSelect' | 'dateRange' | 'autocomplete' | 'numberRange'
export type ListMultiFilterOperator = 'contains' | 'eq' | 'in' | 'between' | 'gte' | 'lte'

export interface ListMultiFilterOption {
  label: string
  value: ListMultiFilterScalar
}

export interface ListMultiFilterSuggestion {
  label?: string
  value: string
  [key: string]: unknown
}

export interface ListMultiFilterDefinition {
  key: string
  label: string
  type: ListMultiFilterFieldType
  queryParamKey?: string
  queryParamKeys?: [string, string]
  operators?: ListMultiFilterOperator[]
  options?: readonly ListMultiFilterOption[]
  placeholder?: string
  defaultVisible?: boolean
  group?: string
  triggerOnFocus?: boolean
  popperClass?: string
  fetchSuggestions?: (
    queryString: string,
    callback: (items: ListMultiFilterSuggestion[]) => void
  ) => void | Promise<void>
}

export interface ListMultiFilterCondition {
  id?: string
  key: string
  operator: ListMultiFilterOperator
  value?: ListMultiFilterValue
  valueEnd?: ListMultiFilterScalar
}

export interface ListMultiFilterState {
  conditions: ListMultiFilterCondition[]
  activeConditionId?: string
}

export type ListMultiFilterQueryParams = Record<string, any> & {
  pageNo?: number
  multiFilters?: ListMultiFilterCondition[]
}

const DEFAULT_OPERATORS: Record<ListMultiFilterFieldType, ListMultiFilterOperator[]> = {
  text: ['contains', 'eq'],
  select: ['eq'],
  multiSelect: ['in'],
  dateRange: ['between'],
  autocomplete: ['contains', 'eq'],
  numberRange: ['between']
}

const DEFAULT_OPERATOR: Record<ListMultiFilterFieldType, ListMultiFilterOperator> = {
  text: 'contains',
  select: 'eq',
  multiSelect: 'in',
  dateRange: 'between',
  autocomplete: 'contains',
  numberRange: 'between'
}

const isEmptyScalarValue = (value: unknown) =>
  value === undefined || value === null || (typeof value === 'string' && value.trim() === '')

const normalizeScalarValue = (value: unknown): ListMultiFilterScalar | undefined => {
  if (typeof value === 'string') {
    const trimmed = value.trim()
    return trimmed || undefined
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return value
  }
  return undefined
}

const getRangeValues = (condition: Partial<ListMultiFilterCondition>) => {
  if (Array.isArray(condition.value)) {
    return [condition.value[0], condition.value[1]]
  }
  return [condition.value, condition.valueEnd]
}

const hasAnyRangeValue = (condition: Partial<ListMultiFilterCondition>) => {
  const [value, valueEnd] = getRangeValues(condition)
  return !isEmptyScalarValue(value) || !isEmptyScalarValue(valueEnd)
}

const hasCompleteRangeValue = (condition: Partial<ListMultiFilterCondition>) => {
  const [value, valueEnd] = getRangeValues(condition)
  return !isEmptyScalarValue(value) && !isEmptyScalarValue(valueEnd)
}

export const getDefaultMultiFilterOperator = (definition: ListMultiFilterDefinition) =>
  DEFAULT_OPERATOR[definition.type]

export const getMultiFilterOperatorOptions = (definition: ListMultiFilterDefinition) =>
  definition.operators || DEFAULT_OPERATORS[definition.type]

export const normalizeMultiFilterCondition = (
  definition: ListMultiFilterDefinition,
  condition: Partial<ListMultiFilterCondition>
): ListMultiFilterCondition | undefined => {
  const operator = condition.operator || getDefaultMultiFilterOperator(definition)
  if (definition.type === 'dateRange' || definition.type === 'numberRange' || operator === 'between') {
    const [rawValue, rawValueEnd] = getRangeValues(condition)
    const value = normalizeScalarValue(rawValue)
    const valueEnd = normalizeScalarValue(rawValueEnd)
    if (isEmptyScalarValue(value) || isEmptyScalarValue(valueEnd)) return undefined
    return {
      id: condition.id,
      key: definition.key,
      operator: 'between',
      value,
      valueEnd
    }
  }

  if (definition.type === 'multiSelect' || operator === 'in') {
    const values = Array.isArray(condition.value) ? condition.value : []
    const selectedValues = values
      .map((item) => normalizeScalarValue(item))
      .filter((item): item is ListMultiFilterScalar => !isEmptyScalarValue(item))
    if (selectedValues.length === 0) return undefined
    return {
      id: condition.id,
      key: definition.key,
      operator: 'in',
      value: selectedValues
    }
  }

  const value = normalizeScalarValue(condition.value)
  if (isEmptyScalarValue(value)) return undefined
  return {
    id: condition.id,
    key: definition.key,
    operator,
    value
  }
}

export const isMultiFilterConditionEmpty = (
  definition: ListMultiFilterDefinition,
  condition: Partial<ListMultiFilterCondition>
) => !normalizeMultiFilterCondition(definition, condition)

export const useTableMultiFilter = <T extends ListMultiFilterQueryParams>(
  tableKey: string,
  filterDefinitions: MaybeRefOrGetter<ListMultiFilterDefinition[]>,
  queryParams: T,
  reload: () => void | Promise<void>
) => {
  const definitions = computed(() => toValue(filterDefinitions))
  const state = reactive<ListMultiFilterState>({
    conditions: [],
    activeConditionId: undefined
  })

  const definitionMap = computed(() => {
    const map = new Map<string, ListMultiFilterDefinition>()
    definitions.value.forEach((definition) => map.set(definition.key, definition))
    return map
  })

  const activeConditions = computed<ListMultiFilterCondition[]>(() => {
    const conditions: ListMultiFilterCondition[] = []
    for (const condition of state.conditions) {
      const definition = definitionMap.value.get(condition.key)
      if (!definition) continue
      const normalizedCondition = normalizeMultiFilterCondition(definition, condition)
      if (normalizedCondition) {
        conditions.push(normalizedCondition)
      }
    }
    return conditions
  })

  const getConditionParamIdentity = (definition: ListMultiFilterDefinition) => {
    if (definition.queryParamKey) return definition.queryParamKey
    if (definition.queryParamKeys) return definition.queryParamKeys.join('|')
    return undefined
  }

  const validateDuplicateMappedConditions = (conditions: ListMultiFilterCondition[]) => {
    const seenParamKeys = new Map<string, string>()
    for (const condition of conditions) {
      const definition = definitionMap.value.get(condition.key)
      if (!definition) continue
      const paramIdentity = getConditionParamIdentity(definition)
      if (!paramIdentity) continue
      const previousLabel = seenParamKeys.get(paramIdentity)
      if (previousLabel) {
        ElMessage.warning(`${definition.label} 已存在筛选条件，请先删除重复条件 Tab。`)
        return false
      }
      seenParamKeys.set(paramIdentity, definition.label)
    }
    return true
  }

  const validate = () => {
    if (!tableKey) {
      ElMessage.error('多维度筛选表格标识缺失，请联系管理员。')
      return false
    }

    for (const condition of state.conditions) {
      const definition = definitionMap.value.get(condition.key)
      if (!definition) {
        ElMessage.warning('存在未注册的筛选字段，请刷新页面后重试。')
        return false
      }

      const operator = condition.operator || getDefaultMultiFilterOperator(definition)
      if (!getMultiFilterOperatorOptions(definition).includes(operator)) {
        ElMessage.warning(`${definition.label} 的筛选条件不合法。`)
        return false
      }

      if (definition.type === 'dateRange' || definition.type === 'numberRange' || operator === 'between') {
        if (hasAnyRangeValue(condition) && !hasCompleteRangeValue(condition)) {
          ElMessage.warning(`请完整填写${definition.label}的起止范围。`)
          return false
        }
      }
    }

    return validateDuplicateMappedConditions(activeConditions.value)
  }

  const clearMultiFilterParams = () => {
    const queryParamTarget = queryParams as ListMultiFilterQueryParams
    delete queryParamTarget.multiFilters
    definitions.value.forEach((definition) => {
      if (definition.queryParamKey) {
        delete queryParamTarget[definition.queryParamKey]
      }
      if (definition.queryParamKeys) {
        delete queryParamTarget[definition.queryParamKeys[0]]
        delete queryParamTarget[definition.queryParamKeys[1]]
      }
    })
  }

  const writeConditionToParams = (
    definition: ListMultiFilterDefinition,
    condition: ListMultiFilterCondition,
    queryParamTarget: ListMultiFilterQueryParams,
    unmappedConditions: ListMultiFilterCondition[]
  ) => {
    if (definition.queryParamKeys && condition.valueEnd !== undefined) {
      queryParamTarget[definition.queryParamKeys[0]] = condition.value
      queryParamTarget[definition.queryParamKeys[1]] = condition.valueEnd
      return
    }
    if (definition.queryParamKey) {
      queryParamTarget[definition.queryParamKey] =
        condition.valueEnd !== undefined ? [condition.value, condition.valueEnd] : condition.value
      return
    }
    unmappedConditions.push(condition)
  }

  const applyMultiFilter = async () => {
    if (!validate()) return
    clearMultiFilterParams()
    const queryParamTarget = queryParams as ListMultiFilterQueryParams
    const unmappedConditions: ListMultiFilterCondition[] = []
    for (const condition of activeConditions.value) {
      const definition = definitionMap.value.get(condition.key)
      if (!definition) continue
      writeConditionToParams(definition, condition, queryParamTarget, unmappedConditions)
    }
    if (unmappedConditions.length > 0) {
      queryParamTarget.multiFilters = unmappedConditions
    }
    queryParams.pageNo = 1
    await reload()
  }

  const resetMultiFilter = async () => {
    state.conditions = []
    state.activeConditionId = undefined
    clearMultiFilterParams()
    queryParams.pageNo = 1
    await reload()
  }

  const updateState = (nextState: Partial<ListMultiFilterState>) => {
    if ('conditions' in nextState) {
      state.conditions = [...(nextState.conditions || [])]
    }
    if ('activeConditionId' in nextState) {
      state.activeConditionId = nextState.activeConditionId
    }
  }

  const setCondition = (condition: ListMultiFilterCondition) => {
    const conditionId = condition.id || condition.key
    const nextCondition = { ...condition, id: conditionId }
    state.conditions = [
      ...state.conditions.filter((currentCondition) => (currentCondition.id || currentCondition.key) !== conditionId),
      nextCondition
    ]
    state.activeConditionId = conditionId
  }

  const removeCondition = (conditionIdOrKey: string) => {
    state.conditions = state.conditions.filter(
      (condition) => (condition.id || condition.key) !== conditionIdOrKey && condition.key !== conditionIdOrKey
    )
    if (state.activeConditionId === conditionIdOrKey) {
      state.activeConditionId = state.conditions[0]?.id || state.conditions[0]?.key
    }
  }

  return {
    tableKey,
    filterDefinitions: definitions,
    state,
    activeConditions,
    applyMultiFilter,
    resetMultiFilter,
    updateState,
    setCondition,
    removeCondition,
    clearMultiFilterParams
  }
}
