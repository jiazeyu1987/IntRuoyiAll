const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')
const envPath = path.join(frontendRoot, '.env')

function parseEnv() {
  const env = {}
  for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/)
    if (match) env[match[1]] = match[2].replace(/^['"]|['"]$/g, '').trim()
  }
  return env
}

const env = parseEnv()
const config = {
  baseUrl: (process.env.CURRENT_TASK_BASE_URL || 'http://127.0.0.1:8082').replace(/\/+$/, ''),
  backendUrl: (process.env.CURRENT_TASK_BACKEND_URL || 'http://127.0.0.1:48082').replace(/\/+$/, ''),
  tenant: process.env.CURRENT_TASK_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
  username: process.env.CURRENT_TASK_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
  password: process.env.CURRENT_TASK_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD,
  timeout: Number(process.env.CURRENT_TASK_TIMEOUT || 90000)
}

function assertRuntimePair() {
  const frontend = new URL(config.baseUrl)
  const backend = new URL(config.backendUrl)
  assert.equal(frontend.hostname, '127.0.0.1')
  assert.equal(backend.hostname, '127.0.0.1')
  assert.equal(Number(backend.port) - Number(frontend.port), 40000)
  assert.equal(config.tenant, '芋道源码')
  assert.equal(config.username, 'admin')
  assert.ok(config.password, '缺少默认登录密码来源')
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible().catch(() => false)) && !(await input.isDisabled().catch(() => true))) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写登录控件：${label}`)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').filter({ visible: true }).first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: config.timeout })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, '租户')
}

async function login(page) {
  const url = new URL('/login', config.baseUrl)
  url.searchParams.set('redirect', '/user/profile')
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页启用了验证码，无法执行无人值守真实 E2E。')
  }
  await selectTenant(page, form)
  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'), config.username, '账号')
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, '密码')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await loginResponsePromise
  const body = await response.json()
  assert.ok(response.ok(), `登录 HTTP 失败：${response.status()}`)
  assert.ok(body.code === 0 || body.code === 200, `登录失败：${body.msg || body.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: config.timeout })
}

function unwrap(raw) {
  if (!raw) return ''
  let current = raw
  for (let index = 0; index < 8; index += 1) {
    try {
      current = JSON.parse(current)
    } catch {
      break
    }
    if (current && typeof current === 'object') {
      if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
        current = current.accessToken
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'value')) {
        current = current.value
        continue
      }
      if (Object.prototype.hasOwnProperty.call(current, 'v')) {
        current = current.v
        continue
      }
    }
    if (typeof current !== 'string') break
  }
  return String(current || '').replace(/^"|"$/g, '')
}

async function authSnapshot(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      result[key] = result[key] || sessionStorage.getItem(key)
    }
    return result
  })
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

async function apiGet(page, auth, pathName, params = {}) {
  const url = new URL(pathName.replace(/^\/+/, ''), `${config.backendUrl}/admin-api/`)
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null) url.searchParams.set(key, String(value))
  }
  const response = await page.request.get(url.toString(), {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      'tenant-id': String(auth.tenantId),
      ...(auth.visitTenantId ? { 'visit-tenant-id': String(auth.visitTenantId) } : {})
    },
    timeout: config.timeout
  })
  assert.notEqual(response.status(), 404, `${pathName} 不应再返回 HTTP 404`)
  assert.equal(response.status(), 200, `${pathName} HTTP 状态应为 200`)
  const body = await response.json()
  assert.notEqual(String(body.msg || body.message || ''), '请求地址不存在', `${pathName} 不应再返回“请求地址不存在”`)
  assert.equal(body.code, 0, `${pathName} 业务响应应成功：${JSON.stringify(body)}`)
  return body.data
}

async function main() {
  assertRuntimePair()
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(page)
    const auth = await authSnapshot(page)
    assert.ok(auth.token, '登录后必须取得 access token')
    assert.ok(auth.tenantId, '登录后必须取得 tenant-id')

    const dossier = await apiGet(page, auth, '/mes/pro/edhr-release-setting/dossier-requirements')
    for (const field of [
      'incomingInspectionReportRequired',
      'sterilizationReportRequired',
      'finishedProductInspectionReportRequired',
      'finishedProductInspectionRecordRequired'
    ]) {
      assert.equal(typeof dossier[field], 'boolean', `资料限制字段必须是 boolean：${field}`)
    }

    const routePage = await apiGet(page, auth, '/mes/pro/route/page', { pageNo: 1, pageSize: 10 })
    const route = (routePage.list || []).find((item) => Number(item.id) > 0)
    assert.ok(route, '当前租户必须至少有一条工艺路线用于接口存在性验证')
    const owners = await apiGet(page, auth, '/mes/pro/route/flow-config/batch-record-attachment-owners', {
      routeId: route.id,
      routeVersionId: route.pendingRouteVersionId || route.activeRouteVersionId || undefined
    })
    assert.equal(owners.length, 4, '批记录附件负责人接口必须返回 4 个固定记录/报告')
    const names = owners.map((owner) => owner.attachmentName).sort()
    assert.deepEqual(names, ['成品检报告', '成品检记录', '来料检报告', '灭菌报告'].sort())
    const roleNames = owners.map((owner) => owner.defaultRoleName).sort()
    assert.deepEqual(roleNames, ['成品检报告上传1', '成品检记录上传1', '来料检报告上传1', '灭菌报告上传1'].sort())
  } finally {
    await browser.close()
  }
  console.log('PASS: current task route attachment owner and dossier requirement endpoints are reachable through real login')
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
