const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_OQ_PQ_E2E_BASE_URL || 'http://127.0.0.1:8110').replace(/\/+$/, ''),
  backendUrl: (process.env.EDHR_OQ_PQ_E2E_BACKEND_URL || 'http://127.0.0.1:48110').replace(/\/+$/, ''),
  tenant: process.env.EDHR_OQ_PQ_E2E_TENANT || '测试租户',
  username: process.env.EDHR_OQ_PQ_E2E_USERNAME || 'aoteman',
  password: process.env.EDHR_OQ_PQ_E2E_PASSWORD || '111111',
  headed: process.env.EDHR_OQ_PQ_E2E_HEADED === '1',
  targetPath: '/mes/pro/feedback/edhr-oq-pq'
}

const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
const runtimeDir = path.resolve(__dirname, '../../.runtime/edhr-oq-pq')
const screenshotPath = path.join(runtimeDir, `oq-pq-${stamp}.png`)
const oqCaseCode = `OQ-T6-03-${stamp}`
const pqCaseCode = `PQ-T6-03-${stamp}`

function assertPrerequisites() {
  assert.equal(config.baseUrl, 'http://127.0.0.1:8110', 'E2E must use this worktree frontend port 8110.')
  assert.equal(config.backendUrl, 'http://127.0.0.1:48110', 'E2E must use this worktree backend port 48110.')
  assert.equal(config.tenant, '测试租户', 'E2E must use 测试租户 for write verification.')
  assert.equal(config.username, 'aoteman', 'E2E must use the test tenant account aoteman.')
  assert.ok(config.password, 'EDHR_OQ_PQ_E2E_PASSWORD is required.')
}

function flattenMenus(list, result = []) {
  for (const item of Array.isArray(list) ? list : []) {
    result.push(item)
    flattenMenus(item.children, result)
  }
  return result
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

async function clickRowByText(page, tableSelector, text, label) {
  const row = page.locator(`${tableSelector} .el-table__body tr`).filter({ hasText: text }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.click()
  return row
}

async function waitBusinessResponse(page, urlPart, method, action) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(urlPart) && item.request().method() === method,
    { timeout: 60000 }
  )
  const payload = await response.json()
  return { response, payload, action }
}

