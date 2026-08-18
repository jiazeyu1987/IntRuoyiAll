const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const helperPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/qaRegulationItemInspection.ts'
)
const pageSource = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

assert.ok(
  fs.existsSync(helperPath),
  'QA item inspection helper must exist so structured backend fields remain item-owned.'
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
const {
  createQaItemInspectionState,
  resolveQaItemInspectionPayload,
  resolveQaItemDisplayInspectionTypes
} = helperModule.exports

const appearance = createQaItemInspectionState({
  applicableInspectionTypes: ['FIRST', 'PATROL'],
  firstInspectionQuantity: 13,
  patrolInspectionRatio: 0.4
})
const pressureRelease = createQaItemInspectionState({
  applicableInspectionTypes: ['FIRST', 'PATROL'],
  firstInspectionQuantity: 5,
  patrolInspectionRatio: 1
})

assert.deepEqual(appearance, {
  firstInspectionEnabled: true,
  firstInspectionQuantity: 13,
  patrolInspectionEnabled: true,
  patrolInspectionRatio: 0.4
})
assert.deepEqual(pressureRelease, {
  firstInspectionEnabled: true,
  firstInspectionQuantity: 5,
  patrolInspectionEnabled: true,
  patrolInspectionRatio: 1
})
assert.deepEqual(
  resolveQaItemInspectionPayload(appearance, false, '外观'),
  {
    applicableInspectionTypes: ['FIRST', 'PATROL'],
    firstInspectionQuantity: 13,
    patrolInspectionRatio: 0.4
  },
  'The appearance item must keep its own structured first and patrol values.'
)
assert.deepEqual(
  resolveQaItemInspectionPayload(pressureRelease, false, '撤压'),
  {
    applicableInspectionTypes: ['FIRST', 'PATROL'],
    firstInspectionQuantity: 5,
    patrolInspectionRatio: 1
  },
  'The pressure-release item must not inherit the appearance quantities.'
)
assert.deepEqual(
  resolveQaItemDisplayInspectionTypes(appearance, true),
  ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
  'Final applicability remains project-wide while first and patrol remain item-owned.'
)
assert.throws(
  () =>
    resolveQaItemInspectionPayload(
      {
        firstInspectionEnabled: true,
        firstInspectionQuantity: undefined,
        patrolInspectionEnabled: false,
        patrolInspectionRatio: undefined
      },
      false,
      '外观'
    ),
  /首检数量/,
  'An enabled item-level first inspection must fail fast without a positive quantity.'
)

assert.match(
  pageSource,
  /data-qa-regulation-configuration-status[\s\S]*qaConfigurationStatusText/,
  'QA page must expose an explicit backend configuration status.'
)
assert.match(
  pageSource,
  /const qaConfigurationStatusText = computed[\s\S]*未配置/,
  'A DCC project without backend QA data must display 未配置.'
)
assert.match(
  pageSource,
  /data-qa-regulation-first-inspection[\s\S]*v-model="row\.firstInspectionEnabled"[\s\S]*v-model="row\.firstInspectionQuantity"/,
  'Each QA item row must expose its own first-inspection switch and fixed quantity.'
)
assert.match(
  pageSource,
  /data-qa-regulation-patrol-inspection[\s\S]*v-model="row\.patrolInspectionEnabled"[\s\S]*v-model="row\.patrolInspectionRatio"/,
  'Each QA item row must expose its own patrol switch and percentage.'
)
assert.match(
  pageSource,
  /createQaItemInspectionState\(item\)/,
  'Loaded QA items must use backend structured fields directly.'
)
assert.doesNotMatch(
  pageSource,
  /parseQaInspectionSamplingPlan\(item\.samplingPlanText/,
  'Save and display must not reconstruct structured values from sampling-plan text.'
)
assert.match(
  pageSource,
  /if \(!qaConfigurationExists\.value && qaRegulationItems\.value\.length === 0\) \{\s*return \[\]/,
  'Unconfigured DCC projects must not show frontend default task preview rows.'
)
assert.match(
  pageSource,
  /qaProcessName[\s\S]*itemName[\s\S]*inspectionTypeText[\s\S]*plannedQuantityText/,
  'QA preview must identify the process, inspection item, inspection type and item-owned plan.'
)
assert.match(
  pageSource,
  /qaRegulationItems\.value\.flatMap\(\(item\)/,
  'QA preview must expand from inspection items instead of global inspection rules.'
)

console.log('PASS: QA regulation item-level inspection display is wired')
