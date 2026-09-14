const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(
    frontendRoot,
    'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
  ),
  'utf8'
)
const qaRegulationApiSource = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/qc/template/index.ts'),
  'utf8'
)
const compact = (text) => text.replace(/\s+/g, '')

assert.match(
  source,
  /QcTemplateApi,[\s\S]*type QaInspectionRegulationProjectStatusVO/,
  'DCC list must import MES QA status API and VO.'
)
assert.ok(
  compact(source).includes("{key:'qaRegulationStatus',label:'QA规程'"),
  'DCC list must register the QA regulation status column.'
)
assert.match(source, /data-testid="dcc-project-code-qa-regulation-link"/, 'QA status cell must have a stable link target.')
assert.ok(
  compact(source).includes("name:'MesProProcessPoolQaRegulation'"),
  'QA status link must navigate to the QA regulation page.'
)
assert.ok(
  compact(source).includes("query:{dccProjectCodeId:String(row.id)}"),
  'QA link must pass only the DCC project code ID.'
)

const qaColumnStart = source.indexOf('label="QA规程"')
const qaColumnEnd = source.indexOf('</el-table-column>', qaColumnStart)
assert.ok(qaColumnStart >= 0 && qaColumnEnd > qaColumnStart, 'QA status column template must exist.')
const qaColumnSource = source.slice(qaColumnStart, qaColumnEnd)
assert.match(
  qaColumnSource,
  /<el-tag[\s\S]*class="scheme-d-tag"[\s\S]*effect="plain"/,
  'QA status must use the same plain status-tag presentation as the main batch record.'
)
assert.match(
  qaColumnSource,
  /resolveQaRegulationStatusTagType\(getQaRegulationProjectStatus\(row\.id\)\)/,
  'QA status tag type must come from the formal QA project status.'
)
assert.match(
  qaColumnSource,
  /data-testid="dcc-project-code-governance-version-qa-regulation"/,
  'Published QA status must expose a stable current-version element.'
)
assert.match(
  qaColumnSource,
  /formatQaRegulationPublishedVersion\(getQaRegulationProjectStatus\(row\.id\)\)/,
  'QA version display must use the formal published version instead of a draft or inferred value.'
)

const loadStart = source.indexOf('const loadQaRegulationStatuses')
const formatStart = source.indexOf('const formatQaRegulationStatus', loadStart)
assert.ok(loadStart >= 0 && formatStart > loadStart, 'QA status loader must exist.')
const loadSource = source.slice(loadStart, formatStart)
const compactLoad = compact(loadSource)

assert.ok(
  compact(source).includes('letqaRegulationStatusLoadSerial=0'),
  'DCC QA status loading must track request serials so stale page responses cannot overwrite newer rows.'
)
assert.ok(
  compact(source).includes('constqaRegulationStatusPermissionDenied=ref(false)'),
  'DCC QA status column must keep an explicit no-permission state.'
)
assert.ok(
  compactLoad.includes('constloadSerial=++qaRegulationStatusLoadSerial'),
  'Each QA status batch request must claim a serial.'
)
assert.ok(
  compactLoad.includes('.map((row)=>Number(row.id))'),
  'The batch request must derive IDs from current-page DCC project rows.'
)
assert.ok(
  compactLoad.includes('QcTemplateApi.getQaRegulationProjectStatuses(dccProjectCodeIds)'),
  'The DCC list must make one frontend-side MES bulk status call for the current page.'
)
assert.match(loadSource, /loadSerial\s*!==\s*qaRegulationStatusLoadSerial[\s\S]*return/)
assert.match(
  source,
  /qaRegulationStatusPermissionDenied\.value[\s\S]*无查询权限/,
  'No QA query permission must render as no permission instead of unconfigured.'
)
assert.match(
  qaRegulationApiSource,
  /publishedVersionNo\?: string/,
  'The frontend QA project-status contract must include the formal published version number.'
)
assert.doesNotMatch(
  loadSource,
  /\b(productId|productMasterId|routeId|routeProcessId|processId|workOrderId)\b|getDccProjectGovernanceStatus/,
  'QA status loading must not infer QA regulation through products, routes, MES processes, or DCC governance.'
)

console.log('PASS: DCC project code QA status column static checks')
