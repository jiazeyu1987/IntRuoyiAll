const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..')
const panelPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'processpool',
  'components',
  'ActiveOrderSubmissionDetailPanel.vue'
)
const pagePath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'processpool',
  'ActiveOrderSubmissionDetailPage.vue'
)

const panel = fs.readFileSync(panelPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

assert.match(panel, /<el-tab-pane\s+[\s\S]*label="总表"[\s\S]*name="summary"/, '详情面板必须声明“总表”主 tab。')
assert.match(panel, /data-active-order-summary-tab/, '“总表”主 tab 必须保留稳定选择器。')
assert.match(
  panel,
  /const\s+showSummaryTab\s*=\s*computed\(\s*\(\)\s*=>\s*!embedded\.value\s*\)/,
  '独立详情页必须显示总表，不能按 production/pqc displayMode 隐藏；仅嵌入模式可隐藏总表。'
)
assert.match(
  panel,
  /activeTab\.value\s*=\s*showSummaryTab\.value\s*\?\s*'summary'\s*:/,
  '重置主 tab 时必须优先选择可见的总表，避免 v-model 指向不可见 tab。'
)
assert.match(
  panel,
  /resolveLatestProductionSubmitterSignature\(\s*process\.submissions\s*\)/,
  '总表工序人员必须按每个工序最后一次生产提交电子签名解析。'
)
assert.match(
  panel,
  /operatorText:\s*formatSummarySignatureOperator\(latestSubmitterSignature\)/,
  '总表操作人员必须显示最后一次提交电子签名人，不得聚合普通提交人。'
)
assert.match(
  panel,
  /assemblyDateText:\s*formatSummarySignatureDate\(latestSubmitterSignature\)/,
  '总表装配日期必须显示最后一次提交电子签名日期，不得使用普通提交时间。'
)
assert.doesNotMatch(
  panel,
  /operatorTexts\s*=\s*Array\.from\([\s\S]*?submission\.submitterName/,
  '总表不得继续聚合 submission.submitterName 作为操作人员。'
)
assert.doesNotMatch(
  panel,
  /assemblyDateTexts\s*=\s*Array\.from\([\s\S]*?submission\.submittedAt/,
  '总表不得继续聚合 submittedAt 作为装配日期。'
)
assert.doesNotMatch(
  page,
  /:display-mode|displayMode=/,
  '独立详情页面不得向详情面板传入 production/pqc displayMode。'
)
assert.doesNotMatch(
  panel,
  /\n}\r?\n\s{2}overflow-x:\s*auto;\r?\n}\r?\n\r?\n\.team-leader-workbench__active-order-detail-tabs\s*:deep/,
  '详情面板样式中不得保留孤立的 overflow-x 片段，避免样式解析异常。'
)

console.log('PASS: active order summary tab visible static contract')
