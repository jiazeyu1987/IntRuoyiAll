export const signoffIdentity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number') ||
      (typeof value === 'number' && !Number.isSafeInteger(value)) ||
      !/^[1-9][0-9]*$/.test(String(value)) || BigInt(value) > 9223372036854775807n)
    throw new Error('文件类别或会签部门身份无效')
  return String(value)
}

export const normalizeSignoffDepartments = (value: unknown, allowDefault = false): string[] | undefined => {
  if (value === undefined && allowDefault) return undefined
  if (!Array.isArray(value) || !value.length) throw new Error('请选择至少一个会签部门')
  const result = value.map(signoffIdentity)
  if (new Set(result).size !== result.length) throw new Error('会签部门不能重复')
  return result
}

export const signoffCategoryKey = (categoryId: unknown, processType: string): string => {
  if (!['CONTROLLED_FILE', 'EXTERNAL_REVIEW'].includes(processType)) throw new Error('申请流程类型无效')
  return JSON.stringify([signoffIdentity(categoryId), processType])
}

export const signoffRequestKey = (categoryId: unknown, selected: unknown, processType: string): string =>
  JSON.stringify([signoffCategoryKey(categoryId, processType), normalizeSignoffDepartments(selected, true)?.sort()])

export const defaultSignoffDepartments = (response: unknown): string[] => {
  const value = response as { ready?: unknown; nodes?: Array<{ stageCode?: string; candidateSourceType?: string; candidateSourceIds?: unknown }>; blockers?: unknown[] }
  if (!value || typeof value.ready !== 'boolean' || !Array.isArray(value.nodes) || !Array.isArray(value.blockers))
    throw new Error('审批路线响应不完整')
  const nodes = value.nodes.filter(node => node.stageCode === 'MATRIX_REVIEW')
  if (nodes.length !== 1 || nodes[0].candidateSourceType !== 'DEPT')
    throw new Error('审批矩阵未带出唯一的会签部门节点')
  return normalizeSignoffDepartments(nodes[0].candidateSourceIds)!
}

export const signoffDepartmentOptions = (response: unknown): Array<{ id: string; name: string }> => {
  if (!Array.isArray(response)) throw new Error('启用部门目录响应缺失')
  const rows = response.map(row => {
    if (!row || typeof row.name !== 'string' || !row.name.trim() || (row.status != null && row.status !== 0))
      throw new Error('启用部门目录包含不可用的部门')
    return { id: signoffIdentity(row.id), name: row.name }
  })
  if (new Set(rows.map(row => row.id)).size !== rows.length) throw new Error('部门目录身份重复')
  return rows
}

export interface UploadApprovalAccount { id: string; name: string; deptName?: string }

const approvalIdentity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number') ||
    (typeof value === 'number' && !Number.isSafeInteger(value)) ||
    !/^[1-9][0-9]*$/.test(String(value)) || BigInt(value) > 9223372036854775807n)
    throw new Error('批准人账号身份无效')
  return String(value)
}

export const approvalUserOptions = (response: unknown): UploadApprovalAccount[] => {
  if (!Array.isArray(response)) throw new Error('启用账号目录响应缺失')
  const accounts = response.map(row => {
    if (!row || typeof row.nickname !== 'string' || !row.nickname.trim()
      || (row.status != null && row.status !== 0)
      || (row.deptName != null && typeof row.deptName !== 'string'))
      throw new Error('启用账号目录包含不可用的人员')
    return { id: approvalIdentity(row.id), name: row.nickname,
      ...(row.deptName ? { deptName: row.deptName } : {}) }
  })
  if (new Set(accounts.map(row => row.id)).size !== accounts.length) throw new Error('启用账号目录身份重复')
  return accounts
}

export const resolvedUploadApprovers = (response: unknown, accounts: UploadApprovalAccount[]): {
  accounts: UploadApprovalAccount[]; rule: string
} => {
  const route = response as { nodes?: Array<{ stageCode?: string; resolvedUserIds?: unknown;
    approveMethod?: string; requireAllApprovals?: boolean; approveRatio?: number | null }> }
  const nodes = route?.nodes?.filter(node => node.stageCode === 'MATRIX_APPROVAL')
  if (!nodes || nodes.length !== 1) throw new Error('审批矩阵未带出唯一的批准节点')
  const node = nodes[0]
  if (!Array.isArray(node.resolvedUserIds) || !node.resolvedUserIds.length) throw new Error('批准节点未带出真实批准人')
  const ids = node.resolvedUserIds.map(approvalIdentity)
  if (new Set(ids).size !== ids.length) throw new Error('批准人身份重复')
  const resolved = ids.map(id => {
    const account = accounts.find(row => row.id === id)
    if (!account) throw new Error(`批准人 ${id} 不在正式启用账号目录中，请重新读取并核对矩阵`)
    return { ...account }
  })
  if (!['ALL', 'ANY'].includes(node.approveMethod || '')) throw new Error('批准节点未返回可核对的批准规则')
  if (node.approveRatio != null && node.approveRatio !== 100) throw new Error('批准比例规则尚未明确，当前不可提交')
  const all = node.requireAllApprovals === true || node.approveMethod === 'ALL'
  return { accounts: resolved, rule: all ? '全部批准' : '矩阵当前配置：任一批准人通过' }
}
