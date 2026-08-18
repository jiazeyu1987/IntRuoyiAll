const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/qc/template/index.ts'),
  'utf8'
)

assert.match(
  apiSource,
  /resetQaRegulationForTesting:\s*async\s*\(\s*dccProjectCodeId: number\s*\)[\s\S]*\/mes\/qa\/inspection-regulation\/test-reset/,
  'QA regulation API must expose the administrator test reset endpoint.'
)

const headerStart = pageSource.indexOf('<div class="qa-regulation-page__version-publish"')
const headerEnd = pageSource.indexOf('</div>', headerStart)
assert.ok(headerStart >= 0 && headerEnd > headerStart, 'QA header operation area must exist.')
const operationHeader = pageSource.slice(headerStart, headerEnd + 6)

assert.match(
  operationHeader,
  /type="danger"[\s\S]*data-qa-regulation-test-reset[\s\S]*测试重置/,
  'QA page must render a visible danger-style test reset button in the top operation area.'
)
assert.match(
  operationHeader,
  /data-qa-regulation-test-reset[\s\S]*v-hasPermi="\['mes:qc-template:update'\]"/,
  'QA test reset button must be visible only to users with QA template update permission.'
)
assert.match(
  pageSource,
  /ElMessageBox\.confirm\([\s\S]*重置 QA 规程[\s\S]*确认重置/,
  'QA test reset must require an explicit confirmation dialog before calling the API.'
)
assert.match(
  pageSource,
  /await QcTemplateApi\.resetQaRegulationForTesting\(dccProjectCodeId\)[\s\S]*resetQaRegulationConfiguration\(dccProjectCodeId\)/,
  'Successful test reset must clear the current page draft state instead of leaving stale published content.'
)
assert.match(
  pageSource,
  /data-qa-regulation-test-reset[\s\S]*qaRegulationSaving[\s\S]*qaRegulationPublishing[\s\S]*qaWordImportSubmitting/,
  'The reset button must be disabled while save, publish, or import operations are running.'
)
assert.match(
  pageSource,
  /\.qa-regulation-page__version-publish\s*:deep\(\.el-tag\)\s*\{[\s\S]*margin-left:\s*0/,
  'The lifecycle tag inside the action row must not push action buttons out of view.'
)

console.log('PASS qa-regulation-test-reset-static')
