const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const source = fs.readFileSync(path.join(repoRoot, 'src/views/approval-center/index.vue'), 'utf8')

assert.match(
  source,
  /<h3 class="approval-center__title">审批中心<\/h3>[\s\S]*?<UnifiedListTemplate/,
  'approval center must render the standard list directly below the title toolbar'
)

assert.doesNotMatch(
  source,
  /<el-tabs\b|<el-tab-pane\b|approval-center__tabs/,
  'approval center must not render the four workflow tabs below the title'
)

assert.doesNotMatch(
  source,
  /label="待办"|label="已办"|label="我发起的"|label="抄送我的"/,
  'approval center tab labels must not appear in the page template'
)

assert.doesNotMatch(
  source,
  /handleTabChange|const\s+activeTab\s*=/,
  'approval center must not keep unused tab state or tab change handlers after hiding tabs'
)

console.log('PASS: approval center hidden tab static contract')
