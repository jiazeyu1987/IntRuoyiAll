import type { ApprovalTaskSummaryVO, ApprovalTaskPageReqVO } from '@/api/approval-center'

export const DCC_OFFLINE_TRAINING_RECORD = 'DCC_OFFLINE_TRAINING_RECORD'
export const DCC_WORKBENCH_PATH = '/dcc/controlled-file/workbench'
const fileIdentity = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value) || BigInt(value) > 9223372036854775807n) throw new Error('线下培训记录文件身份无效')
  return value
}
const processIdentity = (value: unknown): string => {
  if (typeof value !== 'string' || !value || value !== value.trim() || value.length > 64 || /[\u0000-\u001f\u007f]/.test(value)) throw new Error('线下培训记录实际BPM身份缺失')
  return value
}
export interface OfflineTrainingRecordLocation { path: string; query: Record<string, string> }
export interface OfflineTrainingRecordTodo {
  id: string; fileId: string; processInstanceId: string; fileName: string; fileNumber: string; versionNo: string
  summary: ApprovalTaskSummaryVO; location: OfflineTrainingRecordLocation
}
export const readOfflineTrainingManagementLocation = (fileId: unknown, bpm: unknown, path: string, query: Record<string, string>): OfflineTrainingRecordLocation => {
  const id = fileIdentity(fileId), processInstanceId = processIdentity(bpm)
  const keys = ['management', 'from', 'returnTo', 'processInstanceId']
  if (path !== `/dcc/controlled-file/detail/${id}` || Object.keys(query).length !== 4 || Object.keys(query).some(key => !keys.includes(key))
    || query.management !== '1' || query.from !== 'workbench' || query.returnTo !== DCC_WORKBENCH_PATH || query.processInstanceId !== processInstanceId) {
    throw new Error('线下培训记录管理入口与正式文件、BPM或返回上下文不一致')
  }
  return { path, query: { management: '1', from: 'workbench', returnTo: DCC_WORKBENCH_PATH, processInstanceId } }
}
export const resolveOfflineTrainingRecordLocation = (row: ApprovalTaskSummaryVO, path = row.detailRoute, query = row.detailQuery || {}): OfflineTrainingRecordLocation => {
  if (row.moduleCode !== 'DCC' || row.sourceTaskType !== DCC_OFFLINE_TRAINING_RECORD || row.businessDeleted
    || row.requiresSignature !== false || row.availableActions?.length !== 1 || row.availableActions[0] !== 'PROCESS_IN_MODULE') {
    throw new Error('线下培训记录待办来源或办理合同不完整')
  }
  return readOfflineTrainingManagementLocation(row.businessKey, row.processInstanceId, path, query)
}
export const readOfflineTrainingRecordTodo = (row: ApprovalTaskSummaryVO): OfflineTrainingRecordTodo => {
  const location = resolveOfflineTrainingRecordLocation(row)
  const versions = (row.businessContextTags || []).filter(tag => /^版本[:：]/.test(tag)).map(tag => tag.replace(/^版本[:：]\s*/, '').trim())
  if (!row.id?.trim() || !row.sourceTaskId?.trim() || !row.businessTitle?.trim() || !row.businessCode?.trim() || versions.length !== 1 || !versions[0]) {
    throw new Error('线下培训记录待办缺少准确文件编号、版本或来源')
  }
  return { id: row.id, fileId: fileIdentity(row.businessKey), processInstanceId: processIdentity(row.processInstanceId),
    fileName: row.businessTitle, fileNumber: row.businessCode, versionNo: versions[0], summary: { ...row, detailQuery: { ...location.query } }, location }
}
export const loadOfflineTrainingRecordTodos = async (
  loadPage: (query: ApprovalTaskPageReqVO) => Promise<{ list: ApprovalTaskSummaryVO[]; total: number }>,
  isCurrent: () => boolean
): Promise<OfflineTrainingRecordTodo[]> => {
  let total: number | undefined, count = 0, pageNo = 1
  const seen = new Set<string>(), records: OfflineTrainingRecordTodo[] = []
  do {
    if (!isCurrent()) throw new Error('线下培训记录待办上下文已变化')
    const page = await loadPage({ moduleCode: 'DCC', viewType: 'TODO', pageNo, pageSize: 100 })
    if (!isCurrent()) throw new Error('线下培训记录待办上下文已变化')
    if (!page || !Array.isArray(page.list) || !Number.isSafeInteger(page.total) || page.total < 0
      || (total !== undefined && total !== page.total) || (page.list.length === 0 && count < page.total)) throw new Error('线下培训记录待办分页事实变化或不完整，请刷新')
    total = page.total
    for (const row of page.list) {
      if (!row?.id || row.moduleCode !== 'DCC' || seen.has(row.id)) throw new Error('DCC待办分页身份重复或来源不一致')
      seen.add(row.id)
      if (row.sourceTaskType === DCC_OFFLINE_TRAINING_RECORD) records.push(readOfflineTrainingRecordTodo(row))
    }
    count += page.list.length
    if (count > total) throw new Error('DCC待办分页总数与实际行数不一致')
    pageNo++
  } while (count < total)
  return records
}
