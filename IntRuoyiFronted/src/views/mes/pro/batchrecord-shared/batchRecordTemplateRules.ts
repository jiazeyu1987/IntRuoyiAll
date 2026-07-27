import type {
  BatchRecordReportSignatureCellMarkerVO,
  BatchRecordReportCellAttachmentRuleVO,
  BatchRecordReportCellRuleConstraints,
  BatchRecordReportCellRuleVO,
  BatchRecordReportCellValueType
} from '@/api/mes/pro/batchrecordreport'

export type TemplateRawCell = {
  text?: unknown
  value?: unknown
  merge?: unknown
}

export type TemplateRawRow = {
  cells?: Record<string, TemplateRawCell>
}

export type TemplateRawLayout = {
  rows?: Record<string, TemplateRawRow>
}

export type TemplateSimulationComponentKind =
  | 'text'
  | 'number'
  | 'date'
  | 'datetime'
  | 'checkbox'
  | 'signature'
  | 'attachment'

export type TemplateRuleState = 'auto' | 'reviewed' | 'manual' | 'error'

export type TemplateRuleTypeBadge = {
  label: string
  symbol: string
  tone: string
}

export type TemplateSimulationSignatureValue = {
  actorName?: string
  signedAt?: string
}

export type TemplateSimulationValue =
  | string
  | number
  | boolean
  | null
  | TemplateSimulationSignatureValue

export type TemplateSimulationValueMap = Record<string, TemplateSimulationValue | undefined>

export type TemplateSimulationField = {
  fieldIdentity: string
  rowIndex: number
  columnIndex: number
  label: string
  valueType: BatchRecordReportCellValueType
  componentKind: TemplateSimulationComponentKind
  required: boolean
  placeholder?: string
  helpText?: string
  unit?: string
  format?: string
  attachmentRule?: BatchRecordReportCellAttachmentRuleVO
  signatureActionType?: BatchRecordReportSignatureCellMarkerVO['actionType']
  signatureLabel?: string
}

export type TemplateEditableCellContext = {
  fieldIdentity: string
  rowIndex: number
  columnIndex: number
  valueType: BatchRecordReportCellValueType
  componentKind: TemplateSimulationComponentKind
  componentFlag: string
  label: string
  required: boolean
  placeholder?: string
  helpText?: string
  source?: string
  reviewed: boolean
  unit?: string
  format?: string
  attachmentRule?: BatchRecordReportCellAttachmentRuleVO
  signatureActionType?: BatchRecordReportSignatureCellMarkerVO['actionType']
  signatureLabel?: string
}

export const cellRuleValueTypeOptions: Array<{ label: string; value: BatchRecordReportCellValueType }> = [
  { label: '文本', value: 'STRING' },
  { label: '数字', value: 'NUMBER' },
  { label: '日期', value: 'DATE' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '勾选', value: 'BOOLEAN' },
  { label: '签名', value: 'SIGNATURE' }
]

export const cellRuleDefaultComponentMap: Record<BatchRecordReportCellValueType, string> = {
  STRING: 'input-text',
  NUMBER: 'input-number',
  DATE: 'date',
  DATETIME: 'datetime',
  BOOLEAN: 'checkbox',
  SIGNATURE: 'signature'
}

export const templateGuideValueTypeLabels: Record<BatchRecordReportCellValueType, string> = {
  STRING: '文字',
  NUMBER: '数字',
  DATE: '日期',
  DATETIME: '日期时间',
  BOOLEAN: '勾选',
  SIGNATURE: '签名'
}

export const templateSimulationComponentMap: Record<
  BatchRecordReportCellValueType,
  TemplateSimulationComponentKind
> = {
  STRING: 'text',
  NUMBER: 'number',
  DATE: 'date',
  DATETIME: 'datetime',
  BOOLEAN: 'checkbox',
  SIGNATURE: 'signature'
}

export const templateRuleTypeBadgeMap: Record<BatchRecordReportCellValueType, TemplateRuleTypeBadge> = {
  STRING: { label: '文本', symbol: 'A', tone: 'text' },
  NUMBER: { label: '数字', symbol: '#', tone: 'number' },
  DATE: { label: '日期', symbol: '日', tone: 'date' },
  DATETIME: { label: '日期时间', symbol: '时', tone: 'datetime' },
  BOOLEAN: { label: '勾选', symbol: 'Y', tone: 'boolean' },
  SIGNATURE: { label: '签名', symbol: '签', tone: 'signature' }
}

