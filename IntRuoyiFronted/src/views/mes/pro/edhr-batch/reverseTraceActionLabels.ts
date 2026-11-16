const actionLabels: Record<string, string> = {
  BATCH_FIELD_SAVED: '保存批记录字段',
  DYNAMIC_FORM_SAVED: '保存动态表单',
  FIELD_CHANGE_BEFORE: '字段变更前记录',
  FIELD_CHANGE_AFTER: '字段变更后记录',
  FORMAL_MATERIAL_LOT: '关联正式物料批次',
  LISTING_RELEASE_APPROVAL: '批准上市放行',
  LISTING_RELEASE_SUBMIT: '提交上市放行',
  PARAMETER_RECORDED: '记录工艺参数',
  PQC_EQUIPMENT_USED: '使用过程检验器具',
  PQC_ITEM_RESULT: '记录过程检验结果',
  PQC_OPERATOR: '填写过程检验记录',
  PQC_RELEASE_APPLICATION: '申请生产放行',
  PQC_RELEASE_APPROVAL: '批准生产放行',
  PQC_REVIEW: '复核过程检验记录',
  PQC_REVIEW_SIGNATURE: '签署过程检验复核',
  PQC_SIGNATURE: '签署过程检验记录',
  PRODUCTION_DEVICE_USED: '使用生产设备',
  PRODUCTION_OPERATOR: '填写生产记录',
  PRODUCTION_REVIEW: '复核生产记录',
  PRODUCTION_REVIEW_SIGNATURE: '签署生产复核',
  PRODUCTION_SIGNATURE: '签署生产记录'
}

export const formatReverseTraceAction = (action: string) => {
  const label = actionLabels[action]
  if (!label) throw new Error(`反查来源动作缺少中文名称：${action}`)
  return label
}
