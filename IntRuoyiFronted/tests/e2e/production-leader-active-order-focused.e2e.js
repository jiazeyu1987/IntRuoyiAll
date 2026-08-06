const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (
  process.env.ACTIVE_ORDER_E2E_BASE_URL || 'http://127.0.0.1:8081'
).replace(/\/+$/, '')
const TARGET_PATH = '/mes/pro/process-pool/production-leader'
const DEFAULT_WORK_ORDER_CODE = '881MO093613'
const RESULT_DIR = path.resolve(
  FRONTEND_ROOT,
  'test-results',
  'production-leader-active-order-focused'
)
const RESULT_PATH = path.join(RESULT_DIR, 'result.json')
const SCREENSHOT_PATH = path.join(RESULT_DIR, 'active-order-focused.png')

function parseEnvValue(value) {
  const trimmed = String(value || '').trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  const env = {}
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) {
      continue
    }
    const equalsIndex = trimmed.indexOf('=')
    if (equalsIndex <= 0) {
      continue
    }
    env[trimmed.slice(0, equalsIndex).trim()] = parseEnvValue(trimmed.slice(equalsIndex + 1))
  }
  return env
}

function collectConfig() {
  const env = {
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env')),
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env.local')),
    ...process.env
  }
  return {
    tenant: env.ACTIVE_ORDER_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.ACTIVE_ORDER_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.ACTIVE_ORDER_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD,
    workOrderCode: env.ACTIVE_ORDER_E2E_WORK_ORDER_CODE || DEFAULT_WORK_ORDER_CODE,
    headed: env.ACTIVE_ORDER_E2E_HEADED === '1'
  }
}

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function resolveChromiumExecutable() {
  const explicit = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  if (explicit && fs.existsSync(explicit)) {
    return explicit
  }
  const localChrome = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  if (fs.existsSync(localChrome)) {
    return localChrome
  }
  const localEdge = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
  if (fs.existsSync(localEdge)) {
    return localEdge
  }
  return undefined
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible().catch(() => false)) {
      await candidate.fill(value)
      return
    }
  }
  throw new Error(`${label} input is not visible`)
}

