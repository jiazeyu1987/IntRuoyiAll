const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const formPath = path.join(
  workspaceRoot,
  'yudao-ui-admin-vue3/src/views/mes/pro/process/ProProcessForm.vue'
)

assert(fs.existsSync(formPath), `required file missing: ${formPath}`)

const formSource = fs.readFileSync(formPath, 'utf8')

assert.match(
  formSource,
  /AutoCodeRecordApi\.generateAutoCode\(\s*MesAutoCodeRuleCode\.PRO_PROCESS_CODE\s*,\s*'ER'\s*\)/,
  '工序编码生成必须为 PRO_PROCESS_CODE 规则传入 ER 前缀，避免生成 EDHR_PROC_ 长编码。'
)

assert(!formSource.includes('EDHR_PROC_'), '工序表单不得内置 EDHR_PROC_ 长前缀。')
assert(!formSource.includes('Math.random'), '工序编码不得由前端随机兜底生成。')
assert(!formSource.includes('crypto.randomUUID'), '工序编码不得由前端 UUID 兜底生成。')

console.log('PASS: MES pro process code generation uses ER prefix')
