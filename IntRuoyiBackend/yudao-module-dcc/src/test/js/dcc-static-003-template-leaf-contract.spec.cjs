const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const extract = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker`)
  return source.slice(start, end)
}

const templateService = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectFileTemplateServiceImpl.java'
)
const templateServiceTest = read(
  'src/test/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectFileTemplateServiceImplTest.java'
)

const normalizeAndValidate = extract(
  templateService,
  'private DccProjectFileTemplateItemDO normalizeAndValidate',
  'private DccProjectFileTemplateRespVO buildResponse',
  'project template normalize/validate'
)
assert.match(
  normalizeAndValidate,
  /hasActiveChildTaxonomy\(taxonomyId,\s*taxonomyById\)[\s\S]{0,120}PROJECT_FILE_TEMPLATE_TAXONOMY_INVALID/,
  'saving a project file template must reject taxonomy nodes that still have active children'
)

const buildResponse = extract(
  templateService,
  'private DccProjectFileTemplateRespVO buildResponse',
  'private List<DccFileTypeTaxonomyDO> listTemplateTaxonomyOptions',
  'project template response builder'
)
assert.match(
  buildResponse,
  /hasActiveChildTaxonomy\(taxonomyId,\s*taxonomyById\)[\s\S]{0,120}PROJECT_FILE_TEMPLATE_TAXONOMY_INVALID/,
  'loading an already-saved non-leaf template item must fail fast instead of returning an unusable template'
)

const listOptions = extract(
  templateService,
  'private List<DccFileTypeTaxonomyDO> listTemplateTaxonomyOptions',
  'private DccProjectFileTemplateItemRespVO toResponseItem',
  'project template taxonomy options'
)
assert.match(
  listOptions,
  /\.filter\(entry -> !hasActiveChildTaxonomy\(entry\.getKey\(\),\s*taxonomyById\)\)/,
  'template taxonomy options must be generated only from upload-compatible leaf candidates'
)

assert.match(
  templateService,
  /private boolean hasActiveChildTaxonomy\(Long taxonomyId,\s*Map<Long,\s*DccFileTypeTaxonomyDO> taxonomyById\)/,
  'template service must define an explicit active child taxonomy check'
)
assert.match(
  templateServiceTest,
  /replaceProjectTemplate_nonLeafTaxonomyFailsBeforeMutation/,
  'JUnit regression must cover save-time rejection of non-leaf taxonomy'
)
assert.match(
  templateServiceTest,
  /getProjectTemplate_nonLeafSavedItemFailsFast/,
  'JUnit regression must cover existing template data becoming invalid after taxonomy changes'
)

console.log('DCC-STATIC-003 project template leaf taxonomy contract passed')
