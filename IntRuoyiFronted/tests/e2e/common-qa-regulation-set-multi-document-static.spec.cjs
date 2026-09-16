const fs = require('fs')
const path = require('path')
const assert = require('assert')

const page = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

assert(
  page.includes('data-qa-common-set-document-maintain') &&
    page.includes('selectedCommonRegulationSetVersionDocuments') &&
    page.includes('commonRegulationItemsSummaryText') &&
    page.includes('formatCommonRegulationSetDocumentSource'),
  'RED: common items must keep the formal document-composition source and maintenance entry'
)

assert(
  page.includes('data-qa-common-items-table') &&
    page.includes('commonRegulationItems') &&
    page.includes('sourceDocumentTitle') &&
    page.includes('sourceFileName'),
  'RED: the details area must merge document members into the QA-style item table while retaining source provenance'
)

assert(
  page.includes('data-qa-common-items-parameters') &&
    page.includes('selectedCommonRegulationSetVersionDocuments.length') &&
    page.includes('commonRegulationItems.length'),
  'RED: common item parameters must still show the selected version document and item counts'
)

assert(
  page.includes('createCommonRegulationItemSourceNote') &&
    page.includes('formatCommonRegulationSetDocumentTitle(document)') &&
    page.includes('formatCommonRegulationSetDocumentSource(document)'),
  'RED: each merged item row must carry a formal document source note'
)

console.log('GREEN: common regulation set multi-document source contract is present')
