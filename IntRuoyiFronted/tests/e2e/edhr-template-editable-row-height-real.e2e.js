const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_ROW_HEIGHT_E2E_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_ROW_HEIGHT_E2E_TENANT || '测试租户'
const USERNAME = process.env.EDHR_ROW_HEIGHT_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_ROW_HEIGHT_E2E_PASSWORD
const EXECUTABLE_PATH = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
const RESULT_DIR = path.resolve(process.cwd(), 'tests/output/20260713-edhr-row-height')

function requirePrerequisites() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实 E2E 只允许本机前端入口')
  assert(PASSWORD, '缺少 EDHR_ROW_HEIGHT_E2E_PASSWORD')
  assert(EXECUTABLE_PATH, '缺少 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
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
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), TENANT, '租户')
  }
  await fillFirstVisible(
    loginForm.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    USERNAME,
    '账号'
  )
  await fillFirstVisible(loginForm.locator('input[type="password"]'), PASSWORD, '密码')
  const loginResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: /^登录$/ }).click()
  const loginBody = await (await loginResponse).json()
  assert(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

function unwrapResponse(body, label) {
  assert(body.code === 0 || body.code === 200, `${label}失败：${body.msg || body.code}`)
  return body.data
}

function extractRecords(pageData) {
  return Array.isArray(pageData?.list)
    ? pageData.list
    : Array.isArray(pageData?.records)
      ? pageData.records
      : []
}

async function findExecutionContextInRecords(page, records) {
  for (const record of records) {
    const batchExecutionId = Number(record.id)
    if (!Number.isFinite(batchExecutionId) || batchExecutionId <= 0) continue
    const detailResponse = page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/mes/pro/edhr-batch-execution/get?id=${batchExecutionId}`) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${batchExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const detail = unwrapResponse(await (await detailResponse).json(), `批次 ${batchExecutionId}`)
    const task = (detail.tasks || []).find((item) => Number(item.executionId) > 0)
    if (!task) continue
    return {
      batchExecutionId,
      batchCode: detail.batchCode,
      batchTaskId: task.id,
      workTaskId: task.workTaskId,
      executionId: task.executionId
    }
  }
  return undefined
}

async function findExecutionContext(page) {
  const pageResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  let pageData = unwrapResponse(await (await pageResponse).json(), '批次列表加载')
  let records = extractRecords(pageData)
  assert(records.length > 0, '当前租户没有可用于只读验证的批次执行记录')
  const total = Number(pageData?.total || records.length)

  let found = await findExecutionContextInRecords(page, records)
  if (found) return found

  if (total > records.length) {
    await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const sizeSelect = page.locator('.el-pagination__sizes .el-select').first()
    if ((await sizeSelect.count()) > 0 && (await sizeSelect.isVisible())) {
      const expandedResponse = page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/edhr-batch-execution/page') &&
          response.url().includes('pageSize=100') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await sizeSelect.click()
      const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: /^100/ }).first()
      await option.waitFor({ state: 'visible', timeout: 30000 })
      await option.click()
      pageData = unwrapResponse(await (await expandedResponse).json(), '批次列表 100 条加载')
      records = extractRecords(pageData)
      found = await findExecutionContextInRecords(page, records)
      if (found) return found
    }

    for (let pageIndex = 2; pageIndex <= 5; pageIndex += 1) {
      await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution`, {
        waitUntil: 'domcontentloaded',
        timeout: 60000
      })
      const nextButton = page.locator('.el-pagination button.btn-next:not([disabled])').first()
      if ((await nextButton.count()) === 0 || !(await nextButton.isVisible())) break
      const nextResponse = page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/edhr-batch-execution/page') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await nextButton.click()
      pageData = unwrapResponse(await (await nextResponse).json(), `批次列表第 ${pageIndex} 页加载`)
      records = extractRecords(pageData)
      found = await findExecutionContextInRecords(page, records)
      if (found) return found
    }
  }

  throw new Error('当前租户已扫描批次列表前 5 页，仍没有带执行记录的表单任务，无法做行高只读验证')
}

async function verifyEditableRowHeight(page, context) {
  const query = new URLSearchParams({
    id: String(context.executionId),
    executionId: String(context.executionId),
    batchExecutionId: String(context.batchExecutionId),
    batchTaskId: String(context.batchTaskId),
    returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'
  })
  if (context.workTaskId) query.set('workTaskId', String(context.workTaskId))

  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-execution/form?${query}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const sheet = page.locator('.edhr-template-editable-form__sheet').first()
  await sheet.waitFor({ state: 'visible', timeout: 60000 })

  const result = await sheet.evaluate((element) => {
    const rows = Array.from(element.querySelectorAll('tbody tr'))
    const editableRows = rows.filter((row) => row.querySelector('td.is-editable'))
    const violations = []
    const heights = []

    editableRows.forEach((row, rowIndex) => {
      const configuredHeight = Number.parseFloat(row.style.height || '0')
      heights.push(configuredHeight)
      if (configuredHeight < 48) {
        violations.push(`row=${rowIndex} configuredHeight=${configuredHeight}`)
      }
      Array.from(row.querySelectorAll('td.is-editable')).forEach((cell, cellIndex) => {
        const field = cell.querySelector(
          '.edhr-fill-workspace__field, .edhr-template-editable-form__editable-cell'
        )
        if (!field) return
        const cellRect = cell.getBoundingClientRect()
        const fieldRect = field.getBoundingClientRect()
        if (fieldRect.top < cellRect.top - 2 || fieldRect.bottom > cellRect.bottom + 2) {
          violations.push(
            `row=${rowIndex} cell=${cellIndex} fieldTop=${fieldRect.top.toFixed(1)} fieldBottom=${fieldRect.bottom.toFixed(1)} cellTop=${cellRect.top.toFixed(1)} cellBottom=${cellRect.bottom.toFixed(1)}`
          )
        }
      })
    })

    return {
      editableRowCount: editableRows.length,
      minConfiguredHeight: heights.length ? Math.min(...heights) : 0,
      sampleHeights: heights.slice(0, 12),
      violations
    }
  })

  assert(result.editableRowCount > 0, '当前表单没有可编辑单元格，无法验证“请填写”行高')
  assert.deepEqual(result.violations, [], `可编辑行仍有重叠或行高不足：${JSON.stringify(result)}`)
  await page.screenshot({
    path: path.join(RESULT_DIR, 'editable-row-height.png'),
    fullPage: true
  })
  return result
}

async function main() {
  requirePrerequisites()
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: EXECUTABLE_PATH })
  const context = await browser.newContext({ viewport: { width: 1366, height: 768 } })
  const page = await context.newPage()
  const writeRequests = []
  page.on('request', (request) => {
    const method = request.method()
    if (/^(POST|PUT|PATCH|DELETE)$/i.test(method) && request.url().includes('/admin-api/mes/')) {
      writeRequests.push(`${method} ${request.url()}`)
    }
  })
  try {
    await login(page)
    const executionContext = await findExecutionContext(page)
    const result = await verifyEditableRowHeight(page, executionContext)
    assert.deepEqual(writeRequests, [], `行高只读验证不应发送 MES 写请求：${JSON.stringify(writeRequests)}`)
    console.log(
      `PASS: eDHR editable row height real E2E tenant=${TENANT} username=${USERNAME} batch=${executionContext.batchExecutionId} execution=${executionContext.executionId} editableRows=${result.editableRowCount} minHeight=${result.minConfiguredHeight}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
