const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_PERSONAL_FILE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_PERSONAL_FILE_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_PERSONAL_FILE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_PERSONAL_FILE_E2E_PASSWORD
const BROWSER_PATH = '/dcc/controlled-file/browser?scope=global&pageNo=1&pageSize=10'
const OLD_PERSONAL_FILE_PATH = '/dcc/controlled-file/mine'
const OUTPUT_DIR = path.resolve(process.cwd(), 'output', 'playwright')
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'dcc-personal-file-decommission-real')
const CHROME_EXECUTABLE = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function writeResult(payload) {
  writeJson(path.join(RESULT_DIR, 'real-e2e-result.json'), payload)
}

function assertPrerequisites() {
  const base = new URL(BASE_URL)
  assert.match(base.hostname, /^(localhost|127\.0\.0\.1)$/, `real E2E must stay local, got ${BASE_URL}`)
  assert.equal(TENANT, '测试租户', 'real E2E must use 测试租户')
  assert.equal(USERNAME, 'aoteman', 'real E2E must use aoteman')
  assert.ok(PASSWORD, 'DCC_PERSONAL_FILE_E2E_PASSWORD is required')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 20000 }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function login(page) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', BROWSER_PATH)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 15000 })
    await option.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
}

function assertNoPersonalFileContractText(source, label) {
  const forbiddenTokens = ['controlled-file/mine', 'DccControlledFileMine', '个人文件', '我的文件']
  for (const token of forbiddenTokens) {
    assert.equal(String(source).includes(token), false, `${label} must not contain obsolete token: ${token}`)
  }
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
    return unwrap(JSON.parse(raw))
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
  assert.ok(accessToken, 'final API verification requires ACCESS_TOKEN from browser storage')
  assert.ok(tenantId, 'final API verification requires tenant-id from browser storage')
  const headers = {
    Authorization: String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) {
    headers['visit-tenant-id'] = String(visitTenantId)
  }
  return headers
}

async function requestJson(page, apiPath) {
  const headers = await buildAuthHeaders(page)
  return await page.evaluate(
    async ({ targetUrl, requestHeaders }) => {
      const response = await fetch(targetUrl, { method: 'GET', headers: requestHeaders })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, ok: response.ok, payload }
    },
    { targetUrl: `${BASE_URL}${apiPath}`, requestHeaders: headers }
  )
}

function assertPageResult(payload, label) {
  assert.ok([0, 200].includes(payload.code), `${label} business code ${payload.code}`)
  const rows = payload.data?.list || []
  assert.ok((payload.data?.total || 0) > 0, `${label} must return real controlled-file rows`)
  assert.ok(rows.length > 0, `${label} visible page rows are required`)
  assert.ok(
    rows.some((row) => row.canPreview || (row.versionHistory || []).some((version) => version.canPreview)),
    `${label} must include at least one previewable controlled file`
  )
  return rows
}

function waitForContextResponse(context, predicate, label) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      context.off('response', onResponse)
      reject(new Error(`${label} response timed out`))
    }, 60000)
    async function onResponse(response) {
      try {
        if (!predicate(response)) {
          return
        }
        clearTimeout(timer)
        context.off('response', onResponse)
        resolve(response)
      } catch (error) {
        clearTimeout(timer)
        context.off('response', onResponse)
        reject(error)
      }
    }
    context.on('response', onResponse)
  })
}

async function waitForPreviewResponse(collectedResponses, accessEventCode) {
  const matched = () =>
    collectedResponses.find(
      (response) =>
        response.request().headers()['x-dcc-access-event-code'] === accessEventCode ||
        response.headers()['x-dcc-access-event-code'] === accessEventCode
    )
  const existing = matched()
  if (existing) {
    return existing
  }
  return await new Promise((resolve, reject) => {
    const startedAt = Date.now()
    const timer = setInterval(() => {
      const response = matched()
      if (response) {
        clearInterval(timer)
        resolve(response)
        return
      }
      if (Date.now() - startedAt > 60000) {
        clearInterval(timer)
        reject(new Error('controlled preview response timed out'))
      }
    }, 250)
  })
}

async function fetchControlledDocument(page, documentUrl) {
  const parsedUrl = new URL(documentUrl)
  const sameOriginUrl = `${BASE_URL}${parsedUrl.pathname}${parsedUrl.search}`
  return await page.evaluate(async ({ targetUrl }) => {
    const response = await fetch(targetUrl, { method: 'GET' })
    const headers = {}
    response.headers.forEach((value, key) => {
      headers[key.toLowerCase()] = value
    })
    const buffer = await response.arrayBuffer()
    return {
      ok: response.ok,
      status: response.status,
      headers,
      byteLength: buffer.byteLength
    }
  }, { targetUrl: sameOriginUrl })
}

