const PLACEHOLDER_STAGE_NAME_PATTERN = /^[\s?？]+$/
const DCC_STAGE_LABEL_MAP = new Map<string, string>([
  ['DOC_CONTROL_REVIEW', '文控审核'],
  ['MATRIX_REVIEW', '会签审核'],
  ['MATRIX_APPROVAL', '会签批准'],
  ['DOC_CONTROL_APPROVAL', '文控批准']
])

const toTrimmedText = (value: string | number | null | undefined) => {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

const getDccStageLabel = (stageCode: string | number | null | undefined) => {
  const normalizedStageCode = toTrimmedText(stageCode)
  if (!normalizedStageCode) {
    return ''
  }
  return DCC_STAGE_LABEL_MAP.get(normalizedStageCode) || ''
}

export const isPlaceholderStageName = (stageName: string | number | null | undefined) => {
  const normalizedStageName = toTrimmedText(stageName)
  return !normalizedStageName || PLACEHOLDER_STAGE_NAME_PATTERN.test(normalizedStageName)
}

export const resolveDccStageDisplayName = (
  stageCode: string | number | null | undefined,
  rawStageName: string | number | null | undefined
) => {
  const normalizedStageName = toTrimmedText(rawStageName)
  if (!isPlaceholderStageName(normalizedStageName)) {
    return normalizedStageName
  }
  return getDccStageLabel(stageCode) || normalizedStageName || toTrimmedText(stageCode) || '-'
}

export const resolveDccTimelineActivityName = (
  activityId: string | number | null | undefined,
  rawActivityName: string | number | null | undefined
) => {
  const normalizedActivityName = toTrimmedText(rawActivityName)
  if (!isPlaceholderStageName(normalizedActivityName)) {
    return normalizedActivityName
  }
  return getDccStageLabel(activityId) || normalizedActivityName || toTrimmedText(activityId) || '-'
}
