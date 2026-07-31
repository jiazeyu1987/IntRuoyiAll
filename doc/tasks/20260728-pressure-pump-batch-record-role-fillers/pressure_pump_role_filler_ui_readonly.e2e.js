const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..', '..')
const frontendRoot = path.join(root, 'IntRuoyiFronted')
const { chromium } = require(path.join(frontendRoot, 'node_modules', 'playwright'))

const envPath = path.join(frontendRoot, '.env')
const baseUrl = 'http://127.0.0.1:8081'
const backendUrl = 'http://127.0.0.1:48081'
const tenant = '芋道源码'
const tenantId = 1
const username = 'admin'
const targetReport = {
  reportId: '1d05410f1d3140c5b8aa6786887ae69c',
  reportName: '粗洗工序生产记录',
  productName: '球囊扩张压力泵',
  roleName: '粗洗工序填写者角色'
}
const artifactPath = path.join(__dirname, 'pressure-pump-role-filler-ui-e2e.json')

function readEnv(file) {
  const result = {}
  const content = fs.readFileSync(file, 'utf8')
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#') || !trimmed.includes('=')) continue
    const [key, ...rest] = trimmed.split('=')
    result[key.trim()] = rest.join('=').trim().replace(/^['"]|['"]$/g, '')
  }
  return result
}

function redact(value, password) {
  if (typeof value !== 'string') return value
  return password ? value.split(password).join('<redacted>') : value
}

async function assertRuntimeReady() {
  const health = await fetch(`${backendUrl}/actuator/health`)
  assert.equal(health.status, 200, `backend health HTTP must be 200, got ${health.status}`)
  const healthBody = await health.json()
  assert.equal(healthBody.status, 'UP', `backend health must be UP, got ${JSON.stringify(healthBody)}`)
  const frontend = await fetch(`${baseUrl}/`)
  assert.ok(frontend.status >= 200 && frontend.status < 500, `frontend must respond, got ${frontend.status}`)
}

async function login(page, password) {
  const targetPath = `/mes/pro/batch-record-form-list?reportId=${encodeURIComponent(targetReport.reportId)}`
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 90000 })
  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    if ((await tenantOption.count()) > 0) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await loginForm.locator('input[placeholder="请输入租户名称"]').first().fill(tenant)
  }
  await loginForm.locator('input[placeholder="请输入用户名"]').first().fill(username)
  await loginForm.locator('input[placeholder="请输入密码"]').first().fill(password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await loginForm.locator('.el-button--primary').first().click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(Number(loginBody.code)), `login failed: ${redact(JSON.stringify(loginBody), password)}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function readBusinessBody(response, label) {
  const body = await response.json()
  assert.ok([0, 200].includes(Number(body.code)), `${label} failed: ${body.msg || body.code}`)
  return body.data
}

async function run() {
  const env = readEnv(envPath)
  const password = env.VITE_APP_DEFAULT_LOGIN_PASSWORD
  assert.equal(env.VITE_APP_DEFAULT_LOGIN_TENANT, tenant, 'default tenant must be 芋道源码')
  assert.equal(env.VITE_APP_DEFAULT_LOGIN_USERNAME, username, 'default username must be admin')
  assert.ok(password, 'default admin password is required')

  const evidence = {
    status: 'RUNNING',
    baseUrl,
    backendUrl,
    tenant,
    tenantId,
    username,
    targetReport,
    browserDiagnostics: {
      console: [],
      pageErrors: []
    }
  }
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (!['error', 'warning'].includes(message.type())) return
    evidence.browserDiagnostics.console.push({
      type: message.type(),
      text: redact(message.text(), password).slice(0, 1000)
    })
  })
  page.on('pageerror', (error) => {
    evidence.browserDiagnostics.pageErrors.push(redact(error.message, password).slice(0, 1000))
  })

  try {
    await assertRuntimeReady()
    await login(page, password)
    const pageResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/batch-record-report/page') &&
        response.url().includes(`reportId=${encodeURIComponent(targetReport.reportId)}`),
      { timeout: 90000 }
    )
    await page.goto(`${baseUrl}/mes/pro/batch-record-form-list?reportId=${encodeURIComponent(targetReport.reportId)}`, {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    })
    const pageData = await readBusinessBody(await pageResponsePromise, 'batch record report page')
    const rows = Array.isArray(pageData?.list) ? pageData.list : []
    assert.equal(rows.length, 1, `target report page must return exactly one row, got ${rows.length}`)
    assert.equal(rows[0].reportId, targetReport.reportId)
    assert.equal(rows[0].reportName, targetReport.reportName)
    await page.getByText('批记录表单', { exact: false }).first().waitFor({ state: 'visible', timeout: 90000 })
    const targetRow = page
      .locator('.el-table__body-wrapper:visible tbody tr')
      .filter({ hasText: targetReport.reportName })
      .filter({ hasText: targetReport.productName })
      .filter({ hasText: targetReport.roleName })
      .first()
    await targetRow.waitFor({ state: 'visible', timeout: 90000 })
    const permissionResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/edhr-process-form-permission-rule/get-by-report') &&
        response.url().includes(`batchRecordReportId=${encodeURIComponent(targetReport.reportId)}`),
      { timeout: 90000 }
    )
    await targetRow.getByRole('button').filter({ hasText: targetReport.roleName }).first().click()
    const permissionData = await readBusinessBody(await permissionResponsePromise, 'get-by-report')
    assert.equal(permissionData.fillRuleStatus, 'CONFIGURED')
    assert.equal(permissionData.fillRule?.candidateSourceType, 'ROLE')
    assert.deepEqual(permissionData.fillRule?.candidateSourceNames, [targetReport.roleName])
    assert.equal(permissionData.fillRule?.candidateUsers?.length, 3)
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '批记录表单填写人设置' }).last()
    await dialog.waitFor({ state: 'visible', timeout: 90000 })
    await dialog.getByText(targetReport.roleName, { exact: false }).first().waitFor({ state: 'visible', timeout: 90000 })
    evidence.status = 'PASS'
    evidence.pageRowVisible = true
    evidence.dialogRoleVisible = true
    evidence.permissionRule = {
      fillRuleStatus: permissionData.fillRuleStatus,
      candidateSourceType: permissionData.fillRule?.candidateSourceType,
      candidateSourceNames: permissionData.fillRule?.candidateSourceNames,
      candidateUserCount: permissionData.fillRule?.candidateUsers?.length
    }
    fs.writeFileSync(artifactPath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.log(`UI VERIFY PASS: ${targetReport.reportName} -> ${targetReport.roleName}, artifact=${artifactPath}`)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = {
      message: redact(error.message, password),
      stack: redact(error.stack, password)
    }
    fs.writeFileSync(artifactPath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.error(JSON.stringify(evidence, null, 2))
    process.exitCode = 1
  } finally {
    await context.close()
    await browser.close()
  }
}

run()
