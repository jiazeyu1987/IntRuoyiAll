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
  effectiveDate?: string
  publishedTime?: number
  publishedFileId?: number | null
  stampedFileId?: number | null
  currentActiveVersionNo?: string | null
  modifying?: boolean
}

export const getBrowserPublishedFileStatusText = (file?: BrowserVersionSummarySource | null) =>
  file?.publishedFileId ? '已生成' : '未生成'

export const getBrowserStampedFileStatusText = (file?: BrowserVersionSummarySource | null) =>
  file?.stampedFileId ? '已生成' : '未生成'

export const getBrowserCurrentVersionSourceText = (file?: BrowserVersionSummarySource | null) => {
  const currentActiveVersionNo = String(file?.currentActiveVersionNo || '').trim()
  const versionNo = String(file?.versionNo || '').trim()
  if (file?.status === 'ACTIVE' && currentActiveVersionNo && currentActiveVersionNo === versionNo) {
    return `当前有效版来源：master 当前生效版本 ${versionNo}`
  }
  if (file?.status === 'ACTIVE' && !currentActiveVersionNo) {
    return `当前有效版来源：当前列表 ACTIVE 版本 ${versionNo || '-'}`
  }
  return '当前有效版来源：非当前有效版'
}

export const getBrowserStatusLabel = (status: string | undefined) =>
  getDccControlledFileStatusLabel(status as DccControlledFileStatus | undefined)

export const getBrowserStatusTagType = (status: string | undefined): DccControlledFileTagType =>
  getDccControlledFileStatusTagType(status as DccControlledFileStatus | undefined)

export const isBrowserHistoryVisible = (status: string | undefined) => {
  const typedStatus = status as DccControlledFileStatus | undefined
  return typedStatus === 'SUPERSEDED' || typedStatus === 'OBSOLETE'
}

export const getBrowserVersionSummary = (
  version: BrowserVersionSummarySource,
  isLatestVersionSelected: boolean,
  isSelectedVersionModifying: boolean
) => {
  const isCurrentActiveVersion = isLatestVersionSelected && version.status === 'ACTIVE'
  const versionKindText = isCurrentActiveVersion ? '当前有效版' : isLatestVersionSelected ? '最新版' : '历史版'
  const versionKindTagType: DccControlledFileTagType = isLatestVersionSelected ? 'success' : 'info'

  return {
    versionText: version.versionNo || '-',
    statusLabel: getBrowserStatusLabel(version.status),
    statusTagType: getBrowserStatusTagType(version.status),
    versionKindText,
    versionKindTagType,
    isCurrentActiveVersion,
    modifying: Boolean(isSelectedVersionModifying || version.modifying),
    effectiveText: `生效：${version.effectiveDate || '-'}`,
    publishedText: `发布：${formatDateTimeValue(version.publishedTime, '-')}`,
    publishedFileStatusText: getBrowserPublishedFileStatusText(version),
    stampedFileStatusText: getBrowserStampedFileStatusText(version),
    currentVersionSourceText: getBrowserCurrentVersionSourceText(version)
  }
}

export const getBrowserRowActionState = (row: BrowserRowReadableState) => {
  const hasProjection = hasDccControlledFileActionProjection(row)
  return {
    canPreview: isDccControlledFileActionAllowed(row, 'PREVIEW'),
    canDownload: isDccControlledFileActionAllowed(row, 'DOWNLOAD'),
    canPrint: isDccControlledFileActionAllowed(row, 'PRINT'),
    projectionMissing: !hasProjection,
    actionReadonlyReason: hasProjection
      ? resolveDccActionProjectionReadonlyReason(row, '后端动作投影未放行浏览页操作。')
      : DCC_ACTION_PROJECTION_MISSING_REASON,
    isHistoryRow: isBrowserHistoryVisible(row.status)
  }
}
