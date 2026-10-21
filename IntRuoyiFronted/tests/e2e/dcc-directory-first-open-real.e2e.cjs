'use strict'

const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const workspace = path.resolve(__dirname, '../../..')
const taskDir = path.join(workspace, 'doc/tasks/20261016-dcc-e2e-12')
const phase = process.argv[2]
assert.ok(['red', 'green'].includes(phase), 'Specify red or green to retain separate evidence')
const dispatch = JSON.parse(fs.readFileSync(path.join(workspace, 'doc/tasks/20261016-dcc-24-e2e-coordination/dispatch.json'), 'utf8'))
assert.equal(dispatch.environment.profile, 'int_qms')
assert.equal(dispatch.environment.frontendUrl, 'http://127.0.0.1:8061')
assert.equal(dispatch.environment.backendPort, 48061)
assert.equal(dispatch.cases['E2E-12'].gate, 'execute')
const tenant = process.env.DCC_DIRECTORY_E2E_TENANT
const username = process.env.DCC_DIRECTORY_E2E_USERNAME
const password = process.env.DCC_DIRECTORY_E2E_PASSWORD
assert.ok(tenant && username && password, 'Authorized credentials must be provided at runtime')
const out = path.join(taskDir, 'artifacts', `i04-${phase}`)
fs.mkdirSync(out, { recursive: true })
const evidence = {
  issue: 'E2E-I04', phase, status: 'RUNNING', baseUrl: dispatch.environment.frontendUrl,
  tenant, username, freshContext: true, firstOpenClicks: 0, steps: [], pageErrors: [],
  network: [], directoryWrites: [], traceError: [], cleanupError: [], credentialsRecorded: false
}
const safeError = (error) => String(error?.message || error).split(password).join('[REDACTED]')

