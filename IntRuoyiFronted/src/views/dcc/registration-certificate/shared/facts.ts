export type CertificateType = 'DOMESTIC' | 'CE' | 'FDA'
export interface CertificateFacts {
  certificateType: CertificateType
  ownProduct: boolean
  certificateStatus?: string
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
  projectCode:'项目代码（非系统关联）',ownProduct:'是否瑛泰产品', ceRevision:'首条证书Rev.',ceCertificateNo:'首条证书编号',ceIssueDate:'首条证书签发日期',entrustedProduction:'是否委托生产',selfProduction:'是否自行生产',entrustedEnterpriseName:'受托企业',structureComposition:'结构组成',intendedUse:'适用范围／预期用途',technicalRequirements:'产品技术要求',residenceAddress:'住所／持证人地址',registrantName:'注册人名称',modelSpecification:'型号规格',productionAddress:'生产地址',certificateHolder: '证件持证主体', certificateType: '注册证类型', certificateStatus: '证件状态', registrationLink: '注册证链接', classification: '产品分类',
  productNameEn: '产品英文名称', productCode: '产品代码', tradeName: '商品名', notifiedBodyCode: '公告机构代码',
  submissionType: '注册提交类型', kpNumber: 'K/P号', regulationCode: '法规监管代码', approvalDate: '批准日期',
  listingNumber: '列名编号', listingDate: '列名日期', customerHeld: '是否客户持证',
  approvalLetterStatus: '批准信函状态', factoryRegistrationStatus: '工厂登记状态', certificateNo: '注册证号',
  firstObtainedDate: '首次获证日期', effectiveDate: '生效日期', expiryDate: '有效期至', remark: '备注', changeDescription: '变更描述'
}
export const fieldsFor = (type: CertificateType) => [
  'classification', 'certificateHolder', 'registrationLink',
  ...(type === 'DOMESTIC' ? ['certificateNo', 'firstObtainedDate', 'approvalDate', 'effectiveDate', 'expiryDate'] : []),
  ...(type === 'CE' ? ['approvalDate', 'expiryDate', 'productNameEn', 'productCode', 'tradeName', 'notifiedBodyCode'] : []),
  ...(type === 'FDA' ? ['productNameEn', 'submissionType', 'kpNumber', 'regulationCode', 'productCode', 'approvalDate', 'listingNumber', 'listingDate', 'customerHeld', 'approvalLetterStatus', 'factoryRegistrationStatus'] : ['certificateStatus']),
  'remark', 'changeDescription'
]
export const factOptions = (field: string, type: CertificateType): Array<{label: string; value: any}> => {
  if (field === 'certificateStatus') return [{value:'VALID',label:'有效'}, ...(type === 'CE' ? [{value:'SUSPENDED',label:'暂停'}] : []), {value:'REVOKED',label:'撤销'}, {value:'EXPIRED',label:'失效'}]
  if (field === 'submissionType') return submissionTypes
  if (field === 'approvalLetterStatus') return [{value:'VALID',label:'有效'},{value:'REVOKED',label:'撤销'}]
  if (field === 'factoryRegistrationStatus') return [{value:'ACTIVE',label:'Active 激活'},{value:'INACTIVE',label:'Inactive 非激活'}]
  if (['ownProduct','entrustedProduction','selfProduction'].includes(field)) return [{value:true,label:'是'},{value:false,label:'否'}]
  if (field === 'customerHeld') return [{value:true,label:'是'},{value:false,label:'否'}]
  return []
}
export const validateFacts = (facts: CertificateFacts) => {
  const required = ['classification', ...(facts.certificateType === 'DOMESTIC' ? ['certificateNo','firstObtainedDate','effectiveDate','expiryDate'] : []), ...(facts.certificateType === 'CE' ? ['expiryDate'] : []), ...(facts.certificateType === 'FDA' ? ['submissionType','customerHeld','approvalLetterStatus','factoryRegistrationStatus'] : ['certificateStatus'])]
  for (const field of required) if (facts[field] === undefined || facts[field] === null || facts[field] === '') throw new Error(`请填写${factLabels[field]}`)
  const revisions = new Set<string>()
  for (const row of facts.ceCertificates ?? []) { if (!row.revision?.trim() || revisions.has(row.revision.trim())) throw new Error('CE证书Rev.须填写且不能重复'); revisions.add(row.revision.trim()) }
  if (facts.effectiveDate && facts.expiryDate && facts.effectiveDate >= facts.expiryDate) throw new Error('生效日期必须早于有效期至')
  if (facts.firstObtainedDate && facts.effectiveDate && facts.firstObtainedDate > facts.effectiveDate) throw new Error('首次获证日期不能晚于生效日期')
}
