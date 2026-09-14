const fs = require('fs')
const path = require('path')
const assert = require('assert')

const page = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

assert(
  page.includes('data-qa-common-set-document-composition') &&
    page.includes(':data="selectedCommonRegulationSetVersionDocuments"') &&
    page.includes('文档组成') &&
    page.includes('formatCommonRegulationSetDocumentSource'),
  'RED: a common regulation set version must expose a document-composition table, not only a generic member list'
)

assert(
  page.includes('data-qa-common-set-document-detail-tabs') &&
    page.includes('name="byDocument"') &&
    page.includes('name="mergedProcesses"') &&
    page.includes('commonRegulationSetDetailActiveTab'),
  'RED: the details area must provide by-document and merged-process views'
)

assert(
  page.includes('data-qa-common-set-document-card') &&
    page.includes('selectedCommonRegulationSetVersionDocuments') &&
    page.includes('formatCommonRegulationSetDocumentTitle'),
  'RED: by-document view must group processes and inspection items under each source Word regulation'
)

assert(
  page.includes('data-qa-common-set-merged-process-table') &&
    page.includes(':data="selectedCommonRegulationSetMergedRows"') &&
    page.includes('来源文档') &&
    page.includes('selectedCommonRegulationSetMergedRows'),
  'RED: merged-process view must list the final PQC sequence with each row linked to its source document'
)

console.log('GREEN: common regulation set multi-document display contract is present')
