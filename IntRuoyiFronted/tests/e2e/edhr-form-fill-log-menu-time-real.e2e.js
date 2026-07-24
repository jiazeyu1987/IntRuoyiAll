const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_FORM_FILL_LOG_E2E_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_FORM_FILL_LOG_E2E_TENANT || '芋道源码'
const USERNAME = process.env.EDHR_FORM_FILL_LOG_E2E_USERNAME || 'admin'
const PASSWORD = process.env.EDHR_FORM_FILL_LOG_E2E_PASSWORD || 'admin123'
const TARGET_PATH = '/mes/pro/feedback/edhr-form-fill-log'
const TARGET_TEXT = '表单日志'
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const OUTPUT_DIR = path.resolve(
  __dirname,
  '..',
  '..',
  '..',
  'doc',
  'tasks',
  '20260714-edhr-form-log-human-cell-location',
  'e2e-artifacts'
)

function ensurePrerequisites() {
  assert.match(BASE_URL, /^http:\/\/(127\.0\.0\.1|localhost):8081$/, 'E2E 只能验证本机 8081 前端')
  assert.equal(TENANT, '芋道源码', '表单日志最终只读复验必须使用芋道源码租户')
  assert.equal(USERNAME, 'admin', '表单日志最终只读复验必须使用 admin')
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `系统 Chrome 不存在: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
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
  throw new Error(`缺少可填写登录控件：${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible')
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  await tenantInput.click()
  await tenantInput.fill(TENANT)
  const tenantOption = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: TENANT })
    .first()
  if ((await tenantOption.count()) > 0) {
    await tenantOption.click()
  } else {
    await tenantInput.press('Enter')
  }

  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    USERNAME,
    '账号'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), PASSWORD, '密码')

  const loginResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginBody = await (await loginResponse).json()
  assert(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

function unwrapResponse(body, label) {
  assert(body.code === 0 || body.code === 200, `${label}失败：${body.msg || body.code}`)
  return body.data
}

function formatHumanDateTime(value) {
  const date = new Date(value)
  assert.ok(Number.isFinite(date.getTime()), `填写时间不是可解析时间: ${value}`)
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function columnIndexToLetters(columnIndex) {
  let index = columnIndex + 1
  let letters = ''
  while (index > 0) {
    const remainder = (index - 1) % 26
    letters = String.fromCharCode(65 + remainder) + letters
    index = Math.floor((index - 1) / 26)
  }
  return letters
}

function parseIndexedPathPart(pathValue, partName) {
  const match = String(pathValue || '').match(new RegExp(`${partName}\\[(\\d+)\\]`))
  return match ? Number(match[1]) : undefined
}

function formatExpectedCellLocation(item) {
  const sheetIndex = parseIndexedPathPart(item.fieldPath, 'sheet')
  const rowIndex =
    typeof item.rowIndex === 'number' ? item.rowIndex : parseIndexedPathPart(item.fieldPath, 'rows')
  const columnIndex =
    typeof item.columnIndex === 'number'
      ? item.columnIndex
      : parseIndexedPathPart(item.fieldPath, 'cells')
  assert.equal(typeof rowIndex, 'number', `明细记录缺少可解析行号: ${JSON.stringify(item)}`)
  assert.equal(typeof columnIndex, 'number', `明细记录缺少可解析列号: ${JSON.stringify(item)}`)
  const sheetLabel = typeof sheetIndex === 'number' ? `表${sheetIndex + 1}` : ''
  const code = `${columnIndexToLetters(columnIndex)}${rowIndex + 1}`
  const detail = `第${rowIndex + 1}行，第${columnIndex + 1}列`
  return {
    code,
    detail: sheetLabel ? `${sheetLabel} · ${detail}` : detail
  }
}

async function verifyMenuPosition(page) {
  const menu = page.locator('#v-menu')
  await menu.waitFor({ state: 'visible', timeout: 60000 })
  const menuLines = (await menu.innerText())
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)

  const logIndex = menuLines.indexOf(TARGET_TEXT)
  const traceIndex = menuLines.indexOf('表单追溯')
  assert.equal(menuLines.indexOf('放行与归档'), -1, '左侧菜单不得显示“放行与归档”独立入口')
  assert.notEqual(traceIndex, -1, '左侧菜单必须显示“表单追溯”')
  assert.notEqual(logIndex, -1, '左侧菜单必须显示“表单日志”')
  assert.equal(logIndex, traceIndex + 1, '“表单日志”必须紧贴在“表单追溯”下方')
  return menuLines.slice(Math.max(0, traceIndex - 2), logIndex + 3)
}

async function verifyTimeFormat(page) {
  const pageResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/form-fill-log/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}${TARGET_PATH}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText(TARGET_TEXT, { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  const body = await (await pageResponse).json()
  const data = unwrapResponse(body, '表单日志分页')
  const rows = Array.isArray(data?.list) ? data.list : Array.isArray(data?.records) ? data.records : []
  assert.ok(rows.length > 0, '芋道源码租户必须存在真实表单日志记录，不能用空列表替代 E2E')
  const firstWithTime = rows.find((row) => row.changedAt)
  assert.ok(firstWithTime, '真实表单日志记录必须包含 changedAt')

  const expected = formatHumanDateTime(firstWithTime.changedAt)
  const firstDataRow = page.locator('.el-table__body-wrapper tbody tr').first()
  await firstDataRow.waitFor({ state: 'visible', timeout: 60000 })
  const firstRowText = await firstDataRow.innerText()
  assert.match(firstRowText, new RegExp(expected.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), '首行必须显示年月日 时分秒格式')
  assert.doesNotMatch(firstRowText, /T\d{2}:\d{2}:\d{2}/, '首行填写时间不得显示 ISO T 分隔符')

  return {
    auditBatchId: firstWithTime.auditBatchId,
    rawChangedAt: firstWithTime.changedAt,
    expectedChangedAt: expected,
    firstRowText
  }
}

async function verifyDetailCellLocation(page, auditBatchId) {
  const detailResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-execution/form-fill-log/detail') &&
      response.url().includes(`auditBatchId=${auditBatchId}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page
    .locator('.el-table__body-wrapper tbody tr')
    .first()
    .getByRole('button', { name: '明细' })
    .click()
  const detailBody = await (await detailResponse).json()
  const detail = unwrapResponse(detailBody, '表单日志明细')
  const items = Array.isArray(detail?.items) ? detail.items : []
  const targetItem = items.find((item) => item.fieldPath && /rows\[\d+\]\.cells\[\d+\]/.test(item.fieldPath))
  assert.ok(targetItem, '真实明细必须包含可解析的单元格定位数据')
  const expectedLocation = formatExpectedCellLocation(targetItem)

  const drawer = page.locator('.el-drawer').filter({ hasText: '填写单元格明细' }).first()
  await drawer.waitFor({ state: 'visible', timeout: 60000 })
  const drawerText = await drawer.innerText()
  assert.match(drawerText, new RegExp(expectedLocation.code.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), '明细必须以 Excel 风格坐标作为主显示')
  assert.match(drawerText, new RegExp(expectedLocation.detail.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), '明细必须显示一基行列说明')
  assert.doesNotMatch(drawerText, /sheet\[\d+\]\.rows\[\d+\]\.cells\[\d+\]/, '明细主显示不得暴露技术路径定位')

  return {
    auditItemId: targetItem.auditItemId,
    rawFieldPath: targetItem.fieldPath,
    expectedLocation: `${expectedLocation.code} / ${expectedLocation.detail}`
  }
}

async function run() {
  ensurePrerequisites()

  const browser = await chromium.launch({
    headless: process.env.EDHR_FORM_FILL_LOG_E2E_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({
    viewport: { width: 1680, height: 900 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const mesWriteRequests = []
  const consoleErrors = []
  const pageErrors = []

  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
    ) {
      mesWriteRequests.push({ method: request.method(), url: request.url() })
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.stack || error.message))

  try {
    await login(page)
    const timeEvidence = await verifyTimeFormat(page)
    const detailLocationEvidence = await verifyDetailCellLocation(page, timeEvidence.auditBatchId)
    const menuEvidence = await verifyMenuPosition(page)

    assert.equal(
      mesWriteRequests.length,
      0,
      `表单日志只读复验不得产生 MES 写请求: ${JSON.stringify(mesWriteRequests)}`
    )

    const screenshotPath = path.join(OUTPUT_DIR, 'admin-form-log-menu-time.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })
    const result = {
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetPath: TARGET_PATH,
      menuEvidence,
      timeEvidence,
      detailLocationEvidence,
      mesWriteRequests,
      consoleErrors,
      pageErrors,
      screenshotPath
    }
    fs.writeFileSync(
      path.join(OUTPUT_DIR, 'admin-form-log-menu-time.json'),
      `${JSON.stringify(result, null, 2)}\n`,
      'utf8'
    )

    console.log(`PASS: 表单日志菜单位置、填写时间与明细位置真实 E2E 通过，evidence=${OUTPUT_DIR}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
