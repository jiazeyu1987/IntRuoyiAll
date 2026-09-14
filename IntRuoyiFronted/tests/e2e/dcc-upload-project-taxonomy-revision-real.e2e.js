const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_UPLOAD_TAXONOMY_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.DCC_UPLOAD_TAXONOMY_E2E_TEST_PASSWORD
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.DCC_UPLOAD_TAXONOMY_E2E_ADMIN_PASSWORD
const SOURCE_FILE =
  process.env.DCC_UPLOAD_TAXONOMY_E2E_SOURCE_FILE ||
  'D:\\ProjectPackage\\Int\\IntAuth\\fronted\\node_modules\\mammoth\\test\\test-data\\empty.docx'

const ADMIN_PROJECT = {
  keyword: 'AC',
  optionText: '一次性使用造影导管',
  id: 168,
  code: 'AC'
}

const DEFAULT_ADMIN_TAXONOMY_PATH = ['技术文档', '设计和开发输出阶段', '生产图纸']

const TEST_PROJECT = {
  keyword: 'IKFDA',
  optionText: '一次性使用导管鞘套装（FDA)',
  id: 124,
  code: 'IKFDA'
}

const DEFAULT_TEST_TAXONOMY_PATH = ['技术文档', '设计和开发输入阶段', 'Codex输入E2E叶子']

const CATEGORY_NAME = process.env.DCC_UPLOAD_TAXONOMY_E2E_CATEGORY_NAME || 'Codex Local DCC Category'
const ADMIN_TAXONOMY_PATH = parseTaxonomyPathEnv(
  'DCC_UPLOAD_TAXONOMY_E2E_ADMIN_TAXONOMY_PATH',
  DEFAULT_ADMIN_TAXONOMY_PATH
)
const TEST_TAXONOMY_PATH = parseTaxonomyPathEnv(
  'DCC_UPLOAD_TAXONOMY_E2E_TEST_TAXONOMY_PATH',
  DEFAULT_TEST_TAXONOMY_PATH
)

function parseTaxonomyPathEnv(envName, defaultPath) {
  const raw = process.env[envName]
  if (!raw) {
    return defaultPath
  }
  const segments = raw.split('/').map((item) => item.trim()).filter(Boolean)
  assert.ok(segments.length >= 3, `${envName} must contain at least three slash-separated taxonomy segments`)
  return segments
}

function todayString() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function runId() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

function assertPrerequisites() {
  assert.match(new URL(BASE_URL).hostname, /^(localhost|127\.0\.0\.1)$/, 'E2E must target local frontend')
  assert.ok(fs.existsSync(SOURCE_FILE), `source file missing: ${SOURCE_FILE}`)
  assert.ok(TEST_PASSWORD, 'DCC_UPLOAD_TAXONOMY_E2E_TEST_PASSWORD is required')
  if (process.env.DCC_UPLOAD_TAXONOMY_E2E_SKIP_ADMIN_READONLY !== '1') {
    assert.ok(ADMIN_PASSWORD, 'DCC_UPLOAD_TAXONOMY_E2E_ADMIN_PASSWORD is required when admin readonly probe is enabled')
  }
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function login(page, tenant, username, password) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'commit', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    await form.getByText(tenant, { exact: false }).first().waitFor({ state: 'visible', timeout: 10000 })
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)

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
}

function formItem(page, label) {
  return page
    .locator('.el-form-item')
    .filter({ has: page.locator('.el-form-item__label').filter({ hasText: label }) })
    .first()
}

