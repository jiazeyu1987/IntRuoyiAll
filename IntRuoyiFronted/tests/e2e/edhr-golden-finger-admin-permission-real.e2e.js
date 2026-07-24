const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '..', '..')
const defaultProjectRoot = path.resolve(frontendRoot, '..', '..', '..', 'IntRuoyi')
const projectRoot = path.resolve(process.env.EDHR_GOLDEN_FINGER_PROJECT_ROOT || defaultProjectRoot)
const taskDir = path.join(projectRoot, 'doc', 'tasks', '20260721-edhr-golden-finger-admin')
const artifactDir = path.join(taskDir, 'e2e-artifacts')
const resultFile = path.join(artifactDir, 'golden-finger-admin-permission-real-e2e-result.json')
const screenshotFile = path.join(artifactDir, 'golden-finger-admin-permission-real-e2e.png')

const config = {
  baseUrl: process.env.EDHR_GOLDEN_FINGER_BASE_URL || 'http://127.0.0.1:8093',
  tenant: process.env.EDHR_GOLDEN_FINGER_TENANT || '芋道源码',
  username: process.env.EDHR_GOLDEN_FINGER_USERNAME || 'admin',
  password: process.env.EDHR_GOLDEN_FINGER_PASSWORD || '',
  headed: process.env.EDHR_GOLDEN_FINGER_HEADED === '1',
  targetPath: '/mes/pro/feedback/edhr-batch-execution'
}

const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'
const MES_WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

function failFast(message, details = {}) {
  const error = new Error(message)
  error.details = details
  return error
}

function ensureConfig() {
  const blockers = []
  if (!/^http:\/\/(127\.0\.0\.1|localhost):\d+$/.test(config.baseUrl)) {
    blockers.push(`baseUrl must be a local browser URL, got ${config.baseUrl}`)
  }
  if (config.tenant !== '芋道源码' || config.username !== 'admin') {
    blockers.push(`admin permission verification must use 芋道源码/admin, got ${config.tenant}/${config.username}`)
  }
  if (!config.password) {
    blockers.push('EDHR_GOLDEN_FINGER_PASSWORD is required')
  }
  if (!fs.existsSync(path.join(projectRoot, 'scripts', 'preflight', 'login-preflight.mjs'))) {
    blockers.push(`projectRoot must contain scripts/preflight/login-preflight.mjs, got ${projectRoot}`)
  }
  if (blockers.length) {
    throw failFast('edhr_golden_finger_admin_e2e_precondition_failed', { blockers })
  }
}

function runOfficialLoginPreflight() {
  const scriptPath = path.join(projectRoot, 'scripts', 'preflight', 'login-preflight.mjs')
  const result = spawnSync(
    process.execPath,
    [
      scriptPath,
      '--base-url',
      config.baseUrl,
      '--tenant',
      config.tenant,
      '--username',
      config.username,
      '--password',
      config.password,
      '--target-path',
      config.targetPath,
      '--target-text',
      '批次'
    ],
    {
      cwd: projectRoot,
      env: process.env,
      encoding: 'utf8',
      timeout: 180000
    }
  )
  if (result.status !== 0) {
    throw failFast('official_login_preflight_failed', {
      status: result.status,
      stdout: result.stdout,
      stderr: result.stderr
    })
  }
  return result.stdout.trim()
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 60000 })
    await option.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/get-permission-info') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_payload_failed:${JSON.stringify(loginPayload)}`)
  const permissionResponse = await permissionResponsePromise
  const permissionPayload = await permissionResponse.json()
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
  return {
    login: { httpStatus: loginResponse.status(), code: loginPayload.code },
    permission: {
      httpStatus: permissionResponse.status(),
      code: permissionPayload.code,
      permissions: permissionPayload?.data?.permissions || []
    }
  }
}

async function main() {
  ensureConfig()
  fs.mkdirSync(artifactDir, { recursive: true })
  const officialPreflight = runOfficialLoginPreflight()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const evidence = {
    status: 'RUNNING',
    baseUrl: config.baseUrl,
    tenant: config.tenant,
    username: config.username,
    targetPath: config.targetPath,
    officialPreflight,
    mesWriteRequests: []
  }

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('request', (request) => {
      const url = request.url()
      if (url.includes('/admin-api/mes/') && MES_WRITE_METHODS.has(request.method())) {
        evidence.mesWriteRequests.push({ method: request.method(), url })
      }
    })

    const loginEvidence = await login(page)
    evidence.login = loginEvidence.login
    evidence.permissionResponse = {
      httpStatus: loginEvidence.permission.httpStatus,
      code: loginEvidence.permission.code,
      includesGoldenFinger: loginEvidence.permission.permissions.includes(GOLDEN_FINGER_PERMISSION),
      permissionCount: loginEvidence.permission.permissions.length
    }
    assert.equal(
      evidence.permissionResponse.includesGoldenFinger,
      true,
      `admin permission response must include ${GOLDEN_FINGER_PERMISSION}`
    )

    await page.goto(new URL(config.targetPath, config.baseUrl).toString(), { waitUntil: 'commit' })
    await page.getByText('批次', { exact: false }).first().waitFor({ state: 'visible' })
    await page.screenshot({ path: screenshotFile, fullPage: true })
    evidence.pageUrl = page.url()
    evidence.screenshot = screenshotFile
    evidence.status = 'PASS'
    assert.deepEqual(
      evidence.mesWriteRequests,
      [],
      `admin permission visibility E2E must not send MES write requests: ${JSON.stringify(evidence.mesWriteRequests)}`
    )
    fs.writeFileSync(resultFile, JSON.stringify(evidence, null, 2), 'utf8')
    console.log(`PASS: golden finger admin permission real E2E -> ${resultFile}`)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = { message: error.message, details: error.details || null, stack: error.stack }
    fs.writeFileSync(resultFile, JSON.stringify(evidence, null, 2), 'utf8')
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
