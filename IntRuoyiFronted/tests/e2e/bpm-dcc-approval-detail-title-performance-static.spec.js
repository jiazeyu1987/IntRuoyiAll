const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const detailSource = readSource('src/views/bpm/processInstance/detail/index.vue')
const displayNameSource = readSource('src/views/bpm/processInstance/detail/display-name.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:bpm-approval-detail-title-performance:static'],
  'node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js',
  'package.json must expose the DCC BPM approval detail title/performance contract'
)

assert.match(
  displayNameSource,
  /DCC Controlled File Approval[\s\S]*文控受控文件审批/,
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

assert.doesNotMatch(
  detailSource,
  /<el-tabs[\s\S]*v-model="activeTab"/,
  'BPM detail must not render extra tabs when only approval detail is required'
)
assert.doesNotMatch(
  detailSource,
  /<el-tab-pane[^>]*label="流程图"[^>]*name="diagram"/,
  'BPM detail must remove the process diagram tab'
)
assert.doesNotMatch(
  detailSource,
  /<el-tab-pane[^>]*label="流转记录"[^>]*name="record"/,
  'BPM detail must remove the transfer record tab'
)
assert.doesNotMatch(
  detailSource,
  /ProcessInstance(?:Simple|Bpmn)Viewer|ProcessInstanceTaskList/,
  'BPM detail must not import or render diagram/task-list components after removing those tabs'
)
assert.doesNotMatch(
  detailSource,
  /processModelView|processModelViewLoaded|processModelViewLoading|getProcessModelView|ensureProcessModelViewLoaded|getProcessInstanceBpmnModelView/,
  'BPM detail must remove process diagram state and API calls from this first-screen page'
)
assert.doesNotMatch(
  detailSource,
  /const activeTab\s*=\s*ref|watch\(\s*activeTab/,
  'BPM detail must not keep tab state or tab watchers after deleting extra tabs'
)

const getDetailStart = detailSource.indexOf('const getDetail = () => {')
assert.notEqual(getDetailStart, -1, 'BPM detail must keep getDetail function')
const getDetailEnd = detailSource.indexOf('/** 加载流程实例 */', getDetailStart)
assert.ok(getDetailEnd > getDetailStart, 'BPM detail getDetail block must be readable')
const getDetailBlock = detailSource.slice(getDetailStart, getDetailEnd)
assert.doesNotMatch(
  getDetailBlock,
  /activeTab|processModelView|getProcessModelView\(\)/,
  'BPM detail initial load must not prepare or request the removed process diagram'
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
