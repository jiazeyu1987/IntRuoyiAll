const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const apiPath = 'IntRuoyiFronted/src/api/mes/pro/processpool/index.ts'
const pagePath = 'IntRuoyiFronted/src/views/mes/pro/processpool/TimelinePage.vue'

assert(fs.existsSync(path.join(repoRoot, apiPath)), '工序池时间轴前端 API 模块必须存在。')
assert(fs.existsSync(path.join(repoRoot, pagePath)), '工序池时间轴只读页面必须存在。')

const apiSource = read(apiPath)
const pageSource = read(pagePath)

assert(apiSource.includes('/mes/pro/process-pool/timeline/page'), 'API 必须调用工序池时间轴分页查询接口。')
assert(apiSource.includes('/mes/pro/process-pool/timeline/detail'), 'API 必须调用工序池时间轴详情查询接口。')
assert(!/request\.(post|put|delete|upload|download)\(/.test(apiSource), '工序池时间轴 API 模块不得暴露 MES 写请求。')

for (const field of [
  'submitDate',
  'employeeUserId',
  'processId',
  'deviceId',
  'templateType',
  'workOrderId',
  'workOrderCode'
]) {
  assert(apiSource.includes(field), `时间轴过滤契约必须包含 ${field}。`)
}

for (const label of [
  '提交日期',
  '员工',
  '工序',
  '设备',
  '模板类型',
  '生产工单',
  '登录账号',
  '实际填写员工',
  '电子签名员工',
  '提交摘要'
]) {
  assert(pageSource.includes(label), `页面必须展示或过滤 ${label}。`)
}

for (const traceLabel of [
  '原始 payload',
  'PQC',
  'FIFO 分配状态',
  '审核副本状态',
  '修改历史摘要'
]) {
  assert(pageSource.includes(traceLabel), `详情抽屉必须只读展示 ${traceLabel}。`)
}

assert(pageSource.includes('getProcessPoolTimelinePage'), '页面必须通过正式 API 查询时间轴。')
assert(pageSource.includes('getProcessPoolTimelineDetail'), '页面必须通过正式 API 查询事件详情。')
assert(pageSource.includes('readonlyActions'), '详情必须显式呈现只读动作边界。')
assert(!/@click="[^"]*(create|update|submit|allocate|generate|fifo|auditCopy)/i.test(pageSource), '页面不得提供修改、审核副本生成或 FIFO 写操作。')
assert(!/ProFeedbackApi|getFeedbackPage|feedback_surplus_pool|surplusPool/i.test(pageSource), '时间轴页面不得把生产报工列表或余量池当作工序池数据源。')

console.log('PASS process-pool-timeline-frontend-static')
