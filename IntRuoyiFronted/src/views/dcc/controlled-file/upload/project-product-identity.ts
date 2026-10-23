import type { ControlledFileProjectProduct } from '@/api/dcc/controlledFile/workflow'

const identity = (value: unknown): string => {
  if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error('项目产品身份发生精度损失')
  if (typeof value !== 'string' && typeof value !== 'number') throw new Error('项目产品身份缺失')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id) || BigInt(id) > 9223372036854775807n) throw new Error('项目产品身份不合法')
  return id
}

export const readProjectProductIdentity = (response: unknown, selectedProjectId: string | number): ControlledFileProjectProduct => {
  if (!response || typeof response !== 'object') throw new Error('正式项目产品预览缺失')
  const value = response as Record<string, unknown>
  const projectId = identity(selectedProjectId)
  if (typeof value.projectCodeId !== 'string' || identity(value.projectCodeId) !== projectId)
    throw new Error('正式产品预览与所选项目身份不一致')
  const keys = ['productMasterId', 'productCatalogId', 'productRelationId', 'productCreateRequestId'] as const
  for (const key of keys) {
    if (value[key] !== null && (typeof value[key] !== 'string' || identity(value[key]) !== value[key]))
      throw new Error('正式产品预览的身份字段缺失或精度不合法')
  }
  if (value.source === 'MDM_MASTER') {
    if (!value.productMasterId || keys.slice(1).some(key => value[key] !== null)) throw new Error('MDM产品身份与来源不一致')
  } else if (value.source === 'DCC_CATALOG') {
    if (value.productMasterId !== null || keys.slice(1).some(key => value[key] === null)) throw new Error('已批准项目产品缺少目录、关系或申请身份')
  } else if (value.source === 'UNBOUND') {
    if (keys.some(key => value[key] !== null) || value.productCode !== null || value.productName !== null)
      throw new Error('未绑定产品预览包含矛盾身份')
  } else throw new Error('正式项目产品来源不合法')
  if (value.source !== 'UNBOUND' && (typeof value.productCode !== 'string' || !value.productCode.trim()
    || typeof value.productName !== 'string' || !value.productName.trim())) throw new Error('正式产品编码或名称缺失')
  return { projectCodeId: projectId, source: value.source, productMasterId: value.productMasterId as string | null,
    productCatalogId: value.productCatalogId as string | null, productRelationId: value.productRelationId as string | null,
    productCreateRequestId: value.productCreateRequestId as string | null,
    productCode: value.productCode as string | null, productName: value.productName as string | null }
}

export const formatProjectProductSource = (source: ControlledFileProjectProduct['source']): string => {
  if (source === 'MDM_MASTER') return '产品主数据'
  if (source === 'DCC_CATALOG') return '已批准项目产品目录'
  if (source === 'UNBOUND') return '未绑定正式产品'
  throw new Error('正式项目产品来源不合法')
}