export const templateRuleTypeBadgeLegend: TemplateRuleTypeBadge[] = [
  templateRuleTypeBadgeMap.STRING,
  templateRuleTypeBadgeMap.NUMBER,
  templateRuleTypeBadgeMap.DATE,
  templateRuleTypeBadgeMap.DATETIME,
  templateRuleTypeBadgeMap.BOOLEAN,
  templateRuleTypeBadgeMap.SIGNATURE,
  { label: '附件', symbol: '附', tone: 'attachment' }
]

export const normalizeTemplateCellMerge = (cell: TemplateRawCell | undefined) => {
  if (!Array.isArray(cell?.merge)) return { rowSpan: 1, colSpan: 1 }
  const rowDelta = Number(cell.merge[0])
  const columnDelta = Number(cell.merge[1])
  return {
    rowSpan: Number.isInteger(rowDelta) && rowDelta >= 0 ? rowDelta + 1 : 1,
    colSpan: Number.isInteger(columnDelta) && columnDelta >= 0 ? columnDelta + 1 : 1
  }
}

export const stringifyTemplateCell = (value: unknown) => {
  if (value == null || value === '') return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return JSON.stringify(value)
}

export const cleanedAttachmentRule = (attachmentRule?: BatchRecordReportCellAttachmentRuleVO) => {
  if (!attachmentRule) return undefined
  const cleaned: BatchRecordReportCellAttachmentRuleVO = {}
  if (typeof attachmentRule.required === 'boolean') {
    cleaned.required = attachmentRule.required
  }
  if (typeof attachmentRule.minCount === 'number' && Number.isFinite(attachmentRule.minCount)) {
    cleaned.minCount = attachmentRule.minCount
  }
  if (typeof attachmentRule.maxCount === 'number' && Number.isFinite(attachmentRule.maxCount)) {
    cleaned.maxCount = attachmentRule.maxCount
  }
  if (typeof attachmentRule.attachmentType === 'string' && attachmentRule.attachmentType.trim()) {
    cleaned.attachmentType = attachmentRule.attachmentType.trim()
  }
  if (typeof attachmentRule.groupKey === 'string' && attachmentRule.groupKey.trim()) {
    cleaned.groupKey = attachmentRule.groupKey.trim()
  }
  return Object.keys(cleaned).length ? cleaned : undefined
}

export const buildTemplateFieldIdentity = (rowIndex: number, columnIndex: number) =>
  `${rowIndex}:${columnIndex}`

export const cleanedRuleConstraints = (
  constraints: BatchRecordReportCellRuleConstraints | undefined,
  valueType: BatchRecordReportCellValueType
) => {
  const source = constraints || {}
  const cleaned: BatchRecordReportCellRuleConstraints = {}
  const copyNumber = (key: keyof BatchRecordReportCellRuleConstraints) => {
    const value = source[key]
    if (typeof value === 'number' && Number.isFinite(value)) {
      cleaned[key] = value
    }
  }
  if (valueType === 'NUMBER') {
    ;(['min', 'max', 'scale', 'precision'] as const).forEach(copyNumber)
  }
  if (valueType === 'STRING') {
    ;(['minLength', 'maxLength'] as const).forEach(copyNumber)
    if (source.selectionMode === 'single' && Array.isArray(source.options)) {
      cleaned.selectionMode = 'single'
      cleaned.options = source.options
        .map((option) => {
          if (typeof option === 'string' || typeof option === 'number' || typeof option === 'boolean') {
            const value = String(option).trim()
            return value ? { label: value, value } : undefined
          }
          if (!option || typeof option !== 'object') return undefined
          const value = String((option as Record<string, unknown>).value ?? '').trim()
          const label = String((option as Record<string, unknown>).label ?? value).trim()
          return value ? { label: label || value, value } : undefined
        })
        .filter(Boolean)
    }
  }
  if ((valueType === 'DATE' || valueType === 'DATETIME') && typeof source.format === 'string' && source.format.trim()) {
    cleaned.format = source.format.trim()
  }
  return cleaned
}

