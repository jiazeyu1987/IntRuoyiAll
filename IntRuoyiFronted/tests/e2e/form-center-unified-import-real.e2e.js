const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const TASK_ID = '20260829-form-center-unified-import-int-main'
const DATA_PREFIX = 'FCUI-20260829-'
const REPO_ROOT = path.resolve(__dirname, '..', '..', '..')
const DEFAULT_DOCX_PATH = 'E:\\IntRuoyi\\resource\\按压式球囊扩充压力泵IDI-001\\过程检验记录.docx'
const RESULT_DIR = path.join(REPO_ROOT, 'doc', 'tasks', TASK_ID, 'evidence', 'real-browser')

const config = {
  frontendUrl: process.env.FORM_CENTER_UNIFIED_IMPORT_FRONTEND_URL || 'http://127.0.0.1:8081',
  backendUrl: process.env.FORM_CENTER_UNIFIED_IMPORT_BACKEND_URL || 'http://127.0.0.1:48081',
  tenant: process.env.FORM_CENTER_UNIFIED_IMPORT_TENANT || '测试租户',
  username: process.env.FORM_CENTER_UNIFIED_IMPORT_USERNAME || 'aoteman',
  password: process.env.FORM_CENTER_UNIFIED_IMPORT_PASSWORD || '111111',
  docxPath: process.env.FORM_CENTER_UNIFIED_IMPORT_DOCX_PATH || DEFAULT_DOCX_PATH,
  headed: process.env.FORM_CENTER_UNIFIED_IMPORT_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `form_center_unified_import_real_e2e_requires_test_tenant_aoteman:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`
  )
}

if (!fs.existsSync(config.docxPath)) {
  throw new Error(`form_center_unified_import_real_docx_missing:${config.docxPath}`)
}

fs.mkdirSync(RESULT_DIR, { recursive: true })

const isWriteMethod = (method) => !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())
const isMesRequest = (url) => url.includes('/admin-api/mes/') || url.includes('/mes/')
const formName = `${DATA_PREFIX}${Date.now()}`

function redactHeaders(headers) {
  const result = { ...headers }
  for (const key of Object.keys(result)) {
    if (/authorization|token|password|secret/i.test(key)) {
      result[key] = '<redacted>'
    }
  }
  return result
}

function assertPayloadOk(payload, label) {
  assert.ok(payload && typeof payload === 'object', `${label}_payload_missing`)
  assert.ok([0, 200].includes(payload.code), `${label}_payload_failed:${JSON.stringify(payload)}`)
}

function parseStoredValue(raw) {
  let value = raw
  for (let index = 0; index < 8; index += 1) {
    if (typeof value === 'string') {
      try {
        value = JSON.parse(value)
        continue
      } catch {
        break
      }
    }
    if (value && typeof value === 'object') {
      const next = value.v ?? value.value ?? value.accessToken ?? value.token
      if (next === undefined || next === value) {
        break
      }
      value = next
      continue
    }
    break
  }
  return value
}

function parseStoredAccessToken(raw) {
  return String(parseStoredValue(raw) || '').replace(/^Bearer\s+/i, '').trim()
}

function unwrapCommonData(payload) {
  return payload && Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : payload
}

