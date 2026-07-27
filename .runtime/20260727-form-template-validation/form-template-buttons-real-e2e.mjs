import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'

const repoRoot = 'E:/IntRuoyi'
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

const BASE_URL = 'http://127.0.0.1:8081'
const TEMPLATE_VERSION_ROW_ID = 29
const TEMPLATE_ID = 28
const TEMPLATE_VERSION = 'V2.0'
const REPORT_ID = '2ef53e1302bd47bdba9ccbb87cd92032'
const CHROME_PATH = 'C:/Program Files/Google/Chrome/Application/chrome.exe'

function parseDotEnv(filePath) {
  const values = {}
  for (const rawLine of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#') || !line.includes('=')) continue
    const separator = line.indexOf('=')
    const key = line.slice(0, separator).trim()
    const value = line
      .slice(separator + 1)
      .trim()
      .replace(/^['"]|['"]$/g, '')
    values[key] = value
  }
  return values
}

function mysql(sql) {
  return execFileSync(
    'docker',
    [
      'exec',
      '-i',
      'int-ruoyi-mysql',
      'sh',
      '-lc',
      'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N ruoyi-vue-pro'
    ],
    {
      input: sql,
      encoding: 'utf8',
      windowsHide: true
    }
  ).trim()
}

function assertSingleAffectedRow(output, action) {
  const lines = output.split(/\r?\n/).filter(Boolean)
  assert.equal(lines.at(-1), '1', `${action} must affect exactly one row: ${output}`)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`Missing visible login field: ${label}`)
}

async function login(page, credentials) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  await tenantInput.click()
  await tenantInput.fill(credentials.tenant)
  const tenantOption = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: credentials.tenant })
    .first()
  await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
  await tenantOption.click()

  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    credentials.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), credentials.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^\u767b\u5f55$/ }).click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `Login HTTP failed: ${loginResponse.status()}`)
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `Login failed: ${loginBody.msg}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function openTemplatePage(page, expectedBound) {
  const poolResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/form-center/template-pool') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mdm/form-center/template`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const poolResponse = await poolResponsePromise
  const poolBody = await poolResponse.json()
  assert.ok(poolResponse.ok(), `Template pool HTTP failed: ${poolResponse.status()}`)
  assert.equal(poolBody.code, 0, `Template pool failed: ${poolBody.msg}`)

  const rows = poolBody.data?.list || []
  const rowData = rows.find(
    (item) => Number(item.templateId) === TEMPLATE_ID && item.versionNo === TEMPLATE_VERSION
  )
  assert.ok(rowData, 'Target template version is not present on the first template pool page')
  if (expectedBound) {
    assert.equal(rowData.batchRecordBindingStatus, 'BOUND')
    assert.equal(rowData.batchRecordReportId, REPORT_ID)
  } else {
    assert.ok(!rowData.batchRecordBindingStatus)
    assert.ok(!rowData.batchRecordReportId)
  }

  const row = page
    .locator('.el-table__body-wrapper tbody tr:visible')
    .filter({ hasText: rowData.templateName })
    .filter({ hasText: TEMPLATE_VERSION })
    .first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.click()
  await page
    .locator('[data-form-template-preview] .form-template-preview__title')
    .filter({ hasText: rowData.templateName })
    .waitFor({ state: 'visible', timeout: 30000 })
  return rowData
}

async function verifyDesignerRoute(page, buttonName, reportMode) {
  const button = page
    .locator('[data-form-template-preview] .form-template-preview__actions')
    .getByRole('button', { name: new RegExp(`^${buttonName}$`) })
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await Promise.all([
    page.waitForURL(
      (url) =>
        url.pathname === '/mes/pro/batch-record-form-list' &&
        url.searchParams.get('mode') === 'designer' &&
        url.searchParams.get('reportMode') === reportMode &&
        url.searchParams.get('reportId') === REPORT_ID,
      { timeout: 60000 }
    ),
    button.click()
  ])
}

async function verifyFillRoute(page) {
  const button = page
    .locator('[data-form-template-preview] .form-template-preview__actions')
    .getByRole('button', { name: /^\u586b\u5199$/ })
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await Promise.all([
    page.waitForURL(
      (url) =>
        url.pathname === '/mes/pro/feedback/edhr-batch-execution/template-simulate' &&
        url.searchParams.get('reportId') === REPORT_ID &&
        url.searchParams.get('returnLabel') === '\u8fd4\u56de\u8868\u5355\u6a21\u677f',
      { timeout: 60000 }
    ),
    button.click()
  ])
}

const cleanCount = mysql(`
SELECT COUNT(*)
  FROM bpm_form_template_version
 WHERE id = ${TEMPLATE_VERSION_ROW_ID}
   AND deleted = 0
   AND batch_record_report_id IS NULL
   AND batch_record_report_name IS NULL
   AND batch_record_name IS NULL
   AND batch_record_version_no IS NULL
   AND batch_record_form_slot_type IS NULL
   AND batch_record_binding_status IS NULL
   AND batch_record_binding_error IS NULL;
`)
assert.equal(cleanCount, '1', 'Target template fixture is not clean before the test')

