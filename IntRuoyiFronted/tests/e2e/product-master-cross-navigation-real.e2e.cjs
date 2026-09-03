const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontRoot, '..')
const taskDir = path.join(repoRoot, 'doc/tasks/20260828-product-master-cross-navigation-int-main')
const artifactDir = path.join(taskDir, 'artifacts')
const summaryPath = path.join(artifactDir, 'product-master-cross-navigation-real-summary.json')
const screenshotPath = path.join(artifactDir, 'product-master-cross-navigation-real-final.png')

function parseEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {}
  const env = {}
  for (const rawLine of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const match = line.match(/^([^=]+?)\s*=\s*(.*)$/)
    if (!match) continue
    env[match[1].trim()] = match[2].trim().replace(/^['"]|['"]$/g, '')
  }
  return env
}

const baseEnv = parseEnvFile(path.join(frontRoot, '.env'))
const config = {
  baseUrl: (process.env.PRODUCT_CROSS_NAV_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.PRODUCT_CROSS_NAV_TENANT || baseEnv.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: process.env.PRODUCT_CROSS_NAV_USERNAME || baseEnv.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: process.env.PRODUCT_CROSS_NAV_PASSWORD || baseEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD
}

for (const [key, value] of Object.entries(config)) {
  assert.ok(value && String(value).trim(), `${key} is required`)
}

const summary = {
  baseUrl: config.baseUrl,
  identity: `${config.tenant}/${config.username}`,
  checkedAt: new Date().toISOString(),
  candidate: null,
  targetRequests: [],
  targetWrites: [],
  badResponses: [],
  consoleErrors: [],
  pageErrors: [],
  steps: [],
  screenshot: screenshotPath
}

function unwrap(payload) {
  if (!payload || typeof payload !== 'object') return payload
  if (Object.prototype.hasOwnProperty.call(payload, 'code')) {
    assert.equal(payload.code, 0, payload.msg || 'business response should succeed')
    return payload.data
  }
  return Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : payload
}

function asList(pageResult) {
  return Array.isArray(pageResult?.list) ? pageResult.list : []
}

function sanitizeUrl(value) {
  const url = new URL(value)
  return `${url.origin}${url.pathname}${url.search}`
}

function getSearchValue(url, key) {
  return new URL(url).searchParams.get(key)
}

function sameTextId(left, right) {
  return String(left ?? '') === String(right ?? '')
}

async function responsePayload(response, label) {
  const payload = await response.json()
  const data = unwrap(payload)
  const list = asList(data)
  summary.targetRequests.push({
    label,
    method: response.request().method(),
    url: sanitizeUrl(response.url()),
    httpStatus: response.status(),
    businessCode: payload?.code ?? null,
    total: data?.total ?? null,
    firstIds: list.slice(0, 5).map((item) => ({
      id: item.id ?? item.certificateId ?? null,
      productMasterId: item.productMasterId ?? null,
      projectCodeId: item.projectCodeId ?? null
    }))
  })
  return data
}

function waitForPageResponse(page, pathname, query, label) {
  const expected = Object.entries(query || {}).filter(([, value]) => value !== undefined && value !== null)
  return page
    .waitForResponse((response) => {
      if (response.request().method() !== 'GET') return false
      const url = new URL(response.url())
      if (url.pathname !== pathname) return false
      return expected.every(([key, value]) => url.searchParams.get(key) === String(value))
    }, { timeout: 30000 })
    .then((response) => responsePayload(response, label))
}

async function gotoAndCapture(page, routePath, pathname, query, label) {
  const [data] = await Promise.all([
    waitForPageResponse(page, pathname, query, label),
    page.goto(`${config.baseUrl}${routePath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  ])
  return data
}

async function firstVisible(locator, label) {
  const deadline = Date.now() + 30000
  while (Date.now() < deadline) {
    const count = await locator.count()
    for (let index = 0; index < count; index += 1) {
      const item = locator.nth(index)
      if (await item.isVisible().catch(() => false)) {
        return item
      }
    }
    await locator.page().waitForTimeout(250)
  }
  throw new Error(`No visible element found: ${label}`)
}

async function clickVisibleButton(scope, name, label) {
  const button = await firstVisible(
    scope.locator('button').filter({ hasText: new RegExp(`^\\s*${name}\\s*$`) }),
    label
  )
  await button.click()
}

async function selectTenant(page) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) return
  await tenantSelect.click()
  const input = page.locator('.login-form .el-select__input').first()
  await input.fill(config.tenant)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
  if ((await option.count()) > 0) {
    await option.click()
  } else {
    await input.press('Enter')
  }
}

async function fillFirstVisible(locator, value, label) {
  const input = await firstVisible(locator, label)
  await input.fill(value)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'commit', timeout: 60000 })
  await page
    .locator('.login-form, button:has-text("登录")')
    .first()
    .waitFor({ state: 'visible', timeout: 90000 })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await selectTenant(page)
  await fillFirstVisible(
    page.locator('.login-form input[placeholder="请输入用户名"], .login-form input.el-input__inner:not([type="password"]):not([role="combobox"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(
    page.locator('.login-form input[type="password"], .login-form input[placeholder="请输入密码"]'),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  ).catch((error) => ({ error }))
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  ).catch((error) => ({ error }))
  await page.locator('button:has-text("登录")').first().click()
  const loginResponse = await loginResponsePromise
  if (loginResponse.error) {
    throw loginResponse.error
  }
  unwrap(await loginResponse.json())
  const permissionResponse = await permissionResponsePromise
  if (permissionResponse.error) {
    throw permissionResponse.error
  }
  assert.equal(permissionResponse.status(), 200, 'permission info should return HTTP 200 after login')
  unwrap(await permissionResponse.json())
  const permissionRequestHeaders = permissionResponse.request().headers()
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  return Object.fromEntries(
    ['authorization', 'tenant-id', 'visit-tenant-id']
      .filter((key) => permissionRequestHeaders[key])
      .map((key) => [key, permissionRequestHeaders[key]])
  )
}

async function apiGet(page, headers, pathAndQuery) {
  const result = await page.evaluate(
    async ({ pathAndQuery: innerPath, headers: innerHeaders }) => {
      const response = await fetch(innerPath, { headers: innerHeaders })
      return { status: response.status, text: await response.text() }
    },
    { pathAndQuery, headers }
  )
  assert.equal(result.status, 200, `${pathAndQuery} should return HTTP 200`)
  return unwrap(JSON.parse(result.text))
}

async function findCandidate(page, headers) {
  for (let pageNo = 1; pageNo <= 20; pageNo += 1) {
    const data = await apiGet(
      page,
      headers,
      `/admin-api/dcc/registration-certificates/page?pageNo=${pageNo}&pageSize=100`
    )
    const candidate = asList(data).find((item) => item.productMasterId && item.projectCodeId)
    if (candidate) return candidate
    if (!data?.total || pageNo * 100 >= data.total) break
  }
  throw new Error('No current registration certificate row with both productMasterId and projectCodeId was found.')
}

function assertContainsProduct(data, productMasterId, label) {
  assert.ok(asList(data).some((item) => sameTextId(item.id, productMasterId)), `${label} must contain product ${productMasterId}`)
}

function assertContainsProjectCode(data, projectCodeId, label) {
  assert.ok(asList(data).some((item) => sameTextId(item.id, projectCodeId)), `${label} must contain project-code ${projectCodeId}`)
}

function assertContainsRegistrationByProduct(data, productMasterId, label) {
  assert.ok(
    asList(data).some((item) => sameTextId(item.productMasterId, productMasterId)),
    `${label} must contain registration for product ${productMasterId}`
  )
}

function assertContainsRegistrationByProject(data, projectCodeId, label) {
  assert.ok(
    asList(data).some((item) => sameTextId(item.projectCodeId, projectCodeId)),
    `${label} must contain registration for project-code ${projectCodeId}`
  )
}

async function main() {
  fs.mkdirSync(artifactDir, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })

  page.on('console', (message) => {
    if (message.type() === 'error') summary.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => summary.pageErrors.push(error.message))
  page.on('request', (request) => {
    const url = new URL(request.url())
    const isTargetModule = /^\/admin-api\/(dcc\/project-codes|mdm\/product|dcc\/registration-certificates)/.test(url.pathname)
    if (isTargetModule && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(request.method())) {
      summary.targetWrites.push({ method: request.method(), url: sanitizeUrl(request.url()) })
    }
  })
  page.on('response', (response) => {
    const url = new URL(response.url())
    if (url.pathname.startsWith('/admin-api/') && response.status() >= 400) {
      summary.badResponses.push({
        method: response.request().method(),
        url: sanitizeUrl(response.url()),
        httpStatus: response.status(),
        statusText: response.statusText()
      })
    }
  })

  try {
    const headers = await login(page)
    const candidate = await findCandidate(page, headers)
    const productMasterId = String(candidate.productMasterId)
    const projectCodeId = String(candidate.projectCodeId)
    summary.candidate = {
      certificateId: candidate.certificateId,
      certificateNo: candidate.certificateNo,
      productMasterId,
      productName: candidate.productName,
      projectCodeId,
      projectCode: candidate.projectCode
    }

    await gotoAndCapture(page, '/mdm/product', '/admin-api/mdm/product/page', {}, 'warm product page without linked filter')
    await gotoAndCapture(
      page,
      `/mdm/registration-certificate?projectCodeId=${encodeURIComponent(projectCodeId)}`,
      '/admin-api/dcc/registration-certificates/page',
      { projectCodeId },
      'open registration by projectCodeId before product jump'
    )
    const [productData] = await Promise.all([
      waitForPageResponse(
        page,
        '/admin-api/mdm/product/page',
        { productMasterId },
        'registration row click to product management'
      ),
      clickVisibleButton(page.locator('[data-testid="registration-certificate-current-tab"] .el-table'), '产品', 'registration product button')
    ])
    await page.waitForURL((url) => url.pathname === '/mes/md/showroom-product' && getSearchValue(url.href, 'productMasterId') === productMasterId)
    assertContainsProduct(productData, productMasterId, 'registration -> product')
    summary.steps.push('registration -> product query refreshed')

    const [registrationByProductData] = await Promise.all([
      waitForPageResponse(
        page,
        '/admin-api/dcc/registration-certificates/page',
        { productMasterId },
        'product row click to registration certificate management'
      ),
      clickVisibleButton(page.locator('.scheme-d-basic-data-page--mdm-product .el-table'), '注册证', 'product registration button')
    ])
    await page.waitForURL(
      (url) => url.pathname === '/mdm/registration-certificate' && getSearchValue(url.href, 'productMasterId') === productMasterId
    )
    assertContainsRegistrationByProduct(registrationByProductData, productMasterId, 'product -> registration')
    summary.steps.push('product -> registration query refreshed')

    await gotoAndCapture(
      page,
      `/mdm/product?productMasterId=${encodeURIComponent(productMasterId)}`,
      '/admin-api/mdm/product/page',
      { productMasterId },
      'open product by productMasterId before project-code jump'
    )
    const [projectByProductData] = await Promise.all([
      waitForPageResponse(
        page,
        '/admin-api/dcc/project-codes/page',
        { productMasterId },
        'product row click to project-code management'
      ),
      clickVisibleButton(page.locator('.scheme-d-basic-data-page--mdm-product .el-table'), '项目代码', 'product project-code button')
    ])
    await page.waitForURL((url) => url.pathname === '/mdm/project-code' && getSearchValue(url.href, 'productMasterId') === productMasterId)
    assertContainsProjectCode(projectByProductData, projectCodeId, 'product -> project-code')
    summary.steps.push('product -> project-code query refreshed')

    const [registrationByProjectData] = await Promise.all([
      waitForPageResponse(
        page,
        '/admin-api/dcc/registration-certificates/page',
        { projectCodeId },
        'project-code row click to registration certificate management'
      ),
      clickVisibleButton(page.locator('.scheme-d-basic-data-page--dcc-project-code .el-table'), '注册证', 'project-code registration button')
    ])
    await page.waitForURL(
      (url) => url.pathname === '/mdm/registration-certificate' && getSearchValue(url.href, 'projectCodeId') === projectCodeId
    )
    assertContainsRegistrationByProject(registrationByProjectData, projectCodeId, 'project-code -> registration')
    summary.steps.push('project-code -> registration query refreshed')

    const [projectDetail] = await Promise.all([
      waitForPageResponse(
        page,
        `/admin-api/dcc/project-codes/${projectCodeId}`,
        {},
        'registration row click to project-code detail drawer'
      ),
      clickVisibleButton(page.locator('[data-testid="registration-certificate-current-tab"] .el-table'), '项目代码', 'registration project-code button')
    ])
    await page.waitForURL((url) => url.pathname === '/mes/md/dcc-project-code' && getSearchValue(url.href, 'projectCodeId') === projectCodeId)
    assert.ok(sameTextId(projectDetail?.id, projectCodeId), `project detail drawer must load project-code ${projectCodeId}`)
    await page.locator('[data-testid="dcc-project-code-detail-drawer"]').filter({ hasText: candidate.projectCode || projectCodeId }).first()
      .waitFor({ state: 'visible', timeout: 30000 })
    summary.steps.push('registration -> project-code detail drawer opened')

    assert.deepEqual(summary.targetWrites, [], 'read-only cross-navigation verification must not issue target writes')
    assert.equal(summary.steps.length, 5, 'all five cross-navigation checks must pass')
    await page.screenshot({ path: screenshotPath, fullPage: true })
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    console.log(`PASS: product master cross-navigation real E2E ${summaryPath}`)
  } catch (error) {
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => undefined)
    summary.error = error?.stack || String(error)
    fs.writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    throw error
  } finally {
    await browser.close()
  }
}

main()
