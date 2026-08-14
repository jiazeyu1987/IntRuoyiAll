const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const helperPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/qaRegulationSampling.ts'
)
const pageSource = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

assert.ok(
  fs.existsSync(helperPath),
  'QA sampling-plan derivation helper must exist as the single source of inspection types.'
)

const ts = require(path.join(frontendRoot, 'node_modules', 'typescript'))
const helperSource = fs.readFileSync(helperPath, 'utf8')
const transpiled = ts.transpileModule(helperSource, {
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2020,
    strict: true
  },
  fileName: helperPath
})
const helperModule = { exports: {} }
new Function('module', 'exports', 'require', transpiled.outputText)(
  helperModule,
  helperModule.exports,
  require
)
const { parseQaInspectionSamplingPlan, resolveQaApplicableInspectionTypes } =
  helperModule.exports

assert.deepEqual(
  resolveQaApplicableInspectionTypes('GB/T 2828.1，I，AQL=0.4', false),
  ['PATROL_AM', 'PATROL_PM'],
  'Morning and afternoon patrol must be included by default without first or final inspection.'
)
assert.deepEqual(
  resolveQaApplicableInspectionTypes('首件：13 件；GB/T 2828.1，I，AQL=0.4', true),
  ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
  'First inspection must come from the sampling plan and final inspection from the switch.'
)
assert.deepEqual(
  parseQaInspectionSamplingPlan('首检: 5件；GB/T 2828.1，S-3，AQL=0.4', '牢固度'),
  {
    firstInspectionQuantity: 5,
    patrolAql: 0.4,
    patrolInspectionRatio: 0.4
  },
  'Sampling-plan parser must read first quantity and preserve AQL as the backend percentage ratio.'
)
assert.deepEqual(
  parseQaInspectionSamplingPlan('GB/T 2828.1，I，AQL=1.0', '外观'),
  {
    firstInspectionQuantity: undefined,
    patrolAql: 1,
    patrolInspectionRatio: 1
  },
  'Sampling plans without a first-inspection marker must remain patrol-only.'
)
assert.throws(
  () => parseQaInspectionSamplingPlan('GB/T 2828.1，I', '外观'),
  /AQL/,
  'A missing patrol AQL must fail fast before save.'
)
assert.throws(
  () => parseQaInspectionSamplingPlan('首件：五件；AQL=0.4', '牢固度'),
  /首检数量/,
  'An invalid explicit first-inspection quantity must fail fast before save.'
)

assert.match(
  pageSource,
  /data-qa-regulation-applicable-types[\s\S]*v-for="inspectionType in resolveQaItemApplicableTypes\(row\)"[\s\S]*resolveQaInspectionTypeLabel\(inspectionType\)/,
  'Applicable inspection types must render as derived read-only tags.'
)
assert.doesNotMatch(
  pageSource,
  /v-model="row\.applicableTypes"/,
  'Applicable inspection types must not remain independently editable.'
)
assert.doesNotMatch(
  pageSource,
  /applicableTypes:\s*\[/,
  'QA item seed data must not retain a second inspection-type source.'
)
assert.doesNotMatch(
  pageSource,
  /item\.applicableTypes/,
  'Page logic must not read stale per-item inspection-type arrays.'
)
assert.doesNotMatch(
  pageSource,
  /sampleOrderQuantity|rule\.sampleRatio/,
  'The old global sample-ratio source must not remain after AQL becomes item-owned.'
)

const saveItemsStart = pageSource.indexOf('const buildQaRegulationSaveItems =')
const saveItemsEnd = pageSource.indexOf('const resolveQaRegulationItemRouteProcesses =', saveItemsStart)
assert.ok(saveItemsStart >= 0 && saveItemsEnd > saveItemsStart, 'QA save-item builder must exist.')
const saveItemsSource = pageSource.slice(saveItemsStart, saveItemsEnd)

assert.match(
  saveItemsSource,
  /parseQaInspectionSamplingPlan\(item\.samplingPlanText, item\.itemName\)/,
  'Save payload must parse each item sampling plan strictly.'
)
assert.match(
  saveItemsSource,
  /resolveQaApplicableInspectionTypes\(\s*item\.samplingPlanText,\s*finalInspectionRequired\.value\s*\)/,
  'Save payload must derive types from the sampling plan and final-inspection switch.'
)
assert.match(
  saveItemsSource,
  /firstInspectionQuantity:[\s\S]*samplingPlan\??\.firstInspectionQuantity/,
  'FIRST payload quantity must come from the current item sampling plan.'
)
assert.match(
  saveItemsSource,
  /patrolInspectionRatio:[\s\S]*samplingPlan\??\.patrolInspectionRatio/,
  'PATROL payload ratio must come from the current item AQL.'
)

assert.doesNotMatch(
  helperSource,
  /patrolAql\s*\/\s*100/,
  'AQL must remain a percentage because the backend quantity calculation already divides by 100.'
)

console.log('PASS qa-regulation-applicable-types-derived-static')
