const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const BASE_URL = (process.env.EDHR_WORD_IMPORT_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_WORD_IMPORT_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const TEST_TENANT = process.env.EDHR_WORD_IMPORT_TEST_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_WORD_IMPORT_TEST_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_WORD_IMPORT_TEST_PASSWORD || '111111'
const SAMPLE_DOC_PATH = process.env.EDHR_WORD_IMPORT_SAMPLE_DOC || path.join(WORKSPACE_ROOT, 'resource', '批记录模板.doc')
const SAMPLE_DOC_NAME = path.basename(SAMPLE_DOC_PATH)
const PRODUCT_NAME_KEYWORD = process.env.EDHR_WORD_IMPORT_PRODUCT_NAME || '球囊扩张压力泵'
const ROUTE = '/mes/pro/batch-record-form-list'
const ROUTE_KEY = 'B'
const RUN_ID = process.env.EDHR_WORD_IMPORT_RUN_ID || String(Date.now())

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'Word 导入 E2E 必须固定使用本机前端 http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'Word 导入 E2E 必须固定使用本机后端 48081')
  assert.equal(TEST_TENANT, '测试租户', 'Word 导入写入验证必须使用测试租户')
  assert.equal(TEST_USERNAME, 'aoteman', 'Word 导入写入验证必须使用测试租户 aoteman')
  assert.ok(fs.existsSync(SAMPLE_DOC_PATH), `缺少真实 Word 模板样本：${SAMPLE_DOC_PATH}`)
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

function assertBusinessSuccess(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 必须返回 JSON 对象`)
  const code = Number(body.code)
  assert.ok([0, 200].includes(code), `${label} 业务响应失败：${body.msg || body.message || body.code}`)
  return body.data
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

async function unwrapBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    throw new Error(`${label} 等待失败：${result.__error.message}`)
  }
  return result
}

async function maybeBusinessWait(promise, label) {
  const result = await promise
  if (result && result.__error) {
    console.log(`WARN: ${label} 未捕获到响应：${result.__error.message}`)
    return undefined
  }
  return result
}

async function selectedWordImportProductTags(page) {
  return await page.evaluate(() =>
    Array.from(document.querySelectorAll('.el-dialog .el-select__tags .el-tag, .el-dialog .el-select__tags-text'))
      .map((item) => item.textContent?.replace(/\s+/g, ' ').trim())
      .filter(Boolean)
  )
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(ROUTE)}`, {
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
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), TEST_USERNAME, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), TEST_PASSWORD, '密码')
  await clickFirstEnabled(loginForm.getByRole('button', { name: /^登录$/ }), '登录按钮')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openTemplatePage(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批记录名称').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('button', { name: /导入 Word/ }).first().waitFor({ state: 'visible', timeout: 60000 })
}

async function resolveBatchRecordName(page) {
  if (process.env.EDHR_WORD_IMPORT_BATCH_RECORD_NAME) {
    return process.env.EDHR_WORD_IMPORT_BATCH_RECORD_NAME
  }
  const currentPageName = await findExistingSampleBatchRecordName(page)
  if (currentPageName) return currentPageName

  const searchInput = page.locator('.batch-record-toolbar-search input').first()
  if ((await searchInput.count()) > 0 && (await searchInput.isVisible())) {
    const searchResponsePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/batch-record-report/page') &&
          response.status() === 200,
        { timeout: 60000 }
      )
      .catch(() => null)
    await searchInput.fill('E2E-WORD')
    await searchInput.press('Enter')
    await searchResponsePromise
    const searchedName = await findExistingSampleBatchRecordName(page)
    if (searchedName) return searchedName
  }

  return `E2E-WORD-${RUN_ID}`
}

async function findExistingSampleBatchRecordName(page) {
  const selectedRecordName = await page
    .locator('.batch-record-record-list__item.is-active .batch-record-record-list__name')
    .first()
    .innerText()
    .catch(() => '')

  const rows = page.locator('.batch-record-table .el-table__body-wrapper tbody tr, .batch-record-table .el-table__row')
  const rowCount = await rows.count()
  for (let index = 0; index < rowCount; index += 1) {
    const row = rows.nth(index)
    const text = await row.innerText().catch(() => '')
    if (!text.includes(SAMPLE_DOC_NAME)) continue
    const batchRecordName = selectedRecordName.trim()
    if (batchRecordName) {
      return batchRecordName
    }
  }
  return ''
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

async function verifyImportedReportsByApi(page, batchRecordName) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, '最终 API 核验需要浏览器登录 token')
  assert.ok(tenantId, '最终 API 核验需要 tenant-id')
  const response = await page.request.get(`${BACKEND_URL}/admin-api/mes/pro/batch-record-report/page`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    params: {
      pageNo: 1,
      pageSize: 50,
      routeKey: ROUTE_KEY,
      batchRecordName
    }
  })
  assert.equal(response.status(), 200, '导入后报表分页查询 HTTP 必须为 200')
  const data = assertBusinessSuccess(await response.json(), '导入后报表分页查询')
  const list = data?.list || []
  assert.ok(list.length > 0, `导入后必须能按批记录名称查询到报表：${batchRecordName}`)
  assert.ok(
    list.every((item) => item.batchRecordName === batchRecordName && item.routeKey === ROUTE_KEY),
    `导入后报表必须全部属于 ${ROUTE_KEY}/${batchRecordName}：${JSON.stringify(list)}`
  )
  return list
}

