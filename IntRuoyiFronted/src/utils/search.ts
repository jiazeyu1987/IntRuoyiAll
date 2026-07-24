import { unref, type Ref } from 'vue'

type SearchFormField = {
  prop?: string | string[]
}

type SearchFormRef = {
  fields?: SearchFormField[]
}

type SearchModel = Record<string, unknown>

const isEmptySearchValue = (value: unknown): boolean => {
  if (value === undefined || value === null) return true
  if (typeof value === 'string') return value.trim() === ''
  if (Array.isArray(value)) return value.every((item) => isEmptySearchValue(item))
  if (value instanceof Date) return false
  if (typeof value === 'object') return Object.keys(value as Record<string, unknown>).length === 0
  return false
}

const getValueByPath = (model: SearchModel, prop: string | string[]) => {
  const segments = Array.isArray(prop) ? prop : prop.split('.')
  return segments.reduce<unknown>((current, segment) => {
    if (current && typeof current === 'object') {
      return (current as Record<string, unknown>)[segment]
    }
    return undefined
  }, model)
}

export const isSearchModelInputEmpty = (
  model: SearchModel | undefined,
  fields: Array<string | string[] | undefined>
) => {
  const searchFields = fields.filter(
    (field): field is string | string[] => !!field && field !== 'action'
  )
  if (!model || searchFields.length === 0) return false
  return searchFields.every((field) => isEmptySearchValue(getValueByPath(model, field)))
}

export const isSearchFormInputEmpty = (
  formRef: Ref<SearchFormRef | undefined> | SearchFormRef | undefined,
  model: SearchModel | undefined
) => {
  const form = unref(formRef)
  const fields = form?.fields?.map((field) => field.prop) || []
  return isSearchModelInputEmpty(model, fields)
}
