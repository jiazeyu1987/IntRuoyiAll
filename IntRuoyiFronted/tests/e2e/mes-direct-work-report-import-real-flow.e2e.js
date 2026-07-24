const assert = require('node:assert/strict')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(`Playwright is required for direct work report import E2E: ${error.message}`)
  }
}

const config = {
  baseUrl: (process.env.MES_DIRECT_WORK_REPORT_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_DIRECT_WORK_REPORT_E2E_TENANT || '测试租户',
  username: process.env.MES_DIRECT_WORK_REPORT_E2E_USERNAME || 'aoteman',
  password: process.env.MES_DIRECT_WORK_REPORT_E2E_PASSWORD || '111111',
  uploadFile:
    process.env.MES_DIRECT_WORK_REPORT_E2E_UPLOAD_FILE || 'C:\\Users\\BJB110\\Desktop\\文档\\李萍.xlsx',
  headed: process.env.MES_DIRECT_WORK_REPORT_E2E_HEADED === '1',
  browserPath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
}

let latestAuthHeaders = null
const NON_INTERSECTION_DIRECT_WORK_REPORT_WARNING_CODES = new Set([
  'WORK_ORDER_NOT_FOUND',
  'WORK_ORDER_NOT_UNIQUE',
  'SCHEDULE_ORDER_NOT_FOUND',
  'SCHEDULE_ORDER_NOT_UNIQUE',
  'PROCESS_NOT_FOUND',
  'PROCESS_NOT_ENABLED',
  'PROCESS_NOT_UNIQUE'
])

function hasDirectWorkReportText(value) {
  return String(value ?? '').trim().length > 0
}

function isVisibleDirectWorkReportDetail(row) {
  return hasDirectWorkReportText(row?.workOrderCode) && hasDirectWorkReportText(row?.scheduleOrderCode)
}

function isVisibleDirectWorkReportWarning(row) {
  if (!hasDirectWorkReportText(row?.workOrderCode) || !hasDirectWorkReportText(row?.scheduleOrderCode)) {
    return false
  }
  if (NON_INTERSECTION_DIRECT_WORK_REPORT_WARNING_CODES.has(String(row?.reasonCode ?? '').trim())) {
    return false
  }
  return hasDirectWorkReportText(row?.processCode)
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
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
  throw new Error(`No visible input found for ${label}`)
}

function normalizeCacheValue(value) {
  if (value === undefined || value === null) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  if (typeof value === 'object') {
    if (Object.prototype.hasOwnProperty.call(value, 'value')) return normalizeCacheValue(value.value)
    if (Object.prototype.hasOwnProperty.call(value, 'data')) return normalizeCacheValue(value.data)
  }
  return ''
}

function readWsCacheValue(snapshot, key) {
  const candidates = [key, `vueuse_${key}`, `pro__${key}`, `yudao__${key}`]
  for (const candidate of candidates) {
    const raw = snapshot[candidate]
    if (!raw) continue
    try {
      const parsed = JSON.parse(raw)
      const normalized = normalizeCacheValue(parsed)
      if (normalized) return normalized
    } catch {
      const normalized = normalizeCacheValue(raw)
      if (normalized) return normalized
    }
  }
  return ''
}

async function buildAuthHeaders(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const accessToken = readWsCacheValue(snapshot, 'ACCESS_TOKEN')
  const tenantId = readWsCacheValue(snapshot, 'tenantId')
  const visitTenantId = readWsCacheValue(snapshot, 'visitTenantId')
  assert.ok(accessToken, 'ACCESS_TOKEN is missing after login')
  assert.ok(tenantId, 'tenantId is missing after login')
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId)
  }
  if (visitTenantId) {
    headers['visit-tenant-id'] = String(visitTenantId)
  }
  return headers
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[type="password"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `登录 HTTP 失败: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginBody.code), `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openFeedbackPage(page) {
  await page.goto(`${config.baseUrl}/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByRole('button', { name: /第三方导入/ }).waitFor({ state: 'visible', timeout: 30000 })
}

async function importDirectWorkReport(page) {
  await page.getByRole('button', { name: /第三方导入/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入报工' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await dialog.locator('.el-radio-group').count(), 0, '导入报工弹窗不应显示导入类型切换。')
  assert.equal(await dialog.locator('.el-upload__tip').count(), 0, '导入报工弹窗不应显示底部格式提示。')
  await dialog.locator('input[type="file"]').setInputFiles(path.resolve(config.uploadFile))

  const [importResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/feedback/import-direct-work-report-xlsx') &&
        response.request().method() === 'POST',
      { timeout: 120000 }
    ),
    dialog.getByRole('button', { name: /确 定/ }).click()
  ])
  const importRequestHeaders = importResponse.request().headers()
  const authorization = importRequestHeaders.authorization || importRequestHeaders.Authorization || ''
  const tenantId = importRequestHeaders['tenant-id'] || importRequestHeaders['Tenant-Id'] || ''
  assert.ok(authorization, '导入请求必须携带 Authorization 请求头，后置回查需复用真实登录态。')
  assert.ok(tenantId, '导入请求必须携带 tenant-id 请求头，后置回查需复用真实租户。')
  latestAuthHeaders = {
    Authorization: authorization,
    'tenant-id': tenantId
  }
  const visitTenantId = importRequestHeaders['visit-tenant-id'] || importRequestHeaders['Visit-Tenant-Id'] || ''
  if (visitTenantId) {
    latestAuthHeaders['visit-tenant-id'] = visitTenantId
  }
  assert.ok(importResponse.ok(), `李萍报工单导入 HTTP 失败: ${importResponse.status()}`)

  const importBody = await importResponse.json()
  assert.ok([0, 200].includes(importBody.code), `李萍报工单导入失败: ${importBody.msg || importBody.code}`)
  const data = importBody.data || {}
  assert.equal(Number(data.pendingCount || 0), 0, `直接报工进度导入不应产生待归属记录: ${JSON.stringify(data)}`)
  const submittedCount = Number(data.submittedCount || 0)
  assert.ok(submittedCount === 0, `直接报工进度导入不提交审批: ${JSON.stringify(data)}`)
  assert.ok(Array.isArray(data.feedbackCodes) && data.feedbackCodes.length === 0, `直接报工进度导入不返回报工单号: ${JSON.stringify(data)}`)
  assert.ok(Array.isArray(data.importRecordIds) && data.importRecordIds.length === data.importedCount, `必须按更新进度行数返回来源记录: ${JSON.stringify(data)}`)

  const resultDialog = page.locator('.el-dialog:visible').filter({ hasText: '直接报工导入结果' }).last()
  await resultDialog.waitFor({ state: 'visible', timeout: 30000 })
  const resultText = await resultDialog.textContent()
  assert.ok(!resultText.includes('工作表数'), `导入结果弹框顶部不应展示工作表数统计: ${resultText}`)
  assert.ok(!resultText.includes('创建报工数'), `导入结果弹框顶部不应展示创建报工数统计: ${resultText}`)
  assert.ok(!resultText.includes('提交审批数'), `导入结果弹框顶部不应展示提交审批数统计: ${resultText}`)
  assert.ok(!resultText.includes('跳过杂务行'), `导入结果弹框顶部不应展示跳过杂务行统计: ${resultText}`)
  assert.ok(!resultText.includes('未创建提示'), `导入结果弹框顶部不应展示未创建提示表格: ${resultText}`)
  assert.ok(!resultText.includes('报工单号'), `直接报工进度导入结果不应展示报工单号主语义: ${resultText}`)
  assert.ok(!resultText.includes('导入记录数'), `直接报工导入结果大弹框不应展示导入记录数: ${resultText}`)
  assert.ok(!resultText.includes('待归属数'), `直接报工导入结果大弹框不应展示待归属数: ${resultText}`)
  assert.ok(!resultText.includes('状态 / 原因'), `直接报工结果明细不应展示跳过行状态和原因: ${resultText}`)
  const detailRows = Array.isArray(data.directWorkReportDetails) ? data.directWorkReportDetails : []
  const skipWarnings = Array.isArray(data.directWorkReportSkipWarnings) ? data.directWorkReportSkipWarnings : []
  const visibleDetails = detailRows.filter(isVisibleDirectWorkReportDetail)
  const visibleSkipWarnings = skipWarnings.filter(isVisibleDirectWorkReportWarning)
  const updatedWorkOrderCodes = [
    ...new Set(
      visibleDetails
        .map((row) => String(row.workOrderCode || '').trim())
        .filter(Boolean)
    )
  ]
  const skippedWorkOrderCodes = [
    ...new Set(
      visibleSkipWarnings
        .map((row) => String(row.workOrderCode || '').trim())
        .filter(Boolean)
    )
  ]
  const locatedWorkOrderCodes = [...new Set([...updatedWorkOrderCodes, ...skippedWorkOrderCodes])]
  const visibleWorkOrderCodes = locatedWorkOrderCodes
  const cardTexts = await resultDialog.locator('.direct-import-result__work-order-card').allTextContents()
  if (visibleWorkOrderCodes.length > 0) {
    assert.equal(
      cardTexts.length,
      visibleWorkOrderCodes.length,
      `左侧生产工单卡片数量必须等于可展示交集生产工单集合: ${JSON.stringify({
        updatedWorkOrderCodes,
        skippedWorkOrderCodes,
        visibleWorkOrderCodes,
        hiddenWarningCodes: skipWarnings
          .filter((row) => !isVisibleDirectWorkReportWarning(row))
          .map((row) => `${row.workOrderCode || ''}:${row.reasonCode || ''}`),
        cardTexts
      })}`
    )
    for (const workOrderCode of visibleWorkOrderCodes) {
      assert.ok(
        cardTexts.some((text) => text.includes(workOrderCode)),
        `左侧生产工单列表必须包含可展示交集生产工单 ${workOrderCode}: ${JSON.stringify(cardTexts)}`
      )
    }
  } else {
    assert.equal(
      cardTexts.length,
      0,
      `没有可定位生产工单时，左侧生产工单列表必须为空: ${JSON.stringify({
        cardTexts
      })}`
    )
  }
  if (visibleDetails.length > 0 || visibleSkipWarnings.length > 0) {
    assert.ok(resultText.includes('更新结果'), `导入定位到工单后必须按工单分组展示更新结果: ${resultText}`)
    assert.ok(resultText.includes('工序 / 产线'), `导入定位到工单后必须展示工序/产线明细: ${resultText}`)
    assert.ok(resultText.includes('本次报工') || resultText.includes('本次完成'), `导入定位到工单后必须展示报工数量: ${resultText}`)
  } else {
    assert.ok(resultText.includes('本次导入未更新排产进度'), `未更新进度时必须展示空明细提示: ${resultText}`)
  }
  await resultDialog.getByRole('button', { name: /确 定|确定/ }).click().catch(() => {})
  return data
}

async function readJson(page, url, params = {}) {
  const headers = latestAuthHeaders || (await buildAuthHeaders(page))
  const payload = await page.evaluate(
    async ({ url, params, headers }) => {
      const query = new URLSearchParams()
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
      })
      const response = await fetch(`/admin-api${url}${query.toString() ? `?${query.toString()}` : ''}`, {
        credentials: 'include',
        headers
      })
      const body = await response.json()
      return { status: response.status, body }
    },
    { url, params, headers }
  )
  assert.equal(payload.status, 200, `${url} HTTP must be 200`)
  assert.ok([0, 200].includes(payload.body.code), `${url} business code must succeed: ${JSON.stringify(payload.body)}`)
  return payload.body.data
}

function buildProgressSnapshot(importResult, field) {
  const snapshot = new Map()
  for (const detail of importResult.directWorkReportDetails || []) {
    if (!isVisibleDirectWorkReportDetail(detail)) continue
    const key = `${detail.scheduleOrderCode || ''}|${detail.processCode || ''}`
    const current = Number(snapshot.get(key) || 0)
    const value = Number(detail[field] || 0)
    snapshot.set(key, Math.max(current, value))
  }
  return snapshot
}

function assertRepeatedImportProgressAccumulated(firstImport, secondImport) {
  const progressBefore = buildProgressSnapshot(firstImport, 'beforeReportedQuantity')
  const progressAfterFirstImport = buildProgressSnapshot(firstImport, 'afterReportedQuantity')
  const progressAfterSecondImport = buildProgressSnapshot(secondImport, 'afterReportedQuantity')
  assert.ok(progressAfterFirstImport.size > 0, '首次导入必须至少更新一个排产工序进度')
  for (const [key, firstAfter] of progressAfterFirstImport.entries()) {
    const secondAfter = progressAfterSecondImport.get(key)
    assert.ok(
      Number(secondAfter || 0) > Number(firstAfter || 0),
      `第二次导入必须继续累计排产进度: ${key}, first=${firstAfter}, second=${secondAfter}`
    )
  }
  return {
    progressBefore: Object.fromEntries(progressBefore),
    progressAfterFirstImport: Object.fromEntries(progressAfterFirstImport),
    progressAfterSecondImport: Object.fromEntries(progressAfterSecondImport)
  }
}

async function main() {
  const { chromium } = loadPlaywright()
  const launchOptions = {
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  }
  if (config.browserPath) {
    launchOptions.executablePath = config.browserPath
  }
  const browser = await chromium.launch(launchOptions)
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(page)
    await openFeedbackPage(page)
    const importResult = await importDirectWorkReport(page)
    const repeatedImportResult = await importDirectWorkReport(page)
    const progressSnapshots = assertRepeatedImportProgressAccumulated(importResult, repeatedImportResult)
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          username: config.username,
          uploadFile: config.uploadFile,
          importedCount: importResult?.importedCount,
          submittedCount: importResult?.submittedCount,
          secondSubmittedCount: repeatedImportResult?.submittedCount,
          skippedRows: importResult?.skippedRows || 0,
          locatedWorkOrderCodes: [
            ...new Set(
              [
                ...(importResult?.directWorkReportDetails || []),
                ...(importResult?.directWorkReportSkipWarnings || [])
              ]
                .map((row) => String(row.workOrderCode || '').trim())
                .filter(Boolean)
            )
          ],
          visibleWorkOrderCodes: [
            ...new Set(
              [
                ...(importResult?.directWorkReportDetails || []).filter(isVisibleDirectWorkReportDetail),
                ...(importResult?.directWorkReportSkipWarnings || []).filter(isVisibleDirectWorkReportWarning)
              ]
                .map((row) => String(row.workOrderCode || '').trim())
                .filter(Boolean)
            )
          ],
          feedbackCodes: importResult?.feedbackCodes || [],
          secondFeedbackCodes: repeatedImportResult?.feedbackCodes || [],
          importRecordIds: importResult?.importRecordIds || [],
          secondImportRecordIds: repeatedImportResult?.importRecordIds || [],
          ...progressSnapshots
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
