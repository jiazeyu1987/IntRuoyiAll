const assert = require('node:assert/strict')
const fs = require('node:fs')
const { createRequire } = require('node:module')
const path = require('node:path')

const frontendRequire = createRequire(path.resolve(__dirname, '..', '..', '..', 'IntRuoyiFronted', 'package.json'))
const { chromium } = frontendRequire('playwright')

const BASE_URL = (process.env.DCC_PRODUCT_ONBOARDING_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_PRODUCT_ONBOARDING_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_PRODUCT_ONBOARDING_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_PRODUCT_ONBOARDING_E2E_PASSWORD || '111111'
const PROJECT_CODE_PATH = process.env.DCC_PRODUCT_ONBOARDING_E2E_PATH || '/mdm/project-code'
const CHROME_EXECUTABLE = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
const RESULT_PATH = path.join(__dirname, 'dcc-product-onboarding-real-e2e-result.json')

function buildRunData() {
  const runId = (process.env.DCC_PRODUCT_ONBOARDING_E2E_RUN_ID || new Date().toISOString())
    .replace(/\D/g, '')
    .slice(0, 14)
    .padEnd(14, '0')
  const dccProductCode = `C${runId.slice(-13)}`
  return {
    runId,
    productCode: `CODX-MDM-ONB-${runId}`,
    dccProductCode,
    productNameCn: `Codex建档产品${runId}`,
    productNameEn: `Codex Onboarding Product ${runId}`,
    modelSpecification: `CODX-SPEC-${runId.slice(-6)}`,
    productCategory: 'Codex E2E',
    docControlNo: `CODX-DOC-${runId.slice(-6)}`,
    projectName: `Codex建档项目${runId}`,
    projectCode: `CODXONB${runId.slice(-8)}`,
    category: 'Codex E2E',
    projectLeader: 'Codex',
    projectEngineer: 'Codex',
    storageLocation: 'Codex E2E',
    priority: 'P2'
  }
}

function assertSafeBoundary() {
  const url = new URL(BASE_URL)
  assert.match(url.hostname, /^(localhost|127\.0\.0\.1)$/)
  assert.equal(TENANT, '测试租户', 'write E2E must use 测试租户')
  assert.equal(USERNAME, 'aoteman', 'write E2E must use aoteman')
}

function redactUrl(url) {
  return String(url).replace(/accessToken=[^&]+/g, 'accessToken=<redacted>')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if (await input.isVisible()) {
      await input.fill('')
      await input.fill(value)
      return
    }
  }
  throw new Error(`missing visible input: ${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }

  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"]):visible'), USERNAME, 'username')
  await fillFirstVisible(form.locator('input[type="password"]'), PASSWORD, 'password')

  const [response] = await Promise.all([
    page.waitForResponse(
      (item) => item.url().includes('/system/auth/login') && item.request().method() === 'POST',
      { timeout: 60000 }
    ),
    form.getByRole('button', { name: '登录' }).click()
  ])
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && payload && [0, 200].includes(payload.code), `login failed: ${JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) return ''
  const normalizeString = (value) => {
    let current = value || ''
    for (let index = 0; index < 3; index += 1) {
      const trimmed = String(current).trim()
      if (!trimmed.startsWith('"')) return trimmed
      try {
        const parsed = JSON.parse(trimmed)
        if (typeof parsed !== 'string' || parsed === current) return trimmed.replace(/^"(.*)"$/, '$1')
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
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function requestJson(page, headers, requestPath) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, {
        method: 'GET',
        headers: requestHeaders
      })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { raw: text }
      }
      return { status: response.status, payload }
    },
    {
      requestUrl: `${BASE_URL}${requestPath}`,
      requestHeaders: headers
    }
  )
}

function assertApiOk(result, label) {
  assert.equal(result.status, 200, `${label} HTTP failed: ${JSON.stringify(result)}`)
  assert.ok([0, 200].includes(result.payload?.code), `${label} API failed: ${JSON.stringify(result.payload)}`)
}

async function apiGet(page, headers, requestPath, label) {
  const result = await requestJson(page, headers, requestPath)
  assertApiOk(result, label)
  return result.payload.data
}

