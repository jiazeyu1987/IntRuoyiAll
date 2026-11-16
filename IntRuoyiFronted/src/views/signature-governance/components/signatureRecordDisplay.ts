/** Split the backend role-name snapshot without changing the stored evidence. */
export const recordRoles = (value?: string): string[] =>
  value
    ? value
        .split(/[、,，;；\n]+/)
        .map((role) => role.trim())
        .filter(Boolean)
    : []

/** Trace markers remain available in the original comment under signature details. */
export const signatureCommentSummary = (value?: string): string => {
  const text = value?.trim() || ''
  if (!text) return ''
  if (/^Stage\d+模拟\[stage=[^\]]+\]\[run=[^\]]+\]$/i.test(text)) return '模拟测试签署记录'
  return text.replace(/\[(?:stage|run)=[^\]]*\]/gi, '').trim()
}
