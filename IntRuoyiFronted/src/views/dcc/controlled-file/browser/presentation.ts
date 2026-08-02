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
  publishedTime?: string
  modifying?: boolean
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
    publishedText: `发布：${version.publishedTime || '-'}`
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
