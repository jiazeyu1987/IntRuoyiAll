const fs = require('fs')
const path = require('path')
const assert = require('assert')

const page = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

assert(
  page.includes('data-qa-common-layout-header') &&
    page.includes('class="qa-regulation-page__header"') &&
    page.includes('data-qa-common-set-selector') &&
    page.includes('class="qa-regulation-page__form qa-regulation-page__project-form"'),
  'RED: the common regulation header must reuse the QA header and selector layout'
)

assert(
  page.includes('data-qa-common-current-published-version') &&
    page.includes('class="qa-regulation-page__published-version"') &&
    page.includes('data-qa-common-version-publish') &&
    page.includes('class="qa-regulation-page__version-publish"'),
  'RED: the common regulation header must mirror QA published-version and action regions'
)

for (const selector of [
  'data-qa-common-version-dropdown',
  'data-qa-common-effective-date',
  'data-qa-common-selected-version-status',
  'data-qa-common-save-draft',
  'data-qa-common-publish',
  'data-qa-regulation-common-word-import'
]) {
  assert(page.includes(selector), `RED: common regulation header missing ${selector}`)
}

const detailTabs = page.match(/data-qa-common-detail-tabs[\s\S]*?<\/el-tabs>/)?.[0] || ''
assert(
  detailTabs.includes('label="总览"') &&
    detailTabs.includes('label="检验项目"') &&
    detailTabs.includes('label="版本记录"') &&
    page.includes("type CommonRegulationTabName = 'overview' | 'items' | 'versions'"),
  'RED: common regulation details must use QA-style secondary tabs'
)

assert(
  page.includes('data-qa-common-overview') &&
    page.includes('data-qa-common-items') &&
    page.includes('data-qa-common-versions') &&
    page.includes("commonRegulationActiveTab === 'overview'") &&
    page.includes("commonRegulationActiveTab === 'items'") &&
    page.includes("commonRegulationActiveTab === 'versions'"),
  'RED: common regulation overview, inspection items, and versions must render as separate detail sections'
)

assert(
  page.includes('openCommonRegulationDraftDialog') &&
    page.includes('openCommonRegulationPublishDialog') &&
    !page.includes('class="qa-regulation-page__common-workspace-header"'),
  'RED: common version actions must use formal dialogs and the old custom header must be removed'
)

console.log('GREEN: common regulation layout matches the QA page skeleton')
