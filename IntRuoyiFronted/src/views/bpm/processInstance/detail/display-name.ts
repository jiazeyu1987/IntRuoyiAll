import { resolveDccTimelineActivityName } from '@/views/dcc/controlled-file/shared/stage-name'

type DisplayValue = string | number | null | undefined

const PROCESS_NODE_LABELS = new Map<string, string>([
  ['REG_CERT_ACCESS_APPROVAL', '注册证访问审批']
])

const PROCESS_DETAIL_TEXT_REPLACEMENTS: ReadonlyArray<readonly [RegExp, string]> = [
  [/Registration certificate access request (\d+)/gi, '注册证访问申请 $1'],
  [/Registration certificate access approval/gi, '注册证访问审批'],
  [/Registration certificate access workflow/gi, '注册证访问流程'],
  [/DCC Controlled File Approval/gi, '文控受控文件审批']
]

const toDisplayText = (value: DisplayValue) => {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

const replaceKnownProcessText = (value: DisplayValue) =>
  PROCESS_DETAIL_TEXT_REPLACEMENTS.reduce(
    (text, [pattern, replacement]) => text.replace(pattern, replacement),
    toDisplayText(value)
  )

export const resolveProcessInstanceDisplayName = (value: DisplayValue) =>
  replaceKnownProcessText(value)

export const resolveProcessNodeDisplayName = (
  activityId: DisplayValue,
  rawActivityName: DisplayValue
) => {
  const normalizedActivityId = toDisplayText(activityId)
  const configuredLabel = PROCESS_NODE_LABELS.get(normalizedActivityId)
  if (configuredLabel) {
    return configuredLabel
  }
  return replaceKnownProcessText(
    resolveDccTimelineActivityName(normalizedActivityId, rawActivityName)
  )
}

export const resolveProcessFormDisplayName = (value: DisplayValue) => replaceKnownProcessText(value)

export const resolveProcessDetailDescription = (value: DisplayValue) =>
  (() => {
    const normalized = toDisplayText(value)
    if (!normalized) {
      return ''
    }
    const segments = normalized.split(' / ')
    if (segments.length < 5) {
      return normalized
    }
    segments[1] = resolveProcessNodeDisplayName(undefined, segments[1])
    return segments.join(' / ')
  })()
