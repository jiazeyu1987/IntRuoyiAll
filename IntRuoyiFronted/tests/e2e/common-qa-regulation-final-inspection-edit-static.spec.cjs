const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

const commonItemsStart = page.indexOf('data-qa-common-items')
const qaOverviewStart = page.indexOf('data-qa-regulation-common-binding-control', commonItemsStart)
assert(commonItemsStart >= 0, 'RED: common regulation items block is missing')
assert(qaOverviewStart > commonItemsStart, 'RED: common regulation items block boundary is missing')

const commonItemsBlock = page.slice(commonItemsStart, qaOverviewStart)
const commonPayloadStart = page.indexOf('const buildCommonRegulationItemsUpgradePayload')
const qaPayloadStart = page.indexOf('const buildQaRegulationSavePayload', commonPayloadStart)
assert(
  commonPayloadStart >= 0 && qaPayloadStart > commonPayloadStart,
  'RED: common item upgrade payload builder is missing'
)
const commonPayloadBlock = page.slice(commonPayloadStart, qaPayloadStart)
const switchStart = commonItemsBlock.indexOf('<el-switch')
const switchEnd = commonItemsBlock.indexOf('/>', switchStart)
assert(switchStart >= 0 && switchEnd > switchStart, 'RED: common final-inspection switch is missing')
const commonSwitchBlock = commonItemsBlock.slice(switchStart, switchEnd)

assert(
  commonSwitchBlock.includes('v-model="commonFinalInspectionRequired"'),
  'RED: common final-inspection switch must be editable with v-model'
)
assert(
  !commonSwitchBlock.includes(':model-value="commonFinalInspectionRequired"') &&
    !commonSwitchBlock.includes('disabled'),
  'RED: common final-inspection switch must not remain disabled'
)
assert(
  commonItemsBlock.includes('v-model="commonFinalInspectionNotApplicableReason"') &&
    commonItemsBlock.includes('placeholder="填写末检不适用的正式依据"') &&
    !commonItemsBlock.includes(':model-value="commonFinalInspectionReasonText"'),
  'RED: common final-inspection not-applicable reason must be editable'
)
assert(
  page.includes('const applyCommonFinalInspectionRequired') &&
    page.includes('const commonFinalInspectionRequired = computed<boolean>({') &&
    page.includes('const commonFinalInspectionNotApplicableReason = computed<string>({'),
  'RED: common final-inspection state must have setters'
)
assert(
  page.includes('syncCommonFinalInspectionToRows') &&
    page.includes('row.memberFinalInspectionApplicable = required') &&
    page.includes('required,') &&
    page.includes('resetCommonFinalInspectionDraft()'),
  'RED: common final-inspection edits must update member rows and reset when version changes'
)
assert(
  commonPayloadBlock.includes('const finalInspectionApplicable = commonFinalInspectionEdited.value') &&
    commonPayloadBlock.includes('? commonFinalInspectionRequired.value') &&
    commonPayloadBlock.includes(': requireCommonMemberFinalInspectionApplicable(member)') &&
    commonPayloadBlock.includes('finalInspectionApplicable,') &&
    commonPayloadBlock.includes('commonFinalInspectionEdited.value ? finalInspectionApplicable : undefined') &&
    commonPayloadBlock.includes('buildCommonRegulationSaveProcesses(member, memberRows, finalInspectionApplicable)'),
  'RED: common item save payload must use edited final-inspection state only when the user changed it'
)

console.log('GREEN: common final-inspection edit contract is present')