async function waitForApiBusinessOk(responsePromise, label) {
  const response = await responsePromise
  const text = await response.text()
  let payload = null
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    payload = { raw: text }
  }
  assert.equal(response.status(), 200, `${label} HTTP ${response.status()}: ${JSON.stringify(payload)}`)
  assert.ok([0, 200].includes(payload?.code), `${label} business failed: ${JSON.stringify(payload)}`)
  return payload.data
}

async function fillDialogInput(dialog, label, value) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  const input = item.locator('input:visible').first()
  await input.fill('')
  await input.fill(value)
}

async function applyProjectCodeQuickFilter(page, projectCode) {
  const quickFilter = page.locator('.table-quick-filter[data-table-key="dcc.projectCode.main"]').first()
  await quickFilter.waitFor({ state: 'visible', timeout: 30000 })
  await quickFilter.locator('.table-quick-filter__field').click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '项目代码' }).first().click()
  const valueInput = quickFilter.locator('.table-quick-filter__value input.el-input__inner').first()
  await valueInput.fill('')
  await valueInput.fill(projectCode)
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/project-codes/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await quickFilter.getByRole('button', { name: '查询' }).click()
  const pageData = await waitForApiBusinessOk(pageResponsePromise, 'query generated project code')
  assert.ok(
    Array.isArray(pageData?.list) && pageData.list.some((item) => String(item.projectCode) === projectCode),
    `query result must include generated project code: ${JSON.stringify(pageData)}`
  )
  await settle(page)
}

