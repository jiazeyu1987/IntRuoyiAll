const { chromium } = require('playwright')

const BASE_URL = 'http://localhost:8081'
const TENANT = '芋道源码'
const USERNAME = 'admin'
const PASSWORD = 'admin123'
const EXECUTION_CODE = 'BRE202606101202577530183'

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent('/mes/pro/feedback/edhr-approval?tab=pending')}`, {
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

async function openApproveDialog(page) {
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-approval?tab=pending`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  await delay(2500)
  await page.locator('.edhr-workbench__toolbar input').first().fill(EXECUTION_CODE)
  await page.locator('.edhr-workbench__toolbar .el-button').nth(0).click({ force: true })
  await delay(2500)
  const row = page.locator('.el-table__row').filter({ hasText: EXECUTION_CODE }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.locator('button').nth(1).click({ force: true })
}

async function findApproveDialog(page) {
  return await page.evaluate(() => {
    const dialogs = Array.from(document.querySelectorAll('.el-dialog'))
    const dialog = dialogs.find((node) => {
      const text = node.textContent || ''
      const visible = !!(node.offsetWidth || node.offsetHeight || node.getClientRects().length)
      return visible && text.includes('通过 eDHR 审批')
    })
    return dialog ? true : false
  })
}

async function approveOnce(page) {
  for (let attempt = 1; attempt <= 4; attempt += 1) {
    await openApproveDialog(page)
    let found = false
    for (let i = 0; i < 20; i += 1) {
      found = await findApproveDialog(page)
      if (found) break
      await delay(500)
    }
    if (!found) {
      await page.screenshot({
        path: `D:/ProjectPackage/Int/IntRuoyi/worktrees/edhr_e2e_v2/yudao-ui-admin-vue3/test-results/b230-approve-miss-attempt-${attempt}.png`,
        fullPage: true
      })
      await page.keyboard.press('Escape').catch(() => {})
      await delay(800)
      continue
    }
    const respPromise = page.waitForResponse(
      (r) => r.url().includes('/approve') && r.request().method() === 'PUT',
      { timeout: 90000 }
    )
    await page.evaluate(({ password, comment }) => {
      const dialogs = Array.from(document.querySelectorAll('.el-dialog'))
      const dialog = dialogs.find((node) => {
        const text = node.textContent || ''
        const visible = !!(node.offsetWidth || node.offsetHeight || node.getClientRects().length)
        return visible && text.includes('通过 eDHR 审批')
      })
      if (!dialog) throw new Error('找不到审批弹窗')
      const pwd = dialog.querySelector('input[type="password"]')
      const txt = dialog.querySelector('textarea')
      const buttons = dialog.querySelectorAll('.el-dialog__footer .el-button')
      if (!pwd || !txt || buttons.length < 2) throw new Error('审批弹窗控件缺失')
      pwd.value = password
      pwd.dispatchEvent(new Event('input', { bubbles: true }))
      pwd.dispatchEvent(new Event('change', { bubbles: true }))
      txt.value = comment
      txt.dispatchEvent(new Event('input', { bubbles: true }))
      txt.dispatchEvent(new Event('change', { bubbles: true }))
      buttons[buttons.length - 1].click()
    }, { password: PASSWORD, comment: `B230-APPROVE-attempt-${attempt}` })
    const resp = await respPromise
    const json = await resp.json()
    console.log(JSON.stringify({ attempt, json }, null, 2))
    if (json.code === 0) return
    throw new Error(json.msg || json.message || `approve business ${json.code}`)
  }
  throw new Error('B230 审批确认弹窗在 4 次重试后仍未稳定出现')
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  try {
    await login(page)
    await approveOnce(page)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
