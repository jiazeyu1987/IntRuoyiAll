import {
  DCC_ACTION_PROJECTION_MISSING_REASON,
  DCC_CONTROLLED_FILE_STATUS_OPTIONS,
  getDccControlledFileStatusLabel,
  getDccControlledFileStatusTagType,
  hasDccControlledFileActionProjection,
  isDccControlledFileActionAllowed,
  resolveDccActionProjectionReadonlyReason,
  type DccControlledFileStatus,
  type DccControlledFileTagType
} from '../shared/lifecycle'
import type { DccControlledFileActionProjectionVO } from '@/api/dcc/controlledFile/workflow'
import { formatDateTimeValue } from '@/utils/formatTime'
import { getDccVersionDisplayText } from '../shared/version-display'

export const BROWSER_STATUS_FILTER_OPTIONS = DCC_CONTROLLED_FILE_STATUS_OPTIONS

type BrowserRowReadableState = {
  status?: string
  canPreview?: boolean
  canDownload?: boolean
  canPrint?: boolean
  actionProjection?: DccControlledFileActionProjectionVO | null
}

interface BrowserVersionSummarySource {
  versionNo?: string
  status?: string
  effectiveDate?: string | readonly number[] | null
  publishedTime?: number
  publishedArtifactAvailable?: boolean
  stampedArtifactAvailable?: boolean
  currentActiveVersionNo?: string | null
  modifying?: boolean
}

export const getBrowserPublishedFileStatusText = (file?: BrowserVersionSummarySource | null) =>
  file?.publishedArtifactAvailable ? '已生成' : '未生成'

export const getBrowserStampedFileStatusText = (file?: BrowserVersionSummarySource | null) =>
  file?.stampedArtifactAvailable ? '已生成' : '未生成'

export const getBrowserCurrentVersionSourceText = (file?: BrowserVersionSummarySource | null) => {
  const currentActiveVersionNo = String(file?.currentActiveVersionNo || '').trim()
  if (currentActiveVersionNo) {
    return `当前执行受控版本：master 当前执行版本 ${currentActiveVersionNo}`
  }
  return '当前执行受控版本：未记录'
}

export const getBrowserStatusLabel = (status: string | undefined) =>
  getDccControlledFileStatusLabel(status as DccControlledFileStatus | undefined)

export const getBrowserStatusTagType = (status: string | undefined): DccControlledFileTagType =>
  getDccControlledFileStatusTagType(status as DccControlledFileStatus | undefined)

export const formatBrowserEffectiveDate = (value: BrowserVersionSummarySource['effectiveDate']) => {
  if (value == null || value === '') return '-'
  let year: number, month: number, day: number
  if (Array.isArray(value) && value.length === 3 && value.every(Number.isInteger)) {
    ;[year, month, day] = value
  } else if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    ;[year, month, day] = value.split('-').map(Number)
  } else return '日期格式异常'
  const date = new Date(0)
  date.setUTCFullYear(year, month - 1, day)
  if (year < 1 || year > 9999 || date.getUTCFullYear() !== year
    || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return '日期格式异常'
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

export const isBrowserHistoryVisible = (status: string | undefined) => {
  const typedStatus = status as DccControlledFileStatus | undefined
  return typedStatus === 'SUPERSEDED' || typedStatus === 'OBSOLETE'
}

export const getBrowserVersionSummary = (
  version: BrowserVersionSummarySource,
  isSelectedVersionModifying: boolean,
  masterCurrentActiveVersionNo?: string | null
) => {
  const currentActiveVersionNo = String(masterCurrentActiveVersionNo || '').trim()
  const isCurrentActiveVersion = version.status === 'ACTIVE' && Boolean(currentActiveVersionNo) &&
    currentActiveVersionNo === String(version.versionNo || '').trim()
  let versionKindText = '版本属性未记录'
  let versionKindTagType: DccControlledFileTagType = 'info'
  if (version.status === 'WORKING') {
    versionKindText = '工作小版本'
    versionKindTagType = 'warning'
  } else if (version.status === 'CONTROLLED_PENDING_EFFECTIVE') {
    versionKindText = '受控（待生效）'
    versionKindTagType = 'primary'
  } else if (version.status === 'ACTIVE') {
    versionKindText = isCurrentActiveVersion ? '当前执行受控版本' : currentActiveVersionNo
      ? '受控版本（非当前执行）' : '受控版本（执行身份未记录）'
    versionKindTagType = isCurrentActiveVersion ? 'success' : 'info'
  } else if (version.status === 'OBSOLETE') {
    versionKindText = '历史已作废'
  } else if (version.status === 'SUPERSEDED') {
    versionKindText = '历史已替换'
  } else if (version.status === 'REJECTED' || version.status === 'WITHDRAWN') {
    versionKindText = '历史申请'
  } else if (version.status === 'DRAFT') {
    versionKindText = '草稿版本'
  } else if ([
    'PENDING_DOC_CONTROL_REVIEW', 'PENDING_MATRIX_REVIEW', 'PENDING_MATRIX_APPROVAL',
    'PENDING_DOC_CONTROL_APPROVAL', 'PENDING_APPLICANT_REWORK', 'PENDING_APPLICANT_TRAINING_RECORD',
    'READY_TO_PUBLISH', 'FINALIZING', 'TRAINING_IN_PROGRESS', 'PENDING_MANUAL_DISTRIBUTION',
    'FINALIZATION_FAILED'
  ].includes(version.status || '')) {
    versionKindText = '在途版本'
    versionKindTagType = 'warning'
  }

  return {
    versionText: getDccVersionDisplayText(version.versionNo),
    statusLabel: getBrowserStatusLabel(version.status),
    statusTagType: getBrowserStatusTagType(version.status),
    versionKindText,
    versionKindTagType,
    isCurrentActiveVersion,
    modifying: Boolean(isSelectedVersionModifying || version.modifying),
    effectiveText: `生效：${formatBrowserEffectiveDate(version.effectiveDate)}`,
    publishedText: `发布：${formatDateTimeValue(version.publishedTime, '-')}`,
    publishedFileStatusText: getBrowserPublishedFileStatusText(version),
    stampedFileStatusText: getBrowserStampedFileStatusText(version),
    currentVersionSourceText: getBrowserCurrentVersionSourceText({ ...version, currentActiveVersionNo: masterCurrentActiveVersionNo })
  }
}

export const getBrowserRowActionState = (row: BrowserRowReadableState) => {
  const hasProjection = hasDccControlledFileActionProjection(row)
  return {
    canPreview: isDccControlledFileActionAllowed(row, 'PREVIEW'),
    canDownload: isDccControlledFileActionAllowed(row, 'DOWNLOAD'),
    canPrint: isDccControlledFileActionAllowed(row, 'PRINT'),
    canPublish: isDccControlledFileActionAllowed(row, 'PUBLISH'),
    projectionMissing: !hasProjection,
    actionReadonlyReason: hasProjection
      ? resolveDccActionProjectionReadonlyReason(row, '后端动作投影未放行浏览页操作。')
      : DCC_ACTION_PROJECTION_MISSING_REASON,
    isHistoryRow: isBrowserHistoryVisible(row.status)
  }
}
