import type { Component } from 'vue'
import { defineAsyncComponent } from 'vue'
import request from '@/config/axios'

export interface LogQueryField {
  key: string
  label: string
  required?: boolean
  options?: { label: string; value: string }[]
}
export interface LogSource {
  id: string
  label: string
  permissions: string[]
  component?: Component
  internal?: boolean
  fields?: LogQueryField[]
  // Each source uses its original read API and tenant/data-scope checks. No data copying.
  read?: (params: Record<string, any>) => Promise<{ list: Record<string, any>[]; total: number }>
  detail?: (row: Record<string, any>, params: Record<string, any>) => Promise<Record<string, any>>
}
const field = (key: string, label: string, required = false): LogQueryField => ({ key, label, required })
const page = (url: string) => async (params: Record<string, any>) => {
  const result = await request.get({ url, params })
  if (!Array.isArray(result?.list) || typeof result.total !== 'number') throw new Error('日志分页响应无效')
  return result
}
const list = (url: string, property?: string) => async (params: Record<string, any>) => {
  const result = await request.get({ url, params })
  const rows = property ? result?.[property] : result
  if (!Array.isArray(rows)) throw new Error('日志列表响应无效')
  const start = (params.pageNo - 1) * params.pageSize
  return { list: rows.slice(start, start + params.pageSize), total: rows.length }
}
const gxp = (id: string, label: string, permission: string, url: string, identity: string): LogSource => ({
  id, label, permissions: [permission],
  fields: [field(identity, identity === 'applicationId' ? '放行申请编号' : identity === 'batchExecutionId' ? '批次执行编号' : '活跃订单编号', true), field('operationId', '操作标识'), field('action', '操作'), field('resultStatus', '结果')],
  read: page(`${url}/page`),
  detail: (row, params) => request.get({ url: `${url}/get`, params: { [identity]: params[identity], eventId: row.id } })
})

