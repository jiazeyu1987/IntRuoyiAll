const POSITIVE_INTEGER_TEXT = /^[1-9]\d*$/

export const normalizeRouteQueryValue = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (rawValue === undefined || rawValue === null) return ''
  return String(rawValue).trim()
}

export const parsePositiveRouteQueryId = (value: unknown) => {
  const text = normalizeRouteQueryValue(value)
  return POSITIVE_INTEGER_TEXT.test(text) ? text : ''
}

export const sameRouteQueryId = (left: unknown, right: unknown) => {
  const leftText = normalizeRouteQueryValue(left)
  const rightText = normalizeRouteQueryValue(right)
  return Boolean(leftText && rightText && leftText === rightText)
}
