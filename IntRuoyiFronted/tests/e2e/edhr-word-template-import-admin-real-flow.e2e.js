const assert = require('assert')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const BASE_URL = 'http://localhost:8081'
const BACKEND_URL = 'http://127.0.0.1:48081'
const TENANT = '芋道源码'
const USERNAME = 'admin'
const PASSWORD = process.env.EDHR_WORD_IMPORT_ADMIN_PASSWORD || 'admin123'
const SAMPLE_DOC_PATH = process.env.EDHR_WORD_IMPORT_SAMPLE_DOC
const SAMPLE_DOC_NAME = path.basename(SAMPLE_DOC_PATH || '')
const ROUTE = '/mes/pro/batch-record-form-list'
const RUN_ID = process.env.EDHR_WORD_IMPORT_RUN_ID || String(Date.now())
const BATCH_RECORD_NAME =
  process.env.EDHR_WORD_IMPORT_BATCH_RECORD_NAME || `ADMIN-WORD-ROUTE-${RUN_ID}`
let runtimeAuth = {}

function assertLocalAdminRun() {
  assert.ok(SAMPLE_DOC_PATH, '必须通过 EDHR_WORD_IMPORT_SAMPLE_DOC 指定真实 Word 文件')
  assert.ok(fs.existsSync(SAMPLE_DOC_PATH), `缺少真实 Word 文件：${SAMPLE_DOC_PATH}`)
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
  throw new Error(`找不到可见输入框：${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && (await item.isEnabled().catch(() => false))) {
      await item.click()
      return
    }
  }
  throw new Error(`找不到可点击按钮：${label}`)
}

function assertBusinessSuccess(payload, label) {
  assert.equal(payload?.code, 0, `${label} 业务响应失败：${payload?.msg || JSON.stringify(payload)}`)
  return payload.data
}

async function waitForBusinessResponse(page, endpoint, label, method, timeout = 180000) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(endpoint) && item.request().method() === method,
    { timeout }
  )
  assert.equal(response.status(), 200, `${label} HTTP 必须为 200`)
  return assertBusinessSuccess(await response.json(), label)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input.el-input__inner').nth(0), TENANT, '租户')
  }

  await loginForm.locator('input.el-input__inner:not([role="combobox"])').first().fill(USERNAME)
  await loginForm.locator('input[type="password"]').first().fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.status(), 200, '登录接口 HTTP 必须为 200')
  const loginData = assertBusinessSuccess(await loginResponse.json(), '芋道源码 admin 登录')
  runtimeAuth = {
    token: loginData?.accessToken,
    tenantId: 1
  }
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
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
    return result
  })
  let token
  let tenantId
  let visitTenantId
  for (const [key, rawValue] of Object.entries(snapshot)) {
    if (!rawValue) continue
    if (!token && /token/i.test(key)) {
      try {
        const parsed = JSON.parse(rawValue)
        token = parsed?.accessToken || parsed?.access_token || parsed?.value || parsed?.token
      } catch (_) {
        token = rawValue
      }
    }
    if (!tenantId && /tenant/i.test(key)) {
      try {
        const parsed = JSON.parse(rawValue)
        tenantId = parsed?.id || parsed?.tenantId || parsed?.value
        visitTenantId = parsed?.visitTenantId || parsed?.visit_tenant_id
      } catch (_) {
        tenantId = rawValue
      }
    }
  }
  if (token && token.startsWith('"')) token = JSON.parse(token)
  return {
    token: token || runtimeAuth.token,
    tenantId: tenantId || runtimeAuth.tenantId,
    visitTenantId
  }
}

async function authenticatedGet(page, endpoint, params, label) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, `${label} 需要浏览器登录 token`)
  assert.equal(String(tenantId), '1', `${label} 必须在芋道源码 tenant-id=1 下核验，实际 tenant-id=${tenantId}`)
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

async function verifyImportedReportsByApi(page, batchRecordName) {
  const data = await authenticatedGet(
    page,
    '/admin-api/mes/pro/batch-record-report/page',
    { pageNo: 1, pageSize: 100, batchRecordName },
    '导入后批记录报表分页查询'
  )
  const list = data?.list || []
  assert.ok(list.length > 0, `导入后必须能查到批记录报表：${batchRecordName}`)
  assert.ok(
    list.some((item) => String(item.reportName || item.tableTitle || '').replace(/\s+/g, '').includes('产品信息')),
    '导入报表必须包含产品信息固定工序'
  )
  return list
}

async function verifyGeneratedRouteByApi(page, batchRecordName, importResult, importedReports) {
  assert.ok(importResult.routeId, `导入响应必须返回 routeId：${JSON.stringify(importResult)}`)
  assert.ok(importResult.routeCode, `导入响应必须返回 routeCode：${JSON.stringify(importResult)}`)
  assert.equal(importResult.routeName, batchRecordName, '路线名称必须等于批记录名称')
  assert.ok(Number(importResult.routeProcessCount) > 0, '路线工序数必须大于 0')
  assert.equal(
    Number(importResult.batchRecordRouteBindingCount),
    Number(importResult.routeProcessCount),
    '批记录路线绑定数必须等于路线工序数'
  )

  const routePage = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route/page',
    { pageNo: 1, pageSize: 10, name: batchRecordName },
    '导入后路线分页查询'
  )
  const route = (routePage?.list || []).find((item) => String(item.id) === String(importResult.routeId))
  assert.ok(route, `导入后必须能查到生成路线：${JSON.stringify(routePage)}`)
  assert.equal(route.code, importResult.routeCode, '路线编码必须匹配导入响应')
  assert.equal(Number(route.status), 0, '生成路线必须启用')

  const importedReportIds = new Set(importedReports.map((item) => String(item.reportId || item.id)).filter(Boolean))
  const routeProcesses = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route-process/list-by-route',
    { routeId: importResult.routeId },
    '导入后路线工序查询'
  )
  assert.equal(routeProcesses.length, Number(importResult.routeProcessCount), '路线工序数量必须匹配导入响应')
  assert.ok(
    routeProcesses.every((item) => item.processName && !item.processName.replace(/\s+/g, '').includes('产品信息')),
    `路线工序必须去除产品信息固定工序：${JSON.stringify(routeProcesses)}`
  )
  for (const routeProcess of routeProcesses) {
    assert.ok(routeProcess.batchRecordReportId, `路线工序必须绑定报表：${JSON.stringify(routeProcess)}`)
    assert.ok(importedReportIds.has(String(routeProcess.batchRecordReportId)), '路线工序绑定报表必须来自本次导入')
  }

  const useConfigs = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route/flow-config/process-config-list',
    { routeId: importResult.routeId, useType: 'BATCH' },
    '导入后工艺流程批记录配置用途查询'
  )
  let bindingCount = 0
  assert.equal(useConfigs.length, Number(importResult.routeProcessCount), '用途工序数量必须匹配路线工序数')
  for (const useConfig of useConfigs) {
    assert.equal(useConfig.enabled, true, `用途配置必须启用：${JSON.stringify(useConfig)}`)
    assert.equal(useConfig.executionMode, 'SEQUENTIAL', `执行模式必须为 SEQUENTIAL：${JSON.stringify(useConfig)}`)
    const reports = useConfig.batchRecordReports || []
    assert.equal(reports.length, 1, `每个工序必须绑定一个批记录报表：${JSON.stringify(useConfig)}`)
    const report = reports[0]
    assert.equal(report.recordCategory, 'BATCH_RECORD', 'recordCategory 必须为 BATCH_RECORD')
    assert.equal(report.validationProfile, 'CONTROLLED_BATCH', 'validationProfile 必须为 CONTROLLED_BATCH')
    assert.equal(Number(report.reportSort), 1, 'reportSort 必须为 1')
    assert.ok(importedReportIds.has(String(report.batchRecordReportId)), '用途绑定报表必须来自本次导入')
    bindingCount += reports.length
  }
  assert.equal(bindingCount, Number(importResult.batchRecordRouteBindingCount), '用途绑定数量必须匹配导入响应')
  return {
    routeId: importResult.routeId,
    routeCode: importResult.routeCode,
    routeProcessCount: routeProcesses.length,
    batchRecordRouteBindingCount: bindingCount
  }
}

async function importWordTemplateByUi(page) {
  await openTemplatePage(page)
  const fileChooserPromise = page.waitForEvent('filechooser', { timeout: 30000 })
  await clickFirstEnabled(page.getByRole('button', { name: /导入 Word/ }), '导入 Word')
  const fileChooser = await fileChooserPromise
  await fileChooser.setFiles(SAMPLE_DOC_PATH)

  const prompt = page.getByRole('dialog').filter({ hasText: '请输入批记录名称' }).first()
  await prompt.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(prompt.locator('input'), BATCH_RECORD_NAME, '批记录名称')

  const uploadResponsePromise = waitForBusinessResponse(
    page,
    '/admin-api/mes/pro/batch-record-report/recognize-uploaded',
    'Word 导入识别保存',
    'POST',
    600000
  )
  await clickFirstEnabled(prompt.getByRole('button', { name: /^确定$/ }), '确认批记录名称')
  const importResult = await uploadResponsePromise
  await page.getByText(BATCH_RECORD_NAME).first().waitFor({ state: 'visible', timeout: 60000 })
  const importedReports = await verifyImportedReportsByApi(page, BATCH_RECORD_NAME)
  const generatedRoute = await verifyGeneratedRouteByApi(page, BATCH_RECORD_NAME, importResult, importedReports)
  return { batchRecordName: BATCH_RECORD_NAME, importedReports, generatedRoute }
}

async function main() {
  assertLocalAdminRun()
  const launchOptions = { headless: process.env.EDHR_WORD_IMPORT_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  try {
    await login(page)
    const { batchRecordName, importedReports, generatedRoute } = await importWordTemplateByUi(page)
    console.log(
      `PASS: admin eDHR Word import batchRecordName=${batchRecordName} tenant=芋道源码 username=admin reports=${importedReports.length} routeId=${generatedRoute.routeId} routeCode=${generatedRoute.routeCode} routeProcesses=${generatedRoute.routeProcessCount} batchBindings=${generatedRoute.batchRecordRouteBindingCount}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
