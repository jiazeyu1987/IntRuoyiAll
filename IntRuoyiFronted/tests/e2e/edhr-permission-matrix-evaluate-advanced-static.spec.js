const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/PermissionMatrixPage.vue'
)

const source = fs.readFileSync(pagePath, 'utf8')

const assertIncludes = (token, message) => {
  assert.ok(source.includes(token), message)
}

const evaluateToolbarStart = source.indexOf('edhr-permission-matrix__evaluate-toolbar')
assert.ok(evaluateToolbarStart > 0, '权限矩阵页必须保留评估条件区域。')

const evaluateToolbarEnd = source.indexOf('edhr-permission-matrix__result', evaluateToolbarStart)
assert.ok(evaluateToolbarEnd > evaluateToolbarStart, '权限矩阵页评估条件区域必须位于结果区域之前。')

const evaluateToolbarSource = source.slice(evaluateToolbarStart, evaluateToolbarEnd)
const advancedStart = evaluateToolbarSource.indexOf('edhr-permission-matrix__advanced-evaluate')
assert.ok(advancedStart > 0, '权限矩阵页必须提供默认收起的高级评估条件。')

const defaultEvaluateSource = evaluateToolbarSource.slice(0, advancedStart)
const advancedEvaluateSource = evaluateToolbarSource.slice(advancedStart)

for (const token of ['label="执行ID"', 'label="批次ID"', '评估能力']) {
  assert.ok(defaultEvaluateSource.includes(token), `评估条件默认区必须保留主路径字段：${token}`)
}

for (const token of ['label="路线ID"', 'label="工序ID"', 'label="记录表ID"', 'label="记录类型"']) {
  assert.ok(!defaultEvaluateSource.includes(token), `低频上下文字段不得继续占用评估条件默认区：${token}`)
  assert.ok(advancedEvaluateSource.includes(token), `高级评估条件必须保留字段：${token}`)
}

for (const token of [
  'permissionAdvancedEvaluateNames',
  '<el-collapse',
  '<el-collapse-item title="高级评估条件" name="advanced-evaluate"',
  'edhr-permission-matrix__advanced-evaluate-grid'
]) {
  assertIncludes(token, `高级评估条件必须默认收起且有稳定布局：${token}`)
}

assertIncludes('routeId: queryParams.routeId || undefined', '路线ID必须继续进入 evaluate 请求。')
assertIncludes('routeProcessId: queryParams.routeProcessId || undefined', '工序ID必须继续进入 evaluate 请求。')
assertIncludes('reportId: queryParams.reportId.trim() || undefined', '记录表ID必须继续进入 evaluate 请求。')
assertIncludes('recordCategory: queryParams.recordCategory', '记录类型必须继续进入 evaluate 请求。')

console.log('PASS: EDHR permission matrix evaluate advanced static contract')
