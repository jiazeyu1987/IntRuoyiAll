const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const {
  assertRuntimeControlTestAccountBoundary,
  getRuntimeControlActionOrigin,
  getRuntimeControlBaseUrl,
  runRuntimeControlE2E
} = require('./runtime-control-ops-e2e-helper')

const BASE_URL = getRuntimeControlBaseUrl()
const ACTION_ORIGIN = getRuntimeControlActionOrigin()
const MAX_WAIT_MS = Number(process.env.RUNTIME_CONTROL_REAL_BACKUP_TIMEOUT_MS || 60 * 60 * 1000)
const POLL_MS = Number(process.env.RUNTIME_CONTROL_REAL_BACKUP_POLL_MS || 10000)
const ARTIFACT_DIR =
  process.env.RUNTIME_CONTROL_E2E_ARTIFACT_DIR ||
  path.resolve(
    __dirname,
    '../../../release_and_backup-root/doc/tasks/20260609-release-and-backup-implementation/artifacts'
  )
const APPROVAL_TOKEN = 'ALLOW_TEST_RUNTIME_BACKUP_WRITE'
const APPROVAL = process.env.RUNTIME_CONTROL_REAL_TEST_BACKUP_APPROVAL || ''

function requireExplicitApproval() {
  if (process.env.RUNTIME_CONTROL_ALLOW_REAL_TEST_BACKUP_SETUP !== '1') {
    throw new Error(
      'Set RUNTIME_CONTROL_ALLOW_REAL_TEST_BACKUP_SETUP=1 to run this real E2E. ' +
        'It writes a test-environment backup point to NAS Backup/BackupPackage.'
    )
  }
  if (APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set RUNTIME_CONTROL_REAL_TEST_BACKUP_APPROVAL=${APPROVAL_TOKEN} only after explicit user approval.`
    )
  }
  assertRuntimeControlTestAccountBoundary('runtime-control real test backup')
  for (const [name, value] of [
    ['RUNTIME_CONTROL_E2E_BASE_URL', BASE_URL],
    ['RUNTIME_CONTROL_E2E_ACTION_ORIGIN', ACTION_ORIGIN]
  ]) {
    assert.notEqual(
      new URL(value).hostname,
      '172.30.30.57',
      `${name} must not target protected production server 172.30.30.57`
    )
  }
}

function tail(value, maxLength = 1600) {
  if (!value) return ''
  return value.length > maxLength ? value.slice(value.length - maxLength) : value
}

async function openOperation(page, label) {
  await page.locator('.ops-toolbar').getByRole('button', { name: label }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: label }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  return dialog
}

async function dismissMessageBoxes(page) {
  const boxes = page.locator('.el-message-box:visible')
  const count = await boxes.count().catch(() => 0)
  for (let index = 0; index < count; index += 1) {
    const box = boxes.last()
    const close = box.locator('.el-message-box__headerbtn, .el-message-box__btns button').last()
    if ((await close.count().catch(() => 0)) > 0) {
      await close.click({ timeout: 2000 }).catch(() => page.keyboard.press('Escape').catch(() => {}))
    } else {
      await page.keyboard.press('Escape').catch(() => {})
    }
  }
}

async function isClickable(locator) {
  if ((await locator.count().catch(() => 0)) === 0) return false
  return locator
    .evaluate((element) => {
      const button = element
      return (
        !button.disabled &&
        button.getAttribute('aria-disabled') !== 'true' &&
        !button.classList.contains('is-loading')
      )
    })
    .catch(() => false)
}

async function submitAndWait(page, latestLogRef, label, action, configureDialog) {
  const dialog = await openOperation(page, label)
  await configureDialog(dialog)

  let requestPayload = ''
  let actionRequestSeen = false
  page.once('request', (request) => {
    if (
      request.method() === 'POST' &&
      request.url().includes('/admin-api/infra/runtime-control/actions')
    ) {
      actionRequestSeen = true
      requestPayload = request.postData() || ''
      console.log(`ACTION_REQUEST ${request.url()} ${requestPayload}`)
    }
  })

  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/admin-api/infra/runtime-control/actions'),
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '确认执行' }).click()

  let response
  try {
    response = await responsePromise
  } catch (error) {
    const visibleMessages = await page
      .locator('.el-message:visible,.el-alert:visible,.el-form-item__error')
      .allInnerTexts()
      .catch(() => [])
    throw new Error(
      `${action} did not receive action response within 60000ms; requestSeen=${actionRequestSeen}; ` +
        `requestPayload=${requestPayload}; visibleMessages=${JSON.stringify(visibleMessages)}; ` +
        `baseUrl=${BASE_URL}; actionOrigin=${ACTION_ORIGIN}; cause=${error.message}`
    )
  }
  const payload = await response.json()
  assert.equal(response.status(), 200, `${action} HTTP status`)
  assert.equal(payload.code, 0, payload.msg || `${action} should be accepted`)
  const operation = payload.data
  assert.equal(operation.action, action)
  assert.ok(operation.operationId, `${action} should return operationId`)
  console.log(`${action.toUpperCase()}_DISPATCHED operationId=${operation.operationId}`)

  return waitOperation(page, latestLogRef, operation)
}

async function waitOperation(page, latestLogRef, operation) {
  const deadline = Date.now() + MAX_WAIT_MS
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: operation.operationId }).last()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })

  while (Date.now() < deadline) {
    if (latestLogRef.error) {
      throw latestLogRef.error
    }
    const latestLog = latestLogRef.byOperationId.get(operation.operationId)
    if (latestLog?.status === 'succeeded') {
      console.log(`${operation.action.toUpperCase()}_SUCCEEDED operationId=${operation.operationId}`)
      console.log(tail(latestLog.content))
      return latestLog
    }
    if (latestLog?.status === 'failed') {
      console.log(`${operation.action.toUpperCase()}_FAILED operationId=${operation.operationId}`)
      console.log(tail(latestLog.content))
      throw new Error(`${operation.action} failed: ${tail(latestLog.content)}`)
    }

    const status = latestLog?.status || 'waiting-log'
    const length = latestLog?.length || 0
    console.log(`${operation.action.toUpperCase()}_WAIT operationId=${operation.operationId} status=${status} logBytes=${length}`)
    await dismissMessageBoxes(page)
    const refresh = dialog.locator('.log-toolbar button').first()
    if (await isClickable(refresh)) {
      await refresh.click({ timeout: 5000 }).catch(async () => {
        await dismissMessageBoxes(page)
        if (await isClickable(refresh)) {
          await refresh.click({ timeout: 5000 })
        }
      })
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_MS))
  }

  throw new Error(`${operation.action} did not complete within ${MAX_WAIT_MS}ms`)
}

function parseBackupId(content) {
  const patterns = [
    /backupId["'\s:=]+([0-9A-Za-z._-]+)/,
    /BackupId["'\s:=]+([0-9A-Za-z._-]+)/,
    /备份点[:：]\s*([0-9A-Za-z._-]+)/
  ]
  for (const pattern of patterns) {
    const match = pattern.exec(content || '')
    if (match) return match[1]
  }
  return ''
}

function writeArtifacts(payload) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(
    path.join(ARTIFACT_DIR, 'runtime-control-real-test-backup-setup.json'),
    JSON.stringify(payload, null, 2),
    'utf8'
  )
}

requireExplicitApproval()

runRuntimeControlE2E('runtime-control real test backup setup', async ({ page }) => {
  const latestLogRef = { byOperationId: new Map(), error: null }
  page.on('response', async (response) => {
    if (!response.url().includes('/infra/runtime-control/operations/')) return
    if (!response.url().includes('/log')) return
    try {
      const payload = await response.json()
      if (payload?.code === 0 && payload?.data?.operationId) {
        latestLogRef.byOperationId.set(payload.data.operationId, payload.data)
      }
    } catch (error) {
      latestLogRef.error = error
    }
  })

  const reason = `E2E real test backup ${new Date().toISOString()}`
  const backupLog = await submitAndWait(page, latestLogRef, '立即备份', 'backup-now', async (dialog) => {
    await dialog.locator('.el-radio-button').filter({ hasText: '测试服' }).click()
    await dialog.locator('textarea').first().fill(reason)
    const prodInput = dialog.locator('input[placeholder="输入 PROD"]')
    assert.equal(await prodInput.count(), 0, 'test backup must not require PROD confirmation')
  })

  const artifact = {
    baseUrl: BASE_URL,
    actionOrigin: ACTION_ORIGIN,
    backupLogStatus: backupLog.status,
    backupId: parseBackupId(backupLog.content),
    completedAt: new Date().toISOString()
  }
  assert.ok(artifact.backupId, 'real test backup should expose backupId in operation log')
  writeArtifacts(artifact)
  console.log(`REAL_TEST_BACKUP_ARTIFACT ${JSON.stringify(artifact)}`)
})
