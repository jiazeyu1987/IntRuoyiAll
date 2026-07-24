const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_BACKUP_E2E_BASE_URL || '').replace(/\/+$/, '')
const TENANT = process.env.DCC_BACKUP_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_BACKUP_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_BACKUP_E2E_PASSWORD || 'admin123'
const PRESENT_FILES = parseExpectationArray('DCC_RESTORE_E2E_PRESENT', process.env.DCC_RESTORE_E2E_PRESENT || '[]')
const ABSENT_FILES = parseExpectationArray('DCC_RESTORE_E2E_ABSENT', process.env.DCC_RESTORE_E2E_ABSENT || '[]')

function parseExpectationArray(name, rawValue) {
  let parsed = null
  try {
    parsed = JSON.parse(rawValue)
  } catch (error) {
    throw new Error(`${name} must be valid JSON: ${error.message}`)
  }
  if (!Array.isArray(parsed)) {
    throw new Error(`${name} must be a JSON array`)
  }
  return parsed
}

function assertSafeDccBackupBoundary() {
  assert.ok(BASE_URL, 'DCC_BACKUP_E2E_BASE_URL is required for DCC restore E2E')
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'DCC restore E2E must not target protected production server 172.30.30.57')
  if (TENANT !== '测试租户') {
    throw new Error(`DCC restore E2E must use 测试租户, got ${TENANT}`)
  }
  if (USERNAME !== 'aoteman') {
    throw new Error(`DCC restore E2E must use 测试租户/aoteman, got ${USERNAME}`)
  }
}

function assertRestoreExpectations() {
  assert.ok(PRESENT_FILES.length > 0 || ABSENT_FILES.length > 0, 'no restore verification expectations provided')
  for (const file of PRESENT_FILES) {
    if (!file || !String(file.id || '').trim()) {
      throw new Error(`restore present expectation is missing id: ${JSON.stringify(file)}`)
    }
    if (!String(file.fileName || '').trim()) {
      throw new Error(`restore present expectation is missing fileName: ${JSON.stringify(file)}`)
    }
  }
  for (const file of ABSENT_FILES) {
    if (!file || !String(file.id || '').trim()) {
      throw new Error(`restore absent expectation is missing id: ${JSON.stringify(file)}`)
    }
  }
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function fillFirstVisible(page, selector, value, label) {
  const locator = page.locator(selector)
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible ${label}: ${selector}`)
}

async function selectTenant(page, tenantName) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible())) {
    await tenantSelect.click()
    await page.locator('.login-form .el-select__input').first().fill(tenantName)
    await page.keyboard.press('Enter')
    return true
  }
  return false
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  if (page.url().includes('/login')) {
    const selected = await selectTenant(page, TENANT)
    if (!selected) {
      await fillFirstVisible(page, 'input[placeholder="请输入租户名称"]', TENANT, 'tenant input')
    }
    await fillFirstVisible(page, 'input[placeholder="请输入用户名"]', USERNAME, 'username input')
    await fillFirstVisible(page, 'input[placeholder="请输入密码"]', PASSWORD, 'password input')
    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/system/auth/login') &&
          response.request().method() === 'POST',
        { timeout: 30000 }
      ),
      page.locator('.login-form .el-button--primary').first().click()
    ])
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  }
  await settle(page)
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) {
    return ''
  }
  const normalizeString = (value) => {
    let current = value || ''
    for (let index = 0; index < 3; index += 1) {
      const trimmed = String(current).trim()
      if (!trimmed.startsWith('"')) {
        return trimmed
      }
      try {
        const parsed = JSON.parse(trimmed)
        if (typeof parsed !== 'string' || parsed === current) {
          return trimmed.replace(/^"(.*)"$/, '$1')
        }
        current = parsed
      } catch {
        return trimmed.replace(/^"(.*)"$/, '$1')
      }
    }
    return String(current).trim()
  }
  const unwrap = (value) => {
    let current = value
    for (let index = 0; index < 6; index += 1) {
      if (!current || typeof current !== 'object') {
        return typeof current === 'string' ? normalizeString(current) : current || ''
      }
      if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
        current = current.accessToken
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'v')) {
        current = current.v
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'value')) {
        current = current.value
        continue
      }
      return current
    }
    return current || ''
  }
  try {
    const parsed = JSON.parse(raw)
    return unwrap(parsed)
  } catch {
    return normalizeString(raw)
  }
}

async function buildAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const accessToken = readWsCacheValue(snapshot, 'ACCESS_TOKEN')
  const tenantId = readWsCacheValue(snapshot, 'tenantId')
  const visitTenantId = readWsCacheValue(snapshot, 'visitTenantId')
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (tenantId) {
    headers['tenant-id'] = String(tenantId)
  }
  if (visitTenantId) {
    headers['visit-tenant-id'] = String(visitTenantId)
  }
  return headers
}

async function fetchJson(page, headers, url) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, { headers: requestHeaders })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    { requestUrl: url, requestHeaders: headers }
  )
}

function createRequestId() {
  return `codex-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function downloadFile(page, headers, id) {
  const requestId = createRequestId()
  const url = `${BASE_URL}/admin-api/dcc/controlled-files/${id}/download?nonControlledWarningConfirmed=true&downloadRequestId=${encodeURIComponent(requestId)}`
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, { headers: requestHeaders })
      const buffer = await response.arrayBuffer()
      const contentType = response.headers.get('content-type') || ''
      const text = contentType.includes('application/json') ? new TextDecoder().decode(buffer) : ''
      return {
        status: response.status,
        size: buffer.byteLength,
        contentDisposition: response.headers.get('content-disposition') || '',
        contentType,
        downloadRequestId: response.headers.get('x-dcc-download-request-id') || '',
        text
      }
    },
    { requestUrl: url, requestHeaders: headers }
  )
}

