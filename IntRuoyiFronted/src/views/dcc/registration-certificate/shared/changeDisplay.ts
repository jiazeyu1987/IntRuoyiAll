import { factLabels, factOptions, fieldsFor, parseFacts } from './facts'

const changeLabels: Record<string, string> = {
  CERTIFICATE_INFORMATION: '注册证信息',
  PRODUCTION_RELATION: '生产关系',
  PRODUCT_NAME: '产品名称',
  MODEL_SPECIFICATION: '型号规格',
  STRUCTURE_COMPOSITION: '结构组成',
  INTENDED_USE: '适用范围',
  TECHNICAL_REQUIREMENTS: '产品技术要求',
  REGISTRANT_NAME: '注册人名称',
  RESIDENCE_ADDRESS: '住所',
  PRODUCTION_ADDRESS: '生产地址',
  OTHER_CONTENT: '其他内容',
  CERTIFICATE_FILE: '注册证文件',
  CERTIFICATE_ATTACHMENT: '附件',
  FDA_PRODUCT_DESCRIPTION: 'FDA产品描述'
}

export const formatRegistrationCertificateChangeType = (type?: string) =>
  type ? changeLabels[type] ?? '未识别变更项目' : '记录缺失'

const scalar = (value: unknown): string => {
  if (value === undefined || value === null || value === '') return '未填写'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') throw new Error('变更字段格式异常')
  return String(value)
}

const enterpriseNames = (json: unknown) => {
  if (typeof json !== 'string') throw new Error('受托企业记录格式异常')
  const rows = JSON.parse(json)
  if (!Array.isArray(rows)) throw new Error('受托企业记录格式异常')
  return rows.map(row => {
    if (!row || typeof row.enterpriseName !== 'string' || !row.enterpriseName.trim()) {
      throw new Error('受托企业名称缺失')
    }
    return row.enterpriseName
  }).join('；') || '无'
}

const productionRelation = (entrusted: unknown, self: unknown, enterprises: unknown) => {
  if (typeof entrusted !== 'boolean' || typeof self !== 'boolean') throw new Error('生产关系记录缺失')
  return `委托生产：${scalar(entrusted)}\n自行生产：${scalar(self)}\n受托企业：${enterpriseNames(enterprises)}`
}

const certificateInformation = (json: string) => {
  const facts = parseFacts(json)
  const type = facts.certificateType
  const lines = [`注册证类型：${type === 'DOMESTIC' ? '国内' : type}`, `是否瑛泰产品：${scalar(facts.ownProduct)}`]
  if (facts.projectProduct) {
    lines.push(`关联产品：${scalar(facts.projectProduct.productName)}`, `关联产品代码：${scalar(facts.projectProduct.productCode)}`)
  }
  for (const field of fieldsFor(type)) {
    const value = facts[field]
    const options = factOptions(field, type)
    const text = value == null || value === '' ? '未填写' : options.length
      ? options.find(option => option.value === value)?.label ?? '字段选项异常'
      : scalar(value)
    lines.push(`${factLabels[field]}：${text}`)
  }
  if (type === 'DOMESTIC' && !facts.ownProduct) lines.push(`项目代码：${scalar(facts.projectCode)}`)
  if (type === 'CE') {
    lines.push(`证书Rev.记录：${(facts.ceCertificates ?? []).map(row =>
      `Rev. ${scalar(row.revision)} / 证书编号：${scalar(row.certificateNo)} / 签发日期：${scalar(row.issueDate)}`
    ).join('；') || '无'}`)
  }
  return lines.join('\n')
}

export const formatRegistrationCertificateChangeValue = (itemType: string, raw?: string): string => {
  if (!raw) return '记录缺失'
  try {
    const data = JSON.parse(raw)
    if (!data || typeof data !== 'object' || !Object.hasOwn(data, 'value')) return '记录格式异常'
    if (itemType === 'CERTIFICATE_INFORMATION') return certificateInformation(data.value)
    if (itemType === 'PRODUCTION_RELATION') {
      // Both before/after snapshots encode [entrustedProduction, selfProduction, enterprisesJson].
      const relation = JSON.parse(data.value)
      if (!Array.isArray(relation) || relation.length !== 3) return '生产关系记录格式异常'
      return productionRelation(...relation as [unknown, unknown, unknown])
    }
    if (itemType === 'PRODUCTION_ADDRESS' && Object.hasOwn(data, 'entrustedProduction')) {
      return `生产地址：${scalar(data.value)}\n${productionRelation(data.entrustedProduction, data.selfProduction, data.entrustedEnterprisesJson)}`
    }
    return scalar(data.value)
  } catch (error) {
    return `记录解析失败：${error instanceof SyntaxError ? '数据格式异常' : error instanceof Error ? error.message : '数据异常'}`
  }
}
