import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import assert from 'node:assert/strict'

const root = resolve(process.cwd())
const api = readFileSync(resolve(root, 'src/api/approval-center/index.ts'), 'utf8')
const page = readFileSync(resolve(root, 'src/views/approval-center/index.vue'), 'utf8')
const feedbackPage = readFileSync(resolve(root, 'src/views/mes/pro/feedback/index.vue'), 'utf8')

assert.match(api, /ApprovalModuleCode[\s\S]*'MES_FEEDBACK'/,
  'approval center API type must include MES_FEEDBACK module code')
assert.match(page, /supportedModuleCodes[\s\S]*'MES_FEEDBACK'/,
  'approval center route query validation must accept MES_FEEDBACK')
assert.match(page, /row\.detailRoute/,
  'approval center must keep module formal detail route handoff')
assert.match(feedbackPage, /route\.query\.feedbackId/,
  'MES feedback formal page must support feedbackId query from unified approval center')
assert.doesNotMatch(page, /ProFeedbackApi\.approveFeedback|approveFeedback\(/,
  'approval center must not execute MES feedback approval actions')

console.log('approval-center phase8 MES feedback static contract passed')
