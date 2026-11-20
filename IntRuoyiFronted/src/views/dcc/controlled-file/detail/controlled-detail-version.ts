import type { ControlledFileVO, ControlledFileVersionHistoryVO } from '@/api/dcc/controlledFile/workflow'

export const getControlledDetailFormalVersions = (history: ControlledFileVersionHistoryVO[] = []) =>
  history.filter(row =>
    ['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE', 'SUPERSEDED', 'OBSOLETE'].includes(row.status)
    // Closing a working iteration also sets SUPERSEDED; status alone does not prove control.
    && (Boolean(row.controlledTime) || row.publishedTime != null))

export const getControlledDetailPredecessorFact = (file: ControlledFileVO) => {
  const versions = getControlledDetailFormalVersions(file.versionHistory)
  const predecessors = versions.filter(row => String(row.supersededByFileId) === String(file.id))
  const obsolete = predecessors.filter(row => row.status === 'OBSOLETE' && row.obsoletedTime != null)
  if (obsolete.length) return { kind: 'obsolete' as const, versions: obsolete }
  // predecessorControlledFileId can point at the selected draft, including on INITIAL submissions.
  const hasFormalPredecessor = versions.some(row => String(row.id) === String(file.predecessorControlledFileId))
  const hasPredecessor = predecessors.length > 0 || hasFormalPredecessor || Boolean(file.revisionSourceControlledFileId
    || file.revisionSourceVersionNo || file.revisionBaseActiveControlledFileId)
  if (hasPredecessor) return { kind: 'pending' as const, versions: [] }
  if (file.revisionChangeType === 'INITIAL')
    return { kind: 'initial' as const, versions: [] }
  return { kind: 'unknown' as const, versions: [] }
}

export const getControlledDetailTimeEvents = (
  file: ControlledFileVO, format: (value: string | number) => string
) => {
  const grouped = new Map<string, string[]>()
  for (const [label, value] of [
    ['受控', file.controlledTime], ['启用', file.activatedTime],
    ['发布', file.publishedTime], ['下发', file.distributedTime]
  ] as const) {
    if (value == null || value === '') continue
    const time = format(value)
    if (time === '-') continue
    grouped.set(time, [...(grouped.get(time) || []), label])
  }
  return [...grouped].map(([time, labels]) => ({ time, labels }))
}
