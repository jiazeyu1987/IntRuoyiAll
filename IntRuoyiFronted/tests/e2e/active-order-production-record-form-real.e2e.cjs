const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const TASK_ID = '20260906-active-order-production-record-form-button'
const FRONTEND_URL =
  process.env.ACTIVE_ORDER_RECORD_FORM_E2E_FRONTEND_URL || 'http://127.0.0.1:8081'
const TENANT_NAME = process.env.ACTIVE_ORDER_RECORD_FORM_E2E_TENANT || '芋道源码'
const USERNAME = process.env.ACTIVE_ORDER_RECORD_FORM_E2E_USERNAME || 'admin'
const PASSWORD = process.env.ACTIVE_ORDER_RECORD_FORM_E2E_PASSWORD || ''
const WORK_ORDER_CODE =
  process.env.ACTIVE_ORDER_RECORD_FORM_E2E_WORK_ORDER_CODE ||
  'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'

const OUTPUT_DIR = path.resolve(__dirname, '../../../doc/tasks', TASK_ID, 'e2e-artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'active-order-production-record-form-real-result.json')
const SCREENSHOT_PATH = path.join(OUTPUT_DIR, 'active-order-production-record-form-real.png')

if (!PASSWORD) {
  throw new Error(
    'ACTIVE_ORDER_RECORD_FORM_E2E_PASSWORD is required; inject it at runtime and do not commit it.'
  )
}

function sanitizeUrl(url) {
  return String(url || '').replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')
}

async function selectLoginTenant(page) {
  const tenantInput = page.locator('.login-form .el-select input:visible').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 20000 })
  await tenantInput.click()
  await tenantInput.fill(TENANT_NAME)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: TENANT_NAME }).first()
  await option.waitFor({ state: 'visible', timeout: 20000 })
  await option.click()
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await selectLoginTenant(page)
  await page.locator('.login-form input[placeholder="请输入用户名"]:visible').first().fill(USERNAME)
  await page
    .locator('.login-form input[type="password"][placeholder="请输入密码"]:visible')
    .first()
    .fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await page
    .locator('.login-form button[type="submit"]:visible, .login-form button:has-text("登录"):visible')
    .first()
    .click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

async function openTargetActiveOrderDetail(page) {
  await page.goto(`${FRONTEND_URL}/mes/pro/process-pool/production-leader`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })

  const moduleTabs = page.locator('[data-production-leader-module-tabs]').first()
  await moduleTabs.waitFor({ state: 'visible', timeout: 60000 })
  await moduleTabs.getByRole('tab', { name: '活跃订单池', exact: true }).click()

  const root = page.locator('[data-team-leader-active-order-config]').first()
  await root.waitFor({ state: 'visible', timeout: 60000 })

  const codeCell = root
    .locator('[data-team-leader-active-order-work-order-code]', { hasText: WORK_ORDER_CODE })
    .first()
  await codeCell.waitFor({ state: 'visible', timeout: 60000 })
  const row = codeCell.locator('xpath=ancestor::tr[1]')

  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/detail') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  const detailPagePromise = page.waitForURL((url) => url.pathname.includes('/submission-detail'), {
    timeout: 30000
  })
  await row.locator('[data-team-leader-active-order-detail]').first().click()
  await detailPagePromise
  const detailResponse = await detailResponsePromise
  assert.equal(detailResponse.ok(), true, `活跃订单详情 HTTP 失败：${detailResponse.status()}`)

  const detailPage = page.locator('[data-team-leader-active-order-detail-page]').first()
  await detailPage.waitFor({ state: 'visible', timeout: 30000 })
  await detailPage.getByRole('tab', { name: /生产提交/ }).click()
  return detailPage
}

