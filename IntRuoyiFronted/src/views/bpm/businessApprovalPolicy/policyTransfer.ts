import type { BusinessApprovalPolicyTransfer } from '@/api/bpm/businessApprovalPolicy'

const requiredFields = [
  'dataDomain', 'systemCode', 'objectType', 'actionCode', 'objectState',
  'policyMode', 'effectExecutorCode', 'status'
]
const optionalFields = ['processDefinitionKey', 'formPolicyType', 'formSlotsJson', 'remark']

/** Validate without rewriting or omitting any business configuration from the file. */
export function parsePolicyTransfer(text: string): BusinessApprovalPolicyTransfer {
  const value = JSON.parse(text)
  if (value?.schemaVersion === 1) {
    throw new Error('旧版审批策略文件不包含依赖流程，请从源租户重新导出版本2完整配置')
  }
  if (!value || value.schemaVersion !== 2 || value.format !== 'BUSINESS_APPROVAL_POLICIES' ||
      !Array.isArray(value.policies) || value.policies.length === 0 ||
      !Array.isArray(value.processes) ||
      Object.keys(value).some(key => !['schemaVersion', 'format', 'policies', 'processes'].includes(key))) {
    throw new Error('请选择系统导出的完整审批策略 JSON 文件（版本2，包含依赖流程）')
  }
  value.policies.forEach((item: Record<string, unknown>, index: number) => {
    const invalid = !item || typeof item !== 'object' || Array.isArray(item) ||
      requiredFields.some(key => typeof item[key] !== 'string' || !(item[key] as string).trim()) ||
      optionalFields.some(key => item[key] !== undefined && item[key] !== null && typeof item[key] !== 'string') ||
      Object.keys(item).some(key => !requiredFields.includes(key) && !optionalFields.includes(key)) ||
      !['BPM_REQUIRED', 'SIGNATURE_REQUIRED', 'DIRECT'].includes(item.policyMode as string) ||
      !['DRAFT', 'PUBLISHED', 'DISABLED'].includes(item.status as string)
    if (invalid) throw new Error(`第${index + 1}条审批策略字段不完整或包含不支持的内容`)
  })
  value.processes.forEach((process: Record<string, unknown>, index: number) => {
    const object = (v: unknown) => v !== null && typeof v === 'object' && !Array.isArray(v)
    if (!object(process) || ['key', 'name', 'bpmnXml'].some(key => typeof process[key] !== 'string' || !(process[key] as string).trim()) ||
        !object(process.category) || !object(process.metaInfo) || !Array.isArray(process.roles) ||
        !Array.isArray(process.startUsernames) || process.startUsernames.some(v => typeof v !== 'string' || !v.trim()) ||
        Object.keys(process).some(key => !['key', 'name', 'category', 'bpmnXml', 'metaInfo', 'roles', 'startUsernames'].includes(key)) ||
        process.roles.some(role => !object(role) || typeof role.code !== 'string' || !role.code.trim() ||
          typeof role.name !== 'string' || !role.name.trim() || !object(role.category) ||
          Object.keys(role).some(key => !['code', 'name', 'category'].includes(key)))) {
      throw new Error(`第${index + 1}个依赖流程配置不完整或包含不支持的内容`)
    }
  })
  const keys = value.processes.map((process: { key: string }) => process.key)
  const required = [...new Set(value.policies.filter((p: Record<string, unknown>) =>
    p.status === 'PUBLISHED' && p.policyMode === 'BPM_REQUIRED').map((p: Record<string, unknown>) => p.processDefinitionKey))]
  if (new Set(keys).size !== keys.length || keys.length !== required.length || required.some(key => !keys.includes(key))) {
    throw new Error('审批策略包的依赖流程不完整或重复，请从源租户重新导出')
  }
  return value
}
