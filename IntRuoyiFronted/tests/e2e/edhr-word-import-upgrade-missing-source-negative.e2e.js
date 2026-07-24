const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = 'http://localhost:8081'
const BACKEND_URL = 'http://127.0.0.1:48081'
const TENANT = '测试租户'
const USERNAME = 'aoteman'
const PASSWORD = '111111'
const TARGET_PATH = '/mes/pro/batch-record-form-list'
const PROJECT_NAME = '球囊扩张压力泵'
const SAMPLE_DOC_PATH =
  process.env.EDHR_WORD_UPGRADE_SAMPLE_DOC ||
  'C:\\Users\\BJB110\\Desktop\\文档\\批记录压力泵.doc'
const FALLBACK_DOC_PATH = path.join(
  WORKSPACE_ROOT,
  'ruoyi-vue-pro',
  'yudao-module-mes',
  'src',
  'test',
  'resources',
  'fixtures',
  'pressure-pump-record.doc'
)

function resolveSampleDoc() {
  return fs.existsSync(SAMPLE_DOC_PATH) ? SAMPLE_DOC_PATH : FALLBACK_DOC_PATH
}

function assertBusinessSuccess(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 必须返回 JSON 对象`)
  const code = Number(body.code)
  assert.ok([0, 200].includes(code), `${label} 业务响应失败：${body.msg || body.message || body.code}`)
  return body.data
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写控件：${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TENANT, '租户')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'), USERNAME, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), PASSWORD, '密码')
  await Promise.all([
    page.waitForResponse((item) => item.url().includes('/admin-api/system/auth/login') && item.status() === 200, {
      timeout: 60000
    }),
    loginForm.getByRole('button', { name: /^登录$/ }).click()
  ])
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
}

async function browserAuth(page) {
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
  const unwrap = (raw) => {
    if (!raw) return ''
    let current = raw
    for (let index = 0; index < 6; index += 1) {
      try {
        current = JSON.parse(current)
      } catch {
        break
      }
      if (current && typeof current === 'object') {
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) current = current.accessToken
        else if (Object.prototype.hasOwnProperty.call(current, 'v')) current = current.v
        else if (Object.prototype.hasOwnProperty.call(current, 'value')) current = current.value
      }
      if (typeof current !== 'string') break
    }
    return String(current || '').replace(/^"|"$/g, '')
  }
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

async function authenticatedGet(page, endpoint, params, label) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, `${label} 需要浏览器登录 token`)
  assert.equal(String(tenantId), '122', `${label} 必须在测试租户执行`)
  const response = await page.request.get(`${BACKEND_URL}${endpoint}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    params
  })
  assert.equal(response.status(), 200, `${label} HTTP 必须为 200`)
  return assertBusinessSuccess(await response.json(), label)
}

async function main() {
  assert.ok(fs.existsSync(resolveSampleDoc()), `缺少真实 Word 样本：${SAMPLE_DOC_PATH} / ${FALLBACK_DOC_PATH}`)
  const launchOptions = { headless: process.env.EDHR_WORD_UPGRADE_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page)
    const before = await authenticatedGet(
      page,
      '/admin-api/mes/pro/batch-record-report/page',
      { pageNo: 1, pageSize: 200 },
      '缺源版本负向提交前表单分页查询'
    )
    const beforeList = before?.list || []
    const targetReport = beforeList.find((item) =>
      item.routeKey === 'B' &&
      item.formSlotType === 'MAIN' &&
      item.batchRecordName &&
      item.batchRecordVersionId &&
      (String(item.productName || '').includes(PROJECT_NAME) ||
        String(item.batchRecordName || '').includes(PROJECT_NAME) ||
        String(item.sourceFileName || '').includes('压力泵'))
    )
    assert.ok(targetReport, `缺源版本负向提交需要真实已有批记录主表单：${JSON.stringify(beforeList.slice(0, 5))}`)
    const beforeVersionCount = beforeList.filter(
      (item) => Number(item.batchRecordVersionId) === Number(targetReport.batchRecordVersionId)
    ).length
    const { token, tenantId, visitTenantId } = await browserAuth(page)
    const response = await page.request.post(`${BACKEND_URL}/admin-api/mes/pro/batch-record-report/recognize-uploaded`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'tenant-id': String(tenantId),
        ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
      },
      multipart: {
        file: {
          name: path.basename(resolveSampleDoc()),
          mimeType: 'application/msword',
          buffer: fs.readFileSync(resolveSampleDoc())
        },
        routeKey: 'B',
        batchRecordName: targetReport.batchRecordName,
        upgrade: 'true',
        importAction: 'UPGRADE',
        productNames: PROJECT_NAME,
        rebuildBatchRecord: 'true'
      },
      timeout: 180000
    })
    assert.equal(response.status(), 200, '缺源版本负向提交 HTTP 必须为 200')
    const body = await response.json()
    assert.equal(Number(body.code), 1040509060, `缺源版本升版必须被拒绝：${JSON.stringify(body)}`)
    const after = await authenticatedGet(
      page,
      '/admin-api/mes/pro/batch-record-report/page',
      { pageNo: 1, pageSize: 200 },
      '缺源版本负向提交后表单分页查询'
    )
    const afterVersionCount = (after?.list || []).filter(
      (item) => Number(item.batchRecordVersionId) === Number(targetReport.batchRecordVersionId)
    ).length
    assert.equal(afterVersionCount, beforeVersionCount, '缺源版本负向提交不得新增当前版本表单')
    console.log(`PASS: missing expectedSourceVersionId negative E2E rejected code=${body.code} batch=${targetReport.batchRecordName}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
