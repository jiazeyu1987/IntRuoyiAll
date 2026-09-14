const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.QA_REGULATION_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, '')
const TARGET_PATH = '/mes/pro/process-pool/qa-regulation'
const RESULT_DIR = path.resolve(WORKSPACE_ROOT, 'output', 'playwright', '20260804-qa-regulation-tab')
const RESULT_PATH = path.join(RESULT_DIR, 'qa-regulation-original-excerpt-real-e2e.json')
const SCREENSHOT_PATH = path.join(RESULT_DIR, 'qa-regulation-original-excerpt-real-e2e.png')

function parseEnvValue(value) {
  const trimmed = (value || '').trim()
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
    const key = trimmed.slice(0, equalsIndex).trim()
    env[key] = parseEnvValue(trimmed.slice(equalsIndex + 1))
  }
  return env
}

function collectLoginConfig() {
  const env = {
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env')),
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env.local')),
    ...process.env
  }
  return {
    tenant: env.QA_REGULATION_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.QA_REGULATION_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.QA_REGULATION_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
}

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

async function login(page, config) {
  assert.ok(config.tenant, 'local default tenant is required')
  assert.ok(config.username, 'local default username is required')
  assert.ok(config.password, 'local default password is required')

  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form
    .locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])')
    .first()
    .fill(config.username)
  await form.locator('input[type="password"], input[placeholder="请输入密码"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

async function assertSourceExcerptVisible(qaTab, text) {
  const sourceCard = qaTab.locator('[data-qa-regulation-original-excerpt]').filter({ hasText: text }).first()
  await sourceCard.waitFor({ state: 'attached' })
  await sourceCard.scrollIntoViewIfNeeded()
  await sourceCard.waitFor({ state: 'visible' })
  const sourceText = await sourceCard.textContent()
  assert.ok(sourceText.includes(text), `QA source excerpt must include: ${text}`)
}

async function selectIdiProject(page, qaTab) {
  const dccCard = qaTab.locator('[data-qa-regulation-dcc-project]').first()
  const select = dccCard.locator('.el-select').first()
  await select.waitFor({ state: 'visible' })
  await select.click()
  const input = select.locator('input[role="combobox"], input.el-select__input').first()
  await input.fill('IDI')
  const idiOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: /IDI/ }).first()
  await idiOption.waitFor({ state: 'visible' })
  await idiOption.click()
  await dccCard.getByText('IDI', { exact: false }).first().waitFor({ state: 'visible' })
}

async function main() {
  const config = collectLoginConfig()
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const consoleErrors = []
  const pageErrors = []
  const writeRequests = []
  let captureWrites = false

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('request', (request) => {
      if (!captureWrites) {
        return
      }
      const method = request.method()
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && request.url().includes('/admin-api/')) {
        writeRequests.push({ method, url: request.url() })
      }
    })

    await login(page, config)

    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    await page.getByText('QA 规程配置', { exact: false }).first().waitFor({ state: 'visible' })

    captureWrites = true
    const qaTab = page.locator('[data-qa-regulation-page]').first()
    await qaTab.waitFor({ state: 'visible' })
    await selectIdiProject(page, qaTab)

    for (const requiredText of [
      '原文依据',
      'PDF 第 6 页',
      'PDF 第 7 页',
      'PDF 第 3 页',
      'PDF 第 8 页',
      '原文依据摘录'
    ]) {
      await qaTab.getByText(requiredText, { exact: false }).first().waitFor({ state: 'visible' })
    }
    const scopeInputValues = await qaTab
      .locator('[data-qa-regulation-scope] input')
      .evaluateAll((inputs) => inputs.map((input) => input.value))
    assert.ok(scopeInputValues.includes('PQC-IDI-001'), 'QA pressure-pump regulation code must remain PQC-IDI-001')
    assert.ok(
      scopeInputValues.includes('按压式球囊扩充压力泵组装过程检验规程'),
      'QA pressure-pump regulation name must remain visible in the scope form'
    )
    assert.ok(scopeInputValues.includes('B/0'), 'QA pressure-pump version must remain B/0')
    assert.ok(scopeInputValues.includes('2026-01-04'), 'QA pressure-pump effective date must remain 2026-01-04')

    for (const sourceText of [
      '压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷',
      '正常或矫正视力，在 300~700lx 的照度下',
      '推杆组件推入外套，后盖与外套的卡槽扣到位',
      '负压检测：抽负压-80±5kpa，不应有泄漏',
      '20atm 压力打至 20atm 应无跳压现象',
      '每一个检验项目均应合格',
      'RE-PQC-IDI-001-01'
    ]) {
      await assertSourceExcerptVisible(qaTab, sourceText)
    }

    const qaText = await qaTab.innerText()
    assert.doesNotMatch(
      qaText,
      /文件分类|受控文件|controlled-file|fileTypeTaxonomy/i,
      'QA tab must not include document-control classification wording'
    )

    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })

    assert.deepEqual(writeRequests, [], 'QA source excerpt read-only E2E must not send backend write requests')
    assert.deepEqual(consoleErrors, [], 'QA source excerpt read-only E2E must not emit console errors')
    assert.deepEqual(pageErrors, [], 'QA source excerpt read-only E2E must not emit page errors')

    const result = {
      ok: true,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: `${config.tenant}/${config.username}`,
      sourceExcerptCount: await qaTab.locator('[data-qa-regulation-original-excerpt]').count(),
      writeRequests,
      consoleErrors,
      pageErrors,
      screenshotPath: SCREENSHOT_PATH
    }
    writeResult(result)
    console.log(`PASS QA regulation standalone original excerpt real E2E ${JSON.stringify(result)}`)
  } catch (error) {
    writeResult({
      ok: false,
      baseUrl: BASE_URL,
      targetPath: TARGET_PATH,
      actor: config.tenant && config.username ? `${config.tenant}/${config.username}` : 'missing-local-default-login',
      error: error.message,
      writeRequests,
      consoleErrors,
      pageErrors
    })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