export const logSources: LogSource[] = [
  { id: 'system', label: '系统操作（含工艺流程、QA、CRM）', permissions: ['system:operate-log:query'], component: defineAsyncComponent(() => import('./SystemOperateLogList.vue')) },
  { id: 'dcc', label: '文控业务', permissions: ['dcc:controlled-file:log:query'], component: defineAsyncComponent(() => import('@/views/dcc/controlled-file/logs/index.vue')) },
  { id: 'certificate-history', label: '注册证变更历史', permissions: ['dcc:registration-certificate:query-current'], fields: [field('certificateId', '注册证编号', true), field('versionId', '版本编号')], read: params => list(`/dcc/registration-certificates/${encodeURIComponent(params.certificateId)}/history`)(params) },
  { id: 'field', label: 'eDHR 字段审计', permissions: ['mes:pro-batch-record-execution:field-audit-query'], component: defineAsyncComponent(() => import('@/views/mes/pro/edhr/FieldAuditPage.vue')) },
  { id: 'field-detail', label: 'eDHR 字段审计详情', permissions: ['mes:pro-batch-record-execution:field-audit-query'], internal: true, component: defineAsyncComponent(() => import('@/views/mes/pro/edhr/FieldAuditDetailPage.vue')) },
  { id: 'edhr-operation', label: 'eDHR 操作审计', permissions: ['mes:pro-edhr-operation-audit:query'], component: defineAsyncComponent(() => import('@/views/mes/pro/edhr/OperationAuditPage.vue')) },
  { id: 'batch-operation-audit', label: 'eDHR 批次操作审计', permissions: ['mes:pro-edhr-operation-audit:query'], fields: [field('batchExecutionId', '批次执行编号', true), field('executionId', '执行编号'), field('recordCategory', '记录类型'), field('operationType', '操作类型'), field('resultStatus', '结果')], read: page('/mes/pro/edhr-operation-audit/page'), detail: row => request.get({ url: `/mes/pro/edhr-operation-audit/${encodeURIComponent(row.id)}` }) },
  { id: 'form-fill', label: '表单填写与报工修改', permissions: ['mes:pro-edhr-form-fill-log:query'], component: defineAsyncComponent(() => import('@/views/mes/pro/edhr/FormFillLogPage.vue')) },
  { id: 'login', label: '登录', permissions: ['system:login-log:query'], component: defineAsyncComponent(() => import('@/views/system/loginlog/index.vue')) },
  { id: 'sms', label: '短信发送', permissions: ['system:sms-log:query'], component: defineAsyncComponent(() => import('@/views/system/sms/log/index.vue')) },
  { id: 'mail', label: '邮件发送', permissions: ['system:mail-log:query'], component: defineAsyncComponent(() => import('@/views/system/mail/log/index.vue')) },
  { id: 'api-access', label: '接口访问', permissions: ['infra:api-access-log:query'], component: defineAsyncComponent(() => import('@/views/infra/apiAccessLog/index.vue')) },
  { id: 'api-error', label: '接口异常', permissions: ['infra:api-error-log:query'], component: defineAsyncComponent(() => import('@/views/infra/apiErrorLog/index.vue')) },
  { id: 'job', label: '任务调度', permissions: ['infra:job:query'], component: defineAsyncComponent(() => import('@/views/infra/job/logger/index.vue')) },
  { id: 'payment', label: '支付回调', permissions: ['pay:notify:query'], component: defineAsyncComponent(() => import('@/views/pay/notify/index.vue')) },
  { id: 'notify', label: '站内信发送', permissions: ['system:notify-message:query'], component: defineAsyncComponent(() => import('@/views/system/notify/message/index.vue')) },
  { id: 'nas-table-sync', label: 'NAS表格自动同步执行', permissions: ['mes:pro-batch-record-execution:golden-finger'], read: page('/erp/nas-table-sync/run/page') },
  { id: 'workrecord', label: '员工上下工记录', permissions: ['mes:pro-workrecord:query'], component: defineAsyncComponent(() => import('@/views/mes/pro/workrecord/WorkRecordLogList.vue')) },
  { id: 'release-event', label: 'eDHR 放行事务事件', permissions: ['mes:pro-edhr-release:query'], fields: [field('releaseTransactionId', '放行事务编号', true), field('eventType', '事件类型')], read: page('/mes/pro/edhr-release/event/page') },
  { id: 'form-event', label: 'eDHR 表单事件', permissions: ['mes:pro-edhr-form-instance:query'], fields: [field('instanceId', '表单实例编号'), field('templateId', '表单模板编号'), field('eventType', '事件类型'), field('resultStatus', '结果')], read: page('/mes/pro/edhr-form-instance/event/page') },
  { id: 'traveler-event', label: 'eDHR 流转单事件', permissions: ['mes:pro-edhr-traveler:query'], fields: [field('travelerId', '流转单编号'), field('travelerCode', '流转单编码'), field('eventType', '事件类型'), field('resultStatus', '结果')], read: page('/mes/pro/edhr-traveler/event/page') },
  { id: 'change-event', label: 'eDHR 统一变更事件', permissions: ['mes:pro-edhr-change:event-query'], fields: [field('changeRequestId', '变更申请编号', true), field('eventType', '事件类型')], read: page('/mes/pro/edhr-change/unified/event/page') },
  { id: 'runtime-operation', label: '运维操作执行日志', permissions: ['infra:runtime-control:query'], fields: [field('operationId', '操作编号', true)], read: async params => {
    const result = await request.get({ url: `/infra/runtime-control/operations/${encodeURIComponent(params.operationId)}/log`, params: { maxBytes: 65536 } })
    if (!result || typeof result !== 'object') throw new Error('运维日志响应无效')
    return { list: [result], total: 1 }
  } },
  { id: 'runtime-history', label: '运维操作历史', permissions: ['infra:runtime-control:query'], read: list('/infra/runtime-control/operations'), detail: async row => ({ operation: row, executionLog: await request.get({ url: `/infra/runtime-control/operations/${encodeURIComponent(row.operationId)}/log`, params: { maxBytes: 65536 } }) }) },
  { id: 'workflow', label: '本人失败、取消与拦截（最近100条）', permissions: [], read: list('/mes/pro/workflow-interaction-log/own-recent') },
  ...[
    ['clue', '线索', 1], ['customer', '客户', 2], ['contact', '联系人', 3], ['business', '商机', 4],
    ['contract', '合同', 5], ['product', '产品', 6], ['receivable', '回款', 7], ['receivable-plan', '回款计划', 8]
  ].map(([code, label, bizType]): LogSource => ({
    id: `crm-${code}`, label: `CRM ${label}操作`, permissions: [`crm:${code}:query`],
    fields: [field('bizId', '业务编号', true)],
    read: async params => {
      const result = await page('/crm/operate-log/page')({ ...params, bizType })
      const start = (params.pageNo - 1) * params.pageSize
      return { list: result.list.slice(start, start + params.pageSize), total: result.total }
    }
  })),
  { id: 'schedule', label: '排产工单操作', permissions: ['mes:pro-schedule-order:query'], fields: [field('scheduleOrderId', '排产工单编号', true)], read: list('/mes/pro/schedule-order/operation-log') },
  { id: 'trade-order', label: '商城订单操作', permissions: ['trade:order:query'], fields: [field('id', '订单编号', true)], read: list('/trade/order/get-detail', 'logs') },
  { id: 'after-sale', label: '商城售后操作', permissions: ['trade:after-sale:query'], fields: [field('id', '售后编号', true)], read: list('/trade/after-sale/get-detail', 'logs') },
  { id: 'permission', label: '系统权限变更', permissions: ['system:gxp-audit:query'], fields: [
    { key: 'operationId', label: '权限操作', required: true, options: [
      { label: '角色菜单授权', value: 'system.permission.role-menu.assign' },
      { label: '用户角色授权', value: 'system.permission.user-role.assign' },
      { label: '角色数据权限', value: 'system.permission.role-data-scope.assign' }
    ] }, field('subjectId', '对象编号')
  ], read: page('/system/gxp-audit-event/page'), detail: row => request.get({ url: '/system/gxp-audit-event/get', params: { id: row.id } }) },
  { id: 'temporary-role', label: '临时角色授权', permissions: ['system:temporary-role-grant:query'], fields: [field('grantId', '授权编号', true)], read: list('/system/temporary-role-grant/audit-list') },
  { id: 'file-time', label: '文件上传时间修改', permissions: ['intern-user:time-maintenance:file-upload-time-audit:query'], fields: [field('fileId', '文件编号', true)], read: list('/intern-user/time-maintenance/file/upload-time/audits') },
  { id: 'dcc-published-time', label: '文控升版时间修改', permissions: ['intern-user:time-maintenance:dcc-published-time-audit:query'], fields: [field('controlledFileId', '受控文件编号', true)], read: list('/intern-user/time-maintenance/dcc/published-time/audits') },
  { id: 'dcc-obsoleted-time', label: '文控作废时间修改', permissions: ['intern-user:time-maintenance:dcc-obsoleted-time-audit:query'], fields: [field('controlledFileId', '受控文件编号', true)], read: list('/intern-user/time-maintenance/dcc/obsoleted-time/audits') },
  { id: 'signature-authorization', label: '电子签名授权', permissions: ['dcc:controlled-file:signature:manage'], fields: [field('userId', '被授权用户编号', true)], read: params => page(`/dcc/electronic-signature-authorizations/${encodeURIComponent(params.userId)}/audits/page`)({ pageNo: params.pageNo, pageSize: params.pageSize }) },
  { id: 'flow', label: 'eDHR 流程事件', permissions: ['mes:pro-edhr-flow-intervention:event-query'], fields: [field('businessObjectType', '业务对象类型', true), field('businessObjectId', '业务对象编号', true), field('flowInstanceId', '流程实例编号'), field('eventType', '事件类型')], read: page('/mes/pro/edhr-flow-intervention/event/page') },
  { id: 'report-export', label: 'eDHR 报表导出', permissions: ['mes:pro-edhr-report:query'], fields: [field('reportCode', '报表编码'), field('reportName', '报表名称'), field('operationType', '操作类型'), field('resultStatus', '结果'), field('operatorUsername', '操作账号')], read: page('/mes/pro/edhr-report-query/export-audit/page') },
  { id: 'active-order', label: '活跃订单操作记录', permissions: ['mes:pro-process-pool-team-leader:query'], fields: [field('activeOrderId', '活跃订单编号', true)], read: list('/mes/pro/process-pool/team-leader/active-order/detail', 'operationFacts') },
  { id: 'batch-order', label: '批次订单操作记录', permissions: ['mes:pro-edhr-batch-execution:query'], fields: [field('batchExecutionId', '批次执行编号', true)], read: list('/mes/pro/edhr-batch-execution/active-order-detail', 'operationFacts') },
  { id: 'pqc-order', label: 'PQC放行订单操作记录', permissions: ['mes:pro-production-release:query'], fields: [field('applicationId', '放行申请编号', true)], read: async params => {
    const result = await request.get({ url: '/mes/pro/production-release/pqc/order-detail', params })
    if (!Array.isArray(result?.detail?.operationFacts)) throw new Error('PQC订单操作记录响应无效')
    const rows = result.detail.operationFacts
    const start = (params.pageNo - 1) * params.pageSize
    return { list: rows.slice(start, start + params.pageSize), total: rows.length }
  } },
  { id: 'submission', label: '生产/PQC 提交与复核', permissions: ['mes:pro-process-pool-team-leader:query'], fields: [field('id', '提交事件编号', true), { key: 'leaderType', label: '组长类型', required: true, options: [{ label: '生产组长', value: 'PRODUCTION' }, { label: 'PQC组长', value: 'PQC' }] }], read: async params => {
    const result = await request.get({ url: '/mes/pro/process-pool/team-leader/submission/detail', params })
    if (!result || typeof result !== 'object') throw new Error('提交事件不存在或不可见')
    return { list: [result], total: 1 }
  } },
  gxp('team-gxp', '生产组长订单审计', 'mes:pro-process-pool-team-leader:query', '/mes/pro/process-pool/team-leader/active-order/audit', 'activeOrderId'),
  gxp('pqc-gxp', 'PQC生产放行审计', 'mes:pro-production-release:query', '/mes/pro/production-release/pqc/audit', 'applicationId'),
  gxp('batch-gxp', '批次执行审计', 'mes:pro-edhr-batch-execution:query', '/mes/pro/edhr-batch-execution/audit', 'batchExecutionId'),
  gxp('batch-order-gxp', '批次活跃订单审计', 'mes:pro-edhr-batch-execution:query', '/mes/pro/edhr-batch-execution/audit', 'activeOrderId')
]
