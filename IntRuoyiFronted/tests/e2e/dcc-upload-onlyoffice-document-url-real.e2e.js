const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')
const XLSX = require('xlsx')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_ID = process.env.DCC_UPLOAD_ONLYOFFICE_E2E_TASK_ID || '20260803-dcc-upload-onlyoffice-document-url'
const TARGET_PATH = '/dcc/controlled-file/upload'
const UNREADY_MESSAGE = 'OnlyOffice 预览地址未准备好'
const OUTPUT_DIR = process.env.DCC_UPLOAD_ONLYOFFICE_E2E_OUTPUT_DIR
  ? path.resolve(process.env.DCC_UPLOAD_ONLYOFFICE_E2E_OUTPUT_DIR)
  : path.join(WORKSPACE_ROOT, 'output', 'playwright', TASK_ID)
const EVIDENCE_PATH = path.join(OUTPUT_DIR, 'dcc-upload-onlyoffice-document-url-real-evidence.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'dcc-upload-onlyoffice-document-url-real.png')
const FIXTURE_PATH = path.join(OUTPUT_DIR, 'dcc-upload-onlyoffice-document-url-real.xlsx')
const PREFERRED_TAXONOMY_PATH = (
  process.env.DCC_UPLOAD_ONLYOFFICE_E2E_TAXONOMY_PATH ||
  '技术文档/设计和开发输入阶段/专利检索与分析报告（如适用）'
)
  .split('/')
  .map((item) => item.trim())
  .filter(Boolean)

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  const result = {}
  const content = fs.readFileSync(filePath, 'utf8')
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) {
      continue
    }
    const separatorIndex = trimmed.indexOf('=')
    if (separatorIndex <= 0) {
      continue
    }
    const key = trimmed.slice(0, separatorIndex).trim()
    let value = trimmed.slice(separatorIndex + 1).trim()
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1)
    }
    result[key] = value
  }
  return result
}

const baseEnv = readEnvFile(path.join(FRONTEND_ROOT, '.env'))
const localEnv = readEnvFile(path.join(FRONTEND_ROOT, '.env.local'))
const BASE_URL = (
  process.env.DCC_UPLOAD_ONLYOFFICE_E2E_BASE_URL ||
  `http://127.0.0.1:${localEnv.VITE_PORT || '8081'}`
).replace(/\/+$/, '')
const TENANT =
  process.env.DCC_UPLOAD_ONLYOFFICE_E2E_TENANT ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_TENANT ||
  ''
const USERNAME =
  process.env.DCC_UPLOAD_ONLYOFFICE_E2E_USERNAME ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_USERNAME ||
  ''
const PASSWORD =
  process.env.DCC_UPLOAD_ONLYOFFICE_E2E_PASSWORD ||
  baseEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD ||
  ''

function ensureOutputDir() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

function writeEvidence(evidence) {
  ensureOutputDir()
  fs.writeFileSync(EVIDENCE_PATH, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
}

function assertPrerequisites() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/, 'E2E must target local frontend only')
  assert.ok(TENANT, 'default login tenant is missing from env')
  assert.ok(USERNAME, 'default login username is missing from env')
  assert.ok(PASSWORD, 'default login password is missing from env')
  assert.ok(PREFERRED_TAXONOMY_PATH.length >= 3, 'preferred taxonomy path must contain at least 3 segments')
}

function createWorkbookFixture() {
  ensureOutputDir()
  const workbook = XLSX.utils.book_new()
  const worksheet = XLSX.utils.aoa_to_sheet([
    ['E2E Task', TASK_ID],
    ['Purpose', 'OnlyOffice upload preview document URL verification'],
    ['Created At', new Date().toISOString()]
  ])
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Preview')
  XLSX.writeFile(workbook, FIXTURE_PATH)
  return FIXTURE_PATH
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  })
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'commit',
    timeout: 60000
  })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}: ${loginPayload.msg || ''}`)
  const accessToken = loginPayload.data?.accessToken
  assert.ok(accessToken, 'login response must include accessToken')
  await page.waitForFunction(
    () => {
      const raw = window.localStorage.getItem('ACCESS_TOKEN')
      return Boolean(raw && raw !== 'null' && raw !== 'undefined')
    },
    undefined,
    { timeout: 60000 }
  )
  const tenantId = await page.waitForFunction(
    () => {
      const raw = window.localStorage.getItem('tenantId')
      return raw && raw !== 'null' && raw !== 'undefined' ? raw : false
    },
    undefined,
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/index`, { waitUntil: 'commit', timeout: 60000 })
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(unwrapCacheValue(await tenantId.jsonValue())),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
}

