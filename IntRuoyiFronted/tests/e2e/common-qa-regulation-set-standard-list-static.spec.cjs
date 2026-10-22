const fs = require('fs')
const path = require('path')
const assert = require('assert')

const page = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

assert(
  page.includes('data-qa-common-set-selector') &&
    page.includes('data-qa-common-set-switch') &&
    page.includes('v-for="set in commonRegulationSets"') &&
    page.includes('`${set.setCode} / ${set.setName}`'),
  'RED: common regulation sets must keep the formal top selector for switching the selected set'
)

assert(
  page.includes('data-qa-common-overview-summary') &&
    page.includes('data-qa-common-overview-refresh') &&
    page.includes('data-qa-common-set-create') &&
    page.includes('data-qa-common-overview-version-create') &&
    page.includes('data-qa-common-overview-edit') &&
    page.includes('data-qa-common-overview-delete') &&
    page.includes('selectedCommonRegulationSetVersionPreview'),
  'RED: overview must preserve set management operations in the QA-style summary card'
)

assert(
  page.includes('data-qa-common-set-version-detail') &&
    page.includes('data-qa-common-set-member-detail') &&
    page.includes('data-qa-common-set-version-table') &&
    page.includes('data-qa-common-items'),
  'RED: version and member details must remain available outside the overview summary'
)

assert(
  !page.includes('data-qa-common-set-standard-list') &&
    !page.includes('table-key="mes.qa.common-regulation-set.main"') &&
    !page.includes(':data="pagedCommonRegulationSets"'),
  'RED: overview must not retain the old full-width common-set table layout'
)

console.log('GREEN: common regulation set selection and summary actions contract is present')
