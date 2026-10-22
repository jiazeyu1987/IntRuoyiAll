import type { ApprovalMatrixBaselineVO, ControlledFileRuntimeRuleGroupVO } from '@/api/dcc/controlledFile/workflow'

export const signoffIdentity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number') ||
      (typeof value === 'number' && !Number.isSafeInteger(value)) ||
      !/^[1-9][0-9]*$/.test(String(value)) || BigInt(value) > 9223372036854775807n)
    throw new Error('文件类别或会签部门身份无效')
  return String(value)
}

export const normalizeSignoffDepartments = (value: unknown, allowDefault = false): string[] | undefined => {
  if (value === undefined && allowDefault) return undefined
  if (!Array.isArray(value) || !value.length) throw new Error('请至少选择一个会签部门')
  const result = value.map(signoffIdentity)
  if (new Set(result).size !== result.length) throw new Error('会签部门不能重复')
  return result
}

export const normalizeDraftSignoffDepartments = (value: unknown): string[] | null => {
  if (value === null) return null
  if (!Array.isArray(value)) throw new Error('草稿会签部门实际选择缺失')
  const ids = value.map(signoffIdentity)
  if (new Set(ids).size !== ids.length) throw new Error('草稿会签部门不能重复')
  return ids
}

export const normalizeDistributionDepartments = (value: unknown): string[] => {
  if (!Array.isArray(value)) throw new Error('本次下发部门名单未填写，请先核对类型默认名单')
  const result = value.map(signoffIdentity)
  if (new Set(result).size !== result.length) throw new Error('下发部门不能重复')
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

export const initialUploadDepartmentSelection = (defaultRouteResponse: unknown) => {
  const ids = defaultSignoffDepartments(defaultRouteResponse)
  return { selectedSignoffDepartmentIds: [...ids], selectedDistributionDepartmentIds: [...ids] }
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

export interface UploadApprovalAccount { id: string; name: string; deptId?: string; deptName?: string }
export interface UploadApplicationAccount extends UploadApprovalAccount { deptId: string; deptName: string }
export interface UploadApprovalGroup extends ControlledFileRuntimeRuleGroupVO {
  subjectName: string; subjectDepartmentPath: string | null; summary: string
}

const accountIdentity = (value: unknown, label: string): string => {
  if ((typeof value !== 'string' && typeof value !== 'number') ||
    (typeof value === 'number' && !Number.isSafeInteger(value)) ||
    !/^[1-9][0-9]*$/.test(String(value)) || BigInt(value) > 9223372036854775807n)
    throw new Error(`${label}账号身份无效`)
  return String(value)
}
const approvalIdentity = (value: unknown): string => accountIdentity(value, '批准人')

export const approvalUserOptions = (response: unknown): UploadApprovalAccount[] => {
  if (!Array.isArray(response)) throw new Error('启用账号目录响应缺失')
  const accounts = response.map(row => {
    if (!row || typeof row.nickname !== 'string' || !row.nickname.trim()
      || (row.status != null && row.status !== 0)
      || (row.deptName != null && typeof row.deptName !== 'string'))
      throw new Error('启用账号目录包含不可用的人员')
    return { id: approvalIdentity(row.id), name: row.nickname,
      ...(row.deptId == null ? {} : { deptId: approvalIdentity(row.deptId) }),
      ...(row.deptName ? { deptName: row.deptName } : {}) }
  })
  if (new Set(accounts.map(row => row.id)).size !== accounts.length) throw new Error('启用账号目录身份重复')
  return accounts
}

export const uploadApplicationActorKey = (actor: { id?: unknown; deptId?: unknown } | undefined, tenantId: unknown): string =>
  JSON.stringify([actor?.id, actor?.deptId, tenantId])

export const requireUploadApplicationAccount = (value: UploadApprovalAccount | undefined, label: string): UploadApplicationAccount => {
  if (!value || typeof value.id !== 'string' || typeof value.deptId !== 'string'
    || typeof value.name !== 'string' || !value.name.trim() || typeof value.deptName !== 'string' || !value.deptName.trim())
    throw new Error(`提交确认缺少${label}的真实账号、名称或正式部门`)
  return { id: accountIdentity(value.id, label), name: value.name, deptId: accountIdentity(value.deptId, `${label}部门`), deptName: value.deptName }
}

export const resolvedUploadApplicationPeople = (response: unknown, actor: { id?: unknown; deptId?: unknown },
  accounts: UploadApprovalAccount[]): { compiler: UploadApplicationAccount; supervisor: UploadApplicationAccount } => {
  const route = response as { ready?: boolean; nodes?: Array<{ stageCode?: string; resolvedUserIds?: unknown }> }
  if (route?.ready !== true || !Array.isArray(route.nodes)) throw new Error('编制人及直属主管正式审批预检尚未就绪')
  const nodes = route.nodes.filter(node => node.stageCode === 'SUPERVISOR_REVIEW')
  if (nodes.length !== 1 || !Array.isArray(nodes[0].resolvedUserIds) || nodes[0].resolvedUserIds.length !== 1)
    throw new Error('审批预检未带出唯一直属主管节点和真实账号')
  const compilerId = accountIdentity(actor?.id, '编制人'), compilerDeptId = accountIdentity(actor?.deptId, '编制人部门')
  const supervisorId = accountIdentity(nodes[0].resolvedUserIds[0], '直属主管')
  if (!Array.isArray(accounts)) throw new Error('启用账号目录响应缺失')
  const resolve = (id: string, label: string): UploadApplicationAccount => {
    const matching = accounts.filter(account => account.id === id)
    if (matching.length !== 1) throw new Error(`${label}不在唯一正式启用账号目录中，请重新读取并核对`)
    return requireUploadApplicationAccount(matching[0], label)
  }
  const compiler = resolve(compilerId, '编制人'), supervisor = resolve(supervisorId, '直属主管')
  if (compiler.deptId !== compilerDeptId) throw new Error('当前登录编制人与正式启用目录部门不一致，请重新读取账号')
  return { compiler, supervisor }
}

export const resolvedUploadApprovers = (response: unknown, accounts: UploadApprovalAccount[]): {
  accounts: UploadApprovalAccount[]; rule: string; groups: UploadApprovalGroup[]; capabilityLimitations: string[]
} => {
  const route = response as { ready?: boolean; executionContract?: string; derivationVersion?: string; approvalSourceMode?: string; matrixAfter?: ApprovalMatrixBaselineVO | null;
    runtimeRuleGroups?: ControlledFileRuntimeRuleGroupVO[] | null; runtimeCapabilityLimitations?: string[] | null;
    nodes?: Array<{ stageCode?: string; resolvedUserIds?: unknown; candidateSourceType?: string; candidateSourceIds?: Array<number | string>;
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
  const publicExecution = (route.executionContract === 'PUBLIC_MATRIX_NATIVE_V1' || route.executionContract === 'PUBLIC_MATRIX_NATIVE_V2')
    && !(route.approvalSourceMode === 'APPLICATION_CUSTOM' && route.derivationVersion === 'DCC_APPLICATION_CUSTOM_MATRIX_V1')
  if (!publicExecution && (route.matrixAfter != null || route.derivationVersion === 'DCC_PUBLIC_MATRIX_NATIVE_V1'))
    throw new Error('PUBLIC原组响应执行合同缺失或错误，不能使用旧扁平批准展示')
  if (!publicExecution)
    return { accounts: resolved, rule: all ? '全部批准' : '矩阵当前配置：任一批准人通过', groups: [], capabilityLimitations: [] }
  if (typeof route.ready !== 'boolean' || !route.matrixAfter || !Array.isArray(route.matrixAfter.nodes)
    || !Array.isArray(route.runtimeRuleGroups) || !route.runtimeRuleGroups.length
    || !Array.isArray(route.runtimeCapabilityLimitations)
    || route.runtimeCapabilityLimitations.some(value => typeof value !== 'string' || !value.trim()))
    throw new Error('PUBLIC本次原批准组或能力事实缺失，请重新读取正式预检')
  const rawById = new Map(route.matrixAfter.nodes.map(raw => [raw.id, raw]))
  if (rawById.size !== route.matrixAfter.nodes.length) throw new Error('原组来源节点身份重复')
  const seen = new Set<string>()
  const textIdentity = (value: unknown): string => {
    if (typeof value !== 'string') throw new Error('原组或候选身份必须为精确字符串')
    return approvalIdentity(value)
  }
  const policyFields = ['stageType', 'marker', 'required', 'subjectType', 'candidateSourceType',
    'approveMethod', 'approveRatio', 'requireAllApprovals'] as const
  const result: UploadApprovalGroup[] = []
  for (const group of route.runtimeRuleGroups) {
    if (!group || !Array.isArray(group.candidateSourceIds) || !group.candidateSourceIds.length
      || typeof group.required !== 'boolean' || typeof group.requireAllApprovals !== 'boolean' || typeof group.applied !== 'boolean')
      throw new Error('本次原组的规则或实际参与事实不完整')
    const sourceId = textIdentity(group.sourceNodeId)
    if (seen.has(sourceId)) throw new Error('本次原组来源身份重复')
    seen.add(sourceId)
    const raw = rawById.get(sourceId)
    if (!raw || policyFields.some(field => group[field] !== raw[field])) throw new Error(`原组 ${sourceId} 与正式原规则事实不一致`)
    const candidates = group.candidateSourceIds.map(textIdentity)
    const rawCandidates = typeof raw.candidateSourceIds === 'string' && raw.candidateSourceIds.trim()
      ? raw.candidateSourceIds.split(',').map(value => {
        const text = value.replace(/^[\x00-\x20]+|[\x00-\x20]+$/g, '')
        if (!/^[+-]?[0-9]+$/.test(text)) throw new Error(`原组 ${sourceId} 的原候选CSV非法`)
        const id = BigInt(text)
        if (id <= 0n || id > 9223372036854775807n) throw new Error(`原组 ${sourceId} 的原候选身份越界`)
        return id.toString()
      }) : undefined
    if (new Set(candidates).size !== candidates.length || !rawCandidates || new Set(rawCandidates).size !== rawCandidates.length
      || JSON.stringify(rawCandidates) !== JSON.stringify(candidates))
      throw new Error(`原组 ${sourceId} 的精确候选身份与原配置不一致`)
    if (!['ANY', 'ALL'].includes(group.approveMethod) || !['●', '◎', '▲'].includes(group.marker || '')
      || group.approveMethod === 'ALL' && (!group.requireAllApprovals || group.approveRatio !== 100)
      || group.approveMethod === 'ANY' && (group.requireAllApprovals || group.approveRatio !== null))
      throw new Error(`原组 ${sourceId} 的批准规则前后矛盾`)
    // Signoff participation is chosen for this application; approval and document control keep their original requirements.
    if (group.stageType !== 'SIGNOFF' && (group.marker === '●' || group.required) && !group.applied)
      throw new Error(`原组 ${sourceId} 必需参与却未实际参与`)
    if (group.stageType !== 'APPROVAL') continue
    if (typeof raw.subjectName !== 'string' || !raw.subjectName.trim() || raw.subjectId == null)
      throw new Error(`原批准组 ${sourceId} 的正式主体缺失`)
    textIdentity(raw.subjectId)
    const candidateText = candidates.map(id => {
      if (group.candidateSourceType !== 'USER') return `身份 ${id}`
      const account = accounts.find(row => row.id === id)
      if (!account) throw new Error(`原批准组 ${sourceId} 的候选 ${id} 不在正式启用账号目录中`)
      return `${account.name}${account.deptName ? `（${account.deptName}）` : ''} · 账号ID ${id}`
    }).join('；')
    const subjectLabels: Record<string, string> = { USER: '账号', DEPT: '部门', POSITION: '岗位', DCC_POSITION: '文控岗位', ROLE: '角色', POST: '岗位' }
    const summary = `原主体 ${raw.subjectName} · ${subjectLabels[group.subjectType] || group.subjectType} ID ${raw.subjectId}${raw.subjectDepartmentPath == null ? '' : ` · ${raw.subjectDepartmentPath}`}；标记 ${group.marker}；${group.required ? '必需参与' : '非必需'}；实际参与：${group.applied ? '参与' : '未参与'}；${group.approveMethod === 'ALL' ? '组内全部批准' : '组内任一候选批准'}；比例 ${group.approveRatio === null ? '未设置' : `${group.approveRatio}%`}；全部批准 ${group.requireAllApprovals ? '是' : '否'}；候选来源 ${subjectLabels[group.candidateSourceType] || group.candidateSourceType}：${candidateText}`
    result.push({ ...group, candidateSourceIds: [...candidates], subjectName: raw.subjectName,
      subjectDepartmentPath: raw.subjectDepartmentPath, summary })
  }
  const rawApprovals = route.matrixAfter.nodes.filter(raw => raw.stageType === 'APPROVAL')
  if (!result.length || result.length !== rawApprovals.length || rawApprovals.some(raw => !result.some(group => group.sourceNodeId === raw.id)))
    throw new Error('PUBLIC原批准组缺失，不能从扁平批准节点推断')
  const applied = result.filter(group => group.applied)
  if (!applied.length) throw new Error('本次没有实际参与的原批准组')
  if (route.ready) {
    if (applied.some(group => group.candidateSourceType !== node.candidateSourceType)) throw new Error('运行批准候选来源与原组矛盾')
    const expectedCandidates = applied.flatMap(group => group.candidateSourceIds)
    const actualCandidates = node.candidateSourceIds?.map(approvalIdentity)
    if (!actualCandidates || JSON.stringify([...expectedCandidates].sort()) !== JSON.stringify([...actualCandidates].sort())
      || new Set(expectedCandidates).size !== expectedCandidates.length) throw new Error('运行批准候选与各原组的精确集合不一致')
    if (node.candidateSourceType === 'USER' && JSON.stringify([...ids].sort()) !== JSON.stringify([...expectedCandidates].sort()))
      throw new Error('运行批准账号与实际参与原组候选不一致')
    if (applied.length === 1) {
      const group = applied[0]
      if (node.approveMethod !== group.approveMethod || (node.approveRatio ?? null) !== group.approveRatio
        || node.requireAllApprovals !== group.requireAllApprovals) throw new Error('运行批准规则与原组策略矛盾')
    } else if (applied.some(group => group.approveMethod === 'ANY' && group.candidateSourceIds.length > 1)
      || node.approveMethod !== 'ALL' || node.approveRatio !== 100 || node.requireAllApprovals !== true)
      throw new Error('多个原批准组不能扁平为任一批准，当前缺少正式独立组执行能力')
  }
  return { accounts: resolved, rule: `各原批准组分别按本组规则履行${route.ready ? '' : '；正式运行预检尚未就绪'}`,
    groups: result, capabilityLimitations: [...route.runtimeCapabilityLimitations] }
}