async function verifyAutomaticCellRulesByApi(page, importedReports) {
  const reports = importedReports.filter((item) => item.reportId)
  assert.ok(reports.length > 0, '自动规则核验需要至少一份导入报表 reportId')
  const summary = {
    reportCount: reports.length,
    totalSuggestions: 0,
    totalPersistedAutoRules: 0,
    valueTypes: new Set(),
    timePointLabels: []
  }

  for (const report of reports) {
    const cellRules = await authenticatedGet(
      page,
      '/admin-api/mes/pro/batch-record-report/cell-rules',
      { reportId: report.reportId },
      `导入后单元格规则查询 ${report.reportName || report.reportId}`
    )
    const suggestions = cellRules?.suggestions || []
    summary.totalSuggestions += suggestions.length
    for (const suggestion of suggestions) {
      if (suggestion?.valueType) summary.valueTypes.add(suggestion.valueType)
      assert.equal(suggestion.source, 'AUTO', `自动候选 source 必须为 AUTO：${JSON.stringify(suggestion)}`)
    }

    const sheetLayout = JSON.parse(cellRules.sheetLayoutJson || '{}')
    const rows = sheetLayout.rows || {}
    for (const row of Object.values(rows)) {
      const cells = row?.cells || {}
      for (const cell of Object.values(cells)) {
        const rule = cell?.edhrCellRule
        if (!rule || rule.source !== 'AUTO') continue
        summary.totalPersistedAutoRules += 1
        assert.equal(rule.reviewed, false, `自动规则不能标记为人工确认：${JSON.stringify(rule)}`)
        assert.ok(rule.valueType, `自动规则必须有 valueType：${JSON.stringify(rule)}`)
        if (rule.valueType) summary.valueTypes.add(rule.valueType)
        const label = String(rule.label || '')
        if (/操作时间|记录时间|检验时间|审核时间|开始时间|结束时间|完成时间|发生时间/.test(label)) {
          summary.timePointLabels.push({ label, valueType: rule.valueType })
          assert.equal(rule.valueType, 'DATETIME', `时间点字段必须识别为 DATETIME：${JSON.stringify(rule)}`)
        }
      }
    }
  }

  assert.ok(summary.totalSuggestions > 0, '导入后 cell-rules 必须返回自动规则候选')
  assert.ok(summary.totalPersistedAutoRules > 0, '导入后报表 JSON 必须持久化 AUTO edhrCellRule')
  assert.ok(summary.valueTypes.has('NUMBER') || summary.valueTypes.has('BOOLEAN') || summary.valueTypes.has('DATE'),
    `自动规则必须至少识别一种强类型：${JSON.stringify([...summary.valueTypes])}`)
  return {
    ...summary,
    valueTypes: [...summary.valueTypes].sort()
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

async function resolveProductNamesByApi(page) {
  const options = await authenticatedGet(
    page,
    '/admin-api/mes/pro/work-order/product-name-options',
    { keyword: PRODUCT_NAME_KEYWORD },
    '生产工单产品名称候选查询'
  )
  assert.ok(Array.isArray(options), `生产工单产品名称候选必须返回数组：${JSON.stringify(options)}`)
  const matched = options
    .map((item) => String(item || '').trim())
    .filter((item) => item && item.includes(PRODUCT_NAME_KEYWORD))
  assert.ok(
    matched.length > 0,
    `生产工单中必须存在包含“${PRODUCT_NAME_KEYWORD}”的产品名称候选：${JSON.stringify(options)}`
  )
  return [matched.find((item) => item === PRODUCT_NAME_KEYWORD) || matched[0]]
}

async function verifyGeneratedRouteByApi(page, batchRecordName, importResult, importedReports, productNames) {
  assert.ok(importResult, '导入响应必须包含生成结果')
  assert.ok(importResult.routeId, `导入响应必须返回 routeId：${JSON.stringify(importResult)}`)
  assert.ok(importResult.routeCode, `导入响应必须返回 routeCode：${JSON.stringify(importResult)}`)
  assert.equal(importResult.routeName, batchRecordName, '导入生成的路线名称必须等于批记录名称')
  assert.ok(Number(importResult.routeProcessCount) > 0, '导入响应必须返回大于 0 的路线工序数')
  assert.ok(Number(importResult.boundProductNameCount) > 0, '导入响应必须返回大于 0 的绑定产品名称数')
  assert.ok(Number(importResult.boundProductCodeCount) > 0, '导入响应必须返回大于 0 的绑定产品编码数')
  assert.ok(
    Number(importResult.boundProductNameCount) <= productNames.length,
    `绑定产品名称数不能超过提交产品名称数：${JSON.stringify(importResult)}`
  )
  assert.ok(
    Array.isArray(importResult.skippedProductNames || []),
    `导入响应 skippedProductNames 必须为数组或空：${JSON.stringify(importResult)}`
  )
  assert.equal(
    Number(importResult.batchRecordRouteBindingCount),
    Number(importResult.routeProcessCount),
    '导入响应中的批记录路线绑定数必须等于路线工序数'
  )

  const routePage = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route/page',
    { pageNo: 1, pageSize: 10, name: batchRecordName },
    '导入后路线分页查询'
  )
  const routes = routePage?.list || []
  const route = routes.find(
    (item) => String(item.id) === String(importResult.routeId) && item.code === importResult.routeCode
  )
  assert.ok(route, `导入后必须能按批记录名称查询到生成路线：${JSON.stringify(routePage)}`)
  assert.equal(route.name, batchRecordName, '生成路线名称必须等于批记录名称')
  assert.equal(Number(route.status), 0, '生成路线必须为启用状态')

  const routeProcesses = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route-process/list-by-route',
    { routeId: importResult.routeId },
    '导入后路线工序查询'
  )
  assert.equal(
    routeProcesses.length,
    Number(importResult.routeProcessCount),
    '路线工序数量必须等于导入响应 routeProcessCount'
  )
  assert.ok(routeProcesses.every((item) => item.processName && !item.processName.replace(/\s+/g, '').includes('产品信息')),
    `路线工序必须去除产品信息固定工序：${JSON.stringify(routeProcesses)}`)
  const importedReportIds = new Set(importedReports.map((item) => String(item.reportId || item.id)).filter(Boolean))
  assert.ok(importedReportIds.size > 0, `本次导入报表必须返回可核验的 reportId：${JSON.stringify(importedReports)}`)

  const useConfigs = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route/flow-config/process-config-list',
    { routeId: importResult.routeId, useType: 'BATCH' },
    '导入后工艺流程批记录配置用途查询'
  )
  assert.equal(
    useConfigs.length,
    Number(importResult.routeProcessCount),
    '工艺流程批记录配置用途工序数量必须等于导入响应 routeProcessCount'
  )
  let bindingCount = 0
  for (const useConfig of useConfigs) {
    assert.equal(useConfig.enabled, true, `工艺流程批记录配置用途必须启用：${JSON.stringify(useConfig)}`)
    assert.equal(useConfig.executionMode, 'SEQUENTIAL', `批记录执行模式必须为 SEQUENTIAL：${JSON.stringify(useConfig)}`)
    const reports = useConfig.batchRecordReports || []
    assert.equal(reports.length, 1, `每个路线工序必须绑定 1 个批记录报表：${JSON.stringify(useConfig)}`)
    const report = reports[0]
    assert.equal(report.recordCategory, 'BATCH_RECORD', `绑定记录类型必须为 BATCH_RECORD：${JSON.stringify(report)}`)
    assert.equal(report.validationProfile, 'CONTROLLED_BATCH', `绑定校验策略必须为 CONTROLLED_BATCH：${JSON.stringify(report)}`)
    assert.equal(Number(report.reportSort), 1, `绑定 reportSort 必须为 1：${JSON.stringify(report)}`)
    assert.ok(
      importedReportIds.has(String(report.batchRecordReportId)),
      `用途绑定报表必须来自本次导入：${JSON.stringify(report)}`
    )
    bindingCount += reports.length
  }
  assert.equal(
    bindingCount,
    Number(importResult.batchRecordRouteBindingCount),
    '用途绑定数量必须等于导入响应 batchRecordRouteBindingCount'
  )

  const routeProducts = await authenticatedGet(
    page,
    '/admin-api/mes/pro/route-product/list-by-route',
    { routeId: importResult.routeId },
    '导入后工艺路线对应产品查询'
  )
  assert.equal(
    routeProducts.length,
    Number(importResult.boundProductCodeCount),
    '路线对应产品数量必须等于导入响应 boundProductCodeCount'
  )
  assert.ok(
    routeProducts.every((item) => item.itemCode && item.itemName),
    `路线对应产品必须返回产品编码和产品名称：${JSON.stringify(routeProducts)}`
  )
  const routeProductNames = new Set(routeProducts.map((item) => String(item.itemName || '').trim()))
  const skippedProductNames = new Set((importResult.skippedProductNames || []).map((item) => String(item || '').trim()))
  for (const productName of productNames) {
    if (skippedProductNames.has(productName)) continue
    assert.ok(
      routeProductNames.has(productName),
      `路线对应产品必须包含选中且未跳过的产品名称：${productName} / ${JSON.stringify(routeProducts)}`
    )
  }

  return {
    routeId: importResult.routeId,
    routeCode: importResult.routeCode,
    routeProcessCount: routeProcesses.length,
    batchRecordRouteBindingCount: bindingCount,
    boundProductCodeCount: routeProducts.length
  }
}

