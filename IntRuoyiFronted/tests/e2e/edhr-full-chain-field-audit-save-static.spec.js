const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const scriptPath = path.resolve(process.cwd(), 'tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js')
const page = fs.readFileSync(pagePath, 'utf8')
const script = fs.readFileSync(scriptPath, 'utf8')

assert(
  page.includes('@click="handleSaveFieldAuditChanges"') && page.includes('确认保存'),
  '字段审计弹窗必须通过“确认保存”按钮触发真实保存函数。'
)

assert(
  script.includes("dialog.getByPlaceholder('请输入当前账号密码')") &&
    script.includes('/确\\s*认\\s*保\\s*存/'),
  '完整链路脚本必须按字段审计弹窗的真实密码输入框和确认保存按钮操作。'
)

assert(
  /const saveResponsePromise = waitForApiResponse\(page, ENDPOINTS\.fieldAuditSave, '字段审计保存', 'PUT'\)[\s\S]*await confirmButton\.click\(\)/.test(script),
  '完整链路脚本必须在点击字段审计确认保存按钮时等待真实保存接口响应。'
)

console.log('PASS: eDHR full-chain field audit save static contract')
