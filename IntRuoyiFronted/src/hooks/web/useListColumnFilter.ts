import { computed, ref, shallowRef, type MaybeRefOrGetter, toValue } from 'vue'
import type {
  ListMultiFilterCondition,
  ListMultiFilterDefinition,
  ListMultiFilterState
} from './useTableMultiFilter'
import type { UserTableColumnDefinition } from './useUserTableColumns'

export interface ListColumnFilterDefinition extends ListMultiFilterDefinition {
  getValue?: (row: any) => unknown
}

const FUNCTION_COLUMNS = new Set([
  'operation',
  'operations',
  'action',
  'actions',
  'selection',
  'index'
])

export const buildListColumnFilters = (
  columns: UserTableColumnDefinition[],
  definitions: ListColumnFilterDefinition[]
): ListColumnFilterDefinition[] => {
  const byKey = new Map(definitions.map((field) => [field.key, field]))
  const byLabel = new Map(definitions.map((field) => [field.label, field]))
  const fields = columns
    .filter((column) => column.business !== false && !FUNCTION_COLUMNS.has(column.key))
    .map((column): ListColumnFilterDefinition => {
      const field = byKey.get(column.key) || byLabel.get(column.label)
      if (field)
        return {
          ...field,
          key: column.key,
          label: column.label,
          getValue:
            field.getValue ||
            (field.key !== column.key ? (row) => fieldValue(row, field.key) : undefined)
        }
      return {
        key: column.key,
        label: column.label,
        type: /(?:Time|Date|At)$/.test(column.key) ? 'dateRange' : 'text'
      }
    })
  const keys = new Set(fields.map((field) => field.key))
  const labels = new Set(fields.map((field) => field.label))
  return [
    ...fields,
    ...definitions.filter((field) => !keys.has(field.key) && !labels.has(field.label))
  ]
}

const fieldValue = (row: any, key: string): unknown =>
  key.split('.').reduce((value, part) => value?.[part], row)

const textValue = (value: unknown): string => {
  if (value == null) return ''
  if (Array.isArray(value)) return value.map(textValue).join(' ')
  if (typeof value === 'object') return Object.values(value).map(textValue).join(' ')
  return String(value).trim().toLocaleLowerCase()
}

const comparableValue = (value: unknown, date: boolean): number => {
  const dateValue =
    typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value) ? `${value}T00:00:00` : value
  const number = date ? new Date(dateValue as string | number).getTime() : Number(value)
  if (!Number.isFinite(number)) throw new Error('筛选范围必须为有效日期或数字')
  return number
}

export const filterListColumns = <T>(
  rows: T[],
  conditions: ListMultiFilterCondition[],
  definitions: ListColumnFilterDefinition[]
): T[] => {
  const fields = new Map(definitions.map((field) => [field.key, field]))
  const matches = (row: T) =>
    conditions.every((condition) => {
      const field = fields.get(condition.key)
      if (!field) throw new Error(`未配置筛选字段：${condition.key}`)
      const value = field.getValue ? field.getValue(row) : fieldValue(row, field.key)
      const actual = textValue(value)
      const expected = textValue(condition.value)
      switch (condition.operator) {
        case 'contains':
          return actual.includes(expected)
        case 'eq':
          return Array.isArray(value)
            ? value.some((item) => textValue(item) === expected)
            : actual === expected
        case 'in':
          return (Array.isArray(condition.value) ? condition.value : [condition.value]).some(
            (item) =>
              (Array.isArray(value) ? value : [value]).some(
                (actualValue) => textValue(actualValue) === textValue(item)
              )
          )
        case 'between':
        case 'gte':
        case 'lte': {
          if (value == null || value === '') return false
          const date = field.type === 'date' || field.type === 'dateRange'
          const actualNumber = comparableValue(value, date)
          const start = comparableValue(condition.value, date)
          const endValue = condition.valueEnd
          const end = endValue == null ? start : comparableValue(endValue, date)
          // Date pickers supply calendar dates; include the entire end date.
          const inclusiveEnd =
            date && /^\d{4}-\d{2}-\d{2}$/.test(String(endValue ?? condition.value))
              ? new Date(end).setHours(23, 59, 59, 999)
              : end
          if (condition.operator === 'gte') return actualNumber >= start
          if (condition.operator === 'lte') return actualNumber <= inclusiveEnd
          return actualNumber >= start && actualNumber <= inclusiveEnd
        }
        default:
          throw new Error(`不支持的筛选运算符：${condition.operator}`)
      }
    })
  const result: T[] = []
  for (const row of rows) {
    if (matches(row)) {
      result.push(row)
      continue
    }
    const children = (row as any)?.children
    if (Array.isArray(children)) {
      const matchingChildren = filterListColumns(children, conditions, definitions)
      if (matchingChildren.length) result.push({ ...row, children: matchingChildren })
    }
  }
  return result
}

