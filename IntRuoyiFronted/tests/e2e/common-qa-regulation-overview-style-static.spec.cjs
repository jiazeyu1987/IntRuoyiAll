const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const projectRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(projectRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const qaOverviewStart = source.indexOf('data-qa-regulation-common-binding-control')
const qaOverviewEnd = source.indexOf('data-qa-regulation-items', qaOverviewStart)
assert.ok(qaOverviewStart >= 0, 'QA regulation overview must retain its binding card.')
assert.ok(
  qaOverviewEnd > qaOverviewStart,
  'QA regulation overview must have a stable end boundary.'
)
const qaOverview = source.slice(qaOverviewStart, qaOverviewEnd)

const commonOverviewStart = source.indexOf('data-qa-common-overview')
const commonOverviewEnd = source.indexOf('data-qa-common-versions', commonOverviewStart)
assert.ok(commonOverviewStart >= 0, 'Common regulation overview must exist.')
assert.ok(
  commonOverviewEnd > commonOverviewStart,
  'Common regulation overview must have a stable end boundary.'
)
const commonOverview = source.slice(commonOverviewStart, commonOverviewEnd)

assert.match(
  qaOverview,
  /qa-regulation-page__overview-card/,
  'QA overview cards must expose the shared overview-card style hook.'
)
assert.match(
  commonOverview,
  /qa-regulation-page__overview-stack/,
  'Common overview must use the same vertical card stack as QA overview.'
)
assert.match(
  commonOverview,
  /qa-regulation-page__overview-card[\s\S]*qa-regulation-page__common-binding-card[\s\S]*qa-regulation-page__common-binding-head/,
  'Common overview must reuse the QA summary card header skeleton.'
)
assert.match(
  commonOverview,
  /data-qa-common-overview-summary[\s\S]*data-qa-common-overview-info[\s\S]*data-qa-common-overview-note/,
  'Common overview must render the QA-style three-section summary, information, and note composition.'
)
assert.match(
  commonOverview,
  /qa-regulation-page__basic-form[\s\S]*qa-regulation-page__basic-grid/,
  'Common overview information must reuse the QA form and field-grid skeleton.'
)

for (const selector of [
  'data-qa-common-overview-summary',
  'data-qa-common-overview-info',
  'data-qa-common-overview-note',
  'data-qa-common-overview-current',
  'data-qa-common-overview-version',
  'data-qa-common-overview-version-select',
  'data-qa-common-overview-version-dropdown',
  'data-qa-common-overview-compose',
  'data-qa-common-overview-refresh',
  'data-qa-common-overview-version-create',
  'data-qa-common-overview-edit',
  'data-qa-common-overview-delete',
  '通用规程套信息',
  '规程信息',
  '备注',
  '套编号',
  '套名称',
  '当前版本',
  '版本组成',
  'data-qa-common-set-create'
]) {
  assert.ok(commonOverview.includes(selector), `Common overview missing ${selector}`)
}

assert.doesNotMatch(
  commonOverview,
  /data-qa-common-overview-list|data-qa-common-set-standard-list|table-key="mes\.qa\.common-regulation-set\.main"|版本信息|通用规程套<\/template>/,
  'Common overview must not keep the old table-based multi-card layout.'
)
assert.doesNotMatch(
  source.slice(source.lastIndexOf('<ContentWrap', commonOverviewStart), commonOverviewEnd),
  /<el-card[\s\S]*data-qa-regulation-common-panel/,
  'Common overview must not be wrapped in an extra parent card.'
)
assert.match(
  source,
  /const commonRegulationOverviewComposeText = computed/,
  'Common overview must expose a dedicated composition view model.'
)
assert.match(
  source,
  /const commonRegulationOverviewVersionText = computed/,
  'Common overview must expose a dedicated version view model.'
)
assert.match(
  source,
  /const commonRegulationOverviewDetailText = computed/,
  'Common overview must expose a dedicated detail view model.'
)
assert.match(
  source,
  /\.qa-regulation-page__overview-stack\s*\{[\s\S]*display:\s*grid;[\s\S]*gap:\s*12px;/,
  'Shared overview cards must use a stable vertical rhythm.'
)
assert.match(
  source,
  /\.qa-regulation-page__overview-card\s*\{/,
  'Shared overview-card styling must be declared.'
)
assert.match(
  commonOverview,
  /qa-regulation-page__overview-note/,
  'Common overview must provide a QA-style explanation/notes card.'
)
assert.match(
  source,
  /v-if="commonRegulationActiveTab === 'items'"[\s\S]*data-qa-common-items/,
  'Common item detail tab must mount only when active, so hidden item validation cannot block overview rendering.'
)
assert.doesNotMatch(
  source,
  /v-show="commonRegulationActiveTab === 'items'"/,
  'Common item detail tab must not stay mounted while hidden.'
)

console.log('GREEN: common regulation overview matches QA overview style skeleton')
