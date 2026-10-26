const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const tabs = read('IntRuoyiFronted/src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue')
const ncrPage = read('IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')
const routes = read('IntRuoyiFronted/src/router/modules/remaining.ts')

for (const page of [
  'edhr-batch/BatchExecutionListPage.vue',
  'edhr-batch/BatchVoidedPage.vue',
  'edhr-nonconformance/NonconformanceReviewPage.vue',
  'edhr-deviation/index.vue'
]) {
  assert.doesNotMatch(read(`IntRuoyiFronted/src/views/mes/pro/${page}`), /EdhrBatchRecordTabs/, '独立菜单页面不再显示共享导航 Tab')
}

assert.match(
  tabs,
  /nonconformanceReview/,
  'eDHR 批记录页签类型必须登记 nonconformanceReview。'
)

assert.match(
  tabs,
  /nonconformanceReview:\s*'\/mes\/pro\/feedback\/edhr-nonconformance-review'/,
  '不合格评审页签必须导航到既有不合格评审正式页面。'
)

assert.doesNotMatch(ncrPage, /EdhrBatchRecordTabs/)

assert.match(
  routes,
  /path:\s*'pro\/feedback\/edhr-nonconformance-review'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/edhr-nonconformance\/NonconformanceReviewPage.vue'\)/,
  '既有不合格评审路由必须继续指向原正式页面，不能改成占位或跳转。'
)

console.log('PASS edhr-ncr-under-batch-record-tabs-static')