function assertFilePayload(file, payload) {
  assert.equal(payload.code, 0, `detail API failed for ${file.id}: ${JSON.stringify(payload)}`)
  assert.ok(payload.data, `detail API returned empty data for ${file.id}`)
  assert.equal(String(payload.data.id), String(file.id))
  if (file.fileName) {
    assert.equal(payload.data.fileName, file.fileName)
  }
  if (file.versionNo) {
    assert.equal(payload.data.versionNo, file.versionNo)
  }
}

async function verifyPresentFile(page, headers, file) {
  await page.goto(`${BASE_URL}/dcc/controlled-file/detail/${file.id}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  const bodyText = await page.locator('body').innerText({ timeout: 30000 })
  assert.match(bodyText, new RegExp(file.fileName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  if (file.versionNo) {
    assert.match(bodyText, new RegExp(file.versionNo.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  const detail = await fetchJson(page, headers, `${BASE_URL}/admin-api/dcc/controlled-files/${file.id}`)
  assert.equal(detail.status, 200)
  assertFilePayload(file, detail.payload)

  const preview = await fetchJson(
    page,
    headers,
    `${BASE_URL}/admin-api/dcc/controlled-files/${file.id}/preview-metadata`
  )
  assert.equal(preview.status, 200)
  assert.equal(preview.payload.code, 0, `preview metadata failed for ${file.id}: ${JSON.stringify(preview.payload)}`)
  assert.ok(preview.payload.data?.viewerToken, `preview metadata missing viewer token for ${file.id}`)
  if (file.expectedSourceFileName) {
    assert.equal(preview.payload.data.fileName, file.expectedSourceFileName)
  }

  let download = null
  let downloadAccess = 'not-requested'
  if (file.expectDownload) {
    download = await downloadFile(page, headers, file.id)
    const downloadDenied =
      /json/i.test(download.contentType) &&
      download.text.includes('Current user cannot access this controlled file')
    if (downloadDenied && file.allowDownloadAccessDenied) {
      downloadAccess = 'denied'
    } else {
      assert.equal(download.status, 200)
      assert.ok(
        !/json/i.test(download.contentType),
        `DCC restore download returned JSON for ${file.id}: ${download.text || download.contentType}`
      )
      if (file.expectedSize) {
        assert.equal(download.size, Number(file.expectedSize))
      } else {
        assert.ok(download.size > 0, `downloaded file is empty for ${file.id}`)
      }
      if (file.expectedDownloadName) {
        assert.match(download.contentDisposition, new RegExp(file.expectedDownloadName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
      }
      downloadAccess = 'granted'
    }
  }
  return {
    id: String(detail.payload.data.id),
    fileName: detail.payload.data.fileName,
    versionNo: detail.payload.data.versionNo,
    canPreview: detail.payload.data.canPreview,
    previewKind: preview.payload.data.previewKind,
    previewFileName: preview.payload.data.fileName,
    downloadAccess,
    download
  }
}

async function verifyAbsentFile(page, headers, file) {
  const detail = await fetchJson(page, headers, `${BASE_URL}/admin-api/dcc/controlled-files/${file.id}`)
  assert.ok(
    detail.status === 404 || detail.payload?.code !== 0 || !detail.payload?.data,
    `expected file ${file.id} to be absent, got ${JSON.stringify(detail)}`
  )
  return detail
}

;(async () => {
  assertSafeDccBackupBoundary()
  assertRestoreExpectations()
  const browser = await chromium.launch({ headless: process.env.DCC_BACKUP_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()

  try {
    await login(page)
    const headers = await buildAuthHeaders(page)
    const present = []
    for (const file of PRESENT_FILES) {
      present.push(await verifyPresentFile(page, headers, file))
    }
    const absent = []
    for (const file of ABSENT_FILES) {
      absent.push(await verifyAbsentFile(page, headers, file))
    }
    console.log(
      `DCC_RESTORE_VERIFY_RESULT ${JSON.stringify(
        {
          baseUrl: BASE_URL,
          tenant: TENANT,
          present,
          absent
        },
        null,
        2
      )}`
    )
  } finally {
    await browser.close()
  }
})().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
