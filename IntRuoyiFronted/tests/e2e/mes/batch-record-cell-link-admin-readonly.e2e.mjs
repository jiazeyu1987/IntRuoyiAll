import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { chromium } from 'playwright'

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_ADMIN_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.BATCH_RECORD_CELL_LINK_ADMIN_TENANT || '芋道源码',
  username: process.env.BATCH_RECORD_CELL_LINK_ADMIN_USERNAME || 'admin',
  password: process.env.BATCH_RECORD_CELL_LINK_ADMIN_PASSWORD || 'admin123',
  headed: process.env.BATCH_RECORD_CELL_LINK_ADMIN_HEADED === '1',
  taskDir:
    process.env.BATCH_RECORD_CELL_LINK_ADMIN_TASK_DIR ||
    'D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260713-batch-record-cell-link-toolbar-unified/e2e-artifacts'
}

const screenshots = {
  loginFailed: path.join(config.taskDir, 'admin-readonly-login-failed.png'),
  pageFailed: path.join(config.taskDir, 'admin-readonly-page-failed.png'),
  passed: path.join(config.taskDir, 'admin-readonly-passed.png')
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page) {
  const targetPath = '/mes/pro/batch-record-form-list'
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill('')
    await tenantInput.fill(config.tenant)
    await page.waitForTimeout(300)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    if (await tenantOption.count()) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await loginForm.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await loginForm.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await loginForm.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginPayload = await (await loginResponsePromise).json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `admin login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openCellLinkWorkbench(page) {
  await page.goto(`${config.baseUrl}/mes/pro/batch-record-form-list`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('批记录模板').first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page, 30000)

  const cellLinkEntry = page.getByText('单元格链接', { exact: true }).first()
  await cellLinkEntry.waitFor({ state: 'visible', timeout: 60000 })

  const contextPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-cell-link/workbench-context') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await cellLinkEntry.click()
  await page.getByText('批记录单元格链接').first().waitFor({ state: 'visible', timeout: 60000 })
  const contextPayload = await (await contextPromise).json()
  assert.ok(isSuccessPayload(contextPayload), `workbench context failed: ${JSON.stringify(contextPayload)}`)
  assert.ok(
    Array.isArray(contextPayload.data?.forms) && contextPayload.data.forms.length >= 2,
    `admin readonly needs at least two forms: ${JSON.stringify(contextPayload.data)}`
  )
  await settle(page, 30000)
}

async function assertToolbarAndDialog(page) {
  const sourceSelect = page.locator('.batch-record-cell-link__source-select').first()
  const targetSelect = page.locator('.batch-record-cell-link__target-select').first()
  const countButton = page.locator('.batch-record-cell-link__source-link-count').first()
  const createButton = page.locator('.batch-record-cell-link__create-button').first()

  await sourceSelect.waitFor({ state: 'visible', timeout: 60000 })
  await targetSelect.waitFor({ state: 'visible', timeout: 60000 })
  await countButton.waitFor({ state: 'visible', timeout: 60000 })
  await createButton.waitFor({ state: 'visible', timeout: 60000 })

  const positions = await page.evaluate(() => {
    const read = (selector) => {
      const rect = document.querySelector(selector)?.getBoundingClientRect()
      return rect ? { left: rect.left, right: rect.right, top: rect.top } : null
    }
    return {
      target: read('.batch-record-cell-link__target-select'),
      count: read('.batch-record-cell-link__source-link-count'),
      create: read('.batch-record-cell-link__create-button'),
      saveButtonCount: document.querySelectorAll('.batch-record-cell-link__save-button').length,
      oldTargetTabs: document.querySelectorAll('.batch-record-cell-link__target-tabs').length,
      oldRelationPanel: document.querySelectorAll('.batch-record-cell-link__relation-panel').length,
      oldFooter: document.querySelectorAll('.batch-record-cell-link__footer').length,
      oldSelection: document.querySelectorAll('.batch-record-cell-link__selection').length,
      oldRuleList: document.querySelectorAll('.batch-record-cell-link__rule-list').length,
      bodyText: document.body.innerText
    }
  })
  assert.ok(positions.target && positions.count && positions.create, `toolbar positions missing: ${JSON.stringify(positions)}`)
  assert.ok(
    positions.target.right <= positions.count.left && positions.count.right <= positions.create.left,
    `single create action must sit after target select and source link count: ${JSON.stringify(positions)}`
  )
  assert.equal(positions.saveButtonCount, 0, 'save rules button must be removed')
  assert.equal(positions.oldTargetTabs, 0, 'old target tabs must be removed')
  assert.equal(positions.oldRelationPanel, 0, 'old relation card strip must be removed')
  assert.equal(positions.oldFooter, 0, 'old footer must be removed')
  assert.equal(positions.oldSelection, 0, 'old current source/target cards must be removed')
  assert.equal(positions.oldRuleList, 0, 'old established-link list must be removed')
  assert.equal(positions.bodyText.includes('已建立链接'), false, 'old established-link title must be removed')
  assert.equal(positions.bodyText.includes('保存规则'), false, 'old save rules label must be removed')

  await countButton.click()
  await page.getByText('源表单链接详情').first().waitFor({ state: 'visible', timeout: 30000 })
  const detailState = await page.evaluate(() => ({
    hasRows: document.querySelectorAll('.batch-record-cell-link__detail-dialog .el-table__body-wrapper tbody tr')
      .length,
    hasEmpty: document.body.innerText.includes('暂无表单链接关系')
  }))
  assert.ok(detailState.hasRows > 0 || detailState.hasEmpty, `detail dialog must show rows or empty state: ${JSON.stringify(detailState)}`)
}

async function main() {
  assert.equal(config.tenant, '芋道源码', `admin readonly tenant must be 芋道源码, got ${config.tenant}`)
  assert.equal(config.username, 'admin', `admin readonly username must be admin, got ${config.username}`)
  fs.mkdirSync(config.taskDir, { recursive: true })

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } })
  const writeRequests = []
  page.on('request', (request) => {
    const method = request.method()
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD'].includes(method)) {
      writeRequests.push(`${method} ${request.url()}`)
    }
  })

  try {
    await login(page)
    await openCellLinkWorkbench(page)
    await assertToolbarAndDialog(page)
    assert.equal(writeRequests.length, 0, `admin readonly must not send MES write requests: ${writeRequests.join(', ')}`)
    await page.screenshot({ path: screenshots.passed, fullPage: true }).catch(() => null)
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          username: config.username,
          screenshot: screenshots.passed
        },
        null,
        2
      )
    )
  } catch (error) {
    await page.screenshot({ path: screenshots.pageFailed, fullPage: true }).catch(() => null)
    throw new Error(`${error.message}; screenshot=${screenshots.pageFailed}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
