const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const BACKEND_ROOT = path.resolve(FRONTEND_ROOT, '..', 'ruoyi-vue-pro')
const BASE_URL = (process.env.EDHR_DUPLICATE_NAME_CANCEL_BASE_URL || 'http://127.0.0.1:8096').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_DUPLICATE_NAME_CANCEL_BACKEND_URL || 'http://127.0.0.1:48096').replace(/\/+$/, '')
const TEST_TENANT = process.env.EDHR_DUPLICATE_NAME_CANCEL_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_DUPLICATE_NAME_CANCEL_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_DUPLICATE_NAME_CANCEL_PASSWORD || '111111'
const BATCH_RECORD_NAME = process.env.EDHR_DUPLICATE_NAME_CANCEL_BATCH_RECORD_NAME || 'E2E-PHASE2-1783564189622'
const PRODUCT_NAME_KEYWORD = process.env.EDHR_DUPLICATE_NAME_CANCEL_PRODUCT_NAME || '球囊扩张压力泵'
const SAMPLE_DOC_PATH =
  process.env.EDHR_DUPLICATE_NAME_CANCEL_SAMPLE_DOC ||
  path.join(BACKEND_ROOT, 'yudao-module-mes', 'src', 'test', 'resources', 'fixtures', 'pressure-pump-record.doc')
const ROUTE = '/mes/pro/batch-record-form-list'

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://127.0.0.1:8096', '同名升版取消 E2E 必须使用本机前端 8096')
  assert.equal(BACKEND_URL, 'http://127.0.0.1:48096', '同名升版取消 E2E 必须使用本机后端 48096')
  assert.equal(TEST_TENANT, '测试租户', '同名升版取消 E2E 只允许使用测试租户')
  assert.equal(TEST_USERNAME, 'aoteman', '同名升版取消 E2E 只允许使用测试租户 aoteman')
  assert.ok(fs.existsSync(SAMPLE_DOC_PATH), `缺少真实 Word 模板样本：${SAMPLE_DOC_PATH}`)
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

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && !(await item.isDisabled().catch(() => true))) {
      await item.click()
      return
    }
  }
  throw new Error(`缺少可点击控件：${label}`)
}

async function waitForBusinessResponse(page, endpoint, label, method = 'GET', timeout = 60000) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(endpoint) && item.request().method() === method && item.status() === 200,
    { timeout }
  )
  return assertBusinessSuccess(await response.json(), label)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) {
    await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    return
  }
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(TEST_TENANT)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TEST_TENANT, '租户')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'), TEST_USERNAME, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), TEST_PASSWORD, '密码')
  const [response] = await Promise.all([
    page.waitForResponse((item) => item.url().includes('/admin-api/system/auth/login') && item.status() === 200, {
      timeout: 60000
    }),
    clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }), '登录按钮')
  ])
  assertBusinessSuccess(await response.json(), `${TEST_TENANT}/${TEST_USERNAME} 登录`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
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
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
          current = current.accessToken
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'v')) {
          current = current.v
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'value')) {
          current = current.value
          continue
        }
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

async function resolveProductName(page) {
  const options = await authenticatedGet(
    page,
    '/admin-api/mes/pro/work-order/product-name-options',
    { keyword: PRODUCT_NAME_KEYWORD },
    '生产工单产品名称候选查询'
  )
  assert.ok(Array.isArray(options), `生产工单产品名称候选必须返回数组：${JSON.stringify(options)}`)
  const matched = options.map((item) => String(item || '').trim()).filter((item) => item.includes(PRODUCT_NAME_KEYWORD))
  assert.ok(matched.length > 0, `必须存在真实产品名称候选：${PRODUCT_NAME_KEYWORD} / ${JSON.stringify(options)}`)
  return matched.find((item) => item === PRODUCT_NAME_KEYWORD) || matched[0]
}

async function selectedWordImportProductTags(page) {
  return await page.evaluate(() =>
    Array.from(document.querySelectorAll('.el-dialog .el-select__tags .el-tag, .el-dialog .el-select__tags-text'))
      .map((item) => item.textContent?.replace(/\s+/g, ' ').trim())
      .filter(Boolean)
  )
}

