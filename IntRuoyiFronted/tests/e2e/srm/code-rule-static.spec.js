const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const backendRoot = path.resolve(frontendRoot, '../ruoyi-vue-pro')

const read = (relativePath) => {
  const absolutePath = path.resolve(frontendRoot, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const readBackend = (relativePath) => {
  const absolutePath = path.resolve(backendRoot, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist in backend worktree`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const api = read('src/api/srm/code-rule/index.ts')
const page = read('src/views/srm/code-rule/index.vue')
const sql = readBackend('sql/mysql/20260618_srm_d7_1_code_rule_baseline.sql')

for (const fragment of [
  '/srm/code-rule/page',
  '/srm/code-rule/get',
  '/srm/code-rule/create',
  '/srm/code-rule/update',
  '/srm/code-rule/enable',
  'targetForm',
  'dateSegmentEnabled',
  'serialWidth',
  'step',
  'minSerial',
  'maxSerial',
  'separator',
  'srmCodeRuleTargetFormOptions',
  'PROCUREMENT_PLAN',
  'EXPERT_DRAW_APPLICATION'
]) {
  assert(api.includes(fragment), `SRM code-rule API must include ${fragment}`)
}

for (const fragment of [
  "defineOptions({ name: 'SrmCodeRule' })",
  "v-hasPermi=\"['srm:code-rule:create']\"",
  "v-hasPermi=\"['srm:code-rule:update']\"",
  "v-hasPermi=\"['srm:code-rule:enable']\"",
  '目标表单',
  '请选择目标表单',
  'srmCodeRuleTargetFormOptions',
  '规则编码',
  '日期段',
  '流水步长',
  '最小流水',
  '分隔符',
  'getList',
  'message.error'
]) {
  assert(page.includes(fragment), `SRM code-rule page must include ${fragment}`)
}

for (const forbidden of [
  /catch\s*\{\s*\}/,
  /catch\s*\([^)]*\)\s*\{\s*\}/,
  /console\.log\([^)]*error/i
]) {
  assert(!forbidden.test(page), `SRM code-rule page must not contain ${forbidden}`)
}

for (const fragment of [
  "`path` = '/srm'",
  "`path` = 'base'",
  "`path` = 'code-rule'",
  "`component` = 'srm/code-rule/index'",
  "`component_name` = 'SrmCodeRule'",
  "'srm:code-rule:query'",
  "'srm:code-rule:create'",
  "'srm:code-rule:update'",
  "'srm:code-rule:enable'"
]) {
  assert(sql.includes(fragment), `SRM code-rule SQL must include ${fragment}`)
}

console.log('PASS: SRM D7-1 code-rule static contract')
