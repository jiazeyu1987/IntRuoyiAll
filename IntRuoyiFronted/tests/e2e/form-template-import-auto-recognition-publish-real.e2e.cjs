const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.FORM_TEMPLATE_IMPORT_E2E_BASE_URL || 'http://127.0.0.1:8081',
  tenant: process.env.FORM_TEMPLATE_IMPORT_E2E_TENANT || '芋道源码',
  username: process.env.FORM_TEMPLATE_IMPORT_E2E_USERNAME || 'admin',
  password: process.env.FORM_TEMPLATE_IMPORT_E2E_PASSWORD || '',
  templateName: process.env.FORM_TEMPLATE_IMPORT_E2E_TEMPLATE || '按压式压力泵过程检验记录',
  filePath:
    process.env.FORM_TEMPLATE_IMPORT_E2E_DOCX ||
    'E:\\IntRuoyi\\resource\\按压式球囊扩充压力泵IDI-001\\过程检验记录.docx',
  timeout: Number(process.env.FORM_TEMPLATE_IMPORT_E2E_TIMEOUT_MS || 180000),
  headed: process.env.FORM_TEMPLATE_IMPORT_E2E_HEADED === '1'
}

if (!config.password) {
  throw new Error('FORM_TEMPLATE_IMPORT_E2E_PASSWORD is required for the real login path')
}
if (!fs.existsSync(config.filePath)) {
  throw new Error(`Word source file is missing: ${config.filePath}`)
}

