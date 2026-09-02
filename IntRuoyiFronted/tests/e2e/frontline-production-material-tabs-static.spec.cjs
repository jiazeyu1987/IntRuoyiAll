const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const panel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApi = readUtf8('src/api/mes/pro/feedback/index.ts')

assert.match(
  feedbackApi,
  /export interface FrontlineRuntimeMaterialVO[\s\S]*materialId:\s*number[\s\S]*bomQuantity\?:\s*number\s*\|\s*null[\s\S]*batchCodes:\s*string\[\]/,
  'runtime API must expose frozen process materials and locally synchronized batch codes.'
)
assert.match(
  feedbackApi,
  /FrontlineRuntimeConfigVO[\s\S]*materials:\s*FrontlineRuntimeMaterialVO\[\]/,
  'runtime config must carry the frozen material collection.'
)

assert.match(
  panel,
  /data-frontline-production-material-tabs[\s\S]*v-for="material in configuredProductionMaterials"/,
  'the red-box area must render one tab for every frozen process material.'
)
assert.match(
  panel,
  /data-frontline-production-material-tab[\s\S]*role="tab"[\s\S]*switchProductionMaterial\(material\.key\)/,
  'each material tab must be a touch-friendly tab that switches the active material draft.'
)
assert.match(
  panel,
  /<strong>\s*\{\{\s*formatProductionMaterialTabLabel\(material\)\s*\}\}\s*<\/strong>/,
  'material tabs must render the material name with entered output/loss quantity summary.'
)
assert.match(
  panel,
  /const formatProductionMaterialTabLabel = \(material: ProductionMaterialOption\) => \{[\s\S]*outputQuantity[\s\S]*lossQuantity[\s\S]*`\$\{material\.materialName\}\(\$\{outputQuantity\}\/\$\{lossQuantity\}\)`/,
  'completed material tabs must show only name(output/loss), for example 杠杆(3012/12).'
)
assert.doesNotMatch(
  panel,
  /frontline-production-material-batches|material\.batchCodes\.join\('、'\)|<small>\s*\{\{\s*material\.materialCode/,
  'material tabs must not render material codes or batch-code rows.'
)
assert.match(
  panel,
  /'is-complete':\s*isProductionMaterialCompletionEntered\(material\.key\)/,
  'the green state must be derived from the material completion-entry predicate.'
)
assert.match(
  panel,
  /const isProductionMaterialCompletionEntered[\s\S]*outputQuantity !== undefined/,
  'an explicitly entered zero must count as completed for the tab color.'
)
const completionPredicate = panel.match(
  /const isProductionMaterialCompletionEntered[\s\S]*?(?=\nconst )/
)?.[0]
assert.ok(completionPredicate, 'the material completion-entry predicate must exist.')
assert.doesNotMatch(
  completionPredicate,
  /outputQuantity\s*>\s*0|productionDefectDraft|deviceParameterDraft|loss|parameter/,
  'tab color must not depend on a positive quantity, loss data, or parameter data.'
)
assert.match(
  panel,
  /const productionMaterialDrafts[\s\S]*const persistActiveProductionMaterialDraft[\s\S]*const restoreProductionMaterialDraft/,
  'each material must own an isolated draft that is persisted and restored during tab switches.'
)
assert.match(
  panel,
  /const switchProductionMaterial[\s\S]*persistActiveProductionMaterialDraft\(\)[\s\S]*restoreProductionMaterialDraft\(materialKey\)/,
  'switching tabs must persist the current material before restoring the target material.'
)
assert.doesNotMatch(
  panel,
  /当前工序没有冻结物料，无法提交/,
  'an empty batch-record material configuration must not block formal production submission.'
)
assert.match(
  panel,
  /const resolveProductionProgressQuantity[\s\S]*materialDetails\.length[\s\S]*productionDraft\.outputQuantity/,
  'formal submission must use the process completion quantity when no material details are configured.'
)
assert.match(
  panel,
  /const buildFrontlineFormalSubmitPayload[\s\S]*materialDetails[\s\S]*resolveProductionProgressQuantity\(materialDetails\)/,
  'the formal request must send an empty material collection without calculating Math.min over an empty array.'
)
assert.match(
  panel,
  /\.frontline-production-material-tab[\s\S]*background:\s*#(?:e8ece9|eef1ef)[\s\S]*(?:\.frontline-production-material-tab|&)\.is-complete[\s\S]*background:\s*#(?:16825d|17835f|1f8a63)/i,
  'material tabs must use only a restrained gray background and a green completed background.'
)

console.log('PASS: frontline production material tabs use frozen materials and gray/green completion state')
