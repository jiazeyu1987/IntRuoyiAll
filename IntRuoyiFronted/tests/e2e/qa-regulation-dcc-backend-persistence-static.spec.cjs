const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const apiSource = read('src/api/mes/qc/template/index.ts')
const pageSource = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const dccListSource = read(
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)

assert.match(apiSource, /dccProjectCodeId:\s*number/)
assert.match(apiSource, /processes:\s*QaInspectionRegulationProcessVO\[\]/)
assert.match(apiSource, /getCurrentQaRegulation/)
assert.match(apiSource, /dccProjectCodeIds:\s*dccProjectCodeIds\.join\(','\)/)
assert.doesNotMatch(apiSource, /productId:\s*number[\s\S]*routeProcessId:\s*number/)

assert.match(pageSource, /QcTemplateApi\.getCurrentQaRegulation/)
assert.match(pageSource, /dccProjectCodeId:\s*resolvePositiveId/)
assert.match(pageSource, /processes:\s*buildQaRegulationProcesses/)
assert.match(
  pageSource,
  /data-qa-regulation-header-save[\s\S]*@click="previewQaRegulationDraft"/
)
assert.match(pageSource, /v-loading="qaCurrentConfigurationLoading"/)
assert.match(pageSource, /新增 QA 工序\/检验项目/)
assert.doesNotMatch(pageSource, /createPressurePumpQaRegulationItems/)
assert.doesNotMatch(pageSource, /createBalloonPressurePumpQaRegulationItems/)
assert.doesNotMatch(pageSource, /resolveQaRegulationItemRouteProcesses/)
assert.doesNotMatch(pageSource, /QA_PROCESS_SCOPE_BINDINGS_BY_PROJECT_CODE/)
assert.doesNotMatch(pageSource, /saveQaRegulationRouteProductByItem/)

assert.match(dccListSource, /key:\s*'qaRegulationStatus'/)
assert.match(dccListSource, /QcTemplateApi\.getQaRegulationProjectStatuses/)
assert.match(dccListSource, /name:\s*'MesProProcessPoolQaRegulation'/)
assert.match(dccListSource, /dccProjectCodeId:\s*String\(row\.id\)/)

console.log('PASS: QA regulation frontend persists one DCC-owned backend regulation')