async function settle(page) {
  await page.waitForLoadState('domcontentloaded', { timeout: config.timeout }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.isVisible().catch(() => false)) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function fillFirstVisible(scope, selectors, value) {
  for (const selector of selectors) {
    const locator = scope.locator(selector).first()
    if (await locator.isVisible().catch(() => false)) {
      await locator.fill(String(value))
      return
    }
  }
  throw new Error(`找不到可填写控件：${selectors.join(', ')}`)
}

async function login(page) {
  const url = new URL('/login', config.baseUrl)
  url.searchParams.set('redirect', '/index')
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form)
  await fillFirstVisible(
    form,
    [
      'input[placeholder="请输入用户名"]',
      'input[placeholder*="用户名"]',
      'input[placeholder*="账号"]',
      'input.el-input__inner:not([type="password"]):not([role="combobox"])'
    ],
    config.username
  )
  await fillFirstVisible(
    form,
    ['input[type="password"]', 'input[placeholder="请输入密码"]', 'input[placeholder*="密码"]'],
    config.password
  )
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  const permissionResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
      { timeout: config.timeout }
    )
    .catch(() => null)
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok(loginPayload && [0, 200].includes(loginPayload.code), `login_business_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.href.includes('/login'), { timeout: config.timeout })
  await permissionResponsePromise
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: config.timeout })
  await settle(page)
}

function readWsCacheValue(snapshot, key) {
  const raw = snapshot[key]
  if (!raw) return ''
  const normalizeString = (value) => {
    let current = value || ''
    for (let index = 0; index < 3; index += 1) {
      const trimmed = String(current).trim()
      if (!trimmed.startsWith('"')) return trimmed
      try {
        const parsed = JSON.parse(trimmed)
        if (typeof parsed !== 'string' || parsed === current) return trimmed.replace(/^"(.*)"$/, '$1')
        current = parsed
      } catch {
        return trimmed.replace(/^"(.*)"$/, '$1')
      }
    }
    return String(current).trim()
  }
  const unwrap = (value) => {
    let current = value
    for (let index = 0; index < 6; index += 1) {
      if (!current || typeof current !== 'object') return typeof current === 'string' ? normalizeString(current) : current || ''
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
      return current
    }
    return current || ''
  }
  try {
    return unwrap(JSON.parse(raw))
  } catch {
    return normalizeString(raw)
  }
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
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache'
  }
  if (visitTenantId) headers['visit-tenant-id'] = String(visitTenantId)
  return headers
}

async function requestJson(page, headers, relativePath) {
  return await page.evaluate(
    async ({ requestUrl, requestHeaders }) => {
      const response = await fetch(requestUrl, {
        method: 'GET',
        headers: requestHeaders
      })
      const text = await response.text()
      let payload = null
      try {
        payload = text ? JSON.parse(text) : null
      } catch {
        payload = { rawText: text }
      }
      return { status: response.status, ok: response.ok, payload }
    },
    { requestUrl: `/admin-api${relativePath}`, requestHeaders: headers }
  )
}

function unwrapApiData(response, label) {
  assert.ok(response.ok, `${label}_http_failed:${response.status}:${JSON.stringify(response.payload)}`)
  assert.ok(response.payload && [0, 200].includes(response.payload.code), `${label}_business_failed:${JSON.stringify(response.payload)}`)
  return response.payload.data
}

async function openImportDialog(page) {
  await page.goto(new URL('/mdm/form-center/template', config.baseUrl).toString(), { waitUntil: 'commit' })
  await page.getByText('表单模板', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
  await settle(page)
  await page.getByRole('button', { name: '导入' }).first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入表单模板' }).last()
  await dialog.waitFor({ state: 'visible', timeout: config.timeout })
  return dialog
}

async function importWordByUi(page) {
  const manualDetectRequests = []
  page.on('response', (response) => {
    if (response.url().includes('/fill-rule-auto-detect')) {
      manualDetectRequests.push({ status: response.status(), url: response.url() })
    }
  })
  const dialog = await openImportDialog(page)
  await dialog.locator('input[placeholder*="输入新模板名称"]').first().fill(config.templateName)
  await page.waitForTimeout(500)
  const exactSuggestion = page.locator('.el-autocomplete-suggestion li:visible, .el-popper li:visible').filter({
    hasText: config.templateName
  }).first()
  if (await exactSuggestion.count()) {
    await exactSuggestion.click()
  }
  await dialog.locator('[data-testid="form-template-import-upload"] input[type="file"]').setInputFiles(config.filePath)
  const importResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/form-center/templates/import-doc') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await dialog.getByRole('button', { name: '导入' }).click()
  const importResponse = await importResponsePromise
  const importPayload = await importResponse.json().catch(() => null)
  assert.ok(importResponse.ok(), `import_http_failed:${importResponse.status()}:${JSON.stringify(importPayload)}`)
  assert.ok(importPayload && [0, 200].includes(importPayload.code), `import_business_failed:${JSON.stringify(importPayload)}`)
  assert.equal(manualDetectRequests.length, 0, `manual_rule_detect_must_not_be_called:${JSON.stringify(manualDetectRequests)}`)
  const imported = importPayload.data
  assert.equal(imported.importAction, 'UPGRADE', `target_template_must_be_imported_as_upgrade:${JSON.stringify(imported)}`)
  return imported
}

function parseTemplateRules(detail) {
  assert.ok(detail?.jimuSchemaJson, 'published_template_jimu_schema_missing')
  const root = JSON.parse(detail.jimuSchemaJson)
  const layout = typeof root.sheetLayoutJson === 'string' ? JSON.parse(root.sheetLayoutJson) : root.sheetLayoutJson
  const rules = Array.isArray(root.cellRules) ? root.cellRules : []
  const markers = Array.isArray(root.signatureCellMarkers) ? root.signatureCellMarkers : []
  assert.ok(layout?.rows && Object.keys(layout.rows).some((key) => key !== 'len'), 'published_template_schema_rows_missing')
  assert.ok(rules.length > 0, 'published_template_cell_rules_missing')
  return { root, layout, rules, markers }
}

function hasRule(rules, predicate, message) {
  assert.ok(rules.some(predicate), message)
}

async function verifyPublishedVersion(page, imported) {
  assert.equal(imported.status, 'PUBLISHED', `imported_version_not_published:${JSON.stringify(imported)}`)
  const headers = await buildAuthHeaders(page)
  const detailResponse = await requestJson(
    page,
    headers,
    `/form-center/templates/${imported.templateId}/versions/${encodeURIComponent(imported.versionNo)}`
  )
  const detail = unwrapApiData(detailResponse, 'template_detail')
  assert.equal(detail.status, 'PUBLISHED', `published_detail_status_mismatch:${JSON.stringify(detail)}`)
  const { rules, markers } = parseTemplateRules(detail)
  hasRule(rules, (rule) => rule.label === '生产批号' && rule.valueType === 'STRING' && rule.componentFlag === 'input-text',
    'production_batch_cell_must_be_text')
  hasRule(rules, (rule) => rule.label === '型号/规格' && rule.valueType === 'STRING' && rule.componentFlag === 'input-text',
    'spec_cell_must_be_text')
  hasRule(rules, (rule) => rule.label === '批数量' && rule.valueType === 'NUMBER' && rule.componentFlag === 'input-number',
    'batch_quantity_cell_must_be_number')
  hasRule(rules, (rule) => rule.label === '序号' && rule.valueType === 'NUMBER' && rule.componentFlag === 'input-number',
    'sequence_cell_must_be_number')
  hasRule(rules, (rule) => rule.valueType === 'DATE' && rule.componentFlag === 'date',
    'date_cells_must_be_date_component')
  hasRule(rules, (rule) => rule.valueType === 'STRING' && rule.componentFlag === 'radio-group'
      && JSON.stringify(rule).includes('符合要求') && JSON.stringify(rule).includes('不符合要求'),
    'conformity_cells_must_be_radio_group')
  hasRule(rules, (rule) => rule.valueType === 'STRING' && rule.componentFlag === 'radio-group'
      && JSON.stringify(rule).includes('合格') && JSON.stringify(rule).includes('不合格'),
    'judgement_cells_must_be_radio_group')
  hasRule(rules, (rule) => rule.valueType === 'SIGNATURE' && rule.componentFlag === 'signature',
    'signature_cells_must_be_signature_component')
  assert.ok(markers.some((marker) => marker.enabled === true), 'signature_markers_must_be_generated')
  return {
    status: detail.status,
    ruleCount: rules.length,
    signatureMarkerCount: markers.length,
    requiredSamples: {
      productionBatch: rules.find((rule) => rule.label === '生产批号'),
      spec: rules.find((rule) => rule.label === '型号/规格'),
      batchQuantity: rules.find((rule) => rule.label === '批数量'),
      sequence: rules.find((rule) => rule.label === '序号'),
      signature: rules.find((rule) => rule.valueType === 'SIGNATURE' && rule.componentFlag === 'signature')
    }
  }
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(config.timeout)
  page.setDefaultNavigationTimeout(config.timeout)
  try {
    await login(page)
    const imported = await importWordByUi(page)
    const published = await verifyPublishedVersion(page, imported)
    console.log(JSON.stringify({
      status: 'PASS',
      baseUrl: config.baseUrl,
      templateName: config.templateName,
      sourceFile: path.basename(config.filePath),
      imported: {
        templateId: imported.templateId,
        versionNo: imported.versionNo,
        status: imported.status,
        importAction: imported.importAction,
        approvalRequestId: imported.approvalRequestId || null
      },
      published
    }, null, 2))
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