async function selectFirstOqReadyPackage(page) {
  await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('OQ/PQ执行台', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('OQ Ready', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const row = page.locator('.edhr-oq-pq__packages .el-table__body tr').filter({ hasText: 'OQ Ready' }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  const rowText = await row.innerText()
  const packageCodeMatch = rowText.match(/EDHR-VAL-[0-9A-Za-z-]+/)
  assert.ok(packageCodeMatch, `selected package row must include package code: ${rowText}`)
  await row.click()
  await page.getByText('创建用例', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return { packageCode: packageCodeMatch[0], rowText }
}

async function createCase(page, caseType) {
  const caseCode = caseType === 'OQ' ? oqCaseCode : pqCaseCode
  await page.getByRole('button', { name: /创建用例/ }).first().click()
  const dialog = page.getByRole('dialog', { name: /创建OQ\/PQ用例/ }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await selectDialogOption(page, dialog, '用例类型', caseType)
  await fillDialogInput(dialog, '用例编号', caseCode)
  await fillDialogInput(dialog, '用例名称', `${caseType}商业化执行脚本-${stamp}`)
  await fillDialogInput(dialog, '用例版本', 'v1')
  await fillDialogInput(dialog, '步骤编号', caseType === 'OQ' ? 'OQ-01' : 'PQ-01')
  await fillDialogInput(dialog, '步骤标题', caseType === 'OQ' ? '关键功能执行确认' : '真实业务路径性能确认')
  await fillDialogInput(dialog, '预期结果', `${caseType}步骤按受控证据完成且可追溯`)
  await fillDialogInput(dialog, '证据要求', '页面截图、执行日志、证据checksum')
  await fillDialogInput(dialog, '责任人', `${caseType}执行人-${stamp}`)
  await fillDialogInput(dialog, '复核人', `${caseType}复核人-${stamp}`)

  const responsePromise = waitBusinessResponse(page, '/admin-api/mes/pro/edhr-oq-pq/case/create', 'POST', `create ${caseType} case`)
  await dialog.getByRole('button', { name: '创建' }).click()
  const { response, payload } = await responsePromise
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `create ${caseType} case failed: HTTP ${response.status()} ${JSON.stringify(payload).slice(0, 1000)}`
  )
  assert.equal(payload.data.caseCode, caseCode, `${caseType} case code mismatch`)
  assert.equal(payload.data.caseType, caseType, `${caseType} case type mismatch`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  await clickRowByText(page, '.edhr-oq-pq__panel', caseCode, `${caseType} case row`)
  return payload.data
}

async function createRun(page, caseType, options = {}) {
  await page.getByRole('button', { name: /创建执行/ }).first().click()
  const dialog = page.getByRole('dialog', { name: /创建执行记录/ }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillDialogInput(dialog, '执行环境', 'local-test-tenant-122')
  await fillDialogInput(dialog, '发布标签', `release-t6-03-${stamp}`)
  await fillDialogInput(dialog, 'schema版本', `schema-t6-03-${stamp}`)
  await fillDialogInput(dialog, '执行人', `${caseType}执行人-${stamp}`)
  await fillDialogInput(dialog, '复核人', `${caseType}复核人-${stamp}`)
  if (caseType === 'PQ') {
    await fillDialogInput(dialog, '真实业务路径', '测试租户真实批记录创建、执行、复核路径')
    if (options.realTestDataSource) {
      await fillDialogInput(dialog, '真实测试数据来源', options.realTestDataSource)
    }
    await fillDialogInput(dialog, '目标环境证明', 'http://127.0.0.1:8110 测试租户真实页面')
  }
  await fillDialogInput(dialog, '附件证据', `${caseType.toLowerCase()}-run-evidence-${stamp}`)
  await fillDialogInput(dialog, '证据checksum', `sha256-${caseType.toLowerCase()}-run-${stamp}`)

  const responsePromise = waitBusinessResponse(page, '/admin-api/mes/pro/edhr-oq-pq/run/create', 'POST', `create ${caseType} run`)
  await dialog.getByRole('button', { name: '创建' }).click()
  const { response, payload } = await responsePromise
  if (options.expectFailure) {
    const message = payload.msg || payload.message || JSON.stringify(payload)
    assert.notEqual(payload.code, 0, `create ${caseType} run should fail`)
    assert.ok(message.includes(options.expectFailure), `expected ${options.expectFailure}, got ${message}`)
    await page.getByText(options.expectFailure, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    return { dialog, failedPayload: payload }
  }
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `create ${caseType} run failed: HTTP ${response.status()} ${JSON.stringify(payload).slice(0, 1000)}`
  )
  assert.equal(payload.data.caseType, caseType, `${caseType} run case type mismatch`)
  assert.equal(payload.data.runStatus, 'RUNNING', `${caseType} run must start RUNNING`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  await page.getByText(payload.data.runCode, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await clickRowByText(page, '.edhr-oq-pq__panel', payload.data.runCode, `${caseType} run row`)
  return payload.data
}

async function submitStep(page, stepResult, caseType) {
  await page.getByRole('button', { name: stepResult === 'FAIL' ? /提交失败步骤/ : /提交通过步骤/ }).first().click()
  const dialog = page.getByRole('dialog', { name: /提交步骤结果/ }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await selectDialogOption(page, dialog, '步骤结果', stepResult)
  await fillDialogInput(
    dialog,
    '实际结果',
    stepResult === 'FAIL'
      ? `${caseType}实际结果失败-${stamp}，触发偏差`
      : `${caseType}复测后实际结果通过-${stamp}`
  )
  await fillDialogInput(dialog, '附件证据', `${caseType.toLowerCase()}-${stepResult.toLowerCase()}-step-evidence-${stamp}`)
  await fillDialogInput(dialog, '证据checksum', `sha256-${caseType.toLowerCase()}-${stepResult.toLowerCase()}-${stamp}`)

  const responsePromise = waitBusinessResponse(page, '/admin-api/mes/pro/edhr-oq-pq/run/submit-step', 'POST', `submit ${stepResult}`)
  await dialog.getByRole('button', { name: '提交' }).click()
  const { response, payload } = await responsePromise
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `submit ${stepResult} step failed: HTTP ${response.status()} ${JSON.stringify(payload).slice(0, 1000)}`
  )
  assert.equal(payload.data.stepResult, stepResult, `step result mismatch: ${stepResult}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  return payload.data
}

async function expectCompleteBlockedByOpenDeviation(page) {
  const responsePromise = waitBusinessResponse(page, '/admin-api/mes/pro/edhr-oq-pq/run/complete', 'POST', 'complete blocked run')
  await page.getByRole('button', { name: /完成执行/ }).first().click()
  const { payload } = await responsePromise
  const message = payload.msg || payload.message || JSON.stringify(payload)
  assert.notEqual(payload.code, 0, 'complete run with open deviation should fail')
  assert.ok(message.includes('开放偏差'), `expected open deviation error, got ${message}`)
  await page.getByText('开放偏差', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

async function updateDeviation(page, action, fields, expectedStatus) {
  const buttonName = action === 'remediate' ? /登记整改/ : action === 'retest' ? /登记复测/ : /关闭偏差/
  const urlPart =
    action === 'remediate'
      ? '/admin-api/mes/pro/edhr-oq-pq/deviation/remediate'
      : action === 'retest'
        ? '/admin-api/mes/pro/edhr-oq-pq/deviation/retest'
        : '/admin-api/mes/pro/edhr-oq-pq/deviation/close'
  await page.locator('.edhr-oq-pq__deviation').getByRole('button', { name: buttonName }).first().click()
  const dialog = page.getByRole('dialog').filter({ hasText: action === 'close' ? '关闭偏差' : action === 'retest' ? '登记复测结果' : '登记整改措施' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  for (const [label, value] of Object.entries(fields)) {
    await fillDialogInput(dialog, label, value)
  }
  const responsePromise = waitBusinessResponse(page, urlPart, 'POST', `deviation ${action}`)
  await dialog.getByRole('button', { name: '保存' }).click()
  const { response, payload } = await responsePromise
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `deviation ${action} failed: HTTP ${response.status()} ${JSON.stringify(payload).slice(0, 1000)}`
  )
  assert.equal(payload.data.deviationStatus, expectedStatus, `deviation status must be ${expectedStatus}`)
  await dialog.waitFor({ state: 'hidden', timeout: 60000 })
  await page.getByText(expectedStatus, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return payload.data
}

async function completeRun(page, caseType) {
  const responsePromise = waitBusinessResponse(page, '/admin-api/mes/pro/edhr-oq-pq/run/complete', 'POST', `complete ${caseType} run`)
  await page.getByRole('button', { name: /完成执行/ }).first().click()
  const { response, payload } = await responsePromise
  assert.ok(
    response.ok() && [0, 200].includes(payload.code),
    `complete ${caseType} run failed: HTTP ${response.status()} ${JSON.stringify(payload).slice(0, 1000)}`
  )
  assert.equal(payload.data.runStatus, 'PASSED', `${caseType} run must be PASSED`)
  await page.getByText('PASSED', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  return payload.data
}

async function main() {
  assertPrerequisites()
  fs.mkdirSync(runtimeDir, { recursive: true })
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const oqPqResponses = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('response', (response) => {
    if (response.url().includes('/admin-api/mes/pro/edhr-oq-pq')) {
      oqPqResponses.push({ url: response.url(), status: response.status(), method: response.request().method() })
    }
  })

  try {
    const permissionInfo = await loginAndCapturePermissionInfo(page)
    const permissions = new Set(permissionInfo.permissions || [])
    const menus = flattenMenus(permissionInfo.menus || permissionInfo.menuList || [])
    for (const permission of [
      'mes:pro-edhr-oq-pq:query',
      'mes:pro-edhr-oq-pq:create',
      'mes:pro-edhr-oq-pq:execute',
      'mes:pro-edhr-oq-pq:retest',
      'mes:pro-edhr-oq-pq:close'
    ]) {
      assert.ok(permissions.has(permission), `permission missing: ${permission}`)
    }
    assert.ok(
      menus.some((menu) => menu?.component === 'mes/pro/edhr-oq-pq/OqPqPage'),
      'OQ/PQ dynamic menu component missing'
    )

    const selectedPackage = await selectFirstOqReadyPackage(page)

    const oqCase = await createCase(page, 'OQ')
    const oqRun = await createRun(page, 'OQ')
    const failedStep = await submitStep(page, 'FAIL', 'OQ')
    assert.ok(failedStep.deviationId, 'OQ failed step must be linked to generated deviation')
    await page.getByText('DEVIATION_OPEN', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    await page.getByText('OPEN', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    await expectCompleteBlockedByOpenDeviation(page)
    const remediated = await updateDeviation(
      page,
      'remediate',
      {
        原因分析: `OQ失败根因-${stamp}`,
        整改措施: `修正配置并补充执行证据-${stamp}`,
        整改责任人: `整改责任人-${stamp}`
      },
      'REMEDIATED'
    )
    const retested = await updateDeviation(
      page,
      'retest',
      {
        复测结果: `复测通过-${stamp}`,
        复测证据: `oq-retest-evidence-${stamp}`,
        复测复核人: `复测复核人-${stamp}`
      },
      'RETESTED'
    )
    const closed = await updateDeviation(page, 'close', { 关闭签核: `关闭签核人-${stamp}` }, 'CLOSED')
    await submitStep(page, 'PASS', 'OQ')
    const oqPassedRun = await completeRun(page, 'OQ')

    const pqCase = await createCase(page, 'PQ')
    const pqFailure = await createRun(page, 'PQ', {
      expectFailure: '真实业务路径、真实测试数据来源和目标环境证明'
    })
    await fillDialogInput(pqFailure.dialog, '真实测试数据来源', `测试租户真实批记录数据-${stamp}`)
    const pqResponsePromise = waitBusinessResponse(page, '/admin-api/mes/pro/edhr-oq-pq/run/create', 'POST', 'create PQ run after evidence')
    await pqFailure.dialog.getByRole('button', { name: '创建' }).click()
    const pqCreateResult = await pqResponsePromise
    assert.ok(
      pqCreateResult.response.ok() && [0, 200].includes(pqCreateResult.payload.code),
      `create PQ run after evidence failed: HTTP ${pqCreateResult.response.status()} ${JSON.stringify(pqCreateResult.payload).slice(0, 1000)}`
    )
    assert.equal(pqCreateResult.payload.data.runStatus, 'RUNNING', 'PQ run must start RUNNING after real data evidence is supplied')
    await pqFailure.dialog.waitFor({ state: 'hidden', timeout: 60000 })
    await clickRowByText(page, '.edhr-oq-pq__panel', pqCreateResult.payload.data.runCode, 'PQ run row')
    await submitStep(page, 'PASS', 'PQ')
    const pqPassedRun = await completeRun(page, 'PQ')

    for (const businessResponse of oqPqResponses) {
      assert.ok(
        businessResponse.url.startsWith(`${config.backendUrl}/admin-api/`),
        `OQ/PQ business request must target this worktree backend: ${businessResponse.url}`
      )
      assert.ok(businessResponse.status < 500, `OQ/PQ response had server error: ${JSON.stringify(businessResponse)}`)
    }

    await page.screenshot({ path: screenshotPath, fullPage: true })
    assert.deepEqual(pageErrors, [], `page errors were emitted: ${pageErrors.join(' | ')}`)
    console.log(
      [
        'PASS: eDHR OQ/PQ真实E2E',
        `package=${selectedPackage.packageCode}`,
        `oqCase=${oqCase.id}`,
        `oqRun=${oqRun.id}->${oqPassedRun.runStatus}`,
        `deviation=${closed.id}:${remediated.deviationStatus}->${retested.deviationStatus}->${closed.deviationStatus}`,
        `pqCase=${pqCase.id}`,
        `pqRun=${pqPassedRun.id}->${pqPassedRun.runStatus}`,
        `screenshot=${screenshotPath}`
      ].join(' ')
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
