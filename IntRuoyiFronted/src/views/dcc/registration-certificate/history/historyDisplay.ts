const labels: Record<string, string> = {
  RENEWAL_UPLOADED: '延续注册资料上传',
  CHANGE_SUBMITTED: '变更申请提交',
  CHANGE_APPLIED: '变更已应用',
  ACTIVATION_APPLIED: '新版本生效',
  PRODUCT_NAME: '产品名称',
  REGISTRANT_NAME: '注册人名称',
  MODEL_SPECIFICATION: '型号规格',
  STRUCTURE_COMPOSITION: '结构组成',
  INTENDED_USE: '预期用途',
  TECHNICAL_REQUIREMENTS: '技术要求',
  RESIDENCE_ADDRESS: '注册人住所',
  PRODUCTION_ADDRESS: '生产地址',
  OTHER_CONTENT: '其他变更内容'
}
export const certificateHistoryLabel = (value?: string) =>
  value ? labels[value] || `未识别类型（${value}）` : '未记录'
