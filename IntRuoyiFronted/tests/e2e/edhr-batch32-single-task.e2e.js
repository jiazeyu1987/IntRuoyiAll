const { chromium } = require('playwright')

const BASE_URL = 'http://localhost:8081'
const TENANT = '芋道源码'
const USERNAME = 'admin'
const PASSWORD = 'admin123'
const BATCH_ID = 32

const sort = Number(process.env.EDHR_SINGLE_SORT || '17')
const code = process.env.EDHR_SINGLE_CODE || 'B240'
const prefix = `SINGLE-${code}`

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(`/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
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

async function openTask(page) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await delay(3000)
  const row = page.locator('.el-table__row').filter({ hasText: code }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const text = await row.innerText()
  console.log(text)
  if (text.includes('已批准')) {
    return { alreadyApproved: true }
  }
  await row.locator('button').nth(0).click({ force: true })
  await delay(4000)
  const executionId = Number(new URL(page.url()).searchParams.get('id'))
  const summary = await page.locator('.edhr-page-shell__summary').innerText()
  const executionCode = (summary.split(/\s+/).find((v) => /^BRE/.test(v)) || '').trim()
  return { alreadyApproved: false, executionId, executionCode }
}

async function fillAndSubmit(page) {
  const inputs = page.locator('.edhr-page-shell__form input:not([type="hidden"]), .edhr-page-shell__form textarea')
  const count = await inputs.count()
  let filled = 0
  const currentExecutionId = Number(new URL(page.url()).searchParams.get('id'))
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
  const pendingRows = await page.locator('.edhr-page-shell__field-audit-table .el-table__row').count()
  if (pendingRows > 0) {
    const reasonArea = page.locator('.edhr-page-shell__field-audit-reason').first()
    await reasonArea.locator('.el-select__wrapper').first().click({ force: true })
    await page.keyboard.press('ArrowDown')
    await page.keyboard.press('Enter')
    await reasonArea.locator('.el-form-item').last().locator('input:not([type="hidden"])').first().fill(`${prefix}-REASON`)
    await page.keyboard.press('Tab')
    await delay(800)
    const saveRespPromise = page.waitForResponse((r) => r.url().includes('/field-audit/save-changes') && r.request().method() === 'PUT', { timeout: 90000 })
    await page.locator('.edhr-page-shell__field-audit .edhr-page-shell__section-actions .el-button').first().click({ force: true })
    await delay(1000)
    await page.locator('input[type="password"]').last().fill(PASSWORD)
    const saveConfirm = page.locator('.el-dialog__footer .el-button').last()
    await saveConfirm.click({ force: true })
    console.log('save', await (await saveRespPromise).text())
    await delay(1000)
  }
  await page.locator('.edhr-page-shell__actions .el-button').nth(3).click({ force: true })
  await delay(1000)
  await page.locator('input[type="password"]').last().fill(PASSWORD)
  await page.locator('textarea').last().fill(`${prefix}-REVIEW`)
  const reviewRespPromise = page.waitForResponse((r) => r.url().includes('/cosign') && r.request().method() === 'PUT', { timeout: 90000 })
  await page.locator('.el-dialog__footer .el-button').last().click({ force: true })
  console.log('review', await (await reviewRespPromise).text())
  await page.locator('.edhr-page-shell__actions .el-button').nth(6).click({ force: true })
  try {
    await page.waitForURL((url) => url.pathname.includes('/edhr-domain-trace/detail'), { timeout: 8000 })
  } catch {
    await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-domain-trace/detail?executionId=${currentExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    })
  }
  await delay(1500)
  const verifyRespPromise = page.waitForResponse((r) => r.url().includes('/domain-trace/verify') && r.request().method() === 'POST', { timeout: 90000 })
  await page.locator('button').nth(2).click({ force: true })
  console.log('verify', await (await verifyRespPromise).text())
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-execution/detail?id=${currentExecutionId}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await delay(2000)
  const submitRespPromise = page.waitForResponse((r) => r.url().includes('/submit') && r.request().method() === 'PUT', { timeout: 90000 })
  await page.locator('.edhr-page-shell__actions .el-button').nth(4).click({ force: true })
  await delay(1000)
  await page.locator('input[type="password"]').last().fill(PASSWORD)
  await page.locator('textarea').last().fill(`${prefix}-SUBMIT`)
  await page.locator('.el-dialog__footer .el-button').last().click({ force: true })
  console.log('submit', await (await submitRespPromise).text())
}

async function approve(page, executionCode) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-approval?tab=pending`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await delay(2000)
  await page.locator('.edhr-workbench__toolbar input').first().fill(executionCode)
  const pendingRespPromise = page.waitForResponse((r) => r.url().includes('/approval-pending-page') && r.request().method() === 'GET', { timeout: 90000 })
  await page.locator('.edhr-workbench__toolbar .el-button').nth(0).click({ force: true })
  console.log('pending', await (await pendingRespPromise).text())
  await delay(1500)
  await page.locator('.el-table__row').first().locator('button').nth(1).click({ force: true })
  await delay(1500)
  await page.locator('input[type="password"]').last().fill(PASSWORD)
  await page.locator('textarea').last().fill(`${prefix}-APPROVE`)
  const approveRespPromise = page.waitForResponse((r) => r.url().includes('/approve') && r.request().method() === 'PUT', { timeout: 90000 })
  await page.locator('.el-dialog__footer .el-button').last().click({ force: true })
  console.log('approve', await (await approveRespPromise).text())
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page)
    const opened = await openTask(page)
    if (!opened.alreadyApproved) {
      await fillAndSubmit(page)
      await approve(page, opened.executionCode)
    }
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