const setupResult = mysql(`
UPDATE bpm_form_template_version v
JOIN mes_pro_batch_record_report r
  ON r.report_id = '${REPORT_ID}'
 AND r.deleted = 0
JOIN mes_pro_batch_record_version rv
  ON rv.id = r.batch_record_version_id
 AND rv.deleted = 0
SET v.batch_record_report_id = r.report_id,
    v.batch_record_report_name = r.report_name,
    v.batch_record_name = r.batch_record_name,
    v.batch_record_version_no = rv.version_no,
    v.batch_record_form_slot_type = r.form_slot_type,
    v.batch_record_binding_status = 'BOUND',
    v.batch_record_binding_error = NULL
WHERE v.id = ${TEMPLATE_VERSION_ROW_ID}
  AND v.deleted = 0
  AND v.tenant_id = r.tenant_id
  AND rv.status = 'APPROVED'
  AND v.batch_record_report_id IS NULL
  AND v.batch_record_report_name IS NULL
  AND v.batch_record_name IS NULL
  AND v.batch_record_version_no IS NULL
  AND v.batch_record_form_slot_type IS NULL
  AND v.batch_record_binding_status IS NULL
  AND v.batch_record_binding_error IS NULL;
SELECT ROW_COUNT();
`)
assertSingleAffectedRow(setupResult, 'Fixture setup')

let restored = false
let browser
try {
  const env = parseDotEnv(path.join(frontendRoot, '.env'))
  const credentials = {
    tenant: env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
  assert.ok(credentials.tenant && credentials.username && credentials.password, 'Missing login credentials')

  browser = await chromium.launch({ headless: true, executablePath: CHROME_PATH })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const mesWrites = []
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())
    ) {
      mesWrites.push(`${request.method()} ${new URL(request.url()).pathname}`)
    }
  })

  await login(page, credentials)

  await openTemplatePage(page, true)
  await verifyDesignerRoute(page, '\u6253\u5f00', 'preview')

  await openTemplatePage(page, true)
  await verifyDesignerRoute(page, '\u7f16\u8f91', 'edit')

  await openTemplatePage(page, true)
  await verifyFillRoute(page)

  const restoreResult = mysql(`
UPDATE bpm_form_template_version
SET batch_record_report_id = NULL,
    batch_record_report_name = NULL,
    batch_record_name = NULL,
    batch_record_version_no = NULL,
    batch_record_form_slot_type = NULL,
    batch_record_binding_status = NULL,
    batch_record_binding_error = NULL
WHERE id = ${TEMPLATE_VERSION_ROW_ID}
  AND deleted = 0
  AND batch_record_report_id = '${REPORT_ID}'
  AND batch_record_binding_status = 'BOUND';
SELECT ROW_COUNT();
`)
  assertSingleAffectedRow(restoreResult, 'Fixture restore')
  restored = true

  await openTemplatePage(page, false)
  const openButton = page
    .locator('[data-form-template-preview] .form-template-preview__actions')
    .getByRole('button', { name: /^\u6253\u5f00$/ })
  const beforeUrl = page.url()
  await openButton.click()
  await page
    .locator('.el-message--error:visible')
    .filter({ hasText: '\u5f53\u524d\u6a21\u677f\u672a\u7ed1\u5b9a\u6279\u8bb0\u5f55\u8868\u5355' })
    .waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(page.url(), beforeUrl, 'Unbound template must not navigate')
  assert.deepEqual(mesWrites, [], `Unexpected MES writes: ${mesWrites.join(', ')}`)

  await context.close()
  console.log(
    `PASS form-template-buttons-real-e2e templateId=${TEMPLATE_ID} version=${TEMPLATE_VERSION} reportSuffix=${REPORT_ID.slice(-6)}`
  )
} finally {
  if (browser) await browser.close()
  if (!restored) {
    mysql(`
UPDATE bpm_form_template_version
SET batch_record_report_id = NULL,
    batch_record_report_name = NULL,
    batch_record_name = NULL,
    batch_record_version_no = NULL,
    batch_record_form_slot_type = NULL,
    batch_record_binding_status = NULL,
    batch_record_binding_error = NULL
WHERE id = ${TEMPLATE_VERSION_ROW_ID}
  AND deleted = 0
  AND batch_record_report_id = '${REPORT_ID}'
  AND batch_record_binding_status = 'BOUND';
`)
  }
  const finalCleanCount = mysql(`
SELECT COUNT(*)
  FROM bpm_form_template_version
 WHERE id = ${TEMPLATE_VERSION_ROW_ID}
   AND deleted = 0
   AND batch_record_report_id IS NULL
   AND batch_record_report_name IS NULL
   AND batch_record_name IS NULL
   AND batch_record_version_no IS NULL
   AND batch_record_form_slot_type IS NULL
   AND batch_record_binding_status IS NULL
   AND batch_record_binding_error IS NULL;
`)
  assert.equal(finalCleanCount, '1', 'Target template fixture was not restored')
}
