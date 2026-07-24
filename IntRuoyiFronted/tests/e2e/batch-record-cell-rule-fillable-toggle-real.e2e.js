const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.BATCH_RECORD_CELL_RULE_TOGGLE_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.BATCH_RECORD_CELL_RULE_TOGGLE_E2E_TENANT || '测试租户',
  username: process.env.BATCH_RECORD_CELL_RULE_TOGGLE_E2E_USERNAME || 'aoteman',
  password: process.env.BATCH_RECORD_CELL_RULE_TOGGLE_E2E_PASSWORD || '111111',
  headed: process.env.BATCH_RECORD_CELL_RULE_TOGGLE_E2E_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `batch_record_cell_rule_toggle_e2e_requires_test_tenant_aoteman:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`
  )
}

const outputDir = path.resolve(__dirname, '..', 'output', 'batch-record-cell-rule-fillable-toggle')
fs.mkdirSync(outputDir, { recursive: true })

const isWriteMethod = (method) => !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())
const isMesRequest = (url) => url.includes('/admin-api/mes/') || url.includes('/mes/')

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form)
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_payload_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function pickFillableCell(preview) {
  const fillableButtons = preview.locator(
    '.batch-record-cell-rules-editor__cell.is-rule .batch-record-cell-rules-editor__cell-button'
  )
  const count = await fillableButtons.count()
  assert.ok(count > 0, 'no_fillable_blue_cell_found')

  for (let index = 0; index < count; index += 1) {
    const button = fillableButtons.nth(index)
    const text = (await button.innerText()).replace(/\s+/g, ' ').trim()
    if (text) {
      return {
        button,
        cell: button.locator('xpath=ancestor::td[contains(@class, "batch-record-cell-rules-editor__cell")]').first(),
        text
      }
    }
  }

  const button = fillableButtons.first()
  return {
    button,
    cell: button.locator('xpath=ancestor::td[contains(@class, "batch-record-cell-rules-editor__cell")]').first(),
    text: (await button.innerText()).replace(/\s+/g, ' ').trim()
  }
}

async function waitForCellClass(page, cell, className, expected) {
  const deadline = Date.now() + 30000
  while (Date.now() < deadline) {
    const current = await cell.evaluate((element, targetClass) => element.classList.contains(targetClass), className)
      .catch(() => null)
    if (current === expected) return
    await page.waitForTimeout(200)
  }
  const classList = await cell.evaluate((element) => Array.from(element.classList)).catch(() => [])
  assert.fail(`cell_class_timeout:${JSON.stringify({ className, expected, classList })}`)
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const writeRequests = []

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    page.on('request', (request) => {
      if (isWriteMethod(request.method()) && isMesRequest(request.url())) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })

    await login(page)
    await page.goto(new URL('/mes/pro/batch-record-form-list', config.baseUrl).toString(), { waitUntil: 'commit' })
    await page.getByText('批记录表单', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(
      await page.getByRole('button', { name: /单元格规则/ }).count(),
      0,
      'left_table_cell_rule_button_should_be_hidden'
    )

    const cellRulesResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/batch-record-report/cell-rules') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    const previewActions = page.locator('.batch-record-form-preview__actions').first()
    await previewActions.waitFor({ state: 'visible', timeout: 60000 })
    await previewActions.getByRole('button', { name: '规则' }).click()
    await cellRulesResponsePromise

    const editor = page.locator('.batch-record-cell-rules-editor').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })
    const preview = editor.locator('.batch-record-cell-rules-editor__preview').first()
    const sidePanel = editor.locator('.batch-record-cell-rules-editor__side-panel').first()
    await preview.waitFor({ state: 'visible', timeout: 30000 })
    await sidePanel.waitFor({ state: 'visible', timeout: 30000 })
    await preview.locator('.batch-record-cell-rules-editor__cell').first().waitFor({ state: 'visible', timeout: 60000 })

    const { button: fillableButton, text: targetCellText } = await pickFillableCell(preview)
    await fillableButton.click()
    const selectedCell = preview.locator('.batch-record-cell-rules-editor__cell.is-selected').first()
    const selectedCellButton = selectedCell.locator('.batch-record-cell-rules-editor__cell-button').first()
    await selectedCell.waitFor({ state: 'visible', timeout: 30000 })
    await waitForCellClass(page, selectedCell, 'is-rule', true)
    const fillableSwitch = sidePanel.locator('.batch-record-cell-rules-editor__fillable-toggle .el-switch').first()
    const fillableSwitchCore = sidePanel.locator('.batch-record-cell-rules-editor__fillable-toggle .el-switch__core').first()
    await fillableSwitch.waitFor({ state: 'visible', timeout: 30000 })
    await fillableSwitchCore.waitFor({ state: 'visible', timeout: 30000 })

    await fillableSwitchCore.click()
    await waitForCellClass(page, selectedCell, 'is-rule', false)

    assert.equal(
      await sidePanel.locator('.batch-record-cell-rules-editor__selected-card').count(),
      0,
      'selected_card_should_be_hidden_after_turning_off_fillable'
    )
    assert.equal(
      await sidePanel.locator('.batch-record-cell-rules-editor__static-tip').count(),
      0,
      'static_tip_should_be_hidden_after_turning_off_fillable'
    )
    assert.equal(await sidePanel.locator('.batch-record-cell-rules-editor__form').count(), 0)

    const writeCountBeforeWhiteClick = writeRequests.length
    await selectedCellButton.click()
    await waitForCellClass(page, selectedCell, 'is-rule', false)
    assert.equal(writeRequests.length, writeCountBeforeWhiteClick, 'clicking_white_cell_must_not_send_write_request')

    await fillableSwitchCore.click()
    await waitForCellClass(page, selectedCell, 'is-rule', true)
    assert.equal(
      await sidePanel.locator('.batch-record-cell-rules-editor__selected-card').count(),
      0,
      'selected_card_should_stay_hidden_after_turning_on_fillable'
    )
    await sidePanel.locator('.batch-record-cell-rules-editor__form').waitFor({ state: 'visible', timeout: 30000 })

    await fillableSwitchCore.click()
    await waitForCellClass(page, selectedCell, 'is-rule', false)
    assert.equal(
      await sidePanel.locator('.batch-record-cell-rules-editor__selected-card').count(),
      0,
      'selected_card_should_stay_hidden_after_reverting_to_static'
    )
    assert.equal(
      await sidePanel.locator('.batch-record-cell-rules-editor__static-tip').count(),
      0,
      'static_tip_should_stay_hidden_after_reverting_to_static'
    )
    assert.deepEqual(writeRequests, [], 'readonly_toggle_e2e_must_not_send_mes_write_requests')

    const screenshot = path.join(outputDir, 'batch-record-cell-rule-fillable-toggle-pass.png')
    await page.screenshot({ path: screenshot, fullPage: true })
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      targetCellText,
      writeRequests,
      screenshot
    }
    fs.writeFileSync(
      path.join(outputDir, 'batch-record-cell-rule-fillable-toggle-result.json'),
      JSON.stringify(result, null, 2),
      'utf8'
    )
    console.log(JSON.stringify(result, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
