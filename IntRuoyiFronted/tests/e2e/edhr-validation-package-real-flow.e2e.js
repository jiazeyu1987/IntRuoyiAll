const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_VALIDATION_E2E_BASE_URL || 'http://127.0.0.1:8109').replace(/\/+$/, ''),
  backendUrl: (process.env.EDHR_VALIDATION_E2E_BACKEND_URL || 'http://127.0.0.1:48109').replace(/\/+$/, ''),
  tenant: process.env.EDHR_VALIDATION_E2E_TENANT || '测试租户',
  username: process.env.EDHR_VALIDATION_E2E_USERNAME || 'aoteman',
  password: process.env.EDHR_VALIDATION_E2E_PASSWORD || '111111',
  headed: process.env.EDHR_VALIDATION_E2E_HEADED === '1',
  targetPath: '/mes/pro/feedback/edhr-validation'
}

const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
const packageName = process.env.EDHR_VALIDATION_E2E_PACKAGE_NAME || `T6验证包矩阵E2E-${stamp}`
const customerProjectName = `瑛泰eDHR商业化验证-${stamp}`
const releaseTag = `edhr-validation-${stamp}`
const schemaVersion = `schema-20260618-${stamp}`
const runtimeDir = path.resolve(__dirname, '../../.runtime/edhr-validation-package')
const screenshotPath = path.join(runtimeDir, `validation-package-${stamp}.png`)

const itemDefinitions = [
  { type: 'URS', label: 'URS', code: `URS-${stamp}`, name: '批记录商业化用户需求', role: '验证负责人' },
  { type: 'FRS', label: 'FRS', code: `FRS-${stamp}`, name: '批记录功能规格', role: '系统负责人' },
  { type: 'RISK', label: '风险', code: `RISK-${stamp}`, name: '批记录风险评估', role: 'QA' },
  { type: 'IQ', label: 'IQ', code: `IQ-${stamp}`, name: '部署安装确认', role: '实施工程师' },
  { type: 'OQ', label: 'OQ', code: `OQ-${stamp}`, name: '操作确认脚本', role: '验证执行人' },
  { type: 'PQ', label: 'PQ', code: `PQ-${stamp}`, name: '性能确认脚本', role: '验证执行人' }
]

function assertPrerequisites() {
  assert.equal(config.baseUrl, 'http://127.0.0.1:8109', 'E2E must use this worktree frontend port 8109.')
  assert.equal(config.backendUrl, 'http://127.0.0.1:48109', 'E2E must use this worktree backend port 48109.')
  assert.equal(config.tenant, '测试租户', 'E2E must use 测试租户 for write verification.')
  assert.equal(config.username, 'aoteman', 'E2E must use the test tenant account aoteman.')
  assert.ok(config.password, 'EDHR_VALIDATION_E2E_PASSWORD is required.')
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

async function selectDialogOption(page, dialog, label, optionText) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await item.locator('.el-select').first().click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function createValidationPackage(page) {
  await page.getByRole('button', { name: /新建验证包/ }).first().click()
  const dialog = page.getByRole('dialog', { name: /新建验证包/ }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  await fillDialogInput(dialog, '验证包名称', packageName)
  await fillDialogInput(dialog, '客户项目', customerProjectName)
  await fillDialogInput(dialog, '客户名称', '瑛泰医疗')
  await fillDialogInput(dialog, '客户现场', '测试租户现场')
  await fillDialogInput(dialog, '系统范围', 'eDHR批记录、电子签名、审计追踪、放行与交付证据')
  await fillDialogInput(dialog, '验证范围', 'CSV基础信息、URS/FRS/风险/IQ/OQ/PQ追溯矩阵')
  await fillDialogInput(dialog, '发布标签', releaseTag)
  await fillDialogInput(dialog, 'schema版本', schemaVersion)
  await fillDialogInput(dialog, '验证负责人', 'QA负责人')
  await fillDialogInput(dialog, 'QA负责人', 'QA复核人')
  await fillDialogInput(dialog, '备注', 'T6真实E2E创建验证包矩阵对象')

  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-validation-package/create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '创建' }).click()
  const createResponse = await createResponsePromise
  const createPayload = await createResponse.json()
  assert.ok(
    createResponse.ok() && [0, 200].includes(createPayload.code),
    `create validation package failed: HTTP ${createResponse.status()} ${JSON.stringify(createPayload).slice(0, 1000)}`
  )
  assert.ok(createPayload.data?.id, 'created validation package response must include id')
  assert.equal(createPayload.data.validationStatus, 'BLOCKED', 'new validation package must start as BLOCKED')
  assert.equal(createPayload.data.oqReady, false, 'new validation package must not be OQ Ready')
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  return createPayload.data
}

