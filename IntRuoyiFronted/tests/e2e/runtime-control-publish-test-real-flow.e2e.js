const assert = require('node:assert/strict')
const {
  fillDialogReason,
  getRuntimeControlActionOrigin,
  openOperationDialog,
  runRuntimeControlE2E
} = require('./runtime-control-ops-e2e-helper')

const ACTION_ORIGIN = getRuntimeControlActionOrigin()
const PUBLISH_REASON =
  process.env.RUNTIME_CONTROL_REAL_PUBLISH_REASON ||
  `E2E real code-only publish ${new Date().toISOString()}`
const MAX_WAIT_MS = Number(process.env.RUNTIME_CONTROL_REAL_PUBLISH_TIMEOUT_MS || 2 * 60 * 60 * 1000)
const POLL_MS = Number(process.env.RUNTIME_CONTROL_REAL_PUBLISH_POLL_MS || 15 * 1000)
const APPROVAL_TOKEN = 'ALLOW_TEST_RUNTIME_PUBLISH_WRITE'
const APPROVAL = process.env.RUNTIME_CONTROL_REAL_PUBLISH_APPROVAL || ''
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
  if (process.env.RUNTIME_CONTROL_ALLOW_REAL_PUBLISH !== '1') {
    throw new Error(
      'Set RUNTIME_CONTROL_ALLOW_REAL_PUBLISH=1 to run the real publish-test E2E. ' +
        'This test submits a real code-only deployment to the test server.'
    )
  }
  if (APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set RUNTIME_CONTROL_REAL_PUBLISH_APPROVAL=${APPROVAL_TOKEN} only after explicit user approval.`
    )
  }
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function tail(value, maxLength = 1200) {
  if (!value) return ''
  return value.length > maxLength ? value.slice(value.length - maxLength) : value
}

async function fetchJsonOk(url) {
  const response = await fetch(url)
  const body = await response.text()
  assert.ok(
    response.status >= 200 && response.status < 400,
    `${url} should return 2xx/3xx, got HTTP ${response.status}: ${tail(body, 500)}`
  )
  return response.status
}

async function waitForPublishCompletion(page, operationId, latestLogRef) {
  const deadline = Date.now() + MAX_WAIT_MS
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: operationId }).last()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })

  while (Date.now() < deadline) {
    if (latestLogRef.error) {
      throw latestLogRef.error
    }

    const latestLog = latestLogRef.value
    if (latestLog?.status === 'succeeded') {
      const content = latestLog.content || ''
      assert.match(content, /Publish completed\./)
      assert.match(content, /IntRuoyi frontend:/)
      assert.match(content, /Backend health:/)
      assert.match(content, /Website showroom:/)
      console.log(`PUBLISH_SUCCEEDED operationId=${operationId}`)
      console.log(tail(content))
      return latestLog
    }
    if (latestLog?.status === 'failed') {
      throw new Error(`Publish operation failed: ${tail(latestLog.content || latestLog.status)}`)
    }

    const status = latestLog?.status || 'waiting-log'
    const length = latestLog?.length ?? 0
    console.log(`PUBLISH_WAIT operationId=${operationId} status=${status} logBytes=${length}`)

    const refresh = dialog.getByRole('button', { name: '刷新' }).first()
    if ((await refresh.count()) > 0) {
      await refresh.click()
    }
    await delay(POLL_MS)
  }

  throw new Error(`Publish operation did not complete within ${MAX_WAIT_MS}ms`)
}

async function verifyTestServerHealth() {
  for (const url of POST_ACTION_HEALTH_URLS) {
    const status = await fetchJsonOk(url)
    console.log(`HEALTH_OK ${url} HTTP ${status}`)
  }
}

async function assertPublishScopeDefault(dialog) {
  const codeOnly = dialog.locator('.el-radio-button').filter({ hasText: '只发代码' }).first()
  await codeOnly.waitFor({ state: 'visible', timeout: 10000 })
  const className = await codeOnly.getAttribute('class')
  assert.match(className || '', /is-active/, 'publish-test should default to code-only')
}

requireExplicitApproval()

runRuntimeControlE2E('runtime control real code-only publish-test flow', async ({ page }) => {
  const latestLogRef = { value: null }
  page.on('response', async (response) => {
    if (!response.url().includes('/infra/runtime-control/operations/')) return
    if (!response.url().includes('/log')) return
    try {
      const payload = await response.json()
      if (payload?.code === 0 && payload?.data?.operationId) {
        latestLogRef.value = payload.data
      }
    } catch (error) {
      latestLogRef.error = error
    }
  })

  const actionResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url() === `${ACTION_ORIGIN}/admin-api/infra/runtime-control/actions`,
    { timeout: 60000 }
  )

  const dialog = await openOperationDialog(page, '部署发布包到测试服')
  await assertPublishScopeDefault(dialog)
  await fillDialogReason(dialog, PUBLISH_REASON)
  await page.getByRole('button', { name: '确认执行' }).click()

  const actionResponse = await actionResponsePromise
  assert.equal(actionResponse.status(), 200)
  const actionPayload = await actionResponse.json()
  assert.equal(actionPayload.code, 0, actionPayload.msg || 'publish-test action should be accepted')
  const operation = actionPayload.data
  assert.ok(operation?.operationId, 'publish-test action should return operationId')
  assert.equal(operation.action, 'publish-test')
  assert.equal(operation.parameters?.publishScope, 'code-only')
  assert.equal(operation.reason, PUBLISH_REASON)
  console.log(`PUBLISH_DISPATCHED operationId=${operation.operationId}`)

  const latestLog = await waitForPublishCompletion(page, operation.operationId, latestLogRef)
  assert.equal(latestLog.status, 'succeeded')
  await verifyTestServerHealth()
})
