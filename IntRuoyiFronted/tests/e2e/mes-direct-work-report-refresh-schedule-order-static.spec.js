const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const feedbackPagePath = path.resolve(frontendRoot, 'src/views/mes/pro/feedback/index.vue')
const scheduleOrderPagePath = path.resolve(frontendRoot, 'src/views/mes/pro/scheduleorder/index.vue')
const importFormPath = path.resolve(
  frontendRoot,
  'src/views/mes/pro/feedback/ThirdPartyFeedbackImportForm.vue'
)

for (const filePath of [feedbackPagePath, scheduleOrderPagePath, importFormPath]) {
  assert(fs.existsSync(filePath), `直接报工刷新相关文件必须存在：${filePath}`)
}

const feedbackPageSource = fs.readFileSync(feedbackPagePath, 'utf8')
const scheduleOrderPageSource = fs.readFileSync(scheduleOrderPagePath, 'utf8')
const importFormSource = fs.readFileSync(importFormPath, 'utf8')

assert(
  importFormSource.includes("emits('success', result, importMode.value === 'DIRECT_WORK_REPORT'"),
  '直接报工导入弹窗必须在导入完成后把原始导入结果透传给父页面。'
)

assert(
  feedbackPageSource.includes('interface MesScheduleOrderRefreshPayload')
    && feedbackPageSource.includes('scheduleOrderCodes: string[]')
    && feedbackPageSource.includes('workOrderCodes: string[]')
    && feedbackPageSource.includes('buildDirectWorkReportScheduleOrderRefreshPayload')
    && feedbackPageSource.includes('directWorkReportDetails || []')
    && feedbackPageSource.includes('detail.scheduleOrderCode')
    && feedbackPageSource.includes('detail.workOrderCode')
    && !/buildDirectWorkReportScheduleOrderRefreshPayload[\s\S]*directWorkReportSkipWarnings/.test(
      feedbackPageSource
    ),
  '报工页必须只从已成功更新进度的 directWorkReportDetails 构造受影响排产工单刷新 payload，不能把未命中排产的 warning 当成刷新目标。'
)

assert(
  /if \(sourceLabel === '李萍报工单'\) \{[\s\S]*emitDirectWorkReportScheduleOrderRefresh\(result\)[\s\S]*await getList\(\)[\s\S]*return/.test(
    feedbackPageSource
  ),
  '直接报工导入成功后必须在返回前广播受影响排产工单刷新事件。'
)

assert(
  feedbackPageSource.includes('emitScheduleOrderRefresh(payload)')
    && feedbackPageSource.includes('emitter.emit(MES_SCHEDULE_ORDER_REFRESH_EVENT, payload)')
    && feedbackPageSource.includes("source: 'DIRECT_WORK_REPORT'"),
  '排产工单刷新事件必须携带 DIRECT_WORK_REPORT 来源和受影响工单 payload。'
)

assert(
  scheduleOrderPageSource.includes('interface MesScheduleOrderRefreshPayload')
    && scheduleOrderPageSource.includes('shouldRefreshScheduleOrderList')
    && scheduleOrderPageSource.includes('payload?.scheduleOrderCodes')
    && scheduleOrderPageSource.includes('payload?.workOrderCodes')
    && scheduleOrderPageSource.includes('row.code')
    && scheduleOrderPageSource.includes('row.erpWorkOrderCode')
    && scheduleOrderPageSource.includes('const handleScheduleOrderRefresh = async')
    && scheduleOrderPageSource.includes('callback: handleScheduleOrderRefresh'),
  '排产工单页必须监听刷新 payload，并只对当前列表中的受影响排产工单/来源生产工单重新拉取真实进度。'
)

console.log('PASS: MES direct work report refresh affected schedule orders static contract')