/** Read every authorized page before filtering, so matches beyond the current page are included. */
export const loadAllListPages = async <T>(
  loadPage: (pagination: {
    pageNo: number
    pageSize: number
  }) => Promise<{ list: T[]; total: number }>
): Promise<T[]> => {
  const rows: T[] = []
  const pageSize = 100
  let expectedTotal: number | undefined
  for (let pageNo = 1; ; pageNo++) {
    const page = await loadPage({ pageNo, pageSize })
    if (!Array.isArray(page.list) || !Number.isSafeInteger(page.total) || page.total < 0) {
      throw new Error('列表接口未返回有效的分页数据')
    }
    if (expectedTotal !== undefined && expectedTotal !== page.total) {
      throw new Error('筛选期间列表数据已变更，请重新查询')
    }
    expectedTotal = page.total
    rows.push(...page.list)
    if (rows.length === expectedTotal) return rows
    if (page.list.length === 0 || rows.length > expectedTotal) {
      throw new Error('列表接口返回的数据数量与总数不一致')
    }
  }
}

export const useListColumnFilter = (
  definitions: MaybeRefOrGetter<ListColumnFilterDefinition[]>,
  source: () => any[] | undefined,
  loadSource: () => (() => Promise<any[]>) | undefined
) => {
  const state = ref<ListMultiFilterState>({
    conditions: [] as ListMultiFilterCondition[],
    appliedConditions: [] as ListMultiFilterCondition[],
    activeConditionId: undefined as string | undefined
  })
  const loading = ref(false)
  const error = ref('')
  const loadedRows = shallowRef<any[]>([])
  let generation = 0
  const active = computed(() => state.value.appliedConditions.length > 0)
  const rows = computed(() =>
    filterListColumns(
      source() ?? loadedRows.value,
      state.value.appliedConditions,
      toValue(definitions)
    )
  )
  const apply = async () => {
    const conditions = state.value.conditions
      .filter(
        (condition) =>
          condition.value !== undefined &&
          condition.value !== null &&
          condition.value !== '' &&
          (!Array.isArray(condition.value) || condition.value.length > 0)
      )
      .map((condition) => ({ ...condition }))
    const currentGeneration = ++generation
    loading.value = true
    error.value = ''
    try {
      for (const condition of conditions) {
        if (
          condition.operator === 'between' &&
          (condition.valueEnd == null || condition.valueEnd === '')
        ) {
          throw new Error('请填写完整的筛选范围')
        }
      }
      const loader = loadSource()
      const result = conditions.length && loader ? await loader() : (source() ?? [])
      if (currentGeneration !== generation) return
      // Validate before replacing the last successful result.
      filterListColumns(result, conditions, toValue(definitions))
      loadedRows.value = result
      state.value.appliedConditions = conditions
    } catch (cause) {
      if (currentGeneration === generation)
        error.value = cause instanceof Error ? cause.message : '列表筛选失败'
      throw cause
    } finally {
      if (currentGeneration === generation) loading.value = false
    }
  }
  const reset = () => {
    generation++
    state.value = { conditions: [], appliedConditions: [], activeConditionId: undefined }
    loadedRows.value = []
    loading.value = false
    error.value = ''
  }
  const invalidate = () => {
    loadedRows.value = []
  }
  return { state, loading, error, active, rows, apply, reset, invalidate }
}
