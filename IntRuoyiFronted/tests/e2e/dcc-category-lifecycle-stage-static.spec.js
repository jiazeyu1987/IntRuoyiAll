const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const categoryPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const categoryForm = readSource('src/views/dcc/controlled-file/categories/components/CategoryForm.vue')
const categoryApi = readSource('src/api/dcc/controlledFile/fileCategories.ts')
const taxonomyStageUtility = readSource('src/views/dcc/controlled-file/shared/file-type-taxonomy-stage.ts')
const workspaceRoot = path.resolve(root, '..')
const readWorkspaceSource = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')
const categorySaveReq = readWorkspaceSource(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileCategorySaveReqVO.java'
)
const categoryAdminService = readWorkspaceSource(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryAdminServiceImpl.java'
)

assert.equal(
  packageJson.scripts['e2e:dcc:category-lifecycle-stage:static'],
  'node tests/e2e/dcc-category-lifecycle-stage-static.spec.js',
  'package.json must expose DCC category lifecycle stage static test script'
)

assert(categoryApi.includes("lifecycleStage: DccCategoryLifecycleStage"), 'category API type must expose lifecycleStage')
assert(categoryApi.includes('export type DccCategoryLifecycleStage'), 'category API must define lifecycle stage union type')
assert(
  categoryApi.includes('lifecycleStage: DccCategoryLifecycleStage') &&
    categoryApi.includes('interface ControlledFileCategoryReviewMatrixRowVO'),
  'review matrix row API type must expose lifecycleStage for category dialog handoff'
)

assert(categoryPage.includes('prop="taxonomyStageName"'), 'stage column must use taxonomy-derived stage prop')
assert(categoryPage.includes('label="阶段"'), 'category table must show 阶段 column')
assert(categoryPage.includes('formatCategoryTaxonomyStageLabel(row)'), 'stage column must render taxonomy-derived labels')
assert(categoryPage.includes('resolveCategoryTaxonomyStageName(item)'), 'category filter must derive stage from default file taxonomy')
assert(categoryPage.includes('categoryTaxonomyStageOptions'), 'quick filter must use current taxonomy stage options')
assert(categoryPage.includes('queryParams.taxonomyStageName'), 'category filter must match taxonomy stage name')
assert(categoryPage.includes('getCategoryTaxonomyStageTagType(row)'), 'stage column must use taxonomy-derived tag styling')
assert(
  categoryPage.includes("from '../shared/file-type-taxonomy-stage'") &&
    categoryPage.includes('buildDccFileTypeTaxonomyStageNameMap') &&
    categoryPage.includes('getDccFileTypeTaxonomyStageRows') &&
    categoryPage.includes('toDccFileTypeTaxonomyStageOptions'),
  'category list must reuse shared DCC file type taxonomy stage helpers'
)
assert(
  categoryPage.includes("String(queryParams.code || '').trim().toLowerCase()") &&
    categoryPage.includes("String(queryParams.name || '').trim().toLowerCase()"),
  'category quick filter must tolerate cleared text query params'
)
assert(!categoryPage.includes('formatLifecycleStageLabel(row.lifecycleStage)'), 'list must not render old fixed lifecycle labels')
assert(!categoryPage.includes('item.lifecycleStage === queryParams.lifecycleStage'), 'list filter must not use old lifecycleStage value')

assert(categoryForm.includes('categoryTaxonomyStageName'), 'category form must show taxonomy-derived stage')
assert(categoryForm.includes('阶段随默认文件分类自动派生'), 'category form must explain stage derivation')
assert(categoryForm.includes('fileTypeTaxonomyId: [{ required: true'), 'category form must require default file taxonomy')
assert(categoryForm.includes("lifecycleStage: '' as ControlledFileCategoryVO['lifecycleStage']"), 'category form must let backend derive legacy compatibility field')
assert(!categoryForm.includes('DCC_CATEGORY_LIFECYCLE_STAGE_OPTIONS'), 'category form must not expose fixed legacy stage options')
assert(!categoryForm.includes('v-model="formData.lifecycleStage"'), 'category form must not manually bind lifecycleStage')
assert(!categoryForm.includes('阶段不能为空'), 'category form must not require manual lifecycleStage')
assert(!categoryForm.includes('allow-create'), 'category stage display must not allow custom manual creation')

assert(categorySaveReq.includes('@NotNull(message = "默认文件分类不能为空")'), 'backend save VO must require default file taxonomy')
assert(!categorySaveReq.includes('@NotBlank(message = "类别阶段不能为空")'), 'backend save VO must not require manual legacy lifecycleStage')
assert(categoryAdminService.includes('applyLifecycleStageFromFileTypeTaxonomy'), 'backend must derive lifecycleStage from taxonomy path')
assert(categoryAdminService.includes('resolveLifecycleStageFromTaxonomyStage'), 'backend must map taxonomy stage to legacy compatibility field')
assert(!categoryAdminService.includes('normalizeAndValidateLifecycleStage'), 'backend must not validate client-provided lifecycleStage')

for (const token of [
  'DCC_TECHNICAL_DOCUMENT_ROOT_NAME',
  'DCC_UNCLASSIFIED_TAXONOMY_STAGE',
  'buildDccFileTypeTaxonomyPathMap',
  'buildDccFileTypeTaxonomyStageNameMap',
  'getDccFileTypeTaxonomyStageRows',
  'toDccFileTypeTaxonomyStageOptions',
  'resolveDccFileTypeTaxonomyStageName',
  'getDccFileTypeTaxonomyStageTagType'
]) {
  assert(taxonomyStageUtility.includes(token), `shared taxonomy stage utility must expose ${token}`)
}

console.log('PASS: DCC category lifecycle stage static contract')
