const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/feedback/index.ts')
const source = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

for (const token of [
  'edhr-fill-workspace__submit-action',
  "v-hasPermi=\"['mes:pro-batch-record-execution:update']\"",
  ':loading="submitLoading"',
  ':disabled="hasSlotContextBlockers || hasPendingFieldChanges"',
  '@click="openSubmitDialog"'
]) {
  assert.ok(source.includes(token), `填写工作区提交入口缺少契约片段：${token}`)
}

assert.ok(/>\s*提交执行\s*<\/el-button>/.test(source), '填写工作区提交入口必须显示“提交执行”。')

assert.ok(
  source.indexOf('edhr-fill-workspace__submit-action') > source.indexOf('edhr-fill-workspace__primary-action'),
  '提交执行入口应跟随保存入口，确保填表工作区可完成保存后提交。'
)

assert.ok(
  api.includes('submitEdhrExecution: async (data: ProFeedbackEdhrSubmitReqVO)') &&
    api.includes("headers: { 'Content-Type': 'application/json' }"),
  '提交 eDHR 执行请求必须使用后端支持的 application/json Content-Type。'
)

assert(!/mock|降级|静默跳过/.test(source), '填写工作区提交入口不得引入 mock、降级或静默跳过。')

console.log('PASS: EDHR execution fill workspace submit static contract')
