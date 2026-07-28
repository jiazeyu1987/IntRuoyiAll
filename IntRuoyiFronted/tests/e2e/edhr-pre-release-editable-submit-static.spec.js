const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const batchPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/feedback/index.ts')
const source = fs.readFileSync(pagePath, 'utf8')
const batchSource = fs.readFileSync(batchPagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

const preReleaseEditableCopy = '\u5173\u95ed\u524d\u53ef\u4fee\u6539'
const resubmitEvidenceCopy = '\u91cd\u65b0\u63d0\u4ea4\u5c06\u66f4\u65b0\u63d0\u4ea4\u7b7e\u540d\u8bc1\u636e'
const nonDraftBlockCopy = '\u975e\u8349\u7a3f\u8bb0\u5f55\u4e0d\u5f97\u4fdd\u5b58\u5b57\u6bb5\u53d8\u66f4'
const submittedReadonlyCopy = '\u5f53\u524d\u6267\u884c\u8bb0\u5f55\u5df2\u63d0\u4ea4\uff0c\u4e0d\u80fd\u91cd\u590d\u63d0\u4ea4'

assert.ok(
  api.includes('preReleaseEditable?: boolean') &&
    api.includes('preReleaseEditReason?: string'),
  'execution detail API type must expose backend-projected pre-release editable state and reason.'
)

assert.ok(
  source.includes('preReleaseEditable') &&
    source.includes(preReleaseEditableCopy) &&
    source.includes(resubmitEvidenceCopy),
  'execution page must unlock from backend preReleaseEditable and show pre-release amendment copy.'
)

assert.ok(
  source.includes('execution.value?.preReleaseEditable === true') &&
    !source.includes(nonDraftBlockCopy),
  'field audit save gate must not keep blocking all non-draft records.'
)

assert.ok(
  source.includes('hydrateDraftState(detail)') &&
    !source.includes('BatchRecordCellLinkApi.getPrefill'),
  'execution detail load must hydrate only persisted values and must not call draft-only cell-link prefill.'
)

assert.ok(
  source.includes(submittedReadonlyCopy) && source.includes(preReleaseEditableCopy),
  'non-editable submitted records must keep the submitted read-only warning.'
)

const canOpenTaskBody = batchSource.match(/const canOpenTask = \(row: EdhrBatchExecutionTaskRespVO\) =>([\s\S]*?)const canTakeOverFillTask =/)?.[1] || ''
assert.ok(canOpenTaskBody, 'batch detail page must expose canOpenTask gate.')
assert.ok(
  canOpenTaskBody.includes("hasAllowedTaskAction(row, 'OPEN_FORM')") &&
    !canOpenTaskBody.includes('row.status !== EDHR_BATCH_TASK_STATUS_APPROVED'),
  'approved submitted form cards must be reopenable before close when backend grants OPEN_FORM.'
)

assert.ok(!/mock|\u964d\u7ea7|\u9759\u9ed8\u8df3\u8fc7/.test(source), 'pre-release editable contract must not add mock, fallback, or silent skip.')

console.log('PASS: EDHR pre-close editable submitted form static contract')
