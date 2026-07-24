const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const BACKEND_ROOT = path.resolve(FRONTEND_ROOT, '..', 'ruoyi-vue-pro')
const BASE_URL = (process.env.EDHR_PHASE1_BASE_URL || 'http://127.0.0.1:8096').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_PHASE1_BACKEND_URL || 'http://127.0.0.1:48096').replace(/\/+$/, '')
const TEST_TENANT = process.env.EDHR_PHASE1_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_PHASE1_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_PHASE1_PASSWORD || '111111'
const APPROVER_USERNAME = process.env.EDHR_PHASE1_APPROVER_USERNAME || 'smokeappr1'
const APPROVER_PASSWORD = process.env.EDHR_PHASE1_APPROVER_PASSWORD || '111111'
const SAMPLE_DOC_PATH =
  process.env.EDHR_PHASE1_SAMPLE_DOC ||
  path.join(BACKEND_ROOT, 'yudao-module-mes', 'src', 'test', 'resources', 'fixtures', 'pressure-pump-record.doc')
const PRODUCT_NAME_KEYWORD = process.env.EDHR_PHASE1_PRODUCT_NAME || '球囊扩张压力泵'
const ROUTE = '/mes/pro/batch-record-form-list'
const ROUTE_KEY = 'B'
const RUN_ID = process.env.EDHR_PHASE1_RUN_ID || String(Date.now())
const BATCH_RECORD_NAME = process.env.EDHR_PHASE1_BATCH_RECORD_NAME || `E2E-PHASE1-${RUN_ID}`

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://127.0.0.1:8096', '一期升版 E2E 必须使用 edhr_version 前端 8096')
  assert.equal(BACKEND_URL, 'http://127.0.0.1:48096', '一期升版 E2E 必须使用 edhr_version 后端 48096')
  assert.equal(TEST_TENANT, '测试租户', '写入验证必须使用测试租户')
  assert.equal(TEST_USERNAME, 'aoteman', '写入验证必须使用测试租户 aoteman')
  assert.notEqual(APPROVER_USERNAME, TEST_USERNAME, '一期升版审批必须使用独立审批人账号，不能自审')
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
    if (await item.isVisible()) {
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
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click()
      return
    }
  }
  throw new Error(`缺少可点击控件：${label}`)
}

async function waitForBusinessResponse(page, endpoint, label, method, timeout = 180000) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(endpoint) && item.request().method() === method,
    { timeout }
  )
  await response.finished().catch(() => undefined)
  assert.equal(response.status(), 200, `${label} HTTP 必须为 200`)
  return assertBusinessSuccess(await response.json(), label)
}

async function maybeBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    console.log(`WARN: ${label} 未捕获到响应：${result.__error.message}`)
    return undefined
  }
  return result
}

async function unwrapBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    throw new Error(`${label} 等待失败：${result.__error.message}`)
  }
  return result
}

