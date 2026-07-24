const { chromium } = require('playwright')
const assert = require('node:assert/strict')

const BASE_URL = 'http://localhost:8081'
const TENANT = '芋道源码'
const USERNAME = 'admin'
const PASSWORD = 'admin123'
const BATCH_ID = 32
const BATCH_CODE = 'E2E-881MO090863-20260610-104136'
const REMAINING_SORTS = [8, 9, 10, 16, 17, 18, 19, 21]

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function login(page) {
  await page.goto(
    `${BASE_URL}/login?redirect=${encodeURIComponent(`/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)}`,
    { waitUntil: 'domcontentloaded', timeout: 90000 }
  )
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  const textInputs = loginForm.locator('input:not([type="hidden"]):not([type="password"])')
  const pwdInput = loginForm.locator('input[type="password"]').first()
  const count = await textInputs.count()
  if (count >= 2) {
    try {
      await textInputs.nth(0).click({ timeout: 1000 })
      await textInputs.nth(0).fill(TENANT)
      await page.keyboard.press('Enter')
    } catch {}
    await textInputs.nth(count - 1).fill(USERNAME)
  } else if (count === 1) {
    await textInputs.nth(0).fill(USERNAME)
  }
  await pwdInput.fill(PASSWORD)
  await loginForm.locator('button').last().click()
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 90000 })
}

async function openBatchDetail(page) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await page.waitForTimeout(4000)
}

async function openTask(page, sort) {
  await openBatchDetail(page)
  const rows = page.locator('.el-table__row')
  const row = rows.nth(sort - 1)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const text = await row.innerText()
  if (text.includes('已批准')) {
    return { alreadyApproved: true }
  }
  await row.locator('button').nth(0).click({ force: true })
  await page.waitForTimeout(5000)
  const summary = await page.locator('.edhr-page-shell__summary').innerText()
  const executionCode = (summary.split(/\s+/).find((v) => /^BRE/.test(v)) || '').trim()
  const executionId = Number(new URL(page.url()).searchParams.get('id'))
  assert.ok(executionId > 0, `sort ${sort} 未拿到 executionId`)
  return { alreadyApproved: false, executionId, executionCode }
}

async function fetchExecutionDetail(page, executionId) {
  return await page.evaluate(async ({ executionId }) => {
    const raw =
      window.localStorage.getItem('ACCESS_TOKEN') ||
      window.localStorage.getItem('accessToken') ||
      window.sessionStorage.getItem('ACCESS_TOKEN') ||
      window.sessionStorage.getItem('accessToken') ||
      ''
    let token = raw
    try {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object' && 'v' in parsed) {
        token = JSON.parse(parsed.v || '""') || ''
      }
    } catch {}
    const response = await fetch(`/admin-api/mes/pro/batch-record-execution/get?id=${executionId}`, {
      credentials: 'include',
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!response.ok) throw new Error(`execution get HTTP ${response.status}`)
    const json = await response.json()
    if (json.code !== 0) throw new Error(json.msg || json.message || `execution get business ${json.code}`)
    return json.data
  }, { executionId })
}

async function fillCurrentExecution(page, prefix) {
  const inputs = page.locator('.edhr-page-shell__form input:not([type="hidden"]), .edhr-page-shell__form textarea')
  const count = await inputs.count()
  let filled = 0
  for (let i = 0; i < count; i += 1) {
    const input = inputs.nth(i)
    if (!(await input.isVisible())) continue
    if (!(await input.isEnabled())) continue
    const ro = await input.evaluate((el) => el.hasAttribute('readonly'))
    const dis = await input.evaluate((el) => el.hasAttribute('disabled'))
    if (ro || dis) continue
    await input.fill(`${prefix}-${filled}`)
    filled += 1
  }
  await delay(800)
  return filled
}

async function saveFieldAuditIfPresent(page, prefix) {
  const pendingRows = page.locator('.edhr-page-shell__field-audit-table .el-table__row')
  if ((await pendingRows.count()) === 0) return false
  const reasonArea = page.locator('.edhr-page-shell__field-audit-reason').first()
  await reasonArea.locator('.el-select__wrapper').first().click({ force: true })
  await page.keyboard.press('ArrowDown')
  await page.keyboard.press('Enter')
  await reasonArea.locator('.el-form-item').last().locator('input:not([type="hidden"])').first().fill(`${prefix}-REASON`)
  await page.keyboard.press('Tab')
  await delay(1000)
  const saveButton = page.locator('.edhr-page-shell__field-audit .edhr-page-shell__section-actions .el-button').first()
  const saveRespPromise = page.waitForResponse(
    (r) => r.url().includes('/field-audit/save-changes') && r.request().method() === 'PUT',
    { timeout: 90000 }
  )
  await saveButton.click({ force: true })
  const dialog = page.locator('.el-dialog').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[type="password"]').first().fill(PASSWORD)
  await dialog.locator('.el-dialog__footer .el-button').last().click({ force: true })
  const resp = await saveRespPromise
  const json = await resp.json()
  assert.equal(json.code, 0, `字段审计保存失败: ${json.msg || json.message}`)
  assert.equal(json.data?.hashVerification?.status, 'VALID', '字段审计链校验必须为 VALID')
  await delay(1000)
  return true
}

async function formReviewSign(page, prefix) {
  const toolbar = page.locator('.edhr-page-shell__actions .el-button')
  await toolbar.nth(3).click({ force: true })
  const dialog = page.locator('.el-dialog').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[type="password"]').first().fill(PASSWORD)
  await dialog.locator('textarea').first().fill(`${prefix}-REVIEW`)
  const respPromise = page.waitForResponse(
    (r) => r.url().includes('/cosign') && r.request().method() === 'PUT',
    { timeout: 90000 }
  )
  await dialog.locator('.el-dialog__footer .el-button').last().click({ force: true })
  const resp = await respPromise
  const json = await resp.json()
  assert.equal(json.code, 0, `复核签名失败: ${json.msg || json.message}`)
  await delay(1000)
}

async function verifyDomainTrace(page) {
  const toolbar = page.locator('.edhr-page-shell__actions .el-button')
  await toolbar.nth(6).click({ force: true })
  await page.waitForURL((url) => url.pathname.includes('/edhr-domain-trace/detail'), { timeout: 90000 })
  await delay(3000)
  const verifyRespPromise = page.waitForResponse(
    (r) => r.url().includes('/domain-trace/verify') && r.request().method() === 'POST',
    { timeout: 90000 }
  )
  await page.locator('button').nth(2).click({ force: true })
  const resp = await verifyRespPromise
  const json = await resp.json()
  assert.equal(json.code, 0, `主数据追溯校验失败: ${json.msg || json.message}`)
  assert.equal(json.data?.status, 'VERIFIED', '主数据追溯必须 VERIFIED')
}

async function submitExecution(page, executionId, prefix) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-execution/detail?id=${executionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await delay(3000)
  const toolbar = page.locator('.edhr-page-shell__actions .el-button')
  const respPromise = page.waitForResponse(
    (r) => r.url().includes('/submit') && r.request().method() === 'PUT',
    { timeout: 90000 }
  )
  await toolbar.nth(4).click({ force: true })
  const dialog = page.locator('.el-dialog').last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[type="password"]').first().fill(PASSWORD)
  await dialog.locator('textarea').first().fill(`${prefix}-SUBMIT`)
  await dialog.locator('.el-dialog__footer .el-button').last().click({ force: true })
  const resp = await respPromise
  const json = await resp.json()
  assert.equal(json.code, 0, `提交执行失败: ${json.msg || json.message}`)
}

async function approveExecution(page, executionCode, prefix) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-approval?tab=pending`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await delay(3000)
  await page.locator('.edhr-workbench__toolbar input').first().fill(executionCode)
  const pendingRespPromise = page.waitForResponse(
    (r) => r.url().includes('/approval-pending-page') && r.request().method() === 'GET',
    { timeout: 90000 }
  )
  await page.locator('.edhr-workbench__toolbar .el-button').nth(0).click({ force: true })
  const pending = await pendingRespPromise
  const json = await pending.json()
  assert.equal(json.code, 0, `审批列表查询失败: ${json.msg || json.message}`)
  const row = page.locator('.el-table__row').filter({ hasText: executionCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const passwordBefore = await page.locator('input[type="password"]:visible').count()
  await row.locator('button').nth(1).click({ force: true })
  await page.waitForFunction(
    (beforeCount) => {
      const visiblePasswordInputs = Array.from(document.querySelectorAll('input[type="password"]')).filter(
        (node) => !!(node.offsetWidth || node.offsetHeight || node.getClientRects().length)
      )
      return visiblePasswordInputs.length > beforeCount
    },
    passwordBefore,
    { timeout: 30000 }
  )
  await page.evaluate(
    ({ password, comment }) => {
      const visibleDialogs = Array.from(document.querySelectorAll('.el-dialog')).filter(
        (node) => !!(node.offsetWidth || node.offsetHeight || node.getClientRects().length)
      )
      const dialog = visibleDialogs[visibleDialogs.length - 1]
      if (!dialog) throw new Error('找不到可见审批弹窗')
      const passwordInputs = dialog.querySelectorAll('input[type="password"]')
      const commentInputs = dialog.querySelectorAll('textarea')
      const passwordInput = passwordInputs[passwordInputs.length - 1]
      const commentInput = commentInputs[commentInputs.length - 1]
      if (!passwordInput || !commentInput) throw new Error('审批弹窗缺少输入控件')
      passwordInput.value = password
      passwordInput.dispatchEvent(new Event('input', { bubbles: true }))
      passwordInput.dispatchEvent(new Event('change', { bubbles: true }))
      commentInput.value = comment
      commentInput.dispatchEvent(new Event('input', { bubbles: true }))
      commentInput.dispatchEvent(new Event('change', { bubbles: true }))
    },
    { password: PASSWORD, comment: `${prefix}-APPROVE` }
  )
  const respPromise = page.waitForResponse(
    (r) => r.url().includes('/approve') && r.request().method() === 'PUT',
    { timeout: 90000 }
  )
  await page.evaluate(() => {
    const visibleDialogs = Array.from(document.querySelectorAll('.el-dialog')).filter(
      (node) => !!(node.offsetWidth || node.offsetHeight || node.getClientRects().length)
    )
    const dialog = visibleDialogs[visibleDialogs.length - 1]
    if (!dialog) throw new Error('找不到可见审批弹窗')
    const buttons = dialog.querySelectorAll('.el-dialog__footer .el-button')
    const confirmButton = buttons[buttons.length - 1]
    if (!confirmButton) throw new Error('审批弹窗缺少确认按钮')
    confirmButton.click()
  })
  const resp = await respPromise
  const respJson = await resp.json()
  assert.equal(respJson.code, 0, `审批通过失败: ${respJson.msg || respJson.message}`)
}

async function syncBatch(page) {
  await openBatchDetail(page)
  const syncButton = page.locator('.edhr-batch-detail__commands .el-button').nth(1)
  await syncButton.click({ force: true })
  await delay(3000)
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const page = await context.newPage()
  try {
    await login(page)
    for (const sort of REMAINING_SORTS) {
      const prefix = `RESUME-${BATCH_CODE}-S${sort}`
      const opened = await openTask(page, sort)
      if (opened.alreadyApproved) continue
      const execution = await fetchExecutionDetail(page, opened.executionId)
      if (Number(execution.status) === 0) {
        await fillCurrentExecution(page, prefix)
        await saveFieldAuditIfPresent(page, prefix)
        await formReviewSign(page, prefix)
        await verifyDomainTrace(page)
        await submitExecution(page, opened.executionId, prefix)
      } else if (Number(execution.status) === 1) {
        // already submitted, continue with approval
      } else if (Number(execution.status) === 3) {
        await syncBatch(page)
        continue
      } else {
        throw new Error(`sort ${sort} 命中未覆盖执行状态 ${execution.status}`)
      }
      await approveExecution(page, opened.executionCode, prefix)
      await syncBatch(page)
    }
    await openBatchDetail(page)
    await page.screenshot({ path: 'D:/ProjectPackage/Int/IntRuoyi/worktrees/edhr_e2e_v2/yudao-ui-admin-vue3/test-results/batch32-resume-finished.png', fullPage: true })
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
