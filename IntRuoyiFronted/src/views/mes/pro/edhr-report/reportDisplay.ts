const statusLabels: Record<string, string> = {
  ACTIVE: '启用',
  INACTIVE: '停用',
  READY: '可用',
  ERROR: '数据源异常',
  RECORDED: '已记录',
  FAILED: '失败',
  PUBLISHED: '已发布',
  DRAFT: '草稿',
  PENDING: '待处理',
  DISABLED: '已禁用',
  VOID: '已作废',
  FIRST_SLICE_READY: '首阶段已就绪'
}
export const reportStatusLabel = (status?: string) =>
  status ? statusLabels[status] || `未识别状态（${status}）` : '未记录'
const sourceLabels: Record<string, string> = {
  mes_pro_batch_record_execution: '批记录执行记录',
  mes_pro_edhr_batch_execution: '电子批记录批次执行记录'
}
export const reportCellText = (field: string, value: string) =>
  field === 'sourceObject' ? sourceLabels[value] || `未标注数据来源（${value}）` : value
