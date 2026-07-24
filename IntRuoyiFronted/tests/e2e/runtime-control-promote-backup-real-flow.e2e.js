const assert = require('node:assert/strict')

const {
  fillDialogReason,
  fillProdConfirm,
  getRuntimeControlActionOrigin,
  openOperationDialog,
  runRuntimeControlE2E
} = require('./runtime-control-ops-e2e-helper')

const RELEASE_TAG = (process.env.RUNTIME_CONTROL_PROMOTE_BACKUP_RELEASE_TAG || '').trim()
const PROMOTE_BACKUP_REASON =
  process.env.RUNTIME_CONTROL_PROMOTE_BACKUP_REASON ||
  `E2E real promote-backup ${new Date().toISOString()}`
const MAX_WAIT_MS = Number(process.env.RUNTIME_CONTROL_PROMOTE_BACKUP_TIMEOUT_MS || 2 * 60 * 60 * 1000)
const POLL_MS = Number(process.env.RUNTIME_CONTROL_PROMOTE_BACKUP_POLL_MS || 15 * 1000)
const DCC_READBACK_MIN_BYTES = Number(process.env.RUNTIME_CONTROL_BACKUP_DCC_READBACK_MIN_BYTES || 128)
const APPROVAL_TOKEN = 'ALLOW_TEST_RUNTIME_PROMOTE_BACKUP_WRITE'
const APPROVAL = process.env.RUNTIME_CONTROL_REAL_PROMOTE_BACKUP_APPROVAL || ''

