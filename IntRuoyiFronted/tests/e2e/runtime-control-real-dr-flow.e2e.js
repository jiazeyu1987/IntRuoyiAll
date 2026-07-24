const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = process.env.RUNTIME_CONTROL_E2E_BASE_URL || ''
const ACTION_ORIGIN = process.env.RUNTIME_CONTROL_E2E_ACTION_ORIGIN || ''
const TENANT_NAME = process.env.RUNTIME_CONTROL_E2E_TENANT || '测试租户'
const USERNAME = process.env.RUNTIME_CONTROL_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.RUNTIME_CONTROL_E2E_PASSWORD || 'admin123'
const MAX_WAIT_MS = Number(process.env.RUNTIME_CONTROL_REAL_DR_TIMEOUT_MS || 2 * 60 * 60 * 1000)
const POLL_MS = Number(process.env.RUNTIME_CONTROL_REAL_DR_POLL_MS || 5000)
const REASON_PREFIX = process.env.RUNTIME_CONTROL_REAL_DR_REASON_PREFIX || 'E2E真实测试服DR'
const ROLLBACK_TAG = process.env.RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG || ''
const RESTORE_BACKUP_ID = process.env.RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID || ''
const POST_ACTION_HEALTH_ENV = [
  [
    'RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL',
    'backend health proof',
    'RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL is required for backend health proof.'
  ],
  [
    'RUNTIME_CONTROL_TEST_FRONTEND_URL',
    'frontend health proof',
    'RUNTIME_CONTROL_TEST_FRONTEND_URL is required for frontend health proof.'
  ],
  [
    'RUNTIME_CONTROL_TEST_WEBSITE_URL',
    'website health proof',
    'RUNTIME_CONTROL_TEST_WEBSITE_URL is required for website health proof.'
  ],
  [
    'RUNTIME_CONTROL_TEST_SHOWROOM_URL',
    'showroom health proof',
    'RUNTIME_CONTROL_TEST_SHOWROOM_URL is required for showroom health proof.'
  ]
]

function requireHealthUrl(envName, label, missingMessage) {
  const value = process.env[envName] || ''
  const trimmed = value.trim()
  if (!trimmed) {
    throw new Error(missingMessage)
  }
  try {
    new URL(trimmed)
  } catch (error) {
    throw new Error(`${envName} must be an absolute URL for ${label}: ${trimmed}`)
  }
  return trimmed
}

const POST_ACTION_HEALTH_URLS = POST_ACTION_HEALTH_ENV.map(([envName, label, missingMessage]) =>
  requireHealthUrl(envName, label, missingMessage)
)

