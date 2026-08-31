const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const taskDir = path.resolve(
  repoRoot,
  '..',
  'doc/tasks/20260831-approval-model-create-participant-config/e2e-artifacts'
)

function readEnvFile(relativePath) {
  const envPath = path.join(repoRoot, relativePath)
  if (!fs.existsSync(envPath)) return {}
  return Object.fromEntries(
    fs
      .readFileSync(envPath, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/))
      .filter(Boolean)
      .map((match) => [match[1], match[2].replace(/^['"]|['"]$/g, '')])
  )
}

const localEnv = readEnvFile('.env.local')
const baseEnv = readEnvFile('.env')

const config = {
  baseUrl: (process.env.BPM_MODEL_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant:
    process.env.BPM_MODEL_E2E_TENANT ||
    localEnv.VITE_APP_DEFAULT_LOGIN_TENANT ||
    baseEnv.VITE_APP_DEFAULT_LOGIN_TENANT,
  username:
    process.env.BPM_MODEL_E2E_USERNAME ||
    localEnv.VITE_APP_DEFAULT_LOGIN_USERNAME ||
    baseEnv.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password:
    process.env.BPM_MODEL_E2E_PASSWORD ||
    localEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD ||
    baseEnv.VITE_APP_DEFAULT_LOGIN_PASSWORD,
  targetPath: '/bpm/manager/model',
  manualName: '手工输入新审批流名字 20260831'
}

const screenshots = {
  createDialog: path.join(taskDir, 'bpm-model-create-participant-dialog.png'),
  loginFailed: path.join(taskDir, 'bpm-model-create-participant-login-failed.png')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page, context) {
  assert.ok(config.tenant, '本机默认登录租户缺失')
  assert.ok(config.username, '本机默认登录用户名缺失')
  assert.ok(config.password, '本机默认登录密码缺失')

  await context.clearCookies()
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise.catch(async (error) => {
    await page.screenshot({ path: screenshots.loginFailed, fullPage: true }).catch(() => null)
    throw error
  })
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(
    loginPayload && (loginPayload.code === 0 || loginPayload.code === 200),
    `登录失败：${loginPayload?.msg || loginPayload?.message || '未知错误'}`
  )
  await page.waitForResponse(
    (response) => response.url().includes('/system/auth/get-permission-info'),
    { timeout: 60000 }
  ).catch(() => null)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function main() {
  fs.mkdirSync(taskDir, { recursive: true })

  const browser = await chromium.launch({ headless: process.env.BPM_MODEL_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1366, height: 768 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const consoleErrors = []
  const writeRequests = []

  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('request', (request) => {
    const method = request.method()
    const url = request.url()
    if (['POST', 'PUT', 'DELETE'].includes(method) && url.includes('/admin-api/bpm/model')) {
      writeRequests.push({ method, url: url.replace(/\?.*$/, '') })
    }
  })

  try {
    await login(page, context)
    await page.goto(`${config.baseUrl}${config.targetPath}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.getByRole('heading', { name: '流程模型' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.unified-list-template[data-table-key="bpm.model.main"]').waitFor({
      state: 'visible',
      timeout: 60000
    })
    await settle(page)

    await page.getByRole('button', { name: '新建模型' }).click()
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新建审批模型' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })

    const nameItem = dialog.locator('.el-form-item').filter({ hasText: '流程名字' }).first()
    await nameItem.waitFor({ state: 'visible', timeout: 30000 })
    const nameInput = nameItem.locator('input').first()
    await nameInput.fill(config.manualName)

    const result = await dialog.evaluate((element) => {
      const text = element.textContent || ''
      return {
        titleVisible: text.includes('新建审批模型'),
        hasNameLabel: text.includes('流程名字'),
        hasReviewerLabel: text.includes('审核人'),
        hasApproverLabel: text.includes('批准人'),
        hasAddReviewer: text.includes('添加审核对象'),
        hasAddApprover: text.includes('添加批准对象'),
        hasOrRelation: text.includes('或关系'),
        hasAndRelation: text.includes('和关系'),
        hasOptionalApproverEmptyText: text.includes('未配置批准人')
      }
    })
    result.enteredName = await nameInput.inputValue()
    result.writeRequests = writeRequests
    result.pageErrors = pageErrors
    result.consoleErrors = consoleErrors
    result.identity = { tenant: config.tenant, username: config.username }
    result.targetPath = config.targetPath

    assert.equal(result.titleVisible, true, `新建弹窗标题未显示：${JSON.stringify(result)}`)
    assert.equal(result.hasNameLabel, true, `流程名字字段未显示：${JSON.stringify(result)}`)
    assert.equal(result.enteredName, config.manualName, `流程名字不能手工输入：${JSON.stringify(result)}`)
    assert.equal(result.hasReviewerLabel, true, `审核人配置未显示：${JSON.stringify(result)}`)
    assert.equal(result.hasApproverLabel, true, `批准人配置未显示：${JSON.stringify(result)}`)
    assert.equal(result.hasAddReviewer, true, `添加审核对象未显示：${JSON.stringify(result)}`)
    assert.equal(result.hasAddApprover, true, `添加批准对象未显示：${JSON.stringify(result)}`)
    assert.equal(result.hasOrRelation, true, `或关系未显示：${JSON.stringify(result)}`)
    assert.equal(result.hasAndRelation, true, `和关系未显示：${JSON.stringify(result)}`)
    assert.equal(result.hasOptionalApproverEmptyText, true, `批准人可选空态未显示：${JSON.stringify(result)}`)
    assert.equal(result.writeRequests.length, 0, `只读验证不允许发出模型写请求：${JSON.stringify(result)}`)
    assert.deepEqual(pageErrors, [], `页面错误：${pageErrors.join(' || ')}`)

    await page.screenshot({ path: screenshots.createDialog, fullPage: true })
    fs.writeFileSync(
      path.join(taskDir, 'bpm-model-create-participant-dialog-result.json'),
      `${JSON.stringify(result, null, 2)}\n`,
      'utf8'
    )
    process.stdout.write(`PASS: bpm model create participant config real readonly\n${JSON.stringify(result, null, 2)}\n`)
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