function requireExplicitApproval() {
  if (process.env.RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP !== '1') {
    throw new Error(
      'Set RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP=1 to run the real promote-backup E2E. ' +
        'This test submits a real promotion to the backup server.'
    )
  }
  if (APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set RUNTIME_CONTROL_REAL_PROMOTE_BACKUP_APPROVAL=${APPROVAL_TOKEN} only after explicit user approval.`
    )
  }
  if (!RELEASE_TAG) {
    throw new Error('RUNTIME_CONTROL_PROMOTE_BACKUP_RELEASE_TAG is required for promote-backup.')
  }
}

function requireExplicitUrl(name, value, missingMessage) {
  if (!value || !value.trim()) {
    throw new Error(missingMessage)
  }
  const trimmed = value.trim()
  try {
    return new URL(trimmed).href
  } catch (error) {
    throw new Error(`${name} must be a valid absolute URL: ${trimmed}`)
  }
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function tail(value, maxLength = 1600) {
  if (!value) return ''
  return value.length > maxLength ? value.slice(value.length - maxLength) : value
}

function releaseTagMatches(item) {
  return item?.releaseTag === RELEASE_TAG || item?.packageDirectoryName === RELEASE_TAG
}

function validateReleasePackageBinding(packages) {
  const releasePackage = packages.find(releaseTagMatches)
  assert.ok(releasePackage, `release package ${RELEASE_TAG} must be returned by /release-packages`)
  assert.equal(releasePackage.status, 'AVAILABLE', `release package ${RELEASE_TAG} must be AVAILABLE`)
  assert.equal(releasePackage.tested, true, `release package ${RELEASE_TAG} must be tested`)
  assert.notEqual(
    releasePackage.checksumPresent,
    false,
    `release package ${RELEASE_TAG} must have checksum evidence`
  )
  assert.ok(
    releasePackage.testedRecoverySetCandidateId,
    `release package ${RELEASE_TAG} must have testedRecoverySetCandidateId`
  )
  assert.ok(
    releasePackage.testedRecoverySetId,
    `release package ${RELEASE_TAG} must have testedRecoverySetId`
  )
  assert.ok(
    releasePackage.testedRecoverySetManifestHash,
    `release package ${RELEASE_TAG} must have testedRecoverySetManifestHash`
  )
  console.log(
    `PROMOTE_BACKUP_RELEASE_PACKAGE releaseTag=${releasePackage.releaseTag} ` +
      `testedRecoverySetCandidateId=${releasePackage.testedRecoverySetCandidateId} ` +
      `testedRecoverySetId=${releasePackage.testedRecoverySetId} ` +
      `testedRecoverySetManifestHash=${releasePackage.testedRecoverySetManifestHash}`
  )
  return releasePackage
}

async function loadReleasePackagesForBackup(page) {
  const releasePackagesResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes('/admin-api/infra/runtime-control/release-packages'),
    { timeout: 60000 }
  )
  const dialog = await openOperationDialog(page, '上线备份服务器')
  const response = await releasePackagesResponsePromise
  assert.equal(response.status(), 200, 'release-packages HTTP status')
  const payload = await response.json()
  assert.equal(payload.code, 0, payload.msg || 'release-packages response should be ok')
  assert.ok(Array.isArray(payload.data), 'release-packages data must be an array')
  const releasePackage = validateReleasePackageBinding(payload.data)
  return { dialog, releasePackage }
}

async function selectReleasePackage(page, dialog, releaseTag) {
  const select = dialog.locator('.el-select').first()
  await select.click()
  const input = page.locator('.el-select-dropdown:visible input, .el-select__input:visible').last()
  if ((await input.count()) > 0) {
    await input.fill(releaseTag)
  }
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: releaseTag }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function waitForBackupPromotionCompletion(page, operationId, latestLogRef) {
  const deadline = Date.now() + MAX_WAIT_MS
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: operationId }).last()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })

  while (Date.now() < deadline) {
    if (latestLogRef.error) {
      throw latestLogRef.error
    }

    const latestLog = latestLogRef.value
    if (latestLog?.status === 'succeeded') {
      const content = latestLog.content || ''
      assert.match(content, /Promotion completed\./)
      assert.match(content, /Backup|backup|备用|备份/)
      console.log(`PROMOTE_BACKUP_SUCCEEDED operationId=${operationId}`)
      console.log(tail(content))
      return latestLog
    }
    if (latestLog?.status === 'failed') {
      throw new Error(`promote-backup operation failed: ${tail(latestLog.content || latestLog.status)}`)
    }

    const status = latestLog?.status || 'waiting-log'
    const length = latestLog?.length ?? 0
    console.log(`PROMOTE_BACKUP_WAIT operationId=${operationId} status=${status} logBytes=${length}`)

    const refresh = dialog.getByRole('button', { name: '刷新' }).first()
    if ((await refresh.count()) > 0) {
      await refresh.click()
    }
    await delay(POLL_MS)
  }

  throw new Error(`promote-backup operation did not complete within ${MAX_WAIT_MS}ms`)
}

async function fetchOk(url) {
  const response = await fetch(url)
  const body = await response.text()
  assert.ok(
    response.status >= 200 && response.status < 400,
    `${url} should return 2xx/3xx, got HTTP ${response.status}: ${tail(body, 500)}`
  )
  console.log(`BACKUP_HEALTH_OK ${url} HTTP ${response.status}`)
}

async function verifyBackupHealth(urls) {
  for (const url of urls) {
    await fetchOk(url)
  }
}

async function verifyDccReadback(page, url) {
  const readback = await page.evaluate(async ({ targetUrl }) => {
    const unwrapStorageValue = (raw) => {
      if (!raw) return ''
      try {
        const item = JSON.parse(raw)
        if (item && typeof item === 'object' && Object.prototype.hasOwnProperty.call(item, 'v')) {
          try {
            return JSON.parse(item.v)
          } catch (error) {
            return item.v
          }
        }
        return item
      } catch (error) {
        return raw
      }
    }

    const accessToken = unwrapStorageValue(window.localStorage.getItem('ACCESS_TOKEN'))
    const tenantId = unwrapStorageValue(window.localStorage.getItem('tenantId'))
    const visitTenantId = unwrapStorageValue(window.localStorage.getItem('visitTenantId'))

    if (!accessToken) {
      throw new Error('DCC readback verification requires ACCESS_TOKEN from browser storage')
    }
    if (!tenantId) {
      throw new Error('DCC readback verification requires tenant-id from browser storage')
    }

    const headers = {
      'Cache-Control': 'no-cache',
      Pragma: 'no-cache'
    }
    headers.Authorization = `Bearer ${accessToken}`
    headers['tenant-id'] = String(tenantId)
    if (visitTenantId && accessToken) {
      headers['visit-tenant-id'] = String(visitTenantId)
    }

    const response = await window.fetch(targetUrl, { method: 'GET', headers })
    const bytes = new Uint8Array(await response.arrayBuffer())
    return {
      status: response.status,
      contentType: response.headers.get('content-type') || '',
      byteLength: bytes.byteLength,
      auth: {
        hasAccessToken: Boolean(accessToken),
        hasTenantId: Boolean(tenantId),
        hasVisitTenantId: Boolean(visitTenantId)
      }
    }
  }, { targetUrl: url })

  assert.ok(
    readback.status >= 200 && readback.status < 400,
    `${url} should return 2xx/3xx, got HTTP ${readback.status}`
  )
  assert.ok(
    readback.auth.hasAccessToken,
    'DCC readback verification requires ACCESS_TOKEN from browser storage'
  )
  assert.ok(
    readback.auth.hasTenantId,
    'DCC readback verification requires tenant-id from browser storage'
  )
  assert.ok(
    readback.byteLength >= DCC_READBACK_MIN_BYTES,
    `${url} should return at least ${DCC_READBACK_MIN_BYTES} bytes, got ${readback.byteLength}`
  )
  assert.equal(
    /^application\/json\b/i.test(readback.contentType),
    false,
    `${url} returned application/json instead of DCC file or preview content`
  )
  console.log(
    `DCC_READBACK_OK ${url} HTTP ${readback.status} bytes=${readback.byteLength} ` +
      `contentType=${readback.contentType} hasVisitTenantId=${readback.auth.hasVisitTenantId}`
  )
}

requireExplicitApproval()

const ACTION_ORIGIN = getRuntimeControlActionOrigin()
const BACKUP_HEALTH_URLS = [
  requireExplicitUrl(
    'RUNTIME_CONTROL_BACKUP_BACKEND_HEALTH_URL',
    process.env.RUNTIME_CONTROL_BACKUP_BACKEND_HEALTH_URL,
    'RUNTIME_CONTROL_BACKUP_BACKEND_HEALTH_URL is required for backup backend health proof.'
  ),
  requireExplicitUrl(
    'RUNTIME_CONTROL_BACKUP_FRONTEND_URL',
    process.env.RUNTIME_CONTROL_BACKUP_FRONTEND_URL,
    'RUNTIME_CONTROL_BACKUP_FRONTEND_URL is required for backup frontend proof.'
  ),
  requireExplicitUrl(
    'RUNTIME_CONTROL_BACKUP_WEBSITE_URL',
    process.env.RUNTIME_CONTROL_BACKUP_WEBSITE_URL,
    'RUNTIME_CONTROL_BACKUP_WEBSITE_URL is required for backup Website proof.'
  ),
  requireExplicitUrl(
    'RUNTIME_CONTROL_BACKUP_SHOWROOM_URL',
    process.env.RUNTIME_CONTROL_BACKUP_SHOWROOM_URL,
    'RUNTIME_CONTROL_BACKUP_SHOWROOM_URL is required for backup Showroom proof.'
  )
]
const DCC_READBACK_URL = requireExplicitUrl(
  'RUNTIME_CONTROL_BACKUP_DCC_READBACK_URL',
  process.env.RUNTIME_CONTROL_BACKUP_DCC_READBACK_URL,
  'RUNTIME_CONTROL_BACKUP_DCC_READBACK_URL is required for backup DCC readback proof.'
)

runRuntimeControlE2E('runtime control real promote-backup flow', async ({ page }) => {
  const latestLogRef = { value: null, error: null }
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

  const { dialog, releasePackage } = await loadReleasePackagesForBackup(page)
  await selectReleasePackage(page, dialog, releasePackage.releaseTag)
  await fillDialogReason(dialog, PROMOTE_BACKUP_REASON)
  await fillProdConfirm(dialog)
  await page.getByRole('button', { name: '确认执行' }).click()

  const actionResponse = await actionResponsePromise
  assert.equal(actionResponse.status(), 200)
  const actionPayload = await actionResponse.json()
  assert.equal(actionPayload.code, 0, actionPayload.msg || 'promote-backup action should be accepted')
  const operation = actionPayload.data
  assert.ok(operation?.operationId, 'promote-backup action should return operationId')
  assert.equal(operation.action, 'promote-backup')
  assert.equal(operation.environment, 'backup')
  assert.equal(operation.parameters?.releaseTag, releasePackage.releaseTag)
  assert.equal(operation.reason, PROMOTE_BACKUP_REASON)
  console.log(`PROMOTE_BACKUP_DISPATCHED operationId=${operation.operationId}`)

  const latestLog = await waitForBackupPromotionCompletion(page, operation.operationId, latestLogRef)
  assert.equal(latestLog.status, 'succeeded')
  await verifyBackupHealth(BACKUP_HEALTH_URLS)
  await verifyDccReadback(page, DCC_READBACK_URL)
  console.log('PASS: runtime control real promote-backup flow')
})