function unwrapCacheValue(raw) {
  if (!raw) {
    return ''
  }
  let current = raw
  for (let index = 0; index < 8; index += 1) {
    if (typeof current === 'string') {
      const trimmed = current.trim()
      if (!trimmed) {
        return ''
      }
      try {
        current = JSON.parse(trimmed)
        continue
      } catch {
        return trimmed.replace(/^"(.*)"$/, '$1')
      }
    }
    if (current && typeof current === 'object') {
      for (const key of ['accessToken', 'v', 'value', 'token', 'data']) {
        if (Object.prototype.hasOwnProperty.call(current, key)) {
          current = current[key]
          continue
        }
      }
    }
    return String(current || '')
  }
  return String(current || '')
}

async function buildAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < window.localStorage.length; index += 1) {
      const key = window.localStorage.key(index)
      result[key] = window.localStorage.getItem(key)
    }
    return result
  })
  const accessToken = unwrapCacheValue(snapshot.ACCESS_TOKEN)
  const tenantId = unwrapCacheValue(snapshot.tenantId)
  const visitTenantId = unwrapCacheValue(snapshot.visitTenantId)
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) {
    headers['visit-tenant-id'] = String(visitTenantId)
  }
  return headers
}

async function fetchReadonlyApi(page, headers, pathname, label) {
  const result = await page.evaluate(
    async ({ requestPath, requestHeaders }) => {
      const response = await fetch(requestPath, { headers: requestHeaders })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    { requestPath: pathname, requestHeaders: headers }
  )
  assert.equal(result.status, 200, `${label} HTTP ${result.status}`)
  assert.ok([0, 200].includes(result.payload?.code), `${label} business code ${result.payload?.code}: ${result.payload?.msg || ''}`)
  return result.payload.data
}

function buildTaxonomyPath(rows, taxonomyId) {
  const byId = new Map(rows.map((row) => [Number(row.id), row]))
  const pathRows = []
  let current = byId.get(Number(taxonomyId))
  const visited = new Set()
  while (current) {
    const currentId = Number(current.id)
    if (visited.has(currentId)) {
      throw new Error(`taxonomy cycle detected at ${currentId}`)
    }
    visited.add(currentId)
    pathRows.unshift(current)
    const parentId = Number(current.parentId || 0)
    current = parentId > 0 ? byId.get(parentId) : null
  }
  return pathRows
}

function samePath(left, right) {
  return left.length === right.length && left.every((item, index) => item === right[index])
}

function chooseExactUploadCandidate(categories, taxonomies) {
  const activeTaxonomies = taxonomies.filter((row) => row && row.active !== false && row.id)
  const matchingTaxonomies = activeTaxonomies.filter((taxonomy) =>
    samePath(
      buildTaxonomyPath(activeTaxonomies, taxonomy.id)
        .map((row) => String(row.name || '').trim())
        .filter(Boolean),
      PREFERRED_TAXONOMY_PATH
    )
  )
  assert.equal(
    matchingTaxonomies.length,
    1,
    `BLOCKER: expected exactly one active taxonomy path ${PREFERRED_TAXONOMY_PATH.join(' / ')}, found ${matchingTaxonomies.length}`
  )

  const taxonomy = matchingTaxonomies[0]
  const boundCategories = categories.filter(
    (category) => category && category.active !== false && Number(category.fileTypeTaxonomyId) === Number(taxonomy.id)
  )
  assert.equal(
    boundCategories.length,
    1,
    `BLOCKER: taxonomy leaf ${PREFERRED_TAXONOMY_PATH.join(' / ')} must bind exactly one active DCC file category, found ${boundCategories.length}`
  )
  const category = boundCategories[0]

  return {
    categoryId: Number(category.id),
    categoryName: category.name,
    categoryCode: category.code,
    taxonomyId: Number(taxonomy.id),
    taxonomyPath: PREFERRED_TAXONOMY_PATH,
    taxonomyLeafName: PREFERRED_TAXONOMY_PATH[PREFERRED_TAXONOMY_PATH.length - 1]
  }
}

function formItem(page, label) {
  return page
    .locator('.el-form-item')
    .filter({ has: page.locator('.el-form-item__label').filter({ hasText: label }) })
    .first()
}

async function selectCascaderPath(page, label, segments) {
  const item = formItem(page, label)
  await item.waitFor({ state: 'visible', timeout: 30000 })
  let lastError = null
  for (let attempt = 0; attempt < 4; attempt += 1) {
    try {
      await item.locator('.el-cascader').first().click()
      for (const [index, segment] of segments.entries()) {
        const node = page.locator('.el-cascader-node:visible').filter({ hasText: segment }).first()
        await node.waitFor({ state: 'visible', timeout: 15000 })
        if (index === segments.length - 1) {
          const selector = node.locator('.el-radio__input, .el-checkbox__input').first()
          if (await selector.count()) {
            await selector.click({ force: true })
          } else {
            await node.click()
          }
        } else {
          await node.click()
        }
        await page.waitForTimeout(250)
      }
      await page.keyboard.press('Escape').catch(() => undefined)
      await item.getByText(segments.join(' / '), { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
      return
    } catch (error) {
      lastError = error
      await page.keyboard.press('Escape').catch(() => undefined)
      await page.waitForTimeout(1000)
    }
  }
  throw lastError || new Error(`failed to select cascader path ${segments.join(' / ')}`)
}

function sanitizeDocumentUrl(value) {
  assert.ok(value, 'upload-preview response must include onlyofficeDocumentUrl')
  const parsed = new URL(value)
  return {
    origin: parsed.origin,
    pathname: parsed.pathname,
    hasToken: Boolean(parsed.searchParams.get('token')),
    hasUploadPreviewPath: parsed.pathname.includes('/dcc/controlled-files/upload-preview/')
  }
}

function requestPathname(requestOrResponse) {
  try {
    return new URL(requestOrResponse.url()).pathname
  } catch {
    return ''
  }
}

function isAllowedTemporaryUploadWrite(pathname) {
  return [
    '/admin-api/dcc/controlled-files/upload-preview',
    '/admin-api/dcc/controlled-files/upload-temporary/session-cleanup'
  ].includes(pathname)
}

;(async () => {
  assertPrerequisites()
  const fixturePath = createWorkbookFixture()
  const evidence = {
    taskId: TASK_ID,
    startedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    tenantLabel: `${TENANT}/${USERNAME}`,
    targetPath: TARGET_PATH,
    taxonomyPath: PREFERRED_TAXONOMY_PATH,
    selectedCandidate: null,
    fixtureName: path.basename(fixturePath),
    uploadPreview: null,
    cleanup: null,
    dccWriteRequests: [],
    targetNetworkFailures: [],
    consoleErrors: [],
    pageErrors: [],
    screenshot: SCREENSHOT_PATH
  }

  const browser = await chromium.launch({ headless: process.env.DCC_UPLOAD_ONLYOFFICE_E2E_HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  page.on('console', (message) => {
    if (message.type() === 'error') {
      evidence.consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => {
    evidence.pageErrors.push(error.message)
  })
  page.on('request', (request) => {
    const pathname = requestPathname(request)
    if (pathname.startsWith('/admin-api/dcc/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      evidence.dccWriteRequests.push({ method: request.method(), pathname })
    }
  })
  page.on('response', async (response) => {
    const pathname = requestPathname(response)
    if (!pathname.startsWith('/admin-api/dcc/')) {
      return
    }
    if (response.status() >= 400) {
      evidence.targetNetworkFailures.push({ method: response.request().method(), pathname, status: response.status() })
    }
  })

  try {
    const authHeaders = await login(page)
    const [categories, taxonomies] = await Promise.all([
      fetchReadonlyApi(page, authHeaders, '/admin-api/dcc/file-categories', 'file categories'),
      fetchReadonlyApi(page, authHeaders, '/admin-api/dcc/file-type-taxonomies', 'file type taxonomies')
    ])
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)

    const candidate = chooseExactUploadCandidate(categories || [], taxonomies || [])
    evidence.selectedCandidate = candidate

    const directoryResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/upload-directory-tree') &&
        response.url().includes(`categoryId=${candidate.categoryId}`) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    ).catch((error) => error)
    await selectCascaderPath(page, '文件分类', candidate.taxonomyPath)
    const directoryResponse = await directoryResponsePromise
    if (directoryResponse instanceof Error) {
      throw directoryResponse
    }
    const directoryPayload = await directoryResponse.json()
    assert.ok([0, 200].includes(directoryPayload.code), `directory tree business code ${directoryPayload.code}: ${directoryPayload.msg || ''}`)

    const categoryItem = formItem(page, '文件类别')
    await categoryItem.locator('[data-testid="dcc-upload-category-leaf-display"]').first().waitFor({ state: 'visible', timeout: 30000 })
    const categoryText = await categoryItem.innerText()
    assert.ok(categoryText.includes(candidate.taxonomyLeafName), `readonly file category must show taxonomy leaf ${candidate.taxonomyLeafName}`)
    assert.equal(await categoryItem.locator('.el-select').count(), 0, 'controlled upload file category must not render an editable select')

    const uploadResponsePromise = page.waitForResponse(
      (response) =>
        requestPathname(response) === '/admin-api/dcc/controlled-files/upload-preview' &&
        response.request().method() === 'POST',
      { timeout: 90000 }
    )
    await page.locator('input[type="file"]').first().setInputFiles(fixturePath)
    await page.getByText(path.basename(fixturePath), { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    const uploadResponse = await uploadResponsePromise
    assert.equal(uploadResponse.ok(), true, `upload-preview HTTP ${uploadResponse.status()}`)
    const uploadPayload = await uploadResponse.json()
    assert.ok([0, 200].includes(uploadPayload.code), `upload-preview business code ${uploadPayload.code}: ${uploadPayload.msg || ''}`)
    const uploadData = uploadPayload.data || {}
    const sanitizedDocumentUrl = sanitizeDocumentUrl(uploadData.onlyofficeDocumentUrl)
    assert.ok(uploadData.onlyofficeBaseUrl, 'upload-preview response must include onlyofficeBaseUrl')
    assert.equal(Object.prototype.hasOwnProperty.call(uploadData, 'fileId'), false, 'upload-preview response must not expose raw fileId')
    assert.equal(sanitizedDocumentUrl.hasToken, true, 'onlyofficeDocumentUrl must include a signed token')
    assert.equal(sanitizedDocumentUrl.hasUploadPreviewPath, true, 'onlyofficeDocumentUrl must target upload-preview download path')
    evidence.uploadPreview = {
      code: uploadPayload.code,
      fileName: uploadData.fileName,
      previewKind: uploadData.previewKind,
      hasOnlyOfficeBaseUrl: Boolean(uploadData.onlyofficeBaseUrl),
      onlyofficeDocumentUrl: sanitizedDocumentUrl,
      exposesRawFileId: Object.prototype.hasOwnProperty.call(uploadData, 'fileId')
    }

    await page.getByText('提交前预览', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    await page.waitForTimeout(2500)
    const bodyText = await page.locator('body').innerText()
    assert.ok(!bodyText.includes(UNREADY_MESSAGE), `page must not show ${UNREADY_MESSAGE}`)
    const previewErrorText = await page.locator('[data-testid="dcc-upload-preview-error"]').innerText().catch(() => '')
    assert.equal(previewErrorText, '', `upload preview error must be empty: ${previewErrorText}`)

    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })

    const cleanupResponsePromise = page.waitForResponse(
      (response) =>
        requestPathname(response) === '/admin-api/dcc/controlled-files/upload-temporary/session-cleanup' &&
        response.request().method() === 'POST',
      { timeout: 30000 }
    )
    const uploadListItem = page.locator('.el-upload-list__item').filter({ hasText: path.basename(fixturePath) }).first()
    await uploadListItem.waitFor({ state: 'visible', timeout: 30000 })
    await uploadListItem.hover()
    const removeControl = uploadListItem.locator('.el-upload-list__item-delete, .el-icon--close').first()
    await removeControl.click({ force: true })
    const cleanupResponse = await cleanupResponsePromise
    const cleanupPayload = await cleanupResponse.json()
    evidence.cleanup = {
      code: cleanupPayload.code,
      cleanedCount: cleanupPayload.data?.cleanedCount ?? null,
      cleanupStatus: cleanupPayload.data?.cleanupStatus || null
    }
    assert.ok([0, 200].includes(cleanupPayload.code), `cleanup business code ${cleanupPayload.code}: ${cleanupPayload.msg || ''}`)

    const disallowedWrites = evidence.dccWriteRequests.filter((request) => !isAllowedTemporaryUploadWrite(request.pathname))
    assert.deepEqual(disallowedWrites, [], 'E2E must not submit or mutate formal DCC controlled-file records')
    assert.deepEqual(evidence.targetNetworkFailures, [], 'target DCC network requests must not fail')
    assert.deepEqual(evidence.pageErrors, [], 'page must not throw runtime errors')

    evidence.finishedAt = new Date().toISOString()
    evidence.status = 'PASS'
    writeEvidence(evidence)
    console.log(`PASS: DCC upload OnlyOffice document URL real E2E evidence=${EVIDENCE_PATH}`)
  } catch (error) {
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true }).catch(() => undefined)
    evidence.finishedAt = new Date().toISOString()
    evidence.status = 'FAIL'
    evidence.error = error.message
    writeEvidence(evidence)
    throw error
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
  }
})().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