async function selectElOption(page, label, keyword, optionText) {
  const item = formItem(page, label)
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await item.locator('.el-select').first().click()
  const input = item.locator('input').first()
  await input.fill(keyword)
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function selectCategory(page, categoryName) {
  const item = formItem(page, '文件类别')
  await item.locator('.el-select').first().click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: categoryName }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function selectCascaderPath(page, label, segments) {
  const item = formItem(page, label)
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await item.locator('.el-cascader').first().click()
  for (const [index, segment] of segments.entries()) {
    const node = page.locator('.el-cascader-node:visible').filter({ hasText: segment }).first()
    await node.waitFor({ state: 'visible', timeout: 30000 })
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
}

async function readonlyRevisionCandidateFlow(page) {
  await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD)
  const writeRequests = []
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/dcc/') &&
      !['GET', 'HEAD'].includes(request.method().toUpperCase())
    ) {
      writeRequests.push({ method: request.method(), url: request.url() })
    }
  })
  await page.goto(`${BASE_URL}/dcc/controlled-file/upload`, { waitUntil: 'commit', timeout: 60000 })
  await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const adminTaxonomy = await resolveSelectedTaxonomy(page, ADMIN_TAXONOMY_PATH)

  await selectElOption(page, 'DCC项目', ADMIN_PROJECT.keyword, ADMIN_PROJECT.optionText)
  const candidateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/upload-revision-candidates') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await selectCascaderPath(page, '文件分类', adminTaxonomy.path)
  const candidateResponse = await candidateResponsePromise
  const candidatePayload = await candidateResponse.json()
  assert.ok([0, 200].includes(candidatePayload.code), `candidate business code ${candidatePayload.code}: ${candidatePayload.msg || ''}`)
  assert.deepEqual(writeRequests, [], 'readonly admin candidate probe must not send DCC write requests')
  const total = candidatePayload.data?.total || 0
  if (total <= 0) {
    return {
      blocked: true,
      reason: 'no readonly admin revision candidates for selected project and taxonomy',
      projectId: ADMIN_PROJECT.id,
      taxonomyId: adminTaxonomy.id,
      taxonomyPath: adminTaxonomy.path,
      total
    }
  }

  const candidateRegion = page.locator('[data-testid="dcc-upload-revision-candidates"]')
  await candidateRegion.waitFor({ state: 'visible', timeout: 30000 })
  await candidateRegion.getByRole('button', { name: '选择' }).first().click()
  await page.getByText('已选择：', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })

  return {
    projectId: ADMIN_PROJECT.id,
    taxonomyId: adminTaxonomy.id,
    taxonomyPath: adminTaxonomy.path,
    total,
    firstCandidateId: candidatePayload.data.list?.[0]?.id,
    firstCandidateFileName: candidatePayload.data.list?.[0]?.fileName
  }
}

async function waitForDirectoryReady(page) {
  const item = formItem(page, '提交目录')
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForFunction(() => {
    const node = Array.from(document.querySelectorAll('.el-form-item')).find((item) =>
      (item.textContent || '').includes('提交目录')
    )
    return !!node && /(当前绑定目录已经是最后一层目录|最终提交路径：)/.test(node.textContent || '')
  }, { timeout: 30000 })
}

async function fillInputByLabel(page, label, value) {
  await formItem(page, label).locator('input').first().fill(value)
}

async function fillTextareaByLabel(page, label, value) {
  await formItem(page, label).locator('textarea').first().fill(value)
}

async function uploadSourceFile(page) {
  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/upload-preview') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.locator('section[data-testid="dcc-upload-section-attachment"] input[type="file"]').first().setInputFiles(SOURCE_FILE)
  const uploadResponse = await uploadResponsePromise
  const uploadPayload = await uploadResponse.json()
  assert.ok([0, 200].includes(uploadPayload.code), `upload business code ${uploadPayload.code}: ${uploadPayload.msg || ''}`)
  await page.getByText('预览文件：', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  return uploadPayload.data
}

async function apiGet(page, url) {
  const result = await page.evaluate(async (url) => {
    const readWsCache = (key) => {
      const raw = window.localStorage.getItem(key)
      if (!raw) return undefined
      let current = raw
      for (let index = 0; index < 8; index += 1) {
        if (typeof current === 'string') {
          const trimmed = current.trim()
          if (!trimmed) return undefined
          try {
            current = JSON.parse(trimmed)
            continue
          } catch {
            return trimmed.replace(/^"(.*)"$/, '$1')
          }
        }
        if (current && typeof current === 'object') {
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
        }
        return current
      }
      return current
    }
    const accessToken = readWsCache('ACCESS_TOKEN')
    const tenantId = readWsCache('tenantId')
    if (!accessToken || !tenantId) {
      throw new Error('missing authenticated browser cache')
    }
    const response = await fetch(`/admin-api${url}`, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId)
      }
    })
    return { ok: response.ok, status: response.status, payload: await response.json() }
  }, url)
  assert.equal(result.ok, true, `GET ${url} HTTP ${result.status}`)
  assert.ok([0, 200].includes(result.payload.code), `GET ${url} business code ${result.payload.code}: ${result.payload.msg || ''}`)
  return result.payload.data
}

