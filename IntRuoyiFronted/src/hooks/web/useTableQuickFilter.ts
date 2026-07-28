import { computed, reactive, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { ElMessage } from 'element-plus'

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

export const useTableQuickFilter = <T extends TableQuickFilterQueryParams>(
  tableKey: string,
  filterDefinitions: MaybeRefOrGetter<TableQuickFilterDefinition[]>,
  queryParams: T,
  reload: () => void | Promise<void>
) => {
  const definitions = computed(() => toValue(filterDefinitions))
  const state = reactive<{
    fieldKey?: string
    operator?: TableQuickFilterOperator
    value?: string | number | boolean | Array<string | number>
  }>({
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

  const applyQuickFilter = async () => {
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
