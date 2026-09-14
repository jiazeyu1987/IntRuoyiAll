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
  'QA item inspection helper must be the single source of structured inspection applicability.'
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
  resolveQaItemDisplayInspectionTypes,
  resolveQaItemInspectionPayload
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

assert.deepEqual(
  resolveQaItemDisplayInspectionTypes(appearance, false),
  ['FIRST', 'PATROL_AM', 'PATROL_PM'],
  'Appearance must expose its own first and patrol applicability.'
)
assert.deepEqual(resolveQaItemInspectionPayload(appearance, false, '外观'), {
  applicableInspectionTypes: ['FIRST', 'PATROL'],
  firstInspectionQuantity: 13,
  patrolInspectionRatio: 0.4
})
assert.deepEqual(
  resolveQaItemInspectionPayload(pressureRelease, false, '撤压'),
  {
    applicableInspectionTypes: ['FIRST', 'PATROL'],
    firstInspectionQuantity: 5,
    patrolInspectionRatio: 1
  },
  'Items in the same process must retain different first quantities and patrol ratios.'
)
assert.deepEqual(
  resolveQaItemDisplayInspectionTypes(
    {
      firstInspectionEnabled: true,
      firstInspectionQuantity: 2,
      patrolInspectionEnabled: false,
      patrolInspectionRatio: undefined
    },
    true
  ),
  ['FIRST', 'FINAL'],
  'Patrol applicability must be independently disabled per item while final remains project-wide.'
)

assert.match(
  pageSource,
  /data-qa-regulation-applicable-types[\s\S]*v-for="inspectionType in resolveQaItemApplicableTypes\(row\)"[\s\S]*resolveQaInspectionTypeLabel\(inspectionType\)/,
  'Applicable inspection types must render from the current item state.'
)
assert.match(
  pageSource,
  /data-qa-regulation-first-inspection[\s\S]*v-model="row\.firstInspectionEnabled"[\s\S]*v-model="row\.firstInspectionQuantity"/,
  'First inspection must be independently editable per item.'
)
assert.match(
  pageSource,
  /data-qa-regulation-patrol-inspection[\s\S]*v-model="row\.patrolInspectionEnabled"[\s\S]*v-model="row\.patrolInspectionRatio"/,
  'Patrol inspection must be independently editable per item.'
)
assert.doesNotMatch(
  pageSource,
  /parseQaInspectionSamplingPlan|resolveQaApplicableInspectionTypes/,
  'The QA page must not reconstruct structured inspection rules from sampling-plan text.'
)

const saveItemStart = pageSource.indexOf('const buildQaRegulationSaveItem =')
const saveItemEnd = pageSource.indexOf('const buildQaRegulationProcesses =', saveItemStart)
assert.ok(saveItemStart >= 0 && saveItemEnd > saveItemStart, 'QA save-item builder must exist.')
const saveItemSource = pageSource.slice(saveItemStart, saveItemEnd)

assert.match(
  saveItemSource,
  /resolveQaItemInspectionPayload\([\s\S]*item,[\s\S]*finalInspectionRequired\.value,[\s\S]*itemName/,
  'Save payload must validate and serialize each item structured inspection state.'
)
assert.match(
  saveItemSource,
  /firstInspectionQuantity:\s*inspectionConfiguration\.firstInspectionQuantity/,
  'FIRST quantity must come from the current item structured field.'
)
assert.match(
  saveItemSource,
  /patrolInspectionRatio:\s*inspectionConfiguration\.patrolInspectionRatio/,
  'PATROL ratio must come from the current item structured field.'
)

console.log('PASS qa-regulation-applicable-types-derived-static')
