const assert = require('node:assert/strict')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES feedback import attribution real E2E.")
  }
}

const config = {
  baseUrl: (process.env.MES_FEEDBACK_ATTRIBUTION_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_FEEDBACK_ATTRIBUTION_E2E_TENANT || '测试租户',
  username: process.env.MES_FEEDBACK_ATTRIBUTION_E2E_USERNAME || 'aoteman',
  password: process.env.MES_FEEDBACK_ATTRIBUTION_E2E_PASSWORD || 'admin123',
  workOrderCode: process.env.MES_FEEDBACK_ATTRIBUTION_E2E_WORK_ORDER_CODE || 'CODexERP20260610D',
  taskCode: process.env.MES_FEEDBACK_ATTRIBUTION_E2E_TASK_CODE || 'TASK-CODEX-20260610-D-B010',
  uploadFile: process.env.MES_FEEDBACK_ATTRIBUTION_E2E_UPLOAD_FILE,
  headed: process.env.MES_FEEDBACK_ATTRIBUTION_E2E_HEADED === '1'
}

if (!config.uploadFile) {
  throw new Error('MES_FEEDBACK_ATTRIBUTION_E2E_UPLOAD_FILE is required.')
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

  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    loginForm.locator('.el-button--primary').first().click()
  ])
  const loginBody = await loginResponse.json()
  if (loginBody.code !== 0) {
    throw new Error(`登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  }
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openFeedbackPage(page) {
  await page.goto(`${config.baseUrl}/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('第三方导入').waitFor({ state: 'visible', timeout: 30000 })
}

async function importExcel(page) {
  await page.getByRole('button', { name: /第三方导入/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入第三方报工' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[type="file"]').setInputFiles(path.resolve(config.uploadFile))
  const [importResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/feedback/import-third-party-xlsx') && response.status() === 200,
      { timeout: 60000 }
    ),
    dialog.getByRole('button', { name: /确 定/ }).click()
  ])
  const importBody = await importResponse.json()
  assert.equal(importBody.code, 0, `第三方报工导入失败: ${importBody.msg || importBody.code}`)
  assert.equal(importBody.data.importedCount, 1, '第三方报工导入必须导入 1 条记录。')
  assert.equal(importBody.data.pendingCount, 1, '第三方报工导入后必须进入待归属。')
  const importRecordId = importBody.data.importRecordIds?.[0]
  assert.ok(importRecordId, '第三方报工导入必须返回待归属记录 ID。')
  await page.getByRole('button', { name: /确定/ }).click().catch(() => {})
  return importRecordId
}

async function attributeImportRecord(page, importRecordId) {
  await page.getByRole('tab', { name: '待归属' }).click()
  await page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/feedback/import-record/page') && response.status() === 200,
    { timeout: 60000 }
  ).catch(() => {})
  const row = page.locator('tr.el-table__row').filter({ hasText: String(importRecordId) }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  assert.match(await row.innerText(), new RegExp(config.workOrderCode), '待归属列表必须显示导入工单。')

  const [candidateResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/feedback/import-record/candidates') && response.status() === 200,
      { timeout: 60000 }
    ),
    row.getByText('选择归属').click()
  ])
  const candidateBody = await candidateResponse.json()
  assert.equal(candidateBody.code, 0, `归属候选接口失败: ${candidateBody.msg || candidateBody.code}`)
  assert.ok(candidateBody.data.length > 0, '待归属记录必须返回候选排产工单工序。')
  const selected = candidateBody.data.find((item) => item.taskCode === config.taskCode)
  assert.ok(selected, `归属候选必须包含活动任务 ${config.taskCode}`)

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '确认归属' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const selectedRow = dialog.locator('tr.el-table__row').filter({ hasText: config.taskCode }).first()
  await selectedRow.waitFor({ state: 'visible', timeout: 30000 })
  await selectedRow.locator('.el-radio').click()
  const [attributeResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/feedback/import-record/attribute') && response.status() === 200,
      { timeout: 60000 }
    ),
    dialog.getByRole('button', { name: /确认归属/ }).click()
  ])
  const attributeBody = await attributeResponse.json()
  assert.equal(attributeBody.code, 0, `确认归属失败: ${attributeBody.msg || attributeBody.code}`)
  assert.ok(attributeBody.data, '确认归属必须返回正式报工 ID。')
  return { feedbackId: attributeBody.data, selected }
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(page)
    await openFeedbackPage(page)
    const importRecordId = await importExcel(page)
    const { feedbackId, selected } = await attributeImportRecord(page, importRecordId)
    console.log(JSON.stringify({
      status: 'PASS',
      tenant: config.tenant,
      importRecordId,
      feedbackId,
      scheduleOrderId: selected.scheduleOrderId,
      scheduleOrderProcessId: selected.scheduleOrderProcessId,
      taskCode: selected.taskCode
    }, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
