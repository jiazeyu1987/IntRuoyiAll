import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { chromium } from 'playwright'

const repoRoot = resolve(import.meta.dirname, '..')
const workspaceRoot = resolve(repoRoot, '..')
const taskId = '20260602-dcc-delete-stop-active-nas-collection'
const outputDir = resolve(workspaceRoot, 'output/playwright', taskId)

const config = {
  baseUrl: process.env.DCC_DIRECTORY_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.DCC_DIRECTORY_E2E_TENANT || '芋道源码',
  username: process.env.DCC_DIRECTORY_E2E_USERNAME || 'admin',
  password: process.env.DCC_DIRECTORY_E2E_PASSWORD || 'admin123',
  targetName: process.env.DCC_DIRECTORY_E2E_TARGET_NAME || '1. QMS documents',
  headless: process.env.DCC_DIRECTORY_E2E_HEADLESS !== 'false'
}

const evidence = {
  taskId,
  startedAt: new Date().toISOString(),
  config: {
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    targetName: config.targetName
  },
  steps: [],
  responses: [],
  consoleErrors: [],
  requestFailures: []
}

const record = (status, label, detail = {}) => {
  evidence.steps.push({
    status,
    label,
    detail,
    at: new Date().toISOString()
  })
}

const settle = async (page) => {
  await page.waitForLoadState('domcontentloaded', { timeout: 30000 }).catch(() => {})
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {})
  await page.waitForTimeout(500)
}

const screenshot = async (page, fileName) => {
  await page.screenshot({ path: resolve(outputDir, fileName), fullPage: true })
}

const login = async (page) => {
  await page.goto(`${config.baseUrl}/login?redirect=/dcc/controlled-file/directories`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    record('PASS', 'reuse-login-session', { url: page.url() })
    return
  }
  const form = page.locator('.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input[role="combobox"]').first()
  await tenantInput.click()
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await form.locator('input[placeholder="请输入用户名"]').fill(config.username)
  await form.locator('input[placeholder="请输入密码"]').fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
    { timeout: 60000 }
  )
  await form.locator('.el-button--primary').first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => ({}))
  evidence.responses.push({
    label: 'login',
    status: loginResponse.status(),
    code: loginPayload.code,
    msg: loginPayload.msg
  })
  if (loginPayload.code !== 0) {
    throw new Error(`login failed: ${loginPayload.msg || 'unknown error'}`)
  }
  await page.waitForFunction(() => !location.pathname.includes('/login'), null, { timeout: 60000 })
  await settle(page)
  record('PASS', 'login', { url: page.url() })
}

const attachWatchers = (page) => {
  page.on('console', (message) => {
    if (message.type() === 'error') {
      evidence.consoleErrors.push(message.text())
    }
  })
  page.on('requestfailed', (request) => {
    const url = request.url()
    if (url.includes('/admin-api/')) {
      evidence.requestFailures.push({
        method: request.method(),
        url,
        failure: request.failure()?.errorText || ''
      })
    }
  })
}

const verifyDeletePrecheck = async (page) => {
  await page.goto(`${config.baseUrl}/dcc/controlled-file/directories`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const treeResponse = await page.waitForResponse(
    (response) => response.url().includes('/admin-api/dcc/directories/tree') && response.status() === 200,
    { timeout: 60000 }
  )
  const treePayload = await treeResponse.json()
  evidence.responses.push({
    label: 'directory-tree',
    status: treeResponse.status(),
    code: treePayload.code
  })
  await settle(page)
  await page.getByText(config.targetName, { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
  await screenshot(page, '01-directory-page.png')

  const row = page.locator('.el-table__body-wrapper tr').filter({ hasText: config.targetName }).first()
  const deleteButton = row.getByRole('button', { name: '删除父文件夹' }).first()
  await deleteButton.waitFor({ state: 'visible', timeout: 30000 })
  const activeTransferResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/directories/')
      && response.url().includes('/active-nas-transfer')
      && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await deleteButton.click()
  const activeTransferResponse = await activeTransferResponsePromise
  const activeTransferPayload = await activeTransferResponse.json()
  evidence.responses.push({
    label: 'active-nas-transfer',
    status: activeTransferResponse.status(),
    code: activeTransferPayload.code,
    data: activeTransferPayload.data
  })
  if (activeTransferPayload.code !== 0) {
    throw new Error(`active transfer precheck failed: ${activeTransferPayload.msg || 'unknown error'}`)
  }

  if (activeTransferPayload.data?.active) {
    const confirmDialog = page.locator('.el-message-box:visible').filter({ hasText: '确认停止后台收集' }).first()
    await confirmDialog.waitFor({ state: 'visible', timeout: 30000 })
    await screenshot(page, '02-stop-active-transfer-confirm.png')
    await confirmDialog.getByRole('button', { name: '取消' }).click()
    record('PASS', 'active-transfer-stop-confirm-shown', {
      taskId: activeTransferPayload.data.taskId,
      status: activeTransferPayload.data.status
    })
    return
  }

  const deleteDialog = page.locator('.el-dialog:visible').filter({ hasText: '删除父文件夹' }).first()
  await deleteDialog.waitFor({ state: 'visible', timeout: 30000 })
  await screenshot(page, '02-delete-confirm-dialog.png')
  await deleteDialog.getByRole('button', { name: '取消' }).click()
  record('PASS', 'inactive-transfer-delete-dialog-shown', {
    active: false,
    note: 'No PROD confirmation was submitted; tenant data was not changed.'
  })
}

if (!existsSync(outputDir)) {
  mkdirSync(outputDir, { recursive: true })
}

const browser = await chromium.launch({ headless: config.headless })
const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const page = await context.newPage()
attachWatchers(page)

try {
  await login(page)
  await verifyDeletePrecheck(page)
  evidence.finishedAt = new Date().toISOString()
  evidence.result = 'PASS'
} catch (error) {
  evidence.finishedAt = new Date().toISOString()
  evidence.result = 'FAIL'
  evidence.error = {
    message: error.message,
    stack: error.stack
  }
  await screenshot(page, 'error.png').catch(() => {})
  throw error
} finally {
  writeFileSync(resolve(outputDir, 'evidence.json'), `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
  await context.close().catch(() => {})
  await browser.close().catch(() => {})
}
