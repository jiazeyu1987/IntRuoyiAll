const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const categoryPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const reviewMatrixTable = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue'
)

assert.ok(categoryPage.includes('label="审阅矩阵"'), '类别页必须将 DCC审阅矩阵 改名为 审阅矩阵')
assert.ok(
  !categoryPage.includes('label="DCC审阅矩阵"'),
  '类别页不得继续保留 DCC审阅矩阵 旧页签文案'
)

for (const removedLabel of [
  'label="可查阅主体"',
  'label="待审预览主体"',
  'label="下载规则"',
  'label="启用状态"',
  'label="当前状态/风险"',
  'label="当前版本"',
  'label="生效时间"',
  'label="备注"'
]) {
  assert.ok(!reviewMatrixTable.includes(removedLabel), `审阅矩阵总览表不得显示 ${removedLabel}`)
}

for (const removedToken of [
  'formatViewSubjects(',
  'formatPendingPreviewRuleSummary(',
  'formatDownloadSubjects(',
  'formatViewRuleSummary(',
  'formatDownloadRuleSummary(',
  'DEFAULT_VIEW_RULE_SUMMARY',
  'DEFAULT_PENDING_PREVIEW_RULE_SUMMARY',
  'DEFAULT_DOWNLOAD_RULE_SUMMARY',
  'formatRiskSummary(',
  'matrix-risk-list',
  'routeVersionNo',
  'effectiveTime ? formatDate(',
  'prop="remark"',
  'ACTIVE_STATUS_OPTIONS',
  'formatBooleanLabel',
  'getBooleanTagType',
  'configured: true',
  'v-model="queryParams.configured"',
  'v-model="queryParams.active"'
]) {
  assert.ok(!reviewMatrixTable.includes(removedToken), `审阅矩阵隐藏三列后不得继续保留旧展示逻辑：${removedToken}`)
}

console.log('PASS: DCC review matrix hide columns static contract')
