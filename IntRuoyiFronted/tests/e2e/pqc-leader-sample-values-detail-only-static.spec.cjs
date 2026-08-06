const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const reportMarker = 'data-team-leader-report-workbench'
const reportStart = page.indexOf(reportMarker)
assert.ok(reportStart >= 0, 'PQC management list must keep a stable report workbench marker.')
const tableStart = page.indexOf('<el-table', reportStart)
const tableEnd = page.indexOf('</el-table>', tableStart)
assert.ok(tableStart > reportStart && tableEnd > tableStart, 'PQC management table block must be locatable.')
const tableBlock = page.slice(tableStart, tableEnd)
const reportTemplateStart = page.lastIndexOf('<UnifiedListTemplate', tableStart)
const reportTemplateEnd = page.indexOf('</UnifiedListTemplate>', tableEnd)
assert.ok(
  reportTemplateStart > reportStart && reportTemplateEnd > tableEnd,
  'PQC management list must be wrapped by the standard UnifiedListTemplate.'
)
const reportTemplateBlock = page.slice(reportTemplateStart, reportTemplateEnd)
assert.match(
  reportTemplateBlock,
  /table-key="mes\.processPool\.teamLeader\.submissions"[\s\S]*:show-multi-filter="true"/,
  'PQC management list must use the standard list template with multi-filter support.'
)

const pqcColumnsStart = page.indexOf('const pqcSubmissionDefaultColumns')
const pqcColumnsEnd = page.indexOf(']\nconst productionSubmissionColumnControl', pqcColumnsStart)
assert.ok(pqcColumnsStart >= 0 && pqcColumnsEnd > pqcColumnsStart, 'PQC default columns block must be locatable.')
const pqcColumnsBlock = page.slice(pqcColumnsStart, pqcColumnsEnd)

assert.doesNotMatch(
  tableBlock,
  /label="逐件\/样本值"|prop="pieceSampleValues"|data-pqc-leader-piece-sample-values/,
  'PQC management list must not render the noisy sample-values column.'
)
assert.doesNotMatch(
  pqcColumnsBlock,
  /key:\s*'pieceSampleValues'|label:\s*'逐件\/样本值'/,
  'PQC column settings must not expose sample values as a list column.'
)
assert.doesNotMatch(
  pqcColumnsBlock,
  /key:\s*'pqcSubmissionContent'|key:\s*'auditCopyStatus'|key:\s*'processInspectionAggregation'|key:\s*'submissionReviewStatus'/,
  'PQC column settings must not expose legacy merged content, audit copy, aggregation, or review columns.'
)
assert.match(
  pqcColumnsBlock,
  /key:\s*'workOrder'[\s\S]*label:\s*'生产工单'/,
  'PQC column settings must explicitly keep production work order as a structured list column.'
)
assert.doesNotMatch(
  tableBlock,
  /label="PQC提交内容"|data-pqc-leader-submission-content|label="过程检验汇集"|data-pqc-process-inspection-aggregation/,
  'PQC management table must not render merged content or process aggregation columns.'
)
assert.match(
  page,
  /const isSubmissionColumnVisible = \(key: string\) =>\s*\n\s*submissionColumns\.value\.some\(\(column\) => column\.key === key\)\s*\n\s*&& activeSubmissionColumnControl\.value\.isColumnVisible\(key\)/,
  'Submission column visibility must not default missing role-specific columns to visible.'
)
assert.doesNotMatch(
  tableBlock,
  /label="审核副本"|prop="auditCopyStatus"|label="复核判定"|prop="submissionReviewStatus"/,
  'Audit copy and review judgement must not render as list columns.'
)

assert.match(
  page,
  /const activePqcModuleTab = ref<'personnel' \| 'management' \| 'dashboard' \| 'detail'>\('personnel'\)/,
  'PQC module tabs must include an in-page detail tab state.'
)
assert.match(
  page,
  /<el-tab-pane[\s\S]*label="详情"[\s\S]*name="detail"[\s\S]*data-pqc-leader-module-tab-detail/,
  'PQC module tabs must expose a Detail tab after a row detail is opened.'
)