async function apiWrite(page, method, url, body) {
  const result = await page.evaluate(async ({ method, url, body }) => {
    const readWsCache = (key) => {
      const raw = window.localStorage.getItem(key)
      if (!raw) return undefined
      let current = raw
      for (let index = 0; index < 8; index += 1) {
        if (typeof current === 'string') {
          const trimmed = current.trim()
          if (!trimmed) return undefined
          try {
            current = JSON.parse(trimmed)
            continue
          } catch {
            return trimmed.replace(/^"(.*)"$/, '$1')
          }
        }
        if (current && typeof current === 'object') {
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
        }
        return current
      }
      return current
    }
    const accessToken = readWsCache('ACCESS_TOKEN')
    const tenantId = readWsCache('tenantId')
    if (!accessToken || !tenantId) {
      throw new Error('missing authenticated browser cache')
    }
    const headers = {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
      'tenant-id': String(tenantId)
    }
    const options = {
      method,
      headers
    }
    if (typeof body !== 'undefined') {
      headers['Content-Type'] = 'application/json'
      options.body = JSON.stringify(body)
    }
    const response = await fetch(`/admin-api${url}`, options)
    return { ok: response.ok, status: response.status, payload: await response.json() }
  }, { method, url, body })
  assert.equal(result.ok, true, `${method} ${url} HTTP ${result.status}`)
  assert.ok(
    [0, 200].includes(result.payload.code),
    `${method} ${url} business code ${result.payload.code}: ${result.payload.msg || ''}`
  )
  return result.payload.data
}

function normalizeTaxonomyName(value) {
  return String(value || '').trim()
}

function normalizeParentId(value) {
  if (value === null || typeof value === 'undefined' || value === '') {
    return 0
  }
  const id = Number(value)
  return Number.isFinite(id) ? id : 0
}

function resolveTaxonomyByPath(rows, pathSegments) {
  assert.ok(Array.isArray(rows) && rows.length > 0, 'taxonomy list must not be empty')
  let parentId = 0
  let current = null
  for (const segment of pathSegments) {
    const matches = rows.filter(
      (item) =>
        item &&
        item.active !== false &&
        normalizeTaxonomyName(item.name) === segment &&
        normalizeParentId(item.parentId) === parentId
    )
    assert.equal(
      matches.length,
      1,
      `taxonomy path segment must resolve uniquely: ${pathSegments.join(' / ')} at ${segment}, matched=${matches.length}`
    )
    current = matches[0]
    parentId = Number(current.id)
  }
  assert.ok(current?.id, `taxonomy path must resolve to leaf id: ${pathSegments.join(' / ')}`)
  return { id: Number(current.id), path: pathSegments }
}

async function resolveSelectedTaxonomy(page, pathSegments) {
  const rows = await apiGet(page, '/dcc/file-type-taxonomies')
  return resolveTaxonomyByPath(rows || [], pathSegments)
}

