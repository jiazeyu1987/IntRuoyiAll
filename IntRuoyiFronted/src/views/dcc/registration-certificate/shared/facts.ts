import type { DccRegistrationCertificateProjectProduct } from '@/api/dcc/registrationCertificate'

export type CertificateType = 'DOMESTIC' | 'CE' | 'FDA'
export interface CertificateFacts {
  certificateType: CertificateType
  ownProduct: boolean
  projectProducts?: DccRegistrationCertificateProjectProduct[]
  projectProduct?: DccRegistrationCertificateProjectProduct
  certificateStatus?: string
  modelSpecifications?: string[]
  ceCertificates?: Array<{ revision: string; certificateNo?: string; issueDate?: string }>
  [field: string]: any
}
export const emptyFacts = (): CertificateFacts => ({ certificateType: 'DOMESTIC', ownProduct: true, certificateStatus: 'VALID', classification: '', ceCertificates: [] })
export const parseFacts = (json?: string): CertificateFacts => {
  if (!json) throw new Error('注册证完整信息缺失，请先执行注册证字段迁移')
  const facts = JSON.parse(json)
  if (!facts || !['DOMESTIC', 'CE', 'FDA'].includes(facts.certificateType)) throw new Error('注册证类型无效')
  return facts
}
export const submissionTypes = [
  { value: 'EXEMPT', label: '豁免' }, { value: 'PMA', label: 'PMA' },
  { value: 'TRADITIONAL_510K', label: 'Traditional 510(k)' }, { value: 'SPECIAL_510K', label: 'Special 510(k)' },
  { value: 'ABBREVIATED_510K', label: 'Abbreviated 510(k)' }
]
export const factLabels: Record<string, string> = {
  projectCode:'项目代码',ownProduct:'是否瑛泰产品', ceRevision:'首条证书Rev.',ceCertificateNo:'首条证书编号',ceIssueDate:'首条证书签发日期',entrustedProduction:'是否委托生产',selfProduction:'是否自行生产',entrustedEnterpriseName:'受托企业',structureComposition:'结构组成',intendedUse:'适用范围／预期用途',technicalRequirements:'产品技术要求',residenceAddress:'住所／持证人地址',registrantName:'注册人名称',modelSpecification:'型号规格',productionAddress:'生产地址',certificateHolder: '证件持证主体', certificateType: '注册证类型', certificateStatus: '证件状态', registrationLink: '注册证链接', classification: '产品分类',
  productNameEn: '产品英文名称', productCode: '产品代码', tradeName: '商品名', notifiedBodyCode: '公告机构代码',
  submissionType: '注册提交类型', kpNumber: 'K/P号', regulationCode: '法规监管代码', approvalDate: '批准日期',
  listingNumber: '列名编号', listingDate: '列名日期', customerHeld: '是否客户持证',
  approvalLetterStatus: '批准信函状态', factoryRegistrationStatus: '工厂登记状态', certificateNo: '注册证号',
  firstObtainedDate: '首次获证日期', effectiveDate: '生效日期', expiryDate: '有效期至', remark: '备注', otherContentDescription: '其他内容说明', changeDescription: '变更描述'
}
export const fieldsFor = (type: CertificateType) => [
  'classification', 'certificateHolder', 'registrationLink',
  ...(type === 'DOMESTIC' ? ['certificateNo', 'firstObtainedDate', 'approvalDate', 'effectiveDate', 'expiryDate'] : []),
  ...(type === 'CE' ? ['approvalDate', 'expiryDate', 'productNameEn', 'productCode', 'tradeName', 'notifiedBodyCode'] : []),
  ...(type === 'FDA' ? ['productNameEn', 'submissionType', 'kpNumber', 'regulationCode', 'productCode', 'approvalDate', 'listingNumber', 'listingDate', 'customerHeld', 'approvalLetterStatus', 'factoryRegistrationStatus'] : ['certificateStatus']),
  'remark', 'changeDescription', 'otherContentDescription'
]
export const creationFieldsFor = (type: CertificateType) => [
  'classification', 'certificateHolder', 'registrationLink',
  ...(type === 'DOMESTIC' ? ['certificateNo', 'firstObtainedDate', 'approvalDate', 'effectiveDate', 'expiryDate', 'certificateStatus'] : []),
  ...(type === 'CE' ? ['approvalDate', 'expiryDate', 'productNameEn', 'productCode', 'tradeName', 'notifiedBodyCode', 'certificateStatus'] : []),
  ...(type === 'FDA' ? ['productNameEn', 'submissionType', 'kpNumber', 'regulationCode', 'productCode', 'approvalDate', 'listingNumber', 'listingDate', 'customerHeld', 'approvalLetterStatus', 'factoryRegistrationStatus'] : []),
  'remark'
]
export const validateCreationFacts = (facts: CertificateFacts) => {
  const removed = ['changeDescription', ...(facts.certificateType === 'CE' ? ['firstObtainedDate', 'modelSpecifications'] : [])]
  for (const field of removed) {
    if (facts[field] != null) throw new Error(`新建${facts.certificateType === 'DOMESTIC' ? '国内' : facts.certificateType}注册证不支持${field === 'modelSpecifications' ? '型号规格' : factLabels[field]}字段`)
  }
  validateFacts(facts)
  if (facts.certificateType === 'DOMESTIC' && facts.ownProduct === false && !facts.projectCode?.trim()) throw new Error('请填写实际项目代码')
  if (facts.certificateType !== 'FDA') {
    if (!facts.approvalDate) throw new Error('请填写批准日期')
    if (facts.certificateType === 'DOMESTIC' && facts.effectiveDate && facts.approvalDate > facts.effectiveDate) throw new Error('批准日期不能晚于生效日期')
    if (facts.certificateType === 'CE' && facts.expiryDate && facts.approvalDate >= facts.expiryDate) throw new Error('批准日期必须早于有效期至')
  }
}
export const factOptions = (field: string, type: CertificateType): Array<{label: string; value: any}> => {
  if (field === 'certificateStatus') return [{value:'VALID',label:'有效'}, ...(type === 'CE' ? [{value:'SUSPENDED',label:'暂停'}] : []), {value:'REVOKED',label:'撤销'}, {value:'EXPIRED',label:'失效'}]
  if (field === 'submissionType') return submissionTypes
  if (field === 'approvalLetterStatus') return [{value:'VALID',label:'有效'},{value:'REVOKED',label:'撤销'}]
  if (field === 'factoryRegistrationStatus') return [{value:'ACTIVE',label:'Active 激活'},{value:'INACTIVE',label:'Inactive 非激活'}]
  if (['ownProduct','entrustedProduction','selfProduction'].includes(field)) return [{value:true,label:'是'},{value:false,label:'否'}]
  if (field === 'customerHeld') return [{value:true,label:'是'},{value:false,label:'否'}]
  return []
}
export const requiresKpNumber = (facts: CertificateFacts) => facts.certificateType === 'FDA' && facts.submissionType === 'TRADITIONAL_510K'
export const validateFacts = (facts: CertificateFacts) => {
  for (const [field, max] of [['certificateNo', 128], ['classification', 64], ['remark', 1024]] as const) {
    if (facts[field]?.length > max) throw new Error(`${factLabels[field]}长度不能超过${max}个字符`)
  }
  if (facts.certificateType === 'CE') {
    facts.ceCertificates?.forEach((row, index) => {
      if (row.certificateNo && row.certificateNo.length > 128) throw new Error(`第${index + 1}条CE证书编号长度不能超过128个字符`)
    })
  }
  if (requiresKpNumber(facts) && (typeof facts.kpNumber !== 'string' || !facts.kpNumber.trim())) throw new Error('请填写K/P号')
  const required = ['classification', ...(facts.certificateType === 'DOMESTIC' ? ['certificateNo','firstObtainedDate','effectiveDate','expiryDate'] : []), ...(facts.certificateType === 'CE' ? ['expiryDate'] : []), ...(facts.certificateType === 'FDA' ? ['submissionType','customerHeld','approvalLetterStatus','factoryRegistrationStatus'] : ['certificateStatus'])]
  for (const field of required) if (facts[field] === undefined || facts[field] === null || facts[field] === '') throw new Error(`请填写${factLabels[field]}`)
  if (facts.certificateType === 'CE' && facts.modelSpecifications) {
    const models = facts.modelSpecifications.map(model => model.trim())
    if (models.some(model => !model) || new Set(models).size !== models.length) throw new Error('型号规格须填写且不能重复')
    facts.modelSpecifications = models
  }
  const revisions = new Set<string>()
  for (const row of facts.ceCertificates ?? []) { if (!row.revision?.trim() || revisions.has(row.revision.trim())) throw new Error('CE证书Rev.须填写且不能重复'); revisions.add(row.revision.trim()) }
  if (facts.effectiveDate && facts.expiryDate && facts.effectiveDate >= facts.expiryDate) throw new Error('生效日期必须早于有效期至')
  if (facts.firstObtainedDate && facts.effectiveDate && facts.firstObtainedDate > facts.effectiveDate) throw new Error('首次获证日期不能晚于生效日期')
}

export const validateChangeFacts = (facts: CertificateFacts, before: CertificateFacts, newProductDescriptions = false) => {
  validateFacts(facts)
  const modelsChanged = facts.certificateType === 'CE' && JSON.stringify(facts.modelSpecifications) !== JSON.stringify(before.modelSpecifications)
  const approvalChanged = facts.certificateType === 'FDA' && (newProductDescriptions || ['submissionType', 'kpNumber', 'regulationCode', 'productCode', 'approvalDate', 'listingNumber', 'listingDate'].some(key => facts[key] !== before[key]))
  if ((modelsChanged || approvalChanged) && (!facts.changeDescription?.trim() || facts.changeDescription.trim() === before.changeDescription?.trim())) throw new Error(modelsChanged ? 'CE型号调整必须填写本次调整说明' : '新增或替代FDA批准信息必须填写本次变更描述')
}