export const formatTemplateAttachmentRule = (
  attachmentRule?: BatchRecordReportCellAttachmentRuleVO,
  separator = ' / '
) => {
  const cleaned = cleanedAttachmentRule(attachmentRule)
  if (!cleaned) return ''
  const parts = [
    cleaned.required ? '必需附件' : '可选附件',
    cleaned.minCount ? `至少 ${cleaned.minCount} 个` : '',
    cleaned.maxCount ? `最多 ${cleaned.maxCount} 个` : '',
    cleaned.attachmentType ? `类型 ${cleaned.attachmentType}` : '',
    cleaned.groupKey ? `组 ${cleaned.groupKey}` : ''
  ].filter(Boolean)
  return parts.join(separator)
}

export const resolveTemplateSignatureActionLabel = (
  marker?: Pick<BatchRecordReportSignatureCellMarkerVO, 'label' | 'actionType'>
) => {
  if (marker?.label?.trim()) return marker.label.trim()
  if (marker?.actionType === 'FORM_REVIEW') return '历史复核签名'
  if (marker?.actionType === 'SUBMIT') return '提交签名'
  if (marker?.actionType === 'APPROVE') return '放行审批签名'
  return '签名'
}

export const resolveTemplateRuleTypeBadge = (
  context: Pick<TemplateEditableCellContext, 'valueType' | 'componentKind'>
): TemplateRuleTypeBadge => {
  if (context.componentKind === 'attachment') {
    return { label: '附件', symbol: '附', tone: 'attachment' }
  }
  if (context.componentKind === 'signature') {
    return templateRuleTypeBadgeMap.SIGNATURE
  }
  return templateRuleTypeBadgeMap[context.valueType] || templateRuleTypeBadgeMap.STRING
}

export const resolveTemplateRuleState = (
  context: Pick<TemplateEditableCellContext, 'valueType' | 'componentFlag' | 'source' | 'reviewed'>
): TemplateRuleState => {
  if (!context.valueType || !context.componentFlag) {
    return 'error'
  }
  if (context.reviewed) {
    return 'reviewed'
  }
  if ((context.source || '').toUpperCase() === 'AUTO') {
    return 'auto'
  }
  return 'manual'
}

export const resolveTemplateRuleTooltip = (context: TemplateEditableCellContext) => {
  const badge = resolveTemplateRuleTypeBadge(context)
  const state = resolveTemplateRuleState(context)
  const stateLabelMap: Record<TemplateRuleState, string> = {
    auto: '自动识别待确认',
    reviewed: '人工确认',
    manual: '手工规则待确认',
    error: '规则配置异常'
  }
  return [
    badge.label,
    context.componentFlag,
    stateLabelMap[state],
    context.required ? '必填' : '可选',
    context.unit ? `单位 ${context.unit}` : '',
    context.format ? `格式 ${context.format}` : ''
  ]
    .filter(Boolean)
    .join(' / ')
}

export const normalizeCellRule = (rule: BatchRecordReportCellRuleVO): BatchRecordReportCellRuleVO => {
  const valueType = (rule.valueType || 'STRING') as BatchRecordReportCellValueType
  const attachmentRule = cleanedAttachmentRule(rule.attachmentRule)
  return {
    rowIndex: rule.rowIndex,
    columnIndex: rule.columnIndex,
    valueType,
    componentFlag: rule.componentFlag || cellRuleDefaultComponentMap[valueType],
    required: Boolean(rule.required),
    label: rule.label || `第 ${rule.rowIndex + 1} 行第 ${rule.columnIndex + 1} 列`,
    placeholder: typeof rule.placeholder === 'string' ? rule.placeholder.trim() : '',
    helpText: typeof rule.helpText === 'string' ? rule.helpText.trim() : '',
    constraints: { ...(rule.constraints || {}) },
    unit: rule.unit || '',
    source: rule.source || 'AUTO',
    confidence: typeof rule.confidence === 'number' ? rule.confidence : undefined,
    reviewed: Boolean(rule.reviewed),
    ...(attachmentRule ? { attachmentRule } : {})
  }
}

const resolveTemplateSimulationComponentKind = (
  rule: BatchRecordReportCellRuleVO,
  marker?: BatchRecordReportSignatureCellMarkerVO
): TemplateSimulationComponentKind => {
  if (marker?.enabled || rule.valueType === 'SIGNATURE') {
    return 'signature'
  }
  const rawComponent = String(rule.componentFlag || '').toLowerCase()
  if (
    rawComponent.includes('upload-file') ||
    rawComponent.includes('upload-image') ||
    rawComponent.includes('upload-images') ||
    rawComponent.includes('attachment') ||
    cleanedAttachmentRule(rule.attachmentRule)
  ) {
    return 'attachment'
  }
  return templateSimulationComponentMap[rule.valueType] || 'text'
}