async function main() {
  assertPrerequisites()
  const browser = await chromium.launch({
    headless: true,
    executablePath: CHROME_EXECUTABLE || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const writeRequests = []
  const oldPersonalApiRequests = []
  const evidence = {
    status: 'FAIL',
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME
  }
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const previewResponses = []
    context.on('request', (request) => {
      const method = request.method()
      const url = request.url()
      if (url.includes('/admin-api/dcc/controlled-files/page')) {
        oldPersonalApiRequests.push({ method, url })
      }
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && url.includes('/admin-api/dcc/')) {
        writeRequests.push({ method, url })
      }
    })
    context.on('response', (response) => {
      const url = response.url()
      if (
        url.includes('/dcc/controlled-files/') &&
        url.includes('/preview') &&
        !url.includes('/preview-metadata') &&
        response.request().method() === 'GET'
      ) {
        previewResponses.push(response)
      }
    })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)
    const permissionInfoProbe = await requestJson(page, '/admin-api/system/auth/get-permission-info')
    assert.equal(permissionInfoProbe.status, 200, `permission info HTTP ${permissionInfoProbe.status}`)
    assert.ok(
      [0, 200].includes(permissionInfoProbe.payload?.code),
      `permission info business code ${permissionInfoProbe.payload?.code}`
    )
    const permissionInfoPayload = permissionInfoProbe.payload
    assertNoPersonalFileContractText(JSON.stringify(permissionInfoPayload), 'permission menu payload')

    await settle(page)
    const currentBodyText = await page.locator('body').innerText()
    assertNoPersonalFileContractText(currentBodyText, 'current visible UI')

    await page.goto(`${BASE_URL}${OLD_PERSONAL_FILE_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page)
    const oldRouteBodyText = await page.locator('body').innerText()
    assertNoPersonalFileContractText(oldRouteBodyText, 'obsolete personal-file route UI')
    assert.equal(oldPersonalApiRequests.length, 0, 'obsolete personal-file route must not call old personal-file page API')

    const obsoleteApiProbe = await requestJson(page, '/admin-api/dcc/controlled-files/page?pageNo=1&pageSize=1')
    assert.equal(
      obsoleteApiProbe.status === 200 && [0, 200].includes(obsoleteApiProbe.payload?.code),
      false,
      'obsolete personal-file backend page API must not remain available'
    )

    await page.goto(`${BASE_URL}${BROWSER_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page)
    const previewLinks = page.locator('.browser-file-name--link')
    await previewLinks.first().waitFor({ state: 'visible', timeout: 60000 })
    const browserPageProbe = await requestJson(
      page,
      '/admin-api/dcc/controlled-files/browser-page?pageNo=1&pageSize=10'
    )
    assert.equal(browserPageProbe.status, 200, `browser-page HTTP ${browserPageProbe.status}`)
    const browserPagePayload = browserPageProbe.payload
    const rows = assertPageResult(browserPagePayload, 'controlled browser page')
    const clickedFileName = (await previewLinks.first().innerText()).trim()

    const metadataResponsePromise = waitForContextResponse(
      context,
      (response) =>
        response.url().includes('/dcc/controlled-files/') &&
        response.url().includes('/preview-metadata') &&
        response.request().method() === 'GET',
      'preview metadata'
    )
    const [viewerPage] = await Promise.all([context.waitForEvent('page'), previewLinks.first().click()])
    await viewerPage.waitForLoadState('domcontentloaded', { timeout: 60000 })
    const metadataResponse = await metadataResponsePromise
    assert.equal(metadataResponse.ok(), true, `preview metadata HTTP ${metadataResponse.status()}`)
    const metadataPayload = await metadataResponse.json()
    assert.ok([0, 200].includes(metadataPayload.code), `preview metadata business code ${metadataPayload.code}`)
    const metadata = metadataPayload.data || {}
    assert.ok(metadata.accessEventCode, 'preview metadata must include accessEventCode')
    assert.ok(metadata.watermarkTraceCode, 'preview metadata must include watermarkTraceCode')
    assert.ok(metadata.viewerToken, 'preview metadata must include viewerToken')
    assert.notEqual(metadata.previewKind, 'DOWNLOAD_ONLY', 'selected controlled file must support online preview')

    let controlledPreviewEvidence
    if (metadata.previewKind === 'OFFICE') {
      assert.ok(metadata.onlyofficeDocumentUrl, 'Office preview must include controlled document URL')
      await viewerPage.getByText('禁止截图/外传').first().waitFor({ state: 'visible', timeout: 60000 })
      const officeFetch = await fetchControlledDocument(viewerPage, metadata.onlyofficeDocumentUrl)
      assert.equal(officeFetch.ok, true, `OnlyOffice controlled document HTTP ${officeFetch.status}`)
      assert.ok(officeFetch.byteLength > 0, 'OnlyOffice controlled document must return bytes')
      controlledPreviewEvidence = {
        kind: 'OFFICE',
        status: officeFetch.status,
        byteLength: officeFetch.byteLength
      }
    } else {
      const previewResponse = await waitForPreviewResponse(previewResponses, metadata.accessEventCode)
      assert.equal(previewResponse.ok(), true, `controlled preview HTTP ${previewResponse.status()}`)
      assert.equal(
        previewResponse.request().headers()['x-dcc-access-event-code'],
        metadata.accessEventCode,
        'preview request must carry backend-issued accessEventCode'
      )
      assert.ok(previewResponse.headers()['x-dcc-preview-watermark'], 'preview response must expose watermark header')
      assert.equal(
        previewResponse.headers()['x-dcc-access-event-code'],
        metadata.accessEventCode,
        'preview response must echo accessEventCode'
      )
      await viewerPage.locator('[data-testid="protected-preview-badge"]').first().waitFor({
        state: 'visible',
        timeout: 60000
      })
      controlledPreviewEvidence = {
        kind: metadata.previewKind,
        status: previewResponse.status(),
        watermarkHeader: previewResponse.headers()['x-dcc-preview-watermark']
      }
    }
    fs.mkdirSync(OUTPUT_DIR, { recursive: true })
    const screenshotPath = path.join(OUTPUT_DIR, 'dcc-personal-file-decommission-real-preview.png')
    await viewerPage.screenshot({ path: screenshotPath, fullPage: true })

    const auditProbe = await requestJson(
      viewerPage,
      `/admin-api/dcc/controlled-file-audits/page?pageNo=1&pageSize=10&accessEventCode=${encodeURIComponent(
        metadata.accessEventCode
      )}`
    )
    assert.equal(auditProbe.status, 200, `audit API HTTP ${auditProbe.status}`)
    assert.ok([0, 200].includes(auditProbe.payload?.code), `audit API business code ${auditProbe.payload?.code}`)
    const auditRows = auditProbe.payload?.data?.list || []
    assert.ok(auditRows.length > 0, 'audit API must return the access event record')
    assert.ok(
      auditRows.some((row) => row.accessEventCode === metadata.accessEventCode && row.result === 'SUCCESS'),
      'audit API must include successful access record for the preview event'
    )

    assert.deepEqual(writeRequests, [], 'read-only E2E must not send DCC write requests')
    evidence.status = 'PASS'
    evidence.browserPath = BROWSER_PATH
    evidence.oldPersonalFilePath = OLD_PERSONAL_FILE_PATH
    evidence.initialTotal = browserPagePayload.data.total
    evidence.initialRows = rows.length
    evidence.clickedFileName = clickedFileName
    evidence.previewKind = metadata.previewKind
    evidence.accessEventCode = metadata.accessEventCode
    evidence.watermarkTraceCode = metadata.watermarkTraceCode
    evidence.controlledPreviewEvidence = controlledPreviewEvidence
    evidence.auditRows = auditRows.map((row) => ({
      id: row.id,
      controlledFileId: row.controlledFileId,
      fileNumber: row.fileNumber,
      actionType: row.actionType,
      purpose: row.purpose,
      result: row.result,
      occurredAt: row.occurredAt
    }))
    evidence.obsoleteApiProbe = {
      status: obsoleteApiProbe.status,
      code: obsoleteApiProbe.payload?.code,
      message: obsoleteApiProbe.payload?.msg || obsoleteApiProbe.payload?.message || obsoleteApiProbe.payload?.raw
    }
    evidence.writeRequests = writeRequests
    evidence.screenshotPath = screenshotPath
    writeResult(evidence)
    console.log(
      `PASS: DCC personal-file decommission real E2E, rows=${rows.length}, accessEventCode=${metadata.accessEventCode}`
    )
  } catch (error) {
    evidence.error = error && error.message ? error.message : String(error)
    evidence.writeRequests = writeRequests
    evidence.oldPersonalApiRequests = oldPersonalApiRequests
    writeResult(evidence)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
