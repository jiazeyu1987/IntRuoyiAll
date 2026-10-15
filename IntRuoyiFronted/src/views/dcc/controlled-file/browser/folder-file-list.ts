import type { FileCandidate } from '../relations/selector-state'
import type { ReferenceRow } from '../relations/project-reference-contract'
export interface FolderFileRow {
  key: string
  file: FileCandidate | ReferenceRow['selectedVersion']
  reference?: ReferenceRow
  projectCount?: number
  referenced: boolean
}
export const buildFolderFileRows = (
  files: FileCandidate[],
  references: ReferenceRow[],
  usage: Record<string, { referenceProjectCount: number; referenced: boolean }>,
  keyword: string,
  versionView: string = 'ALL'
): FolderFileRow[] => {
  const query = keyword.trim().toLocaleLowerCase()
  const owned = files.map((file) => ({
    key: `file:${file.controlledFileId}`,
    file,
    projectCount: usage[file.masterId]?.referenceProjectCount,
    referenced: usage[file.masterId]?.referenced === true
  }))
  const entries = references
    .filter(
      (row) =>
        versionView === 'ALL' ||
        (versionView === 'LATEST_CONTROLLED'
          ? row.selectedVersion.status === 'ACTIVE' && row.selectedVersion.controlled
            && row.selectedVersion.executable && !row.selectedVersion.pendingEffect
          : row.selectedVersion.status === versionView)
    )
    .filter(
      (row) =>
        !query ||
        [row.selectedVersion.fileName, row.selectedVersion.fileNumber, row.sourceProjectName].some(
          (text) => text?.toLocaleLowerCase().includes(query)
        )
    )
    .map((reference) => ({
      key: `reference:${reference.reference.id}`,
      file: reference.selectedVersion,
      reference,
      projectCount: reference.referenceProjectCount,
      referenced: true
    }))
  return [...owned, ...entries]
}
