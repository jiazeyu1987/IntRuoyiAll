/**
 * Read-only real-browser prerequisite probe for the 2026-10-28 eDHR changes.
 * This is not a six-chain business E2E test and never reports business E2E PASS.
 * All requests originate from real UI navigation; no API client or DB is used.
 * Required environment: EDHR_CHAIN_FRONTEND_URL, EDHR_CHAIN_PASSWORD.
 */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const root = path.resolve(__dirname, '../../..')
const artifactDir = path.join(root, 'output', 'playwright', '20261028-edhr-six-chain-readiness', new Date().toISOString().replace(/[:.]/g, '-'))
const probes = [
  { name: 'NCR列表', route: '/mes/pro/feedback/edhr-nonconformance-review', endpoint: '/mes/pro/edhr-nonconformance-review/page', selector: '[data-edhr-ncr-page]' },
  { name: '偏差列表', route: '/mes/pro/feedback/edhr-deviation', endpoint: '/mes/pro/edhr-deviation/page', selector: '.edhr-record-viewport' },
  { name: '记录变更列表', route: '/mes/pro/feedback/edhr-change', endpoint: '/mes/pro/edhr-change/page', selector: '.edhr-record-change__table' },
  { name: 'PQC生产放行列表', route: '/mes/production-release/pqc', endpoint: '/mes/pro/production-release/pqc/page', selector: '[data-pqc-production-release-page]' }
]

const result = {
  scope: 'READ_ONLY_ENVIRONMENT_PROBE',
  fullBusinessE2e: 'NOT_RUN',
  outcome: 'BLOCKED',
  startedAt: new Date().toISOString(),
  checks: [],
  responses: [],
  writes: [],
  pageErrors: [],
  artifactErrors: []
}
let browser
let page
let phase = 'configuration'

function required(name) {
  const value = process.env[name]?.trim()
  assert.ok(value, `Missing required environment variable: ${name}`)
  return value
}

function serialized(value) {
  const json = JSON.stringify(value, null, 2)
  const password = process.env.EDHR_CHAIN_PASSWORD?.trim()
  return password ? json.split(password).join('[REDACTED]') : json
}

function matches(response, endpoint) {
  return new URL(response.url()).pathname === `/admin-api${endpoint}`
}

async function inspectResponse(response, name) {
  const body = await response.json()
  const summary = {
    name,
    path: new URL(response.url()).pathname,
    status: response.status(),
    code: body.code,
    message: body.msg || '',
    total: body.data?.total,
    rowCount: Array.isArray(body.data?.list) ? body.data.list.length : undefined
  }
  result.responses.push(summary)
  assert.ok(response.ok(), `${name}: HTTP ${response.status()}`)
  assert.equal(body.code, 0, `${name}: business code=${body.code}; ${body.msg || ''}`)
  return body.data
}

