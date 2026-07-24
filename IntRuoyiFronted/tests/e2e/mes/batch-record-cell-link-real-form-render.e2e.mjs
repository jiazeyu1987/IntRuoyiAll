import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { chromium } from 'playwright'

const zh = {
  tenant: '测试租户',
  login: '登录',
  tenantPlaceholder: '请输入租户名称',
  usernamePlaceholder: '请输入用户名',
  passwordPlaceholder: '请输入密码',
  pageTitle: '批记录单元格链接'
}

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_FORM_RENDER_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.BATCH_RECORD_CELL_LINK_FORM_RENDER_TENANT || zh.tenant,
  username: process.env.BATCH_RECORD_CELL_LINK_FORM_RENDER_USERNAME || 'aoteman',
  password: process.env.BATCH_RECORD_CELL_LINK_FORM_RENDER_PASSWORD || '111111',
  sourceReportId:
    process.env.BATCH_RECORD_CELL_LINK_FORM_RENDER_SOURCE_REPORT_ID ||
    'c37d43d73f484c77a8dc5b19c4c5bd86',
  taskDir:
    process.env.BATCH_RECORD_CELL_LINK_FORM_RENDER_TASK_DIR ||
    'D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260712-batch-record-cell-link-question-placeholder/e2e-artifacts'
}

const screenshots = {
  loginFailed: path.join(config.taskDir, 'login-failed.png'),
  pageFailed: path.join(config.taskDir, 'real-form-render-failed.png'),
  passed: path.join(config.taskDir, 'real-form-render-passed.png')
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(`visible_input_missing:${label}`)
}

async function selectTenant(page, loginForm) {
  const tenantInput = loginForm
    .locator(`.el-select input[role="combobox"], input.el-select__input, input[placeholder="${zh.tenantPlaceholder}"]`)
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
}

async function getPageProbe(page) {
  return page.evaluate(() => ({
    url: window.location.href,
    body: (document.body.innerText || '').slice(0, 1500)
  }))
}

async function login(page) {
  const targetPath = `/mes/pro/batch-record-cell-link?sourceReportId=${encodeURIComponent(config.sourceReportId)}`
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

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, loginForm)

  const textboxes = loginForm.getByRole('textbox')
  const textboxCount = await textboxes.count()
  if (textboxCount >= 2) {
    const usernameInput = textboxes.nth(textboxCount >= 3 ? 1 : 0)
    await usernameInput.fill('')
    await usernameInput.fill(config.username)
  } else {
    await fillFirstVisible(loginForm.locator(`input[placeholder="${zh.usernamePlaceholder}"]`), config.username, 'username')
  }
  await fillFirstVisible(
    loginForm.locator(`input[type="password"], input[placeholder="${zh.passwordPlaceholder}"]`),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const loginButton = loginForm.getByRole('button', { name: zh.login }).first()
  await loginButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await loginButton.isEnabled(), true, 'login button must be enabled')
  await loginButton.click()

  let loginResponse
  try {
    loginResponse = await loginResponsePromise
  } catch (error) {
    await page.screenshot({ path: screenshots.loginFailed, fullPage: true }).catch(() => null)
    const probe = await getPageProbe(page).catch((probeError) => ({ probeError: probeError.message }))
    throw new Error(`login_response_timeout:${error.message}; probe=${JSON.stringify(probe)}; screenshot=${screenshots.loginFailed}`)
  }
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openWorkbench(page) {
  const targetPath = `/mes/pro/batch-record-cell-link?sourceReportId=${encodeURIComponent(config.sourceReportId)}`
  const contextPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-cell-link/workbench-context') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}${targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText(zh.pageTitle).first().waitFor({ state: 'visible', timeout: 60000 })
  const contextPayload = await (await contextPromise).json()
  assert.ok(isSuccessPayload(contextPayload), `workbench context failed: ${JSON.stringify(contextPayload)}`)
  assert.ok(Array.isArray(contextPayload.data?.forms) && contextPayload.data.forms.length >= 2, 'need at least two real batch record forms')
  await page.locator('.batch-record-cell-link__pane.is-source table.batch-record-cell-link-sheet').waitFor({
    state: 'visible',
    timeout: 60000
  })
  await page.locator('.batch-record-cell-link__pane.is-target table.batch-record-cell-link-sheet').waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page, 30000)
  return contextPayload.data
}

