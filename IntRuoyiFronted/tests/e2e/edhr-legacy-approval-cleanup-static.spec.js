const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const templatePage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const templateRules = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const remainingRoutes = read('src/router/modules/remaining.ts')

assert.ok(
  !remainingRoutes.includes('edhr-recordbook') && !remainingRoutes.includes('eDHR记录本'),
  'standalone eDHR recordbook route/menu must be removed; recordbook fill stays inside batch execution'
)

for (const [name, source] of [
  ['batch detail', batchDetail],
  ['execution page', executionPage]
]) {
  assert.ok(!source.includes('审批时间'), `${name} must not label ordinary process completion as approval time`)
  assert.ok(!source.includes('复核签名或提交执行'), `${name} must not require form review before ordinary submit`)
  assert.ok(!source.includes('确认字段、附件和复核签名后提交执行'), `${name} submit hint must not require form review`)
}

assert.ok(
  batchDetail.includes("[EDHR_BATCH_TASK_STATUS_APPROVED]: '填写完成'"),
  'batch task complete status must be labeled 填写完成 for ordinary process cards'
)
assert.ok(
  !batchDetail.includes('<div class="edhr-batch-detail__rail-label">完成时间</div>'),
  'batch detail side rail must hide completion time'
)

assert.ok(
  !templatePage.includes("{ label: '复核签名', value: 'FORM_REVIEW' }"),
  'ordinary template signature selector must not expose FORM_REVIEW'
)
assert.ok(
  !templatePage.includes("{ label: '审批签名', value: 'APPROVE' }"),
  'ordinary template signature selector must not expose APPROVE'
)
assert.ok(!templatePage.includes('审批签名位必须先选择'), 'ordinary template marker validation must not require approval source')
assert.ok(!templateRules.includes("if (marker?.actionType === 'FORM_REVIEW') return '复核签名'"))
assert.ok(!templateRules.includes("if (marker?.actionType === 'APPROVE') return '审批签名'"))

console.log('PASS: eDHR legacy approval cleanup static contract')
