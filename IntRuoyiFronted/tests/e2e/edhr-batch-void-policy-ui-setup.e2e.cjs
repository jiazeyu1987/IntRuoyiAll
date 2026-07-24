const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.EDHR_BATCH_VOID_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.EDHR_BATCH_VOID_E2E_TENANT || '测试租户'
const USERNAME = process.env.EDHR_BATCH_VOID_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_BATCH_VOID_E2E_PASSWORD || '111111'
const TARGET_APPROVAL_MODE = (process.env.EDHR_BATCH_VOID_E2E_TARGET_APPROVAL_MODE || '').trim()
const TASK_DIR = process.env.EDHR_BATCH_VOID_E2E_TASK_DIR
  ? path.resolve(process.env.EDHR_BATCH_VOID_E2E_TASK_DIR)
  : path.resolve(
      __dirname,
      '../../../doc/tasks/20260721-batch-record-bpm-toggle-implementation/e2e-artifacts/edhr-batch-void'
    )
const RESULT_PATH = path.join(TASK_DIR, 'edhr-batch-void-policy-ui-setup.json')

function writeResult(result) {
  fs.mkdirSync(TASK_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, JSON.stringify(result, null, 2) + '\n', 'utf8')
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
  throw new Error(`missing visible input: ${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (!(await item.isVisible().catch(() => false))) {
      continue
    }
    if (!(await item.isDisabled().catch(() => true))) {
      await item.click()
      return
    }
  }
  throw new Error(`missing enabled control: ${label}`)
}

async function login(page) {
  await page.context().clearCookies().catch(() => undefined)
  await page.goto(BASE_URL + '/login?redirect=/index', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  }).catch(() => undefined)
  await page.goto(BASE_URL + '/login?redirect=/index', { waitUntil: 'domcontentloaded', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"]:visible, input.el-select__input:visible').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    await page.keyboard.press('Enter')
    await page.waitForTimeout(300)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    if (await option.isVisible().catch(() => false)) {
      await option.click()
    }
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'), USERNAME, 'username')
  await fillFirstVisible(form.locator('input[placeholder="请输入密码"]'), PASSWORD, 'password')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(form.getByRole('button', { name: /^登录$/ }), 'login')
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, 'login HTTP status ' + loginResponse.status())
  assert.ok([0, 200].includes(Number(loginPayload.code)), 'login business code ' + loginPayload.code)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function responseData(response, label) {
  const payload = await response.json()
  assert.ok([0, 200].includes(Number(payload.code)), label + ' business code ' + payload.code + ': ' + (payload.msg || ''))
  return payload.data
}

function findTargetPolicy(list) {
  return (list || []).find((item) =>
    item.dataDomain === 'MES' &&
    item.systemCode === 'MES' &&
    item.objectType === 'EDHR_BATCH_EXECUTION' &&
    item.actionCode === 'VOID' &&
    item.objectState === 'CLOSED' &&
    item.status === 'PUBLISHED'
  )
}

async function queryPolicy(page, label) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/form-center/policies') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const queryInput = page.locator('.form-center-policy__toolbar input[placeholder="动作编码"]').first()
  await queryInput.fill('VOID')
  await page.getByRole('button', { name: /查询/ }).click()
  const pageData = await responseData(await responsePromise, label)
  return {
    pageData,
    policy: findTargetPolicy(pageData.list || [])
  }
}

async function main() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'eDHR batch void policy E2E must use local frontend')
  assert.equal(TENANT, '测试租户', 'write E2E must use test tenant')
  assert.equal(USERNAME, 'aoteman', 'write E2E must use test tenant aoteman')
  assert.ok(
    TARGET_APPROVAL_MODE === '' || ['BPM_REQUIRED', 'DIRECT'].includes(TARGET_APPROVAL_MODE),
    'EDHR_BATCH_VOID_E2E_TARGET_APPROVAL_MODE must be BPM_REQUIRED, DIRECT or empty'
  )

  const browser = await chromium.launch({
    headless: process.env.EDHR_BATCH_VOID_E2E_HEADED !== '1',
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const pageErrors = []
  const writeRequests = []
  const observedResponses = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('request', (request) => {
    if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/')) {
      writeRequests.push({ method: request.method(), url: request.url() })
    }
  })
  page.on('response', (response) => {
    const url = response.url()
    if (url.includes('/form-center/')) {
      observedResponses.push({ method: response.request().method(), status: response.status(), url })
    }
  })

  try {
    await login(page)

    const initialPageResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/form-center/policies') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(BASE_URL + '/mdm/form-center/policy', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await responseData(await initialPageResponsePromise, 'policy initial page')

    const queried = await queryPolicy(page, 'eDHR batch void policy query')
    assert.ok(
      queried.policy,
      'BLOCKER: missing published MES EDHR_BATCH_EXECUTION VOID CLOSED form-center policy'
    )
    assert.equal(
      queried.policy.effectExecutorCode,
      'EDHR_BATCH_VOID',
      'eDHR batch void policy must use EDHR_BATCH_VOID effect executor'
    )
    assert.equal(
      queried.policy.bpmProcessKey,
      'mes-edhr-batch-execution-void-v1',
      'eDHR batch void policy must retain formal BPM process key'
    )

    let finalPolicy = queried.policy
    if (TARGET_APPROVAL_MODE && queried.policy.approvalMode !== TARGET_APPROVAL_MODE) {
      const row = page
        .locator('tr')
        .filter({ hasText: 'EDHR_BATCH_EXECUTION' })
        .filter({ hasText: 'VOID' })
        .filter({ hasText: 'CLOSED' })
        .first()
      await row.waitFor({ state: 'visible', timeout: 60000 })
      const switchResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/form-center/policies/' + queried.policy.id + '/switch-approval-mode') &&
          response.request().method() === 'POST',
        { timeout: 60000 }
      )
      await row.locator('.el-switch').first().click()
      const confirmBox = page.locator('.el-message-box:visible').first()
      await confirmBox.waitFor({ state: 'visible', timeout: 60000 })
      await clickFirstEnabled(confirmBox.getByRole('button', { name: /确\s*定/ }), 'confirm switch approval mode')
      finalPolicy = await responseData(await switchResponsePromise, 'switch approval mode')
      assert.equal(
        finalPolicy.approvalMode,
        TARGET_APPROVAL_MODE,
        'eDHR batch void policy switch must return target approval mode'
      )
    }

    const verified = await queryPolicy(page, 'eDHR batch void policy verify query')
    assert.ok(verified.policy, 'eDHR batch void policy must still be published after switch')
    assert.equal(
      verified.policy.approvalMode,
      TARGET_APPROVAL_MODE || finalPolicy.approvalMode,
      'verified eDHR batch void approval mode mismatch'
    )
    assert.deepEqual(pageErrors, [], 'eDHR batch void policy setup page errors must be empty')

    writeResult({
      status: 'PASS',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetApprovalMode: TARGET_APPROVAL_MODE || null,
      policyId: verified.policy.id,
      publishedPolicy: verified.policy,
      writeRequests,
      observedResponses,
      pageErrors
    })
    console.log(
      'GREEN: edhr-batch-void-policy-ui-setup -> PASS, policyId=' +
        verified.policy.id +
        ', approvalMode=' +
        verified.policy.approvalMode +
        ', artifact=' +
        RESULT_PATH
    )
  } catch (error) {
    writeResult({
      status: 'FAIL',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetApprovalMode: TARGET_APPROVAL_MODE || null,
      error: error.stack || error.message,
      writeRequests,
      observedResponses,
      pageErrors
    })
    console.error(error.stack || error.message)
    process.exitCode = 1
  } finally {
    await context.close()
    await browser.close()
  }
}

main()
