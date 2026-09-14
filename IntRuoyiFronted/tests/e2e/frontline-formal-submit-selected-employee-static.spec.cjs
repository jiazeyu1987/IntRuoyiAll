const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const viewPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(viewPath, 'utf8')

assert.match(
  source,
  /signatureEmployeeId:\s*context\.actualEmployeeId/,
  '一线正式提交签名员工必须来自当前选择的实际填写员工。'
)
assert.doesNotMatch(
  source,
  /当前登录账号必须是实际填写员工/,
  '前端不得用当前登录账号拦截选择员工的电子签名。'
)
assert.doesNotMatch(
  source,
  /formalContext\.signatureEmployeeId[\s\S]{0,240}currentLoginUserId[\s\S]{0,240}signatureEmployeeId\s*!==\s*currentLoginUserId/,
  '前端正式提交上下文不得要求签名员工等于当前登录账号。'
)