export const buildTemplateSimulationField = (
  rule: BatchRecordReportCellRuleVO,
  marker?: BatchRecordReportSignatureCellMarkerVO
): TemplateSimulationField => {
  const normalizedRule = normalizeCellRule(rule)
  const cleanedConstraints = cleanedRuleConstraints(normalizedRule.constraints, normalizedRule.valueType)
  return {
    fieldIdentity: buildTemplateFieldIdentity(normalizedRule.rowIndex, normalizedRule.columnIndex),
    rowIndex: normalizedRule.rowIndex,
    columnIndex: normalizedRule.columnIndex,
    label:
      (marker?.enabled ? resolveTemplateSignatureActionLabel(marker) : '') ||
      normalizedRule.label ||
      `第 ${normalizedRule.rowIndex + 1} 行第 ${normalizedRule.columnIndex + 1} 列`,
    valueType: normalizedRule.valueType,
    componentKind: resolveTemplateSimulationComponentKind(normalizedRule, marker),
    required: Boolean(normalizedRule.required),
    placeholder: normalizedRule.placeholder,
    helpText: normalizedRule.helpText,
    unit: normalizedRule.unit || undefined,
    format: typeof cleanedConstraints.format === 'string' ? cleanedConstraints.format : undefined,
    attachmentRule: cleanedAttachmentRule(normalizedRule.attachmentRule),
    signatureActionType: marker?.actionType,
    signatureLabel: marker?.enabled ? resolveTemplateSignatureActionLabel(marker) : undefined
  }
}

export const buildTemplateEditableCellContext = (
  rule: BatchRecordReportCellRuleVO,
  marker?: BatchRecordReportSignatureCellMarkerVO
): TemplateEditableCellContext => {
  const normalizedRule = normalizeCellRule(rule)
  const field = buildTemplateSimulationField(rule, marker)
  return {
    fieldIdentity: field.fieldIdentity,
    rowIndex: field.rowIndex,
    columnIndex: field.columnIndex,
    valueType: field.valueType,
    componentKind: field.componentKind,
    componentFlag: normalizedRule.componentFlag || cellRuleDefaultComponentMap[field.valueType],
    label: field.label,
    required: field.required,
    placeholder: normalizedRule.placeholder,
    helpText: normalizedRule.helpText,
    source: normalizedRule.source,
    reviewed: normalizedRule.reviewed,
    unit: field.unit,
    format: field.format,
    attachmentRule: field.attachmentRule,
    signatureActionType: field.signatureActionType,
    signatureLabel: field.signatureLabel
  }
}

export const buildTemplateSignatureSimulationField = (
  marker: BatchRecordReportSignatureCellMarkerVO
): TemplateSimulationField => {
  return {
    fieldIdentity: buildTemplateFieldIdentity(marker.rowIndex, marker.columnIndex),
    rowIndex: marker.rowIndex,
    columnIndex: marker.columnIndex,
    label: resolveTemplateSignatureActionLabel(marker),
    valueType: 'SIGNATURE',
    componentKind: 'signature',
    required: false,
    signatureActionType: marker.actionType,
    signatureLabel: resolveTemplateSignatureActionLabel(marker)
  }
}

export const buildTemplateSimulationFields = (
  rules: BatchRecordReportCellRuleVO[],
  markers: BatchRecordReportSignatureCellMarkerVO[] = []
) => {
  const markerMap = new Map<string, BatchRecordReportSignatureCellMarkerVO>()
  markers
    .filter((marker) => marker.enabled)
    .forEach((marker) => markerMap.set(buildTemplateFieldIdentity(marker.rowIndex, marker.columnIndex), marker))

  const fields = rules.map((rule) =>
    buildTemplateSimulationField(rule, markerMap.get(buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex)))
  )
  const fieldIdentitySet = new Set(fields.map((field) => field.fieldIdentity))

  markerMap.forEach((marker, fieldIdentity) => {
    if (fieldIdentitySet.has(fieldIdentity)) return
    fields.push(buildTemplateSignatureSimulationField(marker))
  })

  return fields.sort(
    (left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex
  )
}