async function main() {
  const baseUrl = new URL(required('EDHR_CHAIN_FRONTEND_URL'))
  assert.ok(['127.0.0.1', 'localhost'].includes(baseUrl.hostname), 'Only the explicitly selected local runtime is supported')
  assert.equal(baseUrl.pathname, '/', 'Frontend URL must be an origin without a route')
  const password = required('EDHR_CHAIN_PASSWORD')
  // This approved tenant/account is intentionally fixed; no environment defaults are inferred.
  result.frontend = baseUrl.origin
  result.tenant = '芋道源码'
  result.username = 'admin'
  fs.mkdirSync(artifactDir, { recursive: true })

  phase = 'launch Chrome'
  browser = await chromium.launch({ headless: true, channel: 'chrome' })
  const context = await browser.newContext({ locale: 'zh-CN', viewport: { width: 1440, height: 1000 } })
  page = await context.newPage()
  page.setDefaultTimeout(30000)
  page.setDefaultNavigationTimeout(45000)
  page.on('pageerror', error => result.pageErrors.push(error.message))
  page.on('request', request => {
    const pathname = new URL(request.url()).pathname
    if (pathname.startsWith('/admin-api/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      result.writes.push({ method: request.method(), path: pathname })
    }
  })

  phase = 'real UI login'
  await page.goto(new URL('/login?redirect=/user/profile', baseUrl.origin).toString(), { waitUntil: 'domcontentloaded' })
  const form = page.locator('.login-form:visible').first()
  await form.waitFor({ state: 'visible' })
  assert.match(await form.innerText(), /芋道源码/, 'Login page must already select the approved tenant; stop rather than guessing another tenant')
  await form.getByRole('textbox', { name: '请输入用户名', exact: true }).fill('admin')
  await form.getByRole('textbox', { name: '请输入密码', exact: true }).fill(password)

  // Attach receipts before the visible login click. Errors are retained and checked;
  // they are not retried and do not become an alternate success path.
  const loginReceipt = page.waitForResponse(response => matches(response, '/system/auth/login'))
    .then(response => inspectResponse(response, '真实登录'))
    .then(() => ({ ok: true }), error => ({ ok: false, error }))
  const workbenchReceipt = page.waitForResponse(response =>
    matches(response, '/workbench/tasks/page') || matches(response, '/workbench/tasks/count')
  ).then(response => inspectResponse(response, '个人工作台来源查询'))
    .then(() => ({ ok: true }), error => ({ ok: false, error }))

  await form.getByRole('button', { name: '登录', exact: true }).click()
  const login = await loginReceipt
  if (!login.ok) throw login.error
  result.checks.push({ name: '真实登录', outcome: 'PASSED' })
  phase = 'workbench readiness (fail before opening business pages)'
  const workbench = await workbenchReceipt
  if (!workbench.ok) throw workbench.error
  await page.getByRole('tab', { name: '个人工作台', exact: true }).waitFor({ state: 'visible' })
  assert.equal(await page.getByText('待办任务加载失败', { exact: true }).count(), 0, 'Workbench shows a failed state')
  result.checks.push({ name: '个人工作台来源查询', outcome: 'PASSED' })

  for (const probe of probes) {
    phase = probe.name
    const receipt = page.waitForResponse(response => matches(response, probe.endpoint))
      .then(response => inspectResponse(response, probe.name))
      .then(data => ({ ok: true, data }), error => ({ ok: false, error }))
    await page.goto(new URL(probe.route, baseUrl.origin).toString(), { waitUntil: 'domcontentloaded' })
    const response = await receipt
    if (!response.ok) throw response.error
    await page.locator(probe.selector).waitFor({ state: 'visible' })
    result.checks.push({ name: probe.name, route: probe.route, outcome: 'PASSED' })
  }

  assert.equal(result.pageErrors.length, 0, `Page errors: ${result.pageErrors.join('; ')}`)
  const unexpectedWrites = result.writes.filter(write =>
    !['/admin-api/system/auth/login', '/admin-api/workbench/context'].includes(write.path)
  )
  assert.equal(unexpectedWrites.length, 0, `Unexpected frontend write request: ${JSON.stringify(unexpectedWrites)}`)
  result.outcome = 'READ_ONLY_PREREQUISITES_PASSED'
}

main().catch(error => {
  result.blocker = { phase, name: error.name, message: error.message }
  process.exitCode = 1
}).finally(async () => {
  fs.mkdirSync(artifactDir, { recursive: true })
  if (page) {
    result.lastUrl = page.url()
    try {
      await page.screenshot({ path: path.join(artifactDir, 'last-page.png'), fullPage: true })
    } catch (error) {
      result.artifactErrors.push({ artifact: 'screenshot', message: error.message })
      result.outcome = 'BLOCKED'
      process.exitCode = 1
    }
  }
  if (browser) {
    try {
      await browser.close()
    } catch (error) {
      result.artifactErrors.push({ artifact: 'browser-close', message: error.message })
      result.outcome = 'BLOCKED'
      process.exitCode = 1
    }
  }
  result.finishedAt = new Date().toISOString()
  fs.writeFileSync(path.join(artifactDir, 'result.json'), `${serialized(result)}\n`, 'utf8')
  process.stdout.write(`${serialized({ outcome: result.outcome, fullBusinessE2e: result.fullBusinessE2e, blocker: result.blocker, artifactDir })}\n`)
})