async function login(page, username = TEST_USERNAME, password = TEST_PASSWORD, targetRoute = ROUTE) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(targetRoute)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(TEST_TENANT)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TEST_TENANT, '租户')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), password, '密码')
  await clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }), '登录按钮')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  if (targetRoute) {
    await page.goto(`${BASE_URL}${targetRoute}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  }
}

async function openTemplatePage(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批记录名称').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('button', { name: /导入 Word/ }).first().waitFor({ state: 'visible', timeout: 60000 })
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
  assert.ok(tenantId, `${label} 需要 tenant-id`)
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

async function authenticatedPost(page, endpoint, params, label) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, `${label} 需要浏览器登录 token`)
  assert.ok(tenantId, `${label} 需要 tenant-id`)
  const response = await page.request.post(`${BACKEND_URL}${endpoint}`, {
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

async function selectedWordImportProductTags(page) {
  return await page.evaluate(() =>
    Array.from(document.querySelectorAll('.el-dialog .el-select__tags .el-tag, .el-dialog .el-select__tags-text'))
      .map((item) => item.textContent?.replace(/\s+/g, ' ').trim())
      .filter(Boolean)
  )
}

async function resolveProductNamesByApi(page) {
  const options = await authenticatedGet(
    page,
    '/admin-api/mes/pro/work-order/product-name-options',
    { keyword: PRODUCT_NAME_KEYWORD },
    '生产工单产品名称候选查询'
  )
  assert.ok(Array.isArray(options), `生产工单产品名称候选必须返回数组：${JSON.stringify(options)}`)
  const matched = options.map((item) => String(item || '').trim()).filter((item) => item.includes(PRODUCT_NAME_KEYWORD))
  assert.ok(matched.length > 0, `必须存在真实产品名称候选：${PRODUCT_NAME_KEYWORD} / ${JSON.stringify(options)}`)
  return [matched.find((item) => item === PRODUCT_NAME_KEYWORD) || matched[0]]
}

async function importWordTemplateByUi(page) {
  await openTemplatePage(page)
  const productNames = await resolveProductNamesByApi(page)
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
  for (const productName of productNames) {
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
    await page.keyboard.press('Enter')
    if (!(await selectedWordImportProductTags(page)).some((item) => item.includes(productName))) {
      await option.click({ force: true })
    }
  }
  assert.ok((await selectedWordImportProductTags(page)).length > 0, '导入前必须选择真实产品名称')
  await page.keyboard.press('Escape')
  await page.evaluate(() => {
    const activeElement = document.activeElement
    if (activeElement instanceof HTMLElement) {
      activeElement.blur()
    }
  })
  await page.locator('.el-select-dropdown:visible').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined)
  await dialog.waitFor({ state: 'visible', timeout: 60000 })

  const existsPromise = waitForBusinessResponse(
    page,
    '/admin-api/mes/pro/batch-record-report/exists',
    '批记录名称重复检查',
    'GET',
    10000
  ).catch((error) => ({ __error: error }))
  const uploadResponsePromise = waitForBusinessResponse(
    page,
    '/admin-api/mes/pro/batch-record-report/recognize-uploaded',
    'Word 导入识别保存',
    'POST',
    600000
  ).catch((error) => ({ __error: error }))
  const confirmButton = dialog.getByRole('button', { name: /^确定$/ }).first()
  await confirmButton.waitFor({ state: 'visible', timeout: 60000 })
  if ((await page.locator('.el-select-dropdown:visible').count()) > 0) {
    await page.keyboard.press('Escape')
    await confirmButton.evaluate((button) => button.click())
  } else {
    await confirmButton.click()
  }
  const existed = await maybeBusinessWait(existsPromise, '批记录名称重复检查')
  if (existed) {
    const upgradeConfirm = page.locator('.el-message-box:visible').filter({ hasText: '是否使用 B Word COM 升级' }).first()
    await upgradeConfirm.waitFor({ state: 'visible', timeout: 60000 })
    await clickFirstEnabled(upgradeConfirm.locator('button.el-button--primary'), '确认升级批记录')
  }
  const importResult = await unwrapBusinessWait(uploadResponsePromise, 'Word 导入识别保存')
  assert.ok(importResult.batchRecordDefinitionId, `导入必须返回 definitionId：${JSON.stringify(importResult)}`)
  assert.ok(importResult.batchRecordVersionId, `导入必须返回 versionId：${JSON.stringify(importResult)}`)
  assert.ok(importResult.versionNo, `导入必须返回明确版本号：${JSON.stringify(importResult)}`)
  assert.equal(importResult.versionNo, 'V1.0', `首次导入必须生成 V1.0：${JSON.stringify(importResult)}`)
  assert.equal(importResult.versionStatus, 'APPROVED', `V1.0 首次导入必须直接生效：${JSON.stringify(importResult)}`)
  assert.ok(importResult.routeId, `导入必须返回新版本路线 routeId：${JSON.stringify(importResult)}`)

  await page.waitForTimeout(500)
  assert.equal(
    await page.locator('[data-testid="edhr-batch-version-phase1-panel"]').count(),
    0,
    'V1.0 首次导入不应显示版本提示面板'
  )
  assert.equal(
    await page.getByText(/V1\.0.*无需审批|无需审批.*V1\.0|首次版本无需审批/).count(),
    0,
    'V1.0 首次导入不应显示任何无需审批提示'
  )
  assert.equal(await page.getByRole('button', { name: /提交升版审批/ }).count(), 0, 'V1.0 首次导入不应显示提交升版审批按钮')
  return importResult
}

async function submitApprovalByUi(page, versionId) {
  const submitResponsePromise = waitForBusinessResponse(
    page,
    '/admin-api/mes/pro/batch-record-report/version-approval/submit',
    '提交升版审批',
    'POST',
    60000
  ).catch((error) => ({ __error: error }))
  await clickFirstEnabled(page.getByRole('button', { name: /提交升版审批/ }), '提交升版审批')
  const approvalResult = await unwrapBusinessWait(submitResponsePromise, '提交升版审批')
  assert.equal(Number(approvalResult.versionId), Number(versionId), `审批提交必须返回同一个版本：${JSON.stringify(approvalResult)}`)
  assert.equal(approvalResult.versionStatus, 'PENDING_APPROVAL', `提交后必须进入 PENDING_APPROVAL：${JSON.stringify(approvalResult)}`)
  const panel = page.locator('[data-testid="edhr-batch-version-phase1-panel"]').first()
  await panel.getByText(/审批中|PENDING_APPROVAL/).first().waitFor({ state: 'visible', timeout: 60000 })
  return approvalResult
}

async function verifyVersionIsolationByApi(page, importResult) {
  const reports = await authenticatedGet(
    page,
    '/admin-api/mes/pro/batch-record-report/page',
    { pageNo: 1, pageSize: 50, routeKey: ROUTE_KEY, batchRecordName: BATCH_RECORD_NAME },
    '导入后报表分页查询'
  )
  const list = reports?.list || []
  assert.ok(list.length > 0, `导入后必须能按批记录名称查询到报表：${BATCH_RECORD_NAME}`)
  assert.ok(
    list.every((item) => Number(item.batchRecordVersionId) === Number(importResult.batchRecordVersionId)),
    `报表必须绑定本次版本ID：${JSON.stringify(list)}`
  )

  const routeConfigs = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route/flow-config',
    { routeId: importResult.routeId, useType: 'BATCH' },
    '路线用途绑定查询'
  )
  assert.ok(routeConfigs.length > 0, `导入后路线用途必须存在：${JSON.stringify(routeConfigs)}`)
  const bindings = routeConfigs.flatMap((config) => config.batchRecordReports || [])
  assert.ok(bindings.length > 0, `路线用途必须绑定报表：${JSON.stringify(routeConfigs)}`)
  assert.ok(
    bindings.every((binding) => Number(binding.batchRecordVersionId) === Number(importResult.batchRecordVersionId)),
    `路线用途必须绑定本次版本ID：${JSON.stringify(bindings)}`
  )
}

async function verifyRuntimeOwnership(page) {
  const health = await page.request.get(`${BACKEND_URL}/actuator/health`)
  assert.equal(health.status(), 200, '后端 48096 actuator 必须可达')
  const body = await health.json()
  assert.equal(body.status, 'UP', `后端 48096 health 必须 UP：${JSON.stringify(body)}`)
}

async function main() {
  assertLocalOnly()
  const launchOptions = { headless: process.env.EDHR_PHASE1_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const requests = []
  page.on('request', (request) => {
    const url = request.url()
    if (url.includes('/admin-api/mes/') || url.includes('/actuator/health')) {
      requests.push(`${request.method()} ${url}`)
    }
  })
  try {
    await verifyRuntimeOwnership(page)
    await login(page)
    const importResult = await importWordTemplateByUi(page)
    await verifyVersionIsolationByApi(page, importResult)
    assert.ok(
      requests.some((item) => item.includes('127.0.0.1:48096/admin-api/mes/pro/batch-record-report/recognize-uploaded')),
      `导入请求必须命中 48096：${JSON.stringify(requests)}`
    )
    assert.equal(
      requests.some((item) => item.includes('127.0.0.1:48096/admin-api/mes/pro/batch-record-report/version-approval/submit')),
      false,
      `V1.0 首次导入不得提交升版审批：${JSON.stringify(requests)}`
    )
    console.log(
      `PASS: phase1 V1.0 no-approval E2E batchRecordName=${BATCH_RECORD_NAME} definitionId=${importResult.batchRecordDefinitionId} versionId=${importResult.batchRecordVersionId} versionNo=${importResult.versionNo} routeId=${importResult.routeId} submitter=${TEST_USERNAME}`
    )
  } catch (error) {
    const outputDir = path.join(__dirname, 'output', 'edhr-batch-version-phase1')
    fs.mkdirSync(outputDir, { recursive: true })
    if (!page.isClosed()) {
      await page.screenshot({ path: path.join(outputDir, `failure-${RUN_ID}.png`), fullPage: true }).catch(() => undefined)
      const html = await page.content().catch(() => '')
      if (html) {
        fs.writeFileSync(path.join(outputDir, `failure-${RUN_ID}.html`), html, 'utf8')
      }
    }
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