async function findProcessWithProductionSubmission(detailPage) {
  const processTabs = detailPage.locator('[data-team-leader-active-order-detail-production-process-tab]')
  const processCount = await processTabs.count()
  assert.ok(processCount > 0, '生产提交必须至少展示一个生产工序 tab')

  const formButtons = detailPage.locator('[data-active-order-production-record-form-button]')
  assert.equal(
    await formButtons.count(),
    processCount,
    '每个生产工序卡片必须有一个“表单”按钮'
  )

  for (let index = 0; index < processCount; index += 1) {
    const tab = processTabs.nth(index)
    const processName = (await tab.innerText()).replace(/^\s*\d+\.\s*/, '').trim()
    await tab.click()
    const visibleProcess = detailPage
      .locator('.team-leader-workbench__active-order-process-detail:visible')
      .first()
    await visibleProcess.waitFor({ state: 'visible', timeout: 15000 })
    const button = visibleProcess.locator('[data-active-order-production-record-form-button]').first()
    if (!(await button.isEnabled().catch(() => false))) {
      continue
    }
    const row = visibleProcess.locator('.el-table__body-wrapper tbody tr:visible').first()
    await row.waitFor({ state: 'visible', timeout: 15000 })
    const cells = await row.locator('td').allInnerTexts()
    assert.ok(cells.length >= 9, `工序 ${processName} 的生产提交列表列数不足`)
    return {
      processName,
      cardText: await visibleProcess.innerText(),
      firstRowCells: cells.map((cell) => cell.trim()).filter(Boolean),
      button
    }
  }
  throw new Error('没有找到存在一线生产提交的生产工序，无法验证表单真实数据')
}

async function verifyProductionRecordForm(page, processEvidence) {
  await processEvidence.button.click()
  const dialog = page.locator('[data-active-order-production-record-form-dialog]').first()
  await dialog.waitFor({ state: 'visible', timeout: 15000 })

  const text = await dialog.innerText()
  for (const required of [
    '生产记录表单',
    '工序名称',
    '生产批号',
    '产品规格',
    '提交人',
    '复核人',
    '输出物料编码',
    '输出物料名称',
    '设备',
    '设备参数',
    '生产数量',
    '损耗数量',
    '总数量',
    '清场确认',
    '物料确认',
    '清洁确认'
  ]) {
    assert.ok(text.includes(required), `生产记录表单缺少字段：${required}`)
  }
  assert.ok(text.includes(processEvidence.processName), '表单必须展示当前工序名称')

  const rowFacts = processEvidence.firstRowCells.filter((value) => value && value !== '-')
  const reusableFacts = rowFacts.filter((value) => /\p{Script=Han}|[A-Za-z0-9]/u.test(value))
  const missingFacts = reusableFacts.filter((value) => !text.includes(value))
  assert.deepEqual(
    missingFacts,
    [],
    `表单未复用当前工序生产提交行事实：${missingFacts.join('、')}`
  )

  const bodyRows = dialog.locator('.el-table__body-wrapper tbody tr:visible')
  assert.ok((await bodyRows.count()) > 0, '生产记录表单必须展示至少一条真实提交明细')
  await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
  return text
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const evidence = {
    taskId: TASK_ID,
    mode: 'frontend-only-playwright',
    frontendUrl: FRONTEND_URL,
    tenant: TENANT_NAME,
    username: USERNAME,
    workOrderCode: WORK_ORDER_CODE,
    requests: [],
    consoleErrors: [],
    pageErrors: []
  }

  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 950 } })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
  page.on('response', (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/mes/pro/process-pool/team-leader/active-order')) return
    evidence.requests.push({
      url: sanitizeUrl(url),
      method: response.request().method(),
      status: response.status()
    })
  })

  try {
    await login(page)
    const detailPage = await openTargetActiveOrderDetail(page)
    const processEvidence = await findProcessWithProductionSubmission(detailPage)
    const formText = await verifyProductionRecordForm(page, processEvidence)
    assert.equal(evidence.pageErrors.length, 0, `页面错误：${evidence.pageErrors.join('\n')}`)
    evidence.status = 'PASS'
    evidence.processName = processEvidence.processName
    evidence.firstRowCells = processEvidence.firstRowCells
    evidence.formContainsCurrentProcess = formText.includes(processEvidence.processName)
    evidence.screenshot = SCREENSHOT_PATH
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    try {
      await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
      evidence.screenshot = SCREENSHOT_PATH
    } catch (_) {
      // Keep the original failure.
    }
    throw error
  } finally {
    await browser.close().catch(() => {})
    fs.writeFileSync(RESULT_PATH, JSON.stringify(evidence, null, 2))
  }
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error))
  process.exit(1)
})
