const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')

function readUtf8(absolutePath) {
  assert.ok(fs.existsSync(absolutePath), `missing required file: ${absolutePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function readWorkspace(relativePath) {
  return readUtf8(path.join(workspaceRoot, relativePath))
}

function assertSeededHidden(sql, routePath, label) {
  const escapedPath = routePath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  assert.match(
    sql,
    new RegExp(
      `'${escapedPath}'[\\s\\S]{0,260}0,\\s*b'0',\\s*b'1',\\s*b'[01]'`
    ),
    `${label} must seed ${routePath} as hidden from sidebar`
  )
}

const hiddenSidebarPaths = [
  'controlled-file/distribution',
  'controlled-file/training',
  'controlled-file/training-mine',
  'controlled-file/print-template'
]

const baseSchema = readWorkspace('ruoyi-vue-pro/sql/mysql/20260513_dcc_base_schema.sql')
const governanceSplit = readWorkspace(
  'ruoyi-vue-pro/sql/mysql/20260515_dcc_governance_split_menu.sql'
)
const trainingClosedLoop = readWorkspace(
  'ruoyi-vue-pro/sql/mysql/20260516_dcc_training_closed_loop_menu.sql'
)
const printTemplate = readWorkspace('ruoyi-vue-pro/sql/mysql/20260527_dcc_approval_print_template.sql')
const trainingMineRestore = readWorkspace(
  'ruoyi-vue-pro/sql/mysql/20260529_dcc_training_mine_menu_restore.sql'
)
const runtimePatch = readWorkspace(
  'ruoyi-vue-pro/sql/mysql/20260714_dcc_hide_legacy_menu_tabs_admin_only.sql'
)
const remainingRouter = readUtf8(path.join(repoRoot, 'src/router/modules/remaining.ts'))

assert.match(baseSchema, /SELECT 6805, '上传审批'/, 'base DCC route menu must be 上传审批')
assert.doesNotMatch(baseSchema, /SELECT 6805, '流程路线'/, 'base DCC route menu must not regress to 流程路线')

assertSeededHidden(governanceSplit, 'controlled-file/distribution', 'governance split SQL')
assertSeededHidden(governanceSplit, 'controlled-file/training', 'governance split SQL')
assertSeededHidden(trainingClosedLoop, 'controlled-file/training-mine', 'training closed-loop SQL')
assertSeededHidden(printTemplate, 'controlled-file/print-template', 'print template SQL')
assertSeededHidden(trainingMineRestore, 'controlled-file/training-mine', 'training mine restore SQL')
assertSeededHidden(baseSchema, 'controlled-file/training-mine', 'base schema SQL')
assertSeededHidden(baseSchema, 'controlled-file/print-template', 'base schema SQL')

for (const routePath of hiddenSidebarPaths) {
  assert.ok(runtimePatch.includes(`'${routePath}'`), `runtime patch must target ${routePath}`)
}
assert.match(runtimePatch, /SET\s+`name`\s*=\s*'上传审批'/, 'runtime patch must keep upload approval label')
assert.match(runtimePatch, /`visible`\s*=\s*b'0'/, 'runtime patch must hide retired sidebar entries')

for (const fragment of [
  "path: 'controlled-file/distribution'",
  "title: '分发规则'",
  "path: 'controlled-file/training'",
  "title: '培训规则'",
  "path: 'controlled-file/training-mine'",
  "title: '我的培训'",
  "path: 'controlled-file/print-template'",
  "title: '模板配置'"
]) {
  assert.ok(remainingRouter.includes(fragment), `hidden compatibility route missing: ${fragment}`)
}

console.log('PASS: DCC upload approval menu and admin-only visibility static contract')
