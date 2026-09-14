const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const artifactDir = path.join(
  workspaceRoot,
  'doc',
  'tasks',
  '20260813-scheduler-seven-issues-closure',
  'artifacts',
  'process-wip-historical-orphan',
  'real-ui-readonly'
)
const baseUrl = (
  process.env.MES_SCHEDULER_WIP_READONLY_BASE_URL || 'http://127.0.0.1:8081'
).replace(/\/+$/, '')
const loginConfig = {
  tenant: process.env.MES_SCHEDULER_WIP_READONLY_TENANT || '',
  username: process.env.MES_SCHEDULER_WIP_READONLY_USERNAME || '',
  password: process.env.MES_SCHEDULER_WIP_READONLY_PASSWORD || ''
}

const parseBody = (response) =>
  response.json().catch(async () => ({ raw: await response.text().catch(() => '') }))

const isVisible = async (locator) =>
  (await locator.count()) > 0 && locator.isVisible().catch(() => false)

async function loginWithPrefilledLocalForm(page) {
  await page.goto(
    `${baseUrl}/login?redirect=${encodeURIComponent('/index')}`,
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 }).catch(() => {})
  if (!(await isVisible(form)) && !page.url().includes('/login')) {
    return { tenant: 'current session', username: 'current user' }
  }
  assert.ok(await isVisible(form), `Login form is unavailable at ${page.url()}`)
  assert.equal(
    await page
      .locator(
        '.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible'
      )
      .count(),
    0,
    'Captcha is enabled; unattended read-only verification cannot continue.'
  )

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  const usernameInput = form
    .locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])')
    .first()
  const passwordInput = form.locator('input[placeholder="请输入密码"], input[type="password"]').first()
  if (loginConfig.tenant && (await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
    await page.keyboard.type(loginConfig.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: loginConfig.tenant })
      .first()
    if ((await tenantOption.count()) > 0) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
      await tenantInput.press('Tab')
    }
  }
  if (loginConfig.username) {
    await usernameInput.fill(loginConfig.username)
  }
  if (loginConfig.password) {
    await passwordInput.fill(loginConfig.password)
  }
  const tenant = (await tenantInput.count()) > 0 ? (await tenantInput.inputValue()).trim() : 'default'
  const username = (await usernameInput.inputValue()).trim()
  assert.ok(username, 'The local login form must provide a prefilled username.')
  assert.ok((await passwordInput.inputValue()).trim(), 'The local login form must provide a prefilled password.')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /登录/ }).click()
  const response = await loginResponsePromise
  const body = await parseBody(response)
  assert.equal(response.status(), 200, `Login HTTP ${response.status()}`)
  assert.equal(body.code, 0, `Login failed: ${body.msg || body.code}`)
  await page.waitForFunction(() => !window.location.pathname.includes('/login'), null, {
    timeout: 60000
  })
  return { tenant: tenant || 'default', username }
}

async function main() {
  fs.mkdirSync(artifactDir, { recursive: true })
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  const writes = []
  const pageErrors = []
  let captureWrites = false
  page.on('request', (request) => {
    if (
      captureWrites &&
      request.url().includes('/admin-api/') &&
      !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
    ) {
      writes.push({ method: request.method(), url: request.url() })
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    const identity = await loginWithPrefilledLocalForm(page)
    captureWrites = true
    await page.goto(`${baseUrl}/index?wipReadonlyLeave=${Date.now()}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })

    const wipResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-order/process-wip-statistics') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${baseUrl}/mes/pro/scheduler-workbench?wipReadonly=${Date.now()}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const response = await wipResponsePromise
    const body = await parseBody(response)
    assert.equal(response.status(), 200, `Process WIP HTTP ${response.status()}`)
    assert.equal(body.code, 0, `Process WIP failed: ${body.msg || body.code}`)
    assert.ok(Array.isArray(body.data), 'Process WIP data must be an array.')

    if (body.data.length > 0) {
      const table = page.locator('.scheduler-workbench__process-wip-table').first()
      await table.waitFor({ state: 'visible', timeout: 60000 })
      await page.getByText('夜班', { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
    } else {
      await page.getByText('暂无工序在制订单', { exact: true }).waitFor({
        state: 'visible',
        timeout: 60000
      })
    }
    assert.equal(await page.getByText('系统异常', { exact: true }).count(), 0, 'The page still reports a system error.')
    assert.deepEqual(writes, [], `Read-only verification produced writes: ${JSON.stringify(writes)}`)
    assert.deepEqual(pageErrors, [], `Page errors: ${JSON.stringify(pageErrors)}`)

    await page.screenshot({
      path: path.join(artifactDir, 'process-wip-readable.png'),
      fullPage: true
    })
    fs.writeFileSync(
      path.join(artifactDir, 'result.json'),
      JSON.stringify(
        {
          result: 'PASS',
          baseUrl,
          identity,
          httpStatus: response.status(),
          businessCode: body.code,
          rowCount: body.data.length,
          writes,
          pageErrors,
          screenshot: 'process-wip-readable.png'
        },
        null,
        2
      ),
      'utf8'
    )
    console.log(`PASS: process WIP historical-orphan read model, rows=${body.data.length}, writes=0`)
  } catch (error) {
    await page
      .screenshot({ path: path.join(artifactDir, 'failure.png'), fullPage: true })
      .catch(() => {})
    fs.writeFileSync(path.join(artifactDir, 'error.txt'), `${error.stack || error.message}\n`, 'utf8')
    console.error(error.stack || error.message)
    process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main()