async function testTenantUploadFlow(page) {
  await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD)
  await page.goto(`${BASE_URL}/dcc/controlled-file/upload`, { waitUntil: 'commit', timeout: 60000 })
  await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const testTaxonomy = await resolveSelectedTaxonomy(page, TEST_TAXONOMY_PATH)

  await selectElOption(page, 'DCC项目', TEST_PROJECT.keyword, TEST_PROJECT.optionText)
  await selectCascaderPath(page, '文件分类', testTaxonomy.path)
  await selectCategory(page, CATEGORY_NAME)
  await waitForDirectoryReady(page)

  const stamp = runId()
  const fileNumber = `CODEX-DCC-PT-${stamp}`
  const fileName = `项目分类上传E2E-${stamp}.docx`
  await fillInputByLabel(page, '文件名称', fileName)
  await fillInputByLabel(page, '文件编号', fileNumber)
  await fillInputByLabel(page, '版本号', 'V1.0')
  await fillInputByLabel(page, '生效日期', todayString())
  await fillTextareaByLabel(page, '提交备注', `Codex DCC upload project taxonomy E2E ${stamp}`)
  const uploaded = await uploadSourceFile(page)

  let submitRequest = null
  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/submit') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  page.on('request', (request) => {
    if (request.method() === 'POST' && request.url().includes('/admin-api/dcc/controlled-files/submit')) {
      submitRequest = request.postDataJSON()
    }
  })
  await page.getByRole('button', { name: '提交审批' }).click()
  const submitResponse = await submitResponsePromise
  const submitPayload = await submitResponse.json()
  assert.ok([0, 200].includes(submitPayload.code), `submit business code ${submitPayload.code}: ${submitPayload.msg || ''}`)
  const controlledFileId = submitPayload.data
  assert.ok(controlledFileId, 'submit must return controlled file id')

  const detail = await apiGet(page, `/dcc/controlled-files/${controlledFileId}`)
  assert.equal(detail.dccProjectCodeId, TEST_PROJECT.id, 'detail project id must match upload selection')
  assert.equal(detail.productMasterId ?? null, null, 'detail productMasterId must be null for new DCC writes')
  assert.equal(detail.productCode, TEST_PROJECT.code, 'detail productCode must come from DCC project code')
  assert.equal(detail.fileTypeTaxonomyId, testTaxonomy.id, 'detail taxonomy id must match upload selection')
  assert.equal(detail.fileTypeLevel1, testTaxonomy.path[0])
  assert.equal(detail.fileTypeLevel2, testTaxonomy.path[1])
  assert.equal(detail.fileTypeLevel3, testTaxonomy.path[2])

  assert.equal(submitRequest.dccProjectCodeId, TEST_PROJECT.id, 'submit payload must include selected project')
  assert.equal(submitRequest.productMasterId ?? null, null, 'submit payload must explicitly clear productMasterId')
  assert.equal(submitRequest.productCode, TEST_PROJECT.code, 'submit payload must sync DCC project code for display')
  assert.equal(submitRequest.fileTypeTaxonomyId, testTaxonomy.id, 'submit payload must include selected taxonomy')
  assert.equal(submitRequest.revisionTargetControlledFileId ?? null, null, 'new upload must not carry stale revision target')

  const cleanup = {
    withdraw: await apiWrite(page, 'POST', `/dcc/controlled-files/${controlledFileId}/withdraw`, {
      reason: `Codex cleanup ${stamp}`
    }),
    deleteWithdrawnFlow: await apiWrite(page, 'DELETE', `/dcc/controlled-files/${controlledFileId}/withdrawn-flow`)
  }

  return {
    controlledFileId,
    fileNumber,
    fileName,
    uploadedFileName: uploaded.fileName,
    submitRequest,
    detail: {
      dccProjectCodeId: detail.dccProjectCodeId,
      productMasterId: detail.productMasterId ?? null,
      productCode: detail.productCode,
      fileTypeTaxonomyId: detail.fileTypeTaxonomyId,
      fileTypeLevel1: detail.fileTypeLevel1,
      fileTypeLevel2: detail.fileTypeLevel2,
      fileTypeLevel3: detail.fileTypeLevel3,
      selectedTaxonomyPath: testTaxonomy.path
    },
    cleanup
  }
}

;(async () => {
  assertPrerequisites()
  const browser = await chromium.launch({ headless: process.env.DCC_UPLOAD_TAXONOMY_E2E_HEADLESS !== 'false' })

  try {
    const readonlyContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const readonlyPage = await readonlyContext.newPage()
    readonlyPage.setDefaultTimeout(60000)
    readonlyPage.setDefaultNavigationTimeout(60000)
    const readonlyCandidate =
      process.env.DCC_UPLOAD_TAXONOMY_E2E_SKIP_ADMIN_READONLY === '1'
        ? { skipped: true, reason: 'task only requires test-tenant DCC project-code write path' }
        : await readonlyRevisionCandidateFlow(readonlyPage)
    await readonlyContext.close()

    const uploadContext = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const uploadPage = await uploadContext.newPage()
    uploadPage.setDefaultTimeout(60000)
    uploadPage.setDefaultNavigationTimeout(60000)
    const upload = await testTenantUploadFlow(uploadPage)
    await uploadContext.close()

    console.log(
      `DCC_UPLOAD_PROJECT_TAXONOMY_REVISION_E2E ${JSON.stringify(
        {
          baseUrl: BASE_URL,
          readonlyCandidate,
          upload
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
