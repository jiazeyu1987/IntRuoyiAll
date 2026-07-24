const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_PROCESS_EVIDENCE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.EDHR_PROCESS_EVIDENCE_E2E_TENANT || '测试租户',
  username: process.env.EDHR_PROCESS_EVIDENCE_E2E_USERNAME || 'aoteman',
  password: process.env.EDHR_PROCESS_EVIDENCE_E2E_PASSWORD || '111111',
  batchExecutionId: process.env.EDHR_PROCESS_EVIDENCE_E2E_BATCH_ID || '900000000463',
  headed: process.env.EDHR_PROCESS_EVIDENCE_E2E_HEADED === '1'
}

const repoRoot = path.resolve(__dirname, '../../..')
const outputDir = path.join(repoRoot, 'output/playwright')
const systemChrome = 'C:/Program Files/Google/Chrome/Application/chrome.exe'

const evidenceLabels = [
  '工作任务',
  '执行追踪',
  '签名记录',
  '审批记录',
  '字段审计',
  '操作审计',
  '变更记录',
  '统一变更',
  '主数据追溯',
  '历史同工序',
  '独立表单',
  '记录本引用'
]

const routes = [
  {
    name: 'detail',
    path: `/mes/pro/feedback/edhr-batch-execution/detail?id=${config.batchExecutionId}`,
    screenshot: 'edhr-process-evidence-fusion-detail.png'
  },
  {
    name: 'review',
    path: `/mes/pro/feedback/edhr-batch-execution/review?id=${config.batchExecutionId}`,
    screenshot: 'edhr-process-evidence-fusion-review.png'
  }
]

function assertPrerequisites() {
  assert.equal(config.baseUrl, 'http://127.0.0.1:8081', 'E2E must use the local frontend entry.')
  assert.equal(config.tenant, '测试租户', 'E2E must use 测试租户 for write-safe local verification.')
  assert.equal(config.username, 'aoteman', 'E2E must use the test tenant account aoteman.')
  assert.ok(config.password, 'EDHR_PROCESS_EVIDENCE_E2E_PASSWORD is required.')
  assert.ok(fs.existsSync(systemChrome), `System Chrome is required: ${systemChrome}`)
  fs.mkdirSync(outputDir, { recursive: true })
}

async function settle(page, timeout = 30000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
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

async function collectLoginDiagnostics(page) {
  const body = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
  const buttons = await page.evaluate(() =>
    Array.from(document.querySelectorAll('button'))
      .filter((button) => button.offsetParent)
      .map((button) => ({
        text: (button.textContent || '').replace(/\s+/g, ' ').trim(),
        disabled: button.disabled
      }))
  ).catch(() => [])
  return {
    url: page.url(),
    body: body.slice(0, 1200),
    buttons
  }
}

async function login(page, targetPath) {
  const loginUrl = `${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`
  await page.goto(loginUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  }).catch(() => null)
  await page.goto(loginUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)

  if (page.url().includes('/login')) {
    const loginForm = page.locator('form.login-form:visible, .login-form:visible').first()
    await loginForm.waitFor({ state: 'visible', timeout: 60000 })
    if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
      throw new Error('Captcha is enabled; unattended real E2E cannot continue.')
    }

    const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
    if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
      await tenantInput.fill(config.tenant)
      await tenantInput.press('Enter')
    } else {
      await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
    }

    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"], input[type="password"]'), config.password, 'password')

    let loginResponse
    try {
      ;[loginResponse] = await Promise.all([
        page.waitForResponse(
          (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
          { timeout: 60000 }
        ),
        loginForm.locator('.el-button--primary').first().click()
      ])
    } catch (error) {
      const diagnostics = await collectLoginDiagnostics(page)
      throw new Error(`login response not observed: ${error.message}; diagnostics=${JSON.stringify(diagnostics)}`)
    }
    const loginPayload = await loginResponse.json().catch(() => null)
    assert.ok(
      loginResponse.ok() && loginPayload && [0, 200].includes(loginPayload.code),
      `login failed: ${JSON.stringify(loginPayload)}`
    )
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  }
}

async function verifyRoute(page, route) {
  await page.goto(`${config.baseUrl}${route.path}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page, 60000)

  await page.getByText('工序证据链').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('工序上下文下集中查看建议合并与部分合并的证据').first().waitFor({ state: 'visible', timeout: 60000 })

  for (const label of evidenceLabels) {
    await page.getByRole('button', { name: new RegExp(label) }).first().waitFor({ state: 'visible', timeout: 60000 })
  }

  const evidenceButtons = await page.locator('.edhr-batch-detail__process-evidence-item').count()
  assert.ok(evidenceButtons >= evidenceLabels.length, `${route.name} must show all process evidence buttons.`)

  const bodyText = await page.locator('body').innerText({ timeout: 10000 })
  assert.ok(!bodyText.includes('管理后台工作区'), `${route.name} must render the real eDHR page, not the empty admin shell.`)

  await page.screenshot({ path: path.join(outputDir, route.screenshot), fullPage: true })
}

async function main() {
  assertPrerequisites()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: systemChrome
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } })

  try {
    await login(page, routes[0].path)
    for (const route of routes) {
      await verifyRoute(page, route)
    }
    console.log(`PASS: eDHR process evidence fusion verified on ${routes.map((item) => item.name).join(', ')}.`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