function requireExplicitApproval() {
  if (!BASE_URL.trim()) {
    throw new Error(
      'RUNTIME_CONTROL_E2E_BASE_URL is required; use the current worktree frontend URL ' +
        '(for example http://127.0.0.1:8098) or a frontend deployment built from this task branch.'
    )
  }
  if (!ACTION_ORIGIN.trim()) {
    throw new Error(
      'RUNTIME_CONTROL_E2E_ACTION_ORIGIN is required; use a current-code backend origin ' +
        '(for example a locally started task backend URL) or deploy this task branch backend before running.'
    )
  }
  if (process.env.RUNTIME_CONTROL_ALLOW_REAL_DR !== '1') {
    throw new Error(
      'Set RUNTIME_CONTROL_ALLOW_REAL_DR=1 to run the real backup/restore/rollback E2E. ' +
        'This test submits real operations against the configured runtime-control backend.'
    )
  }
  if (!ROLLBACK_TAG.trim()) {
    throw new Error('RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG is required for the real rollback step.')
  }
  if (!RESTORE_BACKUP_ID.trim()) {
    throw new Error(
      'RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID is required for the real restore step; ' +
        'use a server restore candidate that is already verified and rehearsed.'
    )
  }
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function tail(value, maxLength = 1600) {
  if (!value) return ''
  return value.length > maxLength ? value.slice(value.length - maxLength) : value
}

async function fillVisible(page, selector, value, label) {
  const locator = page.locator(selector)
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible ${label}: ${selector}`)
}

async function fillVisibleIfPresent(page, selector, value) {
  const locator = page.locator(selector)
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return true
    }
  }
  return false
}

async function ensureTenantSelected(page) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible())) {
    await tenantSelect.click()
    await page.locator('.login-form .el-select__input').first().fill(TENANT_NAME)
    await page.keyboard.press('Enter')
    return
  }
  const filled = await fillVisibleIfPresent(page, 'input[placeholder="请输入租户名称"]', TENANT_NAME)
  if (!filled) {
    await page.getByText(TENANT_NAME, { exact: true }).first().waitFor({ state: 'visible', timeout: 10000 })
  }
}

async function loginRuntimeControl(page) {
  await page.goto(`${BASE_URL}/login?redirect=/infra/monitors/runtime-control`, {
    waitUntil: 'domcontentloaded'
  })

  if (page.url().includes('/login')) {
    await ensureTenantSelected(page)
    await fillVisible(page, 'input[placeholder="请输入用户名"]', USERNAME, 'username input')
    await fillVisible(page, 'input[placeholder="请输入密码"]', PASSWORD, 'password input')
    await page.locator('button:has-text("登录")').first().click()
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  }

  await page.goto(`${BASE_URL}/infra/monitors/runtime-control`, { waitUntil: 'domcontentloaded' })
  await page.locator('button:has-text("立即备份")').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('button:has-text("恢复数据")').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('button:has-text("回滚版本")').waitFor({ state: 'visible', timeout: 30000 })
}

async function openOperationDialog(page, label) {
  await closeVisibleDialogs(page)
  await page.locator(`button:has-text("${label}")`).first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: label }).last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  return dialog
}

async function closeVisibleDialogs(page) {
  for (let attempts = 0; attempts < 5; attempts += 1) {
    const dialog = page.locator('.el-dialog:visible').last()
    if ((await dialog.count()) === 0) return
    const closeButton = dialog.locator('.el-dialog__headerbtn').first()
    if ((await closeButton.count()) > 0 && (await closeButton.isVisible())) {
      await closeButton.click()
    } else {
      await page.keyboard.press('Escape')
    }
    await page.waitForTimeout(300)
  }
}

async function selectCandidate(dialog, candidateText) {
  if (!candidateText) return
  const row = dialog.locator('.candidate-row').filter({ hasText: candidateText }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.locator('.el-radio').first().click()
}

async function submitOperation(page, latestLogRef, options) {
  const { label, action, reason, selectedCandidateText } = options
  const dialog = await openOperationDialog(page, label)
  await dialog.locator('textarea').first().fill(reason)
  await selectCandidate(dialog, selectedCandidateText)
  await dialog.locator('input[placeholder="输入 PROD"]').fill('PROD')
  const actionResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url() === `${ACTION_ORIGIN}/admin-api/infra/runtime-control/actions`,
    { timeout: 60000 }
  )
  await dialog.locator('button:has-text("确认执行")').click()

  const response = await actionResponsePromise
  const payload = await response.json()
  console.log(`${action.toUpperCase()}_ACTION_HTTP_STATUS ${response.status()}`)
  console.log(`${action.toUpperCase()}_ACTION_PAYLOAD ${JSON.stringify(payload)}`)
  assert.equal(response.status(), 200)
  assert.equal(payload.code, 0, payload.msg || `${action} should be accepted`)
  const operation = payload.data
  assert.equal(operation.action, action)
  assert.equal(operation.reason, reason)
  console.log(`${action.toUpperCase()}_DISPATCHED operationId=${operation.operationId}`)
  const operationLog = await waitOperation(page, latestLogRef, action, operation.operationId)
  await closeVisibleDialogs(page)
  return operationLog
}

async function waitOperation(page, latestLogRef, action, operationId) {
  const deadline = Date.now() + MAX_WAIT_MS
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: operationId }).last()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })

  while (Date.now() < deadline) {
    if (latestLogRef.error) {
      throw latestLogRef.error
    }
    const latestLog = latestLogRef.byOperationId.get(operationId)
    if (latestLog?.status === 'succeeded') {
      console.log(`${action.toUpperCase()}_SUCCEEDED operationId=${operationId}`)
      console.log(tail(latestLog.content))
      return latestLog
    }
    if (latestLog?.status === 'failed') {
      console.log(`${action.toUpperCase()}_FAILED operationId=${operationId}`)
      console.log(tail(latestLog.content))
      throw new Error(`${action} failed: ${tail(latestLog.content)}`)
    }

    const status = latestLog?.status || 'waiting-log'
    const length = latestLog?.length || 0
    console.log(`${action.toUpperCase()}_WAIT operationId=${operationId} status=${status} logBytes=${length}`)
    const refresh = dialog.getByRole('button', { name: '刷新' }).first()
    if ((await refresh.count()) > 0) {
      await refresh.click()
    }
    await delay(POLL_MS)
  }

  throw new Error(`${action} did not complete within ${MAX_WAIT_MS}ms`)
}

function parseBackupId(logContent) {
  const patterns = [
    /备份点[:：]\s*([0-9]{8}[-_][0-9]{6})/,
    /backupId["'\s:=]+([0-9]{8}[-_][0-9]{6})/,
    /BackupId["'\s:=]+([0-9]{8}[-_][0-9]{6})/
  ]
  for (const pattern of patterns) {
    const match = pattern.exec(logContent || '')
    if (match) return match[1]
  }
  throw new Error(`Unable to parse backup id from backup log: ${tail(logContent)}`)
}

async function verifyTestServerHealth() {
  for (const url of POST_ACTION_HEALTH_URLS) {
    const response = await fetch(url)
    const body = await response.text()
    assert.ok(response.status >= 200 && response.status < 400, `${url} returned ${response.status}: ${tail(body)}`)
    console.log(`HEALTH_OK ${url} HTTP ${response.status}`)
  }
}

requireExplicitApproval()

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } })
  const page = await context.newPage()
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

  try {
    await loginRuntimeControl(page)
    const backupLog = await submitOperation(page, latestLogRef, {
      label: '立即备份',
      action: 'backup-now',
      reason: `${REASON_PREFIX}-立即备份-${new Date().toISOString()}`
    })
    const backupId = parseBackupId(backupLog.content)
    console.log(`BACKUP_ID ${backupId}`)

    await submitOperation(page, latestLogRef, {
      label: '恢复数据',
      action: 'restore-data',
      reason: `${REASON_PREFIX}-恢复数据-${RESTORE_BACKUP_ID}`,
      selectedCandidateText: RESTORE_BACKUP_ID
    })

    await submitOperation(page, latestLogRef, {
      label: '回滚版本',
      action: 'rollback-app',
      reason: `${REASON_PREFIX}-回滚版本-${ROLLBACK_TAG}`,
      selectedCandidateText: ROLLBACK_TAG
    })

    await verifyTestServerHealth()
    console.log('PASS: runtime control real test-server backup restore rollback flow')
  } finally {
    await browser.close()
  }
})().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
