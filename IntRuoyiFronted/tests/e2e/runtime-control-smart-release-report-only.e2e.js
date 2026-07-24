process.env.RUNTIME_CONTROL_E2E_BASE_URL =
  process.env.RUNTIME_CONTROL_E2E_BASE_URL || 'http://localhost:8081'
process.env.RUNTIME_CONTROL_E2E_ACTION_ORIGIN =
  process.env.RUNTIME_CONTROL_E2E_ACTION_ORIGIN || 'http://127.0.0.1:48081'

const assert = require('node:assert/strict')
const {
  fillDialogReason,
  openOperationDialog,
  runRuntimeControlE2E
} = require('./runtime-control-ops-e2e-helper')

const PREVIEW_PATH = '/admin-api/infra/runtime-control/actions/preview'
const EXECUTE_PATH = '/admin-api/infra/runtime-control/actions'

function assertPreviewPayload(payload, action) {
  assert.equal(payload.code, 0, payload.msg || `${action} preview should succeed: ${JSON.stringify(payload)}`)
  const preview = payload.data
  assert.equal(preview.action, action)
  assert.equal(preview.enableSmartReleaseReport, true)
  assert.ok(preview.arguments.includes('-EnableSmartReleaseReport'))
  assert.ok(preview.arguments.includes('-TestServerHost'))
  assert.ok(preview.arguments.includes('-ProdServerHost'))
  assert.ok(preview.arguments.includes('-BackupServerHost'))
  assert.ok(!preview.arguments.join(' ').includes('ssh '))
  assert.ok(!preview.arguments.join(' ').includes('scp '))
  return preview
}

async function expectSmartReleaseUncheckedThenEnable(dialog) {
  const checkbox = dialog.locator('.el-checkbox').filter({ hasText: 'report-only 报告/预检' }).first()
  await checkbox.waitFor({ state: 'visible', timeout: 10000 })
  const className = await checkbox.getAttribute('class')
  assert.doesNotMatch(
    className || '',
    /is-checked/,
    'Smart Release report-only must not be selected by default'
  )
  await checkbox.click()
  const checkedClassName = await checkbox.getAttribute('class')
  assert.match(checkedClassName || '', /is-checked/, 'Smart Release report-only should be selected after manual click')
}

async function previewCurrentDialog(page, dialog, expectedAction) {
  const previewResponsePromise = page.waitForResponse(
    (response) => response.request().method() === 'POST' && new URL(response.url()).pathname === PREVIEW_PATH,
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '预览命令' }).click()
  const response = await previewResponsePromise
  assert.equal(response.status(), 200)
  const payload = await response.json()
  const preview = assertPreviewPayload(payload, expectedAction)
  await dialog.locator('.operation-command-preview').waitFor({ state: 'visible', timeout: 10000 })
  const previewText = await dialog.locator('.operation-command-preview').innerText()
  assert.match(previewText, /enableSmartReleaseReport=true/)
  assert.match(previewText, /-EnableSmartReleaseReport/)
  return preview
}

async function chooseFirstReleasePackage(page, dialog) {
  const select = dialog.locator('.el-select').first()
  await select.waitFor({ state: 'visible', timeout: 30000 })
  await select.click()
  const firstOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first()
  await firstOption.waitFor({ state: 'visible', timeout: 30000 })
  await firstOption.click()
}

runRuntimeControlE2E('runtime control Smart Release report-only preview', async ({ page }) => {
  const executeRequests = []
  page.on('request', (request) => {
    if (request.method() === 'POST' && new URL(request.url()).pathname === EXECUTE_PATH) {
      executeRequests.push(request.postData() || '')
    }
  })

  const buildDialog = await openOperationDialog(page, '构建发布包')
  await expectSmartReleaseUncheckedThenEnable(buildDialog)
  await fillDialogReason(buildDialog, 'E2E Smart Release report-only build preview')
  const buildPreview = await previewCurrentDialog(page, buildDialog, 'build-release')
  assert.equal(buildPreview.environment, 'release')
  await buildDialog.getByRole('button', { name: '取消' }).click()

  const deployDialog = await openOperationDialog(page, '部署发布包到测试服')
  await expectSmartReleaseUncheckedThenEnable(deployDialog)
  await chooseFirstReleasePackage(page, deployDialog)
  await fillDialogReason(deployDialog, 'E2E Smart Release report-only deploy preview')
  const deployPreview = await previewCurrentDialog(page, deployDialog, 'publish-test')
  assert.equal(deployPreview.environment, 'test')
  assert.ok(deployPreview.arguments.includes('-ServerHost'))

  assert.equal(
    executeRequests.length,
    0,
    `Smart Release report-only preview must not submit dangerous execute action requests: ${executeRequests.join(', ')}`
  )
})