async function assertRealFormRender(page) {
  const probe = await page.evaluate(() => {
    const readPane = (selector) => {
      const pane = document.querySelector(selector)
      const scroller = pane?.querySelector('.batch-record-cell-link__sheet-scroll')
      const sheet = pane?.querySelector('table.batch-record-cell-link-sheet')
      const firstCell = sheet?.querySelector('td')
      const sheetStyle = sheet ? window.getComputedStyle(sheet) : null
      const cellStyle = firstCell ? window.getComputedStyle(firstCell) : null
      const scrollerRect = scroller?.getBoundingClientRect()
      const sheetRect = sheet?.getBoundingClientRect()
      const scrollerClientWidth = scroller?.clientWidth || 0
      const scrollerScrollWidth = scroller?.scrollWidth || 0
      const sheetWidth = sheetRect?.width || 0
      const fillableTexts = Array.from(
        pane?.querySelectorAll('.batch-record-cell-link-sheet__cell.is-fillable-cell') || []
      ).map((cell) => (cell.textContent || '').trim())
      return {
        hasSheet: Boolean(sheet),
        colCount: sheet?.querySelectorAll('colgroup col').length || 0,
        rowCount: sheet?.querySelectorAll('tbody tr').length || 0,
        cellCount: sheet?.querySelectorAll('td').length || 0,
        selectableCount: pane?.querySelectorAll('.batch-record-cell-link-sheet__cell.is-source-selectable, .batch-record-cell-link-sheet__cell.is-target-selectable').length || 0,
        borderCollapse: sheetStyle?.borderCollapse || '',
        tableLayout: sheetStyle?.tableLayout || '',
        minWidth: sheetStyle?.minWidth || '',
        maxWidth: sheetStyle?.maxWidth || '',
        scrollerClientWidth,
        scrollerScrollWidth,
        scrollerWidth: scrollerRect?.width || 0,
        sheetWidth,
        fitsPane: Boolean(sheet) && sheetWidth <= scrollerClientWidth + 2 && scrollerScrollWidth <= scrollerClientWidth + 2,
        questionPlaceholderCount: fillableTexts.filter((text) => text === '?').length,
        oldFillPlaceholderCount: fillableTexts.filter((text) => text === '填').length,
        borderTopStyle: cellStyle?.borderTopStyle || '',
        borderTopWidth: cellStyle?.borderTopWidth || ''
      }
    }
    const stage = document.querySelector('.batch-record-cell-link__form-stage')
    const stageStyle = stage ? window.getComputedStyle(stage) : null
    return {
      sheetCount: document.querySelectorAll('.batch-record-cell-link__form-stage table.batch-record-cell-link-sheet').length,
      stageColumns: stageStyle?.gridTemplateColumns || '',
      source: readPane('.batch-record-cell-link__pane.is-source'),
      target: readPane('.batch-record-cell-link__pane.is-target')
    }
  })

  assert.equal(probe.sheetCount, 2, `main area must render exactly two real form sheets: ${JSON.stringify(probe)}`)
  for (const [name, pane] of Object.entries({ source: probe.source, target: probe.target })) {
    assert.equal(pane.hasSheet, true, `${name} pane must have a table sheet`)
    assert.ok(pane.colCount >= 6, `${name} pane must render real colgroup columns: ${JSON.stringify(pane)}`)
    assert.ok(pane.rowCount >= 10, `${name} pane must render real form rows: ${JSON.stringify(pane)}`)
    assert.ok(pane.cellCount >= 20, `${name} pane must render real grid cells: ${JSON.stringify(pane)}`)
    assert.ok(pane.selectableCount > 0, `${name} pane must keep selectable fillable cells: ${JSON.stringify(pane)}`)
    assert.equal(pane.borderCollapse, 'collapse', `${name} sheet must use collapsed form borders`)
    assert.equal(pane.tableLayout, 'fixed', `${name} sheet must keep fixed form layout`)
    assert.equal(pane.minWidth, '0px', `${name} sheet must not keep a fixed minimum width`)
    assert.equal(pane.fitsPane, true, `${name} sheet must fit inside its pane width: ${JSON.stringify(pane)}`)
    assert.ok(pane.questionPlaceholderCount > 0, `${name} sheet must render empty fillable cells as centered question marks`)
    assert.equal(pane.oldFillPlaceholderCount, 0, `${name} sheet must not render old 填 placeholders`)
    assert.equal(pane.borderTopStyle, 'solid', `${name} sheet cells must have visible borders`)
    assert.notEqual(pane.borderTopWidth, '0px', `${name} sheet cell border width must be visible`)
  }
  return probe
}

async function main() {
  if (config.tenant !== zh.tenant || config.username !== 'aoteman') {
    throw new Error(`real_form_render_e2e_must_use_test_tenant_aoteman:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`)
  }
  fs.mkdirSync(config.taskDir, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } })
  try {
    await login(page)
    const context = await openWorkbench(page)
    const probe = await assertRealFormRender(page)
    await page.screenshot({ path: screenshots.passed, fullPage: true })
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          forms: context.forms.length,
          probe,
          screenshot: screenshots.passed
        },
        null,
        2
      )
    )
  } catch (error) {
    await page.screenshot({ path: screenshots.pageFailed, fullPage: true }).catch(() => null)
    const probe = await getPageProbe(page).catch((probeError) => ({ probeError: probeError.message }))
    throw new Error(`${error.message}; probe=${JSON.stringify(probe)}; screenshot=${screenshots.pageFailed}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