async function main() {
  let browser, context, page, tracing = false, failure
  try {
    browser = await chromium.launch({ headless: true })
    context = await browser.newContext({ viewport: { width: 1600, height: 1000 }, locale: 'zh-CN' })
    page = await context.newPage()
    page.setDefaultTimeout(15000)
    await page.goto(`${evidence.baseUrl}/login?redirect=%2Findex`, { waitUntil: 'domcontentloaded' })
    const loginForm = page.locator('form.login-form:visible')
    const tenantField = loginForm.locator('.el-select input[role="combobox"]').first()
    await tenantField.fill(tenant)
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: tenant }).click()
    await loginForm.getByPlaceholder('请输入用户名').fill(username)
    await loginForm.getByPlaceholder('请输入密码').fill(password)
    await loginForm.getByRole('button', { name: '登录', exact: true }).click()
    await page.waitForURL((url) => !url.pathname.includes('/login'), { waitUntil: 'domcontentloaded', timeout: 30000 })

    await context.tracing.start({ screenshots: true, snapshots: false, sources: false })
    tracing = true
    page.on('pageerror', (error) => evidence.pageErrors.push(safeError(error)))
    page.on('response', (response) => {
      const target = new URL(response.url())
      if (!target.pathname.startsWith('/admin-api/dcc/directories')) return
      const entry = { method: response.request().method(), path: target.pathname, status: response.status() }
      evidence.network.push(entry)
      if (entry.method !== 'GET') evidence.directoryWrites.push(entry)
    })
    await page.goto(`${evidence.baseUrl}/dcc/controlled-file/directories`, { waitUntil: 'domcontentloaded' })
    const trigger = page.getByRole('button', { name: '新建目录', exact: true })
    await trigger.waitFor({ state: 'visible' })
    const dialog = page.locator('.el-dialog').filter({
      has: page.locator('.el-dialog__header').filter({ hasText: '新建目录' })
    })
    assert.equal(await dialog.isVisible(), false, 'form must initially be closed')
    evidence.firstOpenClicks++
    await trigger.click()
    await dialog.waitFor({ state: 'visible' })
    await dialog.locator('.el-loading-mask').waitFor({ state: 'hidden' })
    const code = dialog.getByPlaceholder('请输入目录编码')
    const name = dialog.getByPlaceholder('请输入目录名称')
    await code.fill('DCC241016C12I04')
    await name.fill('DCC241016C12首次点击回归')
    assert.equal(await code.inputValue(), 'DCC241016C12I04')
    assert.equal(await name.inputValue(), 'DCC241016C12首次点击回归')
    assert.deepEqual(evidence.pageErrors, [], 'the first click must not produce unhandled page errors')
    evidence.steps.push({ action: 'first-click', status: 'PASS', clicks: evidence.firstOpenClicks,
      title: await dialog.locator('.el-dialog__header').innerText(), code: await code.inputValue(), name: await name.inputValue() })
    evidence.visibleNotifications = await page.locator('.el-message:visible').allTextContents()
    assert.equal(await dialog.getByRole('button', { name: '确定', exact: true }).isEnabled(), true, 'form initialization must finish before screenshot')
    await page.screenshot({ path: path.join(out, 'first-open.png'), animations: 'disabled', fullPage: true, mask: [page.locator('input[type="password"]')] })
    await dialog.getByRole('button', { name: '取消', exact: true }).click()
    await dialog.waitFor({ state: 'hidden' })
    await trigger.click()
    await dialog.waitFor({ state: 'visible' })
    await dialog.locator('.el-loading-mask').waitFor({ state: 'hidden' })
    assert.equal(await code.inputValue(), '', 'cancelled values must not leak into a new create form')
    assert.equal(await name.inputValue(), '')
    evidence.steps.push({ action: 'reopen-after-cancel', status: 'PASS', code: '', name: '' })
    await page.screenshot({ path: path.join(out, 'reopen.png'), animations: 'disabled', fullPage: true, mask: [page.locator('input[type="password"]')] })
    await dialog.getByRole('button', { name: '取消', exact: true }).click()
    await dialog.waitFor({ state: 'hidden' })
    assert.deepEqual(evidence.pageErrors, [])
    assert.equal(evidence.directoryWrites.length, 0, 'open/cancel regression must not save or delete directories')
    evidence.status = 'PASS'
  } catch (error) {
    failure = error
    evidence.status = 'FAIL'
    evidence.error = safeError(error)
    if (page && !page.isClosed()) {
      try {
        evidence.url = page.url()
        evidence.visibleDialogs = await page.locator('.el-dialog:visible').count()
        evidence.visibleText = (await page.locator('body').innerText()).split(password).join('[REDACTED]')
        await page.screenshot({ path: path.join(out, 'failure.png'), fullPage: true, mask: [page.locator('input[type="password"]')] })
      } catch (captureError) { evidence.cleanupError.push({ phase: 'failure-capture', error: safeError(captureError) }) }
    }
  } finally {
    if (tracing) {
      try { await context.tracing.stop({ path: path.join(out, 'trace.zip') }) }
      catch (error) { evidence.traceError.push(safeError(error)); evidence.status = 'FAIL'; failure ||= error }
    }
    for (const [label, resource] of [['context', context], ['browser', browser]]) {
      if (!resource) continue
      try { await resource.close() }
      catch (error) { evidence.cleanupError.push({ phase: label, error: safeError(error) }); evidence.status = 'FAIL'; failure ||= error }
    }
    evidence.exitCode = failure ? 1 : 0
    fs.writeFileSync(path.join(out, 'result.json'), `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
  }
  console.log(JSON.stringify({ issue: evidence.issue, phase, status: evidence.status,
    firstOpenClicks: evidence.firstOpenClicks, pageErrors: evidence.pageErrors, directoryWrites: evidence.directoryWrites.length,
    steps: evidence.steps, traceError: evidence.traceError, cleanupError: evidence.cleanupError, exitCode: evidence.exitCode }, null, 2))
  process.exitCode = evidence.exitCode
}
main().catch((error) => { console.error(safeError(error)); process.exitCode = 1 })