async function main() {
  assertLocalOnly()
  const launchOptions = { headless: process.env.EDHR_DUPLICATE_NAME_CANCEL_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  const writeRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/pro/batch-record-report/recognize-uploaded')) {
      writeRequests.push(request.url())
    }
  })
  try {
    await login(page)
    await page.getByText('批记录名称').first().waitFor({ state: 'visible', timeout: 60000 })
    const productName = await resolveProductName(page)
    const fileChooserPromise = page.waitForEvent('filechooser', { timeout: 30000 })
    await clickFirstEnabled(page.getByRole('button', { name: /导入 Word/ }), '导入 Word')
    const fileChooser = await fileChooserPromise
    await fileChooser.setFiles(SAMPLE_DOC_PATH)

    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入 Word' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    await fillFirstVisible(
      dialog.locator('.el-form-item').filter({ hasText: '批记录名称' }).locator('input'),
      BATCH_RECORD_NAME,
      '批记录名称'
    )
    const productSelect = dialog.locator('.el-form-item').filter({ hasText: '工艺路线对应产品名称' }).locator('.el-select').first()
    await productSelect.click()
    const productInput = productSelect.locator('input:visible').first()
    const optionResponse = page
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/work-order/product-name-options') && response.status() === 200,
        { timeout: 60000 }
      )
      .catch(() => null)
    await productInput.click()
    await productInput.fill(productName)
    await optionResponse
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
      .filter({ hasText: productName })
      .first()
    await option.waitFor({ state: 'visible', timeout: 60000 })
    await option.click({ force: true })
    assert.ok(
      (await selectedWordImportProductTags(page)).some((item) => item.includes(productName)),
      `导入前必须选择真实产品名称：${productName}`
    )
    await page.keyboard.press('Escape')
    await page.locator('.el-select-dropdown:visible').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined)
    await dialog.waitFor({ state: 'visible', timeout: 60000 })

    const existsPromise = waitForBusinessResponse(
      page,
      '/admin-api/mes/pro/batch-record-report/exists',
      '批记录名称重复检查',
      'GET',
      60000
    ).catch((error) => ({ __error: error }))
    const confirmButton = dialog.getByRole('button', { name: /^确定$/ }).first()
    await confirmButton.waitFor({ state: 'visible', timeout: 60000 })
    if ((await page.locator('.el-select-dropdown:visible').count()) > 0) {
      await page.keyboard.press('Escape')
      await confirmButton.evaluate((button) => button.click())
    } else {
      await confirmButton.click()
    }
    const existed = await existsPromise
    if (existed && existed.__error) {
      throw new Error(`批记录名称重复检查等待失败：${existed.__error.message}`)
    }
    assert.equal(existed, true, `测试数据必须触发同名导入确认：${BATCH_RECORD_NAME}`)

    const upgradeConfirm = page.locator('.el-message-box:visible').filter({ hasText: '是否升版' }).first()
    await upgradeConfirm.waitFor({ state: 'visible', timeout: 60000 })
    await upgradeConfirm.getByText('否，放弃本次导入').waitFor({ state: 'visible', timeout: 60000 })
    await clickFirstEnabled(upgradeConfirm.getByRole('button', { name: '否，放弃本次导入' }), '否，放弃本次导入')
    await upgradeConfirm.waitFor({ state: 'hidden', timeout: 60000 })
    await page.waitForTimeout(2000)
    assert.equal(writeRequests.length, 0, `选择否放弃时不得调用导入写接口：${writeRequests.join(',')}`)
    console.log(`PASS: duplicate-name upgrade cancel real E2E batchRecordName=${BATCH_RECORD_NAME} writeRequests=0`)
  } catch (error) {
    const outputDir = path.join(__dirname, 'output', 'edhr-duplicate-name-upgrade-cancel')
    fs.mkdirSync(outputDir, { recursive: true })
    await page.screenshot({ path: path.join(outputDir, `failure-${Date.now()}.png`), fullPage: true }).catch(() => undefined)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