async function assertPackageVisibleAndSelected(page, validationPackage) {
  await page.getByText(packageName, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const row = page.locator('.el-table__body tr').filter({ hasText: packageName }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })

  const itemResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-validation-requirement-item/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await row.click()
  const itemResponse = await itemResponsePromise
  const itemPayload = await itemResponse.json()
  assert.equal(itemPayload.code, 0, `item page business code must be 0: ${itemPayload.msg}`)

  const bodyText = await page.locator('body').innerText({ timeout: 30000 })
  for (const expectedText of [
    'CSV基础信息',
    '验证条目',
    '追溯矩阵',
    validationPackage.packageCode,
    customerProjectName,
    releaseTag,
    schemaVersion,
    '阻塞'
  ]) {
    assert.ok(bodyText.includes(expectedText), `expected package text missing: ${expectedText}`)
  }
}

async function createRequirementItem(page, definition) {
  await page.getByRole('button', { name: /登记条目/ }).first().click()
  const dialog = page.getByRole('dialog', { name: /登记验证条目/ }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  await selectDialogOption(page, dialog, '条目类型', definition.label)
  await fillDialogInput(dialog, '条目编号', definition.code)
  await fillDialogInput(dialog, '条目名称', definition.name)
  await fillDialogInput(dialog, '版本', 'v1')
  await fillDialogInput(dialog, '责任人', `${definition.role}-${stamp}`)
  await fillDialogInput(dialog, '签核角色', definition.role)
  await fillDialogInput(dialog, '来源文档', `${definition.type}-source-${stamp}.docx`)
  await fillDialogInput(dialog, '业务过程', 'eDHR商业化验证')
  await fillDialogInput(dialog, '验收标准', `${definition.type} 条目需进入追溯矩阵`)

  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-validation-requirement-item/create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '登记' }).click()
  const createResponse = await createResponsePromise
  const createPayload = await createResponse.json()
  assert.ok(
    createResponse.ok() && [0, 200].includes(createPayload.code),
    `create item ${definition.type} failed: HTTP ${createResponse.status()} ${JSON.stringify(createPayload).slice(0, 1000)}`
  )
  assert.ok(createPayload.data?.id, `created item ${definition.type} response must include id`)
  assert.equal(createPayload.data.itemType, definition.type, `created item type mismatch for ${definition.type}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  await page.getByText(definition.code, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return createPayload.data
}

async function evaluateTrace(page, expectedReady) {
  const evaluateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-validation-package/evaluate-trace') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: /计算OQ Ready/ }).first().click()
  const evaluateResponse = await evaluateResponsePromise
  const evaluatePayload = await evaluateResponse.json()
  assert.ok(
    evaluateResponse.ok() && [0, 200].includes(evaluatePayload.code),
    `evaluate trace failed: HTTP ${evaluateResponse.status()} ${JSON.stringify(evaluatePayload).slice(0, 1000)}`
  )
  assert.equal(evaluatePayload.data.oqReady, expectedReady, `OQ Ready expected ${expectedReady}`)
  return evaluatePayload.data
}

async function createTraceLink(page, targetDefinition) {
  await page.getByRole('button', { name: /建立追溯/ }).first().click()
  const dialog = page.getByRole('dialog', { name: /建立追溯关系/ }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })

  await selectDialogOption(page, dialog, '目标条目', targetDefinition.code)
  await fillDialogInput(dialog, '责任人', `矩阵责任人-${stamp}`)
  await fillDialogInput(dialog, '下一步动作', `${targetDefinition.type} 追溯已登记，重新计算OQ Ready`)

  const createResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-validation-trace-link/create') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: '建立' }).click()
  const createResponse = await createResponsePromise
  const createPayload = await createResponse.json()
  assert.ok(
    createResponse.ok() && [0, 200].includes(createPayload.code),
    `create trace link to ${targetDefinition.type} failed: HTTP ${createResponse.status()} ${JSON.stringify(createPayload).slice(0, 1000)}`
  )
  assert.ok(createPayload.data?.id, `created trace link to ${targetDefinition.type} response must include id`)
  assert.equal(createPayload.data.targetItemType, targetDefinition.type, `target item type mismatch for ${targetDefinition.type}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  return createPayload.data
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
  const businessResponses = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('response', (response) => {
    if (response.url().includes('/admin-api/mes/pro/edhr-validation')) {
      businessResponses.push({ url: response.url(), status: response.status(), method: response.request().method() })
    }
  })

  try {
    const permissionInfo = await loginAndCapturePermissionInfo(page)
    const permissions = new Set(permissionInfo.permissions || [])
    const menus = flattenMenus(permissionInfo.menus || permissionInfo.menuList || [])
    for (const permission of [
      'mes:pro-edhr-validation:query',
      'mes:pro-edhr-validation:create',
      'mes:pro-edhr-validation:evaluate-trace'
    ]) {
      assert.ok(permissions.has(permission), `permission missing: ${permission}`)
    }
    assert.ok(
      menus.some((menu) => menu?.component === 'mes/pro/edhr-validation/ValidationPage'),
      'validation package dynamic menu component missing'
    )

    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('验证包', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    const validationPackage = await createValidationPackage(page)
    await assertPackageVisibleAndSelected(page, validationPackage)

    const createdItems = []
    for (const itemDefinition of itemDefinitions) {
      createdItems.push(await createRequirementItem(page, itemDefinition))
    }
    assert.equal(createdItems.length, 6, 'all URS/FRS/RISK/IQ/OQ/PQ items must be created through the UI')

    const blockedSummary = await evaluateTrace(page, false)
    assert.equal(blockedSummary.validationStatus, 'BLOCKED', 'trace break must keep validation package BLOCKED')
    assert.equal(blockedSummary.brokenTraceCount, 3, 'URS without links must report FRS/RISK/IQ/OQ/PQ gaps')
    assert.ok(
      blockedSummary.brokenItems.some((item) => item.missingItemType === 'FRS'),
      'broken trace must include missing FRS'
    )
    assert.ok(
      blockedSummary.brokenItems.some((item) => item.missingItemType === 'RISK'),
      'broken trace must include missing RISK'
    )
    assert.ok(
      blockedSummary.brokenItems.some((item) => item.missingItemType === 'IQ/OQ/PQ'),
      'broken trace must include missing IQ/OQ/PQ'
    )

    for (const targetDefinition of itemDefinitions.filter((item) => item.type !== 'URS')) {
      await createTraceLink(page, targetDefinition)
    }

    const readySummary = await evaluateTrace(page, true)
    assert.equal(readySummary.validationStatus, 'PREPARED', 'complete trace matrix must move package to PREPARED')
    assert.equal(readySummary.traceStatus, 'READY', 'complete trace matrix must report READY trace status')
    assert.equal(readySummary.brokenTraceCount, 0, 'complete trace matrix must not have broken items')
    assert.equal(readySummary.ursCount, 1, 'one URS item must be counted')
    assert.equal(readySummary.frsCount, 1, 'one FRS item must be counted')
    assert.equal(readySummary.riskCount, 1, 'one RISK item must be counted')
    assert.equal(readySummary.iqCount, 1, 'one IQ item must be counted')
    assert.equal(readySummary.oqCount, 1, 'one OQ item must be counted')
    assert.equal(readySummary.pqCount, 1, 'one PQ item must be counted')
    assert.equal(readySummary.traceLinkCount, 5, 'URS must be linked to FRS/RISK/IQ/OQ/PQ')
    assert.equal(
      readySummary.summary,
      '所有URS均已追溯到FRS、风险和至少一个IQ/OQ/PQ验证项',
      'complete trace response must include the matrix integrity summary'
    )

    await page.getByText('OQ Ready', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    await page.waitForFunction(
      (name) =>
        Array.from(document.querySelectorAll('.el-table__body tr')).some(
          (row) => row.innerText.includes(name) && row.innerText.includes('PREPARED')
        ),
      packageName,
      { timeout: 60000 }
    )
    const bodyText = await page.locator('body').innerText({ timeout: 30000 })
    for (const expectedText of [
      '进入OQ脚本准备',
      packageName,
      customerProjectName,
      releaseTag,
      schemaVersion,
      'READY'
    ]) {
      assert.ok(bodyText.includes(expectedText), `expected ready-state text missing: ${expectedText}`)
    }

    for (const businessResponse of businessResponses) {
      assert.ok(
        businessResponse.url.startsWith(`${config.backendUrl}/admin-api/`),
        `business request must target this worktree backend: ${businessResponse.url}`
      )
      assert.ok(businessResponse.status < 400, `business response failed: ${JSON.stringify(businessResponse)}`)
    }
    await page.screenshot({ path: screenshotPath, fullPage: true })

    assert.deepEqual(pageErrors, [], `page errors were emitted: ${pageErrors.join(' | ')}`)
    console.log(
      `PASS: eDHR验证包矩阵真实E2E packageId=${validationPackage.id}, packageCode=${validationPackage.packageCode}, items=${createdItems.length}, traceLinks=5, screenshot=${screenshotPath}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
