const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_DELIVERY_E2E_BASE_URL || 'http://127.0.0.1:8097').replace(/\/+$/, ''),
  tenant: process.env.EDHR_DELIVERY_E2E_TENANT || '测试租户',
  username: process.env.EDHR_DELIVERY_E2E_USERNAME || 'aoteman',
  password: process.env.EDHR_DELIVERY_E2E_PASSWORD || '111111',
  headed: process.env.EDHR_DELIVERY_E2E_HEADED === '1',
  targetPath: '/mes/pro/feedback/edhr-delivery'
}

const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
const projectName = process.env.EDHR_DELIVERY_E2E_PROJECT_NAME || `T6交付驾驶舱E2E-${stamp}`
const releaseTag = `edhr-t6-e2e-${stamp}`
const schemaVersion = `schema-20260618-${stamp}`
const runtimeDir = path.resolve(__dirname, '../../.runtime/edhr-delivery')
const screenshotPath = path.join(runtimeDir, `delivery-${stamp}.png`)

const expectedPackages = [
  { packageCode: 'CSV_VALIDATION', packageName: 'CSV验证包', gateCode: 'GATE_CSV_VALIDATION', gateName: 'CSV验证资料' },
  { packageCode: 'OQ_PQ', packageName: 'OQ/PQ执行包', gateCode: 'GATE_OQ_PQ', gateName: 'OQ/PQ执行资料' },
  { packageCode: 'TRAINING', packageName: '培训签核包', gateCode: 'GATE_TRAINING', gateName: '培训资料' },
  { packageCode: 'DEPLOYMENT_AUTH', packageName: '部署授权包', gateCode: 'GATE_DEPLOYMENT_AUTH', gateName: '部署授权资料' },
  { packageCode: 'INTERFACE', packageName: '接口联调包', gateCode: 'GATE_INTERFACE', gateName: '接口联调资料' },
  { packageCode: 'OPERATIONS', packageName: '运维交接包', gateCode: 'GATE_OPERATIONS', gateName: '运维资料' }
]

function assertPrerequisites() {
  assert.equal(config.baseUrl, 'http://127.0.0.1:8097', 'E2E must use this worktree frontend port 8097.')
  assert.equal(config.tenant, '测试租户', 'E2E must use 测试租户 for write verification.')
  assert.equal(config.username, 'aoteman', 'E2E must use the test tenant account aoteman.')
  assert.ok(config.password, 'EDHR_DELIVERY_E2E_PASSWORD is required.')
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function selectTenant(page, loginForm, tenantName) {
  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill('')
    await tenantInput.fill(tenantName)
    const tenantOption = page.locator('.el-select-dropdown__item').filter({ hasText: tenantName }).first()
    if ((await tenantOption.count()) > 0) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
    return
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), tenantName, 'tenant')
}

function flattenMenus(list, result = []) {
  for (const item of Array.isArray(list) ? list : []) {
    result.push(item)
    flattenMenus(item.children, result)
  }
  return result
}

async function loginAndCapturePermissionInfo(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })

  const permissionInfoPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )

  if (page.url().includes('/login')) {
    const loginForm = page.locator('.login-form:visible').first()
    await loginForm.waitFor({ state: 'visible', timeout: 60000 })
    const captchaCount = await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()
    assert.equal(captchaCount, 0, 'Captcha is enabled; unattended real E2E cannot continue.')

    await selectTenant(page, loginForm, config.tenant)
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
    await fillFirstVisible(loginForm.locator('input[type="password"]'), config.password, 'password')

    const loginResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await loginForm.locator('.el-button--primary').first().click()
    const loginResponse = await loginResponsePromise
    const loginPayload = await loginResponse.json()
    assert.ok(
      loginResponse.ok() && [0, 200].includes(loginPayload.code),
      `login failed: HTTP ${loginResponse.status()} ${JSON.stringify(loginPayload).slice(0, 1000)}`
    )
  }

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  const permissionInfoResponse = await permissionInfoPromise
  const permissionPayload = await permissionInfoResponse.json()
  assert.equal(permissionPayload.code, 0, `permission info business code must be 0: ${permissionPayload.msg}`)
  return permissionPayload.data
}

async function fillDialogInput(dialog, label, value) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(item.locator('input, textarea'), value, label)
}

