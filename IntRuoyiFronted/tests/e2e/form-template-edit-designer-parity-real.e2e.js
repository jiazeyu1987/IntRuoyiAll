const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.FORM_TEMPLATE_EDIT_PARITY_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.FORM_TEMPLATE_EDIT_PARITY_E2E_TENANT || '芋道源码',
  username: process.env.FORM_TEMPLATE_EDIT_PARITY_E2E_USERNAME || 'admin',
  password: process.env.FORM_TEMPLATE_EDIT_PARITY_E2E_PASSWORD || 'admin123',
  headed: process.env.FORM_TEMPLATE_EDIT_PARITY_E2E_HEADED === '1',
  screenshotPath: path.resolve(
    __dirname,
    '../../../doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-current-workspace.png'
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function logStep(message) {
  console.log(`[step] ${message}`)
}

function assertPrerequisites() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${config.baseUrl}`
  )
  assert.ok(config.password, '缺少 FORM_TEMPLATE_EDIT_PARITY_E2E_PASSWORD')
  assert.ok(fs.existsSync(executablePath), `缺少 Chrome 可执行文件: ${executablePath}`)
}

function unwrapCacheValue(raw) {
  if (!raw) return null
  let current = raw
  for (let index = 0; index < 4; index += 1) {
    if (typeof current !== 'string') return current
    try {
      current = JSON.parse(current)
    } catch {
      return current
    }
    if (current && typeof current === 'object' && Object.prototype.hasOwnProperty.call(current, 'v')) {
      current = current.v
    }
  }
  return current
}

async function authHeaders(page) {
  const cache = await page.evaluate(() =>
    Object.fromEntries(
      Array.from({ length: localStorage.length }, (_, index) => {
        const key = localStorage.key(index)
        return [key, localStorage.getItem(key)]
      })
    )
  )
  const accessToken = unwrapCacheValue(cache.ACCESS_TOKEN)
  const tenantId = unwrapCacheValue(cache.tenantId)
  const visitTenantId = unwrapCacheValue(cache.visitTenantId)
  assert.ok(accessToken, 'ACCESS_TOKEN missing after real login')
  assert.ok(tenantId, 'tenantId missing after real login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
  if (visitTenantId) {
    headers['visit-tenant-id'] = String(visitTenantId)
  }
  return headers
}

async function apiGet(page, apiPath) {
  const headers = await authHeaders(page)
  const response = await page.evaluate(
    async ({ url, requestHeaders }) => {
      const result = await fetch(url, { headers: requestHeaders })
      return {
        ok: result.ok,
        status: result.status,
        json: await result.json()
      }
    },
    {
      url: `${config.baseUrl}/admin-api${apiPath}`,
      requestHeaders: headers
    }
  )
  assert.ok(response.ok && response.json.code === 0, `GET ${apiPath} failed: ${JSON.stringify(response)}`)
  return response.json.data
}

async function findTemplateCandidate(page) {
  for (let pageNo = 1; pageNo <= 20; pageNo += 1) {
    const data = await apiGet(page, `/form-center/template-pool?pageNo=${pageNo}&pageSize=50`)
    const rows = Array.isArray(data?.list) ? data.list : []
    const candidate = rows.find((row) => row.status !== 'OBSOLETE' && row.status !== 'PENDING_APPROVAL')
    if (candidate) {
      return { candidate, pageNo }
    }
    if (rows.length < 50) break
  }
  throw new Error('模板列表中未找到可用于模板编辑的模板')
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function login(page) {
  logStep('goto login page')
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 120000 })
  logStep('login page committed')
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 120000 })
  logStep('login form visible')
  await selectTenant(page, form)
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 120000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_payload_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 120000, waitUntil: 'commit' })
  logStep(`login finished: ${page.url()}`)
}

async function waitForRow(page, rowSelector, index) {
  const rows = page.locator(rowSelector)
  await rows.nth(index).waitFor({ state: 'visible', timeout: 120000 })
  return rows.nth(index)
}

async function applyTemplateNameQuickFilter(page, templateName, versionNo) {
  logStep(`apply template quick filter: ${templateName} / ${versionNo}`)
  const quickFilter = page.locator('[data-table-key="form.center.template"]')
  const fieldSelect = quickFilter.locator('.table-quick-filter__field input[role="combobox"]').first()
  await fieldSelect.click({ force: true })
  logStep('template quick filter field opened')
  await fieldSelect.fill('模板名称')
  logStep('template quick filter field typed')
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '模板名称' }).first().click({ force: true })
  logStep('template quick filter field selected')
  const valueInput = quickFilter.locator('.table-quick-filter__value input').first()
  await valueInput.waitFor({ state: 'visible', timeout: 120000 })
  logStep('template quick filter value input visible')
  await valueInput.fill(templateName)
  logStep('template quick filter value typed')
  await quickFilter.getByRole('button', { name: '查询' }).click()
  logStep('template quick filter query clicked')
  const targetRow = page
    .locator('.el-table__body-wrapper tbody tr.el-table__row')
    .filter({ hasText: templateName })
    .filter({ hasText: versionNo })
    .first()
  await targetRow.waitFor({ state: 'visible', timeout: 120000 })
  logStep('template target row visible after quick filter')
  return targetRow
}

async function openTemplateDesignerFromEdit(page) {
  logStep('find template candidate')
  const targetResult = await findTemplateCandidate(page)
  const targetRow = targetResult.candidate
  logStep(`template candidate found: ${targetRow.templateName} / ${targetRow.versionNo} @ page ${targetResult.pageNo}`)
  await page.goto(`${config.baseUrl}/mdm/form-center/template?pageNo=${targetResult.pageNo}&pageSize=50`, {
    waitUntil: 'commit',
    timeout: 120000
  })
  logStep('template list page committed')
  await page.getByText('表单模板', { exact: false }).first().waitFor({ state: 'visible', timeout: 120000 })
  logStep('template title visible')
  const row = page
    .locator('.el-table__body-wrapper tbody tr.el-table__row')
    .filter({ hasText: targetRow.templateName })
    .filter({ hasText: targetRow.versionNo })
    .first()
  await row.waitFor({ state: 'visible', timeout: 120000 })
  logStep('template target row visible')
  await row.scrollIntoViewIfNeeded()
  await row.click()
  logStep(`template row selected: ${targetRow.templateName} / ${targetRow.versionNo}`)

  const editButton = page.locator('.form-template-preview__actions').getByRole('button', { name: '编辑' }).first()
  await editButton.waitFor({ state: 'visible', timeout: 120000 })
  logStep('template edit button visible')
  const navigationPromise = page.waitForURL(
    (url) =>
      url.pathname === '/mdm/form-center/template' &&
      url.searchParams.get('mode') === 'designer' &&
      url.searchParams.get('templateMode') === 'edit',
    { timeout: 120000, waitUntil: 'commit' }
  )
  await editButton.click()
  logStep('template edit clicked')
  await navigationPromise
  logStep(`template designer url reached: ${page.url()}`)
  await page.locator('.form-template-route-workspace').first().waitFor({ state: 'visible', timeout: 120000 })
  await page.getByText('规则编辑模式：左侧只选单元格，右侧切换可填写/不可填写').first().waitFor({
    state: 'visible',
    timeout: 120000
  })
  const ruleCellButton = page
    .locator(
      '.form-template-route-workspace .batch-record-cell-rules-editor__cell.is-rule .batch-record-cell-rules-editor__cell-button'
    )
    .first()
  await ruleCellButton.waitFor({ state: 'visible', timeout: 120000 })
  logStep(`rule cell buttons found: ${await page.locator('.form-template-route-workspace .batch-record-cell-rules-editor__cell.is-rule .batch-record-cell-rules-editor__cell-button').count()}`)
  await ruleCellButton.scrollIntoViewIfNeeded({ timeout: 30000 })
  await ruleCellButton.click({ timeout: 30000 })
  logStep('rule cell clicked')
  await page.getByText('是否可填写', { exact: true }).first().waitFor({
    state: 'visible',
    timeout: 120000
  })
  await page.getByText('字段名称', { exact: true }).first().waitFor({
    state: 'visible',
    timeout: 120000
  })
  await page.getByText('字段类型', { exact: true }).first().waitFor({
    state: 'visible',
    timeout: 120000
  })
  await page.screenshot({ path: config.screenshotPath, fullPage: false })
  return {
    templateName: targetRow.templateName,
    versionNo: targetRow.versionNo,
    url: page.url(),
    workspace: 'form-template',
    screenshotPath: config.screenshotPath
  }
}

function normalizeDesignerUrl(rawUrl) {
  const url = new URL(rawUrl)
  return {
    pathname: url.pathname,
    mode: url.searchParams.get('mode'),
    reportMode: url.searchParams.get('reportMode'),
    reportId: url.searchParams.get('reportId')
  }
}

async function main() {
  assertPrerequisites()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(120000)
  page.setDefaultNavigationTimeout(120000)
  page.on('console', (message) => console.log(`[console:${message.type()}] ${message.text()}`))
  page.on('pageerror', (error) => console.log(`[pageerror] ${error.message}`))
  page.on('requestfailed', (request) =>
    console.log(`[requestfailed] ${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`)
  )

  try {
    await login(page)
    const templateFlow = await openTemplateDesignerFromEdit(page)

    const templateRoute = normalizeDesignerUrl(templateFlow.url)

    assert.equal(
      templateRoute.pathname,
      '/mdm/form-center/template',
      `表单模板“编辑”没有留在表单模板工作区: ${templateFlow.url}`
    )
    assert.equal(templateRoute.mode, 'designer', `表单模板“编辑”模式异常: ${templateFlow.url}`)
    assert.equal(templateRoute.reportMode, null, `表单模板“编辑”不应携带 reportMode: ${templateFlow.url}`)
    assert.equal(templateRoute.reportId, null, `表单模板“编辑”不应携带 reportId: ${templateFlow.url}`)

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          username: config.username,
          templateName: templateFlow.templateName,
          templateVersionNo: templateFlow.versionNo,
          templateEditUrl: templateFlow.url,
          workspace: templateFlow.workspace,
          screenshotPath: templateFlow.screenshotPath
        },
        null,
        2
      )
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
