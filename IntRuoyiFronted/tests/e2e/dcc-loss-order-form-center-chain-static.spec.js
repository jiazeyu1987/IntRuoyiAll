const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const frontendRoot = process.cwd()
const repoRoot = path.resolve(frontendRoot, '..')

function read(relativePath, root = frontendRoot) {
  const file = path.join(root, relativePath)
  assert.ok(fs.existsSync(file), `Missing expected file: ${path.relative(frontendRoot, file)}`)
  return fs.readFileSync(file, 'utf8')
}

function assertIncludes(content, expected, message = expected) {
  assert.ok(content.includes(expected), message)
}

function assertNotIncludes(content, unexpected, message = unexpected) {
  assert.equal(content.includes(unexpected), false, message)
}

function assertRegex(content, regex, message = regex.toString()) {
  assert.ok(regex.test(content), message)
}

function nodeCheck(relativePath, root = frontendRoot) {
  const file = path.join(root, relativePath)
  const child = spawnSync(process.execPath, ['--check', file], {
    cwd: root,
    encoding: 'utf8'
  })
  assert.equal(child.status, 0, `node --check failed for ${relativePath}: ${child.stderr || child.stdout}`)
}

const packageJson = JSON.parse(read('package.json'))
assert.equal(
  packageJson.scripts['e2e:dcc:loss-order-form-center:static'],
  'node tests/e2e/dcc-loss-order-form-center-chain-static.spec.js'
)
assert.equal(
  packageJson.scripts['e2e:dcc:loss-order-form-center:check'],
  'node tests/e2e/dcc-loss-order-form-center-chain-static.spec.js'
)
assert.equal(
  packageJson.scripts['e2e:dcc:loss-order-form-center'],
  'node ../scripts/dcc-loss-order-form-center-e2e-chain.mjs'
)

const runner = read('scripts/dcc-loss-order-form-center-e2e-chain.mjs', repoRoot)
const realE2e = read('doc/tasks/20260719-loss-order-form-center-e2e/loss-order-form-center-real-e2e.mjs', repoRoot)
const uploadPage = read('src/views/dcc/controlled-file/upload/index.vue')
const actionPanel = read('src/views/form-center/business-action/ActionFormPanel.vue')
const taskDoc = read('doc/tasks/20260719-loss-order-form-center-test-chain/task.md', repoRoot)
const executionLog = read('doc/tasks/20260719-loss-order-form-center-test-chain/execution-log.md', repoRoot)

nodeCheck('scripts/dcc-loss-order-form-center-e2e-chain.mjs', repoRoot)
nodeCheck('doc/tasks/20260719-loss-order-form-center-e2e/loss-order-form-center-real-e2e.mjs', repoRoot)

assertIncludes(runner, 'scripts/preflight/login-preflight.mjs', 'chain must run official login preflight')
assertIncludes(runner, "process.platform === 'win32' ? 'where.exe' : 'sh'", 'chain must check npx availability correctly on Windows')
assertIncludes(runner, "['npx']", 'chain must look up npx on Windows')
assertIncludes(runner, '/dcc/controlled-file/upload', 'chain must preflight the real DCC upload entry')
assertIncludes(runner, '/approval-center/todo', 'chain must preflight approval role todo entry')
assertIncludes(runner, 'DCC_LOSS_ORDER_E2E_SOURCE_DOC', 'chain must allow explicit source document input')
assertIncludes(runner, 'DCC_LOSS_ORDER_E2E_TASK_DIR', 'chain must isolate real E2E artifacts in the current task')
assertIncludes(runner, 'C:\\\\Users\\\\BJB110\\\\Desktop\\\\文档\\\\损耗单.doc', 'chain must default to the user-provided document')
assertIncludes(runner, 'D0CF11E0A1B11AE1', 'chain must verify real legacy DOC OLE header')
assertIncludes(runner, 'loss-order-form-center-real-e2e.mjs', 'chain must call the real browser E2E script')
assertIncludes(runner, 'ACT_RU_TASK', 'chain must verify no active BPM tasks remain')
assertIncludes(runner, 'ACT_HI_PROCINST', 'chain must verify historic BPM process completion')
assertIncludes(runner, 'bpm_form_action_instance', 'chain must verify form action instance end-state')
assertIncludes(runner, 'bpm_form_effect_execution', 'chain must verify form effect execution end-state')
assertIncludes(runner, 'bpm_form_task_permission', 'chain must verify form-derived task permission lifecycle')
assertIncludes(runner, 'dcc_controlled_file', 'chain must verify DCC file activation')
assertIncludes(runner, 'SELECT COUNT(*) FROM ACT_RU_TASK', 'database verification must be read-only')
assertIncludes(runner, "fileStatus !== 'ACTIVE'", 'chain must assert active DCC status')
assertIncludes(runner, "actionStatus !== 'EFFECTIVE'", 'chain must assert effective form instance')
assertIncludes(runner, "effectStatus !== 'APPLIED'", 'chain must assert applied business effect')
assertIncludes(runner, "status='REVOKED'", 'chain must assert derived task permissions are revoked after approval')
assertIncludes(runner, 'controlledFile: { id: fileId', 'chain must preserve DCC bigint IDs as strings')
assertNotIncludes(runner, 'Number(fileId)', 'chain must not coerce DCC bigint IDs to unsafe numbers')
assertNotIncludes(runner, 'UPDATE ', 'chain runner must not write SQL')
assertNotIncludes(runner, 'INSERT ', 'chain runner must not write SQL')
assertNotIncludes(runner, 'DELETE ', 'chain runner must not write SQL')
assertNotIncludes(runner, 'page.request.put', 'chain must not approve through Playwright request shortcuts')
assertNotIncludes(runner, 'page.request.post', 'chain must not create through Playwright request shortcuts')

