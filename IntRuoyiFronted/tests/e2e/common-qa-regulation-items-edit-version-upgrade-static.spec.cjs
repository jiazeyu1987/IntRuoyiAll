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
assert(commonItemsStart >= 0, 'RED: common regulation items block is missing')
assert(qaOverviewStart > commonItemsStart, 'RED: common regulation items block boundary is missing')

const commonItemsBlock = page.slice(commonItemsStart, qaOverviewStart)

assert(
  commonItemsBlock.includes('data-qa-common-items-save-version') &&
    commonItemsBlock.includes('saveCommonRegulationItemsVersion'),
  'RED: common items must expose a formal save/upgrade entry'
)

for (const editable of [
  'v-model="row.processName"',
  'v-model="row.processCode"',
  'v-model="row.itemCode"',
  'v-model="row.itemName"',
  'v-model="row.firstInspectionEnabled"',
  'v-model="row.patrolInspectionEnabled"',
  'v-model="row.standardText"',
  'v-model="row.inspectionMethod"',
  'v-model="row.inspectionTool"',
  'v-model="row.resultType"',
  'v-model="row.critical"',
  'v-model="row.failureRule"'
]) {
  assert(commonItemsBlock.includes(editable), `RED: common items must edit with ${editable}`)
}

assert(
  !commonItemsBlock.includes('<el-tag size="small" type="info" effect="plain">只读</el-tag>'),
  'RED: common items operation column must not remain read-only only'
)

assert(
  page.includes('ownerModule:') &&
    page.includes(`'MES_QA_COMMON'`) &&
    page.includes('QcTemplateApi.upgradeCommonRegulationSetItems'),
  'RED: common item save must call the formal common-set item upgrade API'
)

assert(
  page.includes('sourceNoteRaw: item.sourceNote ||') &&
    page.includes('sourceNote: item.sourceNoteRaw.trim() || undefined'),
  'RED: common item save must persist the original source note, not the display-only source summary'
)

assert(
  page.includes('sourceCommonRegulationVersionId: memberVersionId') &&
    api.includes('sourceCommonRegulationVersionId: number'),
  'RED: common item upgrade payload must identify the source member version being upgraded'
)

assert(
  api.includes("ownerModule?: QaInspectionRegulationOwnerModule") &&
    api.includes("QaInspectionRegulationOwnerModule = 'MES_QA' | 'MES_QA_COMMON'") &&
    api.includes('QaCommonRegulationSetItemsUpgradeReqVO') &&
    api.includes('/common-set-versions/upgrade-items'),
  'RED: frontend API contract must preserve MES_QA_COMMON and expose the common-set item upgrade endpoint'
)

console.log('GREEN: common regulation items edit and version-upgrade contract is present')
