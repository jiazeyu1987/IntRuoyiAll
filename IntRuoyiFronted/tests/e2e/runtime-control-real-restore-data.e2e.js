const assert = require('node:assert/strict')

const {
  assertRuntimeControlTestAccountBoundary,
  getRuntimeControlActionOrigin,
  getRuntimeControlBaseUrl,
  runRuntimeControlE2E
} = require('./runtime-control-ops-e2e-helper')

const BASE_URL = getRuntimeControlBaseUrl()
const ACTION_ORIGIN = getRuntimeControlActionOrigin()
const BACKUP_ID = (process.env.RUNTIME_CONTROL_REAL_RESTORE_BACKUP_ID || '').trim()
const TARGET_ENV = (process.env.RUNTIME_CONTROL_REAL_RESTORE_TARGET_ENV || 'test').trim()
const MAX_WAIT_MS = Number(process.env.RUNTIME_CONTROL_REAL_RESTORE_TIMEOUT_MS || 2 * 60 * 60 * 1000)
const POLL_MS = Number(process.env.RUNTIME_CONTROL_REAL_RESTORE_POLL_MS || 10000)
const APPROVAL_TOKEN = 'ALLOW_TEST_RUNTIME_RESTORE_WRITE'
const APPROVAL = process.env.RUNTIME_CONTROL_REAL_RESTORE_DATA_APPROVAL || ''

function requireExplicitApproval() {
  if (process.env.RUNTIME_CONTROL_ALLOW_REAL_RESTORE_DATA !== '1') {
    throw new Error(
      'Set RUNTIME_CONTROL_ALLOW_REAL_RESTORE_DATA=1 to run real restore-data E2E. ' +
        'This submits a real restore operation against the configured test or backup target.'
    )
  }
  if (APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set RUNTIME_CONTROL_REAL_RESTORE_DATA_APPROVAL=${APPROVAL_TOKEN} only after explicit user approval.`
    )
  }
  assertRuntimeControlTestAccountBoundary('runtime-control real restore-data')
  assert.ok(BACKUP_ID, 'RUNTIME_CONTROL_REAL_RESTORE_BACKUP_ID is required')
  assert.ok(['test', 'backup'].includes(TARGET_ENV), `RUNTIME_CONTROL_REAL_RESTORE_TARGET_ENV must be test or backup, got ${TARGET_ENV}`)
  for (const [name, value] of [
    ['RUNTIME_CONTROL_E2E_BASE_URL', BASE_URL],
    ['RUNTIME_CONTROL_E2E_ACTION_ORIGIN', ACTION_ORIGIN]
  ]) {
    assert.notEqual(new URL(value).hostname, '172.30.30.57', `${name} must not target protected production server 172.30.30.57`)
  }
}

function tail(value, maxLength = 1600) {
  if (!value) return ''
  return value.length > maxLength ? value.slice(value.length - maxLength) : value
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function readOperationLogPayload(response) {
  const contentType = response.headers()['content-type'] || ''
  if (!contentType.includes('application/json')) {
    return null
  }
  return response.json()
}

async function openRestoreDialog(page) {
  await page.locator('.ops-toolbar').getByRole('button', { name: '恢复数据' }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '恢复数据' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  return dialog
}

async function selectTarget(dialog) {
  const label = TARGET_ENV === 'backup' ? '备份服务器' : '测试服'
  await dialog.locator('.el-radio-button').filter({ hasText: label }).click()
}

async function selectCandidate(dialog) {
  const row = dialog.locator('.candidate-row').filter({ hasText: BACKUP_ID }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.locator('.el-radio').first().click()
}

async function submitRestore(page, latestLogRef) {
  const dialog = await openRestoreDialog(page)
  await selectTarget(dialog)
  await selectCandidate(dialog)
  await dialog.locator('textarea').first().fill(`E2E real restore ${TARGET_ENV} ${BACKUP_ID}`)
  const prodInput = dialog.locator('input[placeholder="输入 PROD"]')
  if (TARGET_ENV === 'backup') {
    await prodInput.fill('PROD')
  } else {
    assert.equal(await prodInput.count(), 0, 'test restore must not require PROD confirmation')
  }

  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url() === `${ACTION_ORIGIN}/admin-api/infra/runtime-control/actions`,
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '确认执行' }).click()
  const response = await responsePromise
  const payload = await response.json()
  assert.equal(response.status(), 200, 'restore-data HTTP status')
  assert.equal(payload.code, 0, payload.msg || 'restore-data should be accepted')
  const operation = payload.data
  assert.equal(operation.action, 'restore-data')
  assert.ok(operation.operationId, 'restore-data should return operationId')
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
      console.log(`RESTORE_DATA_SUCCEEDED operationId=${operation.operationId}`)
      console.log(tail(latestLog.content))
      return latestLog
    }
    if (latestLog?.status === 'failed') {
      throw new Error(`restore-data failed: ${tail(latestLog.content || latestLog.status)}`)
    }
    const refresh = dialog.getByRole('button', { name: '刷新' }).first()
    if ((await refresh.count()) > 0) {
      await refresh.click()
    }
    await delay(POLL_MS)
  }
  throw new Error(`restore-data did not complete within ${MAX_WAIT_MS}ms`)
}

requireExplicitApproval()

runRuntimeControlE2E('runtime-control real restore-data only', async ({ page }) => {
  const latestLogRef = { byOperationId: new Map(), error: null }
  page.on('response', async (response) => {
    if (!response.url().includes('/infra/runtime-control/operations/')) return
    if (!response.url().includes('/log')) return
    try {
      const payload = await readOperationLogPayload(response)
      if (payload?.code === 0 && payload?.data?.operationId) {
        latestLogRef.byOperationId.set(payload.data.operationId, payload.data)
      }
    } catch (error) {
      latestLogRef.error = error
    }
  })
  const restoreLog = await submitRestore(page, latestLogRef)
  const artifact = {
    baseUrl: BASE_URL,
    actionOrigin: ACTION_ORIGIN,
    backupId: BACKUP_ID,
    targetEnvironment: TARGET_ENV,
    operationId: restoreLog.operationId,
    status: restoreLog.status,
    completedAt: new Date().toISOString()
  }
  console.log(`REAL_RESTORE_DATA_ARTIFACT ${JSON.stringify(artifact)}`)
})
