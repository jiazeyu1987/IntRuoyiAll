const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/qc/template/index.ts'),
  'utf8'
)

const commonItemsStart = page.indexOf('data-qa-common-items')
const qaOverviewStart = page.indexOf('data-qa-regulation-common-binding-control', commonItemsStart)
const commonPanelStart = page.indexOf('data-qa-regulation-common-panel')
const commonWorkbenchStart = page.indexOf(
  'qa-regulation-page__common-set-workbench',
  commonPanelStart
)

assert(commonItemsStart >= 0, 'RED: the common regulation items block is missing')
assert(
  qaOverviewStart > commonItemsStart,
  'RED: the common regulation items block must end before the QA overview block'
)
assert(commonPanelStart >= 0, 'RED: the common regulation panel shell is missing')
assert(
  commonWorkbenchStart > commonPanelStart,
  'RED: the common regulation panel shell must wrap the workbench'
)

const commonItemsBlock = page.slice(commonItemsStart, qaOverviewStart)
const commonPanelShellBlock = page.slice(commonPanelStart, commonWorkbenchStart)

assert(
  !commonPanelShellBlock.includes('#header') ||
    commonPanelShellBlock.includes(`v-if="commonRegulationActiveTab !== 'items'"`),
  'RED: common items must not render an unconditional outer panel title/status block above the parameter area'
)

assert(
  commonItemsBlock.includes('data-qa-common-final-inspection-switch') &&
    commonItemsBlock.includes('data-qa-common-final-not-applicable-reason') &&
    commonItemsBlock.includes('commonFinalInspectionRequired'),
  'RED: common items must expose the QA-style final-inspection parameter area'
)

assert(
  commonItemsBlock.includes('data-qa-common-items-table') &&
    commonItemsBlock.includes('table-key="mes.qa.common-regulation.items.processMethods.v1"') &&
    commonItemsBlock.includes('UnifiedListTemplate') &&
    commonItemsBlock.includes('qaItemsColumns'),
  'RED: common items must use the QA table template and the same column configuration'
)

for (const label of ['工序', '检验项目编码', '检验项目', '首检', '巡检', '检验器具及设备', '操作']) {
  assert(commonItemsBlock.includes(`label="${label}"`), `RED: common items must expose the ${label} column`)
}

assert(
  commonItemsBlock.includes('commonRegulationItems') &&
    commonItemsBlock.includes('selectedCommonRegulationSetVersionPreview'),
  'RED: common items must render the selected set-version snapshot rather than a product QA draft'
)

assert(
  api.includes('finalInspectionApplicable: boolean') &&
    api.includes('inspectionTypeRules: QaInspectionRegulationInspectionTypeRuleVO[]'),
  'RED: the frontend common-member type must carry formal final-inspection fields'
)

console.log('GREEN: common regulation items parity contract is present')