assertIncludes(realE2e, 'chromium', 'real E2E must use Playwright browser automation')
assertIncludes(realE2e, 'setInputFiles(sourceDoc)', 'real E2E must select the source DOC through the page')
assertIncludes(realE2e, 'DCC_LOSS_ORDER_E2E_TASK_DIR', 'real E2E must support task-owned artifact directory')
assertIncludes(realE2e, '/dcc/controlled-file/upload', 'real E2E must use the DCC upload page')
assertIncludes(realE2e, '/dcc/controlled-files/upload-preview', 'real E2E must upload through the visible page')
assertIncludes(realE2e, '/form-center/actions/resolve', 'real E2E must resolve business action form policy')
assertIncludes(realE2e, '/form-center/instances', 'real E2E must create form center instance')
assertIncludes(realE2e, '/submit', 'real E2E must submit the form center instance')
assertIncludes(realE2e, '/bpm/process-instance/detail', 'real E2E must open real BPM detail page')
assertIncludes(realE2e, '/bpm/process-instance/get-next-approval-nodes', 'real E2E must wait for next approval nodes')
assertIncludes(realE2e, '/bpm/task/approve', 'real E2E must approve through the page-generated BPM action')
assertIncludes(realE2e, 'selectNextAssignees', 'real E2E must exercise next-assignee selection')
assertIncludes(realE2e, 'candidateStrategy', 'real E2E must handle candidate strategy requirements')
assertIncludes(realE2e, 'smokeappr1', 'real E2E must cover review approver permission')
assertIncludes(realE2e, 'smokeplan1', 'real E2E must cover signoff/approval permission')
assertIncludes(realE2e, 'approvalProbes', 'real E2E must record approval permission probes')
assertNotIncludes(realE2e, 'page.request.put', 'real E2E must not approve by API shortcut')
assertNotIncludes(realE2e, 'page.request.post', 'real E2E must not create by API shortcut')
assertNotIncludes(realE2e, 'context.request.put', 'real E2E must not approve by API shortcut')
assertNotIncludes(realE2e, 'context.request.post', 'real E2E must not create by API shortcut')

assertNotIncludes(uploadPage, '<ActionFormPanel', 'DCC upload page must not embed the form-center action panel')
assertNotIncludes(uploadPage, 'data-testid="dcc-upload-section-form-center"', 'DCC upload page must not expose the removed form center section')
assertNotIncludes(uploadPage, 'dccFormCenterContext', 'DCC upload must remove dedicated form-center context state')
assertNotIncludes(uploadPage, 'dccFormCenterFormData', 'DCC upload must remove dedicated form-center form data state')
assertIncludes(uploadPage, 'data-testid="dcc-upload-section-submit"', 'DCC upload page must keep official submit section')
assertIncludes(uploadPage, 'const submitForm = async () =>', 'DCC upload page must keep official submit handler')
assertIncludes(uploadPage, 'submitControlledFile', 'DCC upload page must keep official DCC submit API')
assertIncludes(actionPanel, 'startUserSelectAssignees 必须是对象', 'form panel must validate selected approver payload')
assertIncludes(actionPanel, 'submitFormInstance', 'form panel must submit the instance through form center API')

assertIncludes(taskDoc, '完整测试链路', 'task doc must describe complete test chain')
assertIncludes(taskDoc, '权限验证', 'task doc must require fill and approval permission coverage')
assertIncludes(executionLog, 'BDD:', 'execution log must include BDD scenarios')
assertIncludes(executionLog, 'RED:', 'execution log must include RED evidence')
assertRegex(executionLog, /experience-preflight -> PASS/, 'execution log must record experience preflight')

console.log('dcc loss-order form-center E2E chain static contract passed')
