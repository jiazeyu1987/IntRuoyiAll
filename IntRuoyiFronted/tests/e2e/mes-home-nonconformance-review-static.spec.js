const assert = require('node:assert')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const homeIndex = readFileSync(resolve(__dirname, '../../src/views/mes/home/index.vue'), 'utf8')
const homeAlertPanel = readFileSync(
  resolve(__dirname, '../../src/views/mes/home/HomeAlertPanel.vue'),
  'utf8'
)

assert.match(homeIndex, /nonconformanceReviewPendingCount: 0/)
assert.match(homeIndex, /MesHomeStatisticsApi\.getHomeSummary\(\)/)
assert.match(homeAlertPanel, /label: '不合格评审'/)
assert.match(homeAlertPanel, /routeName: 'MesProFeedbackEdhrNonconformanceReview'/)
assert.match(homeAlertPanel, /count: props\.summary\.nonconformanceReviewPendingCount/)
assert.match(homeAlertPanel, /待处理的冻结批次/)

console.log('PASS: MES home shows nonconformance review pending badge entry')
