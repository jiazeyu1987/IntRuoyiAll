const fs = require('fs')
const path = require('path')
const assert = require('assert')

const page = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

assert(
  page.includes('data-qa-common-set-standard-list') &&
    page.includes('table-key="mes.qa.common-regulation-set.main"') &&
    page.includes(':data="pagedCommonRegulationSets"'),
  'RED: common regulation sets must use the project UnifiedListTemplate as a full-width main list'
)

assert(
  page.includes('data-qa-common-set-version-detail') &&
    page.includes('data-qa-common-set-member-detail') &&
    page.includes('selectedCommonRegulationSetVersionPreview'),
  'RED: the selected set must reveal version and member details in order below the main list'
)

assert(
  page.includes('highlight-current-row') &&
    page.includes('commonRegulationSetCurrentVersion') &&
    page.includes('commonRegulationSetMemberCount'),
  'RED: the standard list must expose selection, current version, and member-count summaries'
)

assert(
  (page.match(/:fixed="isCommonRegulationSetWideViewport \? 'right' : false"/g) || []).length >= 2 &&
    page.includes('.qa-regulation-page__common-set-list :deep(.unified-list-template__toolbar-actions)') &&
    page.includes('.qa-regulation-page__common-set-list :deep(.unified-list-template__toolbar)') &&
    page.includes('flex-wrap: wrap'),
  'RED: mobile layout must release fixed action columns and wrap the standard-list toolbar'
)

console.log('GREEN: common regulation set standard list contract is present')
