const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduler-workbench/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  source.includes("checkPermi(['mes:pro-scheduler-workbench:update'])"),
  '排产员工作台设置写入口必须绑定 update 权限'
)
assert.ok(
  !source.includes("checkPermi(['mes:pro-scheduler-workbench:smoke-test'])"),
  '排产员工作台前端不得再暴露 smoke-test 权限控制入口'
)
assert.ok(
  source.includes(':disabled="!canUpdateSettings"'),
  '无 update 权限时班次/策略输入必须不可编辑'
)
assert.ok(
  !source.includes('v-if="canOperateSmokeTest"'),
  '前端隐藏冒烟测试后不得保留启动/停止按钮可见性判断'
)

console.log('PASS: MES scheduler workbench frontend permission contract')
