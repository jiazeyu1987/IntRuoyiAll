import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom admin product api exposes product english translation contract', () => {
  const source = readText('src/api/showroom-admin/index.ts')

  assert.match(source, /ShowroomProductFieldTranslateReqVO/)
  assert.match(source, /ShowroomProductFieldTranslateRespVO/)
  assert.match(source, /translateProductFieldsToEn/)
  assert.match(source, /url: '\/showroom\/product\/translate-fields-to-en'/)
  assert.match(source, /narrationScriptZh/)
  assert.match(source, /narrationScriptEn/)
  assert.match(source, /translatedFields/)
})

test('showroom admin product basic dialog renders bilingual tabs and english narration workspace', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const dialogSource =
    source.match(/<el-dialog[\s\S]*?v-model="productDialogVisible"[\s\S]*?<\/el-dialog>/)?.[0] || ''
  const zhTabSource =
    dialogSource.match(/<el-tab-pane label="中文" name="zh">[\s\S]*?<\/el-tab-pane>/)?.[0] || ''
  const enTabSource =
    dialogSource.match(/<el-tab-pane label="English" name="en">[\s\S]*?<\/el-tab-pane>/)?.[0] || ''

  assert.match(dialogSource, /<el-tabs/)
  assert.match(dialogSource, /中文/)
  assert.match(dialogSource, /English/)
  assert.match(dialogSource, /AI Translate/)
  assert.doesNotMatch(dialogSource, /Generate Audio|生成语音/)
  assert.match(dialogSource, /English Narration/)
  assert.match(dialogSource, /<audio/)
  assert.match(source, /translateProductFieldsToEn/)
  assert.match(source, /narrationScriptEn|productNarrationDraftEn|scriptTextEn/)
  assert.match(zhTabSource, /中文音频/)
  assert.doesNotMatch(enTabSource, /中文音频/)
  assert.match(enTabSource, /English Audio/)
  assert.match(enTabSource, /English Name/)
  assert.doesNotMatch(
    enTabSource,
    /AI翻译|生成语音|英文名称|英文讲解稿|英文音频|未生成|取消|保存草稿|保存|提交审批/
  )
})

test('showroom admin product translate handler validates translated fields before batch refill', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /const resolveTranslatedProductText = \(/)
  assert.match(source, /产品翻译失败：\$\{fieldLabel\}缺少英文结果/)
  assert.match(source, /const sourceFields = buildProductFieldSnapshot\(productForm\)/)
  assert.match(source, /const nameCn = productForm\.nameCn\.trim\(\)/)
  assert.match(source, /const narrationScriptZh = productNarrationDraft\.zhScriptText\.trim\(\)/)
  assert.match(
    source,
    /const translatedNameEn = resolveTranslatedProductText\(nameCn, translation\.nameEn, '英文名称'\)/
  )
  assert.match(
    source,
    /const translatedTargetMarketEn = resolveTranslatedProductText\(\s*sourceFields\.target_market,\s*translatedFields\.target_market_en,\s*'在售国家'\s*\)/
  )
  assert.match(
    source,
    /const translatedPipelineLayoutEn = resolveTranslatedProductText\(\s*sourceFields\.pipeline_layout,\s*translatedFields\.pipeline_layout_en,\s*'BU'\s*\)/
  )
  assert.match(
    source,
    /const translatedModelSpecificationEn = resolveTranslatedProductText\(\s*sourceFields\.model_specification,\s*translatedFields\.model_specification_en,\s*'型号规格'\s*\)/
  )
  assert.match(
    source,
    /const translatedNarrationScriptEn = resolveTranslatedProductText\(\s*narrationScriptZh,\s*translation\.narrationScriptEn,\s*'英文讲解稿'\s*\)/
  )
  assert.doesNotMatch(
    source,
    /productForm\.targetMarketEn = resolveStringValue\(translatedFields\.target_market_en\)\.trim\(\)/
  )
  assert.doesNotMatch(
    source,
    /productForm\.nameEn = resolveStringValue\(translation\.nameEn\)\.trim\(\)/
  )
})

test('showroom admin product detail dialog renders bilingual advanced tabs', () => {
  const source = readText('src/views/showroom-admin/product/ProductDetailDialog.vue')

  assert.match(source, /<el-tabs/)
  assert.match(source, /中文/)
  assert.match(source, /English/)
  assert.match(source, /AI Translate/)
  assert.match(source, /registration_certificate_en/)
  assert.match(source, /clinical_effect_en/)
  assert.match(source, /fim_status_en/)
})

test('showroom admin product contracts expose english field variants', () => {
  const source = readText('src/views/showroom-admin/product/contracts.ts')

  for (const token of [
    'target_market_en',
    'pipeline_layout_en',
    'indication_content_en',
    'core_selling_points_en',
    'model_specification_en',
    'registration_certificate_en',
    'clinical_effect_en',
    'fim_status_en'
  ]) {
    assert.match(source, new RegExp(token))
  }
})

test('showroom admin product list exposes row voice action while keeping batch audio entry', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /一键语音/)
  assert.match(
    source,
    /emit\('assign', row\.raw\)[\s\S]*?>\s*指派\s*<\/el-button>[\s\S]*emit\('open-audio-dialog', row\.raw\)[\s\S]*?>\s*语音\s*<\/el-button>/
  )
})