async function resolveAuthHeaders(page) {
  const storage = await page.evaluate(() => {
    const result = {}
    for (const key of Object.keys(localStorage)) {
      result[key] = localStorage.getItem(key)
    }
    return result
  })
  const tokenKey = Object.keys(storage).find((key) => key.toLowerCase().includes('access_token')) ||
    Object.keys(storage).find((key) => key.toLowerCase().includes('token'))
  const tenantKey = Object.keys(storage).find((key) => key.toLowerCase() === 'tenantid') ||
    Object.keys(storage).find((key) => key.toLowerCase().includes('tenant'))
  const token = parseStoredAccessToken(tokenKey ? storage[tokenKey] : '')
  const tenantId = String(parseStoredValue(tenantKey ? storage[tenantKey] : '') || '').trim()
  assert.ok(token, 'auth_token_missing_after_login')
  assert.ok(tenantId, 'tenant_id_missing_after_login')
  return {
    authorization: `Bearer ${token}`,
    'tenant-id': tenantId
  }
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function login(page) {
  const loginUrl = new URL('/login', config.frontendUrl)
  loginUrl.searchParams.set('redirect', '/mes/pro/batch-record-form-list')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form)
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assertPayloadOk(loginPayload, 'login')
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function openFormCenterList(page) {
  await page.goto(new URL('/mes/pro/batch-record-form-list', config.frontendUrl).toString(), {
    waitUntil: 'domcontentloaded'
  })
  await page.locator('.batch-record-form-layout').waitFor({ state: 'visible', timeout: 60000 })
}

async function openWordImportDialog(page) {
  await page.locator('.batch-record-form-toolbar__import-button').waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.batch-record-form-toolbar__import-button').click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入 Word' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.getByText('导入类型', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText('产品名称', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(
    await dialog.getByText('表单名称', { exact: true }).count(),
    0,
    'default_import_type_should_stay_batch_record'
  )
  return dialog
}

async function importPlainForm(page, uploadRequests) {
  const dialog = await openWordImportDialog(page)
  await dialog.locator('.el-radio-button').filter({ hasText: '表单' }).click()
  await dialog.getByText('表单名称', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(
    await dialog.getByText('产品名称', { exact: true }).count(),
    0,
    'form_import_type_should_hide_product_selector'
  )
  await dialog.locator('.batch-record-word-import-form__name-input input').fill(formName)

  const fileChooserPromise = page.waitForEvent('filechooser', { timeout: 30000 })
  await dialog.getByRole('button', { name: '选择文件' }).click()
  const fileChooser = await fileChooserPromise
  await fileChooser.setFiles(config.docxPath)
  await dialog.getByText('已选择 Word 文件', { exact: false }).waitFor({ state: 'visible', timeout: 30000 })

  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-report/upload-extra-slot') &&
      response.request().method() === 'POST',
    { timeout: 600000 }
  )
  await dialog.getByRole('button', { name: '确定' }).click()
  const uploadResponse = await uploadResponsePromise
  const uploadPayload = await uploadResponse.json()
  assert.ok(uploadResponse.ok(), `upload_extra_slot_http_failed:${uploadResponse.status()}`)
  assertPayloadOk(uploadPayload, 'upload_extra_slot')

  const result = unwrapCommonData(uploadPayload)
  assert.equal(result.importedCount, 1, 'form_import_should_create_one_report_from_single_docx_table')
  const reports = Array.isArray(result.reports) ? result.reports : []
  const imported = reports.find((item) => item.batchRecordName === formName || item.reportName === formName)
  assert.ok(imported, `uploaded_form_report_missing:${JSON.stringify(reports)}`)
  assert.equal(imported.formSlotType, 'FORM', 'uploaded_report_must_be_plain_form_slot')
  assert.equal(imported.reportName, formName, 'uploaded_form_report_name_must_use_entered_form_name')
  assert.equal(imported.productName, formName, 'uploaded_form_product_name_must_match_entered_form_name')
  assert.ok(imported.reportId, 'uploaded_form_report_id_missing')
  assert.ok(uploadRequests.length >= 1, 'browser_upload_request_must_be_observed')
  return imported
}

async function selectImportedRow(page) {
  await page.getByText(formName, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const row = page.locator('.batch-record-form-layout__list .el-table__body tr').filter({ hasText: formName }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.locator('.el-tag').filter({ hasText: '表单' }).first().waitFor({ state: 'visible', timeout: 30000 })
  await row.click()
  return row
}

async function verifyPreviewAndSharedActions(page, reportId) {
  const preview = page.locator('.batch-record-form-preview').first()
  await preview.waitFor({ state: 'visible', timeout: 60000 })
  await preview.locator('.edhr-template-sheet').first().waitFor({ state: 'visible', timeout: 60000 })
  const previewText = (await preview.innerText()).replace(/\s+/g, ' ')
  assert.ok(previewText.includes('过程检验记录'), 'preview_should_render_docx_title')
  assert.ok(previewText.includes('生产批号'), 'preview_should_render_docx_batch_no_label')
  assert.ok(previewText.includes('气密性检测工装'), 'preview_should_render_docx_body_label')

  const actions = preview.locator('.batch-record-form-preview__actions').first()
  for (const label of ['打开', '编辑', '填写', '填写配置', '链接', '删除']) {
    await actions.getByRole('button', { name: label, exact: true }).waitFor({
      state: 'visible',
      timeout: 30000
    })
  }

  const cellRulesResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-report/cell-rules') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await actions.getByRole('button', { name: '填写配置', exact: true }).click()
  const cellRulesResponse = await cellRulesResponsePromise
  const cellRulesPayload = await cellRulesResponse.json()
  assert.ok(cellRulesResponse.ok(), `cell_rules_http_failed:${cellRulesResponse.status()}`)
  assertPayloadOk(cellRulesPayload, 'cell_rules')
  const editor = page.locator('.batch-record-cell-rules-editor').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.locator('.batch-record-cell-rules-editor__sheet').first().waitFor({ state: 'visible', timeout: 60000 })
  await editor.locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 60000 })
  await page.screenshot({ path: path.join(RESULT_DIR, 'fill-config-dialog.png'), fullPage: true })
  await page.keyboard.press('Escape')
  await editor.waitFor({ state: 'hidden', timeout: 60000 })

  await actions.getByRole('button', { name: '链接', exact: true }).click()
  await page.waitForURL((current) => current.pathname.includes('/mes/pro/batch-record-cell-link'), {
    timeout: 60000
  })
  await page.locator('.batch-record-cell-link').waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('批记录单元格链接', { exact: true }).first().waitFor({ state: 'visible', timeout: 30000 })
  assert.ok(page.url().includes(`sourceReportId=${encodeURIComponent(reportId)}`), 'link_page_should_receive_source_report_id')
  await page.screenshot({ path: path.join(RESULT_DIR, 'cell-link-page.png'), fullPage: true })

  await openFormCenterList(page)
  await selectImportedRow(page)
}

async function deleteImportedRowByUi(page) {
  const deleteResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-report/delete?') &&
      response.request().method() === 'DELETE',
    { timeout: 60000 }
  )
  await page
    .locator('.batch-record-form-preview__actions')
    .first()
    .getByRole('button', { name: '删除', exact: true })
    .click()
  const messageBox = page.locator('.el-message-box:visible').last()
  await messageBox.waitFor({ state: 'visible', timeout: 30000 })
  await messageBox.getByRole('button', { name: '确定', exact: true }).click()
  const deleteResponse = await deleteResponsePromise
  const deletePayload = await deleteResponse.json()
  assert.ok(deleteResponse.ok(), `delete_report_http_failed:${deleteResponse.status()}`)
  assertPayloadOk(deletePayload, 'delete_report')
  await page.getByText('删除成功', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

async function cleanupByApi(page, reportId, headers) {
  if (!reportId || !headers?.authorization || !headers['tenant-id']) {
    return { status: 'SKIPPED', reason: 'missing_report_id_or_auth_headers' }
  }
  const response = await page.request.delete(
    new URL(`/admin-api/mes/pro/batch-record-report/delete?reportId=${encodeURIComponent(reportId)}`, config.backendUrl).toString(),
    { headers }
  )
  const payload = await response.json().catch(() => ({}))
  if (!response.ok() || ![0, 200].includes(payload.code)) {
    return { status: 'FAILED', httpStatus: response.status(), payload }
  }
  return { status: 'PASS' }
}

async function cleanupExistingTestReports(page, headers, phase) {
  if (!headers?.authorization || !headers['tenant-id']) {
    return { status: 'SKIPPED', phase, reason: 'missing_auth_headers' }
  }
  const query = new URLSearchParams({
    pageNo: '1',
    pageSize: '200',
    name: DATA_PREFIX,
    formSlotType: 'FORM',
    latestVersionOnly: 'false'
  })
  const response = await page.request.get(
    new URL(`/admin-api/mes/pro/batch-record-report/page?${query.toString()}`, config.backendUrl).toString(),
    { headers }
  )
  const payload = await response.json().catch(() => ({}))
  if (!response.ok() || ![0, 200].includes(payload.code)) {
    return { status: 'FAILED', phase, httpStatus: response.status(), payload }
  }
  const data = unwrapCommonData(payload)
  const rows = Array.isArray(data?.list) ? data.list : []
  const targets = rows.filter((row) =>
    row?.reportId &&
    row.formSlotType === 'FORM' &&
    (String(row.reportName || '').startsWith(DATA_PREFIX) ||
      String(row.productName || '').startsWith(DATA_PREFIX) ||
      String(row.batchRecordName || '').startsWith(DATA_PREFIX))
  )
  const deleted = []
  const failed = []
  for (const row of targets) {
    const cleanup = await cleanupByApi(page, row.reportId, headers)
    if (cleanup.status === 'PASS') {
      deleted.push(row.reportId)
    } else {
      failed.push({ reportId: row.reportId, cleanup })
    }
  }
  return {
    status: failed.length ? 'FAILED' : 'PASS',
    phase,
    scanned: rows.length,
    deleted: deleted.length,
    failed
  }
}

function writeResult(result) {
  const sanitized = {
    ...result,
    passwordPresent: Boolean(config.password),
    cleanupHeaders: result.cleanupHeaders ? redactHeaders(result.cleanupHeaders) : undefined
  }
  delete sanitized.password
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(sanitized, null, 2)}\n`, 'utf8')

  const lines = [
    '# 表单中心统一导入真实 E2E 证据',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- Generated At: \`${new Date().toISOString()}\``,
    `- Status: \`${result.status}\``,
    `- Frontend: \`${config.frontendUrl}\``,
    `- Backend: \`${config.backendUrl}\``,
    `- Tenant: \`${config.tenant}\``,
    `- User: \`${config.username}\``,
    `- Test Data: \`${formName}\``,
    `- Source DOCX: \`${config.docxPath}\``,
    '',
    '## BDD',
    '',
    '- BDD: 表单作为单页批记录 DOC 导入 -> Given 测试租户进入批记录表单列表 When 在导入 Word 中选择“表单”并上传指定过程检验记录 DOCX Then 系统生成 FORM 类型表单并显示在同一列表。',
    '- BDD: 新表单复用批记录表单能力 -> Given 导入后的表单在同一列表 When 选中该表单 Then 预览、填写配置、链接入口和删除入口沿用批记录表单页。',
    ''
  ]
  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `node tests/e2e/form-center-unified-import-real.e2e.js` -> PASS')
    lines.push(`- Imported Report ID: \`${result.importedReportId || '--'}\``)
    lines.push(`- Imported Version: \`${result.versionNo || '--'}\``)
    lines.push('- UI: 导入类型默认为批记录；切换表单后隐藏产品选择、显示表单名称。')
    lines.push('- UI: 表单导入后在批记录表单同一列表出现，类型显示“表单”。')
    lines.push('- UI: 预览显示指定 DOCX 中的“过程检验记录 / 生产批号 / 气密性检测工装”。')
    lines.push('- UI: 填写配置与链接入口能从同一页进入。')
    lines.push(`- Cleanup: \`${result.cleanup?.status || '--'}\``)
  } else {
    lines.push('## FAILED', '')
    lines.push(`- E2E: \`node tests/e2e/form-center-unified-import-real.e2e.js\` -> FAIL, ${result.error || 'unknown error'}`)
    lines.push(`- Cleanup: \`${result.cleanup?.status || '--'}\``)
  }
  fs.writeFileSync(path.join(RESULT_DIR, 'e2e-real-browser-evidence.md'), `${lines.join('\n')}\n`, 'utf8')
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const uploadRequests = []
  const writeRequests = []
  let page
  let importedReport
  let cleanupHeaders
  let cleanupBefore = { status: 'NOT_STARTED' }
  let cleanup = { status: 'NOT_STARTED' }

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    page.on('request', (request) => {
      const requestUrl = request.url()
      if (isWriteMethod(request.method()) && isMesRequest(requestUrl)) {
        writeRequests.push({ method: request.method(), url: requestUrl })
      }
      if (
        request.method() === 'POST' &&
        requestUrl.includes('/mes/pro/batch-record-report/upload-extra-slot')
      ) {
        const formData = request.postData() || ''
        const headers = request.headers()
        cleanupHeaders = {
          authorization: headers.authorization,
          'tenant-id': headers['tenant-id'],
          'visit-tenant-id': headers['visit-tenant-id']
        }
        uploadRequests.push({
          url: requestUrl,
          batchRecordName: formData.includes(`name="batchRecordName"\r\n\r\n${formName}`) ? formName : undefined,
          formSlotType: formData.includes('name="formSlotType"\r\n\r\nFORM') ? 'FORM' : undefined
        })
      }
    })

    await login(page)
    cleanupHeaders = await resolveAuthHeaders(page)
    cleanupBefore = await cleanupExistingTestReports(page, cleanupHeaders, 'before')
    assert.equal(cleanupBefore.status, 'PASS', `pre_e2e_cleanup_failed:${JSON.stringify(cleanupBefore)}`)
    await openFormCenterList(page)
    importedReport = await importPlainForm(page, uploadRequests)
    await selectImportedRow(page)
    await page.screenshot({ path: path.join(RESULT_DIR, 'imported-form-list-preview.png'), fullPage: true })
    await verifyPreviewAndSharedActions(page, importedReport.reportId)
    await deleteImportedRowByUi(page)
    cleanup = { status: 'PASS', method: 'ui-delete' }

    const result = {
      status: 'PASS',
      importedReportId: importedReport.reportId,
      formSlotType: importedReport.formSlotType,
      reportName: importedReport.reportName,
      productName: importedReport.productName,
      versionNo: importedReport.versionNo,
      uploadRequests,
      writeRequests,
      cleanupBefore,
      cleanup,
      cleanupHeaders
    }
    writeResult(result)
    console.log(JSON.stringify({ ...result, cleanupHeaders: redactHeaders(cleanupHeaders || {}) }, null, 2))
  } catch (error) {
    if (page && importedReport?.reportId && cleanup.status !== 'PASS') {
      cleanup = await cleanupByApi(page, importedReport.reportId, cleanupHeaders)
    } else if (page && cleanupHeaders && cleanup.status !== 'PASS') {
      cleanup = await cleanupExistingTestReports(page, cleanupHeaders, 'after-failure')
    }
    if (page) {
      await page.screenshot({ path: path.join(RESULT_DIR, 'failure.png'), fullPage: true }).catch(() => {})
    }
    const result = {
      status: 'FAIL',
      error: error?.stack || String(error),
      importedReportId: importedReport?.reportId,
      cleanup,
      cleanupHeaders
    }
    writeResult(result)
    console.error(error)
    process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main()