async function createDeliveryProject(page) {
  await page.getByRole('button', { name: /新建项目/ }).first().click()
  const dialog = page.getByRole('dialog', { name: /新建交付项目/ }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  await fillDialogInput(dialog, '项目名称', projectName)
  await fillDialogInput(dialog, '客户名称', '瑛泰医疗')
  await fillDialogInput(dialog, '客户现场', '测试租户现场')
  await fillDialogInput(dialog, '发布标签', releaseTag)
  await fillDialogInput(dialog, 'schema版本', schemaVersion)
  await fillDialogInput(dialog, '负责人', 'QA负责人')
  await fillDialogInput(dialog, '负责部门', '质量/IT')
  await fillDialogInput(dialog, '备注', 'T6真实E2E初始化交付证据对象')

  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-delivery-cockpit/project/create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '创建' }).click()
  const createResponse = await createResponsePromise
  const createPayload = await createResponse.json()
  assert.ok(
    createResponse.ok() && [0, 200].includes(createPayload.code),
    `create delivery project failed: HTTP ${createResponse.status()} ${JSON.stringify(createPayload).slice(0, 1000)}`
  )
  assert.ok(createPayload.data?.id, 'created delivery project response must include id')
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  return createPayload.data
}

async function assertProjectVisibleAndSelected(page, project) {
  await page.getByText(projectName, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const row = page.locator('.el-table__body tr').filter({ hasText: projectName }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })

  const packageResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/edhr-delivery-cockpit/evidence-package/page'),
    { timeout: 60000 }
  )
  const gateResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/edhr-delivery-cockpit/gate-summary'),
    { timeout: 60000 }
  )
  await row.click()
  const packageResponse = await packageResponsePromise
  const gateResponse = await gateResponsePromise
  const packagePayload = await packageResponse.json()
  const gatePayload = await gateResponse.json()
  assert.equal(packagePayload.code, 0, `package page business code must be 0: ${packagePayload.msg}`)
  assert.equal(gatePayload.code, 0, `gate summary business code must be 0: ${gatePayload.msg}`)
  assert.equal(packagePayload.data.total, 6, 'created project must initialize 6 evidence packages')
  assert.equal(gatePayload.data.gateCount, 6, 'created project must initialize 6 gate items')
  assert.equal(gatePayload.data.blockedCount, 6, 'all first-slice gate items must block signoff')
  assert.equal(gatePayload.data.signoffAllowed, false, 'first-slice project must not allow signoff')
  assert.equal(gatePayload.data.projectId, project.id, 'gate summary must belong to the created project')
}

async function assertDeliveryEvidenceVisible(page) {
  await page.getByText('不允许签核', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  const bodyText = await page.locator('body').innerText({ timeout: 30000 })
  for (const expected of expectedPackages) {
    assert.ok(bodyText.includes(expected.packageCode), `package code missing on page: ${expected.packageCode}`)
    assert.ok(bodyText.includes(expected.packageName), `package name missing on page: ${expected.packageName}`)
    assert.ok(bodyText.includes(expected.gateCode), `gate code missing on page: ${expected.gateCode}`)
    assert.ok(bodyText.includes(expected.gateName), `gate name missing on page: ${expected.gateName}`)
  }
  for (const expectedText of [
    '缺失证据',
    '责任人',
    '下一步动作',
    '签核影响',
    '恢复演练',
    '培训覆盖',
    releaseTag,
    schemaVersion,
    '存在缺失证据，当前不能进行商业化交付签核'
  ]) {
    assert.ok(bodyText.includes(expectedText), `expected delivery cockpit text missing: ${expectedText}`)
  }
}

async function main() {
  assertPrerequisites()
  fs.mkdirSync(runtimeDir, { recursive: true })
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    const permissionInfo = await loginAndCapturePermissionInfo(page)
    const permissions = new Set(permissionInfo.permissions || [])
    const menus = flattenMenus(permissionInfo.menus || permissionInfo.menuList || [])
    assert.ok(permissions.has('mes:pro-edhr-delivery:query'), 'query permission missing')
    assert.ok(permissions.has('mes:pro-edhr-delivery:create'), 'create permission missing')
    assert.ok(
      menus.some((menu) => menu?.component === 'mes/pro/edhr-delivery/DeliveryPage'),
      'delivery cockpit dynamic menu component missing'
    )

    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('交付驾驶舱', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    const project = await createDeliveryProject(page)
    await assertProjectVisibleAndSelected(page, project)
    await assertDeliveryEvidenceVisible(page)
    await page.screenshot({ path: screenshotPath, fullPage: true })

    assert.deepEqual(pageErrors, [], `page errors were emitted: ${pageErrors.join(' | ')}`)
    console.log(
      `PASS: eDHR交付驾驶舱真实E2E创建项目 ${projectName}, projectId=${project.id}, releaseTag=${releaseTag}, screenshot=${screenshotPath}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