async function login(page, config) {
  assert.ok(config.tenant, 'local default tenant is required')
  assert.ok(config.username, 'local default username is required')
  assert.ok(config.password, 'local default password is required')

  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({
      hasText: config.tenant
    }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }

  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])'),
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
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginBody.code), `login business code ${loginBody.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

function getVisibleTab(page, name) {
  return page.locator('.el-tabs__item:visible').filter({
    hasText: new RegExp(`^\\s*${name}\\s*$`)
  }).first()
}

async function searchRemoteOrderCandidate(page, comboInput, workOrderCode) {
  const [candidateResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/process-pool/team-leader/active-order/candidates') &&
        response.request().method() === 'GET',
      { timeout: 30000 }
    ),
    (async () => {
      await comboInput.click()
      await comboInput.press('Control+A')
      await comboInput.type(workOrderCode, { delay: 40 })
    })()
  ])
  return candidateResponse
}

async function run() {
  const config = collectConfig()
  const result = {
    status: 'RUNNING',
    baseUrl: BASE_URL,
    targetPath: TARGET_PATH,
    tenant: config.tenant,
    username: config.username,
    workOrderCode: config.workOrderCode,
    candidate: null,
    activeBefore: null,
    addPayload: null,
    addBusinessCode: null,
    addBusinessMessage: '',
    cleanup: 'NOT_NEEDED'
  }

  const executablePath = resolveChromiumExecutable()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath
  })
  const context = await browser.newContext()
  const page = await context.newPage()

  let latestActiveOrders = []
  page.on('response', async (response) => {
    if (!response.url().includes('/mes/pro/process-pool/team-leader/active-order/list')) {
      return
    }
    try {
      const body = await response.json()
      latestActiveOrders = Array.isArray(body.data) ? body.data : []
    } catch {
      latestActiveOrders = []
    }
  })

  try {
    await login(page, config)
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    await page.locator('[data-production-leader-workbench-page]').waitFor({
      state: 'visible',
      timeout: 60000
    })
    await getVisibleTab(page, '活跃订单池').click()
    const activeOrderSection = page.locator('[data-team-leader-active-order-config]').first()
    await activeOrderSection.waitFor({ state: 'visible', timeout: 30000 })
    await page.waitForTimeout(1000)

    await activeOrderSection.getByRole('button', { name: '新增活跃订单' }).click()
    const dialog = page.locator('[data-team-leader-active-order-add-dialog]').first()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    const comboInput = dialog
      .locator('[data-team-leader-active-order-work-order-code] input[role="combobox"]')
      .first()
    await comboInput.waitFor({ state: 'visible', timeout: 30000 })

    const candidateResponse = await searchRemoteOrderCandidate(page, comboInput, config.workOrderCode)
    const candidateBody = await candidateResponse.json()
    assert.equal(candidateResponse.ok(), true, `candidate HTTP ${candidateResponse.status()}`)
    assert.ok([0, 200].includes(candidateBody.code), `candidate business code ${candidateBody.code}`)
    const candidates = Array.isArray(candidateBody.data) ? candidateBody.data : []
    const candidate = candidates.find((item) => item.workOrderCode === config.workOrderCode)
    assert.ok(candidate, `候选接口未返回订单号 ${config.workOrderCode}`)
    result.candidate = candidate
    const option = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.workOrderCode })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()

    const activeBefore = latestActiveOrders.some(
      (row) => Number(row.workOrderId) === Number(candidate.workOrderId)
    )
    result.activeBefore = activeBefore

    const [addResponse] = await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/mes/pro/process-pool/team-leader/active-order/add') &&
          response.request().method() === 'POST',
        { timeout: 30000 }
      ),
      dialog.getByRole('button', { name: '加入活跃订单' }).click()
    ])
    const addRequestPayload = JSON.parse(addResponse.request().postData() || '{}')
    result.addPayload = addRequestPayload
    assert.deepEqual(
      Object.keys(addRequestPayload).sort(),
      ['workOrderId'],
      'add payload must only contain workOrderId'
    )
    assert.equal(
      Number(addRequestPayload.workOrderId),
      Number(candidate.workOrderId),
      'add payload workOrderId must be resolved from the selected order code'
    )
    const addBody = await addResponse.json()
    result.addBusinessCode = addBody.code
    result.addBusinessMessage = addBody.msg || addBody.message || ''
    assert.equal(addResponse.ok(), true, `add HTTP ${addResponse.status()}`)
    assert.ok(
      [0, 200].includes(addBody.code),
      `加入活跃订单业务失败：${result.addBusinessMessage || addBody.code}`
    )

    if (!activeBefore) {
      await page.locator('[data-team-leader-active-order-add-dialog]').waitFor({
        state: 'hidden',
        timeout: 30000
      })
      await page.waitForTimeout(1000)
      const row = page
        .locator('[data-team-leader-active-order-list] tbody tr:visible')
        .filter({ hasText: String(candidate.workOrderId) })
        .first()
      if (await row.isVisible().catch(() => false)) {
        const [removeResponse] = await Promise.all([
          page.waitForResponse(
            (response) =>
              response.url().includes('/mes/pro/process-pool/team-leader/active-order/remove') &&
              response.request().method() === 'PUT',
            { timeout: 30000 }
          ),
          row.getByRole('button', { name: '移出活跃订单' }).click()
        ])
        const removeBody = await removeResponse.json()
        assert.equal(removeResponse.ok(), true, `remove HTTP ${removeResponse.status()}`)
        assert.ok([0, 200].includes(removeBody.code), `remove business code ${removeBody.code}`)
        result.cleanup = 'REMOVED_BY_UI'
      } else {
        result.cleanup = 'SKIPPED_ROW_NOT_VISIBLE'
      }
    }

    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    result.status = 'PASS'
    result.screenshot = SCREENSHOT_PATH
    writeResult(result)
    await browser.close()
    return result
  } catch (error) {
    result.status = 'FAIL'
    result.error = error.message
    try {
      await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
      result.screenshot = SCREENSHOT_PATH
    } catch {
      // ignore screenshot failures after browser/page teardown
    }
    writeResult(result)
    await browser.close()
    throw error
  }
}

run()
  .then((result) => {
    console.log(
      `PASS: active order focused E2E order=${result.workOrderCode} workOrderId=${result.candidate.workOrderId} cleanup=${result.cleanup}`
    )
  })
  .catch((error) => {
    console.error(`FAIL: active order focused E2E ${error.message}`)
    process.exitCode = 1
  })
