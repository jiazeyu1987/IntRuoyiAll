const itemLabels: Record<string, string> = {
  WORK_ORDER: '生产订单',
  ROUTE_PROCESS: '路线工序',
  BATCH_RECORD_REPORT: '批记录报表',
  BATCH: '生产批次',
  EXECUTION_SNAPSHOT: '执行快照',
  FIELD_AUDIT_BASELINE: '字段审计基线',
  NONCONFORMANCE_REVIEW: '不合格评审'
}
const blockerLabels: Record<string, string> = {
  EDHR_DOMAIN_TRACE_WORK_ORDER_REQUIRED: '缺少生产订单关联或订单号',
  EDHR_DOMAIN_TRACE_ROUTE_PROCESS_REQUIRED: '缺少路线工序关联',
  EDHR_DOMAIN_TRACE_BATCH_RECORD_REPORT_REQUIRED: '缺少批记录报表关联',
  EDHR_DOMAIN_TRACE_BATCH_CODE_REQUIRED: '缺少生产批号',
  EDHR_DOMAIN_TRACE_EXECUTION_SNAPSHOT_REQUIRED: '缺少执行快照',
  EDHR_DOMAIN_TRACE_FIELD_AUDIT_BASELINE_REQUIRED: '缺少字段审计基线校验码或版本'
}
export const traceItemLabel = (type?: string) =>
  type ? itemLabels[type] || `未识别追溯对象（${type}）` : '未记录对象类型'
export const traceBlockerMessage = (item: { blockerCode?: string; blockerMessage?: string }) =>
  (item.blockerCode && blockerLabels[item.blockerCode]) ||
  item.blockerMessage ||
  '未提供阻塞原因，请查看技术详情并核查来源记录'
