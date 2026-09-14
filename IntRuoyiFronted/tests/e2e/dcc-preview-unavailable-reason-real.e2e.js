const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const taskId = '20260803-dcc-preview-unavailable-e2e'
const evidenceDir = path.join(workspaceRoot, 'doc', 'tasks', taskId)
const evidencePath = path.join(evidenceDir, 'real-e2e-result.json')
const screenshotDir = path.join(evidenceDir, 'screenshots')
const dotEnv = readDotEnv(path.join(repoRoot, '.env'))

const previewKinds = ['PDF', 'IMAGE', 'VIDEO', 'AUDIO', 'TEXT', 'DOWNLOAD_ONLY', 'OFFICE']
const baseUrl = (process.env.DCC_PREVIEW_UNAVAILABLE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, '')
const config = {
  baseUrl,
  tenant:
    process.env.DCC_PREVIEW_UNAVAILABLE_E2E_TENANT ||
    dotEnv.VITE_APP_DEFAULT_LOGIN_TENANT ||
    '',
  username:
    process.env.DCC_PREVIEW_UNAVAILABLE_E2E_USERNAME ||
    dotEnv.VITE_APP_DEFAULT_LOGIN_USERNAME ||
    '',
  password:
    process.env.DCC_PREVIEW_UNAVAILABLE_E2E_PASSWORD ||
    dotEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD ||
    '',
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || chromium.executablePath(),
  timeout: Number(process.env.DCC_PREVIEW_UNAVAILABLE_E2E_TIMEOUT || 90000)
}

const evidence = {
  taskId,
  startedAt: new Date().toISOString(),
  baseUrl: config.baseUrl,
  tenant: config.tenant,
  username: config.username,
  previewKinds,
  status: 'FAIL',
  cases: [],
  metadataResponses: [],
  binaryRequests: [],
  dccWriteRequests: [],
  targetNetworkFailures: [],
  consoleErrors: [],
  pageErrors: []
}

function readDotEnv(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  const result = {}
  const source = fs.readFileSync(filePath, 'utf8')
  for (const rawLine of source.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) {
      continue
    }
    const separatorIndex = line.indexOf('=')
    if (separatorIndex < 0) {
      continue
    }
    const key = line.slice(0, separatorIndex).trim()
    let value = line.slice(separatorIndex + 1).trim()
    value = value.replace(/^['"]|['"]$/g, '').trim()
    result[key] = value
  }
  return result
}

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function assertPrerequisites() {
  const parsedBaseUrl = new URL(config.baseUrl)
  assert.match(
    parsedBaseUrl.hostname,
    /^(localhost|127\.0\.0\.1)$/,
    `E2E must stay on local frontend, got ${config.baseUrl}`
  )
  assert.ok(config.tenant, 'login tenant is required from env or .env')
  assert.ok(config.username, 'login username is required from env or .env')
  assert.ok(config.password, 'login password is required from env or .env')
  assert.ok(
    fs.existsSync(config.executablePath),
    `Playwright Chromium executable is missing: ${config.executablePath}`
  )
}

async function settle(page, timeout = 500) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined)
  await page.waitForTimeout(timeout)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible ${label} input`)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (!(await tenantInput.count())) {
    await fillFirstVisible(form.locator('input.el-input__inner').first(), config.tenant, 'tenant')
    return
  }
  await tenantInput.fill(config.tenant)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
  await option.waitFor({ state: 'visible', timeout: config.timeout })
  await option.click()
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/dcc/controlled-file/browser')}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form)
  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(
    form.locator('input[type="password"], input[placeholder="请输入密码"]'),
    config.password,
    'password'
  )
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok(
    loginPayload && [0, 200].includes(loginPayload.code),
    `login business code ${loginPayload && loginPayload.code}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, {
    timeout: config.timeout
  })
}