const detailTabMarkerStart = page.indexOf('data-pqc-leader-detail-tab')
assert.ok(detailTabMarkerStart >= 0, 'PQC detail must render as an in-page tab, not only as a drawer.')
const detailTabStart = page.lastIndexOf('<ContentWrap', detailTabMarkerStart)
const detailTabEnd = page.indexOf('</ContentWrap>', detailTabMarkerStart)
assert.ok(detailTabStart >= 0 && detailTabEnd > detailTabStart, 'PQC detail tab content block must be locatable.')
const detailTabBlock = page.slice(detailTabStart, detailTabEnd)

assert.match(
  detailTabBlock,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.processPool\.teamLeader\.pqcSubmissionDetailItems"[\s\S]*data-pqc-leader-item-snapshot-table/,
  'PQC detail item table must be wrapped by the standard list template.'
)
assert.match(
  detailTabBlock,
  /label="样本值"[\s\S]*data-pqc-leader-detail-sample-values[\s\S]*formatPqcSnapshotSampleValues\(row\)/,
  'PQC detail tab must still show sample values in the item snapshot table.'
)
assert.doesNotMatch(
  detailTabBlock,
  /label="结构化报工内容"|resolveStructuredPayloadItems\(detail\.originalPayloadJson\)/,
  'PQC detail tab must not show the structured reporting payload block.'
)
assert.doesNotMatch(
  detailTabBlock,
  /label="原始提交内容"|data-pqc-submission-original-payload|detail\.originalPayloadJson \|\| '--'/,
  'PQC detail tab must not show the raw original payload block.'
)
assert.match(
  detailTabBlock,
  /class="[^"]*team-leader-workbench__detail-descriptions[^"]*"[\s\S]*label-width="400px"|label-width="400px"[\s\S]*class="[^"]*team-leader-workbench__detail-descriptions[^"]*"/,
  'PQC detail tab descriptions must use the widened-detail class and 400px label-width.'
)

const drawerMarkerStart = page.indexOf('data-team-leader-submission-detail-drawer')
assert.ok(drawerMarkerStart >= 0, 'Production/non-tab detail drawer can remain for non-PQC module contexts.')
const drawerStart = page.lastIndexOf('<el-drawer', drawerMarkerStart)
const drawerOpenEnd = page.indexOf('>', drawerMarkerStart)
const drawerOpenTag = page.slice(drawerStart, drawerOpenEnd + 1)
assert.match(
  drawerOpenTag,
  /v-if="!showPqcDetailAsTab"/,
  'The drawer must be disabled when PQC detail is shown as a module tab.'
)

const openDetailStart = page.indexOf('const openDetail = async')
const openDetailEnd = page.indexOf('const openReview', openDetailStart)
assert.ok(openDetailStart >= 0 && openDetailEnd > openDetailStart, 'openDetail function must be locatable.')
const openDetailBlock = page.slice(openDetailStart, openDetailEnd)
assert.match(
  openDetailBlock,
  /if \(activeLeaderTab\.value === 'PQC' && showPqcModuleTabs\.value\)[\s\S]*activePqcModuleTab\.value = 'detail'[\s\S]*return/,
  'PQC detail clicks must route to the in-page detail tab instead of opening the drawer.'
)

assert.match(
  page,
  /\.team-leader-workbench__detail-descriptions\s*:deep\(\.el-descriptions__label\)\s*\{[\s\S]*width:\s*400px[\s\S]*min-width:\s*400px[\s\S]*white-space:\s*nowrap/,
  'PQC detail label column must be enforced by scoped deep CSS so the real bordered table cannot compress it.'
)

console.log('PASS: PQC detail opens in a standard-list tab and keeps sample values detail-only')
