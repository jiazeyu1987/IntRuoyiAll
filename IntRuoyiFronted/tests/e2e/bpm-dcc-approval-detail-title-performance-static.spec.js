const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const detailSource = readSource('src/views/bpm/processInstance/detail/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:bpm-approval-detail-title-performance:static'],
  'node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js',
  'package.json must expose the DCC BPM approval detail title/performance contract'
)

assert.match(
  detailSource,
  /DCC_APPROVAL_PROCESS_TITLE_LABELS:\s*Record<string,\s*string>\s*=\s*\{[\s\S]*'DCC Controlled File Approval':\s*'文控受控文件审批'/,
  'BPM detail must map DCC Controlled File Approval to its Chinese title'
)
assert.match(
  detailSource,
  /const processInstanceDisplayName\s*=\s*computed\(/,
  'BPM detail must compute a display title instead of rendering the raw process instance name'
)
assert.match(
  detailSource,
  /\{\{\s*processInstanceDisplayName\s*\}\}/,
  'BPM detail header must render the Chinese-aware display title'
)
assert.doesNotMatch(
  detailSource,
  /text-26px font-bold mb-5px">\s*\{\{\s*processInstance\.name\s*\}\}/,
  'BPM detail header must not directly render processInstance.name'
)

assert.match(
  detailSource,
  /const processModelViewLoaded\s*=\s*ref\(false\)/,
  'BPM detail must track whether the process diagram has been loaded'
)
assert.match(
  detailSource,
  /const ensureProcessModelViewLoaded\s*=\s*async\s*\(\s*\)\s*=>/,
  'BPM detail must load the process diagram through a lazy ensure function'
)
assert.match(
  detailSource,
  /watch\(\s*activeTab[\s\S]*activeName\s*===\s*'diagram'[\s\S]*ensureProcessModelViewLoaded\(\)/,
  'BPM detail must load the diagram only after the diagram tab is opened'
)

const getDetailStart = detailSource.indexOf('const getDetail = () => {')
assert.notEqual(getDetailStart, -1, 'BPM detail must keep getDetail function')
const getDetailEnd = detailSource.indexOf('/** 加载流程实例 */', getDetailStart)
assert.ok(getDetailEnd > getDetailStart, 'BPM detail getDetail block must be readable')
const getDetailBlock = detailSource.slice(getDetailStart, getDetailEnd)
assert.doesNotMatch(
  getDetailBlock,
  /getProcessModelView\(\)/,
  'BPM detail initial load must not eagerly request the process diagram'
)

assert.doesNotMatch(
  detailSource,
  /await loadDccApprovalFileSummary\(\)/,
  'DCC approval summary must load independently and not block the initial BPM detail render'
)
assert.match(
  detailSource,
  /void loadDccApprovalFileSummary\(\)/,
  'DCC approval summary must be launched asynchronously after approval detail data is available'
)

assert.match(
  detailSource,
  /<BusinessFormComponent v-else :id="processInstance\.businessKey" \/>/,
  'Non-DCC custom forms must still mount their configured business form component'
)
assert.doesNotMatch(
  detailSource,
  /mock|fallback|降级|吞异常/i,
  'Title/performance fix must not add mock data, fallback behavior, downgrade, or swallowed errors'
)

console.log('PASS: BPM DCC approval detail title and performance static contract')