async function importWordTemplateByUi(page) {
  await openTemplatePage(page)
  const batchRecordName = await resolveBatchRecordName(page)
  const productNames = await resolveProductNamesByApi(page)
  const fileChooserPromise = page.waitForEvent('filechooser', { timeout: 30000 })
  await clickFirstEnabled(page.getByRole('button', { name: /导入 Word/ }), '导入 Word')
  const fileChooser = await fileChooserPromise
  await fileChooser.setFiles(SAMPLE_DOC_PATH)

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入 Word' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(dialog.locator('.el-form-item').filter({ hasText: '批记录名称' }).locator('input'), batchRecordName, '批记录名称')
  const productSelect = dialog.locator('.el-form-item').filter({ hasText: '工艺路线对应产品名称' }).locator('.el-select').first()
  for (const productName of productNames) {
    await productSelect.click()
    const productInput = productSelect.locator('input:visible').first()
    const optionResponse = page
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/work-order/product-name-options') &&
          response.status() === 200,
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
      await option.scrollIntoViewIfNeeded()
      await option.click({ force: true })
    }
    await page.waitForFunction(
      (name) =>
        Array.from(document.querySelectorAll('.el-dialog .el-select__tags .el-tag, .el-dialog .el-select__tags-text'))
          .some((item) => (item.textContent || '').includes(name)),
      productName,
      { timeout: 10000 }
    )
  }
  const selectedProductTags = await selectedWordImportProductTags(page)
  assert.ok(
    selectedProductTags.length > 0,
    `导入确认前必须已选择至少一个工艺路线对应产品：${JSON.stringify(selectedProductTags)}`
  )
  const selectedProductNames = selectedProductTags.map((item) => item.replace(/\s*×\s*$/, '').trim()).filter(Boolean)
  assert.ok(
    selectedProductNames.length > 0,
    `导入确认前必须能解析出已选产品名称：${JSON.stringify(selectedProductTags)}`
  )
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
  const successText = new RegExp(`批记录名称「${escapeRegExp(batchRecordName)}」路线 B 解析完成`)
  await page.getByText(successText).first().waitFor({ state: 'visible', timeout: 600000 })
  await page.getByText(batchRecordName).first().waitFor({ state: 'visible', timeout: 60000 })
  const importedReports = await verifyImportedReportsByApi(page, batchRecordName)
  const automaticCellRules = await verifyAutomaticCellRulesByApi(page, importedReports)
  const generatedRoute = await verifyGeneratedRouteByApi(page, batchRecordName, importResult, importedReports, selectedProductNames)
  return { batchRecordName, importedReports, generatedRoute, automaticCellRules, productNames: selectedProductNames, upgraded: existed === true }
}

async function main() {
  assertLocalOnly()
  const launchOptions = { headless: process.env.EDHR_WORD_IMPORT_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page)
    const { batchRecordName, importedReports, generatedRoute, automaticCellRules, productNames, upgraded } = await importWordTemplateByUi(page)
    console.log(
      `PASS: eDHR Word template import real E2E batchRecordName=${batchRecordName} route=${ROUTE_KEY} upgraded=${upgraded} reports=${importedReports.length} autoSuggestions=${automaticCellRules.totalSuggestions} persistedAutoRules=${automaticCellRules.totalPersistedAutoRules} valueTypes=${automaticCellRules.valueTypes.join(',')} routeId=${generatedRoute.routeId} routeCode=${generatedRoute.routeCode} routeProcesses=${generatedRoute.routeProcessCount} batchBindings=${generatedRoute.batchRecordRouteBindingCount} productNames=${productNames.length} boundProductCodes=${generatedRoute.boundProductCodeCount}`
    )
  } catch (error) {
    const outputDir = path.join(__dirname, 'output', 'edhr-word-import-product-binding')
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