async function main() {
  assertSafeBoundary()
  const runData = buildRunData()
  const result = {
    status: 'RUNNING',
    baseUrl: BASE_URL,
    path: PROJECT_CODE_PATH,
    tenant: TENANT,
    username: USERNAME,
    runData,
    requestId: null,
    generatedProjectCodeId: null,
    productMasterId: null,
    targetWriteRequests: [],
    targetNetworkFailures: [],
    criticalNetworkFailures: [],
    consoleErrors: [],
    pageErrors: [],
    verifiedProjectCode: null,
    verifiedMdmProduct: null
  }

  const browser = await chromium.launch({
    headless: process.env.DCC_PRODUCT_ONBOARDING_E2E_HEADED !== '1',
    args: ['--disable-dev-shm-usage'],
    ...(CHROME_EXECUTABLE ? { executablePath: CHROME_EXECUTABLE } : {})
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  page.on('console', (message) => {
    if (message.type() === 'error') {
      result.consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => {
    result.pageErrors.push(error.message)
  })
  page.on('request', (request) => {
    const method = request.method()
    if (request.url().includes('/admin-api/dcc/') && !['GET', 'HEAD'].includes(method)) {
      result.targetWriteRequests.push(`${method} ${redactUrl(request.url())}`)
    }
  })
  page.on('requestfailed', (request) => {
    if (request.url().includes('/admin-api/dcc/') || request.url().includes('/admin-api/mdm/')) {
      result.targetNetworkFailures.push({
        method: request.method(),
        url: redactUrl(request.url()),
        failure: request.failure()?.errorText || 'unknown'
      })
    }
  })

  try {
    await login(page)
    const headers = await buildAuthHeaders(page)
    await page.goto(`${BASE_URL}${PROJECT_CODE_PATH}`, { waitUntil: 'commit', timeout: 60000 })
    await page.getByText('基础数据 / DCC项目代码', { exact: false }).first().waitFor({ state: 'visible' })

    const openButton = page.locator('[data-testid="dcc-product-onboarding-open"]').first()
    await openButton.waitFor({ state: 'visible', timeout: 30000 })
    await openButton.click()

    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '产品建档申请' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText('审批通过后生成 DCC 项目代码并绑定 MDM 产品', { exact: false }).waitFor({
      state: 'visible',
      timeout: 30000
    })

    await fillDialogInput(dialog, '产品编码', runData.productCode)
    await fillDialogInput(dialog, 'DCC 产品编号', runData.dccProductCode)
    await fillDialogInput(dialog, '产品中文名', runData.productNameCn)
    await fillDialogInput(dialog, '产品英文名', runData.productNameEn)
    await fillDialogInput(dialog, '型号规格', runData.modelSpecification)
    await fillDialogInput(dialog, '产品类别', runData.productCategory)
    await fillDialogInput(dialog, '文控', runData.docControlNo)
    await fillDialogInput(dialog, '目标项目名称', runData.projectName)
    await fillDialogInput(dialog, '目标项目代码', runData.projectCode)
    await fillDialogInput(dialog, 'DCC 类别', runData.category)
    await fillDialogInput(dialog, '项目组负责人', runData.projectLeader)
    await fillDialogInput(dialog, '项目工程师', runData.projectEngineer)
    await fillDialogInput(dialog, '存放位置', runData.storageLocation)
    await fillDialogInput(dialog, '优先级', runData.priority)

    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/product-onboarding-requests/create') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await page.locator('[data-testid="dcc-product-onboarding-submit"]').first().click()
    result.requestId = await waitForApiBusinessOk(createResponsePromise, 'create onboarding request')
    assert.ok(result.requestId, 'create onboarding request did not return request id')

    const approveButton = page.locator('[data-testid="dcc-product-onboarding-approve"]').first()
    await page.waitForFunction(() => {
      const button = document.querySelector('[data-testid="dcc-product-onboarding-approve"]')
      return Boolean(button && !button.hasAttribute('disabled') && button.getAttribute('aria-disabled') !== 'true')
    })

    const approveResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/dcc/product-onboarding-requests/${result.requestId}/approve`) &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await approveButton.click()
    const approvedRequest = await waitForApiBusinessOk(approveResponsePromise, 'approve onboarding request')
    result.generatedProjectCodeId = approvedRequest.generatedProjectCodeId
    result.productMasterId = approvedRequest.productMasterId
    assert.ok(result.generatedProjectCodeId, 'approve did not return generated project code id')
    assert.ok(result.productMasterId, 'approve did not return product master id')

    await page.locator('.el-dialog:visible').filter({ hasText: '产品建档申请' }).waitFor({
      state: 'hidden',
      timeout: 60000
    })
    await applyProjectCodeQuickFilter(page, runData.projectCode)
    await page.locator('.dcc-project-code-list-template').getByText(runData.projectCode, { exact: false }).waitFor({
      state: 'visible',
      timeout: 60000
    })
    const listText = await page.locator('.dcc-project-code-list-template').innerText()
    assert.match(listText, new RegExp(runData.projectName), 'project code list must show generated project name')

    const projectCode = await apiGet(
      page,
      headers,
      `/admin-api/dcc/project-codes/${result.generatedProjectCodeId}`,
      'generated project code detail'
    )
    assert.equal(String(projectCode.projectCode), runData.projectCode)
    assert.equal(String(projectCode.projectName), runData.projectName)
    assert.equal(String(projectCode.status), 'ENABLE')
    assert.equal(Number(projectCode.productMasterId), Number(result.productMasterId))
    result.verifiedProjectCode = projectCode

    const mdmProduct = await apiGet(
      page,
      headers,
      `/admin-api/mdm/product/get?id=${result.productMasterId}`,
      'generated MDM product detail'
    )
    assert.equal(String(mdmProduct.productCode), runData.productCode)
    assert.equal(String(mdmProduct.dccProductCode), runData.dccProductCode)
    assert.equal(String(mdmProduct.nameCn), runData.productNameCn)
    assert.equal(String(mdmProduct.status), 'ENABLE')
    result.verifiedMdmProduct = mdmProduct

    result.criticalNetworkFailures = result.targetNetworkFailures.filter((failure) =>
      /\/admin-api\/(dcc\/product-onboarding-requests|dcc\/project-codes|mdm\/product)/.test(failure.url)
    )
    const onboardingWriteRequests = result.targetWriteRequests.filter((request) =>
      request.includes('/admin-api/dcc/product-onboarding-requests/')
    )
    assert.equal(onboardingWriteRequests.length, 2, `expected create and approve write requests: ${onboardingWriteRequests.join('; ')}`)
    assert.deepEqual(result.criticalNetworkFailures, [], 'critical DCC onboarding/MDM network requests must not fail')
    assert.deepEqual(result.pageErrors, [], 'page must not raise runtime errors')
    result.status = 'PASS'
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(
      `PASS: product onboarding requestId=${result.requestId} projectCodeId=${result.generatedProjectCodeId} productMasterId=${result.productMasterId} projectCode=${runData.projectCode}`
    )
  } catch (error) {
    result.status = 'FAIL'
    result.error = error && error.stack ? error.stack : String(error)
    result.currentUrl = page.url()
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
