const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.PROFILE_NAS_SYNC_E2E_BASE_URL || 'http://127.0.0.1:8088',
  tenant: process.env.PROFILE_NAS_SYNC_E2E_TENANT || '测试租户',
  username: process.env.PROFILE_NAS_SYNC_E2E_USERNAME || 'aoteman',
  password: process.env.PROFILE_NAS_SYNC_E2E_PASSWORD || '111111',
  nasDirectory:
    process.env.PROFILE_NAS_SYNC_E2E_NAS_DIR ||
    `profile-nas-table-sync-e2e-${new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)}`
}

const resultDir = path.resolve(__dirname, '../../test-results/profile-nas-table-auto-sync-real')
const resultPath = path.join(resultDir, 'result.json')

function assertSuccessPayload(payload, label) {
  assert.ok(payload && [0, 200].includes(payload.code), `${label} failed: ${JSON.stringify(payload)}`)
  return payload.data
}

function findChromeExecutable() {
  const candidates = [
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH,
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
  ].filter(Boolean)
  return candidates.find((candidate) => fs.existsSync(candidate))
}

async function login(page, targetPath) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', targetPath)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 15000 })
    await tenantOption.click()
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginPayload = await (await loginResponsePromise).json()
  assertSuccessPayload(loginPayload, `login(${config.username})`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function clickTab(page, name) {
  const tab = page.getByRole('tab', { name: new RegExp(`^${name}$`) }).first()
  await tab.waitFor({ state: 'visible', timeout: 60000 })
  await tab.click()
}

async function fillFormItem(page, label, value) {
  const item = page.locator('.profile-nas-table-sync .el-form-item', { hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  const input = item.locator('input:visible').first()
  await input.fill('')
  await input.fill(value)
  await page.keyboard.press('Enter').catch(() => undefined)
  await page.keyboard.press('Escape').catch(() => undefined)
}

async function ensureSwitch(page, label, expected) {
  const item = page.locator('.profile-nas-table-sync .el-form-item', { hasText: label }).first()
  const switchControl = item.locator('.el-switch:visible').first()
  const switchInput = item.locator('input[role="switch"]').first()
  await switchControl.waitFor({ state: 'visible', timeout: 30000 })
  await switchInput.waitFor({ state: 'attached', timeout: 30000 })
  for (let attempt = 0; attempt < 50; attempt += 1) {
    const className = (await switchControl.getAttribute('class')) || ''
    if (!className.includes('is-disabled')) break
    await page.waitForTimeout(100)
  }
  assert.ok(!((await switchControl.getAttribute('class')) || '').includes('is-disabled'), `${label} switch enabled`)
  const checked = await switchInput.getAttribute('aria-checked')
  if ((checked === 'true') !== expected) {
    await switchControl.locator('.el-switch__core').click()
    for (let attempt = 0; attempt < 30; attempt += 1) {
      if ((await switchInput.getAttribute('aria-checked')) === String(expected)) return
      await page.waitForTimeout(100)
    }
    assert.equal(await switchInput.getAttribute('aria-checked'), String(expected), `${label} switch state`)
  }
}

async function waitForApi(page, urlPart, method, action) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes(urlPart) && response.request().method() === method,
    { timeout: 90000 }
  )
  await action()
  const response = await responsePromise
  let payload = null
  try {
    payload = await response.json()
  } catch (error) {
    payload = { parseError: String(error) }
  }
  return { httpStatus: response.status(), ok: response.ok(), payload }
}

async function visibleMessage(page, pattern) {
  const message = page.locator('.el-message:visible').filter({ hasText: pattern }).first()
  await message.waitFor({ state: 'visible', timeout: 30000 })
  return (await message.textContent()) || ''
}

async function main() {
  assert.equal(config.tenant, '测试租户', 'NAS sync write E2E must use 测试租户')
  assert.equal(config.username, 'aoteman', 'NAS sync write E2E must use aoteman')
  fs.mkdirSync(resultDir, { recursive: true })

  const browser = await chromium.launch({
    headless: true,
    executablePath: findChromeExecutable(),
    args: ['--disable-dev-shm-usage']
  })

  const evidence = {
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    targetApis: [],
    pageErrors: [],
    consoleErrors: [],
    targetNetworkAborts: [],
    targetNetworkFailures: []
  }

  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
    page.setDefaultTimeout(60000)

    page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') evidence.consoleErrors.push(message.text())
    })
    page.on('requestfailed', (request) => {
      if (request.url().includes('/erp/nas-table-sync/')) {
        const failure = {
          url: request.url(),
          method: request.method(),
          failure: request.failure()?.errorText
        }
        if (failure.method === 'GET' && failure.failure === 'net::ERR_ABORTED') {
          evidence.targetNetworkAborts.push(failure)
        } else {
          evidence.targetNetworkFailures.push(failure)
        }
      }
    })

    await login(page, '/user/profile')
    await page.goto(new URL('/user/profile', config.baseUrl).toString(), { waitUntil: 'domcontentloaded' })
    await clickTab(page, '配置')
    await clickTab(page, 'NAS表格自动同步')

    const panel = page.locator('.profile-nas-table-sync')
    await panel.waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(await panel.getByText('NAS表格自动同步').first().isVisible(), true, 'NAS tab title must be visible')

    const productCheckbox = panel.locator('.el-checkbox', { hasText: '产品' }).first()
    await productCheckbox.waitFor({ state: 'visible', timeout: 30000 })
    const productChecked = await productCheckbox.locator('input[type="checkbox"]').isChecked().catch(() => false)
    if (!productChecked) {
      await productCheckbox.click()
    }

    await ensureSwitch(page, '启用自动同步', true)
    await fillFormItem(page, '每日开始时间', '23:59:00')
    await fillFormItem(page, 'NAS 目录', config.nasDirectory)
    await fillFormItem(page, '文件名规则', 'ERP_NAS_E2E_{yyyyMMdd_HHmmss}.xlsx')

    const saveResult = await waitForApi(page, '/erp/nas-table-sync/plan/save', 'PUT', async () => {
      await page.getByRole('button', { name: '保存配置' }).click()
    })
    evidence.targetApis.push({ name: 'savePlan', ...saveResult })
    const savedPlan = assertSuccessPayload(saveResult.payload, 'savePlan')
    assert.equal(savedPlan.enabled, true, 'saved plan must be enabled')
    assert.equal(savedPlan.dailyStartTime, '23:59:00', 'saved start time must round-trip')
    assert.ok(savedPlan.items.some((item) => item.syncType === 'PRODUCT' && item.enabled), 'PRODUCT sync must be selected')
    await visibleMessage(page, /配置已保存/)

    const testWriteResult = await waitForApi(page, '/erp/nas-table-sync/plan/test-nas-write', 'POST', async () => {
      await page.getByRole('button', { name: '测试NAS写入' }).click()
    })
    evidence.targetApis.push({ name: 'testNasWrite', ...testWriteResult })
    const testWriteCode = testWriteResult.payload?.code
    if ([0, 200].includes(testWriteCode)) {
      await visibleMessage(page, /测试NAS写入成功/)
    } else {
      await visibleMessage(page, /测试NAS写入失败|NAS|失败/)
    }

    const runOnceResult = await waitForApi(page, '/erp/nas-table-sync/plan/run-once', 'POST', async () => {
      await page.getByRole('button', { name: '立即执行一次' }).click()
    })
    evidence.targetApis.push({ name: 'runOnce', ...runOnceResult })
    const runOnceData = assertSuccessPayload(runOnceResult.payload, 'runOnce')
    assert.ok(['SUCCESS', 'FAILED'].includes(runOnceData.status), `runOnce status must be explicit: ${runOnceData.status}`)
    if (runOnceData.status === 'FAILED') {
      await visibleMessage(page, /失败|NAS/)
    } else {
      await visibleMessage(page, /已完成/)
    }
    const runPageResponse = await page.waitForResponse(
      (response) =>
        response.url().includes('/erp/nas-table-sync/run/page') && response.request().method() === 'GET',
      { timeout: 90000 }
    )
    evidence.targetApis.push({
      name: 'runPageAfterRun',
      httpStatus: runPageResponse.status(),
      ok: runPageResponse.ok(),
      payload: await runPageResponse.json().catch((error) => ({ parseError: String(error) }))
    })

    await page.locator('.profile-nas-table-sync__table').waitFor({ state: 'visible', timeout: 30000 })
    await page.waitForFunction(
      ({ runId, status }) => {
        const text = document.querySelector('.profile-nas-table-sync__table')?.textContent || ''
        return text.includes(String(runId)) || text.includes(status)
      },
      { runId: runOnceData.runId, status: runOnceData.status },
      { timeout: 30000 }
    )

    await ensureSwitch(page, '启用自动同步', false)
    const disableResult = await waitForApi(page, '/erp/nas-table-sync/plan/save', 'PUT', async () => {
      await page.getByRole('button', { name: '保存配置' }).click()
    })
    evidence.targetApis.push({ name: 'disablePlan', ...disableResult })
    const disabledPlan = assertSuccessPayload(disableResult.payload, 'disablePlan')
    assert.equal(disabledPlan.enabled, false, 'cleanup save must disable auto sync plan')

    assert.deepEqual(evidence.pageErrors, [], 'pageerror must be empty')
    assert.deepEqual(evidence.targetNetworkFailures, [], 'target NAS sync requests must not fail at network layer')

    evidence.status = 'PASS'
    evidence.runStatus = runOnceData.status
    fs.writeFileSync(resultPath, JSON.stringify(evidence, null, 2), 'utf8')
    console.log(`PASS: profile NAS table auto sync real E2E (${runOnceData.status})`)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    fs.writeFileSync(resultPath, JSON.stringify(evidence, null, 2), 'utf8')
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
