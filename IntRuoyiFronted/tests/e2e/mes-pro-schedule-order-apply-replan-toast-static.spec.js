const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const apiPath = path.join(root, 'src/api/mes/pro/task/autoSchedule/index.ts')
const source = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

const applyStart = source.indexOf('const applyReplan = async () => {')
const applyEnd = source.indexOf('const openDailyCompareDialog', applyStart)
assert.ok(applyStart >= 0 && applyEnd > applyStart, '应用重排处理函数必须存在')

const applySource = source.slice(applyStart, applyEnd)

assert.ok(
  !apiSource.includes("import type { FormInstanceVO } from '@/api/form-center/instance'"),
  '手动重排不走审批，API 不应依赖表单中心实例响应类型'
)
assert.ok(
  /export interface ProTaskAutoScheduleReplanApplyReqVO[\s\S]*idempotencyKey:\s*string/.test(apiSource),
  '手动重排应用请求必须携带幂等键'
)
assert.ok(
  !/export interface ProTaskAutoScheduleReplanApplyReqVO[\s\S]*startUserSelectAssignees\?:\s*Record<string,\s*number\[\]>/.test(
    apiSource
  ),
  '手动重排不走审批，请求不得继续携带发起人选择审批人字段'
)
assert.ok(
  /replanApply:\s*async\s*\(data:\s*ProTaskAutoScheduleReplanApplyReqVO\):\s*Promise<ProTaskAutoScheduleApplyRespVO>/.test(
    apiSource
  ),
  '手动重排应用 API 必须返回直接应用结果'
)
assert.ok(
  /replanApply[\s\S]*request\.post<ProTaskAutoScheduleApplyRespVO>/.test(apiSource),
  '手动重排应用 API 必须按直接重排应用结果解析响应'
)

assert.ok(
  applySource.includes('const applyResult = await ProTaskAutoScheduleApi.replanApply'),
  '手动重排必须直接接收应用结果'
)
assert.ok(
  applySource.includes('idempotencyKey: buildReplanApplyIdempotencyKey(applyRequest)'),
  '手动重排应用必须按本次范围生成幂等键'
)
assert.ok(
  !applySource.includes('startUserSelectAssignees'),
  '手动重排不走审批，不得提交审批人映射'
)
assert.ok(
  applySource.includes('buildReplanApplySuccessMessage(applyResult)'),
  '手动重排成功提示必须描述重排已应用'
)
assert.ok(
  source.includes('应用重排成功') && source.includes('正式排程已更新'),
  '手动重排成功文案必须说明重排已直接生效'
)
assert.ok(
  source.includes('不能应用重排') && source.includes('应用重排失败'),
  '手动重排阻断和失败文案必须使用直接应用语义'
)
assert.ok(
  source.includes('createdTaskIds') &&
    source.includes('deletedTaskIds') &&
    source.includes('preservedTaskIds'),
  '手动重排直接应用后必须展示直接应用统计'
)

console.log('PASS: MES schedule replan direct apply frontend contract')
