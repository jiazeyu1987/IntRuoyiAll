const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const qaSource = fs.readFileSync(qaPagePath, 'utf8')

const applyProjectStart = qaSource.indexOf('const applyDccProjectToQaDraft =')
const handleProjectStart = qaSource.indexOf('const handleDccProjectCodeChange =', applyProjectStart)

assert.ok(
  applyProjectStart >= 0 && handleProjectStart > applyProjectStart,
  'QA project application function must remain available.'
)

const applyProjectSource = qaSource.slice(applyProjectStart, handleProjectStart)
const pressurePumpBindingStart = qaSource.indexOf('const registerPressurePumpProductBinding =')
const saveProductDraftStart = qaSource.indexOf(
  'const saveCurrentQaProductRuleDraft =',
  pressurePumpBindingStart
)

assert.ok(
  pressurePumpBindingStart >= 0 && saveProductDraftStart > pressurePumpBindingStart,
  'Pressure-pump product binding registration must remain available.'
)

const pressurePumpBindingSource = qaSource.slice(pressurePumpBindingStart, saveProductDraftStart)
const emptyRuleProfileStart = qaSource.indexOf('const createEmptyQaInspectionTypeRules =')
const pressurePumpRuleProfileStart = qaSource.indexOf(
  'const createPressurePumpQaInspectionTypeRules =',
  emptyRuleProfileStart
)

assert.ok(
  emptyRuleProfileStart >= 0 && pressurePumpRuleProfileStart > emptyRuleProfileStart,
  'Empty and pressure-pump rule profiles must remain explicit and separately testable.'
)

const emptyRuleProfileSource = qaSource.slice(emptyRuleProfileStart, pressurePumpRuleProfileStart)

assert.match(
  qaSource,
  /interface QaProductRuleDraftSnapshot/,
  'QA page must model a product-owned rule draft snapshot.'
)
assert.match(
  qaSource,
  /const qaProductRuleDrafts = new Map<number, QaProductRuleDraftSnapshot>\(\)/,
  'QA page must cache draft state by formal product ID.'
)
assert.match(
  qaSource,
  /const createEmptyQaInspectionTypeRules = \(\): QaInspectionTypeRule\[\] =>/,
  'Unconfigured products must receive an explicit empty rule profile.'
)
assert.match(
  emptyRuleProfileSource,
  /=>\s*\[\s*\]/,
  'Unconfigured or other products must render no inherited inspection-rule rows until their own product rules are configured.'
)
assert.match(
  qaSource,
  /const createBaseQaInspectionTypeRules = \(\): QaInspectionTypeRule\[\] =>/,
  'Shared first/patrol/final row labels may only live in a base profile used by configured product templates.'
)
assert.match(
  qaSource,
  /const createPressurePumpQaInspectionTypeRules = \(\): QaInspectionTypeRule\[\] =>/,
  'The existing pressure-pump rules must remain an explicit product profile.'
)
assert.match(
  qaSource,
  /const createPressurePumpQaInspectionTypeRules = \(\): QaInspectionTypeRule\[\] =>\s*createBaseQaInspectionTypeRules\(\)\.map/,
  'The pressure-pump profile may seed first/patrol/final rows only through its own product template.'
)
assert.match(
  qaSource,
  /const replaceQaInspectionTypeRules = \(rules: QaInspectionTypeRule\[\]\) =>/,
  'Product switches must replace the shared reactive rule array deterministically.'
)
assert.match(
  qaSource,
  /v-for="rule in qaInspectionTypeRules"[\s\S]*\{\{\s*rule\.label\s*\}\}/,
  'The inspection-rule tag rail must render only the current product rule labels.'
)
assert.doesNotMatch(
  qaSource,
  /<el-tag effect="plain">首检<\/el-tag>[\s\S]*<el-tag effect="plain">上午巡检<\/el-tag>[\s\S]*<el-tag effect="plain">下午巡检<\/el-tag>[\s\S]*<el-tag effect="plain">末检<\/el-tag>/,
  'The QA page must not hardcode first/patrol/final tag labels for every selected product.'
)
assert.match(
  qaSource,
  /const qaPatrolPreviewText = computed/,
  'The patrol preview hint must be derived from the current product rules.'
)
assert.doesNotMatch(
  qaSource,
  /Math\.ceil\(qaRegulationDraft\.sampleOrderQuantity \* 0\.05\)/,
  'Other products must not inherit the pressure-pump 5% patrol preview text.'
)
assert.match(
  qaSource,
  /const cloneQaRegulationItems = \(items: QaRegulationItem\[\]\) =>[\s\S]*applicableTypes:\s*\[\.\.\.item\.applicableTypes\]/,
  'Product snapshots must deep-clone nested inspection-type selections.'
)
assert.match(
  qaSource,
  /const saveCurrentQaProductRuleDraft = \(\) =>/,
  'QA page must preserve the current product draft before switching products.'
)
assert.match(
  qaSource,
  /const productId = activeQaRegulationProductId\.value[\s\S]*qaProductRuleDrafts\.set\(\s*productId,/,
  'The current draft must be stored under the active formal product ID.'
)
assert.match(
  qaSource,
  /const loadQaProductRuleDraft = \([\s\S]*productId: number[\s\S]*project: DccProjectCodeRespVO[\s\S]*\) =>/,
  'QA page must load rule state by formal product ID.'
)
assert.match(
  qaSource,
  /const pressurePumpProductId = ref<number \| undefined>\(\)/,
  'The pressure-pump profile must be attached to a resolved formal product ID.'
)
assert.match(
  pressurePumpBindingSource,
  /resolveDccProjectProductId\(pressurePumpProject\)[\s\S]*pressurePumpProductId\.value = productId/,
  'The legacy IDI seed may only register the pressure-pump profile through its formal product binding.'
)
assert.match(
  applyProjectSource,
  /saveCurrentQaProductRuleDraft\(\)[\s\S]*const productId = resolveDccProjectProductId\(project\)/,
  'Switching DCC projects must save the previous product and resolve the next formal product ID.'
)
assert.doesNotMatch(
  applyProjectSource,
  /projectCode\s*===\s*PRESSURE_PUMP_PROJECT_CODE/,
  'Project code must not directly select the pressure-pump rule profile.'
)
assert.match(
  applyProjectSource,
  /if \(!productId\)[\s\S]*replaceQaInspectionTypeRules\(createEmptyQaInspectionTypeRules\(\)\)/,
  'A DCC project without product binding must clear rules instead of loading the pressure-pump profile.'
)
assert.match(
  applyProjectSource,
  /loadQaProductRuleDraft\(productId, project\)/,
  'The selected product must drive regulation, item, and rule draft loading.'
)
assert.match(
  qaSource,
  /qaProductRuleDrafts\.get\(productId\)[\s\S]*qaProductRuleDrafts\.set\(productId,/,
  'The same product must reuse one cached rule draft across DCC project entries.'
)

console.log('PASS qa-regulation-product-specific-rules-static')