function isTargetPreviewMetadataRequest(url) {
  return /\/admin-api\/dcc\/controlled-files\/[^/]+\/preview-metadata(?:[?#]|$)/.test(url)
}

function isTargetPreviewBinaryRequest(url) {
  return /\/admin-api\/dcc\/controlled-files\/[^/]+\/preview(?:[?#]|$)/.test(url)
}

function withCacheBust(url, kind) {
  const parsed = new URL(url)
  parsed.searchParams.set('__e2ePreviewKind', kind)
  parsed.searchParams.set('__e2eTs', String(Date.now()))
  return parsed.toString()
}

function resolveUnavailableReason(kind) {
  return `E2E-PREVIEW-UNAVAILABLE-${kind}-20260803`
}

async function installPreviewMetadataFixture(context, state) {
  await context.route('**/admin-api/dcc/controlled-files/*/preview-metadata**', async (route) => {
    const kind = state.currentKind
    const reason = resolveUnavailableReason(kind)
    const originalResponse = await route.fetch()
    const originalPayload = await originalResponse.json().catch(() => null)
    if (!originalResponse.ok() || !originalPayload || ![0, 200].includes(originalPayload.code)) {
      evidence.metadataResponses.push({
        kind,
        url: route.request().url(),
        status: originalResponse.status(),
        businessCode: originalPayload && originalPayload.code,
        fixtureApplied: false
      })
      await route.fulfill({ response: originalResponse })
      return
    }
    const originalData = originalPayload.data || {}
    const patchedPayload = {
      ...originalPayload,
      data: {
        ...originalData,
        previewKind: kind,
        previewUnavailableReason: reason
      }
    }
    evidence.metadataResponses.push({
      kind,
      url: route.request().url(),
      status: originalResponse.status(),
      businessCode: originalPayload.code,
      originalPreviewKind: originalData.previewKind,
      fixtureApplied: true,
      reason
    })
    await route.fulfill({
      response: originalResponse,
      contentType: 'application/json;charset=utf-8',
      body: JSON.stringify(patchedPayload)
    })
  })
}

async function openViewerFromControlledBrowser(context, page) {
  await page.goto(`${config.baseUrl}/dcc/controlled-file/browser?scope=global&pageNo=1&pageSize=10`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await settle(page, 1000)
  const previewLinks = page.locator('.browser-file-name--link:visible')
  await previewLinks.first().waitFor({ state: 'visible', timeout: config.timeout })
  const clickedFileName = (await previewLinks.first().innerText()).trim()
  const popupPromise = context.waitForEvent('page', { timeout: 8000 }).catch(() => null)
  await previewLinks.first().click()
  const viewerPage = (await popupPromise) || page
  await viewerPage.waitForLoadState('domcontentloaded', { timeout: config.timeout })
  await viewerPage.waitForURL(
    (url) => url.pathname.includes('/dcc/controlled-file/detail/') && url.searchParams.get('viewer') === '1',
    { timeout: config.timeout }
  )
  return { viewerPage, clickedFileName, viewerUrl: viewerPage.url() }
}

async function assertUnavailableCase(viewerPage, kind, targetUrl) {
  const reason = resolveUnavailableReason(kind)
  const metadataCountBefore = evidence.metadataResponses.length
  const binaryCountBefore = evidence.binaryRequests.length
  await viewerPage.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await viewerPage.waitForFunction((text) => document.body.innerText.includes(text), reason, {
    timeout: config.timeout
  })
  await settle(viewerPage, 1000)
  const bodyText = await viewerPage.locator('body').innerText()
  assert.ok(bodyText.includes(reason), `${kind} must display exact previewUnavailableReason`)
  assert.ok(
    evidence.metadataResponses.length > metadataCountBefore,
    `${kind} must request preview metadata before rendering`
  )
  const caseMetadata = evidence.metadataResponses
    .slice(metadataCountBefore)
    .filter((item) => item.kind === kind)
  assert.ok(caseMetadata.some((item) => item.fixtureApplied), `${kind} metadata fixture must be applied`)
  const caseBinaryRequests = evidence.binaryRequests.slice(binaryCountBefore).filter((item) => item.kind === kind)
  assert.equal(caseBinaryRequests.length, 0, `${kind} must not request preview binary after unavailable reason`)
  if (kind === 'DOWNLOAD_ONLY') {
    assert.equal(
      bodyText.includes('仅支持下载'),
      false,
      'DOWNLOAD_ONLY must not overwrite previewUnavailableReason with download-only empty state'
    )
  }
  const screenshotPath = path.join(screenshotDir, `preview-unavailable-${kind.toLowerCase()}.png`)
  await viewerPage.screenshot({ path: screenshotPath, fullPage: true })
  evidence.cases.push({
    kind,
    reason,
    metadataCount: caseMetadata.length,
    binaryRequestCount: caseBinaryRequests.length,
    screenshotPath
  })
}

async function main() {
  assertPrerequisites()
  fs.mkdirSync(screenshotDir, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const state = { currentKind: previewKinds[0] }
  try {
    const context = await browser.newContext({ viewport: { width: 1680, height: 960 }, locale: 'zh-CN' })
    await installPreviewMetadataFixture(context, state)
    context.on('request', (request) => {
      const url = request.url()
      const method = request.method()
      if (isTargetPreviewBinaryRequest(url)) {
        evidence.binaryRequests.push({ kind: state.currentKind, method, url })
      }
      if (url.includes('/admin-api/dcc/') && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        evidence.dccWriteRequests.push({ kind: state.currentKind, method, url })
      }
    })
    context.on('requestfailed', (request) => {
      const url = request.url()
      if (url.includes('/admin-api/dcc/controlled-files/')) {
        evidence.targetNetworkFailures.push({
          kind: state.currentKind,
          method: request.method(),
          url,
          failure: request.failure()
        })
      }
    })
    const page = await context.newPage()
    page.setDefaultTimeout(config.timeout)
    page.setDefaultNavigationTimeout(config.timeout)
    for (const pageLike of [page]) {
      pageLike.on('console', (message) => {
        if (['error'].includes(message.type())) {
          evidence.consoleErrors.push({ type: message.type(), text: message.text() })
        }
      })
      pageLike.on('pageerror', (error) => {
        evidence.pageErrors.push({ message: error.message, stack: error.stack })
      })
    }

    await login(page)
    const opened = await openViewerFromControlledBrowser(context, page)
    evidence.clickedFileName = opened.clickedFileName
    evidence.viewerUrl = opened.viewerUrl
    opened.viewerPage.on('console', (message) => {
      if (['error'].includes(message.type())) {
        evidence.consoleErrors.push({ type: message.type(), text: message.text() })
      }
    })
    opened.viewerPage.on('pageerror', (error) => {
      evidence.pageErrors.push({ message: error.message, stack: error.stack })
    })

    await assertUnavailableCase(opened.viewerPage, previewKinds[0], opened.viewerUrl)
    for (const kind of previewKinds.slice(1)) {
      state.currentKind = kind
      await assertUnavailableCase(opened.viewerPage, kind, withCacheBust(opened.viewerUrl, kind))
    }
    assert.deepEqual(evidence.dccWriteRequests, [], 'read-only preview E2E must not send DCC write requests')
    assert.deepEqual(evidence.targetNetworkFailures, [], 'target DCC preview network failures must be empty')
    evidence.status = 'PASS'
    evidence.finishedAt = new Date().toISOString()
    writeJson(evidencePath, evidence)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = {
      message: error.message,
      stack: error.stack
    }
    evidence.finishedAt = new Date().toISOString()
    writeJson(evidencePath, evidence)
    throw error
  } finally {
    await browser.close().catch(() => undefined)
  }
}

main()
  .then(() => {
    console.log(`PASS: DCC preview unavailable reason real E2E -> ${evidencePath}`)
  })
  .catch((error) => {
    console.error(error)
    process.exit(1)
  })
