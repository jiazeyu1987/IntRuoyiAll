const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/mes/pro/batchrecordreport/index.ts')
const feedbackApi = read('src/api/mes/pro/feedback/index.ts')
const formListPage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')

assert.match(api, /BatchRecordReportCellValueType[\s\S]*'STRING'[\s\S]*'NUMBER'[\s\S]*'DATE'[\s\S]*'DATETIME'[\s\S]*'BOOLEAN'[\s\S]*'SIGNATURE'/)
assert(api.includes('BatchRecordReportCellRuleVO'), 'Report API must expose cell rule contract.')
assert(api.includes('BatchRecordReportCellAttachmentRuleVO'), 'Report API must expose attachment rule contract.')
assert(api.includes('getCellRules'), 'Report API must expose cell rule read endpoint.')
assert(api.includes('saveCellRules'), 'Report API must expose cell rule save endpoint.')
assert(api.includes('/cell-rules'), 'Report API must call the backend cell-rules route.')

assert(feedbackApi.includes('value: string | number | boolean | null'), 'Execution cell values must no longer be string-only.')
assert(feedbackApi.includes('valueType?'), 'Execution values must carry typed valueType.')
assert(feedbackApi.includes("'SIGNATURE'"), 'Execution values must preserve SIGNATURE valueType.')
assert(feedbackApi.includes('valueDisplay?'), 'Execution values must carry typed display value.')
assert(feedbackApi.includes('valueHash?'), 'Execution values must carry hash for typed projection.')
assert(feedbackApi.includes('unit?'), 'Execution values must carry unit when configured.')

assert(formListPage.includes('规则'), 'Batch record form list must expose the cell rule action.')
assert(formListPage.includes("openTemplateAction(selectedReport, 'cellRules')"), 'Cell rule action must route with selected report context.')
assert(formListPage.includes('BatchRecordReportApi.getCellRules'), 'Preview must load cell rules for readonly rendering.')
assert(formListPage.includes('BatchRecordReportApi.getSignatureCellMarkers'), 'Preview must load signature markers together with rules.')
assert(formListPage.includes('EdhrExecutionReadonlyForm'), 'Preview must render the real readonly template component.')
assert(!formListPage.includes('catch {}'), 'Template rule entry must not silently swallow errors.')

assert(executionPage.includes("componentKind === 'number'") && executionPage.includes('field.unit'), 'Execution page must render typed number input with unit.')
assert(executionPage.includes("field.componentKind === 'checkbox'"), 'Execution page must render BOOLEAN as checkbox.')
assert(executionPage.includes("field.componentKind === 'datetime'"), 'Execution page must render DATETIME with datetime picker.')
assert(executionPage.includes('resolveRuleConstraintValidation'), 'Execution page must validate typed constraints before saving/signing.')
assert(executionPage.includes("field.componentKind === 'signature'"), 'Execution page must keep SIGNATURE cells readonly and out of normal field audit input.')
assert(!executionPage.includes("componentKind === 'switch'"), 'Execution page must not keep the old switch-only boolean branch.')

assert(readonlyForm.includes('valueDisplay'), 'Readonly form must render typed valueDisplay.')
assert(readonlyForm.includes('valueType'), 'Readonly form must inspect typed valueType.')
assert(readonlyForm.includes('unit'), 'Readonly form must append configured unit.')
assert(readonlyForm.includes("valueType === 'BOOLEAN'"), 'Readonly form must render boolean cells explicitly.')
assert(
  readonlyForm.includes('const actor = signature.actorName') &&
    readonlyForm.includes('formatSignatureTime(signature.signedAt)') &&
    readonlyForm.includes('return `${actor}\\n${formatSignatureTime(signature.signedAt)}`'),
  'Readonly form must render signature cells as signer plus signed time.'
)

console.log('PASS: eDHR cell rules static contract')
