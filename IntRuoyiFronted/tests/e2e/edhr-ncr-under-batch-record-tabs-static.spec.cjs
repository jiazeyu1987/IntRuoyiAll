const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const tabs = read('IntRuoyiFronted/src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue')
const ncrPage = read('IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')
const routes = read('IntRuoyiFronted/src/router/modules/remaining.ts')

assert.match(
  tabs,
  /<el-tab-pane\s+label="不合格评审"\s+name="nonconformanceReview"\s*\/>/,
  'eDHR 批记录共享页签必须包含“不合格评审”。'
)

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

assert.match(
  ncrPage,
  /import EdhrBatchRecordTabs from '..\/edhr-batch\/EdhrBatchRecordTabs.vue'/,
  '不合格评审页面必须复用 eDHR 批记录页签组件。'
)

assert.match(
  ncrPage,
  /<EdhrBatchRecordTabs active-tab="nonconformanceReview" \/>/,
  '不合格评审页面必须把当前页签标记为 nonconformanceReview。'
)

assert.match(
  routes,
  /path:\s*'pro\/feedback\/edhr-nonconformance-review'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/edhr-nonconformance\/NonconformanceReviewPage.vue'\)/,
  '既有不合格评审路由必须继续指向原正式页面，不能改成占位或跳转。'
)

console.log('PASS edhr-ncr-under-batch-record-tabs-static')
